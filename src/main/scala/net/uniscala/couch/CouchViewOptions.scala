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

import net.uniscala.json._

import util.Url


object CouchViewOptions {
  
  import CouchView.Param._
  
  sealed abstract class Stale(val value: String) {
    override lazy val toString = value
  }
  
  object Stale {
    case object OK extends Stale(STALE_OK)
    case object UPDATE_AFTER extends Stale(STALE_UPDATE_AFTER)
  }
    
}

/**
 * A set of options used for view requests.
 * The default set of options is the same as the defaults in the specification.
 * 
 * Different options may be obtained by chaining the 
 * methods named after the corresponding option in the specification, 
 * like this:
 * <pre>
 *   val options = CouchViewOptions().limit(100).groupLevel(3)
 * </pre>
 * An option can also be set back to its default using the similarly
 * named methods, but prefixed by 'default':
 * <pre>
 *   val options2 = options.defaultLimit
 * </pre>
 *
 * See http://wiki.apache.org/couchdb/HTTP_view_API
 * for a description of the list of options.
 */
case class CouchViewOptions(
  keyOption: Option[JsonValue[_]] = None,
  keysOption: Option[JsonArray] = None,
  startKeyOption: Option[JsonValue[_]] = None,
  startKeyDocIdOption: Option[String] = None,
  endKeyOption: Option[JsonValue[_]] = None,
  endKeyDocIdOption: Option[String] = None,
  limitOption: Option[Int] = None,
  staleOption: Option[CouchViewOptions.Stale] = None,
  descendingOption: Option[Boolean] = None,
  skipOption: Option[Long] = None,
  groupOption: Option[Boolean] = None,
  groupLevelOption: Option[Int] = None,
  reduceOption: Option[Boolean] = None,
  includeDocsOption: Option[Boolean] = None,
  inclusiveEndOption: Option[Boolean] = None,
  updateSeqOption: Option[Boolean] = None
) {
  
  def toUrl(baseUrl: Url): Url = baseUrl & (toList:_*)
  
  def toList: List[(String, String)] = {
    import CouchView.Param._
    List(
      keyOption.map((k) =>              (KEY, k.toCompactString)),
      keysOption.map((ks) =>            (KEYS, ks.toCompactString)),
      startKeyOption.map((k) =>         (START_KEY, k.toCompactString)),
      startKeyDocIdOption.map((id) =>   (START_KEY_DOC_ID, id)),
      endKeyOption.map((k) =>           (END_KEY, k.toCompactString)),
      endKeyDocIdOption.map((id) =>     (END_KEY_DOC_ID, id)),
      limitOption.map((l) =>            (LIMIT, l.toString)),
      staleOption.map((st) =>           (STALE, st.toString)),
      descendingOption.map((d) =>       (DESCENDING, d.toString)),
      skipOption.map((n) =>             (SKIP, n.toString)),
      groupOption.map((g) =>            (GROUP, g.toString)),
      groupLevelOption.map((l) =>       (GROUP_LEVEL, l.toString)),
      reduceOption.map((s) =>           (REDUCE, s.toString)),
      includeDocsOption.map((i) =>      (INCLUDE_DOCS, i.toString)),
      inclusiveEndOption.map((b) =>     (INCLUSIVE_END, b.toString)),
      updateSeqOption.map((u) =>        (UPDATE_SEQ, u.toString))
    ).flatten
  }
  
  def toMap: Map[String, String] = toList.toMap
  
  def key(k: JsonValue[_]): CouchViewOptions = {
    assert(k != null, "null key")
    this.copy(keyOption = Some(k))
  }
  
  def defaultKey(): CouchViewOptions =
    this.copy(keyOption = None)
  
  def keys(ks: JsonArray): CouchViewOptions = {
    assert(ks != null, "null keys")
    this.copy(keysOption = Some(ks))
  }
  
  def defaultKeys(): CouchViewOptions =
    this.copy(keysOption = None)
  
  def startKey(key: JsonValue[_]): CouchViewOptions =
    this.copy(startKeyOption = Some(key))
  
  def defaultStartKey(): CouchViewOptions =
    this.copy(startKeyOption = None)
  
  def startKeyDocId(id: String): CouchViewOptions = {
    assert(id != null, "null id")
    this.copy(startKeyDocIdOption = Some(id))
  }
  
  def defaultStartKeyDocId(): CouchViewOptions =
    this.copy(startKeyDocIdOption = None)
  
  def endKey(key: JsonValue[_]): CouchViewOptions =
    this.copy(endKeyOption = Some(key))
  
  def defaultEndKey(): CouchViewOptions =
    this.copy(endKeyOption = None)
  
  def endKeyDocId(id: String): CouchViewOptions = {
    assert(id != null, "null id")
    this.copy(endKeyDocIdOption = Some(id))
  }
  
  def defaultEndKeyDocId(): CouchViewOptions =
    this.copy(endKeyDocIdOption = None)
  
  def limit(lim: Int): CouchViewOptions = {
    assert(lim >= 0)
    this.copy(limitOption = Some(lim))
  }
  
  def defaultLimit(): CouchViewOptions =
    this.copy(limitOption = None)
  
  def stale(st: CouchViewOptions.Stale): CouchViewOptions =
    this.copy(staleOption = Some(st))
  
  def defaultStale(): CouchViewOptions =
    this.copy(staleOption = None)
  
  def descending(desc: Boolean): CouchViewOptions =
    this.copy(descendingOption = Some(desc))
  
  def defaultDescending(): CouchViewOptions =
    this.copy(descendingOption = None)
  
  def skip(n: Int): CouchViewOptions = {
    assert(n >= 0)
    this.copy(skipOption = Some(n))
  }
  
  def defaultSkip(): CouchViewOptions =
    this.copy(skipOption = None)
  
  def group(grp: Boolean): CouchViewOptions =
    this.copy(groupOption = Some(grp))
  
  def defaultGroup(): CouchViewOptions =
    this.copy(groupOption = None)
  
  def groupLevel(lev: Int): CouchViewOptions = {
    assert(lev >= 0)
    this.copy(groupLevelOption = Some(lev))
  }
  
  def defaultGroupLevel(): CouchViewOptions =
    this.copy(groupLevelOption = None)
  
  def reduce(red: Boolean): CouchViewOptions =
    this.copy(reduceOption = Some(red))
  
  def defaultReduce(): CouchViewOptions =
    this.copy(reduceOption = None)
  
  def includeDocs(incl: Boolean): CouchViewOptions =
    this.copy(includeDocsOption = Some(incl))
  
  def defaultIncludeDocs(): CouchViewOptions =
    this.copy(includeDocsOption = None)
  
  def inclusiveEnd(incl: Boolean): CouchViewOptions =
    this.copy(inclusiveEndOption = Some(incl))
  
  def defaultInclusiveEnd(): CouchViewOptions =
    this.copy(inclusiveEndOption = None)
  
  def updateSeq(incl: Boolean): CouchViewOptions =
    this.copy(updateSeqOption = Some(incl))
  
  def defaultUpdateSeqSeq(): CouchViewOptions =
    this.copy(updateSeqOption = None)
}
