package minijava.symboltable;

import minijava.visitor.DepthFirstVisitor;

/**
 * Created by jensen on 2017/3/30.
 */
public class TableTravese extends DepthFirstVisitor {
    public static ScopeEntry global_entry;
    ScopeEntry cur_entry; // Used to record current state.

    void leave_cur_scope(){
        // System.out.printf("Go out %s into %s\n", cur_entry.identifier.name, cur_entry.parent_scope.identifier.name);
        cur_entry = cur_entry.parent_scope;
    }
    void enter_new_scope(ScopeEntry new_scope){
        // System.out.printf("Go into %s from %s\n", new_scope.identifier.name, cur_entry.identifier.name);
        cur_entry = new_scope;
    }
}
