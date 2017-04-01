package minijava.Test;

import java.util.*;

import minijava.syntaxtree.*;
import minijava.visitor.*;

public class TableBuilder extends DepthFirstVisitor {

	public HashMap table;

	public TableBuilder(HashMap t) { table = t; }

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	public void visit(VarDeclaration n) {
		n.f0.accept(this);
		n.f1.accept(this);
		n.f2.accept(this);

		if (n.f0.f0.choice instanceof IntegerType) {
			String name = n.f1.f0.tokenImage;
			table.put(name, new IntVar(name));
		}
	}

}
