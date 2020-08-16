Upload an image:

  $ result=$(curl -s -F "file=@$TESTDIR/../src/test/resources/imgs/about4.tif" localhost:8080/objects/upload)
  $ object=$(echo "$result" | grep -o 'object:[a-zA-Z0-9]*')
  $ echo "${object%%:*}"
  object

Validate uploaded image:

  $ curl -s "localhost:8080/objects/$object" | json_pp | sed "s/$object/THE_OBJECT_ID/"
  {
     "class" : "net.imagej.DefaultDataset",
     "created_at" : ".*", (re)
     "created_by" : "uploadFile:filename=about4.tif",
     "id" : "THE_OBJECT_ID",
     "last_used" : null
  }

Download the image as a PNG:

  $ curl -s "localhost:8080/objects/$object/png" | file -
  /dev/stdin: PNG image data, 800 x 533, 8-bit/color RGB, non-interlaced

  $ curl -s "localhost:8080/objects/$object/png" | md5sum
  780175b46bf5d2329bab4b19759fc2d7  -

Delete the image:

  $ curl -s -XDELETE "localhost:8080/objects/$object"

Verify it was deleted:

  $ curl -s "localhost:8080/objects/$object" | json_pp
  {
     "code" : 404,
     "message" : "ID does not exist"
  }
