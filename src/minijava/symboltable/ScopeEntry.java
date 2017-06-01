package minijava.symboltable;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jensen on 2017/3/24.
 */
public abstract class ScopeEntry extends SymbolTableEntry{
    public Hashtable<String, SymbolTableEntry> table;

    public ScopeEntry(String name, Type type, ScopeEntry parent){
        super(new IdentifierPair(name, type), parent);
        this.table = new Hashtable<String, SymbolTableEntry>();
    }
    public void add_entry(IdentifierPair id, SymbolTableEntry entry){
        if(this.table.containsKey(id.namecode)){
            System.out.printf("Error: In Scope %s, Symbol %s already exists!\n", this.identifier.name, entry.identifier.name);
        }
        else{
            this.table.put(id.namecode, entry);
        }
    }
    public void print_table(){
        System.out.printf("Table %s, size: %d\n", this.identifier.name, this.table.size());
        this.table.forEach((id, entry) -> System.out.printf("%s %s\n", id, entry.identifier.name));
    }
    public void add_param(NodeEntry param){/* only for MethodEntry */}
}

/* This class is only for the most outside -- global scope "GLOBAL" */
class GlobalEntry extends ScopeEntry{
    public GlobalEntry(){
        super("GLOBAL", Type.GLOBAL, null);    // "GLOBAL" is only served as a name, but not a valid identifier.
    }
}

class ClassEntry extends ScopeEntry{
    String extends_classname;   // Used to resolve extends_class in-table. If empty string is given, it means it doesn't have a base class.
    ClassEntry extends_class;
    ArrayList<NodeEntry> VTable;
    ArrayList<MethodEntry> DTable;
    int DTable_list_id;
    public ClassEntry(String name, ScopeEntry parent, String extends_name){
        super(name, Type.CLASS, parent);
        this.extends_classname = extends_name;
        this.extends_class = null;
    }
    public void resolve_extends_method(){
        if(extends_class == null) return;
        for(Map.Entry<String, SymbolTableEntry> entry : extends_class.table.entrySet()){
            if(this.table.containsKey(entry.getKey())){
                if(entry.getValue() instanceof MethodEntry){
                    MethodEntry this_method = (MethodEntry)this.table.get(entry.getKey());
                    MethodEntry parent_method = (MethodEntry)entry.getValue();
                    /* Override */
                    if(this_method.same_param_list_as(parent_method)){
                        if(this_method.return_type.identifier.type != parent_method.return_type.identifier.type){
                            System.out.printf("Overrided Method %s in Class %s: %s is wanted but %s is given.\n", this_method.identifier.name, this.identifier.name, parent_method.return_type.identifier.type.realname(), this_method.return_type.identifier.type.realname());
                        }
                        /* Return Type is Object */
                        else if(this_method.return_type instanceof ObjectEntry && !((ObjectEntry)this_method.return_type).same_subclass_of((ObjectEntry)parent_method.return_type)){
                            System.out.printf("Overrided Method %s in Class %s: %s is not identical to or a subclass of %s.\n", this_method.identifier.name, this.identifier.name, ((ObjectEntry) this_method.return_type).classname, ((ObjectEntry) parent_method.return_type).classname);
                        }
                        else{
                            /* Successfully overrided */
                            this_method.is_overrided = true;
                        }
                    }
                    /* Overload */
                    else{
                        this.table.put(entry.getKey(), entry.getValue());
                        System.out.printf("Method %s in Class %s is overloaded: it has been defined in parent %s.\n", this_method.identifier.name, this.identifier.name, extends_class.identifier.name);
                    }
                }
            }
            else this.table.put(entry.getKey(), entry.getValue());  // Inherented method/variable is referenced to the original place. Not sure if it'll bring something intractable.
        }
    }
    void resolve_method_object(Hashtable<String, SymbolTableEntry> global_table){
        for(SymbolTableEntry entry : this.table.values()){
            if(entry instanceof MethodEntry) ((MethodEntry)entry).resolve_object_class(global_table);
            else if(entry instanceof ObjectEntry) ((ObjectEntry) entry).resolve_class(global_table);
        }
    }
    boolean same_subclass_of(ClassEntry c){
        if(this == c) return true;
        if(this.extends_class != null) return this.extends_class.same_subclass_of(c);
        return false;
    }
}

class MethodEntry extends ScopeEntry{
    boolean is_overrided = false;
    int table_index = -1;    // DTable index
    NodeEntry return_type;  // store a NodeEntry Token */
    ArrayList<NodeEntry> param_list;
    public MethodEntry(String name, ScopeEntry parent, NodeEntry return_type){
        super(name, Type.METHOD, parent);
        this.return_type = return_type;
        this.param_list = new ArrayList<NodeEntry>();
    }
    void resolve_object_class(Hashtable<String, SymbolTableEntry> global_table){    // Including return_type.
        for(SymbolTableEntry entry : this.table.values()){
            if(entry instanceof ObjectEntry){
                ObjectEntry obj_entry = (ObjectEntry)entry;
                obj_entry.resolve_class(global_table);
            }
        }
        if(return_type instanceof ObjectEntry) ((ObjectEntry)return_type).resolve_class(global_table);
    }
    Boolean same_param_list_as(MethodEntry m){
        if(this.param_list.size() != m.param_list.size()) return false;
        for(int i = 0;i < m.param_list.size(); i++){
            Type t1 = this.param_list.get(i).identifier.type;
            Type t2 = m.param_list.get(i).identifier.type;
            if(t1 != t2) return false;
            if(t1 == Type.OBJECT && ((ObjectEntry)this.param_list.get(i)).classname != ((ObjectEntry)m.param_list.get(i)).classname) return false;
        }
        return true;
    }
    NodeEntry get_node_by_name(String name){
        String namecode = SymbolTableEntry.name2namecode(Type.VARIABLE, name);
        SymbolTableEntry ret = this.table.get(namecode);
        if(ret == null) ret = this.parent_scope.table.get(namecode);
        return (NodeEntry)ret;
    }
    boolean call_params_check(ArrayList<NodeEntry> call_params){
        boolean flag = true;
        if(this.param_list.size() != call_params.size()){
            flag = false;
            System.out.printf("Message Send: Method %s requires %d parameters, but %d is given.\n", this.identifier.name, this.param_list.size(), call_params.size());
        }
        else for(int i = 0;i < this.param_list.size();i++){
            NodeEntry p1 = this.param_list.get(i);
            NodeEntry p2 = call_params.get(i);
            if(p2 == null){
                flag = false;
                continue;
            }
            else {
                int rid = p2.identical_or_subclass_of(p1);
                if (rid == 0) {
                    System.out.printf("Message Send: Method %s's parameter %d requires %s, but %s is given.\n", this.identifier.name, i + 1, p1.identifier.type.realname(), p2.identifier.type.realname());
                    flag = false;
                }
                else if (rid == 2) {
                    System.out.printf("Message Send: Method %s's parameter %d requires %s, but the given parameter Class %s is not identical to or subclass of it.\n", this.identifier.name, i + 1, ((ObjectEntry) p1).classname, ((ObjectEntry) p2).classname);
                    flag = false;
                }
            }
        }
        return flag;
    }
    public void add_param(NodeEntry param){
        this.param_list.add(param);
    }
    public String get_piglet_name(){
        return this.parent_scope.identifier.name + "_" + this.identifier.name;
    }
}