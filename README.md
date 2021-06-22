# ZoarialIoT-Java-Node
>ZoarialIoT is currently in its early alpha stages.

ZoarialIoT is an alternative to the other IoT protocols. ZoarialIoT is designed to be flexable, while also providing good security.
ZoarialIoT is designed to be completely local, completely configurable, secure by default, and distributed with some centralization.


## Overview

ZoarialIoT recognizes nodes, actions, and (in the future) events.

- Nodes are the indiviual devices which communicate with the ZIoT protocol. (There are 3 types of nodes which will be explained [later](#Nodes))
- Actions are owned by nodes. Each action has arguments, return value(s), and security values. (This will be explained [later](#Actions))
  - Eventually may be a part of 'subsystems'. Subsystems would group actions and provided some shared resources inside the node, but invisble to the outside.
- Events (or hooks) are something nodes subscribe to. When a node publishes a hook, other nodes may subscribe and automate certain tasks. (Explained [here](#Events))

## Nodes
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


## Actions
Each action has 0 or more arguments and a return type. (Currently, only strings are allowed)
Each action has a security level, an encrypted flag, and local flag.

### Action Security
There are a few options to configure the security on an action

#### Security Level
The security level tells the server what level of authentication is needed
> Connection from localhost overrides the need for this
  - Level 0: No authentication
  - Level 1: Basic authentication - HTOP/TOTP
  - Level 2: Strong authentication - Public/Private Keys

#### Encrypted toggle
The encrypted toggle says whether the node should be contacted over an encryped channel. (This will likely be SSH-like)

#### Local toggle
The local toggle says whether the action has to be run from localhost or not.

When true, the action can _**NOT**_ be run remotely


## Server Configuration
The server configuration file is a java-properties file located in /etc/.
Most configuration options are located in `app`, `app.device`, or `app.logging`.

`app` contains metadata about the config file.
- version - config/server version
- name - the app name. If this is something unexpected, then the config file is ignored.

`app.device` contains information the server needs to know in order to act properly.
- is_volatile - Describes whether this node often disconnects from the network. E.g. a device inside of a car.
- is_head_capable - Describes whether this device could be the head of the network. (See [head-node behaviour]())
- hostname - The hostname used in just the ZIoT network
- uuid - The node's UUID. This may be auto-generated if not provided.
- message_timeout - currently unused
- node_type - node type. (See [Nodes](#Nodes))
- port - Server port. Default is 9494.
- ping_timeout - Currently unused, but will used to determine whether a node is online or not.
- network_device - Is used to select a network device, eth0, wlan0, etc. The server will try and determine an IP from that device.
