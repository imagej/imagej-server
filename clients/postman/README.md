# Postman collection for imagej-server

[Postman](https://www.getpostman.com/) is a powerful GUI platform for API development and testing on different systems.

A [Postman collection](https://www.getpostman.com/docs/collections) is a group of individual requests. The collection here contains a small list of requests that demonstrate the basic usage of the imagej-server APIs.

## Usage

First, install Postman. Once inside, import the collection by clicking the **Import** button at the top left corner. Then go to the **Body** tab of the `upload_img` request, and choose a file for upload. You can press `ctrl + s` to save this request so that you don't have to repeat this process next time.

If you already have an imagej-server instance running, then you are ready to go! Try click on different requests and hit the **Send** button to see the results. Notice that some requests are chained so that you need to run one before another. For example, you need to run `upload_img` before running `retrieve_img` or `create_img_from_img`.

The collection is actually organized such that when run in order, they will be properly chained and all should produce `200` status codes. To do this more efficiently, you can click the **Runner** button next to the **Import**, and hit **Start Test** on the `imagej_server` collection you just imported. This will not show results from individual requests, but will show some statistics and whether they pass tests.

![Postman screenshot](screenshot.png?raw=true "Example Postman interface with imagej_server collection")

After getting more familiar with Postman, you can try to create your own requests. Using global variables in the **Tests** tab will be very helpful for handling random object IDs. Checkout my sample requests and [this document](https://www.getpostman.com/docs/environments) for more details.