List modules:

  $ curl -s localhost:8080/modules | json_pp | grep '^ *"command:' | sed 's/^ *//' | sort | head -n5
  "command:io.scif.commands.OpenDataset",
  "command:io.scif.commands.SaveAsImage",
  "command:io.scif.commands.SaveDataset",
  "command:io.scif.commands.SaveImage",
  "command:net.imagej.app.AboutImageJ",

Inspect a module:

  $ curl -s localhost:8080/modules/command:net.imagej.app.AboutImageJ | json_pp | grep '"\(identifier\|name\)"' | sed 's/^ *//'
  "identifier" : "command:net.imagej.app.AboutImageJ",
  "name" : "context",
  "name" : "log",
  "name" : "appService",
  "name" : "ioSrv",
  "name" : "dataSrv",
  "name" : "dispSrv",
  "name" : "rendSrv",
  "name" : "display",

Execute a module:

  $ curl -s -XPOST -H "Content-Type: application/json" -d '{"a":1,"b":3}' 'localhost:8080/modules/command:net.imagej.ops.math.PrimitiveMath$IntegerAdd?process=false'
  {"result":4} (no-eol)
