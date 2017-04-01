package minijava.symboltable;

import minijava.syntaxtree.*;
import minijava.visitor.DepthFirstVisitor;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by jensen on 2017/4/1.
 */
public class ExpressionChecker extends DepthFirstVisitor {
    boolean is_valid;
    NodeEntry type;
    MethodEntry cur_scope;
    ExpressionChecker(MethodEntry cur){
        this.cur_scope = cur;
        this.is_valid = false;
    }
    void wrong_exp(){
        this.is_valid = false;
        this.type = null;
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
    public void visit(Expression n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(subchecker);
        this.is_valid = subchecker.is_valid;
        this.type = subchecker.type;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    public void visit(AndExpression n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionChecker sub2 = new ExpressionChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(sub2);
        if(sub1.is_valid && sub2.is_valid && sub1.type.identifier.type == Type.BOOLEAN && sub2.type.identifier.type == Type.BOOLEAN){
            this.is_valid = true;
            this.type = new BooleanEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(sub1.is_valid && sub2.is_valid) System.out.printf("And Expression: \"BOOLEAN && BOOLEAN\" is wanted, but \"%s && %s\" is given.\n", sub1.type.identifier.type.realname(), sub2.type.identifier.type.realname());
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public void visit(CompareExpression n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionChecker sub2 = new ExpressionChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(sub2);
        if(sub1.is_valid && sub2.is_valid && sub1.type.identifier.type == Type.INTEGER && sub2.type.identifier.type == Type.INTEGER){
            this.is_valid = true;
            this.type = new BooleanEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(sub1.is_valid && sub2.is_valid) System.out.printf("Compare Expression: \"INTEGER < INTEGER\" is wanted, but \"%s < %s\" is given.\n", sub1.type.identifier.type.realname(), sub2.type.identifier.type.realname());
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public void visit(PlusExpression n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionChecker sub2 = new ExpressionChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(sub2);
        if(sub1.is_valid && sub2.is_valid && sub1.type.identifier.type == Type.INTEGER && sub2.type.identifier.type == Type.INTEGER){
            this.is_valid = true;
            this.type = new IntegerEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(sub1.is_valid && sub2.is_valid) System.out.printf("Plus Expression: \"INTEGER + INTEGER\" is wanted, but \"%s + %s\" is given.\n", sub1.type.identifier.type.realname(), sub2.type.identifier.type.realname());
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public void visit(MinusExpression n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionChecker sub2 = new ExpressionChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(sub2);
        if(sub1.is_valid && sub2.is_valid && sub1.type.identifier.type == Type.INTEGER && sub2.type.identifier.type == Type.INTEGER){
            this.is_valid = true;
            this.type = new IntegerEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(sub1.is_valid && sub2.is_valid) System.out.printf("Minus Expression: \"INTEGER - INTEGER\" is wanted, but \"%s - %s\" is given.\n", sub1.type.identifier.type.realname(), sub2.type.identifier.type.realname());
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public void visit(TimesExpression n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionChecker sub2 = new ExpressionChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(sub2);
        if(sub1.is_valid && sub2.is_valid && sub1.type.identifier.type == Type.INTEGER && sub2.type.identifier.type == Type.INTEGER){
            this.is_valid = true;
            this.type = new IntegerEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(sub1.is_valid && sub2.is_valid) System.out.printf("Times Expression: \"INTEGER * INTEGER\" is wanted, but \"%s * %s\" is given.\n", sub1.type.identifier.type.realname(), sub2.type.identifier.type.realname());
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public void visit(ArrayLookup n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionChecker sub2 = new ExpressionChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(sub2);
        n.f3.accept(this);
        if(sub1.is_valid && sub2.is_valid && sub1.type.identifier.type == Type.ARRAY && sub2.type.identifier.type == Type.INTEGER){
            this.is_valid = true;
            this.type = new IntegerEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(sub1.is_valid && sub2.is_valid){
                if(sub1.type.identifier.type == Type.STRINGARRAY && sub2.type.identifier.type == Type.INTEGER) System.out.printf("String Array is not implemented.\n");
                else System.out.printf("Array Lookup Expression: \"ARRAY[INTEGER]\" is wanted, but \"%s[%s]\" is given.\n", sub1.type.identifier.type.realname(), sub2.type.identifier.type.realname());
            }
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public void visit(ArrayLength n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(subchecker);
        n.f1.accept(this);
        n.f2.accept(this);
        if(subchecker.is_valid && subchecker.type instanceof ArrayEntry){
            this.is_valid = true;
            this.type = new IntegerEntry();
        }
        else{
            this.is_valid = false;
            this.type = null;
            if(subchecker.is_valid) System.out.printf("Array Length Expression: \"ARRAY.length\" or \"STRINGARRAY.length\" is wanted, but \"%s.length\" is given.\n", subchecker.type.identifier.type.realname());
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public void visit(MessageSend n) {
        ExpressionChecker sub1 = new ExpressionChecker(this.cur_scope);
        ExpressionListChecker sub2 = new ExpressionListChecker(this.cur_scope);
        n.f0.accept(sub1);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(sub2);
        n.f5.accept(this);
        if(sub1.is_valid && sub1.type.identifier.type == Type.OBJECT){
            ObjectEntry obj = (ObjectEntry)sub1.type;
            if(obj.isvalid){
                MethodEntry m = (MethodEntry)obj.class_pointer.table.get(SymbolTableEntry.name2namecode(Type.METHOD, n.f2.f0.tokenImage));
                if(m == null){
                    this.wrong_exp();
                    System.out.printf("Message Send: Method %s is not found in Class %s.\n", n.f2.f0.tokenImage, obj.classname);
                }
                /* Valid Method */
                else{
                    this.is_valid = true;
                    this.type = m.return_type;
                    m.call_params_check(sub2.type);
                }
            }
            else this.wrong_exp();
        }
        else if(sub1.is_valid){
            this.wrong_exp();
            System.out.printf("Message Send: OBJECT is wanted but %s is given.\n", sub1.type.identifier.type.realname());
        }
        else this.wrong_exp();
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
    public void visit(PrimaryExpression n) {
        if(n.f0.choice instanceof Identifier){
            NodeEntry id_type = this.cur_scope.get_node_by_name(((Identifier)n.f0.choice).f0.tokenImage);
            if(id_type == null){
                System.out.printf("In Scope %s, Symbol %s cannot be resolved.\n", this.cur_scope.identifier.name, ((Identifier)n.f0.choice).f0.tokenImage);
                this.is_valid = false;
                this.type = null;
            }
            else{
                this.is_valid = true;
                this.type = id_type;
            }
        }
        else{
            ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
            n.f0.accept(subchecker);
            this.is_valid = subchecker.is_valid;
            this.type = subchecker.type;
        }
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public void visit(IntegerLiteral n) {
        n.f0.accept(this);
        this.is_valid = true;
        this.type = new IntegerEntry();
    }

    /**
     * f0 -> "true"
     */
    public void visit(TrueLiteral n) {
        n.f0.accept(this);
        this.is_valid = true;
        this.type = new BooleanEntry();
    }

    /**
     * f0 -> "false"
     */
    public void visit(FalseLiteral n) {
        n.f0.accept(this);
        this.is_valid = true;
        this.type = new BooleanEntry();
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public void visit(Identifier n) {
        /* Different semantics could reach, so do not make any decisions here. */
        n.f0.accept(this);
    }

    /**
     * f0 -> "this"
     */
    public void visit(ThisExpression n) {
        n.f0.accept(this);
        this.is_valid = true;
        this.type = new ObjectEntry((ClassEntry) this.cur_scope.parent_scope);
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public void visit(ArrayAllocationExpression n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(subchecker);
        n.f4.accept(this);
        if(subchecker.is_valid){
            if(subchecker.type.identifier.type == Type.INTEGER){
                this.is_valid = true;
                this.type = new IntArrayEntry();
            }
            else{
                System.out.printf("Array Allocation: INTEGER is wanted but %s is given.\n", subchecker.type.identifier.type.realname());
                this.wrong_exp();
            }
        }
        else this.wrong_exp();
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public void visit(AllocationExpression n) {
        n.f0.accept(this);
        n.f1.accept(this);
        ObjectEntry obj_token = new ObjectEntry(n.f1.f0.tokenImage);
        obj_token.resolve_class(SecondPassChecker.global_entry.table);
        this.is_valid = true;
        this.type = obj_token;
        n.f2.accept(this);
        n.f3.accept(this);

    }

    /**
     * f0 -> "!"
     * f1 -> Expression()
     */
    public void visit(NotExpression n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(this);
        n.f1.accept(subchecker);
        if(!subchecker.is_valid) this.wrong_exp();
        else if (subchecker.type.identifier.type != Type.BOOLEAN){
            this.wrong_exp();
            System.out.printf("Not Expression: BOOLEAN is wanted, but %s is given.\n", subchecker.type.identifier.type.realname());
        }
        else{
            this.is_valid = true;
            this.type = new BooleanEntry();
        }
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public void visit(BracketExpression n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(this);
        n.f1.accept(subchecker);
        n.f2.accept(this);
        this.is_valid = subchecker.is_valid;
        this.type = subchecker.type;
    }
}

class ExpressionListChecker extends DepthFirstVisitor{
    ArrayList<Boolean> is_valid;
    ArrayList<NodeEntry> type;
    MethodEntry cur_scope;

    ExpressionListChecker(MethodEntry m){
        this.cur_scope = m;
        this.is_valid = new ArrayList<Boolean>();
        this.type = new ArrayList<NodeEntry>();
    }
    /**
     * f0 -> Expression()
     * f1 -> ( ExpressionRest() )*
     */
    public void visit(ExpressionList n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(subchecker);
        this.is_valid.add(subchecker.is_valid);
        this.type.add(subchecker.type);
        n.f1.accept(this);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public void visit(ExpressionRest n) {
        ExpressionChecker subchecker = new ExpressionChecker(this.cur_scope);
        n.f0.accept(this);
        n.f1.accept(subchecker);
        this.is_valid.add(subchecker.is_valid);
        this.type.add(subchecker.type);
    }
}
