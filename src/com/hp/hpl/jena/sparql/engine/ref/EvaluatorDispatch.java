package com.hp.hpl.jena.sparql.engine.ref;

import java.util.Stack;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.Table;
import com.hp.hpl.jena.sparql.algebra.TableFactory;
import com.hp.hpl.jena.sparql.algebra.op.*;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.ALog;

import com.hp.hpl.jena.query.QueryExecException;

/**  Class to provide type-safe eval() dispatch using the visitor support of Op */

public class EvaluatorDispatch implements OpVisitor
{
    // TODO Clean up: OpGraph, OpDatasetNames (needed?)
    
    private Stack stack = new Stack() ;
    private Evaluator evaluator ;
    
    public EvaluatorDispatch(Evaluator evaluator)
    {
        this.evaluator = evaluator ;
    }

    private Table eval(Op op)
    {
        op.visit(this) ;
        return pop() ;
    }
    
    Table getResult()
    {
        if ( stack.size() != 1 )
            ALog.warn(this, "Warning: getResult: stack size = "+stack.size()) ;
        
        Table table = pop() ;
        return table ;
    }
    
    public void visit(OpBGP opBGP)
    {
        Table table = evaluator.basicPattern(opBGP.getPattern()) ;
        push(table) ;
    }

    public void visit(OpQuadPattern quadPattern)
    {
        push(Eval.evalQuadPattern(quadPattern, evaluator)) ;
    }

    public void visit(OpProcedure opProc)
    {
        Table table = eval(opProc.getSubOp()) ;
        if ( opProc.getArgs() != null )
            table = evaluator.procedure(table, opProc.getProcId(), opProc.getArgs()) ;
        else
            table = evaluator.procedure(table, opProc.getProcId(), opProc.getSubjectArgs(), opProc.getObjectArgs()) ;
        push(table) ;
    }

    public void visit(OpJoin opJoin)
    {
        Table left = eval(opJoin.getLeft()) ;
        Table right = eval(opJoin.getRight()) ;
        Table table = evaluator.join(left, right) ;
        push(table) ;
    }
    
    public void visit(OpStage opStage)
    {
        // Evaluate as a join (reference implementation).
        Table left = eval(opStage.getLeft()) ;
        Table right = eval(opStage.getRight()) ;
        Table table = evaluator.join(left, right) ;
        push(table) ;
    }

    public void visit(OpLeftJoin opLeftJoin)
    {
        Table left = eval(opLeftJoin.getLeft()) ;
        Table right = eval(opLeftJoin.getRight()) ;
        Table table = evaluator.leftJoin(left, right, opLeftJoin.getExprs()) ;
        push(table) ;
    }

    public void visit(OpDiff opDiff)
    {
        Table left = eval(opDiff.getLeft()) ;
        Table right = eval(opDiff.getRight()) ;
        Table table = evaluator.diff(left, right) ;
        push(table) ;
    }

    public void visit(OpUnion opUnion)
    {
        Table left = eval(opUnion.getLeft()) ;
        Table right = eval(opUnion.getRight()) ;
        Table table = evaluator.union(left, right) ;
        push(table) ;
    }

    public void visit(OpFilter opFilter)
    {
        Table table = eval(opFilter.getSubOp()) ;
        table = evaluator.filter(opFilter.getExprs(), table) ;
        push(table) ;
    }

    public void visit(OpGraph opGraph)
    {
        push(Eval.evalGraph(opGraph, evaluator)) ;
    }

    public void visit(OpService opService)
    {
        QueryIterator qIter = Service.exec(opService) ;
        Table table = TableFactory.create(qIter) ;
        push(table) ;
    }

    public void visit(OpDatasetNames dsNames)
    {
        push(Eval.evalDS(dsNames, evaluator)) ;
    }

    public void visit(OpTable opTable)
    {
        push(opTable.getTable()) ;
    }

    public void visit(OpExt opExt)
    { throw new QueryExecException("Encountered OpExt during execution of reference engine") ; }

    public void visit(OpNull opNull)
    { 
        push(TableFactory.createEmpty()) ;
    }

    public void visit(OpList opList)
    {
        Table table = eval(opList.getSubOp()) ;
        table = evaluator.list(table) ;
        push(table) ;
    }

    public void visit(OpOrder opOrder)
    {
        Table table = eval(opOrder.getSubOp()) ;
        table = evaluator.order(table, opOrder.getConditions()) ;
        push(table) ;
    }

    public void visit(OpProject opProject)
    {
        Table table = eval(opProject.getSubOp()) ;
        table = evaluator.project(table, opProject.getVars()) ;
        push(table) ;
    }

    public void visit(OpDistinct opDistinct)
    {
        Table table = eval(opDistinct.getSubOp()) ;
        table = evaluator.distinct(table) ;
        push(table) ;
    }

    public void visit(OpReduced opReduced)
    {
        Table table = eval(opReduced.getSubOp()) ;
        table = evaluator.reduced(table) ;
        push(table) ;
    }

    public void visit(OpSlice opSlice)
    {
        Table table = eval(opSlice.getSubOp()) ;
        table = evaluator.slice(table, opSlice.getStart(), opSlice.getLength()) ;
        push(table) ;
    }

    public void visit(OpAssign opAssign)
    {
        Table table = eval(opAssign.getSubOp()) ;
        table = evaluator.assign(table, opAssign.getVarExprList()) ;
        push(table) ;
    }

    public void visit(OpGroupAgg opGroupAgg)
    {
        Table table = eval(opGroupAgg.getSubOp()) ;
        table = evaluator.groupBy(table, opGroupAgg.getGroupVars(), opGroupAgg.getAggregators()) ;
        push(table) ;
    }

    private void push(Table table)  { stack.push(table) ; }
    private Table pop()
    { 
        if ( stack.size() == 0 )
            ALog.warn(this, "Warning: pop: empty stack") ;
        return (Table)stack.pop() ;
    }

}