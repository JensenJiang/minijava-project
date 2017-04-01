package minijava.Test;

import java.io.*;
import java.util.*;

import minijava.syntaxtree.*;
import minijava.visitor.*;

public class AssignmentChecker extends DepthFirstVisitor {

	public HashMap table;

	public AssignmentChecker(HashMap t) { table = t; }

	   /**
	    * f0 -> Identifier()
	    * f1 -> "="
	    * f2 -> Expression()
	    * f3 -> ";"
	    */
	   public void visit(AssignmentStatement n) {
	      	n.f0.accept(this);
		String name = n.f0.f0.tokenImage;
		if (!table.containsKey(name)) {
			System.out.println("undefined integer var:" + name);
		}
	   }

}
