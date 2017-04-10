import java.io.*;
import java.net.*;
import java.util.*;
public class Node
{
	public enum Status {ACTIVE, PASSIVE};
	public enum Color {BLUE, RED};
	public int nodeID;             // node ID
	public String hostName;        // node host name
	public Status status;          // node Status
	public int listenPort;         // node socket listen port
	public int[] neighbor = null;  // node neighbors
	public int numOfMsgSent;       // Number of message sent
	public int[] vector;           // Vector clock
	ConfigReader R = new ConfigReader(Project1.ConfigFile);
	HashMap<Integer,Socket> channels = new HashMap<Integer,Socket>();
	HashMap<Integer,ObjectOutputStream> oStream = new HashMap<Integer,ObjectOutputStream>();
	public Color color;

	// Constructor
	public Node(int nodeID){
		this.nodeID = nodeID;
		this.hostName = R.getNodeHostName(nodeID);
		this.status = (nodeID == 0)? Status.ACTIVE: Status.PASSIVE; // Initial node 0 as ACTIVE
		this.listenPort = R.getNodeListenPort(nodeID);
		this.neighbor = R.getNodeNeighbor(nodeID);
		this.numOfMsgSent = 0;
		this.vector = new int [R.getGeneralInfo()[0]];
		this.color = Node.Color.BLUE;
	}

	// Socket Server listen
	public void socketListen(){				
		new ServerThread(this).start();	
	}

	// Build Channel
	public void buildChannel() throws IOException, UnknownHostException{
		for(int i=0;i<Project1.NumOfNode;i++){
			// If the value in adjacency matrix is one for the current Node then its a neighbor
			if(Project1.connectTable[nodeID][i] == 1){
				String hostName = R.getNodeHostName(i);
				//InetAddress hostName = InetAddress.getLocalHost();
				int port = R.getNodeListenPort(i);
				InetAddress address = InetAddress.getByName(hostName);
				Socket client = new Socket(address,port);
				// Get the sockets for all neighbors
				//Socket client = new Socket(hostName,port);
				// Put the neighbor sockets in hash map called channels indexed by their node id's
				channels.put(i, client);
				// Get an output stream associated with each socket and put it in a hashmap oStream
				ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
				oStream.put(i, oos);
			}	
		}
	}

	// Socket Client Sending message 
	public void sendAppMsg(int nodeID, ApplicationMsg message){
		
		 try{
		 ObjectOutputStream oos = this.oStream.get(nodeID);
		 oos.writeObject(message);
		 oos.flush();
		 }
		 catch(IOException ex) {ex.printStackTrace();}
		 
	}
	/*	
		public static void main (String args[])
		{
		Node n = new Node(1);
		System.out.println("NodeID: " + n.nodeID );
		System.out.println("status: " + n.status );
		System.out.println("port: " + n.listenPort);
		System.out.print("neighbor #: " + n.neighbor.length + ", ");
		for(int i=0; i<n.neighbor.length; i++)
		System.out.print(n.neighbor[i] + " ");
		System.out.print("\n");

		for(int i=0; i<n.vector.length; i++)
		System.out.print(n.vector[i] + " ");
		}
		*/ 
}
// Server listen thread
class ServerThread extends Thread{
	Node node;
	int port; 
	String hostName;  
	int nodeID; 

	public ServerThread(Node node){
		this.node = node;
		this.port = node.listenPort;
		this.hostName = node.hostName;
		this.nodeID  = node.nodeID;
	}	

	public void run(){
		try{
			ServerSocket serverSock = new ServerSocket(port);  // Create a server socket service at port
			System.out.println( hostName +"(" + nodeID + ")" + " server socket listening...");

			while(true){                                       //  Server starts infinite loop waiting for accept client
				Socket sock = serverSock.accept();             //    Wait for client connection
				new ClientThread(sock,node).start();           //    Start new thead to handle client connection
			}
		}catch (IOException ex) {ex.printStackTrace();}
	}
}
// Socket accept connection
//    create new thread for each connection
class ClientThread extends Thread{	
	Socket cSocket; // Client Socket
	Node node;

	public ClientThread(Socket cSocket, Node node){
		this.cSocket = cSocket;
		this.node = node;
	}

	public void run() {
		ObjectInputStream ois = null;
		try{
			ois = new ObjectInputStream(cSocket.getInputStream());
		}catch (IOException e){e.printStackTrace();}

		while(true){
		try{				
			Message message;
			message = (Message) ois.readObject();	
			ConfigReader R = new ConfigReader(Project1.ConfigFile);
			synchronized(node){
				// Get Application message
				if(message instanceof ApplicationMsg){
					// MAP Protocol passive node process
					if((node.status == Node.Status.PASSIVE) && (node.numOfMsgSent < Project1.maxNumber)){
						node.status = Node.Status.ACTIVE;
					}
					// Handle received message
					//System.out.println( "App from " + ((ApplicationMsg) message).srcNodeID + " to " + node.nodeID);
					// Update Vector Clock
					for(int k=0; k<Project1.NumOfNode; k++)
						node.vector[k] = Math.max(node.vector[k], ((ApplicationMsg) message).vector[k]);
					node.vector[node.nodeID]++;
					// Check if need to record app message
					if(node.color == Node.Color.RED){
						CLProtocol.channelState++;
					}
				}
				// Get Marker message
				if(message instanceof MarkerMsg){
					//System.out.println(  "Marker form " + ((MarkerMsg) message).srcNodeID + " to " + node.nodeID);
					CLProtocol.getMarkerMsg(node, (MarkerMsg)message);
				}
				// Get State message
				if(message instanceof StateMsg){
					//System.out.println(node.nodeID + " get State message from " + ((StateMsg) message).nodeID);
					CLProtocol.getStateMsg(node, (StateMsg)message);
				}
				if(message instanceof FinishMsg){
					CLProtocol.getFinishMsg(node);
				}
			}
		}catch (IOException e) {e.printStackTrace();}
		 catch (ClassNotFoundException e){e.printStackTrace();}
		}
	}
}
