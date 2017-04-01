package minijava.Test;

import java.io.*;
import java.util.*;

import minijava.syntaxtree.*;
import minijava.visitor.*;
import minijava.MiniJavaParser;

public class Main {

	public static HashMap table; // maintain status of all integer variables

	public static void main (String[] args) {

		table = new HashMap();

		try {
			MiniJavaParser parser = new MiniJavaParser(new FileInputStream("samples/Test.java"));
			Node goal = parser.Goal();

			TableBuilder builder = new TableBuilder(table);
			goal.accept(builder);

			AssignmentChecker checker = new AssignmentChecker(table);
			goal.accept(checker);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
	int main;
	Zhanglaoshi a = new Zhanglaoshi();

	//String f = a.shit();
}

class Laoshi{
	int id;
	int shit(){
		int c = (new int[12])[0];
		return 1;
	}
}

class Zhanglaoshi extends Laoshi{
	String id;
	//public String shit(){return "";}
}