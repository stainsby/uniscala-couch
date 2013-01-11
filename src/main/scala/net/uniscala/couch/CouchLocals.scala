/**
 * This file is part of the Uniscala Couch project.
 * Copyright (C) 2012 Sustainable Software Pty Ltd.
 * This is open source software, licensed under the Apache License
 * version 2.0 license - please see the LICENSE file included in
 * the distribution.
 *
 * Authors:
 * Sam Stainsby (sam@sustainablesoftware.com.au)
 */
package net.uniscala.couch

import net.uniscala.json.JsonObject


/**
 * Represents a base for resources found under "/_local/" in a database.
 */
class CouchLocals private[couch] (val database: CouchDatabase)
extends CouchSubPath(database, CouchDatabase.Path.LOCAL)
with CouchPathContainerOps[CouchSimpleDoc] {
  
  override def newDoc(id: String, rev: String, jdoc: JsonObject) =
    CouchSimpleDoc(database, id, rev, jdoc)
}
