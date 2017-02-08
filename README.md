[![](http://jenkins.imagej.net/job/ImageJ-Server/lastBuild/badge/icon)](http://jenkins.imagej.net/job/ImageJ-Server/)
[![Join the chat at https://gitter.im/imagej/imagej-server](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/imagej/imagej-server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# ImageJ Server

This is a RESTful image server backed by ImageJ.

It is currently only a prototype! Testing needed!

## Launching

To start the server from the CLI, use:

```
mvn exec:java -Dexec.mainClass="net.imagej.server.Main"
```

Or from an IDE, execute the class `net.imagej.server.Main`.

## Usage

### [Python Client](clients/python)

Uses a Python wrapper for the web API. Clients for more languages are coming.

### [Postman Collection](clients/postman)

A collection of sample API calls to imagej-server using [Postman](https://www.getpostman.com/).

### APIs

- __GET__ /modules

 Returns a list of modules. By default, imagej-server exposes its API at `localhost:8080`, which will be used throughout this documentation.
 
 `$ curl localhost:8080/modules`

- __GET__ /modules/*{id}*

 Returns detailed information of a module specified by `{id}`. Notice that `{id}` could contain special characters such as dollar sign (`$`), which needs to be escaped when using `cURL`.
 
 `$ curl localhost:8080/modules/'command:net.imagej.ops.math.PrimitiveMath$IntegerAdd'`

- __POST__ /modules/*{id}*?process=*{process}*

 Executes a module with with JSON inputs. Use the module details to determine the correct input keys. The optional query parameter `process` determines if the execution should be pre/post processed.

 ```
 $ curl -XPOST -H "Content-Type: application/json" -d '{"a":1,"b":3}' \
 > localhost:8080/modules/'command:net.imagej.ops.math.PrimitiveMath$IntegerAdd'?process=false
 {"result":4}
 ```

- __GET__ /io/objects

 Lists all object IDs available on imagej-server.
 
 `$ curl localhost:8080/io/objects`

- __DELETE__ /io/objects/*{id}*

 Delete one object from imagej-server.
 
 `$ curl -XDELETE localhost:8080/io/objects/object:1234567890abcdef`

- __POST__ /io/file

 Uploads a file to server. A 16-bit lowercase alphanumeric ID prefixed with `object:` will be returned as a JSON string. The ID can be used in module execution to represent the file. Currently only supports uploading images.

 ```
 $ curl -F "file=@src/test/resources/imgs/about4.tif" localhost:8080/io/file
 {"id":"object:0123456789abcdef"}
 ```

- __POST__ /io/file/*{id}*?format=*{format}*

 Download an object in some specific format from the server. The query parameter `format` is required.

 `$ curl -XPOST localhost:8080/io/object:0123456789abcdef?format=png`

- __DELETE__ /admin/stop

 Stop the imagej-server gracefully without shutting down the imagej runtime.

 `curl -XDELETE localhost:8080/admin/stop`

## Notes and memo

- DefaultObjectService effectively prevents garbage collection of all objects produced during the lifetime of this imagej-server instance, which could lead to serious memory issues. The timestamped object ID could be used to determine if an object should "expire" on a regular basis or when memory runs low. 
- Ops that need to be initialized are not working when run as module. See the HACK in `ModulesResource`
- The converters from `Number` to `NumericType` could be refined and considered moving to other projects such as SciJava.
- It might be a good idea for the client to provide optional language-neutral type information for module execution, so that we can support more complex data structures without too much guessing. This would also solve the problem of nested special object (i.e. `List<Integer>` could not be converted into `List<IntType>` even with converter between `Integer` and `IntType`).
- What test framework should be used?
- Might need to add logging/debug info
- The module information should be improved. The inputs outputs names are not very informative.
-  The current design of setting inputs/getting outputs could easily break if those names are changed. Should we just assume they will never change, or should those libraries (i.e. Ops) also take care of all the client implementation?
