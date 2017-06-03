package piglet.piglet2spiglet;

import piglet.syntaxtree.Temp;
import piglet.visitor.DepthFirstVisitor;

/**
 * Created by jensen on 2017/6/2.
 */
public class MaxTempVisitor extends DepthFirstVisitor{
    int max_temp;
    MaxTempVisitor(){
        this.max_temp = 20;
    }
    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public void visit(Temp n) {
        n.f0.accept(this);
        n.f1.accept(this);
        this.max_temp = Integer.max(this.max_temp, Integer.parseInt(n.f1.f0.tokenImage));
    }
}
