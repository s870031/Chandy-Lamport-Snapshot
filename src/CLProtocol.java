import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class CLProtocol{

	static int channelState = 0;					 // count the number of App msg receive during RED
	static HashMap<Integer,Boolean> receivedMarker;  // Record if all marker message get from neighbor
	static HashMap<Integer,Boolean> receivedState;    // For Node 0, record if all state message received
	static HashMap<Integer,StateMsg> stateMsgQueue;  // For Node 0, queue all state message received;


	// Handle get Marker message condition
	public static void getMarkerMsg(Node node, MarkerMsg message){
		synchronized(node){
			// If node is BLUE
			if(node.color == Node.Color.BLUE){
				receivedMarker.put(message.srcNodeID,true); // Record the marker message
				node.color = Node.Color.RED;
				// TODO add record loacal snapshot
				CLProtocol.sendMarkerMsg(node);

				// If it's leaf then its CLProtocol has finish send State to parent
				if(node.neighbor.length == 1 && node.nodeID != 0){
					/*System.out.println(node.nodeID + "(" + node.status + ", " + channelState  + ")" + 
							" received 1 markers. finish CL ");*/
					node.color = Node.Color.BLUE;
					// Send state message to parent
					sendStateMsg(node,channelState);
					CLProtocol.init(node);
				}
				// TODO Save vector timestamp for output file
				int[] vector = new int[node.vector.length];
				for(int i=0; i<vector.length; i++){
					vector[i] = node.vector[i]; 
				}
				Project1.output.add(vector);
			}
			// If node is RED
			else if(node.color == Node.Color.RED){
				// Add record state of Channel C since turning red
				// if havent get all Marker from neighbors:
				//     record channel state;
				// else if get all Marker message turn blue:
				//     turn blue;
				receivedMarker.put(message.srcNodeID,true);  // Record the marker message
				int i=0; // Check if all marker message received
				while(i<node.neighbor.length && receivedMarker.get(node.neighbor[i]) == true){
					i++;
				}
				// If get all marker messages, means finish CLProtocol once
				if(i == node.neighbor.length && node.nodeID != 0){
					/*System.out.println(node.nodeID + "(" + node.status + ", " + channelState  + ")" + 
							" received " + i  + " markers. finish CL ");*/
					node.color = Node.Color.BLUE;
					// Send state message to paraent
					sendStateMsg(node,channelState);
					CLProtocol.init(node);
				}
				else if(i == node.neighbor.length && node.nodeID == 0){
					/*System.out.println(node.nodeID + "(" + node.status + ", " + channelState  + ")" + 
							" received " + i  + " markers. finish CL ");*/
					//node.color = Node.Color.BLUE;
					// CLProtocol.init(node);
				}
			}
		}
	}
	public static void getStateMsg(Node node, StateMsg message){
		if(node.nodeID != 0){
			forwardToParent(node, message); // Forward state message to parent
		}
		else if (node.nodeID == 0){
			// Record, check if it needs restart, 
			//               else  terminate(send Finish)
			//System.out.println(node.nodeID + " get State message from " + message.nodeID + "(" + message.status + ", " + message.channelState + ")");

			receivedState.put(message.nodeID,true);      // Record which state message has receive
			stateMsgQueue.put(message.nodeID,message);   // Queue the state message

			// Check if all state message get
			int num=1;
			while(num<Project1.NumOfNode && receivedState.get(num) == true){
				num++;
			}
			// If receive all state message check if needs restart CL Protocol
			if(num == Project1.NumOfNode){
				boolean restartCL = checkRestartCL(node);
				if(restartCL){
					System.out.println("== Restart CL ==");
					node.color = Node.Color.BLUE;
					CLProtocol.init(node);

					try{Thread.sleep(Project1.snapshotDelay);} catch(InterruptedException e){};
					new CLThread(node).start(); 
				}
				else{
					sendFinishMsg(node);
					System.out.println("== Terminate == ");
				}
			}
		}
	}
	// Handle get Finish message condition
	public static void getFinishMsg(Node node){
		// TODO
		sendFinishMsg(node);
		System.out.println(node.nodeID + " get Finish message ");
		new OutputWriter(node).writeToFile();
		System.exit(0);
	}

	public static void init(Node node){
		channelState = 0;
		receivedMarker = new HashMap<Integer,Boolean>();
		receivedState = new HashMap<Integer,Boolean>();
		stateMsgQueue = new HashMap<Integer,StateMsg>();

		for(Integer e: node.neighbor){
			receivedMarker.put(e,false);
		}
		for(int id=0; id<Project1.NumOfNode; id++){
			receivedState.put(id,false);
		}
	}

	// Send Marker message to all neighbor
	public static void sendMarkerMsg(Node node){
		MarkerMsg message;
		synchronized(node){
			for(int i=0; i<node.neighbor.length; i++){
				try{
					int neighborID = node.neighbor[i];
					message = new MarkerMsg(node,neighborID);

					ObjectOutputStream oos = node.oStream.get(neighborID);
					oos.writeObject(message);
					oos.flush();
				}catch(IOException ex) {ex.printStackTrace();}
			}
		}
	}
	// Send State message to parent
	public static void sendStateMsg(Node node, int channelState){
		StateMsg message;
		synchronized(node){
			try{
				int parentID = SpanningTree.getParent(node.nodeID);
				message = new StateMsg(node,channelState);

				ObjectOutputStream oos = node.oStream.get(parentID);
				oos.writeObject(message);
				oos.flush();
			}catch(IOException ex){ex.printStackTrace();}
		}
	}
	public static void forwardToParent(Node node, StateMsg message){
		synchronized(node){
			try{
				int parentID = SpanningTree.getParent(node.nodeID);

				ObjectOutputStream oos = node.oStream.get(parentID);
				oos.writeObject(message);
				oos.flush();
			}catch(IOException ex){ex.printStackTrace();}
		}	
	}
	public static boolean checkRestartCL(Node node){
		synchronized(node){
			// if 0 hasnt not done map protocol, restart CL
			if(node.status == Node.Status.ACTIVE || channelState != 0){
				return true;
			}
			// if all node passive and all channel are 0, terminate
			for(int id=1; id<Project1.NumOfNode; id++){
				if(stateMsgQueue.get(id).status == Node.Status.ACTIVE || 
						stateMsgQueue.get(id).channelState != 0){
					return true;
				}
			}
			return false;		
		}
	}
	public static void sendFinishMsg(Node node){
		// TODO
		FinishMsg message;
		synchronized(node){
			for(int i=0; i<node.neighbor.length; i++){
				try{
					int neighborID = node.neighbor[i];
					message = new FinishMsg();

					ObjectOutputStream oos = node.oStream.get(neighborID);
					oos.writeObject(message);
					oos.flush();
				}catch(IOException ex) {ex.printStackTrace();}
			}
		}
	}
}
class OutputWriter {
	Node node;

	public OutputWriter(Node node) {
		this.node = node;
	}
	public void writeToFile() {
		String fileName = Project1.OutputFile + "-" + node.nodeID + ".out";
		synchronized(Project1.output){
			try {
				File file = new File(fileName);
				FileWriter fileWriter;
				if(file.exists()){
					fileWriter = new FileWriter(file,true);
				}
				else{
					fileWriter = new FileWriter(file);
				}
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

				for(int i=0;i<Project1.output.size();i++){
					for(int j:Project1.output.get(i)){
						bufferedWriter.write(j+" ");
					}
					if(i<(Project1.output.size()-1)){
						bufferedWriter.write("\n");
					}
				}			
				Project1.output.clear();
				// Always close files.
				bufferedWriter.close();
			}
			catch(IOException ex) {
				System.out.println("Error writing to file '" + fileName + "'");
			}
		}
	}
}
