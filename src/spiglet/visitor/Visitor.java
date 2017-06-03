//
// Generated by JTB 1.3.2
//

package spiglet.visitor;
import spiglet.syntaxtree.*;

/**
 * All void visitors must implement this interface.
 */

public interface Visitor {

   //
   // void Auto class visitors
   //

   public void visit(NodeList n);
   public void visit(NodeListOptional n);
   public void visit(NodeOptional n);
   public void visit(NodeSequence n);
   public void visit(NodeToken n);

   //
   // User-generated spiglet.visitor methods below
   //

   /**
    * f0 -> "MAIN"
    * f1 -> StmtList()
    * f2 -> "END"
    * f3 -> ( Procedure() )*
    * f4 -> <EOF>
    */
   public void visit(Goal n);

   /**
    * f0 -> ( ( Label() )? Stmt() )*
    */
   public void visit(StmtList n);

   /**
    * f0 -> Label()
    * f1 -> "["
    * f2 -> IntegerLiteral()
    * f3 -> "]"
    * f4 -> StmtExp()
    */
   public void visit(Procedure n);

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
   public void visit(Stmt n);

   /**
    * f0 -> "NOOP"
    */
   public void visit(NoOpStmt n);

   /**
    * f0 -> "ERROR"
    */
   public void visit(ErrorStmt n);

   /**
    * f0 -> "CJUMP"
    * f1 -> Temp()
    * f2 -> Label()
    */
   public void visit(CJumpStmt n);

   /**
    * f0 -> "JUMP"
    * f1 -> Label()
    */
   public void visit(JumpStmt n);

   /**
    * f0 -> "HSTORE"
    * f1 -> Temp()
    * f2 -> IntegerLiteral()
    * f3 -> Temp()
    */
   public void visit(HStoreStmt n);

   /**
    * f0 -> "HLOAD"
    * f1 -> Temp()
    * f2 -> Temp()
    * f3 -> IntegerLiteral()
    */
   public void visit(HLoadStmt n);

   /**
    * f0 -> "MOVE"
    * f1 -> Temp()
    * f2 -> Exp()
    */
   public void visit(MoveStmt n);

   /**
    * f0 -> "PRINT"
    * f1 -> SimpleExp()
    */
   public void visit(PrintStmt n);

   /**
    * f0 -> Call()
    *       | HAllocate()
    *       | BinOp()
    *       | SimpleExp()
    */
   public void visit(Exp n);

   /**
    * f0 -> "BEGIN"
    * f1 -> StmtList()
    * f2 -> "RETURN"
    * f3 -> SimpleExp()
    * f4 -> "END"
    */
   public void visit(StmtExp n);

   /**
    * f0 -> "CALL"
    * f1 -> SimpleExp()
    * f2 -> "("
    * f3 -> ( Temp() )*
    * f4 -> ")"
    */
   public void visit(Call n);

   /**
    * f0 -> "HALLOCATE"
    * f1 -> SimpleExp()
    */
   public void visit(HAllocate n);

   /**
    * f0 -> Operator()
    * f1 -> Temp()
    * f2 -> SimpleExp()
    */
   public void visit(BinOp n);

   /**
    * f0 -> "LT"
    *       | "PLUS"
    *       | "MINUS"
    *       | "TIMES"
    */
   public void visit(Operator n);

   /**
    * f0 -> Temp()
    *       | IntegerLiteral()
    *       | Label()
    */
   public void visit(SimpleExp n);

   /**
    * f0 -> "TEMP"
    * f1 -> IntegerLiteral()
    */
   public void visit(Temp n);

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public void visit(IntegerLiteral n);

   /**
    * f0 -> <IDENTIFIER>
    */
   public void visit(Label n);

}

