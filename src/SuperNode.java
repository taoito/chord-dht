import java.math.*;
import java.rmi.*;
import java.rmi.server.*;
import java.security.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
//
// SuperNode 	 
//
// A RMI service that return the Client requests for Lookup() and Insert() 
//
public class SuperNode extends UnicastRemoteObject implements SuperNodeDef
{
    private static int numNodes;
    private static int busy;
    //int m = 5;
    //int numDHT = (int)Math.pow(2,m);
    //Node[] nodeList = new Node[numDHT];
    private static int m;
    private static int numDHT;
    private static Node[] nodeList;
    private List<Integer> nodeIDList = new ArrayList<Integer>();
    
    public SuperNode () throws RemoteException
    {
        //super();
    }

    public String getNodeInfo(String nodeIP, String nodePort) throws RemoteException{
	if (busy == 0) {
		
		synchronized (this) {
			busy = 1;
		}

		int nodeID = 0;
		String initInfo = "";
		numNodes++;
	    	System.out.println("*** Node Initation Call: Connection from " + nodeIP);
	    	try{ 
	        	MessageDigest md = MessageDigest.getInstance("SHA1");
			md.reset();
			String hashString = nodeIP+ nodePort;
			md.update(hashString.getBytes());
			byte[] hashBytes = md.digest();
			BigInteger hashNum = new BigInteger(1,hashBytes);

			nodeID = Math.abs(hashNum.intValue()) % numDHT;  

			System.out.println("Generated ID: " + nodeID + " for requesting node");

			while(nodeList[nodeID] != null) { //ID Collision
				md.reset();
				md.update(hashBytes);
				hashBytes = md.digest();
				hashNum = new BigInteger(1,hashBytes);
				nodeID = Math.abs(hashNum.intValue()) % numDHT;  
				System.out.println("ID Collision, new ID: " + nodeID);
			}


			if (nodeList[nodeID] == null) {
				nodeList[nodeID] = new Node(nodeID,nodeIP,nodePort);
				nodeIDList.add(nodeID);
					System.out.println("New node added ... ");
			}


			Collections.sort(nodeIDList,Collections.reverseOrder());

			int predID = nodeID;
			Iterator<Integer> iterator = nodeIDList.iterator();
			while (iterator.hasNext()) {
				int next = iterator.next();
				if (next < predID) {
					predID = next;
			       		break;	
				}
			}
			if (predID == nodeID) 
				predID = Collections.max(nodeIDList);

			initInfo = nodeID + "/" + predID + "/" + nodeList[predID].getIP() + "/" + nodeList[predID].getPort();

	    	} catch (NoSuchAlgorithmException nsae){}


		return initInfo;

	} else {
		return "NACK";
	}
    } 

    public String getRandomNode() throws RemoteException {
	    Random rand = new Random();
	    int randID = rand.nextInt(nodeIDList.size());
	    int index = nodeIDList.get(randID);
	    String result = nodeList[index].getIP() + ":" + nodeList[index].getPort();
	    return result;
    }

    public String getNodeList() throws RemoteException {
		String result = "";
		Collections.sort(nodeIDList);
		result = result + nodeIDList.size() + "/";
		Iterator<Integer> iterator = nodeIDList.iterator();
		while (iterator.hasNext()) {
			int next = iterator.next();
			result = result + nodeList[next].getID() + ":" + nodeList[next].getIP() + ":" + nodeList[next].getPort() + "/";
		}

	return result;
    }

    public void finishJoining(int id) throws RemoteException {
	    System.out.println("*** Post Initiation Call: Node " +id + " is in the DHT.");
	    System.out.println("Current number of nodes = " + numNodes + "\n");
		synchronized (this) {
			busy = 0;
		}
    }

    public static void main ( String args[] ) throws Exception
    {
		if (args.length != 1)
		{
			System.out.println
			("Syntax - SuperNode [numNodes]");
			System.exit(1);
		}

        int numNodesRequested = Integer.parseInt(args[0]);
        m = (int) Math.ceil(Math.log(numNodesRequested) / Math.log(2));
        numDHT = (int)Math.pow(2,m);

        nodeList = new Node[numDHT];
        // Assign a security manager, in the event that dynamic
	    // classes are loaded
        if (System.getSecurityManager() == null)
            System.setSecurityManager ( new RMISecurityManager() );

	    busy = 0;

        // Create an instance of our power service server ...
	    try {
        	SuperNode svr = new SuperNode();

        	// ... and bind it with the RMI Registry
        	Naming.rebind ("SuperNodeDef", svr);
        	System.out.println ("SuperNode started, service bound and waiting for connections ....");
		    numNodes = 0;
		    System.out.println ("Current number of nodes = " + numNodes + "\n");
	    } catch (Exception e) {
		    System.out.println ("Supernode Failed to start: " + e);
	    }

    }
}

