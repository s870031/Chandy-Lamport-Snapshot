// An Object which can build Spanning Tree 
// for all nodes by connect Table
//
import java.util.LinkedList;
import java.util.Queue;

//QNode stores the node value and level
class QNode{
	int nodeID;
	int level;
	// Construct
	public QNode(int id, int level) {
		this.nodeID = id;
	}
}

public class SpanningTree {

	static int[] parent;
	// Return node parent
	public static int getParent(int nodeID) {
		return parent[nodeID];
	}
	// Buid Spanning Tree by BFS method
	static void buildSpanningTree(int[][] connectTable){
		boolean[] visited = new boolean[connectTable.length];
		parent = new int[connectTable.length];
		Queue<QNode> queue = new LinkedList<QNode>();
		queue.add(new QNode(0,0));
		parent[0] = 0;

		visited[0] = true;
		// Use Breadth First Search to construct a tree
		while(!queue.isEmpty()){
			QNode u = queue.remove();
			for(int i=0;i<connectTable[u.nodeID].length;i++){
				if(connectTable[u.nodeID][i] == 1 && visited[i] == false){
					queue.add(new QNode(i,u.level+1));
					SpanningTree.parent[i] = u.nodeID;
					visited[i] = true;
				}
			}
		}
	}
	/*public static void main(String[] args){
	  int[][] connectTable ={ {0,0,1,1,0,0},{0,0,0,1,1,0},{1,0,0,1,0,1},{1,1,1,0,1,0},{0,1,0,1,0,0},{0,0,1,0,0,0}};
	  buildSpanningTree(connectTable);
	  for(int i=0;i<connectTable.length;i++)
	  System.out.println("Node  "+i+" Parent is "+getParent(i));
	  }*/
}
