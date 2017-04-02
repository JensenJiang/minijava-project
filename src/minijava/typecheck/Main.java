package minijava.typecheck;

import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.symboltable.ExpressionChecker;
import minijava.symboltable.SecondPassChecker;
import minijava.symboltable.SymbolTable;
import minijava.syntaxtree.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by jensen on 2017/3/19.
 */
public class Main {
    public static void main(String[] args){
        try{
            MiniJavaParser parser = new MiniJavaParser(new FileInputStream("samples/typecheck/TreeVisitor-error.java"));
            Node goal = parser.Goal();

            SymbolTable table = new SymbolTable();
            goal.accept(table);
            table.do_in_table_check();
            goal.accept(new SecondPassChecker());
            // table.global_entry.print_table();
        }
        catch(ParseException e){
            System.out.println(e);
        }
        catch(FileNotFoundException e){
            System.out.println(e);
        }
    }
    static void test(){

    }
}
