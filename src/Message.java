import java.io.Serializable;

@SuppressWarnings("serial")
public class Message implements Serializable{
	String msg;
}

@SuppressWarnings("serial")
// Application Message to implement MAP Protocol
class ApplicationMsg extends Message implements Serializable{

	String msg;
	int [] vector;
	int srcNodeID;
	int dstNodeID;

	public  ApplicationMsg(Node node, int dstNodeID){
		synchronized(node){
		this.msg = "AppMsg";
		this.vector = node.vector;
		this.srcNodeID = node.nodeID;
		this.dstNodeID = dstNodeID;
		}
	}
}
// Marker Message to implement Chandy Lamport's Protocol
@SuppressWarnings("serial")
class MarkerMsg extends Message implements Serializable{
	String msg;
	int srcNodeID;
	int dstNodeID;

	public MarkerMsg(Node node, int dstNodeID){
		synchronized(node){
		this.msg = "Marker";
		this.srcNodeID = node.nodeID;
		this.dstNodeID = dstNodeID;
		}
	}
}
// State Message to implement Chandy Lamport's Protocol
@SuppressWarnings("serial")
class StateMsg extends Message implements Serializable{
	String msg;
	int nodeID;
	Node.Status status;
	int channelState;

	public StateMsg(Node node, int channelState){
		synchronized(node){
		this.msg = "State";
		this.nodeID = node.nodeID;
		this.status = node.status;
		this.channelState = channelState;
		}
	}
}
class FinishMsg extends Message implements Serializable{
	String msg = "Finish";
}
