
import java.util.*;

//approximation algorithm in (Zhu et al. 2016)
public class AlgApx extends Solver{

	ArrayList<DNode> nodes=null;	

	static public ArrayList<DNode> make_a_copy(ArrayList<DNode> inputnodes){	
		ArrayList<DNode> newnodes=new ArrayList<DNode>(inputnodes.size());
		for(DNode n:inputnodes){
			DNode nn=new DNode();
			nn.x=n.x;nn.y=n.y;nn.E=n.E;nn.id=n.id;nn.visited=false;nn.degree=n.degree;
			newnodes.add(nn);
		}
		for(DNode n:inputnodes){
			DNode u=newnodes.get(n.id);
			DEdge ne=n.edge;
			while(ne!=null){
				if(ne.to.id>ne.from.id){
					DNode v=newnodes.get(ne.to.id);
					DEdge e=new DEdge();e.from=u;e.to=v;e.inSPT=ne.inSPT;e.previous=null;e.next=null;
					DEdge e2=new DEdge();e2.from=e.to;e2.to=e.from;e2.inSPT=ne.inSPT;e2.previous=null;e2.next=null;
					e2.pair=e;e.pair=e2;DNode.insertEdge(e);DNode.insertEdge(e2);
				}
				ne=ne.next;
			}
		}
		return newnodes;		
	}

	static public void build_initial_tree(ArrayList<DNode> inputnodes){
		DFS_build_tree(inputnodes.get(0));	
	}

	static public void DFS_build_tree(DNode v){
		v.visited=true;
		DEdge e=v.edge;
		while(e!=null){
			if(!e.to.visited){
				e.inSPT=true;e.pair.inSPT=true;e.from.degree++;e.to.degree++;
				DFS_build_tree(e.to);
			}
			e=e.next;
		}
	}

	static public double build_random_spt(ArrayList<DNode> inputnodes){
		for(DNode n:inputnodes){n.rank=-1;n.degree=0;}
		Queue<DNode> queue=new LinkedList<DNode>();
		queue.offer(inputnodes.get(0));
		inputnodes.get(0).rank=0;
		while(!queue.isEmpty()){
			//bfs search to establish the rank of each node
			DNode n=queue.poll();	
			DEdge e=n.edge;
			while(e!=null){
				if(e.to.rank==-1){
					e.to.rank=n.rank+1;
					queue.offer(e.to);
				}
				e=e.next;
			}			
		}

		for(DNode n:inputnodes){//now randomly pick a parent
			if(n.id==0) continue;
			ArrayList<DEdge> tmp=new ArrayList<DEdge>();
			DEdge e=n.edge;
			while(e!=null){
				if(e.to.rank==n.rank-1){//candidate parent
					tmp.add(e);
				}
				e=e.next;
			}			

			int index=(int)Math.floor(Math.random()*tmp.size());
			DEdge t=tmp.get(index);
			//now set t as a tree edge
			t.inSPT=true;t.pair.inSPT=true;t.from.degree++;t.to.degree++;			
		}

		//compute the lifetime of the random tree
		double l=f(inputnodes.get(1).E,inputnodes.get(1).degree);
		for(int i=2;i<inputnodes.size();i++){
			double tmp=f(inputnodes.get(i).E,inputnodes.get(i).degree);
			if(tmp>l)l=tmp;
		}
		return l;

	}
	static double f(double ei,int j) {		
		return GenRWUtils.f(ei, j);	
	}
	boolean debug=true;
	
	public boolean reduce_set_of_critical_nodes(){
		//true, reduced, should continue
		//false, stop, retrieve value at field optobj

		//compute the load of current tree T
		double l=f(nodes.get(1).E,nodes.get(1).degree);
		for(int i=2;i<nodes.size();i++){
			double tmp=f(nodes.get(i).E,nodes.get(i).degree);
			if(tmp>l)l=tmp;
		}

		//mark the nodes as S, W or others, make empty components, clear toBeDelete edge
		for(int i=0;i<nodes.size();i++){
			double tmp=f(nodes.get(i).E,nodes.get(i).degree);
			if(i!=0&&tmp==l){
				nodes.get(i).type=1;//1 for S, 0 for W
	//			if(debug)System.out.println("S("+i+", "+nodes.get(i).degree+","+l+")");
			}else if(i!=0&&f(nodes.get(i).E,nodes.get(i).degree+1)>=l){
				nodes.get(i).type=0;
	//			if(debug)System.out.println("W("+i+", "+nodes.get(i).degree+","+f(nodes.get(i).E,nodes.get(i).degree)+"->"+f(nodes.get(i).E,nodes.get(i).degree+1)+")");
			}else{
				nodes.get(i).type=-1;// other nodes
	//			if(debug)System.out.println("O("+i+", "+nodes.get(i).degree+","+f(nodes.get(i).E,nodes.get(i).degree)+"->"+f(nodes.get(i).E,nodes.get(i).degree+1)+")");
			}
			nodes.get(i).make_set();
			nodes.get(i).toDelete=null;nodes.get(i).toAdd=null;
		}

		//now generate components, and store edges connecting different components
		LinkedList<DEdge> cedge=new LinkedList<DEdge>();
		for(int i=0;i<nodes.size();i++){
			DEdge e=nodes.get(i).edge;
			while(e!=null){
				if(e.from.id<=e.to.id&&e.from.type==-1&&e.to.type==-1){//each edge is examined only once, and each edge should not be incident to nodes in W or S
					if(e.inSPT){
						//unite
						e.from.union(e.to);
					}else if(e.from.find_set()!=e.to.find_set()){
						//candidate edge
						cedge.addFirst(e);
					}

				}				
				e=e.next;
			}
		}

		//now repeatedly find w 
		DNode w=null;	

		//establish the tree's parent-child relationship, as well as the height of each node, by a BFS
		//this is to guarantee the running time of the next while loop
		LinkedList<DNode> queue=new LinkedList<DNode>();
		nodes.get(0).height=0;nodes.get(0).parent=null;
		queue.addFirst(nodes.get(0));
		while(!queue.isEmpty()){
			DNode t=queue.removeLast();
			DEdge e=t.edge;
			while(e!=null){
				if(e.inSPT&&e.to!=t.parent){
					e.to.parent=t;e.to.height=t.height+1;e.to.parent_edge=e;queue.addFirst(e.to);
				}
				e=e.next;
			}
		}

		while(w==null&&!cedge.isEmpty()){
			//pick one edge from cedge
			DEdge ce=cedge.removeLast();
			if(ce.from.find_set()==ce.to.find_set()){
				continue;
			}
	
			//now find a cycle in T containing e.from and e.to
			//check which one is higher; u always points to the higher
			DNode u=ce.from,v=ce.to;
			DNode toProcess;

			while(u!=v){

				if(u.height>=v.height){
					toProcess=u;
					u=u.parent;
				}else{
					toProcess=v;
					v=v.parent;					
				}

				if(toProcess.parent.type==1){
					//S
					w=toProcess.parent;w.toDelete=toProcess.parent_edge;w.toAdd=ce;break;					
				}else if(toProcess.parent.type==0){
					//U
					toProcess.parent.toDelete=toProcess.parent_edge;toProcess.parent.toAdd=ce;toProcess.parent.type=-1;//remove it from U
					//now merge components
					DEdge neu=toProcess.parent.edge;
					while(neu!=null){
						if(neu.to.type==-1){
							if(neu.inSPT){
								neu.from.union(neu.to);
							}else{						
								    cedge.addFirst(neu);							
							}
						}							
						neu=neu.next;
					}				
				}				
			}

		}

		//now reduce w
		if(w!=null){	
			reduce(w);
			return true;
		}else{
			optobj=l;
			return false;
		}

	}

	public void printTree(ArrayList<DNode> nodes) {
		//print tree suggested by inSPT
		for(DNode n:nodes) {
			DEdge e=n.edge;
			while(e!=null) {
				if(e.inSPT) {
					if(e.to.id>e.from.id) {
						System.out.println("tree edge ("+e.from.id+" ,"+e.to.id+")");
					}
				}
				e=e.next;
			}
		}
	}

	public void reduce(DNode u){
		DEdge e=u.toDelete;
		e.inSPT=false;e.pair.inSPT=false;e.from.degree--;e.to.degree--;
		e=u.toAdd;
		e.inSPT=true;e.pair.inSPT=true;e.from.degree++;e.to.degree++;
		if(e.from.toDelete!=null){
			reduce(e.from);
		}
		if(e.to.toDelete!=null){
			reduce(e.to);
		}

	}

	public double find_min(){

		while(this.reduce_set_of_critical_nodes());
		return optobj;
	}
	public void debug_print_degree() {
		//compute the load of current tree T
		double l=f(nodes.get(1).E,nodes.get(1).degree);
		System.out.println("(node "+1+", degree "+nodes.get(1).degree+", load "+l+",->"+l+")");
		for(int i=2;i<nodes.size();i++){
			double tmp=f(nodes.get(i).E,nodes.get(i).degree);
			if(tmp>l)l=tmp;
			System.out.println("(node "+i+", degree "+nodes.get(i).degree+", load "+tmp+",->"+l+")");
		}
	}


	@Override
	public void compute_optimal() {
		// TODO Auto-generated method stub

		AlgApx.build_random_spt(graph);
		ArrayList<DNode> network_copy1=AlgApx.make_a_copy(graph);
		this.nodes=network_copy1;
		optobj=find_min();	

		//	debug_print_degree();
	}

}
