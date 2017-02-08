# Postman collection for imagej-server

[Postman](https://www.getpostman.com/) is a powerful GUI platform for API development and testing on different systems.

A [Postman collection](https://www.getpostman.com/docs/collections) is a group of individual requests. The collection here contains a small list of requests that demonstrate the basic usage of the imagej-server APIs.

## Usage

Once installed Postman, import the [collection](imagej_server.postman_collection.json) and [environment](imagej_server.postman_environment.json) JSON files by clicking the **Import** button at the top left corner. Select `imagej_server` as the environment. Then go to the **Body** tab of the `upload_img` request, and choose a file for upload. You can press `ctrl + s` to save this request so that you don't have to repeat this process next time.

Then launch an imagej-server instance. Try clicking on different requests and hit the **Send** button to see the results. Notice that some requests are chained so that one needs to be run before another. For example, you need to run `upload_img` before `retrieve_img` or `create_img_from_img`.

![Postman screenshot](screenshot.png?raw=true "Example Postman interface with imagej_server collection")

The collection is actually setup as an integration test suit. You can click the **Runner** button next to the **Import**, and hit **Start Test** on the `imagej_server` collection with `imagej_server` as the environment. This will not show results from individual requests, but will show some statistics and whether they pass tests.

After getting more familiar with Postman, you can try to create your own requests. Using global variables in the **Tests** tab will be very helpful for handling random object IDs. Checkout and [this document](https://www.getpostman.com/docs/environments) for more details.
