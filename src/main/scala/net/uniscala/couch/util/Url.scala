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


object Url {
  
  val LOCALHOST = "localhost"
  
  object Chars {
    val DIGIT: String = "0123456789"
    val ALPHA_LOWER: String = "abcdefghijklmnopqrstuvwxyz"
    lazy val ALPHA_UPPER: String = ALPHA_LOWER.toUpperCase()
    lazy val ALPHA: String = ALPHA_UPPER + ALPHA_LOWER
    lazy val ALPHA_OR_DIGIT: String = DIGIT + ALPHA
    object Uri {
      // URIs - refer to http://tools.ietf.org/html/rfc3986 (Appendix A)
      lazy val UNRESERVED = ALPHA_OR_DIGIT + "-._~"
      lazy val SUB_DELIMS_NOT_PLUS = "!$&'()*,;="
      lazy val SUB_DELIMS = SUB_DELIMS_NOT_PLUS + "+"
      lazy val PCHAR_NOT_PLUS: String = UNRESERVED + SUB_DELIMS_NOT_PLUS + ":@"
      lazy val PCHAR: String = PCHAR_NOT_PLUS + "+"
    }
  }
  
  object percentEncode {
    
    private val PLUS_CHAR = '+'
    
    // eg. percentHex(11) = "%0b"
    private lazy val percentHex: Array[String] = {
      val hexChars = "0123456789abcdef"
        for (ch1 <- hexChars; ch2 <- hexChars) yield "%" + ch1 + ch2
    }.toArray
    
    def apply(
      str: String,
      builder: StringBuilder,
      except: Seq[Char],
      encodeSpaceAsPlus: Boolean
    ): StringBuilder = {
      
      val exclusions = if (encodeSpaceAsPlus && except.contains(PLUS_CHAR)) {
        except.filter(_ != PLUS_CHAR)
      } else {
        except
      }
      
      str foreach {
        case c if exclusions contains c => builder.append(c)
        case c => {
          toBytes("" + c) foreach { b =>
            if (encodeSpaceAsPlus && b == ' ') {
              builder.append(PLUS_CHAR)
            } else {
              builder.append(percentHex(if (b < 0) 256 + b else b))
            }
          }
        }
      }
      
      builder
    }
    
    private def toBytes(str: String): Seq[Byte] = str.getBytes("UTF-8")
  }
  
  object encodeUrlPathSegment {
    def apply(segment: String, builder: StringBuilder): StringBuilder =
      percentEncode(segment, builder, Chars.Uri.PCHAR , false)
  }
  
  object encodeUrlQueryString {
    
    def apply(segment: String, builder: StringBuilder): StringBuilder =
      percentEncode(segment, builder, Chars.Uri.PCHAR_NOT_PLUS , true)
    
    def apply(
      queryMap: Map[String, String],
      builder: StringBuilder
    ): StringBuilder = {
      if (!queryMap.isEmpty) {
        builder append "?"
        queryMap foreach { case (k, v) =>
          if (!builder.endsWith("?")) builder.append("&")
          encodeUrlQueryString(k, builder).append("=")
          encodeUrlQueryString(v, builder)
        }
      }
      builder
    }
  }
}


object HttpUrl {
  
  import Url._
  
  val PROTOCOL = "http"
  val DEFAULT_PORT = 80
  
  def apply(
    host: String = LOCALHOST,
    port: Int = DEFAULT_PORT,
    userinfoOption: Option[UserInfo] = None,
    path: List[String] = Nil,
    query: Map[String, String] = Map.empty,
    fragmentOption: Option[String] = None
  ): Url = Url(PROTOCOL, host, port, userinfoOption, path, query, fragmentOption)
}


object SecureHttpUrl {
  
  import Url._
  
  val PROTOCOL = "https"
  val DEFAULT_PORT = 443
  
  def apply(
    host: String = LOCALHOST,
    port: Int = DEFAULT_PORT,
    userinfoOption: Option[UserInfo] = None,
    path: List[String] = Nil,
    query: Map[String, String] = Map.empty,
    fragmentOption: Option[String] = None
  ): Url = Url(PROTOCOL, host, port, userinfoOption, path, query, fragmentOption)
}


case class UserInfo(parts: String*) {
  
  import Url._
  
  def encode(
    builder: StringBuilder,
    hideSecrets: Boolean = true
  ): StringBuilder = {
    
    def encodePart(part: String) = encodeUrlPathSegment(part, builder)
    
    var isFirst = true
    parts foreach { part =>
      if (isFirst) {
        encodePart(part)
      } else {
        builder.append(':')
        if (hideSecrets) builder.append("xxxxxxxx") else encodePart(part)
      }
      isFirst = false
    }
    
    builder.append('@')
  }
  
  override def toString() = encode(new StringBuilder, true).toString
}


case class Url(
  protocol: String,
  host: String = Url.LOCALHOST,
  port: Int,
  userinfoOption: Option[UserInfo] = None,
  path: List[String] = Nil,
  query: Map[String, String] = Map.empty,
  fragmentOption: Option[String] = None
) {
  
  import Url._
  
  def host(host: String): Url = copy(host = host)
  
  def port(port: Int): Url = copy(port = port)
  
  def user(user: String): Url = copy(userinfoOption = Some(UserInfo(user)))
  
  def user(userSecret: (String, String)): Url =
    copy(userinfoOption = Some(UserInfo(userSecret._1, userSecret._2)))
  
  def user(info: UserInfo): Url = copy(userinfoOption = Some(info))
  
  def /(segments: String*): Url = copy(path = this.path ++ segments)
  
  def &(query: (String, String)*): Url = copy(query = this.query ++ query)
  
  def &(query: Map[String, String]): Url = copy(query = this.query ++ query)
  
  def #=(fragment: String): Url = copy(fragmentOption = Some(fragment))
  
  def #!(): Url = copy(fragmentOption = None)
  
  def encode(builder: StringBuilder): StringBuilder =
    encode(builder, hideSecrets = false)
  
  lazy val encode: String =
    encode(new StringBuilder, hideSecrets = false).toString
  
  lazy val encodePathOnwards: String =
    encodePathOnwards(new StringBuilder).toString
  
  override lazy val toString =
    encode(new StringBuilder, hideSecrets = true).toString
  
  def encode(
    builder: StringBuilder,
    hideSecrets: Boolean = true
  ): StringBuilder = {
    builder.
      append(protocol).
      append("://")
      
    userinfoOption foreach { userInfo =>
      if (hideSecrets) {
        builder.append(userInfo.toString)
      } else {
        userInfo.encode(builder, false)
      }
    }
    
    builder.
      append(host).
      append(":").
      append(port)
    
    encodePathOnwards(builder)
    
    builder
  }
  
  def encodePathOnwards(
    builder: StringBuilder
  ): StringBuilder = {
    builder.
      append("/")
    path foreach { segment =>
      if (!builder.endsWith("/")) builder append "/"
      encodeUrlPathSegment(segment, builder)
    }
    encodeUrlQueryString(query, builder)
    fragmentOption foreach { fragment: String =>
      builder.append("#")
      encodeUrlQueryString(fragment, builder)
    }
    builder
  }
}
