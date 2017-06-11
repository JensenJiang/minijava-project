package spiglet.spiglet2kanga;

import spiglet.syntaxtree.*;
import spiglet.visitor.DepthFirstVisitor;

import java.util.*;

/**
 * Created by jensen on 2017/6/9.
 */
public class LinearScanner extends DepthFirstVisitor {
    /* Regs */
    public static String[] regs_name = {"t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "v0", "v1", "a0", "a1", "a2", "a3"};
    public int[] tempreg_ids;
    public static int MAX_GENERAL_REGS_NUM = 16;
    public static int TOTAL_REGS_NUM = 24;
    public Register[] regs;

    /* Lookup */
    Hashtable<Integer, Integer> temp_stack_id;  // TEMP to stack_id
    Hashtable<Integer, Integer> temp_reg_id;    // TEMP to reg_id
    Hashtable<Integer, BasicBlock> entry_stmt_block;    // stmt_num to BasicBlock
    LinkedList<Integer> _avail_general_regs;
    LinkedList<Integer> _avail_temp_regs;

    /* State */
    int global_stack_id;
    int max_call_argu;
    int argu_num;   // arguments num of current procedure/main
    int backup_start;
    public BasicBlock cur_block;
    LinkedHashSet<Integer> used_regs;


    public LinearScanner(int _argu_num) {
        /* Regs */
        regs = new Register[TOTAL_REGS_NUM];
        this._avail_general_regs = new LinkedList<>();
        for(int i = 0;i < TOTAL_REGS_NUM;i++) {
            regs[i] = new Register(i);
        }
        for(int i = 0;i < MAX_GENERAL_REGS_NUM;i++) {
            this._avail_general_regs.add(i);
        }
        this.tempreg_ids = new int[3];
        this.tempreg_ids[0] = 16;
        this.tempreg_ids[1] = 17;
        this.tempreg_ids[2] = 19;
        this._avail_temp_regs = new LinkedList<>();
        for(int i : this.tempreg_ids) {
            this._avail_temp_regs.add(i);
        }

        /* Lookup */
        this.temp_stack_id = new Hashtable<>();
        this.temp_reg_id = new Hashtable<>();
        this.entry_stmt_block = new Hashtable<>();

        /* State */
        this.argu_num = _argu_num;
        this.max_call_argu = 0;
        this.global_stack_id = Integer.max(this.argu_num, 4) - 4;
        this.cur_block = new BasicBlock(0);
        this.used_regs = new LinkedHashSet<>();
    }

    /* Regs Management */
    void add_to_avail_list(int reg_id) {
        if(is_general_reg(reg_id)) {
            this._avail_general_regs.add(reg_id);
        }
        else if(is_temp_reg(reg_id)) {
            this._avail_temp_regs.add(reg_id);
        }
    }
    void write_back_all_regs(KangaBuilder.KangaFragment kf) {    // it is used at the end of a basic blocks
        for(Register reg : this.regs) {
            this.write_back_reg(reg, kf);
        }
        assert this._avail_general_regs.size() == MAX_GENERAL_REGS_NUM : "General Regsiters Num Wrong!";
        assert this._avail_temp_regs.size() == this.tempreg_ids.length : "Temp Registers Num Wrong!";
    }

    void write_back_temp_regs(KangaBuilder.KangaFragment kf) {
        for(int i : this.tempreg_ids) {
            this.write_back_reg(regs[i], kf);
        }
    }

    void _allocate_nomadic_reg(int temp_id, int reg_id) {
        this.temp_reg_id.put(temp_id, reg_id);
        Register reg = regs[reg_id];
        reg.temp_id = temp_id;
    }

    void _allocate_reg(int temp_id, int reg_id) {
        if(this.is_general_reg(reg_id)) this._avail_general_regs.remove(new Integer(reg_id));
        else if(this.is_temp_reg(reg_id)) this._avail_temp_regs.remove(new Integer(reg_id));

        this._allocate_nomadic_reg(temp_id, reg_id);
    }

    void load_to_reg(int reg_id, KangaBuilder.KangaFragment kf) {
        Register reg = regs[reg_id];
        if(reg.is_null) {
            kf.write_aload(reg_id, this.temp_stack_id.get(reg.temp_id));
            reg.is_null = false;
        }
    }

    Register force_get_reg(int temp_id, KangaBuilder.KangaFragment kf) {
        Integer reg_id = this.temp_reg_id.get(temp_id);
        if(reg_id == null) {
            /* search for not-write and unlocked temp register */
            for(int i : this._avail_temp_regs) {
                if(!regs[i].is_locked && !regs[i].is_write) {
                    this.write_back_reg(regs[i], kf);
                    this._allocate_reg(temp_id, i);
                    reg_id = i;
                    break;
                }
            }
            if(reg_id == null) {
                /* search for unlocked temp register */
                for(int i : this._avail_temp_regs) {
                    if(!regs[i].is_locked) {
                        this.write_back_reg(regs[i], kf);
                        this._allocate_reg(temp_id, i);
                        reg_id = i;
                        break;
                    }
                }
            }
        }
        assert reg_id != null : "Temp Register Allocation Error!";
        return regs[reg_id];
    }

    void write_back_reg(Register reg, KangaBuilder.KangaFragment kf) {
        if(reg.is_used()) {
            /* check if write-back is needed */
            if(reg.is_write) {
                kf.write_astore(this.get_stack_id(reg.temp_id), reg.reg_id);
            }

            /* recycle register */
            Integer assert_reg_id = this.temp_reg_id.remove(reg.temp_id);
            assert assert_reg_id == reg.reg_id : "Inconsistent Register Infomation!";
            this.add_to_avail_list(reg.reg_id);
            reg.set_unused();
        }

    }

    boolean is_general_reg(int reg_id) {
        return reg_id >=0 && reg_id < MAX_GENERAL_REGS_NUM;
    }

    boolean is_temp_reg(int reg_id) {
        for(int i : this.tempreg_ids) {
            if(reg_id == i) return true;
        }
        return false;
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


    /* Lookup helper */
    int get_stack_id(int temp_id) {
        Integer ret = this.temp_stack_id.get(temp_id);
        if(ret == null) {
            ret = this.allocate_stack_id();
            this.temp_stack_id.put(temp_id, ret);
        }
        return ret;
    }

    /* State Control */
    int allocate_stack_id() {
        this.global_stack_id ++;
        return this.global_stack_id - 1;
    }

    void update_max_argu(int argu_num) {
        this.max_call_argu = Integer.max(this.max_call_argu, argu_num);
    }

    /* Current Block Control Shortcuts */
    void register_allocation(int cur_stmt_num, KangaBuilder.KangaFragment kf) {
        this.cur_block._register_allocation(cur_stmt_num, kf);
    }

    public void enter_block(int stmt_num) {
        BasicBlock _to_enter = this.entry_stmt_block.get(stmt_num);
        assert _to_enter != null : "Block is missing!";
        this.cur_block = _to_enter;
        this.cur_block.running_stage_init();
    }

    class BasicBlock extends DepthFirstVisitor {
        /* State */
        int stmt_num;   // next stmt num
        int start_stmt_num;     // starting stmt num

        /* Interval */
        ArrayList<IntervalPair> _intervals;     // all the intervals in the block
        Hashtable<Integer, IntervalPair> _temp_cur_interval;
        PriorityQueue<IntervalPair> active_list;


        BasicBlock(int _start) {
            /* State */
            this.stmt_num = this.start_stmt_num = _start;
            entry_stmt_block.put(this.stmt_num, this);

            /* Interval */
            this._intervals = new ArrayList<>();
            this._temp_cur_interval = new Hashtable<>();
            this.active_list = new PriorityQueue<>(IntervalPair.RightComparator);
        }

        public void running_stage_init() {
            this._intervals.sort(IntervalPair.LeftComparator);
        }

        /* State Control */
        int allocate_stmt_num() {
            this.stmt_num ++;
            return this.stmt_num - 1;
        }

        /* Interval helpers */
        IntervalPair new_interval(int _L, int _tid) {
            IntervalPair interval = new IntervalPair(_L, _L, _tid);
            this._intervals.add(interval);
            this._temp_cur_interval.put(_tid, interval);
            return interval;
        }

        IntervalPair get_interval(int temp_id) {
            IntervalPair ret = this._temp_cur_interval.get(temp_id);
            if(ret == null) ret = this.new_interval(this.start_stmt_num, temp_id);
            return ret;
        }

        void update_interval_R(int temp_id, int _R) {
            IntervalPair interval = this.get_interval(temp_id);
            interval.set_second(_R);
        }

        void update_interval_L(int temp_id, int _L) {
            IntervalPair interval = this.new_interval(_L, temp_id);
        }

        /* Block Register Allocation */
        void _register_allocation(int cur_stmt_num, KangaBuilder.KangaFragment kf) {
            boolean has_clean = false;
            while(!this._intervals.isEmpty() && this._intervals.get(0).get_first() <= cur_stmt_num) {
                /* Remove the outdated registers */
                if(!has_clean) {
                    this.remove_outdated(cur_stmt_num, kf);
                    has_clean = true;
                }


                IntervalPair cur_interval = this._intervals.remove(0);
                /* check if TEMP has taken up a register */
                if(temp_reg_id.get(cur_interval.temp_id) != null) continue;

                /* exists available register */
                if(!_avail_general_regs.isEmpty()) {
                    int reg_id = _avail_general_regs.poll();
                    _allocate_nomadic_reg(cur_interval.temp_id, reg_id);
                    used_regs.add(reg_id);
                    active_list.add(cur_interval);
                }
                else {
                    IntervalPair first = this.active_list.peek();
                    if(first.get_second() > cur_interval.get_second()) {
                        /* recycle */
                        Integer reg_id = temp_reg_id.get(first.temp_id);
                        write_back_reg(regs[reg_id], kf);
                        active_list.remove(first);

                        /* add new */
                        _allocate_reg(cur_interval.temp_id, reg_id);
                        active_list.add(cur_interval);
                        // do not need to aload from stack. (1) it may not have space in stack (2) it should be written in new data immediately
                    }
                }
            }
        }

        void remove_outdated(int cur_stmt_num, KangaBuilder.KangaFragment kf) {
            ArrayList<IntervalPair> to_remove = new ArrayList<>();
            for(IntervalPair e : active_list) {
                if(e.get_second() < cur_stmt_num) {
                    to_remove.add(e);

                    /* recycle the register */
                    Register reg = regs[temp_reg_id.get(e.temp_id)];
                    write_back_reg(reg, kf);
                }
            }
            for(IntervalPair e : to_remove) {
                active_list.remove(e);
            }
        }

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
            n.f2.accept(this);
            this.update_interval_R(Integer.parseInt(n.f2.f1.f0.tokenImage), cur_stmt_num);
            this.update_interval_L(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
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
            n.f2.accept(this);
            n.f1.accept(this);
            this.update_interval_L(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
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

        /**
         * f0 -> Temp()
         *       | IntegerLiteral()
         *       | Label()
         */
        public void visit(SimpleExp n) {
            n.f0.accept(this);
            Node choice = n.f0.choice;
            if(choice instanceof Temp) {
                this.update_interval_R(Integer.parseInt(((Temp) choice).f1.f0.tokenImage), this.stmt_num - 1);
            }
        }

        /**
         * f0 -> Operator()
         * f1 -> Temp()
         * f2 -> SimpleExp()
         */
        public void visit(BinOp n) {
            n.f0.accept(this);
            n.f1.accept(this);
            this.update_interval_R(Integer.parseInt(n.f1.f1.f0.tokenImage), this.stmt_num - 1);
            n.f2.accept(this);
        }

        public void visit(NodeListOptional n) {
            if ( n.present() )
                for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                    Node next = e.nextElement();
                    if(next instanceof Temp) {
                        this.update_interval_R(Integer.parseInt(((Temp) next).f1.f0.tokenImage), this.stmt_num - 1);
                    }
                }
        }
        /* Visitor Section ends */
    }

    /* Visitor Section starts */
    public void visit(NodeListOptional n) {
        if ( n.present() ) {
            int _count = 0;
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                Node next = e.nextElement();
                /* StmtList */
                if (next instanceof NodeSequence) {
                    for (Node ele : ((NodeSequence) next).nodes) {
                        /* Label */
                        if (ele instanceof NodeOptional && ((NodeOptional) ele).present()) {
                            BasicBlock _new_block = new BasicBlock(this.cur_block.stmt_num);
                            this.cur_block = _new_block;
                            ele.accept(this.cur_block);
                        }
                        /* Stmt */
                        else if(ele instanceof Stmt){
                            Node stmt_ele = ((Stmt) ele).f0.choice;
                            /* CJumpStmt or JumpStmt */
                            if (stmt_ele instanceof CJumpStmt || stmt_ele instanceof JumpStmt) {
                                ele.accept(this.cur_block);
                                BasicBlock _new_block = new BasicBlock(this.cur_block.stmt_num);
                                this.cur_block = _new_block;
                            } else {
                                ele.accept(this.cur_block);
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
    int temp_id;    // -1: not taken up by any TEMP
    boolean is_write;
    boolean is_null;
    boolean is_locked;

    Register(int reg_id) {
        this.reg_id = reg_id;
        this.temp_id = -1;
        this.is_write = false;
        this.is_locked = false;
        this.is_null = true;
    }

    void set_unused() {
        this.temp_id = -1;
        this.is_write = false;
        this.is_locked = false;
        this.is_null = true;
    }

    void set_locked() {
        this.is_locked = true;
    }

    void set_unlocked() {
        this.is_locked = false;
    }

    void set_write() {
        this.is_write = true;
        this.is_null = false;
    }

    public boolean is_used() {
        return this.temp_id != -1;
    }
}