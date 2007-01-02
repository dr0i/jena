/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sdb.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlColumn;

public class ScopeBase implements Scope
{
    Map<Var, SqlColumn> frame = new HashMap<Var, SqlColumn>() ;
    Scope parent = null ;
    
    public ScopeBase() { parent = null ; }
    
    public ScopeBase(Scope parent)
    { 
        this.parent = parent ;
    }
    
    public boolean hasColumnForVar(Var var)
    { 
        if ( frame.containsKey(var) )
            return true ;
        if ( parent != null )
            return parent.hasColumnForVar(var) ;
        return false ;
    }
        
    public Set<Var> getVars()
    {
        if ( parent == null )
            return frame.keySet() ;
        
        Set<Var> x = new HashSet<Var>() ;
        x.addAll(frame.keySet()) ;
        x.addAll(parent.getVars()) ;
        return x ;
    }
    
    public SqlColumn getColumnForVar(Var var)
    { 
        if ( frame.containsKey(var) )
            return frame.get(var) ;
        if ( parent != null )
            return parent.getColumnForVar(var) ;
        return null ;
    }
        
    public void setColumnForVar(Var var, SqlColumn column)
    { 
        // Only check the frame.
        if ( frame.containsKey(var) )
        {
            LogFactory.getLog(Scope.class).warn("Already has an alias: "+var+" => "+getColumnForVar(var)) ;
            return ;
        }
        frame.put(var, column) ;
    }
    
    @Override
    public String toString()
    {
        String str = "" ;
        String sep = "" ;
        for ( Var v : frame.keySet() )
        {
            SqlColumn c = frame.get(v) ;
            str = str + sep + v + ":"+c ;
            sep = " " ;
        }
        if ( parent != null )
            str = str + "=>" + parent.toString() ;
        return str ;
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