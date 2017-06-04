package spiglet.spiglet2kanga;

import spiglet.syntaxtree.*;
import spiglet.visitor.DepthFirstVisitor;
import sun.jvm.hotspot.utilities.Interval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;

/**
 * Created by jensen on 2017/6/4.
 */
public class LinearScanner extends DepthFirstVisitor {
    public static String[] regs = {"t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "a0", "a1", "a2", "a3", "v0", "v1"};
    int stmt_num;
    Hashtable<Integer, IntervalPair> _temp_cur_interval;
    ArrayList<IntervalPair> _intervals;

    LinearScanner() {
        this.stmt_num = 0;
    }

    int allocate_stmt_num() {
        this.stmt_num ++;
        return this.stmt_num - 1;
    }

    void sort_intervals() {
        _intervals.sort(IntervalPair.LeftComparator);
    }

    /* interval helper starts */
    IntervalPair new_interval(int _L, int _tid) {
        IntervalPair interval = new IntervalPair(_L, _L, _tid);
        this._intervals.add(interval);
        this._temp_cur_interval.put(_tid, interval);
        return interval;
    }

    IntervalPair get_interval(int temp_id) {
        IntervalPair ret = this._temp_cur_interval.get(temp_id);
        if(ret == null) ret = this.new_interval(0, temp_id);
        return ret;
    }

    void update_interval_R(int temp_id, int _R) {
        IntervalPair interval = this.get_interval(temp_id);
        interval.set_second(_R);
    }

    void update_interval_L(int temp_id, int _L) {
        IntervalPair interval = this.new_interval(_L, temp_id);
    }

    /* interval helper ends */


    /* Visitor Section starts */
    /**
     * f0 -> "NOOP"
     */
    public void visit(NoOpStmt n) {
        this.allocate_stmt_num();
        n.f0.accept(this);
    }

    /**
     * f0 -> "ERROR"
     */
    public void visit(ErrorStmt n) {
        this.allocate_stmt_num();
        n.f0.accept(this);
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Temp()
     * f2 -> Label()
     */
    public void visit(CJumpStmt n) {
        int cur_stmt_num = this.allocate_stmt_num();
        n.f0.accept(this);
        n.f1.accept(this);
        this.update_interval_R(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
        n.f2.accept(this);
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public void visit(JumpStmt n) {
        int cur_stmt_num = this.allocate_stmt_num();
        n.f0.accept(this);
        n.f1.accept(this);
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Temp()
     * f2 -> IntegerLiteral()
     * f3 -> Temp()
     */
    public void visit(HStoreStmt n) {
        int cur_stmt_num = this.allocate_stmt_num();
        n.f0.accept(this);
        n.f1.accept(this);
        this.update_interval_R(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
        n.f2.accept(this);
        n.f3.accept(this);
        this.update_interval_R(Integer.parseInt(n.f3.f1.f0.tokenImage), cur_stmt_num);
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Temp()
     * f2 -> Temp()
     * f3 -> IntegerLiteral()
     */
    public void visit(HLoadStmt n) {
        int cur_stmt_num = this.allocate_stmt_num();
        n.f0.accept(this);
        n.f1.accept(this);
        this.update_interval_L(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
        n.f2.accept(this);
        this.update_interval_R(Integer.parseInt(n.f2.f1.f0.tokenImage), cur_stmt_num);
        n.f3.accept(this);
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Temp()
     * f2 -> Exp()
     */
    public void visit(MoveStmt n) {
        int cur_stmt_num = this.allocate_stmt_num();
        n.f0.accept(this);
        n.f1.accept(this);
        this.update_interval_L(Integer.parseInt(n.f1.f1.f0.tokenImage), cur_stmt_num);
        n.f2.accept(this);
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public void visit(PrintStmt n) {
        int cur_stmt_num = this.allocate_stmt_num();
        n.f0.accept(this);
        n.f1.accept(this);
        if(n.f1.f0.choice instanceof Temp) this.update_interval_R(Integer.parseInt(((Temp)n.f1.f0.choice).f1.f0.tokenImage), cur_stmt_num);
    }
}

class Pair<A, B> {
    protected A first;
    protected B second;
    public Pair(A _f, B _s) {
        this.first = _f;
        this.second = _s;
    }
    public void set_first(A _f) {
        this.first = _f;
    }
    public void set_second(B _s) {
        this.second = _s;
    }
    public A get_first() {
        return this.first;
    }
    public B get_second() {
        return this.second;
    }
}

class IntervalPair extends Pair<Integer, Integer> {
    int temp_id;
    public IntervalPair(Integer _f, Integer _s, int _tid) {
        super(_f, _s);
        this.temp_id = _tid;
    }
    public static Comparator<IntervalPair> LeftComparator = new Comparator<IntervalPair>() {
        public int compare(IntervalPair a, IntervalPair b) {
            return a.first.compareTo(b.first);
        }
    };

    public static Comparator<IntervalPair> RightComparator = new Comparator<IntervalPair>() {
        public int compare(IntervalPair a, IntervalPair b) {
            return a.second.compareTo(b.second);
        }
    };
}
