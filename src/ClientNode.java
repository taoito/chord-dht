import java.security.*;
import java.math.*;
import java.rmi.*;
import java.rmi.Naming;
import java.io.*;
import java.util.*;
import java.net.*;

public class ClientNode 
{
    private static SuperNodeDef service; 
    private static int m;
    private static int numDHT;

    public static void readSampleWords(){
        File fileSample = null;
        BufferedReader buff = null;
        String line = "";
        String[] stringSplit = null;
        //SortedMap<String, String>       sortedMapWords  = null;

        try{
            try{
                fileSample = new File ("src/SampleWords.txt");
                buff = new BufferedReader( new FileReader (fileSample));
                //sortedMapWords  = new TreeMap<String, String>();

                int count = 0;
                while(( line = buff.readLine()) != null){
                    stringSplit = line.split(":");
                    //sortedMapWords.put(stringSplit[0], stringSplit[1]);

                    MessageDigest md = MessageDigest.getInstance("SHA1");
                    md.reset();
                    md.update(stringSplit[0].getBytes());
                    byte[] hashBytes = md.digest();
                    BigInteger hashNum = new BigInteger(1,hashBytes);
                    int key = Math.abs(hashNum.intValue()) % numDHT;  
                    //System.out.println("String 1=> "+stringSplit[0] + " || String 2=> "+ key);

                    String response = service.getRandomNode();
                    String[] token = response.split(":");
                    String message = key + "/" + stringSplit[0] + "/" + stringSplit[1];
                    insertKeyDHT(token[0],token[1],message);
                }
            }
            finally{
                buff.close();
            }
        } catch (Exception ae){
            System.err.println("Error reading the file");
            ae.printStackTrace();
        }
    }

    public static void lookupKeyDHT(String ip, String port, String message) throws Exception{
        Socket sendingSocket = new Socket(ip,Integer.parseInt(port));
        DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

        out.writeBytes("lookupKey/" + message + "\n");

        String result = inFromServer.readLine();
        String[] token = result.split("/");
        if (!result.equals("No Word Found!"))
            System.out.println("Lookup result: the meaning is <" + token[1] + "> found in Node " + token[0]);
        else System.out.println(result);
        out.close();
        inFromServer.close();
        sendingSocket.close(); 
    }

    public static void insertKeyDHT(String ip, String port, String message) throws Exception{
        Socket sendingSocket = new Socket(ip,Integer.parseInt(port));
        DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

        out.writeBytes("tryInsert/" + message + "\n");

        String result = inFromServer.readLine();
        System.out.println(result);
        out.close();
        inFromServer.close();
        sendingSocket.close(); 
    }

    public static void getEachNode(String id, String ip, String port) throws Exception{
        Socket sendingSocket = new Socket(ip,Integer.parseInt(port));
        DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

        out.writeBytes("print" + "\n");

        String result = inFromServer.readLine();
        String[] token = result.split("/");

        System.out.println("NODE:<" + id + "> at " + ip + ":" + port + " **************");
        System.out.println("\tPredecessor: " + token[0] + " at " + token[1]); 
        System.out.println("\tContains: " + token[2] + " word:meaning pairs at this node");
        for (int i = 1,j=3; i <= m; i++,j+=3) {
            System.out.println("\tFinger[" + i + "] starts at " + token[j] + "\thas Successor Node ("
                    + token[j+1] + ")\tat " + token[j+2]);
        }
        System.out.println("");
        out.close();
        inFromServer.close();
        sendingSocket.close(); 
    }

    public static void printAllNodeInfo() throws Exception{
        String nodeList = service.getNodeList();
        String[] tokens = nodeList.split("/");
        for (int i = 1; i <= Integer.parseInt(tokens[0]); i++) {
            String[] nodeTok = tokens[i].split(":");
            getEachNode(nodeTok[0], nodeTok[1], nodeTok[2]);
        }
    }


    public static void main(String args[]) throws Exception
    {
        // Check for hostname argument
        if (args.length != 2)
        {
            System.out.println
                ("Syntax - ClientNode [Supernode's IP] [maxNumNodes]");
            System.exit(1);
        }

        int maxNumNodes = Integer.parseInt(args[1]);
        m = (int) Math.ceil(Math.log(maxNumNodes) / Math.log(2));
        numDHT = (int)Math.pow(2,m);

        // Assign security manager
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager
                (new RMISecurityManager());
        }

        // Call registry for PowerService
        service = (SuperNodeDef) Naming.lookup("rmi://" + args[0] + "/SuperNodeDef");

        DataInputStream din = new DataInputStream (System.in);

        int initSample = 0;

        for (;;)
        {
            if (initSample == 0)
                System.out.println("1 - Insert the given Sample Dictionary into DHT (only do this once)");
            System.out.println("2 - Look up for a Word in DHT");
            System.out.println("3 - Insert a Word into DHT");
            System.out.println("4 - Print DHT Structure - All Nodes Info");
            System.out.println("5 - Exit"); 
            System.out.println();

            System.out.print ("Choice : ");

            String line = din.readLine();

            if (line.equals("1")) {
                readSampleWords();
                initSample = 1;
                System.out.println("");
            }
            else if (line.equals("2")) {
                System.out.print ("Lookup for this word: ");
                String wordLookup = din.readLine();


                MessageDigest md2 = MessageDigest.getInstance("SHA1");
                md2.reset();
                md2.update(wordLookup.getBytes());
                byte[] hashBytes2 = md2.digest();
                BigInteger hashNum2 = new BigInteger(1,hashBytes2);
                int key2 = Math.abs(hashNum2.intValue()) % numDHT;  

                System.out.println("Hashed key: " + key2);

                String response2 = service.getRandomNode();
                String[] token2 = response2.split(":");
                String message2 = key2 + "/" + wordLookup;
                lookupKeyDHT(token2[0],token2[1],message2);
                System.out.println("");
            }
            else if (line.equals("3")) {
                System.out.print ("Tell me the word you want to insert: ");
                String wordInput = din.readLine();					

                MessageDigest md3 = MessageDigest.getInstance("SHA1");
                md3.reset();
                md3.update(wordInput.getBytes());
                byte[] hashBytes3 = md3.digest();
                BigInteger hashNum3 = new BigInteger(1,hashBytes3);
                int key3 = Math.abs(hashNum3.intValue()) % numDHT;  

                System.out.println("Hashed key: " + key3);
                System.out.print ("Tell me the meaning of this word: ");
                String meaningInput = din.readLine();

                // Call remote method
                String response3 = service.getRandomNode();
                String[] token3 = response3.split(":");
                String message3 = key3 + "/" + wordInput + "/" + meaningInput;
                insertKeyDHT(token3[0],token3[1],message3);
                System.out.println("");
            }
            else if (line.equals("4")) {
                printAllNodeInfo();
            }

            else if (line.equals("5")) {
                System.exit(0);
            }
            else {
                System.out.println("Invalid option");
                System.out.println("");
            }
        }
    }
}

