package kanga.kanga2mips;

import kanga.syntaxtree.*;
import kanga.visitor.GJNoArguDepthFirst;

import java.util.Enumeration;

/**
 * Created by jensen on 2017/6/11.
 */
public class MIPSBuilder extends GJNoArguDepthFirst<MIPSBuilder.MIPSFragment> {
    /* Indent */
    int indent;
    int _SPACES_PER_INDENT;
    int _SIZE_PER_UNIT;

    /* MIPS related data */
    static String FRAME_PTR = "$fp";
    static String STACK_PTR = "$sp";
    static String RET_ADDR = "$ra";

    /* State */
    String _to_reg;     // only for BinOp

    public MIPSBuilder() {
        /* Indent */
        this.indent = 0;
        this._SPACES_PER_INDENT = 4;
        this._SIZE_PER_UNIT = 4;
    }

    /* Indent */
    void inc_indent(){
        this.indent += 1;
    }
    void dec_indent(){
        this.indent -= 1;
    }

    public class MIPSFragment {
        StringBuilder mips;
        String exp;
        MIPSExp exp_type;

        MIPSFragment() {
            this.mips = new StringBuilder();
            this.exp_type = MIPSExp.NONEXP;
        }

        MIPSFragment append(MIPSFragment mf) {
            this.mips.append(mf.getString());
            return this;
        }

        MIPSFragment append(String s) {
            this.mips.append(s);
            return this;
        }

        public String getString() {
            return this.mips.toString();
        }

        /* Writer Section starts */
        void write_some_indent(int _indent) {
            for(int i = 0;i < _indent * _SPACES_PER_INDENT;i++) this.append(" ");
        }
        void write_indent() {
            this.write_some_indent(indent);
        }

        void write_label(String _label) {
            this.write_indent();
            this.append(_label + ":\n");
        }

        void write_text_seg() {
            this.write_some_indent(1);
            this.append(".text\n");
        }

        void write_data_seg() {
            this.write_some_indent(1);
            this.append(".data\n");
        }

        void write_globl(String _label) {
            this.write_some_indent(1);
            this.append(".globl " + _label + "\n");
        }

        void write_procedure_init(int stack_unit_num) {
            this.write_move(FRAME_PTR, STACK_PTR);
            this.write_addi(STACK_PTR, STACK_PTR, Integer.toString(-_SIZE_PER_UNIT * (stack_unit_num + 2)));
            this.write_sw(FRAME_PTR, STACK_PTR, 1);
            this.write_sw(RET_ADDR, STACK_PTR, 2);
        }

        void write_procedure_final() {
            this.write_lw(RET_ADDR, STACK_PTR, 2);
            this.write_move(STACK_PTR, FRAME_PTR);
            this.write_lw(FRAME_PTR, STACK_PTR, 1);
            this.write_jr(RET_ADDR);
        }

        void write_move(String reg1, String reg2) {     // reg1 <- reg2
            this.write_indent();
            this.append("move " + reg1 + ", " + reg2 + "\n");
        }

        void write_addi(String reg1, String reg2, String intnum) {      // reg1 <- reg2 + intnum
            this.write_indent();
            this.append("addi " + reg1 + ", " + reg2 + ", " + intnum + "\n");
        }

        void write_sub(String reg1, String reg2, String reg3) {     // reg1 <- reg2 - reg3
            this.write_indent();
            this.append("sub " + reg1 + ", " + reg2 + ", " + reg3 + "\n");
        }

        void write_add(String reg1, String reg2, String reg3) {     // reg1 <- reg2 + reg3
            this.write_indent();
            this.append("add " + reg1 + ", " + reg2 + ", " + reg3 + "\n");
        }

        void write_mult(String reg1, String reg2) {
            this.write_indent();
            this.append("mult " + reg1 + ", " + reg2 + "\n");
        }

        void write_mflo(String reg) {
            this.write_indent();
            this.append("mflo " + reg + "\n");
        }

        void write_sw(String reg1, String reg2, String offset) {
            this.write_indent();
            this.append("sw " + reg1 + ", " + offset + "(" + reg2 + ")\n");
        }

        void write_sw(String reg1, String reg2, int unit_offset) {
            this.write_sw(reg1, reg2, Integer.toString(unit_offset * _SIZE_PER_UNIT));
        }

        void write_lw(String reg1, String reg2, String offset) {
            this.write_indent();
            this.append("lw " + reg1 + ", " + offset + "(" + reg2 + ")\n");
        }

        void write_lw(String reg1, String reg2, int unit_offset) {
            this.write_indent();
            this.append("lw " + reg1 + ", " + Integer.toString(unit_offset * _SIZE_PER_UNIT) + "(" + reg2 + ")\n");
        }

        void write_li(String reg, String intexp) {
            this.write_indent();
            this.append("li " + reg + ", " + intexp + "\n");
        }

        void write_la(String reg, String addr) {
            this.write_indent();
            this.append("la " + reg + ", " + addr + "\n");
        }

        void write_nop() {
            this.write_indent();
            this.append("nop\n");
        }

        void write_syscall(int _code) {
            this.write_li("$v0", Integer.toString(_code));
            this.write_indent();
            this.append("syscall\n");
        }

        void write_print_string(String straddr) {
            this.write_la("$a0", straddr);
            this.write_syscall(4);
        }

        void write_print_int_reg(String reg) {
            this.write_move("$a0", reg);
            this.write_syscall(1);
            this.write_la("$a0", "newline");
            this.write_syscall(4);
        }

        void write_print_int_int(String intexp) {
            this.write_li("$a0", intexp);
            this.write_syscall(1);
            this.write_la("$a0", "newline");
            this.write_syscall(4);
        }

        void write_exit() {
            this.write_syscall(10);
        }

        void write_beqz(String reg, String label) {
            this.write_indent();
            this.append("beqz " + reg + ", " + label + "\n");
        }

        void write_b(String label) {
            this.write_indent();
            this.append("b " + label + "\n");
        }

        void write_slt(String reg1, String reg2, String reg3) {
            this.write_indent();
            this.append("slt " + reg1 + ", " + reg2 + ", " + reg3 + "\n");
        }

        void write_slti(String reg1, String reg2, String intexp) {
            this.write_indent();
            this.append("slti " + reg1 + ", " + reg2 + ", " + intexp + "\n");
        }

        void write_jalr(String reg) {
            this.write_indent();
            this.append("jalr " + reg + "\n");
        }

        void write_jal(String label) {
            this.write_indent();
            this.append("jal " + label + "\n");
        }

        void write_jr(String reg) {
            this.write_indent();
            this.append("jr " + reg + "\n");
        }

        void write_data_part() {
            this.write_data_seg();
            this.write_some_indent(1);
            this.append(".align 0\n");
            this.append("error_str: .asciiz \"ERROR: abnormal termination\"\n");
            this.write_some_indent(1);
            this.append(".align 0\n");
            this.append("newline: .asciiz \"\\n\"\n");
        }

        /* Writer Section ends */
    }

    /* Visitor Section starts */
    /**
     * f0 -> "MAIN"
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> "["
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     * f7 -> "["
     * f8 -> IntegerLiteral()
     * f9 -> "]"
     * f10 -> StmtList()
     * f11 -> "END"
     * f12 -> ( Procedure() )*
     * f13 -> <EOF>
     */
    public MIPSFragment visit(Goal n) {
        MIPSFragment mf = new MIPSFragment();
        mf.write_text_seg();
        mf.write_globl("main");

        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);

        mf.write_label("main");
        this.inc_indent();
        mf.write_procedure_init(Integer.parseInt(n.f5.f0.tokenImage));

        mf.append(n.f10.accept(this));
        mf.write_lw(RET_ADDR, STACK_PTR, 2);
        mf.write_jr(RET_ADDR);
        mf.append("\n");
        this.dec_indent();

        n.f11.accept(this);
        mf.append(n.f12.accept(this));
        n.f13.accept(this);
        mf.write_data_part();
        return mf;
    }

    /**
     * f0 -> Label()
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> "["
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     * f7 -> "["
     * f8 -> IntegerLiteral()
     * f9 -> "]"
     * f10 -> StmtList()
     * f11 -> "END"
     */
    public MIPSFragment visit(Procedure n) {
        MIPSFragment mf = new MIPSFragment();
        mf.write_text_seg();
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);

        mf.write_label(n.f0.f0.tokenImage);
        this.inc_indent();
        mf.write_procedure_init(Integer.parseInt(n.f5.f0.tokenImage));

        mf.append(n.f10.accept(this));

        mf.write_procedure_final();
        mf.append("\n");
        this.dec_indent();

        n.f11.accept(this);
        return mf;
    }

    /**
     * f0 -> ( ( Label() )? Stmt() )*
     */
    public MIPSFragment visit(StmtList n) {
        MIPSFragment mf;
        mf = n.f0.accept(this);
        return mf;
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
     *       | ALoadStmt()
     *       | AStoreStmt()
     *       | PassArgStmt()
     *       | CallStmt()
     */
    public MIPSFragment visit(Stmt n) {
        MIPSFragment mf;
        mf = n.f0.accept(this);
        return mf;
    }

    /**
     * f0 -> "NOOP"
     */
    public MIPSFragment visit(NoOpStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        mf.write_nop();
        return mf;
    }

    /**
     * f0 -> "ERROR"
     */
    public MIPSFragment visit(ErrorStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        mf.write_print_string("error_str");
        mf.write_exit();
        return mf;
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Reg()
     * f2 -> Label()
     */
    public MIPSFragment visit(CJumpStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub1 = n.f1.accept(this);
        MIPSFragment mf_sub2 = n.f2.accept(this);

        mf.write_beqz(mf_sub1.exp, mf_sub2.exp);

        return mf;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public MIPSFragment visit(JumpStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub = n.f1.accept(this);

        mf.write_b(mf_sub.exp);

        return mf;
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Reg()
     * f2 -> IntegerLiteral()
     * f3 -> Reg()
     */
    public MIPSFragment visit(HStoreStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub1 = n.f1.accept(this);
        MIPSFragment mf_sub2 = n.f2.accept(this);
        MIPSFragment mf_sub3 = n.f3.accept(this);

        mf.write_sw(mf_sub3.exp, mf_sub1.exp, mf_sub2.exp);

        return mf;
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Reg()
     * f2 -> Reg()
     * f3 -> IntegerLiteral()
     */
    public MIPSFragment visit(HLoadStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub1 = n.f1.accept(this);
        MIPSFragment mf_sub2 = n.f2.accept(this);
        MIPSFragment mf_sub3 = n.f3.accept(this);

        mf.write_lw(mf_sub1.exp, mf_sub2.exp, mf_sub3.exp);

        return mf;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public MIPSFragment visit(PrintStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub = n.f1.accept(this);

        switch(mf_sub.exp_type) {
            case REG:
                mf.write_print_int_reg(mf_sub.exp);
                break;
            case INT:
                mf.write_print_int_int(mf_sub.exp);
                break;
            case LABEL:
                assert false : "LABEL is not allowed to print!";
                break;
        }

        return mf;
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Reg()
     * f2 -> Exp()
     */
    public MIPSFragment visit(MoveStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub1 = n.f1.accept(this);

        if(n.f2.f0.choice instanceof BinOp) {
            this._to_reg = mf_sub1.exp;
            MIPSFragment mf_sub2 = n.f2.accept(this);
            mf.append(mf_sub2);
        }
        else {
            MIPSFragment mf_sub2 = n.f2.accept(this);
            mf.append(mf_sub2);
            switch(mf_sub2.exp_type) {
                case REG:
                    mf.write_move(mf_sub1.exp, mf_sub2.exp);
                    break;
                case INT:
                    mf.write_li(mf_sub1.exp, mf_sub2.exp);
                    break;
                case LABEL:
                    mf.write_la(mf_sub1.exp, mf_sub2.exp);
                    break;
            }
        }

        return mf;
    }

    /**
     * f0 -> "ALOAD"
     * f1 -> Reg()
     * f2 -> SpilledArg()
     */
    public MIPSFragment visit(ALoadStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub = n.f1.accept(this);
        n.f2.accept(this);

        mf.write_lw(mf_sub.exp, FRAME_PTR, -Integer.parseInt(n.f2.f1.f0.tokenImage));

        return mf;
    }

    /**
     * f0 -> "ASTORE"
     * f1 -> SpilledArg()
     * f2 -> Reg()
     */
    public MIPSFragment visit(AStoreStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        MIPSFragment mf_sub = n.f2.accept(this);

        mf.write_sw(mf_sub.exp, FRAME_PTR, -Integer.parseInt(n.f1.f1.f0.tokenImage));

        return mf;
    }

    /**
     * f0 -> "PASSARG"
     * f1 -> IntegerLiteral()
     * f2 -> Reg()
     */
    public MIPSFragment visit(PassArgStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub1 = n.f1.accept(this);
        MIPSFragment mf_sub2 = n.f2.accept(this);

        mf.write_sw(mf_sub2.exp, STACK_PTR, -(Integer.parseInt(mf_sub1.exp) - 1));

        return mf;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     */
    public MIPSFragment visit(CallStmt n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub = n.f1.accept(this);

        switch(mf_sub.exp_type) {
            case REG:
                mf.write_jalr(mf_sub.exp);
                break;
            case LABEL:
                mf.write_jal(mf_sub.exp);
                break;
            case INT:
                assert false : "IntegerLiteral is not allowed here!";
                break;
        }

        return mf;
    }

    /**
     * f0 -> HAllocate()
     *       | BinOp()
     *       | SimpleExp()
     */
    public MIPSFragment visit(Exp n) {
        MIPSFragment mf;
        mf = n.f0.accept(this);
        return mf;
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public MIPSFragment visit(HAllocate n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub = n.f1.accept(this);

        switch(mf_sub.exp_type) {
            case REG:
                mf.write_move("$a0", mf_sub.exp);
                break;
            case INT:
                mf.write_li("$a0", mf_sub.exp);
                break;
            case LABEL:
                assert false : "Label is not allowed here!\n";
        }

        mf.write_syscall(9);
        mf.exp = "$v0";
        mf.exp_type = MIPSExp.REG;

        return mf;
    }

    /**
     * f0 -> Operator()
     * f1 -> Reg()
     * f2 -> SimpleExp()
     */
    public MIPSFragment visit(BinOp n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        MIPSFragment mf_sub1 = n.f1.accept(this);
        MIPSFragment mf_sub2 = n.f2.accept(this);

        switch(n.f0.f0.which) {
            /* LT */
            case 0:
                switch(mf_sub2.exp_type) {
                    case REG:
                        mf.write_slt(this._to_reg, mf_sub1.exp, mf_sub2.exp);
                        break;
                    case INT:
                        mf.write_slti(this._to_reg, mf_sub1.exp, mf_sub2.exp);
                        break;
                }
                break;
            /* PLUS */
            case 1:
                switch(mf_sub2.exp_type) {
                    case REG:
                        mf.write_add(this._to_reg, mf_sub1.exp, mf_sub2.exp);
                        break;
                    case INT:
                        mf.write_addi(this._to_reg, mf_sub1.exp, mf_sub2.exp);
                        break;
                }
                break;
            /* MINUS */
            case 2:
                switch(mf_sub2.exp_type) {
                    case REG:
                        mf.write_sub(this._to_reg, mf_sub1.exp, mf_sub2.exp);
                        break;
                    case INT:
                        mf.write_addi(this._to_reg, mf_sub1.exp, "-" + mf_sub2.exp);    // not sure
                        break;
                }
                break;
            /* TIMES */
            case 3:
                switch(mf_sub2.exp_type) {
                    case REG:
                        mf.write_mult(mf_sub1.exp, mf_sub2.exp);
                        break;
                    case INT:
                        mf.write_li("$v0", mf_sub2.exp);
                        mf.write_mult(mf_sub1.exp, "$v0");
                        break;
                }
                mf.write_mflo(this._to_reg);
                break;
        }

        return mf;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public MIPSFragment visit(Label n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        mf.exp_type = MIPSExp.LABEL;
        mf.exp = n.f0.tokenImage;
        return mf;
    }

    /**
     * f0 -> "a0"
     *       | "a1"
     *       | "a2"
     *       | "a3"
     *       | "t0"
     *       | "t1"
     *       | "t2"
     *       | "t3"
     *       | "t4"
     *       | "t5"
     *       | "t6"
     *       | "t7"
     *       | "s0"
     *       | "s1"
     *       | "s2"
     *       | "s3"
     *       | "s4"
     *       | "s5"
     *       | "s6"
     *       | "s7"
     *       | "t8"
     *       | "t9"
     *       | "v0"
     *       | "v1"
     */
    public MIPSFragment visit(Reg n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        mf.exp_type = MIPSExp.REG;
        mf.exp = "$" + n.f0.choice.toString();
        return mf;
    }

    /**
     * f0 -> Reg()
     *       | IntegerLiteral()
     *       | Label()
     */
    public MIPSFragment visit(SimpleExp n) {
        MIPSFragment mf;
        mf = n.f0.accept(this);
        return mf;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public MIPSFragment visit(IntegerLiteral n) {
        MIPSFragment mf = new MIPSFragment();
        n.f0.accept(this);
        mf.exp_type = MIPSExp.INT;
        mf.exp = n.f0.tokenImage;
        return mf;
    }

    public MIPSFragment visit(NodeListOptional n) {
        MIPSFragment mf = new MIPSFragment();
        if ( n.present() ) {
            int _count=0;
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                mf.append(e.nextElement().accept(this));
                _count++;
            }
        }
        return mf;
    }

    public MIPSFragment visit(NodeSequence n) {
        MIPSFragment mf = new MIPSFragment();
        int _count=0;
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            Node next = e.nextElement();
            MIPSFragment mf_sub = next.accept(this);
            if(next instanceof NodeOptional && ((NodeOptional) next).present()) {
                mf.write_label(mf_sub.exp);
            }
            else if(next instanceof Stmt) {
                mf.append(mf_sub);
            }
            _count++;
        }
        return mf;
    }

    public MIPSFragment visit(NodeOptional n) {
        if ( n.present() )
            return n.node.accept(this);
        else
            return new MIPSFragment();
    }
}

enum MIPSExp {
    REG, INT, LABEL, NONEXP
}
