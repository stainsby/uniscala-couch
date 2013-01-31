# README - Uniscala Couch

**A couchdb driver for Scala, but not an object mapper.**


## About

Uniscala Couch is a low/mid-level driver for CouchDB written in the Scala
programming language. It doesn't attempt to map CouchDB JSON documents to and
from your domain/model Scala objects - it's not an object mapper.

Uniscala Couch require Scala 2.10 futures, so it's not compatible with Scala
2.9.x. It also depends on Netty 4, and Uniscala JSON. The latter is an 
efficient JSON library built with document-oriented databases in mind, which 
features a hierarchy of type-safe, immutable JSON data types implemented
as a complete set of case classes.


### Feedback

We've created this driver because we needed it, and placed it here in the hope
others can make use of it. If you end up using it for a project, or you find 
bugs or errors in  the documentation, please let us know. If it ends up
being instrumental to an important project, please consider a donation
to [Sustainable Software Pty Ltd][ss].


## Features

  * (almost) full CouchDB HTTP API coverage - see 'CouchDB HTTP API Coverage' below
  * asynchronous I/O using Netty 4 and Scala futures
  * streaming view results (including view-like results such as *all_docs* 
    operations)
  * streamed uploads for attachments 
  * backed by a JSON library with immutable, Scala collection-compliant JSON types
  * object mapping is not forced upon the user


Sessions, OAuth and URL rewriting parts of the CouchDB API have not been 
implemented at this time. The *new_edits* modes of operations are also not
currently available. Please let us know if there is a great demand for 
any of these.


## Stability

The driver is tested and stable, but yet to be used in any non-trivial
projects. It's possible that the API may change considerably once we start 
using it in earnest.

Note that our driver project is using Netty 4 (https://netty.io/), which is version 
4.0.0 alpha 8 at this time, but due to be beta in the next release. That said, 
we haven't encountered any problems with it so far. I have heard there is a 
problem ith uploads of more than 2GB that is fixed in Netty trunk.


## License

Copyright 2012 Sustainable Software Pty Ltd.
Licensed under the Apache License, Version 2.0 (the "License").
You may obtain a copy of the License at
[http://www.apache.org/licenses/LICENSE-2.0][license].
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


## Contributors

Uniscala Couch is managed and developed by Sam Stainsby at 
[Sustainable Software Pty Ltd][ss]


## Usage

See the [Usage] document.


## API documentation

See the [scaladoc].


## Including the library in your project

Uniscala Couch releases are available in the mainstream repositories:

    <dependency>
      <groupId>net.uniscala</groupId>
      <artifactId>uniscala-couch_2.10</artifactId>
      <version>0.3</version>
    </dependency>

in your `pom.xml`, or using SBT, in your `build.sbt`:

    "net.uniscala" %% "uniscala-couch" % "0.3"


## Building

Uniscala Couch is built in a fairly standard way using SBT.


## CouchDB HTTP API Coverage

Below, the API sections correspond to those in the Couch API specification - 
refer to [Complete HTTP API Reference][spec] on the CouchDB website.


### Server-level miscellaneous methods

`GET /` - **implemented & tested**

`GET /favicon.ico` - **implemented & tested**

`GET /_all_dbs` - **implemented & tested**

`GET /_active_tasks` - **implemented & tested**

`POST /_replicate` - **implemented** - not tested (legacy - use
`_replicator` instead)

`* /_replicator` - **implemented** (nothing special 
implemented; use ordinary database API methods)

`GET /_uuids` - **implemented & tested**

`POST /_restart` - **implemented & tested**

`GET /_stats` - **implemented & tested**

`GET /_log` - **implemented & tested**

`GET /_utils/file` - **implemented & tested**


### Server configuration

`GET /_config` - **implemented & tested**

`GET /_config/section` - **implemented & tested**

`GET /_config/section/key` - **implemented & tested**

`PUT /_config/section/key` - **implemented & tested**

`DELETE /_config/section/key` - **implemented & tested**


### Authentication

NOTE: this section of the API doesn't refer to HTTP basic 
authentication that may be needed to authorise other API methods. Basic
authentication is achieved by doing something like this:

    val client = new CouchClient(credentialsOption = Some(BasicCredentials("admin", "changeme")))


`GET /_session` - not implemented (not useful?)

`POST /_session` - not implemented (not useful?)

`DELETE /_session` - not implemented (not useful?)

`GET /_oauth/access_token` - not implemented

`GET /_oauth/authorize` - not implemented

`POST /_oauth/authorize` - not implemented

`* /_oauth/request_token` - not implemented

`* /_users` - **implemented** - (nothing special 
implemented; use ordinary database API methods)


### Database methods

`GET /db` - **implemented & tested**

`PUT /db` - **implemented & tested**

`DELETE /db` - **implemented & tested**

`GET /db/_changes` - **implemented & tested**

`POST /db/_compact` - **implemented & tested**

`POST /db/_compact/design-doc` - **implemented & tested**

`POST /db/_view_cleanup` - **implemented & tested**

`POST /db/_temp_view` - **implemented & tested**

`POST /db/_ensure_full_commit` - **implemented & tested**

`POST /db/_bulk_docs` - **implemented & tested** (support for *new_edits* mode
is not implemented)

`POST /db/_purge` - **implemented** - not tested

`GET /db/_all_docs` - **implemented & tested**

`POST /db/_all_docs` - **implemented & tested**

`POST /db/_missing_revs` - **implemented** - not tested

`POST /db/_revs_diff` - **implemented** - not tested

`GET /db/_security` - **implemented** (nothing special 
implemented; use ordinary database API methods)

`PUT /db/_security` - **implemented** (nothing special 
implemented; use ordinary database API methods)

`GET /db/_revs_limit` - **implemented & tested**

`PUT /db/_revs_limit` - **implemented & tested**


### Database document methods

`POST /db` - **implemented & tested**

`GET /db/doc` - **implemented & tested**

`HEAD /db/doc` - **implemented & tested**

`PUT /db/doc` - **implemented & tested**

`DELETE /db/doc` - **implemented & tested**

`COPY /db/doc` - **implemented & tested**

`GET /db/doc/attachment` - **implemented & tested**

`PUT /db/doc/attachment` - **implemented & tested**

`DELETE /db/doc/attachment` - **implemented & tested**


### Special non-replicating documents

`GET /db/_local/local-doc` - **implemented & tested**

`PUT /db/_local/local-doc` - **implemented & tested**

`DELETE /db/_local/local-doc` - **implemented & tested**

`COPY /db/_local/local-doc` - **implemented & tested**


### Special design documents

`GET /db/_design/design-doc` - **implemented & tested**

`PUT /db/_design/design-doc` - **implemented & tested**

`DELETE /db/_design/design-doc` - **implemented & tested**

`COPY /db/_design/design-doc` - **implemented & tested**

`GET /db/_design/design-doc/attachment` - **implemented & tested**

`PUT /db/_design/design-doc/attachment` - **implemented & tested**

`DELETE /db/_design/design-doc/attachment` - **implemented & tested**


### Special design document handlers

`GET /db/_design/design-doc/_info` - **implemented & tested**

`GET /db/_design/design-doc/_view/view-name` - **implemented & tested**

`POST /db/_design/design-doc/_view/view-name` - **implemented & tested**

`? /db/_design/design-doc/_show/show-name` - **implemented & tested**

`? /db/_design/design-doc/_show/show-name/doc` - **implemented & tested**

`GET /db/_design/design-doc/_list/list-name/view-name` -
**implemented & tested**

`POST /db/_design/design-doc/_list/list-name/view-name` -
**implemented & tested**

`GET /db/_design/design-doc/_list/list-name/other-design-doc/view-name` -
**implemented & tested**

`POST /db/_design/design-doc/_list/list-name/other-design-doc/view-name` -
**implemented & tested**

`? /db/_design/design-doc/_update/update-name` - **implemented & tested**

`? /db/_design/design-doc/_update/update-name/doc` - **implemented & tested**

`* /db/_design/design-doc/_rewrite/rewrite-name/anything` - not implemented


[spec]: http://wiki.apache.org/couchdb/Complete_HTTP_API_Reference "Complete HTTP API Reference"
[project]: https://github.com/stainsby/uniscala-couch "Uniscala Couch on GitHub"
[usage]: https://github.com/stainsby/uniscala-couch/blob/master/Usage.md "Uniscala Couch - Usage"
[license]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License Version 2.0, January 2004"
[scaladoc]: http://stainsby.github.com/uniscala-couch/scaladocs/index.html "Scaladoc"
[ss]: http://www.sustainablesoftware.com.au/ "Sustainable Software Pty Ltd"