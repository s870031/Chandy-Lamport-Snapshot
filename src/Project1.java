import java.io.*;
import java.net.*;
import java.util.*;

public class Project1 {
	// Project General Settings
	public static int nodeID, NumOfNode, minPerAct, maxPerAct, minSendDelay, snapshotDelay, maxNumber;	
	public static String ConfigFile, OutputFile;
	public static int [][] connectTable;
	public static SpanningTree T;
	public static boolean runMAP = true;
	public static ArrayList<int[]> output = new ArrayList<int[]>(); 
	
	public static void main(String args[]) throws IOException{
		Project1.ConfigFile = args[1];                          // Get config file path from args[1]
		ConfigReader R = new ConfigReader(Project1.ConfigFile); // Create Reader Object
		Project1.OutputFile = Project1.ConfigFile.substring(0, Project1.ConfigFile.lastIndexOf('.'));
		int nodeID = Integer.parseInt(args[0]);                 // args[0] is nodeID
		Node node = new Node (nodeID);                          // Create a Node	
		Project1.NumOfNode = R.getGeneralInfo()[0];
		Project1.minPerAct = R.getGeneralInfo()[1];
		Project1.maxPerAct = R.getGeneralInfo()[2];
		Project1.minSendDelay = R.getGeneralInfo()[3];
		Project1.snapshotDelay = R.getGeneralInfo()[4];
		Project1.maxNumber = R.getGeneralInfo()[5];
		connectTable = R.getConnectTable();
		T = new SpanningTree();                                  // Spanning Tree of all nodes
		T.buildSpanningTree(connectTable);                       // Build Spanning tree of all nodes

		node.socketListen();                                       // Turn on Server Socket
		try{Thread.sleep(5000);} catch(InterruptedException e){}; // Sleep 1 sec wait all socket turn on
		node.buildChannel();

		new MAPThread(node).start();  // Start MAP Protocol
		
		CLProtocol.init(node);	
		if(node.nodeID == 0){
			new CLThread(node).start();
		}
	}

	public static int getRandom(int min, int max){
		int r;
		Random  rand = new Random();
		r = min + rand.nextInt((max-min)+1);
		return r;
	}
}

class MAPThread extends Thread {
		Node node;
		
		public MAPThread(Node node){
			this.node = node;
		}
		
		public void run(){
			// MAP PROTOCOL
			Node node = this.node;
			while(Project1.runMAP){
				synchronized(node){
					if(node.status == Node.Status.ACTIVE){
						int msgNum = Project1.getRandom(Project1.minPerAct, Project1.maxPerAct);   // Random number of messge to send
						ApplicationMsg message; 

						// Send msgNum mesages to random neighbor
						while (msgNum > 0){				
							int randReceiver = Project1.getRandom(0, node.neighbor.length-1);			
							int neighborID = node.neighbor[randReceiver];
							
							node.vector[node.nodeID]++;  // Update Vector clock
							
							message = new ApplicationMsg(node,neighborID); // Construct App message
							node.sendAppMsg(node.neighbor[randReceiver], message);                  // Send message to random neighbor
							try{Thread.sleep(Project1.minSendDelay);} catch(InterruptedException e){};  // Messagge Delay
							node.numOfMsgSent++;
							msgNum--;
						}
						node.status = Node.Status.PASSIVE;
					}
				}
			}
		}
	}

// Only node 0 will keep running this thread
class CLThread extends Thread {
	Node node;

	public CLThread(Node node){
		this.node = node;
	}

	public void run(){
		synchronized(node){
		node.color = Node.Color.RED;
		// Save vector timestamp
		int[] vector = new int[node.vector.length];
		for(int i=0; i<vector.length; i++){
			vector[i] = node.vector[i]; 
		}
		Project1.output.add(vector);

		CLProtocol.sendMarkerMsg(node);
		}
	}
}
