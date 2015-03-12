##Chord Peer-to-Peer Distributed Hash Table implementation

###Design

There are three main components of this Chord DHT Implementation:

1. The Super Node (SuperNode.java)
  * SuperNode is a well-known node in the DHT, all Nodes and Client knows its IP Address when they start running to connect to SuperNode through RMI. The SuperNode binds itself as the interface name "SuperNodeDef" on the rmiregistry at the default port 1099, on its running host. So Nodes and Client can do RMI calls. 

  * For the Nodes: 
    * SuperNode serves as a bootstrapping location for each node when joining the DHT, each node would connect to the SuperNode, which will hash the node's <IP Address + Port> string combination using SHA-1 Hash Function, into a unique ID within the key space of 2^m. (Ex. HashResult % 32). 
    * To ensure this ID is unique, the SuperNode keeps the ID List of Nodes currently in the DHT. If the newly hashed ID collides with one of these IDs, the SuperNode hashes once more the previously hashed result, and again until a unique ID is found for this node. SuperNode then returns this ID back to the requesting Node, along with the ID of its predecessor (by looking at the ID list) and wait for the node's Post-Iniatiation confirmation call.

  * During the process mentioned above, the SuperNode blocks with synchronization lock, so any other Node requesting to join/get its ID is returned with "NACK" response. Only after the Post-Initiation Call of the current requesting Node is received, the SuperNode releases this lock and other nodes can now ask to join. Both these getNodeInfo and finishJoining calls are done with RMI.

  * For the Client: SuperNode serves as point of contact into the DHT for the client. It returns all the nodes contact when Client wants to contact all of them to retrieve FingerTable for printing. It returns a random Node's contact each time the Client needs to look up or insert a key into the DHT. Both getNodeList and getRandomNode calls are done with RMI.
    
2. The Node in DHT (NodeDHT.java)

  * Each Node has its own ID and FingerTable information about the DHT, it contains a list of keys (word:meaning pairs) that it is responsible for. 

  * With the SuperNode: 
    * When a Node starts, it looks up at the rmiregistry running at the known SuperNode's host to make RMI calls to the SuperNode. It first calls getNodeInfo with its IPAddress and Port number, receiving the ID and its predecessor ID, it starts its FingerTable with all entries containing its own information. If the SuperNode returns NACK, it exits and needs to run again later. 
    * It starts a separate thread to finish its FingerTable initialization and updating others. Once this is done, it calls finishJoining to let the SuperNode knows it's in. Then the Node starts another thread for listening to other Nodes or Client, and spawn a new thread for each connection received.

  * With other Nodes: 
    * Inter-node communications are done with TCP Sockets Programming, sending and receiving data serialized as String message. For the init_finger_table call, since it already knows the predecessor given by the SuperNode, it contacts its predecessor to get its successor (or Finger[1].node) information as well as resetting its predcessor and its Successor's predecessor information. Then it goes through its Finger Table and update each entry. Each node then update_other Nodes. 
    * Beside init_finger_table and update_other, several methods are implemented such as setPredecessor, getPredecessor, find_successor, getSuccessor, closet_preceding_finger, update_finger_table as outlined in the Chord paper. Each time a connection comes in with request message, the method considerInput(message) parse the request and refer to the appropriate method above. Response is then combined as a string and sent back to the requesting Node.

  * With the Client: 
    * Node-Client communication is also done with Sockets. Again, considerInput() refers to the appropriate method to respond to the Client's. The Node has method returnAllFingers which returns all its information and fingerTable to the client as string, for printing. It has tryInsert (which calls insertKey to add to a node's key list, after routing around the DHT to find this responsible node for this given key) and lookupKey (which calls getWord, after routing around the DHT, thats check all keys in that responsible Node's key list to find the matching word) that respond to the Client's need for Insertion and Lookup of words. Routing around the DHT is done with the find_successor method implemented. Each visit to a node to find_successor is printed out to the command line (for log purpose)  

3. The Client (ClientNode.java)

  * The Client UI has 5 options: Inserting the whole given Sample Dictionary file, Look up a word for its meaning, Insert a word:meaning pair into the DHT, Print all nodes information and Exit. 	

  * With the SuperNode: when the Client starts, it looks up at the rmiregistry running at the known SuperNode's host to make RMI calls to the SuperNode later.

  * With the Node:
    * Inserting the Sample Dictionary file: it parses the whole file, and for each line: it hashes the Word string using SHA-1 hash into a key within the 2^m key space. The Client contacts the SuperNode through RMI to get a random node's contact. It then establishes a connection to this Node, and send a message tryInsert with the key, the parsed Word and Meaning and wait for the Node's response as confirmation.
    * Lookup a Word: the user can enter a word string, then the Client hashes this into a key. Again, it asks the SuperNode for a random Node's contact and establishes a connection to this node to send a message lookupKey with this key. It waits for the Node to return the retrieved Meaning string from the DHT and prints out the command line. If it's not there, the string is returned as "Not Found!".
    * Inserting a word: the User can enter a Word string, then its Meaning string. After that, the same is done as part (i) where the client hashes the Word and sends the tryInsert message to a random Node.


###Execution Setup

1. Sample Setup with multiple machines:
  * 1 for SuperNode 
  * 1 for ClientNode
  * 5 for NodeDHT nodes 

2. For machine 1 (SuperNode)

  Execute:

        ./startRmiRegistry.sh

  to start the rmiregistry for the SuperNode to bind to, if this gives an error, it means it's already running, that is fine & continue to the next command below:

        ./compile.sh

        java -cp bin/ -Djava.security.policy=src/policyfile SuperNode

3. For machine 2 (ClientNode)

  Execute:

        java -cp bin/ -Djava.security.policy=src/policyfile ClientNode [SuperNode's IP Address]

4. For all other machines (NodeDHT)

  Execute at each machine, to start a Node at each host:

        java -cp bin-Djava.security.policy=src/policyfile NodeDHT [Port Number] [SuperNode's IP Address]

