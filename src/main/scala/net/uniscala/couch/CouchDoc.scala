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

import java.io._

import net.uniscala.json._


object CouchDoc {
  
  import scala.language.implicitConversions
  
  /**
   * Convenient implicit conversions that can be imported to speed conversion 
   * of strings to Id and Rev objects.
   */
  object Implicits {
    
    // TODO: consider using the new Scala value class mechanism instead
    
    import scala.language.implicitConversions
    
    implicit def wrapId(id: String): CouchDoc.Id = CouchDoc.Id(id)
    
    implicit def wrapIdRev(ref: (String, String)): CouchDoc.Rev =
      CouchDoc.Rev(ref._1, ref._2)
    
    //implicit def wrapStringToRef(id: String) = Id(id)
    
    //implicit def wrapStringsToRef(id: String, rev: String) = Rev(id, rev)
  }
  
  private[couch] object Field {
    val ID                = "_id"
    val REV               = "_rev"
    val ATTACHMENTS       = "_attachments"
    val DELETED           = "_deleted"
    val REVISIONS         = "_revisions"
    val REVISIONS_INFO    = "_revs_info"
    val CONFLICTS         = "_conflicts"
    val DELETED_CONFLICTS = "_deleted_conflicts"
    val LOCAL_SEQ         = "_local_seq"
  }
  
  private[couch] object Param {
    val ID  = "id"
    val REV = "rev"
    val BATCH = "batch"
    val OK = "ok"
  }
  
  private[couch] object Id {
    val DESIGN = "_design"
    val LOCAL  = "_local"
  }
  
  /**
   * A reference to a couch document. This includes an ID and optionally the 
   * revision information to refer to a document in a database.
   */
  sealed abstract class Ref {
    def id: String
    def revOption: Option[String]
  }
  
  /**
   * A reference to a couch document by ID.
   */
  case class Id(id: String) extends Ref {
    assert(id != null && id.length > 0)
    override def revOption = None
  }
  
  /**
   * A reference to a couch document by ID and specific revision.
   */
  case class Rev(id: String, rev: String) extends Ref {
    assert(id != null && id.length > 0)
    assert(rev != null && rev.length > 0)
    lazy val revOpt = Option(rev)
    override def revOption = Some(rev)
  }
  
  /**
   * Set of revision numbers for a document. This is used by 'missing revision'
   * and 'revision diff' operations.
   */
  case class RevSet(id: String, revs: List[String])
  
  
  /**
   * Options to specify any extra fields  that should be retrieved in
   * database get operations.
   */
  sealed abstract class ExtraFieldOption(val name: String)
  
  /**
   * The set of possible extra field options - see ExtraFieldOption.
   */
  object ExtraFieldOptions {
    case object AttachementField extends ExtraFieldOption("attachments")
    case object ConflictsField extends ExtraFieldOption("conflicts")
    case object DeletedConflictsField extends ExtraFieldOption("deleted_conflicts")
    case object LocalSeqField extends ExtraFieldOption("local_seq")
    case object OpenRevsField extends ExtraFieldOption("open_revs")
    case object RevsField extends ExtraFieldOption("revs")
    case object RevsInfoField extends ExtraFieldOption("revs_info")
  }
}


private[couch] abstract sealed class CouchDocBase private[couch] (
  parent: CouchPath,
  id: String
) extends CouchSubPath(parent, id) {
  
  /**
   * Some IDs, such as those of design documents (eg. '_design/mydes'), have 
   * a special prefix with a following '/' that should not be escaped. Here 
   * we find such prefixes and return them if present.
   */
  private def specialPrefix(idStr: String): Option[String] = {
    import CouchDoc.Id._
    val parts = idStr.split('/')
    if (parts.size < 2) {
      None
    } else {
      parts.head match {
        case DESIGN => Some(DESIGN)
        case _ => None
      }
    }
  }
  
  override lazy val baseUrl = {
    import CouchDoc.Id._
    specialPrefix(id) map { prefix =>
      parent.baseUrl / prefix / id.substring(prefix.length + 1)
    } getOrElse {
      parent.baseUrl / id
    }
  }
  
  /**
   * The document revision.
   */
  def rev: String
  
  /**
   * The document as JSON.
   */
  def json: JsonObject
}


/**
 * A simplified couch document that has a smaller set of API operation
 * for a normal couch document. Example: local docs, which don't have
 * attachments.
 */
case class CouchSimpleDoc private[couch] (
  override val parent: CouchPath,
  override val id: String,
  rev: String,
  json: JsonObject
) extends CouchDocBase(parent, id)
with CouchPathSimpleOps[CouchSimpleDoc] {
  
  def newDoc(id: String, rev: String, jdoc: JsonObject) = 
    CouchSimpleDoc(parent, id, rev, jdoc)
}

/**
 * A couch document.
 */
case class CouchDoc private[couch] (
  override val parent: CouchPath,
  override val id: String,
  rev: String,
  json: JsonObject
) extends CouchDocBase(parent, id)
with CouchPathSimpleOps[CouchDoc]
with CouchPathAttachmentOps[CouchDoc] {
  
  def newDoc(id: String, rev: String, jdoc: JsonObject) = 
    CouchDoc(parent, id, rev, jdoc)
}
