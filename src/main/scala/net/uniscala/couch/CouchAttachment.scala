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

import scala.concurrent.{Await, ExecutionContext, future, Future}

import java.io._

import net.uniscala.json._

import http.Response
import util.{Futures, Http, Mime, Url}
import Futures.await


object CouchAttachment {
  
  import ExecutionContext.Implicits.global
  
  import Http.Header._
  
  private val BINARY = Mime.BINARY.toString
  
  private[couch] def appendFileName(url: Url, attachmentId: String): Url = {
    // ensure slashes in a file name are not escaped
    url / (attachmentId.split('/'):_*)
  }
  
  /**
   * Returns a Future for the attachment at the specified path (see 
   * also CouchPath). If there is no attachment for the give 'attachmentId'
   * the Future returns None.
   */
  def atPath(
    path: CouchPath,
    attachmentId: String
  ): Future[Option[CouchAttachment]] = {
    
    assert(attachmentId != null)
    val url = appendFileName(path.baseUrl, attachmentId)
    
    val respOptFuture: Future[Option[Response]] = {
      path.client.send(path.prepareGet(url)) map { resp =>   
        val statusCode: Int = resp.statusCode
        if (statusCode == 200) {
          Some(resp)
        } else if (statusCode == 404) {
          None
        } else {
          val reply = await { resp.content.text }
          val msgOpt = if (reply.length > 0) Some(reply)
            else resp.statusMessageOption
          throw Couch.toCouchFailure(statusCode, msgOpt)
        }
      }
    }
    
    respOptFuture flatMap { respOpt: Option[Response] =>
      
      respOpt match {
        case None => future { None } // TODO: can we avoid this?
        case Some(resp) => {
          
          val contentType = Option(resp.header(CONTENT_TYPE)).getOrElse(BINARY)
          val contentLength = Option(resp.header(CONTENT_LENGTH)) flatMap {
            lengthStr => try {
              Some(lengthStr.toLong)
            } catch {
              case _:NumberFormatException => None
            }
          }
          
          resp.readStream { in =>
            Some(
              new CouchAttachment(
                path, attachmentId, contentType, contentLength, in
              )
            )
          }
        }
      }
    }
  }
}


/**
 * Represents an attachment. The stream can be used ONCE ONLY to read the 
 * contents of the attachment. The stream can be closed directly of through 
 * the close method here.
 */
class CouchAttachment(
  val path: CouchPath,
  val name: String,
  val contentType: String,
  val lengthOption: Option[Long],
  val stream: InputStream
) {
  
  /**
   * Closes the stream.
   */
  def close = stream.close
}
