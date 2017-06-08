package spiglet.spiglet2kanga;

import spiglet.syntaxtree.*;
import spiglet.visitor.DepthFirstVisitor;
import sun.jvm.hotspot.utilities.Interval;

import java.rmi.registry.Registry;
import java.util.*;

/**
 * Created by jensen on 2017/6/4.
 */
public class LinearScanner extends DepthFirstVisitor {
    public static String[] regs = {"t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "v0", "v1", "a0", "a1", "a2", "a3"};
    public static int MAX_GENERAL_REGS_NUM = 16;
    BasicBlock _cur_block;      // For both construction stage and running stage
    Hashtable<Integer, Integer> temp_stack_id;
    Hashtable<Integer, BasicBlock> entry_stmt_block;
    LinkedHashSet<Integer> used_regs;
    int global_stack_id;
    int max_call_argu = 0;
    int argu_num;
    int backup_start, backup_end;

    public LinearScanner(int _argu_num) {
        this.argu_num = _argu_num;
        this.global_stack_id = Integer.max(this.argu_num, 4) - 4;
        this.temp_stack_id = new Hashtable<>();
        this.entry_stmt_block = new Hashtable<>();
        this._cur_block = new BasicBlock(0);
        this.used_regs = new LinkedHashSet<>();
    }

    int allocate_stack_id() {
        this.global_stack_id ++;
        return this.global_stack_id - 1;
    }

    void save_registers(KangaBuilder.KangaFragment kf) {
        this.backup_start = this.global_stack_id;
        for(Integer i : this.used_regs) {
            kf.write_astore(this.allocate_stack_id(), i);
        }
    }

    void restore_registers(KangaBuilder.KangaFragment kf) {
        int stack_id = this.backup_start;
        for(Integer i : this.used_regs) {       // the iteration order should be the same!!
            kf.write_aload(i, stack_id);
            stack_id ++;
        }
    }

    void update_max_argu(int argu_num) {
        this.max_call_argu = Integer.max(this.max_call_argu, argu_num);
    }

    int get_stack_id(int temp_id) {
        Integer ret = this.temp_stack_id.get(temp_id);
        if(ret == null) {
            ret = this.allocate_stack_id();
            this.temp_stack_id.put(temp_id, ret);
        }
        return ret;
    }

    void release_all_temp_reg(KangaBuilder.KangaFragment kf) {
        this._cur_block._release_all_temp_reg(kf);
    }

    Pair<Boolean, Integer> get_location(int temp_id) {
        return this._cur_block._get_location(temp_id);
    }
    void register_allocation(int cur_stmt_num, KangaBuilder.KangaFragment kf) {
        this._cur_block._register_allocation(cur_stmt_num, kf);
    }

    int get_reg(int temp_id, KangaBuilder.KangaFragment kf) {
        return this._cur_block._get_reg(temp_id, kf);
    }

    /* state helper */
    public void enter_block(int stmt_num) {
        BasicBlock _to_enter = this.entry_stmt_block.get(stmt_num);
        assert(_to_enter != null);
        this._cur_block = _to_enter;
        this._cur_block.running_stage_init();
    }


    class BasicBlock extends DepthFirstVisitor {
        int stmt_num;
        Hashtable<Integer, IntervalPair> _temp_cur_interval;
        Hashtable<Integer, Integer> _temp2reg;
        ArrayList<IntervalPair> _intervals;
        ArrayList<Integer> _avail_regs;
        LinkedList<Integer> _avail_temp_regs;
        LinkedList<Pair<Integer, Integer>> _reg2temp;  // only for temp register
        PriorityQueue<IntervalPair> active_list;

        BasicBlock(int _start) {
            this.stmt_num = _start;
            entry_stmt_block.put(this.stmt_num, this);
            this._temp_cur_interval = new Hashtable<>();
            this._intervals = new ArrayList<>();
            this._temp2reg = new Hashtable<>();
            this._reg2temp = new LinkedList<>();
            this.active_list = new PriorityQueue<>(IntervalPair.RightComparator);
            this._avail_regs = new ArrayList<>();
            this._avail_temp_regs = new LinkedList<>();
        }

        int allocate_stmt_num() {
            this.stmt_num ++;
            return this.stmt_num - 1;
        }

        Pair<Boolean, Integer> _get_location(int temp_id) {  // in stack or in register
            Integer reg_id = this._temp2reg.get(temp_id);
            if(reg_id == null) {
                int stack_id = get_stack_id(temp_id);
                return new Pair<>(false, stack_id);
            }
            else return new Pair<>(true, reg_id);
        }

        int _get_reg(int temp_id, KangaBuilder.KangaFragment kf) {
            Pair<Boolean, Integer> loc = this._get_location(temp_id);
            if(loc.get_first()) return loc.second;
            else {
                if(!this._avail_temp_regs.isEmpty()) {
                    int reg_id = this._avail_temp_regs.pollFirst();
                    this._temp2reg.put(temp_id, reg_id);
                    this._reg2temp.add(new Pair<>(reg_id, temp_id));
                    kf.write_aload(reg_id, loc.second);
                    return reg_id;
                }
                else {
                    int reg_id = this._release_first_reg(kf);
                    this._temp2reg.put(temp_id, reg_id);
                    this._reg2temp.add(new Pair<>(reg_id, temp_id));
                    kf.write_aload(reg_id, loc.second);
                    return reg_id;
                }
            }
        }

        int _release_first_reg(KangaBuilder.KangaFragment kf) {
            Pair<Integer, Integer> pair = this._reg2temp.pollFirst();
            assert(pair != null);
            int reg_id = pair.get_first();
            this._temp2reg.remove(pair.second);
            kf.write_astore(temp_stack_id.get(pair.second), reg_id);
            return reg_id;
        }

        void _release_all_temp_reg(KangaBuilder.KangaFragment kf) {
            for(Pair<Integer, Integer> e : this._reg2temp) {
                this._temp2reg.remove(e.get_second());
                this._avail_temp_regs.add(e.get_first());
                kf.write_astore(temp_stack_id.get(e.get_second()), e.get_first());
            }
            this._reg2temp.clear();
        }

        public void running_stage_init() {
            this._intervals.sort(IntervalPair.LeftComparator);

            for(int i = 0;i < MAX_GENERAL_REGS_NUM;i++) this._avail_regs.add(i);
            this._avail_temp_regs.add(16);
            this._avail_temp_regs.add(17);
            this._avail_temp_regs.add(19);
        }

        void remove_outdated(int cur_stmt_num, KangaBuilder.KangaFragment kf) { // pass the maximum stmt_num + 1 to this method, it would clean out all the regsiters
            ArrayList<IntervalPair> to_remove = new ArrayList<>();
            for(IntervalPair e : active_list) {
                if(e.get_second() < cur_stmt_num) {
                    to_remove.add(e);
                    /* recycle the register */
                    Integer reg_id = this._temp2reg.remove(e.temp_id);
                    assert(reg_id != null);
                    kf.write_astore(get_stack_id(e.temp_id), reg_id);
                    _avail_regs.add(reg_id);

                }
            }
            for(IntervalPair e : to_remove) {
                active_list.remove(e);
            }
        }

        void remove_all(KangaBuilder.KangaFragment kf) {
            for(IntervalPair e : active_list) {
                /* recycle the register */
                Integer reg_id = this._temp2reg.remove(e.temp_id);
                assert(reg_id != null);
                kf.write_astore(get_stack_id(e.temp_id), reg_id);
                _avail_regs.add(reg_id);
            }
        }


        void _register_allocation(int cur_stmt_num, KangaBuilder.KangaFragment kf) {
            boolean has_clean = false;
            while(!this._intervals.isEmpty() && this._intervals.get(0).get_first() <= cur_stmt_num) {
                if(!has_clean) {
                    this.remove_outdated(cur_stmt_num, kf);
                    has_clean = true;
                }
                IntervalPair cur_interval = this._intervals.remove(0);
                /* exists available register */
                if(!this._avail_regs.isEmpty()) {
                    int reg_id = this._avail_regs.remove(0);
                    _temp2reg.put(cur_interval.temp_id, reg_id);
                    used_regs.add(reg_id);
                    active_list.add(cur_interval);
                    kf.write_aload(reg_id, get_stack_id(cur_interval.temp_id));
                }
                else {
                    IntervalPair first = this.active_list.peek();
                    if(first.get_second() > cur_interval.get_second()) {
                        /* recycle */
                        Integer reg_id = this._temp2reg.remove(first.temp_id);
                        assert(reg_id != null);
                        kf.write_astore(get_stack_id(first.temp_id), reg_id);
                        active_list.remove(first);

                        /* add new */
                        this._temp2reg.put(cur_interval.temp_id, reg_id);
                        active_list.add(cur_interval);
                        // do not need to aload from stack. (1) it may not have space in stack (2) it should be written in new data immediately
                    }
                }
            }
        }

        void sort_intervals() {
            _intervals.sort(IntervalPair.LeftComparator);
        }

        /* interval helper starts */
        IntervalPair new_interval(int _L, int _tid) {
            IntervalPair interval = new IntervalPair(_L, _L, _tid);
            this._intervals.add(interval);
            this._temp_cur_interval.put(_tid, interval);
            return interval;
        }

        IntervalPair get_interval(int temp_id) {
            IntervalPair ret = this._temp_cur_interval.get(temp_id);
            if(ret == null) ret = this.new_interval(0, temp_id);
            return ret;
        }

        void update_interval_R(int temp_id, int _R) {
            IntervalPair interval = this.get_interval(temp_id);
            interval.set_second(_R);
        }

        void update_interval_L(int temp_id, int _L) {
            IntervalPair interval = this.new_interval(_L, temp_id);
        }

    /* interval helper ends */


    /* Visitor Section starts */
        /**
         * f0 -> "NOOP"
         */
        public void visit(NoOpStmt n) {
            this.allocate_stmt_num();
            n.f0.accept(this);
        }

        /**
         * f0 -> "ERROR"
         */
        public void visit(ErrorStmt n) {
            this.allocate_stmt_num();
            n.f0.accept(this);
        }

        /**
         * f0 -> "CJUMP"
         * f1 -> Temp()
         * f2 -> Label()
         */
        public void visit(CJumpStmt n) {
            int cur_stmt_num = this.allocate_stmt_num();
            n.f0.accept(this);
            n.f1.accept(this);
            this.update_interval_R(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
            n.f2.accept(this);
        }

        /**
         * f0 -> "JUMP"
         * f1 -> Label()
         */
        public void visit(JumpStmt n) {
            int cur_stmt_num = this.allocate_stmt_num();
            n.f0.accept(this);
            n.f1.accept(this);
        }

        /**
         * f0 -> "HSTORE"
         * f1 -> Temp()
         * f2 -> IntegerLiteral()
         * f3 -> Temp()
         */
        public void visit(HStoreStmt n) {
            int cur_stmt_num = this.allocate_stmt_num();
            n.f0.accept(this);
            n.f1.accept(this);
            this.update_interval_R(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
            n.f2.accept(this);
            n.f3.accept(this);
            this.update_interval_R(Integer.parseInt(n.f3.f1.f0.tokenImage), cur_stmt_num);
        }

        /**
         * f0 -> "HLOAD"
         * f1 -> Temp()
         * f2 -> Temp()
         * f3 -> IntegerLiteral()
         */
        public void visit(HLoadStmt n) {
            int cur_stmt_num = this.allocate_stmt_num();
            n.f0.accept(this);
            n.f1.accept(this);
            this.update_interval_L(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
            n.f2.accept(this);
            this.update_interval_R(Integer.parseInt(n.f2.f1.f0.tokenImage), cur_stmt_num);
            n.f3.accept(this);
        }

        /**
         * f0 -> "MOVE"
         * f1 -> Temp()
         * f2 -> Exp()
         */
        public void visit(MoveStmt n) {
            int cur_stmt_num = this.allocate_stmt_num();
            n.f0.accept(this);
            n.f1.accept(this);
            this.update_interval_L(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
            n.f2.accept(this);
        }

        /**
         * f0 -> "PRINT"
         * f1 -> SimpleExp()
         */
        public void visit(PrintStmt n) {
            int cur_stmt_num = this.allocate_stmt_num();
            n.f0.accept(this);
            n.f1.accept(this);
            if(n.f1.f0.choice instanceof Temp) this.update_interval_R(Integer.parseInt(((Temp)n.f1.f0.choice).f1.f0.tokenImage), cur_stmt_num);
        }
    }

    /* Visitor Section starts */
    public void visit(NodeListOptional n) {
        if ( n.present() ) {
            int _count = 0;
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                Node next = e.nextElement();
                if (next instanceof NodeSequence) {
                    for (Node ele : ((NodeSequence) next).nodes) {
                        if (ele instanceof NodeOptional && ((NodeOptional) ele).present()) {
                            BasicBlock _new_block = new BasicBlock(this._cur_block.stmt_num);
                            this._cur_block = _new_block;
                            ele.accept(this._cur_block);
                        }
                        else if(ele instanceof Stmt){
                            Node stmt_ele = ((Stmt) ele).f0.choice;
                            if (stmt_ele instanceof CJumpStmt || stmt_ele instanceof JumpStmt) {
                                ele.accept(this._cur_block);
                                BasicBlock _new_block = new BasicBlock(this._cur_block.stmt_num);
                                this._cur_block = _new_block;
                            } else {
                                ele.accept(this._cur_block);
                            }
                        }
                    }
                }
                _count++;
            }
        }
    }
    /* Visitor Section ends */

}

class Pair<A, B> {
    protected A first;
    protected B second;
    public Pair(A _f, B _s) {
        this.first = _f;
        this.second = _s;
    }
    public void set_first(A _f) {
        this.first = _f;
    }
    public void set_second(B _s) {
        this.second = _s;
    }
    public A get_first() {
        return this.first;
    }
    public B get_second() {
        return this.second;
    }
}

class IntervalPair extends Pair<Integer, Integer> {
    int temp_id;
    public IntervalPair(Integer _f, Integer _s, int _tid) {
        super(_f, _s);
        this.temp_id = _tid;
    }
    public static Comparator<IntervalPair> LeftComparator = new Comparator<IntervalPair>() {
        public int compare(IntervalPair a, IntervalPair b) {
            return a.first.compareTo(b.first);
        }
    };

    public static Comparator<IntervalPair> RightComparator = new Comparator<IntervalPair>() {   // descending order
        public int compare(IntervalPair a, IntervalPair b) {
            return b.second.compareTo(a.second);
        }
    };
}

class Register {
    int reg_id;
    boolean is_dirty;
    Register(int _reg) {
        this.is_dirty = false;
        this.reg_id = _reg;
    }
    void set_dirty() {
        this.is_dirty = true;
    }
}