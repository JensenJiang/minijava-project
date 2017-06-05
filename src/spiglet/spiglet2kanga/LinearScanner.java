package spiglet.spiglet2kanga;

import spiglet.syntaxtree.*;
import spiglet.visitor.DepthFirstVisitor;
import sun.jvm.hotspot.utilities.Interval;

import java.util.*;

/**
 * Created by jensen on 2017/6/4.
 */
public class LinearScanner extends DepthFirstVisitor {
    public static String[] regs = {"t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "a0", "a1", "a2", "a3", "v0", "v1"};
    public static int MAX_GENERAL_REGS_NUM = 18;
    BasicBlock _cur_block;      // For both construction stage and running stage
    Hashtable<Integer, Integer> temp_stack_id;
    Hashtable<Integer, BasicBlock> entry_stmt_block;
    int global_stack_id;

    public LinearScanner(int _stack_start_id) {
        this.global_stack_id = _stack_start_id;
        this.temp_stack_id = new Hashtable<>();
        this.entry_stmt_block = new Hashtable<>();
        this._cur_block = new BasicBlock(0);
    }

    int allocate_stack_id() {
        this.global_stack_id ++;
        return this.global_stack_id - 1;
    }

    int get_stack_id(int temp_id) {
        Integer ret = this.temp_stack_id.get(temp_id);
        if(ret == null) {
            ret = this.allocate_stack_id();
            this.temp_stack_id.put(temp_id, ret);
        }
        return ret;
    }

    /* state helper */
    public void enter_block(int stmt_num) {
        BasicBlock _to_enter = this.entry_stmt_block.get(stmt_num);
        assert(_to_enter != null);
        this._cur_block = _to_enter;
    }


    class BasicBlock extends DepthFirstVisitor {
        int stmt_num;
        Hashtable<Integer, IntervalPair> _temp_cur_interval;
        Hashtable<Integer, Integer> _temp2reg;
        ArrayList<IntervalPair> _intervals;
        ArrayList<Integer> _avail_regs;
        PriorityQueue<IntervalPair> active_list;

        BasicBlock(int _start) {
            this.stmt_num = _start;
            entry_stmt_block.put(this.stmt_num, this);
            this._temp_cur_interval = new Hashtable<>();
            this._intervals = new ArrayList<>();
        }

        int allocate_stmt_num() {
            this.stmt_num ++;
            return this.stmt_num - 1;
        }

        public void running_stage_init() {
            this._intervals.sort(IntervalPair.LeftComparator);
            this.active_list = new PriorityQueue<>(IntervalPair.RightComparator);
            this._avail_regs = new ArrayList<>();
            for(int i = 0;i < MAX_GENERAL_REGS_NUM;i++) this._avail_regs.add(i);
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

        public void register_allocation(int cur_stmt_num, KangaBuilder.KangaFragment kf) {
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
                    kf.write_aload(reg_id, get_stack_id(cur_interval.temp_id));
                }
                else {
                    // TODO: when there is no available register!
                    hahaha
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
        if ( n.present() )
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                Node next = e.nextElement();
                if(next instanceof NodeSequence) {
                    for(Node ele : ((NodeSequence)next).nodes) {
                        if(ele instanceof Label) {
                            BasicBlock _new_block = new BasicBlock(this._cur_block.stmt_num);
                            this._cur_block = _new_block;
                            ele.accept(this._cur_block);
                        }
                        else if(ele instanceof CJumpStmt || ele instanceof JumpStmt) {
                            ele.accept(this._cur_block);
                            BasicBlock _new_block = new BasicBlock(this._cur_block.stmt_num);
                            this._cur_block = _new_block;
                        }
                        else {
                            ele.accept(this._cur_block);
                        }
                    }
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
