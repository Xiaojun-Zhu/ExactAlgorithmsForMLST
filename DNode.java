
public class DNode {

//common variables
int id=-1;//node id
double x,y;//node location
double E;//initial energy
DEdge edge;// incident edges
int degree=0;//degree of a node in the tree, for computing lifetime

int deg_G=0;//degree in the graph, same as the number of out edges

//the following are all temporary variables
int height=-1;//for finding the path in the tree between any two nodes
DNode parent=null;// the parent of each node in the tree, for speeding up identifying a cycle
DEdge parent_edge=null;//the same purpose
DEdge toDelete=null,toAdd=null;
DNode groupParent=null;//for union find
int rank=0;//for union find
int type=-1;//1 for S, 0 for W, and -1 otherwise, used in AlgApx
boolean visited=false;//for searching the graph, BFS or DFS



public static double Rx=3.33*Math.pow(10, -4),Tx=6.66*Math.pow(10, -4);//system parameters, energy consumption for transmitting and receiving one message




public static void deleteEdge(DEdge e){
	if(e.previous==null){//the first edge for a node
		e.from.edge=e.next;
		if(e.next!=null) e.next.previous=null;
	}else{
		e.previous.next=e.next;
		if(e.next!=null){
			e.next.previous=e.previous;
		}
	}
}

public static void insertEdge(DEdge e){
	e.previous=null;
	if(e.from.edge==null){
		//no edge
		e.from.edge=e;
		e.next=null;//this bug was found at 2013-2-9 10:08
	}else{
	    e.from.edge.previous=e;
	    e.next=e.from.edge;
	    e.from.edge=e;
	}
}

public DNode find_set(){//used in AlgApx
	if(this!=this.groupParent){
		this.groupParent=this.groupParent.find_set();
	}
	return this.groupParent;
}

public void make_set(){//used in AlgApx
	groupParent=this;
	rank=0;
}

private void link(DNode x,DNode y){//used in AlgApx
	if(x==y) return;
	if(x.rank>y.rank){
		y.groupParent=x;
	}else{
		x.groupParent=y;
		if(x.rank==y.rank){
			y.rank++;
		}
	}
}

public void union(DNode y){//used in AlgApx
	link(this.find_set(),y.find_set());
}


}
