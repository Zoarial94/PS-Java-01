# ZoarialIoT-Java-Node
>ZoarialIoT is currently in its early alpha stages.

ZoarialIoT is an alternative to the other IoT protocols. ZoarialIoT is designed to be much more flexable, while also providing good security.
ZoarialIoT is designed to be completely local, completely configurable, secure by default, and distributed with some centralization.


## Overview

ZoarialIoT recognizes nodes, actions, and (in the future) events.

- Nodes are the indiviual devices which communicate with the ZIoT protocol. (There are 3 types of nodes which will be explained [later](#Nodes))
- Actions are owned by nodes. Each action has arguments, return value(s), and security values. (This will be explained [later](#Actions))
  - Eventually may be a part of 'subsystems'. Subsystems would group actions and provided some shared resources inside the node, but invisble to the outside.
- Events (or hooks) are something nodes subscribe to. When a node publishes a hook, other nodes may subscribe and automate certain tasks. (Explained [here](#Events))

### Nodes

There are 3 types of nodes: Head, Nonhead, and satellites.
- Head
  - Most complex and heavy-weight node
  - Provides the centralized benefits
  - Has actions, and can run other node's actions.
    - May also create a chain of actions
  - Makes a best-effort to keep an updated list of the network's devices and actions
  - Intended use: Desktop, raspberry pi, etc.
- Nonhead
  - Simplest 'real' node
  - Has actions and events, and can optionally run other node's actions
  - Intended use: arduino, ESP, etc.
- Satellite
  - Not really a node
  - Has no actions/events
  - Can call other actions or monitor a network
  - Could potentially control a network remotely
  - Intended use: managment website, mobile app, etc.
 
