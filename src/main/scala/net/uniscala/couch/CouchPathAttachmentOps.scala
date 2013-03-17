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

import java.io.{File, FileInputStream, InputStream}
import java.nio.channels.ReadableByteChannel

import io.netty.buffer.ByteBuf

import Couch._
import util.{Http, Mime, Url}
import net.uniscala.couch.http.Response
import Http.Header.CONTENT_LENGTH


/**
 * Provides Couch API methods for attachments.
 */
trait CouchPathAttachmentOps[C <: CouchDocBase] extends CouchPathOpsBase[C] {
  
  this: CouchSubPath =>
  
  import CouchDoc._
  
  def rev: String
  
  private def appendAttachmentId(url: Url, attachmentId: String): Url = {
    // ensure slashes in attachment ID are not escaped
    url / (attachmentId.split('/'):_*)
  }
  
  private val attachResultHandler: Response => Future[Rev] = { resp =>
    import CouchDoc.Param._
    import scala.concurrent.ExecutionContext.Implicits.global
    resp.text map { txt =>
      val jobj = toSafeJsonObject(txt)
      val id0 = jobj(ID).value.toString
      val rev0 = jobj(REV).value.toString
      Rev(id0, rev0)
    }
  }
  
  private def prepareAttachRequest(attachmentId: String, contentType: Mime) = {
    assert(attachmentId != null, "null attachment ID")
    assert(contentType != null, "null MIME type")
    val url = appendAttachmentId(baseUrl, attachmentId) &
      (CouchDoc.Param.REV -> rev)
    preparePut(url)
  }
  
  /**
   * API METHOD: Attaches the data read from the specified file.
   */
  def attach(
    attachmentId: String,
    file: File,
    contentType: Mime
  ): Future[Rev] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    assert(file != null, "null file")
    assert(file .exists(), "file doesn't exist")
    val req = prepareAttachRequest(attachmentId: String, contentType: Mime)
    client.upload(req, file, contentType).flatMap(attachResultHandler)
  }
  
  /**
   * API METHOD: Attaches the data read from the specified input stream.
   */
  def attach(
    attachmentId: String,
    in: InputStream,
    contentType: Mime,
    contentLength: Long
  ): Future[Rev] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    assert(in != null, "null input stream")
    val req = prepareAttachRequest(attachmentId: String, contentType: Mime)
    req.headers.set(CONTENT_LENGTH, contentLength)
    client.upload(req, in, contentType, contentLength).
      flatMap(attachResultHandler)
  }
  
  /**
   * API METHOD: Attaches the data read from the specified NIO channel.
   */
  def attach(
    attachmentId: String,
    in: ReadableByteChannel,
    contentType: Mime,
    contentLength: Long
  ): Future[Rev] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    assert(in != null, "null input stream")
    val req = prepareAttachRequest(attachmentId: String, contentType: Mime)
    req.headers.set(CONTENT_LENGTH, contentLength)
    client.upload(req, in, contentType, contentLength).
      flatMap(attachResultHandler)
  }
  
  /**
   * API METHOD: Gets an attachment.
   */
  def attachment(attachmentId: String): Future[Option[CouchAttachment]] =
    CouchAttachment.atPath(this, attachmentId)
  
  /**
   * API METHOD: Deletes an attachment.
   */
  def deleteAttachment(attachmentId: String): Future[Rev] = {
    val url = appendAttachmentId(baseUrl, attachmentId) &
      (CouchDoc.Param.REV -> rev)
    fetchRev(prepareDelete(url))
  }
}
