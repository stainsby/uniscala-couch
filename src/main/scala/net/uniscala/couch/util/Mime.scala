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
package net.uniscala.couch.util


case class Mime(major: String, minor: String) {
  override lazy val toString = major + "/" + minor
}


/**
 * A selection of MIME-types that are commonly used, at least as attachments 
 *  in CouchDB.
 */
object Mime {
  
  private val app       = "application"
  private val audio     = "audio"
  private val image     = "image"
  private val text      = "text"
  private val video     = "video"
  
  val ANY               = Mime("*", "*")
  
  val BINARY            = Mime(app, "octet-stream")
  val FORM_ENC          = Mime(app, "x-www-form-urlencoded")
  val GZIP              = Mime(app, "gzip")
  val JAVASCRIPT        = Mime(app, "javascript")
  val JSON              = Mime(app, "json")
  val PDF               = Mime(app, "pdf")
  val ZIP               = Mime(app, "zip")
  
  val MP4_AUDIO         = Mime(audio, "mp4")
  val MPEG_AUDIO        = Mime(audio, "mpeg")
  val OGG_AUDIO         = Mime(audio, "ogg")
  val VORBIS_AUDIO      = Mime(audio, "vorbis")
  val WEBM_AUDIO        = Mime(audio, "webm")
  
  val GIF               = Mime(image, "gif")
  val JPEG              = Mime(image, "jpeg")
  val PNG               = Mime(image, "png")
  
  val CSS               = Mime(text, "css")
  val HTML              = Mime(text, "html")
  val TEXT              = Mime(text, "plain")
  val XML               = Mime(text, "xml")
  
  val FLASH_VIDEO       = Mime(video, "x-flv")
  val MATROSKA_VIDEO    = Mime(video, "x-matroska")
  val MP4_VIDEO         = Mime(video, "mp4")
  val MPEG_VIDEO        = Mime(video, "mpeg")
  val OGG_VIDEO         = Mime(video, "ogg")
  val QUICKTIME_VIDEO   = Mime(video, "quicktime")
  val WEBM_VIDEO        = Mime(video, "webm")
  val WIN_VIDEO         = Mime(video, "x-ms-wmv")
}