/*
 * (c) Copyright 2006, 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.pgraph.assembler;


import com.hp.hpl.jena.assembler.Assembler;
import com.hp.hpl.jena.assembler.assemblers.AssemblerGroup;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDB;

public class PGraphAssemblerVocab
{
    private static final String NS = TDB.namespace ;
    
    public static String getURI() { return NS ; } 

    // Types
    public static final Resource PGraphType         = Vocab.type(NS, "GraphBTree") ;
    public static final Resource PGraphBDBType      = Vocab.type(NS, "GraphBDB") ;

    public static final Property pLocation          = Vocab.property(NS, "location") ;
    
    public static final Property pIndex             = Vocab.property(NS, "index") ;
    public static final Property pNodes             = Vocab.property(NS, "nodes") ;
    
    public static final Property pDescription       = Vocab.property(getURI(), "description") ;
    public static final Property pFile              = Vocab.property(getURI(), "file") ;

    
    // java properties.
    
    private static boolean initialized = false ; 
    
    static { init() ; }
    
    static public void init()
    {
        if ( initialized )
            return ;
        register(Assembler.general) ;
        initialized = true ;
    }
    
    static public void register(AssemblerGroup g)
    {
        // Wire in the extension assemblers (extensions relative to the Jena assembler framework)

        assemblerClass(g, PGraphType,            new PGraphAssembler()) ;
        assemblerClass(g, PGraphBDBType,         new PGraphAssembler()) ;
    }
    
    private static void assemblerClass(AssemblerGroup g, Resource r, Assembler a)
    {
        if ( g == null )
            g = Assembler.general ;
        g.implementWith(r, a) ;
        //**assemblerAssertions.add(r, RDFS.subClassOf, JA.Object) ;
    }
}

/*
 * (c) Copyright 2006, 2007, 2008 Hewlett-Packard Development Company, LP
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