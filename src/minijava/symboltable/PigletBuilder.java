package minijava.symboltable;

import minijava.symboltable.ScopeEntry;
import minijava.symboltable.TableTravese;
import minijava.syntaxtree.*;
import minijava.visitor.GJNoArguDepthFirst;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

/**
 * Created by jensen on 2017/5/29.
 */
public class PigletBuilder extends GJNoArguDepthFirst<PigletBuilder.PigletFragment> {
    public static ScopeEntry global_entry;
    ScopeEntry cur_entry; // Used to record current state.
    ClassEntry main_class;
    int global_temp_cnt;
    int indent;
    int while_cnt;
    int if_cnt;
    Integer _SIZE_PER_UNIT;
    int _SPACES_PER_INDENT;
    int global_DTable_temp;

    public class PigletFragment{
        public StringBuilder piglet;

        PigletFragment(){
            this.piglet = new StringBuilder();
        }

        void append(PigletFragment pf_next){
            this.piglet.append(pf_next.piglet);
        }

        void append(String str_next){
            this.piglet.append(str_next);
        }

        /* Write to piglet helper starts */
        void write_indent(){
            for(int i = 0;i < PigletBuilder.this.indent * PigletBuilder.this._SPACES_PER_INDENT;i++) this.piglet.append(' ');
        }

        void write_label(String _label){
            this.write_indent();
            this.append(_label + "\n");
        }

        void write_array_assign(int arr_temp, int index, String exp){
            this.write_indent();
            this.append(PigletBuilder.this.HSTORE(TEMP(arr_temp), index + 1, exp));
        }

        int write_allocate_array(String size){
            int ret_temp = PigletBuilder.this.allocate_temp();

            this.write_indent();
            this.append(PigletBuilder.this.MOVE(ret_temp, PigletBuilder.this.HALLOCATE(PigletBuilder.this.BINOP('+', size, "1"))));

            this.write_indent();
            this.append(PigletBuilder.this.HSTORE(TEMP(ret_temp), 0, size));
            return ret_temp;
        }

        void write_begin_part(){
            this.append("\n");
            this.write_indent();
            this.append("BEGIN\n");
            PigletBuilder.this.inc_indent();
        }
        void write_end_part(String ret_exp){
            this.write_indent();
            this.append(RETURN(ret_exp));
            PigletBuilder.this.dec_indent();
            this.write_indent();
            this.append("END");
        }

        void write_procedure_head(String label, int params_num){
            this.write_indent();
            this.append(label + " [" + Integer.toString(params_num) +"]");
        }

        /* Write to piglet helper ends */

        void allocate_DTable_mem(){
            int DTable_num = PigletBuilder.global_entry.table.size() - 1;
            this.write_label("INIT");
            this.write_indent();
            this.append(NOOP());
            int DTable_temp = this.write_allocate_array(Integer.toString(DTable_num));
            PigletBuilder.this.global_DTable_temp = DTable_temp;
            int temp_v = PigletBuilder.this.allocate_temp();
            /* Write to mem */
            int dtable_id = 0;
            for(SymbolTableEntry _entry : PigletBuilder.this.global_entry.table.values()){
                ClassEntry entry = (ClassEntry)_entry;
                if(entry != PigletBuilder.this.main_class){
                    entry.DTable_list_id = dtable_id;
                    dtable_id ++;
                    this.write_indent();
                    this.append(MOVE(temp_v, HALLOCATE(Integer.toString(entry.DTable.size()))));
                    this.write_array_assign(DTable_temp, entry.DTable_list_id, TEMP(temp_v));
                    for(int i = 0;i < entry.DTable.size();i++){
                        this.write_indent();
                        this.append(HSTORE(TEMP(temp_v), i, entry.DTable.get(i).get_piglet_name()));
                    }
                }
            }
        }

        void write_allocate_local(MethodEntry entry){
            for(int i = 0;i < entry.param_list.size();i++){
                entry.param_list.get(i).temp_index = i + 1;
            }
            for(SymbolTableEntry param : entry.table.values()){
                if(((NodeEntry)param).temp_index < 0) ((NodeEntry)param).temp_index = PigletBuilder.this.allocate_temp();
            }
        }

    }
    String linebreak(String str){
        return str.endsWith("\n") ? str : str + "\n";
    }
    /* INLINE */
    String TEMP(Integer temp_id){
        return "TEMP " + temp_id.toString();
    }

    String HALLOCATE(String unit_num){
        return "HALLOCATE " + this.BINOP('*', unit_num, this._SIZE_PER_UNIT.toString());
    }
    String CALL(String exp, String parameters){
        return "CALL " + exp + " " + parameters;
    }
    String BINOP(char operator, String exp1, String exp2){
        String ret;
        switch(operator){
            case '<':
                ret = "LT " + exp1 + " " + exp2;
                break;
            case '+':
                ret = "PLUS " + exp1 + " " + exp2;
                break;
            case '-':
                ret = "MINUS " + exp1 + " " + exp2;
                break;
            case '*':
                ret = "TIMES " + exp1 + " " + exp2;
                break;
            default:
                ret = "WRONGOP " + exp1 + " " + exp2;
                break;
        }
        return ret;
    }

    String get_IFFALSE_label(int id){
        return "IFFALSE" + Integer.toString(id);
    }
    String get_IFEND_label(int id){
        return "IFEND" + Integer.toString(id);
    }
    int allocate_if(){
        this.if_cnt ++;
        return this.if_cnt - 1;
    }

    String get_WHILEEND_label(int id){
        return "WHILEEND" + Integer.toString(id);
    }
    String get_WHILESTART_label(int id){
        return "WHILESTART" + Integer.toString(id);
    }
    int allocate_while(){
        this.while_cnt ++;
        return this.while_cnt - 1;
    }

    String access_array_ele(String exp1, String exp2){  // return an expression
        PigletFragment pf = new PigletFragment();
        pf.write_begin_part();
        int ret_temp = this.allocate_temp();

        pf.write_indent();
        pf.append(this.MOVE(
                ret_temp,
                this.BINOP('+', exp2, "1")
        ));

        pf.write_indent();
        pf.append(this.MOVE(
                ret_temp,
                this.BINOP('*', TEMP(ret_temp), this._SIZE_PER_UNIT.toString())
        ));

        pf.write_indent();
        pf.append(this.HLOAD(
                ret_temp,
                this.BINOP('+', exp1, TEMP(ret_temp)),
                0
        ));

        pf.write_end_part(TEMP(ret_temp));
        return pf.piglet.toString();
    }

    String access_array_length(String exp){     // also applicable to get DTABLE ptr
        PigletFragment pf = new PigletFragment();
        pf.write_begin_part();
        int ret_temp = this.allocate_temp();

        pf.write_indent();
        pf.append(this.HLOAD(
                ret_temp,
                exp,
                0
        ));

        pf.write_end_part(TEMP(ret_temp));
        return pf.piglet.toString();
    }

    String array_ele_ptr(String exp1, String exp2){
        PigletFragment pf = new PigletFragment();
        pf.write_begin_part();
        int ret_temp = this.allocate_temp();

        pf.write_indent();
        pf.append(this.MOVE(
                ret_temp,
                this.BINOP('+', exp2, "1")
        ));

        pf.write_indent();
        pf.append(this.MOVE(
                ret_temp,
                this.BINOP('*', TEMP(ret_temp), this._SIZE_PER_UNIT.toString())
        ));

        pf.write_indent();
        pf.append(this.MOVE(
                ret_temp,
                this.BINOP('+', exp1, TEMP(ret_temp))
        ));

        pf.write_end_part(TEMP(ret_temp));
        return pf.piglet.toString();
    }

    /* LINE BREAK */
    String MOVE(Integer temp_id, String exp){
        String ret = "MOVE " + this.TEMP(temp_id) + " " + exp + "\n";
        return ret;
    }
    String HSTORE(String exp1, Integer u_offset, String exp2){
        String ret = "HSTORE " + exp1 + " " + u_offset * PigletBuilder.this._SIZE_PER_UNIT + " " + exp2 + "\n";
        return ret;
    }
    String HLOAD(Integer temp_id, String exp, Integer u_offset){
        String ret = "HLOAD " + TEMP(temp_id) + " " + exp + " " + u_offset * PigletBuilder.this._SIZE_PER_UNIT + "\n";
        return ret;
    }
    String CJUMP(String exp, String label){
        return "CJUMP " + exp + " " + label + "\n";
    }
    String JUMP(String label){
        return "JUMP " + label + "\n";
    }
    String PRINT(String exp){
        return "PRINT " + exp + "\n";
    }
    String RETURN(String exp){
        return "RETURN " + exp + "\n";
    }
    String NOOP(){
        return "NOOP\n";
    }

    void leave_cur_scope(){
        // System.out.printf("Go out %s into %s\n", cur_entry.identifier.name, cur_entry.parent_scope.identifier.name);
        cur_entry = cur_entry.parent_scope;
    }
    void enter_new_scope(ScopeEntry new_scope){
        // System.out.printf("Go into %s from %s\n", new_scope.identifier.name, cur_entry.identifier.name);
        cur_entry = new_scope;
    }


    public PigletBuilder(SymbolTable _table){
        this.global_entry = _table.global_entry;
        this.cur_entry = this.global_entry;
        this.main_class = _table.main_class;
        this.global_temp_cnt = 20;
        this.indent = 0;
        this._SPACES_PER_INDENT = 4;
        this._SIZE_PER_UNIT = 4;
        this.while_cnt = this.if_cnt = 0;
    }
    void inc_indent(){
        this.indent += 1;
    }
    void dec_indent(){
        this.indent -= 1;
    }
    int allocate_temp(){
        this.global_temp_cnt ++;
        return this.global_temp_cnt - 1;
    }

    /* Visitor Section starts */
    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public PigletFragment visit(Goal n) {
        PigletFragment pf = new PigletFragment();
        pf.append(n.f0.accept(this));
        pf.append(n.f1.accept(this));
        n.f2.accept(this);
        return pf;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public PigletFragment visit(MainClass n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        this.enter_new_scope((ScopeEntry) this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage)));
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        MethodEntry entry = (MethodEntry) this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.METHOD, "main"));
        this.enter_new_scope(entry);
        pf.write_label("MAIN");
        this.inc_indent();
        pf.allocate_DTable_mem();
        pf.write_label("REALSTART");
        pf.write_indent();
        pf.append(NOOP());
        pf.write_allocate_local(entry);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);
        n.f13.accept(this);
        n.f14.accept(this);
        pf.append(n.f15.accept(this));
        n.f16.accept(this);
        this.leave_cur_scope();
        this.dec_indent();
        pf.write_indent();
        pf.append("END");
        n.f17.accept(this);
        this.leave_cur_scope();
        return pf;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public PigletFragment visit(ClassDeclaration n) {
        PigletFragment pf;
        n.f0.accept(this);
        n.f1.accept(this);
        ClassEntry entry = (ClassEntry) this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage));
        this.enter_new_scope(entry);
        n.f2.accept(this);
        n.f3.accept(this);
        pf = n.f4.accept(this);
        n.f5.accept(this);
        this.leave_cur_scope();
        return pf;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public PigletFragment visit(ClassExtendsDeclaration n) {
        PigletFragment pf;
        n.f0.accept(this);
        n.f1.accept(this);
        ClassEntry entry = (ClassEntry) this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage));
        this.enter_new_scope(entry);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        pf = n.f6.accept(this);
        n.f7.accept(this);
        this.leave_cur_scope();
        return pf;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public PigletFragment visit(MethodDeclaration n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        MethodEntry entry = (MethodEntry)this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.METHOD, n.f2.f0.tokenImage));
        this.enter_new_scope(entry);
        pf.write_procedure_head(entry.get_piglet_name(), entry.param_list.size() + 1);  // this ptr should be passed in
        pf.write_begin_part();
        /* Allocate Local Temp */
        pf.write_allocate_local(entry);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        PigletFragment pf_sub1 = n.f8.accept(this);
        pf.append(pf_sub1);
        n.f9.accept(this);
        PigletFragment pf_sub2 = n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);

        pf.write_end_part(pf_sub2.piglet.toString());
        this.leave_cur_scope();
        return pf;
    }


    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public PigletFragment visit(Statement n) {
        PigletFragment pf;
        pf = n.f0.accept(this);
        return pf;
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public PigletFragment visit(Block n) {
        PigletFragment pf;
        n.f0.accept(this);
        pf = n.f1.accept(this);
        n.f2.accept(this);
        return pf;
    }

    public PigletFragment visit(NodeListOptional n) {
        PigletFragment pf = new PigletFragment();
        if ( n.present() ) {
            int _count = 0;
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                if (n.nodes.firstElement() instanceof Statement) {
                    PigletFragment pf_sub = e.nextElement().accept(this);
                    pf.append(pf_sub);
                }
                else if (n.nodes.firstElement() instanceof ExpressionRest) {
                    PigletFragment pf_sub = e.nextElement().accept(this);
                    pf.append(" " + pf_sub.piglet.toString());
                }
                else if (n.nodes.firstElement() instanceof MethodDeclaration || n.nodes.firstElement() instanceof TypeDeclaration) {
                    PigletFragment pf_sub = e.nextElement().accept(this);
                    pf.append("\n");
                    pf.append(pf_sub);
                }
                else e.nextElement().accept(this);
                _count++;
            }
        }
        return pf;
    }

    /**
     * f0 -> Expression()
     * f1 -> ( ExpressionRest() )*
     */
    public PigletFragment visit(ExpressionList n) {
        PigletFragment pf;
        pf = n.f0.accept(this);
        pf.append(n.f1.accept(this));
        return pf;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public PigletFragment visit(ExpressionRest n) {
        PigletFragment pf;
        n.f0.accept(this);
        pf = n.f1.accept(this);
        return pf;
    }

    public PigletFragment visit(NodeOptional n) {
        PigletFragment pf = new PigletFragment();
        if ( n.present() )
            if(n.node instanceof ExpressionList) pf.append(n.node.accept(this));
        pf.append(")");
        return pf;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public PigletFragment visit(AssignmentStatement n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub1 = n.f2.accept(this);
        n.f3.accept(this);

        /* Get variable */
        NodeEntry id_type = ((MethodEntry)this.cur_entry).get_node_by_name(n.f0.f0.tokenImage);
        if(id_type.parent_scope instanceof MethodEntry){
            pf.write_indent();
            pf.append(this.MOVE(id_type.temp_index, pf_sub1.piglet.toString()));
        }
        else if(id_type.parent_scope instanceof ClassEntry){
            pf.append(HSTORE(
                    TEMP(0),
                    id_type.table_index + 1,
                    pf_sub1.piglet.toString()
            ));
        }
        return pf;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public PigletFragment visit(ArrayAssignmentStatement n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub1 = n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        PigletFragment pf_sub2 = n.f5.accept(this);
        n.f6.accept(this);

        /* Get variable */
        NodeEntry id_type = ((MethodEntry)this.cur_entry).get_node_by_name(n.f0.f0.tokenImage);
        if(id_type.parent_scope instanceof MethodEntry){
            int ele_temp = this.allocate_temp();
            pf.write_indent();
            pf.append(HSTORE(
                    array_ele_ptr(TEMP(id_type.temp_index), pf_sub1.piglet.toString()),
                    0,
                    pf_sub2.piglet.toString()
            ));
        }
        else if(id_type.parent_scope instanceof ClassEntry){
            int arr_temp = this.allocate_temp();
            pf.append(HSTORE(
                    array_ele_ptr(access_array_ele(TEMP(0), Integer.toString(id_type.table_index)), pf_sub1.piglet.toString()),
                    0,
                    pf_sub2.piglet.toString()
            ));
        }
        return pf;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public PigletFragment visit(IfStatement n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub1 = n.f2.accept(this);
        n.f3.accept(this);
        PigletFragment pf_sub2 = n.f4.accept(this);
        n.f5.accept(this);
        PigletFragment pf_sub3 = n.f6.accept(this);

        int ifid = this.allocate_if();
        pf.write_indent();
        pf.append(CJUMP(pf_sub1.piglet.toString(), this.get_IFFALSE_label(ifid)));
        pf.append(pf_sub2);
        pf.write_indent();
        pf.append(JUMP(this.get_IFEND_label(ifid)));
        pf.write_label(this.get_IFFALSE_label(ifid));
        pf.append(pf_sub3);
        pf.write_label(this.get_IFEND_label(ifid));
        pf.write_indent();
        pf.append(NOOP());
        return pf;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public PigletFragment visit(WhileStatement n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub1 = n.f2.accept(this);
        n.f3.accept(this);
        PigletFragment pf_sub2 = n.f4.accept(this);

        int whileid = this.allocate_while();
        pf.write_label(this.get_WHILESTART_label(whileid));
        pf.write_indent();
        pf.append(CJUMP(pf_sub1.piglet.toString(), this.get_WHILEEND_label(whileid)));
        pf.append(pf_sub2);
        pf.write_indent();
        pf.append(JUMP(this.get_WHILESTART_label(whileid)));
        pf.write_label(this.get_WHILEEND_label(whileid));
        pf.write_indent();
        pf.append(NOOP());
        return pf;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public PigletFragment visit(PrintStatement n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub = n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        pf.write_indent();
        pf.append(PRINT(pf_sub.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    public PigletFragment visit(Expression n) {
        PigletFragment pf;
        pf = n.f0.accept(this);
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    public PigletFragment visit(AndExpression n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub2 = n.f2.accept(this);
        pf.append(this.BINOP('*', pf_sub1.piglet.toString(), pf_sub2.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public PigletFragment visit(CompareExpression n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub2 = n.f2.accept(this);
        pf.append(this.BINOP('<', pf_sub1.piglet.toString(), pf_sub2.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public PigletFragment visit(PlusExpression n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub2 = n.f2.accept(this);
        pf.append(BINOP('+', pf_sub1.piglet.toString(), pf_sub2.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public PigletFragment visit(MinusExpression n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub2 = n.f2.accept(this);
        pf.append(BINOP('-', pf_sub1.piglet.toString(), pf_sub2.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public PigletFragment visit(TimesExpression n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub2 = n.f2.accept(this);
        pf.append(BINOP('*', pf_sub1.piglet.toString(), pf_sub2.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public PigletFragment visit(ArrayLookup n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f1.accept(this);
        PigletFragment pf_sub2 = n.f2.accept(this);
        n.f3.accept(this);
        pf.append(access_array_ele(pf_sub1.piglet.toString(), pf_sub2.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public PigletFragment visit(ArrayLength n) {
        PigletFragment pf = new PigletFragment();
        PigletFragment pf_sub = n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        pf.append(access_array_length(pf_sub.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public PigletFragment visit(MessageSend n) {
        PigletFragment pf = new PigletFragment();
        ExpressionChecker type_detect = new ExpressionChecker((MethodEntry) this.cur_entry);
        pf.write_begin_part();

        PigletFragment pf_sub1 = n.f0.accept(this);
        n.f0.accept(type_detect);

        /* get DTable ptr */
        int d_temp = this.allocate_temp();

        pf.write_indent();
        pf.append(MOVE(
                d_temp,
                access_array_length(pf_sub1.piglet.toString())
        ));

        n.f1.accept(this);
        n.f2.accept(this);

        /* get Compile-Time type */
        assert(type_detect.type instanceof ObjectEntry);
        ClassEntry ct_type = ((ObjectEntry)type_detect.type).class_pointer;
        MethodEntry ct_method = (MethodEntry) ct_type.table.get(SymbolTableEntry.name2namecode(Type.METHOD, n.f2.f0.tokenImage));

        /* get Method ptr */
        pf.write_indent();
        pf.append(HLOAD(
                d_temp,
                TEMP(d_temp),
                ct_method.table_index
        ));


        n.f3.accept(this);
        PigletFragment pf_sub2 = n.f4.accept(this);
        pf.write_end_part(this.CALL(
                TEMP(d_temp),
                "(" + pf_sub1.piglet.toString() + " " + pf_sub2.piglet.toString()
        ));
        n.f5.accept(this);
        return pf;
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    public PigletFragment visit(PrimaryExpression n) {
        PigletFragment pf;
        if(n.f0.choice instanceof Identifier){
            pf = new PigletFragment();
            assert(this.cur_entry instanceof MethodEntry);
            NodeEntry id_type = ((MethodEntry)this.cur_entry).get_node_by_name(((Identifier)n.f0.choice).f0.tokenImage);
            if(id_type.parent_scope instanceof MethodEntry){
                pf.append(TEMP(id_type.temp_index));
            }
            else{
                pf.append(this.access_array_ele(TEMP(0), Integer.toString(id_type.table_index)));
            }
        }
        else pf = n.f0.accept(this);
        return pf;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public PigletFragment visit(IntegerLiteral n) {
        PigletFragment pf = new PigletFragment();
        pf.append(n.f0.tokenImage);
        n.f0.accept(this);

        return pf;
    }

    /**
     * f0 -> "true"
     */
    public PigletFragment visit(TrueLiteral n) {
        PigletFragment pf = new PigletFragment();
        pf.append("1");
        n.f0.accept(this);
        return pf;
    }

    /**
     * f0 -> "false"
     */
    public PigletFragment visit(FalseLiteral n) {
        PigletFragment pf = new PigletFragment();
        pf.append("0");
        n.f0.accept(this);
        return pf;
    }

    /**
     * f0 -> "this"
     */
    public PigletFragment visit(ThisExpression n) {
        PigletFragment pf = new PigletFragment();
        pf.append(TEMP(0));
        n.f0.accept(this);
        return pf;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public PigletFragment visit(ArrayAllocationExpression n) {
        PigletFragment pf = new PigletFragment();
        pf.write_begin_part();
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        PigletFragment pf_sub = n.f3.accept(this);
        int arr_temp = pf.write_allocate_array(pf_sub.piglet.toString());
        n.f4.accept(this);
        pf.write_end_part(TEMP(arr_temp));
        return pf;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public PigletFragment visit(AllocationExpression n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);

        pf.write_begin_part();
        /* Find corresponding Class */
        ClassEntry entry = (ClassEntry) this.global_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage));

        /* Allocate Table */
        int ret_temp = this.allocate_temp();

        pf.write_indent();
        pf.append(this.MOVE(
                ret_temp,
                this.HALLOCATE(this.BINOP('+', Integer.toString(entry.VTable.size()), "1"))
        ));

        pf.write_indent();
        pf.append(this.HSTORE(
                TEMP(ret_temp),
                0,
                access_array_ele(TEMP(this.global_DTable_temp), Integer.toString(entry.DTable_list_id))
        ));

        pf.write_end_part(TEMP(ret_temp));

        return pf;
    }

    /**
     * f0 -> "!"
     * f1 -> Expression()
     */
    public PigletFragment visit(NotExpression n) {
        PigletFragment pf = new PigletFragment();
        n.f0.accept(this);
        PigletFragment pf_sub = n.f1.accept(this);
        pf.append(this.BINOP('-', "1", pf_sub.piglet.toString()));
        return pf;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public PigletFragment visit(BracketExpression n) {
        PigletFragment pf;
        n.f0.accept(this);
        pf = n.f1.accept(this);
        n.f2.accept(this);
        return pf;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public PigletFragment visit(TypeDeclaration n) {
        PigletFragment pf;
        pf = n.f0.accept(this);
        return pf;
    }

    /* Visitor Section ends */

}