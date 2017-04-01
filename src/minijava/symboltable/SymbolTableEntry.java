package minijava.symboltable;

import minijava.syntaxtree.Identifier;
import sun.jvm.hotspot.debugger.cdbg.Sym;

/**
 * Created by jensen on 2017/3/24.
 */
enum Type{
    GLOBAL, CLASS, METHOD, INTEGER, ARRAY, OBJECT, BOOLEAN, STRINGARRAY, VARIABLE;
    public String toString(){
        if(this == GLOBAL) return "GLOBAL";
        if(this == CLASS) return "CLASS";
        if(this == METHOD) return "METHOD";
        return "VARIABLE";
    }
    public String realname(){
        if(this == GLOBAL) return "GLOBAL";
        if(this == CLASS) return "CLASS";
        if(this == METHOD) return "METHOD";
        if(this == INTEGER) return "INTEGER";
        if(this == ARRAY) return "ARRAY";
        if(this == OBJECT) return "OBJECT";
        if(this == STRINGARRAY) return "STRINGARRAY";
        return "BOOLEAN";
    }
}

class IdentifierPair{
    String name;
    Type type;
    String namecode;
    IdentifierPair(String name, Type type){
        this.name = name;
        this.type = type;
        this.namecode = type + "-" + name;
    }

}

public abstract class SymbolTableEntry {
    public IdentifierPair identifier;
    public ScopeEntry parent_scope;
    public SymbolTableEntry(){}
    public SymbolTableEntry(IdentifierPair id, ScopeEntry parent){
        this.identifier = id;
        this.parent_scope = parent;
        if(parent != null) parent.add_entry(id, this);
    }
    public static String name2namecode(Type t, String name){
        return t + "-" + name;
    }
}
