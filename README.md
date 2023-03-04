# Wireless-Network-Routing-Simulation
This project implements a simulation of a routing protocol used in wireless networks. It was implemented as a part of coursework on Advanced Computer Networks


## Project Overview
The network simulated has unidirectional channel(links). Unix processes correspond to nodes in the network and files correspond to channel in the network. There are atmost 10 nodes in the network.


## Process Names and Arguments
Nodes send data messages to each other. All nodes can receive the data messages.

If node has data to send, the command line arguments will be as follows
* java Node ID duration dest "message" &

where ID is the identifier of the process, duration is the no of seconds before the node kills itself, dest is the node to which data message will be sent (0-9), "message" are contents of data message sent to dest, which is an arbitrary string of text

If node doesn't have data to send, its command line args are as follows:
* java Node ID duration -1 &

Controller is executed as follows:
* java Controller duration &


## Channels, Process and Files
Assume there is a channel from y to x (y ---> x). Then we can say that
* y is an incoming neighbor of x
* x is an outgoing neighbor of y

Each node periodically broadcasts a hello message. E.g when y sends a hello message, x receives it and it adds to its list of know incoming neighbors.

Each node has a output file called output_x.txt and an input fle called input_x.txt, where x is from 0-9
These files consist of a sequence of messages, each of which is in a seperate file in the line.

In wireless network, each message sent by a node is received by all its outgoing neighbors but a node is not aware of its outgoing neighbors. So we use a controller node. The controller has an input file (written before execution of simulation) that contains network topology which describes which nodes are neighbors of each other. This file can be accesed only by controller.

Therefore, if node y sends a message, it appends it to its output file output_y.txt
Then controller will read this file, check network topology, and if x is an outgoing neighbor if y, append the message to file input_x.txt

Thus we simulate the behavior of wireless networks i.e, when node transmits the message over air, all other nodes can hear it. Since this is a simualtion and we are using text files and not real transmitters, we use controllers to help distribute message to neighbors.

The topology file for controller is called topology.txt and controller opens it at beginning of execution. The file consists of list of pairs, each pair corresponding to a unidirectional channel.

Each node x also opens a file called x_received.txt. When x receives a data message from node z, it will write this string to this file

Example: There are three nodes - 0,1 & 2, nodes 0 & 1 want to send message to 2. Then commands executed would be:
* java Controller 100 &
* java Node 0 100 2 "message from 0 to 2" &
* java Node 1 100 2 "message from 1 to 2" &
* java Node 2 100 -1

To avoid time required to write the commands, these are written in bash files called scenario.
So you just have to run the bash files after which we can see all nodes and controller processes running in background.
bash scenario1


## Hello Protocol
Every 5 secs, each node sends a hello message with format: hello ID
where ID is the id of node sending hello message

If after 30 secs, node does not receive messsage from some neighbor, it assumes the node no longer exists


## In-Tree Protocol
This protocol helps each node to compute the shortest path from other nodes to itself

Consider the network shown below. Arrows correspond to unidirectional links, straight lines correspond to bidirectional links

![Network Topology](/assests/images/network_topology.png)

Our first objective is to build in-tree for every node. In-trees are spanning trees that contain path from all other nodes to the node.
For example, in-trees of node A and C are shown below. We use minimum hop routing, so trees contain minimum hop path from all nodes to root.

![In-trees](/assests/images/node_intrees.png)

To achieve this, nodes periodically send their in-tree (or any small part of computed in-tree) to all their neighbors (nodes add it in their output files, and controller makes a copy in input files of all outgoing neighbors). In-tree of each node is initially empty.

When a node combines its in-tree with in-tree of its neighbors, it improves its own in-tree. Eventually the in-tree of all nodes reaches a stable configuration. 

For example, assume node D just joined the network and its in-tree is empty. (i.e contains only node D itself). Assume D then receives copy of in-tree of node C above (note in-tree of C will not contain D since D just joined the network). It new tree T1 is pictured below.

![In-tree T1](/assests/images/intree_t1.png)

Let T1 be the current in-tree of D. Now D receives copy of in-tree of A. D has to compare its own in-tree with that of A to see if improvements are possible. Before that, it modifies received in-tree of A in two ways
1. it removes entire subtree of D on this tree (which in this case is just edge D --> A)
2. it adds edge A --> D

T2 is tree obtained after above modifications as shown below. Now D combines T1 and T2 to obtain its new in-tree. This merging is done level by level, i.e we first consider nodes which are one hop away, then nodes which are two hops away etc.

![In-tree T2](/assests/images/intree_t2.png)

Nodes 1 hop away: A(in T2) and C(in T1)
We consider nodes in order of their id
Therefore, we add edge A-->D of T2 to our final tree, and we remove subtree rooted at A from T1
Next we add edge C-->D of T1 to our final tree, and remove subtree rooted at C from T2
Results shown below

![Final-tree](/assests/images/intree_one_hops.png)

Next, we consider nodes two hops away from D: B and E at T1, and B at T2
We start with B(lowest id), it has same hop count in T1 and T2, but parent in T2 is A which is less than parent in T2 which is C, hence we break ties according to parent, so we choose edge of B in T2 and remove from T1 the subtree of B. Result show below

![Final-tree](/assests/images/intree_two_hops.png)

We are left with only one node at level 2, E in T1, so we add edge E-->C from T1 to our final tree and remove from T2 the subtree rooted at E.
End result is shown below

![Final-tree](/assests/images/intree_hops_two.png)

Final tree is the union of above edges as shown below

![Final-tree](/assests/images/final_tree.png)

Every 10 secs, each node transmits a message containing its in-tree. Format of the tree of D above is as follows:
intree D (A D)(C D)(E C)(B A)
i.e, the word intree, root node, and then list of edges

In addition to the periodic transmission of the in-tree, if the in-tree of node changes in any way, immediately a new in-tree message is transmitted. If after 20 secs, a node does not receive intree messages from some neighbor, it assumes the neighbor no longer exists.


## Routing of data messages
We use source routing. We assume that if there is a path from node A to another node B, then there is also a path from node B to node A (not necessarily a direct path).

Consider node A sending message to node E. According to A's in-tree, E can reach A via path E-->B-->A. So, A will route the message towards B. This cannot be done directly since according to in-tree of B, the path from A to B is A-->C-->B. A thus uses source routing to send the message to B, i.e message travels A-->C-->B first, then B sends message to E.

Wnen message reaches B, B repeats the process, and looks in its own in-tree to see how it can reach E. As E is an incoming neighbor of B, so B source routes the message to E.

Format of data message is as follows
* data src i1 i2 ... dest begin "message"
data is keyword, src is id of node sending message, dest is the dest id of message, begin is keyword, what follows after begin is actual text string we want to send from A to E, i1 i2 ... list of intermediate nodes(source routing)

For example, like in example above, when A sends message to E, the message goes through path A B E (backwards along in-tree of A), but to reach B, we need path A C B, hence A sends message as follows:
* data A C B E begin

When data arrives at C, C removes itself from list and sees how it can reach B. From its own in-tree, it knows that B is an incoming neighbor. From in-tree of B, it realizes that C can reach B directly(edge C-->B is in in-tree of B), and sends it out
* data A B E begin

When B receives the data, it must determine how to reach E, so now it looks at its own in-tree, and figure out which incoming neighbor is in the path from E to B, from in-tree of X, figure out a soure-routing path to reach X. In this case, X is E itself, and path from B to E is just B-->E, so B sends out the message
* data A E begin