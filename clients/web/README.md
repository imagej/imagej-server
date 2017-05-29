# Web Client for ImageJ Server

This client provides a basic web interface for the ImageJ Server.

## Requirements

Needs HTML5 support. Currently only tested under Chrome 58.0.3029.110 (64-bit) in local environment.

## Usage

Launch ImageJ Server locally on port 8080 (default). Open [`index.html`](index.html) on appropriate browser. Use the top-left pane to select modules (for now use browser search function).

After selection, use the bottom-left pane for parameter I/O and execution. See module `WebClientDemo` for more details.

Use the right pane for image upload. Objects generated during module execution will also appear here. Click "Refresh" if any image is updated during execution. Drag and drop any image to the input fields in the module pane if an image (or general object) is expected. You can click on the image to see its details.

When an image is the output, you can view it in different formats by changing the box next to the "View As" botton.  

## Memo

- Add menu similar to ImageJ (need server side API support)
- Make UI more user friendly (not programmer oriented)
- Use more mature framework for web image management (thumbnail caching, image scaling, etc.)
- Refactor client.js if it gets too large
- Organize styles.css
