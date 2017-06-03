package piglet.piglet2spiglet;

import piglet.syntaxtree.*;
import piglet.visitor.GJNoArguDepthFirst;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by jensen on 2017/6/2.
 */
public class SPigletBuilder extends GJNoArguDepthFirst<SPigletBuilder.SPigletFragment> {
    int global_temp_cnt;
    int indent;
    int _SPACES_PER_INDENT;

    SPigletBuilder(int _start_temp) {
        this.global_temp_cnt = _start_temp;
        this.indent = 0;
        this._SPACES_PER_INDENT = 4;
    }

    public class SPigletFragment {
        StringBuilder spiglet;
        ArrayList<String> exps;
        boolean is_simexp;
        int temp_id;

        SPigletFragment() {
            this.spiglet = new StringBuilder();
            this.is_simexp = false;
            this.temp_id = -1;
            this.exps = new ArrayList<String>();
        }

        SPigletFragment append(SPigletFragment sf) {
            this.spiglet.append(sf.getString());
            return this;
        }

        SPigletFragment append(String s) {
            this.spiglet.append(s);
            return this;
        }

        public String getString() {
            return this.spiglet.toString();
        }

        void copyExps(SPigletFragment sf) {
            for(String exp : sf.exps) {
                this.addExp(exp);
            }
            this.is_simexp = sf.is_simexp;
        }

        String getFirstExp(){
            return exps.get(0);
        }

        void addExp(String exp) {
            this.exps.add(exp);
        }

        void setSimExp() {
            this.is_simexp = true;
        }

        void setTemp(int temp_id) {
            this.temp_id = temp_id;
        }

        /* spiglet writer section starts */
        void write_indent() {
            for(int i = 0;i < indent * _SPACES_PER_INDENT;i++) this.append(" ");
        }

        void write_label(String label) {
            this.write_indent();
            this.append(label + "\n");
        }

        void write_main_begin() {
            this.write_label("MAIN");
            inc_indent();
        }

        void write_main_end() {
            dec_indent();
            this.write_label("END");
        }

        void write_noop() {
            this.write_indent();
            this.append("NOOP\n");
        }

        void write_error() {
            this.write_indent();
            this.append("ERROR\n");
        }

        void write_cjump(int temp_id, String label) {
            this.write_indent();
            this.append("CJUMP " + TEMP(temp_id) + " " + label + "\n");
        }

        void write_jump(String label) {
            this.write_indent();
            this.append("JUMP " + label + "\n");
        }

        void write_hstore(int exp_temp1, String intexp, int exp_temp2) {
            this.write_indent();
            this.append("HSTORE " + TEMP(exp_temp1) + " " + intexp + " " + TEMP(exp_temp2) + "\n");
        }

        void write_hload(int exp_temp1, int exp_temp2, String intexp) {
            this.write_indent();
            this.append("HLOAD " + TEMP(exp_temp1) + " " + TEMP(exp_temp2) + " " + intexp + "\n");
        }

        void write_move(int temp_id, String exp) {
            this.write_indent();
            this.append("MOVE " + TEMP(temp_id) + " " + exp + "\n");
        }

        void write_print(String simexp) {
            this.write_indent();
            this.append("PRINT " + simexp + "\n");
        }

        void write_procedure_begin(String label, int params_num) {
            this.write_indent();
            this.append(label + " [" + Integer.toString(params_num) + "]\n");
            this.append("BEGIN\n");
            inc_indent();
        }

        void write_procedure_end(String simexp) {
            this.write_indent();
            this.append("RETURN " + simexp + "\n");
            dec_indent();
            this.write_indent();
            this.append("END\n");
        }

        String write_wrap_simexp(SPigletFragment sub) {
            if(sub.is_simexp) return sub.getFirstExp();
            else{
                int temp_sub = allocate_temp();
                this.write_move(temp_sub, sub.getFirstExp());
                return TEMP(temp_sub);
            }
        }

        int write_wrap_temp(SPigletFragment sub) {
            if(sub.temp_id >= 0) return sub.temp_id;
            else{
                int temp_sub = allocate_temp();
                this.write_move(temp_sub, sub.getFirstExp());
                return temp_sub;
            }
        }

        /* spiglet writer section ends */
    }

    /* inline */
    String TEMP(int temp_id) {
        return "TEMP " + Integer.toString(temp_id);
    }
    String CALL(String simexp, String params) {
        return "CALL " + simexp + " " + params;
    }
    String HALLOCATE(String simexp) {
        return "HALLOCATE " + simexp;
    }
    String BINOP(String op, int temp_id, String simexp) {
        return op + " " + TEMP(temp_id) + " " + simexp;
    }

    /* state helper */
    void inc_indent(){
        this.indent += 1;
    }
    void dec_indent(){
        this.indent -= 1;
    }
    int allocate_temp() {
        this.global_temp_cnt ++;
        return this.global_temp_cnt - 1;
    }

    /* Visitor Section Starts */
    /**
     * f0 -> "MAIN"
     * f1 -> StmtList()
     * f2 -> "END"
     * f3 -> ( Procedure() )*
     * f4 -> <EOF>
     */
    public SPigletFragment visit(Goal n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        sf.write_main_begin();

        SPigletFragment sf_sub1 = n.f1.accept(this);
        sf.append(sf_sub1);
        n.f2.accept(this);
        sf.write_main_end();
        sf.append("\n");

        SPigletFragment sf_sub2 = n.f3.accept(this);
        sf.append(sf_sub2);
        n.f4.accept(this);
        return sf;
    }

    /**
     * f0 -> ( ( Label() )? Stmt() )*
     */
    public SPigletFragment visit(StmtList n) {
        SPigletFragment sf;
        sf = n.f0.accept(this);
        return sf;
    }

    /**
     * f0 -> Label()
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> StmtExp()
     */
    public SPigletFragment visit(Procedure n) {
        SPigletFragment sf = new SPigletFragment();
        SPigletFragment sf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        sf.write_procedure_begin(sf_sub1.getFirstExp(), Integer.parseInt(n.f2.f0.tokenImage));

        SPigletFragment sf_sub2 = n.f4.accept(this);
        sf.append(sf_sub2);
        String exp_sub2 = sf.write_wrap_simexp(sf_sub2);
        sf.write_procedure_end(exp_sub2);

        return sf;
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
    public SPigletFragment visit(Stmt n) {
        SPigletFragment sf;
        sf = n.f0.accept(this);
        return sf;
    }

    /**
     * f0 -> "NOOP"
     */
    public SPigletFragment visit(NoOpStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        sf.write_noop();
        return sf;
    }

    /**
     * f0 -> "ERROR"
     */
    public SPigletFragment visit(ErrorStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        sf.write_error();
        return sf;
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Exp()
     * f2 -> Label()
     */
    public SPigletFragment visit(CJumpStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        SPigletFragment sf_sub2 = n.f2.accept(this);

        sf.append(sf_sub1);
        sf.append(sf_sub2);
        int sub1_temp = sf.write_wrap_temp(sf_sub1);
        sf.write_cjump(sub1_temp, sf_sub2.getFirstExp());
        return sf;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public SPigletFragment visit(JumpStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        sf.write_jump(n.f1.f0.tokenImage);
        return sf;
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Exp()
     * f2 -> IntegerLiteral()
     * f3 -> Exp()
     */
    public SPigletFragment visit(HStoreStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        n.f2.accept(this);
        SPigletFragment sf_sub2 = n.f3.accept(this);

        sf.append(sf_sub1);
        int sub1_temp = sf.write_wrap_temp(sf_sub1);

        sf.append(sf_sub2);
        int sub2_temp = sf.write_wrap_temp(sf_sub2);

        sf.write_hstore(sub1_temp, n.f2.f0.tokenImage, sub2_temp);
        return sf;
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Temp()
     * f2 -> Exp()
     * f3 -> IntegerLiteral()
     */
    public SPigletFragment visit(HLoadStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        SPigletFragment sf_sub2 = n.f2.accept(this);
        n.f3.accept(this);

        sf.append(sf_sub1);
        sf.append(sf_sub2);
        int temp_sub2 = sf.write_wrap_temp(sf_sub2);

        sf.write_hload(sf_sub1.temp_id, temp_sub2, n.f3.f0.tokenImage);

        return sf;
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Temp()
     * f2 -> Exp()
     */
    public SPigletFragment visit(MoveStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        SPigletFragment sf_sub2 = n.f2.accept(this);

        sf.append(sf_sub1);
        sf.append(sf_sub2);

        sf.write_move(sf_sub1.temp_id, sf_sub2.getFirstExp());

        return sf;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> Exp()
     */
    public SPigletFragment visit(PrintStmt n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub = n.f1.accept(this);

        sf.append(sf_sub);
        String exp_sub = sf.write_wrap_simexp(sf_sub);

        sf.write_print(exp_sub);

        return sf;
    }

    /**
     * f0 -> StmtExp()
     *       | Call()
     *       | HAllocate()
     *       | BinOp()
     *       | Temp()
     *       | IntegerLiteral()
     *       | Label()
     */
    public SPigletFragment visit(Exp n) {
        SPigletFragment sf;
        sf = n.f0.accept(this);
        return sf;
    }

    /**
     * f0 -> "BEGIN"
     * f1 -> StmtList()
     * f2 -> "RETURN"
     * f3 -> Exp()
     * f4 -> "END"
     */
    public SPigletFragment visit(StmtExp n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        n.f2.accept(this);
        SPigletFragment sf_sub2 = n.f3.accept(this);
        n.f4.accept(this);

        sf.append(sf_sub1);
        sf.append(sf_sub2);

        sf.copyExps(sf_sub2);   // This is somewhat special, we don't check simpleExp here.
        return sf;
    }

    /**
     * f0 -> "CALL"
     * f1 -> Exp()
     * f2 -> "("
     * f3 -> ( Exp() )*
     * f4 -> ")"
     */
    public SPigletFragment visit(Call n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        n.f2.accept(this);
        SPigletFragment sf_sub2 = n.f3.accept(this);
        n.f4.accept(this);

        sf.append(sf_sub1);
        String sub1_exp = sf.write_wrap_simexp(sf_sub1);

        sf.append(sf_sub2);
        StringBuilder params = new StringBuilder();
        params.append("(");
        for(String exp : sf_sub2.exps){
            int cur_temp = this.allocate_temp();
            sf.write_move(cur_temp, exp);
            params.append(TEMP(cur_temp) + " ");
        }
        params.append(")");

        sf.addExp(CALL(sub1_exp, params.toString()));

        return sf;
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> Exp()
     */
    public SPigletFragment visit(HAllocate n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub = n.f1.accept(this);
        sf.append(sf_sub);

        String sub_exp = sf.write_wrap_simexp(sf_sub);

        sf.addExp(HALLOCATE(sub_exp));
        return sf;
    }

    /**
     * f0 -> Operator()
     * f1 -> Exp()
     * f2 -> Exp()
     */
    public SPigletFragment visit(BinOp n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        SPigletFragment sf_sub1 = n.f1.accept(this);
        SPigletFragment sf_sub2 = n.f2.accept(this);

        int temp_sub1 = this.allocate_temp();
        sf.append(sf_sub1);
        sf.write_move(temp_sub1, sf_sub1.getFirstExp());

        sf.append(sf_sub2);
        String sub2_exp = sf.write_wrap_simexp(sf_sub2);


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

        sf.addExp(BINOP(op, temp_sub1, sub2_exp));

        return sf;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public SPigletFragment visit(Label n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        sf.addExp(n.f0.tokenImage);
        sf.setSimExp();
        return sf;
    }

    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public SPigletFragment visit(Temp n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        sf.addExp(TEMP(Integer.parseInt(n.f1.f0.tokenImage)));
        sf.setSimExp();
        sf.setTemp(Integer.parseInt(n.f1.f0.tokenImage));
        return sf;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public SPigletFragment visit(IntegerLiteral n) {
        SPigletFragment sf = new SPigletFragment();
        n.f0.accept(this);

        sf.addExp(n.f0.tokenImage);
        sf.setSimExp();
        return sf;
    }

    public SPigletFragment visit(NodeListOptional n) {
        SPigletFragment sf = new SPigletFragment();
        if ( n.present() ) {
            int _count=0;
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                Node next = e.nextElement();
                // System.out.println(next);
                SPigletFragment sf_sub = next.accept(this);
                if(next instanceof NodeSequence){
                    for(Node ele : ((NodeSequence)next).nodes){
                        SPigletFragment sf_ele = ele.accept(this);
                        if(ele instanceof NodeOptional){
                            try {
                                sf.write_label(sf_ele.getFirstExp());
                            }
                            catch(IndexOutOfBoundsException _e){

                            }
                        }
                        else if(ele instanceof Stmt){
                            sf.append(sf_ele);
                        }
                    }
                }
                else if(next instanceof Exp){
                    sf.append(sf_sub);
                    sf.copyExps(sf_sub);
                }
                else if(next instanceof Procedure){
                    sf.append("\n");
                    sf.append(sf_sub);
                }
                _count++;
            }
        }
        return sf;
    }

    public SPigletFragment visit(NodeOptional n) {
        SPigletFragment sf;
        if ( n.present() )
            sf = n.node.accept(this);
        else sf = new SPigletFragment();
        return sf;
    }

    /* Visitor Section Ends */
}
