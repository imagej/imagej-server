[![](http://jenkins.imagej.net/job/ImageJ-Server/lastBuild/badge/icon)](http://jenkins.imagej.net/job/ImageJ-Server/)
[![Join the chat at https://gitter.im/imagej/imagej-server](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/imagej/imagej-server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# ImageJ Server

This is a RESTful image server backed by ImageJ.

It is currently only a prototype! Testing needed!

## Launching

To start the server from the CLI, use:

```
mvn -Pexec
```

Or from an IDE, execute the class `net.imagej.server.ImageJServerApplication`.

On some systems, you may need to change the value of `tmpDir` in `imagej-server.yml`.

## Usage

APIs (try [Postman](https://www.getpostman.com/) if not already):

- `curl localhost:8080/modules`

 Returns a list of modules

- `curl localhost:8080/modules/{id}`

 Returns detailed information of a module

- `curl -XPOST -H "Content-Type: application/json" -d '{"inputs":{INPUTS}}' localhost:8080/modules/{id}`

 Executes a module with inputs from json

- `curl -F "file=@PATH/TO/IMAGE" localhost:8080/io/file`

 Uploads an image to server. id of the uploaded image will be return in form of `{"id":"_img_ID"}` The id will be used in module execution to represent the image.

- `curl -XPOST localhost:8080/io/_img_ID?ext=EXTENSION`

 Request download of image specified by ID. The image will be saved into a file in imagej-server side with EXTENSION. The filename is returned.

- `curl localhost:8080/io/FILENAME`

 Download the image with FILENAME form the server. The FILENAME must be the return value from the request download API call.

## Notes and memo

- Ops that need to be initialized are not working when run as module. See the HACK in `ModulesResource`
- The converters from `Number` to `NumericType` could be refined and considered moving to other projects such as SciJava.
- The `Mixins` is the initial attempt for providing json annotation to java types that we could not modify. More Mixins should be implemented for serializing complex types (potentially also for deserializing).
- Do we have better ways to do the I/O? such that no file is needed to store to the disk?
- It might be a good idea for the client to provide optional language-neutral type information for module execution, so that we can support more complex data structures without too much guessing. This would also solve the problem of nested special object (i.e. `List<Integer>` could not be converted into `List<IntType>` even with converter between `Integer` and `IntType`).
- What test framework should be used?
- Might need to add logging/debug info
- The module information should be improved. The inputs outputs names are not very informative.
-  The current design of setting inputs/getting outputs could easily break if those names are changed. Should we just assume they will never change, or should those libraries (i.e. Ops) also take care of all the client implementation?
