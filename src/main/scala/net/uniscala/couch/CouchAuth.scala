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

/*

SEEMED LIKE A GOOD IDEA AT THE TIME, BUT NOT SO SURE NOW. DISABLING 
THIS FOR NOW.

package net.uniscala.couch

import net.uniscala.json._

import util.{Mime, Url}


object CouchAuth {
  
  object Path {
    val SESSION: String = "_session"
  }
  
  object Param {
    val NAME: String     = "name"
    val PASSWORD: String = "password"
  }
  
  case class Session(cookie: String, json: JsonObject)
}


class CouchAuth private[couch] (val couch: CouchServer)
extends CouchPath(Some(couch), CouchAuth.Path.SESSION) {
  
  import Couch._
  import CouchAuth._
  import CouchAuth.Param._
  import Json._
  import Http.Header._
  
  
  // create a session
  
  
  def login(
    username: String,
    password: String
   ): ListenableFuture[Either[CouchFailure, Session]] = {
    assert(username != null, "null username")
    assert(password != null, "null password")
    var credentialStr = Url.encodeUrlQueryString(
      Map(NAME -> username, PASSWORD -> password),
      new StringBuilder
    ).toString
    if (credentialStr.head == '?') credentialStr = credentialStr.tail
    val req = POSTBuilder().setBody(credentialStr).
      setHeader(CONTENT_TYPE, Mime.FORM_ENC.toString).
      build
    request(
      req,
      (resp: Response) => {
        Couch.Handle.JsonObject(resp) match {
          case Right(jobj: JsonObject) => {
            //val cookie: String = resp.getCookies().get(0).getValue()
            val cookie = resp.getCookies().get(0)
            val cookieStr = cookie.getName + '=' + cookie.getValue
            Right(Session(cookieStr, jobj))
          }
          case Left(err) => Left(err)
        }
      }
    )
  }
  
  def login(
    username: String,
    password: String
   ): Either[CouchFailure, Session] =
     login(username, password).get
  
  def login(username: String, password: String): Session =
    requireResult(login(username, password))
  
  
  // get the current session
   
   
  def session(
    session: Session
  ): ListenableFuture[Either[CouchFailure, JsonObject]] = {
    requestJsonObject(GETBuilder().setHeader(COOKIE, session.cookie).build)
  }
   
  def session(session: Session): Either[CouchFailure, JsonObject] =
    session(session).get
   
  def session(session: Session): JsonObject =
    requireResult(session(session))
  
  
  // delete a session
  
  
  def deleteSession(
    session: Session
  ): ListenableFuture[Option[CouchFailure]] = {
    requestEmpty(DELETEBuilder().setHeader(COOKIE, session.cookie).build)
  }
  
  def deleteSession(session: Session): Option[CouchFailure] =
    deleteSession(session).get
  
  def deleteSession(session: Session): Unit =
    requireSuccess(deleteSession(session))
}
*/
