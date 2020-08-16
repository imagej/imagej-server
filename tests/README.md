This folder contains integration tests for the ImageJ Server.

To run them, first start the server:

    mvn -Pexec

And then in a separate console window:

    cram tests

You'll need [cram](https://bitheap.org/cram/) at version 0.6.
One way to install it is via `pip install cram==0.6`.
