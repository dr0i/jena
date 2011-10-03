/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tx ;

import static org.junit.Assert.assertEquals ;
import static org.junit.Assert.assertFalse ;

import java.io.File ;
import java.nio.ByteBuffer ;
import java.util.Iterator ;

import org.junit.After ;
import org.junit.Before ;
import org.junit.Test ;
import org.openjena.atlas.lib.FileOps ;
import org.openjena.atlas.lib.Pair ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.shared.Lock ;
import com.hp.hpl.jena.sparql.core.DatasetGraph ;
import com.hp.hpl.jena.sparql.core.Quad ;
import com.hp.hpl.jena.tdb.DatasetGraphTxn ;
import com.hp.hpl.jena.tdb.ReadWrite ;
import com.hp.hpl.jena.tdb.StoreConnection ;
import com.hp.hpl.jena.tdb.TDB ;
import com.hp.hpl.jena.tdb.TDBFactory ;
import com.hp.hpl.jena.tdb.base.file.FileFactory ;
import com.hp.hpl.jena.tdb.base.file.Location ;
import com.hp.hpl.jena.tdb.base.objectfile.ObjectFile ;
import com.hp.hpl.jena.tdb.sys.Names ;
import com.hp.hpl.jena.vocabulary.RDFS ;

public class TestNodeTableObjectFileCorruption {

    // private static final String path = System.getProperty("java.io.tmpdir") + File.separator + "DB" ;
    private static final String path = "tmp/DB" ;
    private static Location location = new Location (path) ;
    
    @Before public void setup() {
        cleanup() ;

        FileOps.ensureDir(path) ;
        StoreConnection sc = StoreConnection.make(location) ; 
        DatasetGraphTxn dsg = sc.begin(ReadWrite.WRITE) ; 
        dsg.add(Quad.defaultGraphIRI, Node.createURI("foo:bar"), RDFS.label.asNode(), Node.createLiteral("foo")) ; 
        dsg.commit() ; 
        TDB.sync(dsg) ; 
        dsg.close() ; 
        StoreConnection.release(location) ; 
    }
    
    @After public void teardown() {
        cleanup() ;
    }

    private static void cleanup() {
        File dir = new File(path) ;
        if ( dir.exists() ) {
            FileOps.clearDirectory(path) ;
            FileOps.delete(path) ;
        }
        assertFalse ( dir.exists() ) ;
    }
    
    @Test public void test_01() {
        assertEquals (3, countRDFNodes()) ;

        StoreConnection sc = StoreConnection.make(location) ; 
        DatasetGraphTxn dsg = sc.begin(ReadWrite.WRITE) ; 
        dsg.add(Quad.defaultGraphIRI, Node.createURI("foo:bar"), RDFS.label.asNode(), Node.createLiteral("bar")) ; 
        dsg.commit() ; 
        dsg.close() ;
        StoreConnection.release(location) ; 
    
        assertEquals (4, countRDFNodes()) ;
    }


    @Test public void test_02() {
        assertEquals (3, countRDFNodes()) ;

        DatasetGraph dsg = TDBFactory.createDatasetGraph(location) ;
        Lock lock = dsg.getLock() ;
        lock.enterCriticalSection(Lock.WRITE) ;
        dsg.add(Quad.defaultGraphIRI, Node.createURI("foo:bar"), RDFS.label.asNode(), Node.createLiteral("bar")) ;
        lock.leaveCriticalSection() ;
        TDB.sync(dsg) ;
        dsg.close() ; 

        assertEquals (4, countRDFNodes()) ;
    }
    
    private int countRDFNodes() {
        ObjectFile objects = FileFactory.createObjectFileDisk( location.getPath(Names.indexId2Node, Names.extNodeData) ) ;
        int count = 0 ;
        Iterator<Pair<Long,ByteBuffer>> iter = objects.all() ; 
        while ( iter.hasNext() ) { 
            iter.next() ;
            count++ ;
        }
        objects.close() ;
        return count ;
    }
    
}

