import java.rmi.*;
import java.rmi.Naming;
import java.rmi.server.*;
import java.io.*;
import java.net.*;
import java.net.UnknownHostException;
import java.util.*;

//
//
// This is the Code for the Node that is part of the DHT. 
//
//
public class NodeDHT implements Runnable //extends UnicastRemoteObject implements NodeDHTInterface
{

	private int ID;
	private static SuperNodeDef service;

	private Socket connection;
	private static ServerSocket serverSocket = null; 

	private static Node me, pred;
	//static int m = 5;
	//static FingerTable[] finger = new FingerTable[m+1];
	//static int numDHT = (int)Math.pow(2,m);
	private static int m;
	private static FingerTable[] finger;
	private static int numDHT;
	private static List<Word> wordList = new ArrayList<Word>();

	public NodeDHT(Socket s, int i) {
		this.connection = s;
		this.ID = i;
	}

	public static void main(String args[]) throws Exception
	{
		System.out.println(" ***************************************************************************************************");
		// Check for hostname argument
		if (args.length < 3)
		{
			System.out.println("Syntax - NodeDHT [LocalPortnumber] [SuperNode-HostName] [numNodes]");
		System.out.println("         *** [LocaPortNumber] = is the port number which the Node will be listening waiting for connections.");
			System.out.println("         *** [SuperNode-HostName] = is the hostName of the SuperNode.");
			System.exit(1);
		}	
        
        int numNodesRequested = Integer.parseInt(args[2]);
        m = (int) Math.ceil(Math.log(numNodesRequested) / Math.log(2));
	    finger = new FingerTable[m+1];
	    numDHT = (int)Math.pow(2,m);

		System.out.println("The Node starts by connecting at the SuperNode.");
		System.out.println("Establishing connection to the SuperNode...");
		// Assign security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}

		InetAddress myIP = InetAddress.getLocalHost();
		System.out.println("My IP: " + myIP.getHostAddress() + "\n");

		// Call registry for PowerService
		service = (SuperNodeDef) Naming.lookup("rmi://" + args[1] + "/SuperNodeDef");
			
		String initInfo = service.getNodeInfo(myIP.getHostAddress(),args[0]);
		if (initInfo.equals("NACK")) {
			System.out.println("NACK! SuperNode is busy. Try again in a few seconds...");
			System.exit(0);
		} else {
			System.out.println("Connection to the SuperNode established succefully");
			System.out.println("Now Joining the DHT network and receiving the Node ID.");	
		}

		String[] tokens = initInfo.split("/");
		me = new Node(Integer.parseInt(tokens[0]),myIP.getHostAddress(),args[0]);
		pred = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);

		System.out.println("My given Node ID is: "+me.getID() + ". Predecessor ID: " +pred.getID());

		Socket temp = null;
		Runnable runnable = new NodeDHT(temp,0);
		Thread thread = new Thread(runnable);
		thread.start();

		int count = 1;
		System.out.println("Listening for connection from Client or other Nodes...");
		int port = Integer.parseInt(args[0]);

		try {
			serverSocket = new ServerSocket( port );
		} catch (IOException e) {
			System.out.println("Could not listen on port " + port);
			System.exit(-1);
		}
		
		while (true) {
			//System.out.println( "*** Listening socket at:"+ port + " ***" );
			Socket newCon = serverSocket.accept();
			Runnable runnable2 = new NodeDHT(newCon,count++);
			Thread t = new Thread(runnable2);
			t.start();
		}
		//Start the Client for NodeDHT 	
	}

	public static String makeConnection(String ip, String port, String message) throws Exception {
		//System.out.println("Making connection to " + ip + " at " +port + " to " + message);
		if (me.getIP().equals(ip) && me.getPort().equals(port)){
			String response = considerInput(message);
			//System.out.println("local result " + message + " answer: "  + response);
			return response;
		} else {

		Socket sendingSocket = new Socket(ip,Integer.parseInt(port));
		DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));
		
		//System.out.println("Sending request: " + message + " to " + ip + " at " + port);
		out.writeBytes(message + "\n");

		String result = inFromServer.readLine();
		//System.out.println("From Server: " + result);
		out.close();
		inFromServer.close();
		sendingSocket.close(); 
		return result;
		}
	}


public void run() {

	if (this.ID == 0) {

		System.out.println("Building Finger table ... ");
		for (int i = 1; i <= m; i++) {
			finger[i] = new FingerTable();
			finger[i].setStart((me.getID() + (int)Math.pow(2,i-1)) % numDHT);
		}
		for (int i = 1; i < m; i++) {
			finger[i].setInterval(finger[i].getStart(),finger[i+1].getStart()); 
		}
			finger[m].setInterval(finger[m].getStart(),finger[1].getStart()-1); 
		

		if (pred.getID() == me.getID()) { //if predcessor is same as my ID -> only node in DHT
			for (int i = 1; i <= m; i++) {
				finger[i].setSuccessor(me);
			}
			System.out.println("Done, all finger tablet set as me (only node in DHT)");
		}
		else {
			for (int i = 1; i <= m; i++) {
				finger[i].setSuccessor(me);
			}
			try{
			init_finger_table(pred);
			System.out.println("Initiated Finger Table!");
			update_others();
			System.out.println("Updated all other nodes!");
			} catch (Exception e) {}
		}
		try { 
			service.finishJoining(me.getID());
		} catch (Exception e) {}
	}
	else {
		try {
		//System.out.println( "*** A Client came; Service it *** " + this.ID );
		
		BufferedReader inFromClient =
			new BufferedReader(new InputStreamReader(connection.getInputStream()));
		DataOutputStream outToClient = new DataOutputStream(connection.getOutputStream());
		String received = inFromClient.readLine();
		//System.out.println("Received: " + received);
		String response = considerInput(received);
		//System.out.println("Sending back to client: "+ response);

		outToClient.writeBytes(response + "\n");	
		} catch (Exception e) {
			System.out.println("Thread cannot serve connection");
		}

	}
}


	public static String considerInput(String received) throws Exception {
		String[] tokens = received.split("/");
		String outResponse = "";

		if (tokens[0].equals("setPred")) {
			Node newNode = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
		 	setPredecessor(newNode);
		 	outResponse = "set it successfully";	
		}
		else if (tokens[0].equals("getPred")) {
			Node newNode = getPredecessor();
			outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
		}
		else if (tokens[0].equals("findSuc")) {
			Node newNode = find_successor(Integer.parseInt(tokens[1]));
			outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
		}
		else if (tokens[0].equals("getSuc")) {
			Node newNode = getSuccessor();
			outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
		}
		else if (tokens[0].equals("closetPred")) {
			Node newNode = closet_preceding_finger(Integer.parseInt(tokens[1]));
			outResponse = newNode.getID() + "/" + newNode.getIP() + "/" + newNode.getPort() ;
		}
		else if (tokens[0].equals("updateFing")) {
			Node newNode = new Node(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
			update_finger_table(newNode,Integer.parseInt(tokens[4]));
		 	outResponse = "update finger " + Integer.parseInt(tokens[4]) + " successfully";	
		}
		else if (tokens[0].equals("print")) {
			outResponse = returnAllFingers();
		}
		else if (tokens[0].equals("tryInsert")){
			tryInsert(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
			outResponse = "Inserted pair " + tokens[2] + ":" + tokens[3] + " into DHT";
		}
		else if (tokens[0].equals("insertKey")) {
			insertKey(Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
		}
		else if (tokens[0].equals("lookupKey")){
			outResponse = lookupKey(Integer.parseInt(tokens[1]),tokens[2]);
		}
		else if (tokens[0].equals("getWord")) {
			outResponse = getWord(tokens[1]);
		}
		//System.out.println("outResponse for " + tokens[0] + ": " + outResponse);
		return outResponse;
	}

	public static String getWord(String word){
		Iterator<Word> iterator = wordList.iterator();
		while (iterator.hasNext()) {
			Word wordScan = iterator.next();
			String wordMatch = wordScan.getWord();
			if (word.equals(wordMatch)) {
				System.out.println("*** Found at this Node [" + me.getID() + "] the meaning (" 
						+ wordScan.getMeaning() + ") of word (" + word + ")"); 
				return me.getID() + "/" + wordScan.getMeaning(); 
			}
		}
		System.out.println("*** Found its Node [" + me.getID() + "] but No Word ("+word+") Found here!");
		return "No Word Found!";
	}

	public static String lookupKey(int key, String word) throws Exception {
		System.out.println("*** Looking Up starting here at Node [" + me.getID() +
			       	"] for word (" + word + ") with key (" + key + ")");
		Node destNode = find_successor(key);
		String request = "getWord/" +  word ;
		String response = "";
		response = makeConnection(destNode.getIP(),destNode.getPort(),request);
		return response;
	}

	public static void tryInsert(int key, String word, String meaning) throws Exception {
		System.out.println("*** Starting here at this Node ["+me.getID()+"] to insert word ("+word+
				") with key ("+key+"), routing to destination Node...");
		Node destNode = find_successor(key);
		String request = "insertKey/" + key + "/" +  word + "/" + meaning;
		makeConnection(destNode.getIP(),destNode.getPort(),request);
	}

	public static void insertKey(int key, String word, String meaning) throws Exception { 
		System.out.println("*** Found the dest Node ["+me.getID()+"] here for Insertion of word ("
			       	+ word + ") with key ("+key+")");
		wordList.add(new Word(key,word,meaning));
	}

	public static String returnAllFingers(){
		String response = "";
		response = response + pred.getID() + "/" + pred.getIP() + ":" + pred.getPort() + "/";
		response = response + wordList.size() + "/";
		for (int i = 1; i <= m; i++) {
			response = response + finger[i].getStart() + "/" + finger[i].getSuccessor().getID() + "/" 
				+ finger[i].getSuccessor().getIP() + ":" + finger[i].getSuccessor().getPort() + "/";
		}
		return response;
	}

	public static void init_finger_table(Node n) throws Exception {
		int myID, nextID;

		String request = "findSuc/" + finger[1].getStart();
		String result = makeConnection(n.getIP(),n.getPort(),request);
		System.out.println("Asking node " + n.getID() + " at " + n.getIP());

		String[] tokens = result.split("/");
		finger[1].setSuccessor(new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]));
		//printAllFingers();
		
		String request2 = "getPred";
		String result2 = makeConnection(finger[1].getSuccessor().getIP(),finger[1].getSuccessor().getPort(),request2);
		String[] tokens2 = result2.split("/");
		pred = new Node(Integer.parseInt(tokens2[0]),tokens2[1],tokens2[2]);
		
		String request3 = "setPred/" + me.getID() + "/" + me.getIP() + "/" + me.getPort();
		makeConnection(finger[1].getSuccessor().getIP(),finger[1].getSuccessor().getPort(),request3);

		int normalInterval = 1;
		for (int i = 1; i <= m-1; i++) {

			myID = me.getID();
			nextID = finger[i].getSuccessor().getID(); 
			
			if (myID >= nextID)
				normalInterval = 0;
			else normalInterval = 1;

			if ( (normalInterval==1 && (finger[i+1].getStart() >= myID && finger[i+1].getStart() <= nextID))
				|| (normalInterval==0 && (finger[i+1].getStart() >= myID || finger[i+1].getStart() <= nextID))) {

				finger[i+1].setSuccessor(finger[i].getSuccessor());
			} else {

				String request4 = "findSuc/" + finger[i+1].getStart();
				String result4 = makeConnection(n.getIP(),n.getPort(),request4);
				String[] tokens4 = result4.split("/");

				int fiStart = finger[i+1].getStart();
				int succ = Integer.parseInt(tokens4[0]); 
				int fiSucc = finger[i+1].getSuccessor().getID();
				if (fiStart > succ) 
					succ = succ + numDHT;
				if (fiStart > fiSucc)
					fiSucc = fiSucc + numDHT;

				if ( fiStart <= succ && succ <= fiSucc ) {
					finger[i+1].setSuccessor(new Node(Integer.parseInt(tokens4[0]),tokens4[1],tokens4[2]));
				}
			}
		}
	}

	public static void update_others() throws Exception{
		Node p;
		for (int i = 1; i <= m; i++) {
			int id = me.getID() - (int)Math.pow(2,i-1) + 1;
		 	if (id < 0)
				id = id + numDHT; 

			p = find_predecessor(id);

			
			String request = "updateFing/" + me.getID() + "/" + me.getIP() + "/" + me.getPort() + "/" + i;  
			makeConnection(p.getIP(),p.getPort(),request);

		}
	}

	public static void update_finger_table(Node s, int i) throws Exception // RemoteException,
	{

		Node p;
		int normalInterval = 1;
		int myID = me.getID();
		int nextID = finger[i].getSuccessor().getID();
		if (myID >= nextID) 
			normalInterval = 0;
		else normalInterval = 1;

		//System.out.println("here!" + s.getID() + " between " + myID + " and " + nextID);

		if ( ((normalInterval==1 && (s.getID() >= myID && s.getID() < nextID)) ||
			(normalInterval==0 && (s.getID() >= myID || s.getID() < nextID)))
			       	&& (me.getID() != s.getID() ) ) {

		//	System.out.println("there!");

			finger[i].setSuccessor(s);
			p = pred;

				String request = "updateFing/" + s.getID() + "/" + s.getIP() + "/" + s.getPort() + "/" + i;  
				makeConnection(p.getIP(),p.getPort(),request);
		}
		//printAllFingers();
	}

	public static void setPredecessor(Node n) // throws RemoteException
	{
		pred = n;
	}

	public static Node getPredecessor() //throws RemoteException 
	{
		return pred;
	}

	public static Node find_successor(int id) throws Exception //RemoteException,
       	{
		System.out.println("Visiting here at Node <" + me.getID()+"> to find successor of key ("+ id +")"); 

		Node n;
		n = find_predecessor(id);

		String request = "getSuc/" ;
		String result = makeConnection(n.getIP(),n.getPort(),request);
		String[] tokens = result.split("/");
		Node tempNode = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);
		return tempNode;
	}

	public static Node find_predecessor(int id)  throws Exception
       	{
		Node n = me;
		int myID = n.getID();
		int succID = finger[1].getSuccessor().getID();
		int normalInterval = 1;
		if (myID >= succID)
			normalInterval = 0;

		
		//	System.out.println("id ... " + id + " my " + myID + " succ " + succID + " " );
	
		while ((normalInterval==1 && (id <= myID || id > succID)) ||
			       	(normalInterval==0 && (id <= myID && id > succID))) {


			String request = "closetPred/" + id ;
			String result = makeConnection(n.getIP(),n.getPort(),request);
			String[] tokens = result.split("/");

			n = new Node(Integer.parseInt(tokens[0]),tokens[1],tokens[2]);

			myID = n.getID();

			String request2 = "getSuc/" ;
			String result2 = makeConnection(n.getIP(),n.getPort(),request2);
			String[] tokens2 = result2.split("/");

			succID = Integer.parseInt(tokens2[0]);

			if (myID >= succID) 
				normalInterval = 0;
			else normalInterval = 1;
		}
		//System.out.println("Returning n" + n.getID());

		return n;
	}

	public static Node getSuccessor() //throws RemoteException
       	{
		return finger[1].getSuccessor();
	}

	public static Node closet_preceding_finger(int id) //throws RemoteException 
	{
		int normalInterval = 1;
		int myID = me.getID();
		if (myID >= id) {
			normalInterval = 0;
		}

		for (int i = m; i >= 1; i--) {
			int nodeID = finger[i].getSuccessor().getID();
			if (normalInterval == 1) {
				if (nodeID > myID && nodeID < id) 
					return finger[i].getSuccessor();
			} else {
				if (nodeID > myID || nodeID < id) 
					return finger[i].getSuccessor();
			}
		}
		return me;
	}

}

