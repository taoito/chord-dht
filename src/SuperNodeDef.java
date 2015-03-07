import java.rmi.*;

//
// SuperNodeDef Interface
// RMI Interface
//
public interface SuperNodeDef extends Remote
{
	public String getRandomNode() throws RemoteException;

	public String getNodeList() throws RemoteException;

	public void finishJoining(int id) throws RemoteException;

	public String getNodeInfo(String ipAddr, String port) throws RemoteException;

}
