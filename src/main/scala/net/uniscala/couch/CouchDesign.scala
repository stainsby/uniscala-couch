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

import scala.concurrent.Future

import net.uniscala.json._

import Couch._
import util.Url


class CouchDesign private[couch] (
  val designs: CouchDesigns,
  id: String
) extends CouchSubPath(designs, id) {
  
  /**
   * The database corresponding to this design.
   */
  lazy val database = designs.database
  
  /**
   * Instantiates a views path for this design (see also CouchPath).
   */
  protected lazy val views = new CouchViews(this)
  
  /**
   * Instantiates a lists path for this design (see also CouchPath).
   */
  protected lazy val lists = new CouchLists(this)
  
  /**
   * Instantiates a shows path for this design (see also CouchPath).
   */
  protected lazy val shows = new CouchShows(this)
  
  /**
   * Instantiates an updates path for this design (see also CouchPath).
   */
  protected lazy val updates = new CouchUpdates(this)
  
  /**
   * Instantiates a view path for this design (see also CouchPath).
   */
  def view(viewName: String): CouchView = new CouchView(views, viewName)
  
  /**
   * Instantiates a list path for this design (see also CouchPath).
   */
  def list(listName: String, view: CouchView): CouchList =
    new CouchList(lists, listName, view)
  
  /**
   * Instantiates a show path for this design (see also CouchPath).
   */
  def show(showName: String): CouchShow = new CouchShow(shows, showName)
  
  /**
   * Instantiates an update path for this design (see also CouchPath).
   */
  def update(updateName: String): CouchUpdate =
    new CouchUpdate(updates, updateName)
  
  /**
   * Gets the document for this design.
   */
  def doc(): Future[Option[CouchDoc]] = designs.get(id)
  
  
  // API METHODS
  
  
  /**
   * API METHOD: get info/metadata about the design.
   */
  def info(): Future[JsonObject] =
    fetchJsonObject(prepareGet(CouchClient.Path.INFO))
  
  /**
   * API METHOD: compacts the views in the design.
   */
  def compactViews(): Future[Unit] =
    fetchNothing(database.preparePost(CouchDatabase.Path.COMPACT, id))
}
