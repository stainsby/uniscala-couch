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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil.UTF_8

import net.uniscala.json._

import Couch._
import CouchDoc._

import util.Http


private[couch] object CouchDatabase {
  
  private[couch] object Field {
    val CHANGES = "changes"
    val LAST_SEQ = "last_seq"
    val RESULTS = "results"
  }
  
  private[couch] object Path {
    val ALL_DOCS = "_all_docs"
    val BULK_DOCS = "_bulk_docs"
    val CHANGES = "_changes"
    val COMPACT = "_compact"
    val DESIGN = "_design"
    val ENSURE_FULL_COMMIT = "_ensure_full_commit"
    val LIST = "_list"
    val LOCAL = "_local"
    val MISSING_REVS = "_missing_revs"
    val PURGE = "_purge"
    val REV_LIMIT = "_revs_limit"
    val REVS_DIFF = "_revs_diff"
    val SHOW = "_show"
    val TEMP_VIEW = "_temp_view"
    val VIEW = "_view"
    val UPDATE = "_update"
    val VIEW_CLEANUP = "_view_cleanup"
  }
  
  private[couch] object Param {
    
    val DOCS = "docs"
    val SINCE = "since"
    val LIMIT = "limit"
    val DESCENDING = "descending"
    val FEED = "feed"
    val HEARTBEAT = "heartbeat"
    val TIMEOUT = "timeout"
    val FILTER = "filter"
    val INCLUDE_DOCS = "include_docs"
    val STYLE = "style"
    
    object Feed {
      val NORMAL = "normal"
      val LONG_POLL = "longpoll"
      val CONTINUOUS = "continuous"
    }
    
    object Style {
      val ALL_DOCS = "all_docs"
      val MAIN_ONLY = "main_only"
    }
  }
  
  private[couch] object BulkDocs {
    val ALL_OR_NOTHING = "all_or_nothing"
  }
}


/**
 * Represents a couch database.
 */
class CouchDatabase(client: CouchClient, name: String)
extends CouchSubPath(client, name) with CouchPathContainerOps[CouchDoc] {
  
  import ExecutionContext.Implicits.global
  
  import Couch._
  import CouchDoc._
  import CouchDatabase.Field._
  import CouchDatabase.Path._
  
  /**
   * Instantiates a path representing a container for everything under 
   * "_design" (i.e. for design docs).
   */
  lazy val designs = new CouchDesigns(this)
  
  /**
   * Instantiates a path representing a container for everything under 
   * "_local" (i.e. for local docs).
   */
  lazy val locals = new CouchLocals(this)
  
  /**
   * Instantiates a path representing a couch design as a path/container 
   * (see CouchPath).
   */
  def design(designName: String): CouchDesign = {
    assert(designName != null && designName.length > 0)
    new CouchDesign(designs, designName)
  }
  
  override def newDoc(id: String, rev: String, jdoc: JsonObject) =
    CouchDoc(this, id, rev, jdoc)
  
  private[couch] def ref(id: String): Id = Id(id)
  
  private[couch] def ref(id: String, rev: String): Rev = Rev(id, rev)
  
  private[couch] def doc(id: String, rev: String, value: JsonObject): CouchDoc =
    CouchDoc(this, id, rev, value)
  
  private[couch] def doc(ref: Rev, value: JsonObject): CouchDoc =
    doc(ref.id, ref.rev, value)
  
  private def revSetsToJson(revSets: RevSet*): JsonObject = {
    val objectEntries = revSets map { revSet =>
      val arrayElems: Seq[JsonString] = revSet.revs.map(JsonString(_))
      val revsArray: JsonArray = Json(arrayElems:_*)
      (revSet.id, revsArray)
    }
    Json(objectEntries:_*)
  }
  
  override def toString() =
    classOf[CouchDatabase].getSimpleName + "[" + baseUrl + "]"
  
  
  //    ======================== API METHODS ========================
  
  
  /**
   * API METHOD: gets info/metadata about the database.
   */
  def info(): Future[JsonObject] = fetchJsonObject(prepareGet())
  
  /**
   * API METHOD: ensures the database is fully committed.
   */
  def ensureFullCommit(): Future[Unit] =
    fetchNothing(preparePost(ENSURE_FULL_COMMIT))
  
  /**
   * API METHOD: performs a bulk documents operation.
   */
  def bulkDocs(
    docs: Seq[(Option[Ref], JsonObject)],
    allOrNothing: Boolean = false
  ): Future[Seq[Try[Rev]]] = {
    
    // NOTE: there is a 'new_edits' mode - not sure if it would ever be used 
    // in a normal application, so we don't support it
    
    import Json._
    
    val docsEntries: Seq[JsonObject] = docs map { doc =>
      import CouchDoc.Field.{ID, REV}
      doc match {
        case (None, value) => value
        case (Some(ref: Id), value) => value :+ (ID -> ref.id)
        case (Some(ref: Rev), value) => value :+ (ID -> ref.id, REV -> ref.rev)
      }
    }
    var docsJson = Json(
      CouchDatabase.Param.DOCS -> Json(docsEntries:_*)
    )
    if (allOrNothing) {
      docsJson = docsJson :+ (CouchDatabase.BulkDocs.ALL_OR_NOTHING -> true)
    }
    val data = docsJson.toCompactString.getBytes(UTF_8)
    var req = preparePost(CouchDatabase.Path.BULK_DOCS)
    req.setContent(Unpooled.wrappedBuffer(data))
    req.setHeader(Http.Header.CONTENT_LENGTH, data.size)
    
    fetchJsonArray(req) map { jarr =>
      jarr map { jval =>
        jval match {
          case jobj: JsonObject if jobj.get("error").isDefined => {
            Failure(toCouchFailure(jobj))
          }
          case jobj: JsonObject => {
            import CouchDoc.Param._
            def get(f: String): String = jobj.getAt[JsonString](f).get.value
            Success(Rev(get(ID), get(REV)))
          }
          case _ => throw new RuntimeException("unknown entry in results")
        }
      }
    }
  }
  
  /**
   * API METHOD: gets the database revision limit.
   */
  def revisionLimit(): Future[Int] = {
    client.text(prepareGet(REV_LIMIT)) map { body =>
      if (body.startsWith("{")) {
        val err = JsonParser.parseObject(body)
        throw Couch.toCouchFailure(err)
      } else {
        Integer.parseInt(body.trim())
      }
    }
  }
  
  /**
   * API METHOD: sets the database revision limit.
   */
  def revisionLimit(limit: Int): Future[Unit] = {
    assert(limit >= 0)
    fetchNothing(
      preparePut(limit.toString.getBytes("utf8"), baseUrl / REV_LIMIT)
    )
  }
  
  /**
   * API METHOD: compacts the database.
   */
  def compact(): Future[Unit] = fetchNothing(preparePost(COMPACT))
  
  def purge(revSets: RevSet*): Future[JsonObject] = {
    val revsJson = revSetsToJson(revSets:_*)
    fetchJsonObject(preparePost(revsJson, baseUrl / PURGE))
  }
  
  /**
   * API METHOD: gets all documents (performs an all documents operation
   * via HTTP GET).
   */
  def allDocs(
    options: CouchViewOptions = CouchViewOptions(),
    otherOptions: Map[String, String] = Map.empty
  ): Future[CouchResultIterator] = {
    val url = options.toUrl(baseUrl / ALL_DOCS) & otherOptions
    CouchResultIterator(client, prepareGet(url))
  }
  
  /**
   * API METHOD: gets all documents (performs an all documents operation
   * via HTTP POST).
   */
  def allDocsPost(
    ids: Seq[String],
    options: CouchViewOptions = CouchViewOptions(),
    otherOptions: Map[String, String] = Map.empty
    
  ): Future[CouchResultIterator] = {
    val url = options.toUrl(baseUrl / ALL_DOCS) & otherOptions
    val jsonIds = ids.map(JsonString(_))
    val body = Json(CouchView.Param.KEYS -> Json(jsonIds:_*))
    CouchResultIterator(client, preparePost(body, url))
  }
  
  /**
   * API METHOD: gets missing revisions.
   */
  def missingRevisions(revSets: RevSet*) : Future[JsonObject] = {
    val revsJson = revSetsToJson(revSets:_*)
    fetchJsonObject(preparePost(revsJson, baseUrl / MISSING_REVS))
  }
  
  /**
   * API METHOD: gets revision differences.
   */
  def revisionsDiff(revSets: RevSet*) : Future[JsonObject] = {
    val revsJson = revSetsToJson(revSets:_*)
    fetchJsonObject(preparePost(revsJson, baseUrl / REVS_DIFF))
  }
  
  /**
   * API METHOD: peforms a view cleanup operation (cleans up indexes).
   */
  def viewCleanup(): Future[Unit] =
    fetchNothing(preparePost(baseUrl / VIEW_CLEANUP))
  
  
  // temporary views
  
  
  /**
   * API METHOD: Executes a temporary view.
   * Map bodies are inserted like this:
   *   "function(doc) { " + mapBody + " }"
   * So, for example, you can do this:
   *   db.temporaryView("emit(null, doc.foo);")
   * Similarly, reduce bodies are inserted like this:
   *   "function(key, values, rereduce) { " + reduceBody + " }"
   */
  def query(
    mapBody: String,
    reduceBodyOpt: Option[String] = None,
    options: CouchViewOptions = CouchViewOptions(),
    otherOptions: Map[String, String] = Map.empty
  ): Future[CouchResultIterator] = {
    
    import Json._
    import CouchView.Field.{MAP, REDUCE}
    
    assert(mapBody != null && mapBody.length > 0)
    
    def wrapMapBody(fnBody: String) = "function(doc) { " + fnBody + " }"
    
    def wrapReduceBody(fnBody: String) =
      "function(key, values, rereduce) { " + fnBody + " }"
    
    val mapFn = wrapMapBody(mapBody)
    val json: JsonObject = reduceBodyOpt match {
      case None => {
        Json(MAP -> mapFn)
      }
      case Some(reduceBody) => {
        assert(reduceBody != null && reduceBody.length > 0)
        Json(MAP -> mapFn, REDUCE -> wrapReduceBody(reduceBody))
      }
    }
    val url = options.toUrl(baseUrl / TEMP_VIEW) & otherOptions
    CouchResultIterator(client, preparePost(json, false, url))
  }
  
  
  // change feeds
  
  
  /**
   * API METHOD: Requests a normal changes feed (i.e. not long poll 
   * or continuous).
   */
  def changes(
    options: CouchChangeOptions = CouchChangeOptions()
  ): Future[CouchResultIterator]  =
    basicChanges(CouchDatabase.Param.Feed.NORMAL, RESULTS, options)
  
  /**
   * API METHOD: Requests a long-poll changes feed.
   */
  def longpollChanges(
    options: CouchChangeOptions = CouchChangeOptions()
  ): Future[CouchResultIterator] =
    basicChanges(CouchDatabase.Param.Feed.LONG_POLL, RESULTS, options)
  
  /**
   * API METHOD: Continuously fetches the change stream and applies 
   * 'withChanges' to them.
   *
   * The streaming of changes is performed in a separate thread - one taken
   * from the same thread pool used by the rest of the library
   * (via CouchHttp.executor). Thus this method returns immediately
   * after checking the reply headers. Changes will continue to stream until
   * 'finish' is called on the return CouchChangeStream.
   *
   * The default set of options, given by a new ChangeQuery object,  is almost
   * exactly the default in the specification
   * (see http://wiki.apache.org/couchdb/HTTP_database_API#Changes) except
   * a heartbeat is set to 60 seconds - thus overriding the default
   * 60 second timeout: i.e. ChangeQuery() heartbeat(60*1000)
   *
   * The changes can be streamed using a different ExecutorService by setting
   * 'executorOpt'.
   */
  def continuousChanges(
    withChanges: JsonObject => _,
    options: CouchChangeOptions = CouchChangeOptions() heartbeat (60 * 1000),
    executorOpt: Option[ExecutionContext] = None
  ): Future[CouchChangePusher] = {
    basicChanges(
      CouchDatabase.Param.Feed.CONTINUOUS,
      CouchDatabase.Field.CHANGES,
      options
    ) map { changes =>
      val streamer = new CouchChangePusher(changes, withChanges)
      executorOpt map { _.execute(streamer) } getOrElse streamer.run
      streamer
    }
  }
  
  private[couch] def basicChanges(
    feedType: String,
    resultsKey: String,
    options: CouchChangeOptions
  ): Future[CouchResultIterator] = {
    val url = options.toUrl(baseUrl / CouchDatabase.Path.CHANGES) &
      CouchDatabase.Param.FEED -> feedType
    CouchResultIterator(client, prepareGet(url), resultsKey)
  }
}
