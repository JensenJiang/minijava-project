package spiglet.spiglet2kanga;

import spiglet.ParseException;
import spiglet.syntaxtree.Node;
import spiglet.SpigletParser;

import java.io.InputStreamReader;

/**
 * Created by jensen on 2017/6/3.
 */
public class Main {
    public static void main(String args[]) {
        try{
            SpigletParser parser = new SpigletParser(new InputStreamReader(System.in));
            Node goal = parser.Goal();
            KangaBuilder kgbuilder = new KangaBuilder();
            KangaBuilder.KangaFragment kf = goal.accept(kgbuilder);
            System.out.printf("%s", kf.getString());
        }
        catch(ParseException e){
            System.out.println(e);
        }
    }
}
