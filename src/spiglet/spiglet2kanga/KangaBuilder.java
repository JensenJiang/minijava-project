package spiglet.spiglet2kanga;

import spiglet.syntaxtree.*;
import spiglet.visitor.GJNoArguDepthFirst;

import java.util.Enumeration;

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

        KangaFragment() {
            this.kanga = new StringBuilder();
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

        void write_main_begin() {
            this.write_label("MAIN");
            inc_indent();
        }

        void write_main_end() {
            dec_indent();
            this.write_label("END");
        }

        void write_move(int reg_id, String exp) {
            this.write_indent();
            this.append("MOVE " + LinearScanner.regs[reg_id] + " " + exp + "\n");
        }

        void write_astore(int stack_id, int reg_id) {
            this.write_indent();
            this.append("ASTORE " + SPILLEDARG(stack_id) + " " + REG(reg_id) + "\n");
        }

        void write_aload(int reg_id, int stack_id) {
            this.write_indent();
            this.append("ALOAD " + REG(reg_id) + " " + SPILLEDARG(stack_id) + "\n");
        }

        /* kanga writer section ends */
    }

    /* inline */
    static String SPILLEDARG(int stack_id) {
        return "SPILLEDARG " + Integer.toString(stack_id);
    }

    static String REG(int reg_id) {
        return LinearScanner.regs[reg_id];
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
        kf.write_main_begin();
        this.reset_stmt_num();

        n.f1.accept(this._cur_scanner);   // Build flow graph and compute live intervals.
        n.f1.accept(this);

        n.f2.accept(this);
        kf.write_main_end();
        n.f3.accept(this);
        n.f4.accept(this);
        return kf;
    }

    /**
     * f0 -> ( ( Label() )? Stmt() )*
     */
    public KangaFragment visit(StmtList n) {
        KangaFragment kf;
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
                if(next instanceof NodeSequence) {
                    int cur_stmt_num = this.allocate_stmt_num();
                    for(Node ele : ((NodeSequence) next).nodes) {
                        if(ele instanceof Label) {
                            this._cur_scanner.enter_block(cur_stmt_num);
                            next_kf = ele.accept(this);
                        }
                        else if(ele instanceof CJumpStmt || ele instanceof JumpStmt) {
                            next_kf = ele.accept(this);
                            this._cur_scanner.enter_block(cur_stmt_num + 1);
                        }
                        else {
                            next_kf = ele.accept(this);
                        }
                        kf.append(next_kf);
                    }
                }

                _count++;
            }
        }
        return kf;
    }
    /* Visitor Section ends */
}
