## HandBag - User Guide

HandBag is a light-weight GUI tool for manually assembling and transferring BagIt packaged content.
It has a self-contained application binary for all major platforms: Windows, Mac, and Linux
and has no external dependencies (e.g. installing Java).

### Table of Contents
1. [Basic Operation](#basic-operation)
2. [Quick Start](#quick-start)

### Basic Operation

HandBag has various advanced capabilities, such as profile-aware metadata constraints, fetch file
handling, etc. but all these features are optional, and not required to master to use the tool
productively. HandBag employs a sensible set of default configuration values, and you will not
typically need to adjust (or even understand) them to perform basic activities.

### Quick Start

To simply collect a few payload files, add some descriptive metadata (using the reserved
metadata elements), then transfer the serialized bag to a destination, follow these easy steps:

1. Launch the application, which will open to the _Basic_ settings tab.
2. Assign a name to the bag.
3. Enter or browse for a destination directory.
4. Switch to the _Payload_ tab, and 'drag and drop' from anywhere on the desktop to the
   area under the _data_ folder icon.
5. Switch to the _Metadata_ tab and enter or select desired information.
6. Press the _Destination_ button to the right of the tabs.

### Primary Use-Case

HandBag is a tool for selecting resources from the local (or an accessible mounted) file system to be placed
in a directory structure conforming to the BagIt specification, describing this resource set,
and transferring/transmitting it to a local or remote destination, in a compressed package, or as a
loose directory.

Visually, the UI operates roughly from left to right, starting with the _Settings_ tab, where you specify
the minimum information (name for the bag, destination) necessary for a transfer. Next, you can select
files from the file system, and drop them into the _Payload_ tab area. Finally, the _Metadata_ tab exposes
a property editor where you can describe the bag. In fact, you may enter metadata or payload files in any order,
moving back and forth across tabs.

When the bag is complete, there are just two alternative actions to perform, using the buttons to the right of the
tabs. You may either discard your work (_Trash_), or press the _Destination_ button to effect the transfer. Note that
discarding the bag will _not_ delete or remove any source files, so it is a safe operation. It merely clears your workspace
to start another bag with a single button press.

[def]: #quick-start