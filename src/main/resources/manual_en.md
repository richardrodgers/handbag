# HandBag - User Guide #

HandBag is a light-weight GUI tool for manually assembling and transferring BagIt packaged content.

It has a self-contained application binary for all major platforms: Windows, Mac, and Linux

and has no dependencies (e.g. installing Java).

## Basic Operation ##

HandBag has various advanced capabilities, such as profile-aware metadata constraints, fetch file
handling, etc. but all these features are optional, and not required to master to use the tool
productively. HandBag employs a sensible set of default configuration values, and you will not
typically need to adjust (or even understand) them to perform basic activities.

### Quick Start ###

To simply collect a few payload files, add some descriptive metadata (using the reserved
metadata elements), then transfer a serialized bag to a destination, follow these easy steps:

1. Launch the application, which will open to the _Basic_ settings tab.
2. Assign a name to the bag.
3. Enter or browse for a destination directory.
4. Switch to the _Payload_ tab, and 'drag and drop' from anywhere on the desktop to the
   area under the _data_ folder icon.
5. Switch to the _Metadata_ tab and enter or select desired information.
6. Press the _Destination_ button to the right of the tabs.