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

On some systems, you may need to change the value of `tmpDir` in `imagej-server.yml`.

## Usage

### [Interactive Client in Python](clients/python)

Uses a Python wrapper for the web API. Clients for more languages are coming. 

### APIs

(try [Postman](https://www.getpostman.com/) if not already):

- `curl HOST/modules`

 Returns a list of modules at `HOST`. By default, imagej-server exposes its API at `localhost:8080`, which will be used throughout this documentation.

- `curl HOST/modules/'ID'`
  - example:

    ```
    $ curl HOST/modules/'command:net.imagej.ops.math.PrimitiveMath$IntegerAdd'
    ```

 Returns detailed information of a module specified by `ID`. Notice that `ID` should be single quoted to escape special characters such as $.

- `curl -XPOST -H "Content-Type: application/json" -d '{INPUTS}' HOST/modules/'ID'?[process=PROCESS]`
  - example:

    ```
    $ curl -XPOST -H "Content-Type: application/json" -d '{"a":1,"b":3}' \
    > localhost:8080/modules/'command:net.imagej.ops.math.PrimitiveMath$IntegerAdd'?process=false
    {"result":4}
    ```

 Executes a module with `INPUTS` in JSON format. The optional query parameter `process` determines if the execution should be pre/post processed.

- `curl -F "file=@PATH/TO/FILE" HOST/io/file`
  - example:

    ```
    $ curl -F "file=@/tmp/about4.tif" localhost:8080/io/file
    {"id":"object:0123456789abcdef"}
    ```

 Uploads a file to server and a 16-bit lowercase alphanumeric ID with prefix `object:` will be returned as a JSON string. The ID will be used in module execution to represent the file. Currently only supports uploading images.

- `curl -XPOST HOST/io/file/ID?format=FORMAT`
  - example:

    ```
    $ curl -XPOST localhost:8080/io/object:0123456789abcdef?format=png`
    {"filename":"asdf1234.png"}
    ```

 Request download of a file specified by ID. The object will be saved into a file on the imagej-server side with FORMAT. The filename is returned.

- `curl -O HOST/io/file/FILENAME`
  - example:

    ```
    $ curl -O localhost:8080/io/asdf1234.png
    ```

 Download the file with `FILENAME` from the server.

- `curl -XDELETE HOST/admin/stop`

 Stop the imagej-server gracefully without shutting down the imagej runtime.

## Notes and memo

- Ops that need to be initialized are not working when run as module. See the HACK in `ModulesResource`
- The converters from `Number` to `NumericType` could be refined and considered moving to other projects such as SciJava.
- Do we have better ways to do the I/O? such that no file is needed to store to the disk?
- It might be a good idea for the client to provide optional language-neutral type information for module execution, so that we can support more complex data structures without too much guessing. This would also solve the problem of nested special object (i.e. `List<Integer>` could not be converted into `List<IntType>` even with converter between `Integer` and `IntType`).
- What test framework should be used?
- Might need to add logging/debug info
- The module information should be improved. The inputs outputs names are not very informative.
-  The current design of setting inputs/getting outputs could easily break if those names are changed. Should we just assume they will never change, or should those libraries (i.e. Ops) also take care of all the client implementation?
