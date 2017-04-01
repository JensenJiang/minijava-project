package minijava.symboltable;
import minijava.syntaxtree.*;
import minijava.syntaxtree.Identifier;
import minijava.visitor.DepthFirstVisitor;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by jensen on 2017/3/24.
 */
public class SymbolTable extends TableTravese{
    public ClassEntry main_class;

    /* in-table check variables */
    private String circle_start = "";
    private Hashtable<String, Boolean> visit;
    private Hashtable<String, Boolean> in_stack;
    private Boolean in_circle;

    public SymbolTable(){
        global_entry = new GlobalEntry();
        cur_entry = global_entry;
    }

    NodeEntry type2token(minijava.syntaxtree.Type t){
        Node tn = t.f0.choice;
        if(tn instanceof Identifier){
            return new ObjectEntry(((Identifier)tn).f0.tokenImage);
        }
        else{
            if(tn instanceof ArrayType) return new IntArrayEntry();
            else if(tn instanceof BooleanType) return new BooleanEntry();
            else if(tn instanceof IntegerType) return new IntegerEntry();
        }
        return null;    // This should not be possible.
    }

    NodeEntry type2entry(minijava.syntaxtree.Type type, Identifier id){
        Node tn = type.f0.choice;
        NodeEntry ret = null;
        String name = id.f0.tokenImage;
        if(tn instanceof Identifier){
             ret = new ObjectEntry(name, this.cur_entry, ((Identifier)tn).f0.tokenImage);
        }
        else{
            if(tn instanceof ArrayType) ret = new IntArrayEntry(name, this.cur_entry);
            else if(tn instanceof BooleanType) ret = new BooleanEntry(name, this.cur_entry);
            else if(tn instanceof IntegerType) ret = new IntegerEntry(name, this.cur_entry);
        }
        return ret;    // This should not be possible.
    }

    /* in-table check section starts */
    public void do_in_table_check(){
        extends_resolve_all();
        resolve_objects_class();
        extends_method_resolve_all();
    }
    void extends_resolve_all(){
        visit = new Hashtable<String, Boolean>();
        in_stack = new Hashtable<String, Boolean>();
        this.global_entry.table.forEach((id, entry) -> visit.put(id, false));
        this.global_entry.table.forEach((id, entry) -> in_stack.put(id, false));
        in_circle = false;
        this.global_entry.table.forEach((id, entry) -> extends_resolve(id, (ClassEntry)entry));
    }
    void extends_resolve(String id, ClassEntry entry){
        if(this.visit.get(id)){
            if(in_stack.get(id)){
                in_circle = true;
                circle_start = id;
                System.out.printf("Cyclic Inheritence Found: %s", entry.identifier.name);
            }
            return;
        }
        else{
            this.visit.put(id, true);
            this.in_stack.put(id, true);
            if(!entry.extends_classname.equals("")){
                String class_namecode = SymbolTableEntry.name2namecode(Type.CLASS, entry.extends_classname);
                ClassEntry parent = (ClassEntry)this.global_entry.table.get(class_namecode);
                if(parent == null){
                    /* Extends Class Not Exist */
                }
                else{
                    extends_resolve(class_namecode, parent);
                    /* Not in circle */
                    if(!this.in_circle){
                        entry.extends_class = parent;
                    }
                    /* In circle */
                    else{
                        System.out.printf("<-%s", entry.identifier.name);
                        if(entry.identifier.namecode.equals(this.circle_start)){
                            System.out.println();
                            this.in_circle = false;
                            this.circle_start = "";
                        }
                    }
                }
            }
            this.in_stack.put(id, false);
        }
    }

    void extends_method_resolve_all(){
        this.global_entry.table.forEach((id, entry) -> visit.put(id, false));
        this.global_entry.table.forEach((id, entry) -> extends_method_resolve(id, (ClassEntry)entry));
    }
    void extends_method_resolve(String id, ClassEntry entry){
        if(!this.visit.get(id)){
            this.visit.put(id, true);
            if(entry.extends_class != null){
                extends_method_resolve(entry.extends_class.identifier.namecode, entry.extends_class);
                entry.resolve_extends_method();
            }
        }
    }
    void resolve_objects_class(){
        for(SymbolTableEntry entry : this.global_entry.table.values()){
            ((ClassEntry)entry).resolve_method_object(this.global_entry.table);
        }
    }
    /* in-table check section ends */

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
    public void visit(MainClass n) {
        n.f0.accept(this);
        n.f1.accept(this);
        ClassEntry _class = new ClassEntry(n.f1.f0.tokenImage, cur_entry, "");
        main_class = _class;
        this.enter_new_scope(_class);

        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        MethodEntry _main_method = new MethodEntry("main", cur_entry, null);
        this.enter_new_scope(_main_method);

        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        StringArrayEntry _string_arr = new StringArrayEntry(n.f11.f0.tokenImage, cur_entry);
        this.cur_entry.add_param(_string_arr);

        n.f12.accept(this);
        n.f13.accept(this);
        n.f14.accept(this);
        n.f15.accept(this);
        n.f16.accept(this);
        this.leave_cur_scope();

        n.f17.accept(this);
        this.leave_cur_scope();
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public void visit(VarDeclaration n) {
        n.f0.accept(this);
        n.f1.accept(this);
        type2entry(n.f0, n.f1);
        n.f2.accept(this);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public void visit(ClassDeclaration n) {
        n.f0.accept(this);
        n.f1.accept(this);
        ClassEntry _class = new ClassEntry(n.f1.f0.tokenImage, cur_entry, "");
        this.enter_new_scope(_class);

        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        this.leave_cur_scope();
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
    public void visit(ClassExtendsDeclaration n) {
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        ClassEntry _class = new ClassEntry(n.f1.f0.tokenImage, cur_entry, n.f3.f0.tokenImage);
        this.enter_new_scope(_class);

        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        this.leave_cur_scope();
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
    public void visit(MethodDeclaration n) {
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        MethodEntry _method = new MethodEntry(n.f2.f0.tokenImage, this.cur_entry, this.type2token(n.f1));
        this.enter_new_scope(_method);

        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);
        this.leave_cur_scope();
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public void visit(FormalParameter n) {
        n.f0.accept(this);
        n.f1.accept(this);
        NodeEntry param = type2entry(n.f0, n.f1);
        this.cur_entry.add_param(param);
    }
}
