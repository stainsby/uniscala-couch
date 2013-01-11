# Uniscala Couch driver - Usage

While we don't cover the entire driver API here, this document plus the 
scaladoc should be enough to get you started.


### Feedback

We've created this driver because we needed it, and placed it here in the hope
others can make use of it. If you end up using it for a project, or you find 
bugs or errors in  the documentation, please let us know. If it ends up
being instrumental to an important project, please consider a donation
to [Sustainable Software Pty Ltd][ss].


## Connecting to a CouchDB server:

    import net.uniscala.couch._
    import net.uniscala.couch.http.BasicCredentials
    
    val creds = BasicCredentials("admin", "changeme")
    val client = new CouchClient(credentialsOption = Some(creds))

You can omit authentication credentials for anonymous access:

    val client2 = new CouchClient()

This gives you both an HTTP client tuned for CouchDB interaction, and access
to server-level API methods.

Once you've finished with a client, you should shut it down:

    client.shutdown

to release any resources it's holding. Beware that, currently, the Scala REPL
will freeze if you have performed some database operations, and try to exit the 
REPL without calling `shutdown` first.


### A note on Scala futures

Functions in the driver that make calls to the CouchDB HTTP API typically 
return a Scala `Future` for the response. Scala futures are new in Scala 2.10 .
sing futures allows asynchronous I/Op rogramming approaches. It's also easy to 
convert to a synchronous call using 'await' - see the 'Server info' example 
below.


## Example: Server info and asynchronous / synchronous styles

Let get very basic information about the server. For the purpose of 
demonstration, we'll wait for the response using `await`.
    
    import net.uniscala.couch.util.Futures.await
    
    await { client.info }
    
gets us:
  
    net.uniscala.json.JsonObject = {"couchdb": "Welcome", "version": "1.2.0"}

You might prefer to use await with a timeout, which, for convenience, you 
can alias like this:

    import scala.concurrent._
    import scala.concurrent.duration._
    import net.uniscala.couch.util.Futures.awaitFor
    
    def await60[T](f: Future[T]): T = awaitFor[T](60 seconds)(f)

so we can now do this sort of thing everywhere:

    await60 { client.info }


A timeout exception will be thrown if the server fails to respond within 60
seconds.


## Other server-level operations

Other server-level operations are available through a CouchClient instance, 
such as listing, creating and deleting databases, generating UUIDS and
even restarting the server. In fact, let's do three of those four operations
in sequence now:

    import ExecutionContext.Implicits.global
    
    await {
      client.createDatabase("mydb001") andThen
      { case _ => client.deleteDatabase("mydb001") } andThen
      { case _ => client.restartServer }
    }

Let's also generate some UUIDs to illustrate a point about return values for
our driver:

    val uuids = await { client.generateUuids(4) }

will return something like:

    uuids: net.uniscala.json.JsonObject = {
      "uuids": [
        "9bd260aa00b47d691e8d5361d500063c",
        "9bd260aa00b47d691e8d5361d5000838",
        "9bd260aa00b47d691e8d5361d50014cc",
        "9bd260aa00b47d691e8d5361d50016e5"
      ]
    }

Note that we get the same JSON back that the server sends. We don't 
attempt to map it into 'POJO' Scala objects. This is a recurrent theme
throughout the driver - see the note below.  We can use the UUIDs
by doing this sort of thing:

    import net.uniscala.json._
    import Json._
    
    uuids.getAt[JsonArray]("uuids").get.map(_.value).mkString(", ")
    
to print:
    
    res45: String = c926684503b1f19112ebd62013004332, c926684503b1f19112ebd620130049e9, ...

Refer to the [Uniscala JSON][uniscalajson] documentation for details.

The are a range of other server-level API operations. Refer to the Uniscala
Couch scaladoc for details.


### A note on response types

As seen in the UUID example above, almost all of the API methods in our 
driver that return data return it as the original JSON, rather than trying 
to convert it to pure Scala objects (POJOs). This is for simplicity and
efficiency, and to keep the driver as robust as possible under
future changes to the Couch HTTP API. No doubt it is entirely possible to
build a mapping layer on top of our library, but for the time being that's
somebody else's problem. We'd prefer not to force users of our API
to incur the overhead of continually mapping results to Scala POJOs, 
particularly for complex documents, if it's not required for their 
application.

We needed a JSON library powerful enough to ease the use and
manipulation of the returned values. We were unsatisfied with the 
offerings at the time, which is why we created [Uniscala JSON][uniscalajson].


# Database operations

Database-level operation are accessed in the driver by creating a
`CouchDatabase` "path":

    scala> val db = client.database("tmpfoobat")
    db: net.uniscala.couch.CouchDatabase = CouchDatabase[localhost://localhost:5984/tmpfoobat]

which gives the your access to basic Couch document CRUD operations:

    scala> val jdoc = Json("aaa" -> "bbb")
    jdoc: net.uniscala.json.JsonObject = {"bbb": "bbb"}
    
    scala> val doc = await { db.insert("foodoc", jdoc) }
    doc: net.uniscala.couch.CouchDoc = CouchDoc( ...
    
    scala> val doc2 = await { doc.update(Json("aaa" -> "ccc")) }
    doc2: net.uniscala.couch.CouchDoc = CouchDoc( ...
    
    scala> await { db.get("foodoc") }
    res1: Option[net.uniscala.couch.CouchDoc] = Some(CouchDoc( ...
    
    scala> await { db.delete(doc2.ref) }
    res2: net.uniscala.couch.CouchDoc.Rev = Rev(foodoc,3-d73808f4d35f24d5bf49bc9ac8dca83e)
    
    scala> await { db.get("foodoc") }
    res3: Option[net.uniscala.couch.CouchDoc] = None

A wide range of database-level operations are implemented - see the scaladoc 
for details. 


# Document operations

Couch documents are represented by instances of `CouchDoc`. We've already seen 
one document operation - `update` in the example above. There are also
attachment and copy operations.

### Attachment

We could have also called `delete` directly on our document in the example. 
We can also add, get and delete attachments:

    import net.uniscala.couch.util.Mime
    
    val attachmentFileName = "badge.png"
    val attachmentFilePath = "/some/path/" + attachmentFileName
    val jdoc = Json("attach" -> "to me")
    val doc = await { db.insert("mydoc", jdoc) }
    val file = new java.io.File(attachmentFilePath)
    
    // add and attachment
    await { doc.attach(attachmentFileName, file, Mime.PNG) }
    
    // get an attachment
    await { doc.attachment(attachmentFileName) }
    
    // delete an attachment
    await { doc.delete(attachmentFileName) }

Note that getting an attachment returns a CouchAttachment object (or None
if the attachment doesn't exist).:

    scala> val attachmentOpt = await { doc.attachment(attachmentFileName) }
    attachmentOpt: Option[net.uniscala.couch.CouchAttachment] = ...

The attachment data can then be accessed through the `stream` method:

    scala> val attachment =  await { doc.attachment(attachmentFileName) } get
    attachment: net.uniscala.couch.CouchAttachment = net.uniscala.couch.CouchAttachment@375465a1
    
    scala> val in = attachment.stream
    in: java.io.InputStream = sun.nio.ch.ChannelInputStream@491a768b
    
    val bytes = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
    bytes: Array[Byte] = Array(-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, ...


## Querying views

We access a view through the path `CouchDatabase` -> 
`CouchDesign` -> `CouchView`:

    scala> val db = client.database("hnkiosk")
    db: net.uniscala.couch.CouchDatabase = CouchDatabase[localhost://localhost:5984/hnkiosk]
    
    scala> val design = db.design("kiosk")
    design: net.uniscala.couch.CouchDesign = net.uniscala.couch.CouchDesign@7c0f3ac0
    
    scala> val view = design.view("commands")
    view: net.uniscala.couch.CouchView = net.uniscala.couch.CouchView@19f8d435

and then query the view using options set with a `CouchViewOptions` instance:

    scala> await(view.query(CouchViewOptions().limit(10)))
    res12: net.uniscala.couch.CouchResultIterator = non-empty iterator

The resulting `CouchResultIterator` will stream and parse the results from 
the server as you iterate through them. Thus, larger results sets will not 
increase memory usage. `CouchResultIterator` is a true Scala iterator.
You can also conveniently extract rows using `nextResult`:

    scala> res12.nextResult
    res13: Option[net.uniscala.json.JsonObject] = Some({"id": "d046b868adbb80a77290814a930a33fb",
        "key": ["executed", 1340777483278], "value": {"command": "PING"}})
    
    scala> res12.nextResult
    res14: Option[net.uniscala.json.JsonObject] = Some({"id": "d046b868adbb80a77290814a93000549",
        "key": ["executed", 1340779379732], "value": {"command": "PING"}})
    
    scala> res12.nextResult
    res15: Option[net.uniscala.json.JsonObject] = Some({"id": "d046b868adbb80a77290814a93100efb",
        "key": ["executed", 1340802162900], "value": {"command": "PING"}})
    
    scala> res12.nextResult
    res16: Option[net.uniscala.json.JsonObject] = None

Once all results have been read out, `nextResult` with always return `None`.


## Design document operations

Designs are created though the `CouchDesigns` path (note the trailing 's'!).

    scala> val designs = db.designs
    designs: net.uniscala.couch.CouchDesigns = net.uniscala.couch.CouchDesigns@642ccba7
    
    scala> val mapfn = "function(doc) { if (doc.Type == 'customer')  emit(null, doc) }"
    mapfn: String = function(doc) { if (doc.Type == 'customer')  emit(null, doc) }
    
    scala> designs.insert("mydes", Json("views" -> Json("myview" -> Json("map" -> mapfn))))
    res20: scala.concurrent.Future[net.uniscala.couch.CouchDoc] = ..

They can be deleted in a similar manner.

**It is important to notice that a `CouchDesign` object does not
represent a couch document** - for that we use a `CouchDoc` which can be 
obtained from a `CouchDesign` thus

    val des: CouchDesign = db.design("mydes")
    val desdoc: Option[CouchDoc] = await(des.doc)

and then manipulated by the usual document operations, including adding 
attachments. A `CouchDesign` object represents a "path" (in a URL sense) to
a design, rather than the document.


[uniscalajson]: https://github.com/stainsby/uniscala-json "Uniscala JSON on GitHub"
[ss]: http://www.sustainablesoftware.com.au/ "Sustainable Software Pty Ltd"
