package piglet.piglet2spiglet;

import piglet.ParseException;
import piglet.PigletParser;
import piglet.syntaxtree.Node;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 * Created by jensen on 2017/6/2.
 */
public class Main {
    public static void main(String args[]){
        try{
            PigletParser parser = new PigletParser(new InputStreamReader(System.in));
            Node goal = parser.Goal();
            MaxTempVisitor v_max = new MaxTempVisitor();
            goal.accept(v_max);
            SPigletBuilder spbuilder = new SPigletBuilder(v_max.max_temp + 1);
            SPigletBuilder.SPigletFragment sf = goal.accept(spbuilder);
            System.out.printf("%s", sf.getString());
        }
        catch(ParseException e){
            System.out.println(e);
        }
    }
}
