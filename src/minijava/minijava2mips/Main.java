package minijava.minijava2mips;

import kanga.KangaParser;
import kanga.kanga2mips.MIPSBuilder;
import minijava.MiniJavaParser;
import minijava.ParseException;
import minijava.symboltable.PigletBuilder;
import minijava.symboltable.SecondPassChecker;
import minijava.symboltable.SymbolTable;
import minijava.syntaxtree.Node;
import piglet.PigletParser;
import piglet.piglet2spiglet.MaxTempVisitor;
import piglet.piglet2spiglet.SPigletBuilder;
import spiglet.SpigletParser;
import spiglet.spiglet2kanga.KangaBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by jensen on 2017/6/12.
 */
public class Main {
    public static void main(String args[]) {
        PrintStream stdout = System.out;
        ByteArrayOutputStream typecheck_err_buffer = new ByteArrayOutputStream();
        PrintStream typecheck_stream = new PrintStream(typecheck_err_buffer);
        try{
            // MiniJavaParser mparser = new MiniJavaParser(new FileInputStream("samples/minijava/BubbleSort.java"));
            /* typecheck & minijava2piglet */
            MiniJavaParser mparser = new MiniJavaParser(new InputStreamReader(System.in));

            Node mgoal = mparser.Goal();
            SymbolTable table = new SymbolTable();

            System.setOut(typecheck_stream);
            mgoal.accept(table);
            table.do_in_table_check();
            mgoal.accept(new SecondPassChecker());
            String typecheck_err = new String(typecheck_err_buffer.toByteArray(), StandardCharsets.UTF_8);

            System.setOut(stdout);
            if(!typecheck_err.isEmpty()) {
                System.out.println(typecheck_err);
                return;
            }

            table.build_vd_tables_all();
            PigletBuilder pgbuilder = new PigletBuilder(table);
            PigletBuilder.PigletFragment pf = mgoal.accept(pgbuilder);

            /* piglet2spiglet */
            PigletParser pparser = new PigletParser(new StringReader(pf.piglet.toString()));
            piglet.syntaxtree.Node pgoal = pparser.Goal();
            MaxTempVisitor v_max = new MaxTempVisitor();
            pgoal.accept(v_max);
            SPigletBuilder spbuilder = new SPigletBuilder(v_max.max_temp + 1);
            SPigletBuilder.SPigletFragment sf = pgoal.accept(spbuilder);

            /* spiglet2kanga */
            SpigletParser sparser = new SpigletParser(new StringReader(sf.getString()));
            spiglet.syntaxtree.Node spgoal = sparser.Goal();
            KangaBuilder kgbuilder = new KangaBuilder();
            KangaBuilder.KangaFragment kf = spgoal.accept(kgbuilder);

            /* kanga2mips */
            KangaParser kparser = new KangaParser(new StringReader(kf.getString()));
            kanga.syntaxtree.Node kgoal = kparser.Goal();
            MIPSBuilder mbuilder = new MIPSBuilder();
            MIPSBuilder.MIPSFragment mf = kgoal.accept(mbuilder);
            System.out.printf("%s", mf.getString());
        }
        catch(ParseException e){
            System.out.println(e);
        }
        catch(piglet.ParseException e){
            System.out.println(e);
        }
        catch (spiglet.ParseException e) {
            System.out.println(e);
        }
        catch (kanga.ParseException e) {
            System.out.println(e);
        }
        /*
        catch(FileNotFoundException e){
            System.out.println(e);
        }
        */
    }
}
