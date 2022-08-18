[![](https://github.com/imagej/imagej-server/actions/workflows/build-main.yml/badge.svg)](https://github.com/imagej/imagej-server/actions/workflows/build-main.yml)
[![developer chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://imagesc.zulipchat.com/#narrow/stream/327236-ImageJ2)

# ImageJ Server

This is a RESTful image server backed by ImageJ.

To provide easy-to-use and highly reusable image processing functionalities is
a [central goal of ImageJ](https://imagej.net/Ops). As a result, the components
of ImageJ are [modular](https://imagej.net/Architecture#Modularity), to
make them easily accessible from other Java programs. However, for programs
written in other languages besides Java, the interaction becomes complicated.
In order to mitigate this problem, this RESTful image server is offered as a
universal interfacing layer.

See the [Rationale](https://github.com/imagej/imagej-server/wiki/Rationale)
page of this repository's wiki for a longer discussion of cross-language
integration and interoperability.

This is currently only a prototype! Testing needed!

## Launching

The server can run with a graphical display, or in
[headless](https://imagej.net/Headless) mode.

* With a graphical display, remote commands can affect the ImageJ graphical
  user interface (GUI). E.g., images can be displayed to the user.
* In headless mode, there is no GUI. E.g., in a cluster computing environment,
  each node can have its own local ImageJ Server instance.

There are several ways to invoke the server:

<details><summary><b>If ImageJ is already running</b></summary>

* Use _Plugins &#8250; Utilities &#8250; Start Server_
  to make ImageJ start listening for remote commands.
* Use _Plugins &#8250; Utilities &#8250; Stop Server_
  to shut down the server. The local ImageJ will continue running.

You must enable the Server [update site](https://imagej.net/Update_Sites) first.

</details>
<details><summary><b>Launch via jgo</b></summary>

The [jgo](https://github.com/scijava/jgo) launcher makes it easy to launch the
ImageJ Server. No need to explicitly clone the repository or download any JARs.

After installing jgo, add the following stanza to your `~/.jgorc` file:
```ini
[repositories]
imagej.public = https://maven.imagej.net/content/groups/public
```

Then invoke the server with a graphical display as follows:
```
jgo net.imagej:imagej-server
```

Or in headless mode:
```
jgo -Djava.awt.headless=true net.imagej:imagej-server
```

</details>
<details><summary><b>Launch from CLI via Maven</b></summary>

Clone this repository. Then start the server from the CLI _in headless mode_:
```
mvn -Pexec
```

</details>
<details><summary><b>Launch from IDE</b></summary>

Clone this repository, import the project, then run the class
`net.imagej.server.Main`. The server will launch _in headless mode_.

</details>
<details><summary><b>Launch via the ImageJ Launcher</b></summary>

Enable the Server [update site](https://imagej.net/Update_Sites).

Then launch ImageJ with a graphical display:
```
./ImageJ --server
```

Or in headless mode:
```
./ImageJ --server --headless
```

See also the [ImageJ Launcher](https://imagej.net/Launcher) documentation.

</details>
<details><summary><b>Including additional plugins</b></summary>

If you want to make additional ImageJ plugins (e.g. plugins from
[Fiji](https://github.com/fiji)) available remotely, you can include the
additional components on the runtime classpath.

One easy way is via the `jgo`-based launch mechanism with the `+` syntax.
For example:

```
jgo sc.fiji:fiji+net.imagej:image-server
```

Another way is make your own Maven project depending on
`net.imagej:imagej-server` and other things, with a `main` entry point
that invokes `net.imagej.server.Main.main(String[])`.

</details>

## Usage

### Python Client

The [pyimagej](https://github.com/imagej/pyimagej) module includes a Python wrapper for the web API.

### [Postman Collection](clients/postman)

A collection of sample API calls to imagej-server using [Postman](https://www.getpostman.com/).

### Web client with GUI

Installation

- Check that you have got *Node.js* and *npm* installed ([instructions](https://www.npmjs.com/get-npm)).
- Open a command prompt, navigate to `clients\webNew` and run `npm install`. This will install all required *Node.js* packages.

Compilation and execution (development)

- Open a command prompt, navigate to `clients\webNew` and run `npm start`.
- Open a web browser and navigate to `localhost:4200`.

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

- __GET__ /objects

 Lists all object IDs available on imagej-server.

 `$ curl localhost:8080/objects`

- __GET__ /objects/*{id}*

 Shows the information of an object.

 ```
 $ curl localhost:8080/objects/object:1234567890abcdef
 {"class":"net.imagej.DefaultDataset","created_at":"Sun Jan 01 00:00:00 CST 2017"}
 ```

- __DELETE__ /objects/*{id}*

 Delete one object from imagej-server.

 `$ curl -XDELETE localhost:8080/objects/object:1234567890abcdef`

- __POST__ /objects/upload?[type=*{type}*]

 Uploads a file to server. A 16-bit lowercase alphanumeric ID prefixed with `object:` will be returned as a JSON string. The ID can be used in module execution to represent the file. Currently only supports uploading images and tables in text. An optional query parameter `type` could be provided as hint to file type. If it is empty, filename would be used for guessing.

 ```
 $ curl -F "file=@src/test/resources/imgs/about4.tif" localhost:8080/objects/upload
 {"id":"object:0123456789abcdef"}
 ```

- __GET__ /objects/*{id}*/*{format}*?[&key=*{value}*]...

 Download an object in some specific format from the server. Optional query parameters will be used for configuration depending on the type of the object.

 `$ curl localhost:8080/objects/object:0123456789abcdef/png`

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
- The current design of setting inputs/getting outputs could easily break if those names are changed. Should we just assume they will never change, or should those libraries (i.e. Ops) also take care of all the client implementation?
