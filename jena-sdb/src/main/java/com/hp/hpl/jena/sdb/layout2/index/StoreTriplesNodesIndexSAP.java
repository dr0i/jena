/*
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

package com.hp.hpl.jena.sdb.layout2.index;
/* SAP contribution from Fergal Monaghan (m#)/May 2012 */
import com.hp.hpl.jena.sdb.StoreDesc;
import com.hp.hpl.jena.sdb.core.sqlnode.GenerateSQL;
import com.hp.hpl.jena.sdb.layout2.LoaderTuplesNodes;
import com.hp.hpl.jena.sdb.layout2.SQLBridgeFactory2;
import com.hp.hpl.jena.sdb.sql.SAPStorageType;
import com.hp.hpl.jena.sdb.sql.SDBConnection;

public class StoreTriplesNodesIndexSAP extends StoreBaseIndex
{
	public StoreTriplesNodesIndexSAP(SDBConnection connection, StoreDesc desc)
	{
		this(connection, desc, null);
	}
	
    public StoreTriplesNodesIndexSAP(SDBConnection connection, StoreDesc desc, SAPStorageType storageType)
    {
        super(connection, desc,
              new FmtLayout2IndexSAP(connection, 
                      (storageType!=null)? storageType : SAPStorageType.row),
              new LoaderTuplesNodes(connection, TupleLoaderIndexSAP.class),
              new QueryCompilerFactoryIndex(),
              new SQLBridgeFactory2(),
              new GenerateSQL()) ;
        
        ((LoaderTuplesNodes) this.getLoader()).setStore(this);
    }
}
