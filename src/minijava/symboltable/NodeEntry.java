package minijava.symboltable;

import java.util.Hashtable;

/**
 * Created by jensen on 2017/3/24.
 */
abstract class NodeEntry extends SymbolTableEntry{
    int table_index = -1;    // VTable index, only for NodeEntry whose parent_scope is a Class
    public NodeEntry(String name, Type type, ScopeEntry parent){
        super(new IdentifierPair(name, type), parent);
        // System.out.printf("Variable Define: %s\n", this);
    }
    public String toString(){
        String ret = "name: " + this.identifier.name;
        if(this.parent_scope != null) ret += ", parent scope: " + this.parent_scope.identifier.name;
        return ret;
    }

    public int identical_or_subclass_of(NodeEntry n){
        /**
         * 0: not identical
         * 1: identical, not OBJECT
         * 2: identical, is OBJECT, but not the same class or subclass of
         * 3: identical, is OBJECT, is the same class or subclass of
         */
        if(this.identifier.type != n.identifier.type) return 0;
        else if(this.identifier.type != Type.OBJECT) return 1;
        else if(!((ObjectEntry)this).same_subclass_of((ObjectEntry)n)) return 2;
        return 3;
    }
}

class ObjectEntry extends NodeEntry{
    String classname;
    ClassEntry class_pointer;
    boolean isvalid;
    public ObjectEntry(String name, ScopeEntry parent, String classname){
        super(name, Type.OBJECT, parent);
        this.classname = classname;
        this.class_pointer = null;
    }
    public ObjectEntry(String classname){
        /* Token */
        super("", Type.OBJECT, null);
        this.classname = classname;
        this.class_pointer = null;
    }
    public ObjectEntry(ClassEntry c){
        super("", Type.OBJECT, null);
        this.class_pointer = c;
        this.classname = c.identifier.name;
        this.isvalid = true;
    }

    void resolve_class(Hashtable<String, SymbolTableEntry> global_table){
        String class_namecode = SymbolTableEntry.name2namecode(Type.CLASS, this.classname);
        if(global_table.containsKey(class_namecode)){
            this.class_pointer = (ClassEntry) global_table.get(class_namecode);
            this.isvalid = true;
        }
        else{
            System.out.printf("Class %s Not Found.\n", this.classname);
            this.isvalid = false;
        }
    }

    boolean same_subclass_of(ObjectEntry o){
        if(this.isvalid && o.isvalid) return this.class_pointer.same_subclass_of(o.class_pointer);
        else return this.classname.equals(o.classname);
    }

    public String toString(){
        return super.toString() + ", type: Object";
    }
}

class IntegerEntry extends NodeEntry{
    public IntegerEntry(String name, ScopeEntry parent){
        super(name, Type.INTEGER, parent);
    }
    public IntegerEntry(){
        /* Token */
        super("", Type.INTEGER, null);
    }
    public String toString(){
        return super.toString() + ", type: Integer";
    }
}

class BooleanEntry extends NodeEntry{
    public BooleanEntry(String name, ScopeEntry parent){
        super(name, Type.BOOLEAN, parent);
    }
    public BooleanEntry(){
        /* Token */
        super("", Type.BOOLEAN, null);
    }
    public String toString(){
        return super.toString() + ", type: Boolean";
    }
}

abstract class ArrayEntry extends NodeEntry{
    public ArrayEntry(String name, Type type, ScopeEntry parent){
        super(name, type, parent);
    }
    public ArrayEntry(){
        /* Token */
        super("", Type.ARRAY, null);
    }
    public String toString(){
        return super.toString() + ", type: Array";
    }
}

class IntArrayEntry extends ArrayEntry{
    public IntArrayEntry(String name, ScopeEntry parent){
        super(name, Type.ARRAY, parent);
    }
    public IntArrayEntry(){
        /* Token */
        super("", Type.ARRAY, null);
    }
    public String toString(){
        return super.toString() + ", type: Int Array";
    }
}

class StringArrayEntry extends ArrayEntry{
    public StringArrayEntry(String name, ScopeEntry parent){
        super(name, Type.STRINGARRAY, parent);
    }
    public String toString(){
        return super.toString() + ", type: String Array";
    }
}