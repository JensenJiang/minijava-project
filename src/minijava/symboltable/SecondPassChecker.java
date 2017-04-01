package minijava.symboltable;

import minijava.syntaxtree.*;

/**
 * Created by jensen on 2017/4/1.
 */
public class SecondPassChecker extends TableTravese{
    public SecondPassChecker(){
        this.global_entry = SymbolTable.global_entry;
        this.cur_entry = this.global_entry;
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
    public void visit(MainClass n) {
        n.f0.accept(this);
        n.f1.accept(this);
        this.enter_new_scope((ScopeEntry)this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage)));
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        this.enter_new_scope((ScopeEntry)this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.METHOD, "main")));
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
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
        this.enter_new_scope((ScopeEntry)this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage)));
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
        this.enter_new_scope((ScopeEntry)this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.CLASS, n.f1.f0.tokenImage)));
        n.f2.accept(this);
        n.f3.accept(this);
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
        this.enter_new_scope((ScopeEntry)this.cur_entry.table.get(SymbolTableEntry.name2namecode(Type.METHOD, n.f2.f0.tokenImage)));
        ExpressionChecker checker = new ExpressionChecker((MethodEntry)this.cur_entry);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(checker);
        n.f11.accept(this);
        n.f12.accept(this);
        if(checker.is_valid){
            int rid = checker.type.identical_or_subclass_of(((MethodEntry) this.cur_entry).return_type);
            if(rid == 0) System.out.printf("Return: In Scope %s, %s value is required, but %s is given.\n", this.cur_entry.identifier.name, ((MethodEntry) this.cur_entry).return_type.identifier.type.realname(), checker.type.identifier.type.realname());
            else if(rid == 2) System.out.printf("Return: In Scope %s, %s value is required, but %s is not identical to or subclass of it.\n", this.cur_entry.identifier.name, ((ObjectEntry)((MethodEntry) this.cur_entry).return_type).classname, ((ObjectEntry)checker.type).classname);
        }
        this.leave_cur_scope();
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public void visit(AssignmentStatement n) {
        ExpressionChecker checker = new ExpressionChecker((MethodEntry)this.cur_entry);
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(checker);
        n.f3.accept(this);
        NodeEntry left = ((MethodEntry) this.cur_entry).get_node_by_name(n.f0.f0.tokenImage);
        if(left == null) System.out.printf("In Scope %s, Symbol %s cannot be resolved.\n", this.cur_entry.identifier.name, n.f0.f0.tokenImage);
        else if(checker.is_valid){
            int rid = checker.type.identical_or_subclass_of(left);
            if(rid == 0) System.out.printf("Assignment Statement: In Scope %s, %s value is required, but %s is given.\n", this.cur_entry.identifier.name, left.identifier.type.realname(), checker.type.identifier.type.realname());
            else if(rid == 2) System.out.printf("Assignment Statement: In Scope %s, %s value is required, but %s is not identical to or subclass of it.\n", this.cur_entry.identifier.name, ((ObjectEntry)left).classname, ((ObjectEntry)checker.type).classname);
        }
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
    public void visit(ArrayAssignmentStatement n) {
        ExpressionChecker checker1 = new ExpressionChecker((MethodEntry)this.cur_entry);
        ExpressionChecker checker2 = new ExpressionChecker((MethodEntry)this.cur_entry);
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(checker1);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(checker2);
        n.f6.accept(this);
        NodeEntry left = ((MethodEntry) this.cur_entry).get_node_by_name(n.f0.f0.tokenImage);
        if(left == null) System.out.printf("In Scope %s, Symbol %s cannot be resolved.\n", this.cur_entry.identifier.name, n.f0.f0.tokenImage);
        else if(left.identifier.type == Type.STRINGARRAY){
            System.out.printf("STRINGARRAY is not implemented.\n");
        }
        if(checker1.is_valid && checker1.type.identifier.type != Type.INTEGER) System.out.printf("Array Assignment Statement: In Scope %s, INTEGER array index is required, but %s is given.\n", this.cur_entry.identifier.name, checker1.type.identifier.type.realname());
        /* Not sure if it should always be INTEGER now */
        if(checker2.is_valid && checker2.type.identifier.type != Type.INTEGER) System.out.printf("Array Assignment Statement: In Scope %s, INTEGER value is required, but %s is given.\n", this.cur_entry.identifier.name, checker2.type.identifier.type.realname());
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
    public void visit(IfStatement n) {
        ExpressionChecker checker = new ExpressionChecker((MethodEntry)this.cur_entry);
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(checker);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        if(checker.is_valid && checker.type.identifier.type != Type.BOOLEAN) System.out.printf("If Statement: In Scope %s, condition should be BOOLEAN, but %s is given.\n", this.cur_entry.identifier.name, checker.type.identifier.type.realname());
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public void visit(WhileStatement n) {
        ExpressionChecker checker = new ExpressionChecker((MethodEntry)this.cur_entry);
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(checker);
        n.f3.accept(this);
        n.f4.accept(this);
        if(checker.is_valid && checker.type.identifier.type != Type.BOOLEAN) System.out.printf("While Statement: In Scope %s, condition should be BOOLEAN, but %s is given.\n", this.cur_entry.identifier.name, checker.type.identifier.type.realname());
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public void visit(PrintStatement n) {
        ExpressionChecker checker = new ExpressionChecker((MethodEntry)this.cur_entry);
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(checker);
        n.f3.accept(this);
        n.f4.accept(this);
        if(checker.is_valid && checker.type.identifier.type != Type.INTEGER) System.out.printf("Print Statement: In Scope %s, printed content should be INTEGER, but %s is given.\n", this.cur_entry.identifier.name, checker.type.identifier.type.realname());
    }
}
