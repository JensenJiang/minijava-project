package spiglet.spiglet2kanga;

import com.sun.org.apache.xpath.internal.operations.Bool;
import spiglet.syntaxtree.*;
import spiglet.visitor.GJNoArguDepthFirst;

import java.util.Enumeration;
import java.util.LinkedList;

/**
 * Created by jensen on 2017/6/4.
 */
public class KangaBuilder extends GJNoArguDepthFirst<KangaBuilder.KangaFragment>{
    int stmt_num;
    int indent;
    int _SPACES_PER_INDENT;
    LinearScanner _cur_scanner;

    public KangaBuilder() {
        this.indent = 0;
        this._SPACES_PER_INDENT = 4;
    }

    void reset_stmt_num() {
        this.stmt_num = 0;
    }

    int allocate_stmt_num() {
        this.stmt_num ++;
        return this.stmt_num - 1;
    }

    public class KangaFragment {
        StringBuilder kanga;
        String exp;
        LinkedList<Register> regs;
        int list_ele_num;   // only for arguments optional list

        KangaFragment() {
            this.kanga = new StringBuilder();
            this.list_ele_num = 0;
            this.regs = new LinkedList<>();
        }

        KangaFragment append(KangaFragment kf) {
            this.kanga.append(kf.getString());
            return this;
        }

        public String getString() {
            return this.kanga.toString();
        }

        KangaFragment append(String s) {
            this.kanga.append(s);
            return this;
        }

        /* kanga writer section starts */
        void write_indent() {
            for(int i = 0;i < indent * _SPACES_PER_INDENT;i++) this.append(" ");
        }

        void write_label(String label) {
            this.write_indent();
            this.append(label + "\n");
        }

        void write_begin(String label, int a, int b, int c) {
            this.write_label(label + " " + "[" + Integer.toString(a) + "][" + Integer.toString(b) + "][" + Integer.toString(c) + "]");
        }

        void write_end() {
            this.write_label("END");
        }

        void write_move(String reg, String exp) {
            this.write_indent();
            this.append("MOVE " + reg + " " + exp + "\n");
        }

        void write_astore(int stack_id, int reg_id) {
            this.write_indent();
            this.append("ASTORE " + SPILLEDARG(stack_id) + " " + REG(reg_id) + "\n");
        }

        void write_aload(int reg_id, int stack_id) {
            this.write_indent();
            this.append("ALOAD " + REG(reg_id) + " " + SPILLEDARG(stack_id) + "\n");
        }

        void write_noop() {
            this.write_indent();
            this.append("NOOP\n");
        }

        void write_error() {
            this.write_indent();
            this.append("ERROR\n");
        }

        void write_cjump(String reg, String label) {
            this.write_indent();
            this.append("CJUMP " + reg + " " + label + "\n");
        }

        void write_jump(String label) {
            this.write_indent();
            this.append("JUMP " + label + "\n");
        }

        void write_hstore(String reg1, String intexp, String reg2) {
            this.write_indent();
            this.append("HSTORE " + reg1 + " " + intexp + " " + reg2 + "\n");
        }

        void write_hload(String reg1, String reg2, String intexp) {
            this.write_indent();
            this.append("HLOAD " + reg1 + " " + reg2 + " " + intexp + "\n");
        }

        void write_print(String simexp) {
            this.write_indent();
            this.append("PRINT " + simexp + "\n");
        }

        void write_call(String simexp) {
            this.write_indent();
            this.append("CALL " + simexp + "\n");
        }

        void write_passarg(int id, String reg) {
            this.write_indent();
            this.append("PASSARG " + Integer.toString(id) + " " + reg + "\n");
        }
        /* kanga writer section ends */
    }

    /* inline */
    static String SPILLEDARG(int stack_id) {
        return "SPILLEDARG " + Integer.toString(stack_id);
    }

    static String REG(int reg_id) {
        return LinearScanner.regs_name[reg_id];
    }

    static String BINOP(String op, String reg, String simexp) {
        return op + " " + reg + " " + simexp;
    }

    static String HALLOCATE(String simexp) {
        return "HALLOCATE " + simexp;
    }

    /* state helper */
    void inc_indent(){
        this.indent += 1;
    }
    void dec_indent(){
        this.indent -= 1;
    }

    /* Visitor Section starts */
    /**
     * f0 -> "MAIN"
     * f1 -> StmtList()
     * f2 -> "END"
     * f3 -> ( Procedure() )*
     * f4 -> <EOF>
     */
    public KangaFragment visit(Goal n) {
        KangaFragment kf = new KangaFragment();
        this._cur_scanner = new LinearScanner(0);

        n.f0.accept(this);
        this.inc_indent();
        this.reset_stmt_num();

        n.f1.accept(this._cur_scanner);   // Build flow graph and compute live intervals.
        KangaFragment kf_sub1 = n.f1.accept(this);

        n.f2.accept(this);
        this.dec_indent();

        kf.write_begin("MAIN", 0, this._cur_scanner.global_stack_id, this._cur_scanner.max_call_argu);
        kf.append(kf_sub1);
        kf.write_end();
        kf.append("\n");

        KangaFragment kf_sub2 = n.f3.accept(this);
        kf.append(kf_sub2);

        n.f4.accept(this);
        return kf;
    }

    /**
     * f0 -> ( ( Label() )? Stmt() )*
     */
    public KangaFragment visit(StmtList n) {
        KangaFragment kf;
        this._cur_scanner.enter_block(0);
        kf = n.f0.accept(this);
        return kf;
    }

    public KangaFragment visit(NodeListOptional n) {
        KangaFragment kf = new KangaFragment();
        if ( n.present() ) {
            int _count=0;
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                Node next = e.nextElement();
                KangaFragment next_kf;
                /* StmtList */
                if(next instanceof NodeSequence) {
                    int cur_stmt_num = this.allocate_stmt_num();
                    for(Node ele : ((NodeSequence) next).nodes) {
                        /* Label */
                        if(ele instanceof NodeOptional && ((NodeOptional) ele).present()) {
                            this._cur_scanner.write_back_all_regs(kf);
                            this._cur_scanner.enter_block(cur_stmt_num);
                            kf.write_label(ele.accept(this).exp);
                        }
                        else if(ele instanceof Stmt){
                            Node stmt_ele = ((Stmt) ele).f0.choice;
                            /* CJumpStmt or JumpStmt */
                            if(stmt_ele instanceof CJumpStmt || stmt_ele instanceof JumpStmt) {
                                this._cur_scanner.register_allocation(cur_stmt_num, kf);
                                next_kf = ele.accept(this);
                                this._cur_scanner.write_back_all_regs(kf);
                                kf.append(next_kf);     // should not proceed write_back_all_regs()!!!
                                this._cur_scanner.enter_block(cur_stmt_num + 1);
                            }
                            else {
                                this._cur_scanner.register_allocation(cur_stmt_num, kf);
                                kf.append(ele.accept(this));
                            }
                        }
                    }
                    // if(!e.hasMoreElements()) this._cur_scanner.write_back_all_regs(kf);
                }
                /* Temp */
                else if(next instanceof Temp) {
                    int temp_id = Integer.parseInt(((Temp) next).f1.f0.tokenImage);
                    if(_count < 4) {
                        Integer reg_id = this._cur_scanner.temp_reg_id.get(temp_id);
                        /* has Regsiter */
                        if(reg_id != null) {
                            this._cur_scanner.load_to_reg(reg_id, kf);
                            kf.write_move(REG(20 + _count), REG(reg_id));
                        }
                        else {
                            Integer stack_id = this._cur_scanner.temp_stack_id.get(temp_id);
                            assert stack_id != null : "It hasn't been allocated stack!";
                            kf.write_aload(20 + _count, stack_id);
                         }
                    }
                    else {
                        next_kf = next.accept(this);
                        kf.append(next_kf);
                        Register next_reg = next_kf.regs.poll();
                        this._cur_scanner.load_to_reg(next_reg.reg_id, kf);
                        kf.write_passarg(_count - 3, next_kf.exp);
                        next_reg.set_unlocked();
                    }
                }
                else if(next instanceof Procedure) {
                    next_kf = next.accept(this);
                    kf.append(next_kf);
                }

                _count++;
            }
            kf.list_ele_num = _count;
        }
        return kf;
    }

    /**
     * f0 -> Label()
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> StmtExp()
     */
    public KangaFragment visit(Procedure n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        int argu_num = Integer.parseInt(n.f2.f0.tokenImage);
        this._cur_scanner = new LinearScanner(argu_num);
        n.f4.accept(this._cur_scanner);
        KangaFragment kf_sub = n.f4.accept(this);
        kf.write_begin(n.f0.f0.tokenImage, argu_num, this._cur_scanner.global_stack_id, this._cur_scanner.max_call_argu);
        kf.append(kf_sub);

        return kf;
    }

    /**
     * f0 -> "BEGIN"
     * f1 -> StmtList()
     * f2 -> "RETURN"
     * f3 -> SimpleExp()
     * f4 -> "END"
     */
    public KangaFragment visit(StmtExp n) {
        KangaFragment kf = new KangaFragment();
        this.inc_indent();
        n.f0.accept(this);
        /* arguments processing */
        for(int i = 4;i < Integer.max(4, this._cur_scanner.argu_num);i++) {
            this._cur_scanner.temp_stack_id.put(i, i - 4);
        }
        for(int i = 0;i < Integer.min(4, this._cur_scanner.argu_num);i++) {
            int stack_id = this._cur_scanner.allocate_stack_id();
            kf.write_astore(stack_id, i + 20);
            this._cur_scanner.temp_stack_id.put(i, stack_id);
        }

        this.reset_stmt_num();
        n.f1.accept(this._cur_scanner);
        KangaFragment kf_sub1 = n.f1.accept(this);
        n.f2.accept(this);
        KangaFragment kf_sub2 = n.f3.accept(this);

        n.f4.accept(this);


        this._cur_scanner.save_registers(kf);
        kf.append(kf_sub1);
        kf.append(kf_sub2);
        if(!kf_sub2.regs.isEmpty()) {
            this._cur_scanner.load_to_reg(kf_sub2.regs.peek().reg_id, kf);
        }
        kf.write_move(REG(18), kf_sub2.exp);
        this._cur_scanner.restore_registers(kf);
        this.dec_indent();
        kf.write_end();
        kf.append("\n");
        return kf;
    }

    /**
     * f0 -> NoOpStmt()
     *       | ErrorStmt()
     *       | CJumpStmt()
     *       | JumpStmt()
     *       | HStoreStmt()
     *       | HLoadStmt()
     *       | MoveStmt()
     *       | PrintStmt()
     */
    public KangaFragment visit(Stmt n) {
        KangaFragment kf;
        kf = n.f0.accept(this);
        return kf;
    }

    /**
     * f0 -> "NOOP"
     */
    public KangaFragment visit(NoOpStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        kf.write_noop();
        return kf;
    }

    /**
     * f0 -> "ERROR"
     */
    public KangaFragment visit(ErrorStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        kf.write_error();
        return kf;
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Temp()
     * f2 -> Label()
     */
    public KangaFragment visit(CJumpStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub1 = n.f1.accept(this);
        KangaFragment kf_sub2 = n.f2.accept(this);

        kf.append(kf_sub1);
        Register sub1_reg = kf_sub1.regs.poll();
        this._cur_scanner.load_to_reg(sub1_reg.reg_id, kf);
        kf.append(kf_sub2);

        kf.write_cjump(kf_sub1.exp, kf_sub2.exp);
        sub1_reg.set_unlocked();
        return kf;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public KangaFragment visit(JumpStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        kf.write_jump(n.f1.f0.tokenImage);
        return kf;
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Temp()
     * f2 -> IntegerLiteral()
     * f3 -> Temp()
     */
    public KangaFragment visit(HStoreStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub1 = n.f1.accept(this);
        n.f2.accept(this);
        KangaFragment kf_sub2 = n.f3.accept(this);

        kf.append(kf_sub1);
        Register sub1_reg = kf_sub1.regs.poll();
        this._cur_scanner.load_to_reg(sub1_reg.reg_id, kf);

        kf.append(kf_sub2);
        Register sub2_reg = kf_sub2.regs.poll();
        this._cur_scanner.load_to_reg(sub2_reg.reg_id, kf);
        kf.write_hstore(kf_sub1.exp, n.f2.f0.tokenImage, kf_sub2.exp);
        sub1_reg.set_unlocked();
        sub2_reg.set_unlocked();
        return kf;
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Temp()
     * f2 -> Temp()
     * f3 -> IntegerLiteral()
     */
    public KangaFragment visit(HLoadStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub1 = n.f1.accept(this);
        KangaFragment kf_sub2 = n.f2.accept(this);
        n.f3.accept(this);

        kf.append(kf_sub1);
        Register sub1_reg = kf_sub1.regs.poll();

        kf.append(kf_sub2);
        Register sub2_reg = kf_sub2.regs.poll();
        this._cur_scanner.load_to_reg(sub2_reg.reg_id, kf);
        kf.write_hload(kf_sub1.exp, kf_sub2.exp, n.f3.f0.tokenImage);
        sub1_reg.set_write();
        sub1_reg.set_unlocked();
        sub2_reg.set_unlocked();
        return kf;
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Temp()
     * f2 -> Exp()
     */
    public KangaFragment visit(MoveStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub1 = n.f1.accept(this);
        KangaFragment kf_sub2 = n.f2.accept(this);

        kf.append(kf_sub1);
        Register sub1_reg = kf_sub1.regs.poll();

        kf.append(kf_sub2);
        /* if Exp() is TEMP or BinOP */
        if(!kf_sub2.regs.isEmpty()) {
            for(Register reg : kf_sub2.regs) {
                this._cur_scanner.load_to_reg(reg.reg_id, kf);
            }
        }
        kf.write_move(kf_sub1.exp, kf_sub2.exp);

        sub1_reg.set_write();
        sub1_reg.set_unlocked();
        /* if Exp() is TEMP or BinOP */
        if(!kf_sub2.regs.isEmpty()) {
            for(Register reg : kf_sub2.regs) {
                reg.set_unlocked();
            }
        }
        return kf;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public KangaFragment visit(PrintStmt n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub = n.f1.accept(this);

        kf.append(kf_sub);
        /* if SimpleExp() is TEMP */
        if(!kf_sub.regs.isEmpty()) {
            this._cur_scanner.load_to_reg(kf_sub.regs.peek().reg_id, kf);
        }
        kf.write_print(kf_sub.exp);

        if(!kf_sub.regs.isEmpty()) {
            kf_sub.regs.poll().set_unlocked();
        }
        return kf;
    }

    /**
     * f0 -> Temp()
     *       | IntegerLiteral()
     *       | Label()
     */
    public KangaFragment visit(SimpleExp n) {
        KangaFragment kf;
        kf = n.f0.accept(this);
        return kf;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public KangaFragment visit(IntegerLiteral n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        kf.exp = n.f0.tokenImage;
        return kf;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public KangaFragment visit(Label n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        kf.exp = n.f0.tokenImage;
        return kf;
    }

    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public KangaFragment visit(Temp n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        int temp_id = Integer.parseInt(n.f1.f0.tokenImage);
        Register reg = this._cur_scanner.force_get_reg(temp_id, kf);
        reg.set_locked();
        kf.exp = REG(reg.reg_id);
        kf.regs.add(reg);
        return kf;
    }

    /**
     * f0 -> Call()
     *       | HAllocate()
     *       | BinOp()
     *       | SimpleExp()
     */
    public KangaFragment visit(Exp n) {
        KangaFragment kf;
        kf = n.f0.accept(this);
        return kf;
    }

    /**
     * f0 -> Operator()
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public KangaFragment visit(BinOp n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub1 = n.f1.accept(this);
        KangaFragment kf_sub2 = n.f2.accept(this);

        kf.append(kf_sub1);
        kf.regs.add(kf_sub1.regs.poll());
        kf.append(kf_sub2);
        if(!kf_sub2.regs.isEmpty()) {
            kf.regs.add(kf_sub2.regs.poll());
        }

        String op;
        switch(n.f0.f0.which){
            case 0:
                op = "LT";
                break;
            case 1:
                op = "PLUS";
                break;
            case 2:
                op = "MINUS";
                break;
            case 3:
                op = "TIMES";
                break;
            default:
                op = "WRONGOP";
                break;
        }

        kf.exp = BINOP(op, kf_sub1.exp, kf_sub2.exp);

        return kf;
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public KangaFragment visit(HAllocate n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub = n.f1.accept(this);

        kf.append(kf_sub);
        if(!kf_sub.regs.isEmpty()) {
            kf.regs.add(kf_sub.regs.poll());
        }
        kf.exp = HALLOCATE(kf_sub.exp);

        return kf;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     * f2 -> "("
     * f3 -> ( Temp() )*
     * f4 -> ")"
     */
    public KangaFragment visit(Call n) {
        KangaFragment kf = new KangaFragment();
        n.f0.accept(this);
        KangaFragment kf_sub1 = n.f1.accept(this);
        n.f2.accept(this);
        KangaFragment kf_sub2 = n.f3.accept(this);
        n.f4.accept(this);

        kf.append(kf_sub1);
        if(!kf_sub1.regs.isEmpty()) {
            this._cur_scanner.load_to_reg(kf_sub1.regs.peek().reg_id, kf);
        }

        kf.append(kf_sub2);

        if(!kf_sub1.regs.isEmpty()) {
            kf_sub1.regs.poll().set_unlocked();
        }
        this._cur_scanner.write_back_temp_regs(kf);         // backup temp registers

        kf.write_call(kf_sub1.exp);
        kf.exp = REG(18);   // v0
        this._cur_scanner.update_max_argu(kf_sub2.list_ele_num);

        return kf;
    }
    public KangaFragment visit(NodeOptional n) {
        KangaFragment kf;
        if ( n.present() )
            kf = n.node.accept(this);
        else
            kf = new KangaFragment();
        return kf;
    }


    /* Visitor Section ends */
}
