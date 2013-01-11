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

import util.Url


object CouchChangeOptions {
  
  sealed abstract class Style(val styleName: String) {
    override lazy val toString = styleName
  }
  
  object Style {
    case object ALL_DOCS extends Style(CouchDatabase.Param.Style.ALL_DOCS)
    case object MAIN_ONLY extends Style(CouchDatabase.Param.Style.MAIN_ONLY)
  }
}


/**
 * A set of options used for view requests.
 * An instance can be used in the CouchDatabase 'changes', 'longpollChanges' and
 * 'continuousChanges' methods.
 * The default set of options is the same as the defaults in the specification.
 * 
 * Different options may be obtained by chaining the 
 * methods named after the corresponding option in the specification, 
 * like this:
 * <pre>
 *   val options = CouchChangeOptions().limit(100).heartbeat(15*1000)
 * </pre>
 * An option can also be set back to its default using the similarly
 * named methods, but prefixed by 'default':
 * <pre>
 *   val options2 = options.defaultHeartbeat
 * </pre>
 *
 * See http://wiki.apache.org/couchdb/HTTP_database_API#Changes
 * for a description of the list of options.
 */
case class CouchChangeOptions(
  sinceOption: Option[Int] = None,
  limitOption: Option[Int] = None,
  descendingOption: Option[Boolean] = None,
  heartbeatOption: Option[Int] = Some(60*1000),
  timeoutOption: Option[Int] = None,
  filterOption: Option[CouchFilter] = None,
  includeDocsOption: Option[Boolean] = None,
  styleOption: Option[CouchChangeOptions.Style] = None
) {
  
  def toUrl(baseUrl: Url): Url = baseUrl & (toList:_*)
  
  def toList: List[(String, String)] = {
    import CouchDatabase.Param._
    List(
      sinceOption.map((s) =>            (SINCE, s.toString)),
      limitOption.map((l) =>            (LIMIT, l.toString)),
      descendingOption.map((d) =>       (DESCENDING, d.toString)),
      heartbeatOption.map((t) =>        (HEARTBEAT, t.toString)),
      timeoutOption.map((t) =>          (TIMEOUT, t.toString)),
      filterOption.map((f) =>           (FILTER, f.toString)),
      includeDocsOption.map((i) =>      (INCLUDE_DOCS, i.toString)),
      styleOption.map((s) =>            (STYLE, s.toString))
    ).flatten
  }
  
  def toMap: Map[String, String] = toList.toMap
  
  def since(seq: Int): CouchChangeOptions = {
    assert(seq >= 0)
    this.copy(sinceOption = Some(seq))
  }
  
  def defaultSince: CouchChangeOptions = {
    this.copy(sinceOption = None)
  }
  
  def limit(lim: Int): CouchChangeOptions = {
    assert(lim >= 0)
    this.copy(limitOption = Some(lim))
  }
  
  def defaultLimit: CouchChangeOptions = {
    this.copy(limitOption = None)
  }
  
  def descending(desc: Boolean): CouchChangeOptions = {
    this.copy(descendingOption = Some(desc))
  }
  
  def defaultDescending: CouchChangeOptions = {
    this.copy(descendingOption = None)
  }
  
  def heartbeat(t: Int): CouchChangeOptions = {
    assert(t >= 0)
    this.copy(heartbeatOption = Some(t))
  }
  
  def defaultHeartbeat: CouchChangeOptions = {
    this.copy(heartbeatOption = Some(60*1000))
  }
  
  def timeout(t: Int): CouchChangeOptions = {
    assert(t >= 0)
    this.copy(timeoutOption = Some(t))
  }
  
  def defaultTimeout: CouchChangeOptions = {
    this.copy(timeoutOption = None)
  }
  
  def filter(f: CouchFilter): CouchChangeOptions = {
    assert(f != null)
    this.copy(filterOption = Some(f))
  }
  
  def defaultFilter: CouchChangeOptions = {
    this.copy(filterOption = None)
  }
  
  def includeDocs(incl: Boolean): CouchChangeOptions = {
    this.copy(includeDocsOption = Some(incl))
  }
  
  def defaultIncludeDocs: CouchChangeOptions = {
    this.copy(includeDocsOption = None)
  }
  
  def style(s: CouchChangeOptions.Style): CouchChangeOptions = {
    assert(s != null)
    this.copy(styleOption = Some(s))
  }
  
  def defaultStyle: CouchChangeOptions = {
    this.copy(styleOption = None)
  }
}