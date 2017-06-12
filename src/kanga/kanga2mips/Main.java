package kanga.kanga2mips;

import kanga.KangaParser;
import kanga.ParseException;
import kanga.syntaxtree.Node;
import spiglet.spiglet2kanga.KangaBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 * Created by jensen on 2017/6/11.
 */
public class Main {
    public static void main(String args[]) {
        try{
            // KangaParser parser = new KangaParser(new FileInputStream("samples/kanga/TreeVisitor.kg"));
            KangaParser parser = new KangaParser(new InputStreamReader(System.in));
            Node goal = parser.Goal();
            MIPSBuilder mbuilder = new MIPSBuilder();
            MIPSBuilder.MIPSFragment mf = goal.accept(mbuilder);
            System.out.printf("%s", mf.getString());
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
