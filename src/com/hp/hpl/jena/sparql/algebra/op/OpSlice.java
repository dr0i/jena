/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra.op;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.Table;
import com.hp.hpl.jena.sparql.algebra.Transform;
import com.hp.hpl.jena.sparql.engine.ref.Evaluator;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class OpSlice extends OpModifier
{
    private long start ;
    private long length ;

    public OpSlice(Op subOp, long start, long length)
    {
        super(subOp) ;
        this.start = start ;
        this.length = length ;
    }
    
    public long getLength()         { return length ; }
    public long getStart()          { return start ; } 

    public Op copy()
    {
        return null ;
    }

    public Table eval_1(Table table, Evaluator evaluator)
    {
        return evaluator.slice(table, start, length) ;
    }

//    public Op apply(Transform transform, Op subOp)
//    { return transform.transform(this, subOp) ; }

    public String getName()                 { return "slice" ; }
    public void visit(OpVisitor opVisitor)  { opVisitor.visit(this) ; }
    public Op copy(Op subOp)                { return new OpSlice(subOp, start, length) ; }

    public Op apply(Transform transform, Op subOp)
    { return transform.transform(this, subOp) ; }
    
    public int hashCode()
    {
        return getSubOp().hashCode() ^ (int)(start&0xFFFFFFFF) ^ (int)(length&0xFFFFFFFF) ;
    }

    public boolean equalTo(Op other, NodeIsomorphismMap labelMap)
    {
        if ( ! (other instanceof OpSlice) ) return false ;
        OpSlice opSlice = (OpSlice)other ;
        if ( opSlice.start != start || opSlice.length != length )
            return false;
        return getSubOp().equalTo(opSlice, labelMap) ;
    }
}

/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */