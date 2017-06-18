package minijava.minijava2piglet;

import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.symboltable.PigletBuilder;
import minijava.symboltable.SecondPassChecker;
import minijava.symboltable.SymbolTable;
import minijava.syntaxtree.Node;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 * Created by jensen on 2017/5/15.
 */
public class Main {
    public static void main(String[] args){
        try{
            // MiniJavaParser parser = new MiniJavaParser(new FileInputStream("samples/minijava2piglet/TreeVisitor.java"));
            MiniJavaParser parser = new MiniJavaParser(new InputStreamReader(System.in));
            Node goal = parser.Goal();
            SymbolTable table = new SymbolTable();
            goal.accept(table);
            table.do_in_table_check();
            table.build_vd_tables_all();
            PigletBuilder pgbuilder = new PigletBuilder(table);
            PigletBuilder.PigletFragment pf = goal.accept(pgbuilder);
            System.out.println(pf.piglet.toString());
            // table.print_vd_tables();
        }
        catch(ParseException e){
            System.out.println(e);
        }
        /*
        catch(FileNotFoundException e){
            System.out.println(e);
        }
        */
    }
}

