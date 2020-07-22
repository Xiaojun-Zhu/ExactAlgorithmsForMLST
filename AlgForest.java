

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;



import java.util.*;




public class  AlgForest extends Solver{

	public static final double MIN_VALUE=0.000000001;


	double f(double ei,int j) {		
		return GenRWUtils.f(ei, j);	
	}

	double [] generate_possible_loads(ArrayList<DNode> nodes) {
		//return possible loads in ascending order
		//also establish the deg_G field
		//use approximation algorithm to find a spanning tree first

		//build the deg_G field
		for(DNode d:nodes) {
			d.deg_G=0;
			DEdge e=d.edge;
			while(e!=null) {
				d.deg_G++;
				e=e.next;
			}
		}


		//first improvement here
		Solver nina=new AlgApx();
		double apxopt=Double.MAX_VALUE;
		for(int i=0;i<10;i++) {
			nina.set_graph(nodes);
			nina.compute_optimal();
			if(nina.get_optimal()<apxopt) {
				apxopt=nina.get_optimal();
				if(debug)System.out.println("run # "+i+": "+nina.get_optimal());
			}
		}


		double a=apxopt,lower=a;
		//	System.out.println(a);
		for(int i=1;i<nodes.size();i++) {
			//find the minimum j such that f_i(j+1)>=a
			int j=0;
			double ei=nodes.get(i).E;
			//	System.out.print("test node "+i+" ");
			while(f(ei,j+1)<a&&j+1<=nodes.get(i).deg_G) {//find f_i(j+1)>=a
				//		System.out.print("(j+1)="+(j+1)+" load "+f(ei,j+1));
				j++;
			}
			//	System.out.println();
			if(j+1<=nodes.get(i).deg_G) {//otherwise, if j+1>deg_G, then this node will not be a bottleneck node.
				double cur=f(ei,j);
				if(cur<lower)lower=cur;
			}
			//	System.out.println("bounds corresponding to node "+i+" "+f(ei,j)+" adopted?"+(j+1<=nodes.get(i).deg_G));
		}

		ArrayList<Double> loads=new ArrayList<Double>();
		//loads.add(lower-1);//ensure there is at least one infeasible value

		for(int i=1;i<nodes.size();i++) {
			DNode n=nodes.get(i);
			for(int j=1;j<=n.deg_G;j++) {
				double cur=f(n.E,j);
				if(cur>=lower-MIN_VALUE&&cur<=a+MIN_VALUE)//again, numerical instability problem
					loads.add(cur);
			}
		}
		Collections.sort(loads);
		ArrayList<Double> uniq_loads=new ArrayList<Double>();


		//exclude obviously infeasible loads
		int i=0;
		double value=0;
		while(i<loads.size()) {
			value=loads.get(i);
			int []Bs=degree_bounds(nodes,value);
			if(Bs!=null) {
				//sum it up
				int sum=0;
				for(int b:Bs) {
					sum+=b;
				}
				if(sum>=2*nodes.size()-2) {
					//may be feasible
					break;
				}				
			}
			i++;			
		}
		uniq_loads.add(value);

		for(;i<loads.size();i++) {
			double c=loads.get(i);
			if(c>value+MIN_VALUE) {

				uniq_loads.add(c);
				value=c;
			}
		}
		if(debug)System.out.println("# of unique loads:"+uniq_loads.size()+" # of previous loads:"+loads.size());



		double[] t=new double[uniq_loads.size()];
		for(i=0;i<t.length;i++) {
			t[i]=uniq_loads.get(i);
			if(debug)  System.out.print(" "+t[i]);
		}


		if(debug)System.out.println();
		return t;	
	}

	class Forest{//ensure constant time lookup
		int[] tree_ID;//tree ID
		int[] ranks;// ranks in the tree
		int[] degrees;//degree in the forest
		int nodeNum;//total number of nodes, for initialize the size of every list


		LinkedList<Integer> edges_from;
		LinkedList<Integer> edges_to;//record the edges for later recover

		ArrayList<ArrayList<Integer>> active_trees;//record only active trees, the ID of the tree

		public Forest(int numberOfvertices) {
			nodeNum=numberOfvertices;
			tree_ID=new int[nodeNum];
			ranks=new int[nodeNum];			
			active_trees=new ArrayList<ArrayList<Integer>>(nodeNum);//at most nodeNum trees
			degrees=new int[nodeNum];
			edges_from=new LinkedList<Integer>();
			edges_to=new LinkedList<Integer>();
			Arrays.fill(tree_ID,-1);
			Arrays.fill(ranks,-1);
		}

		public Forest clone() {
			Forest newF=new Forest(nodeNum);
			for(int i=0;i<tree_ID.length;i++) {
				newF.tree_ID[i]=tree_ID[i];
				newF.ranks[i]=ranks[i];
				newF.degrees[i]=degrees[i];
			}

			Iterator<Integer> from=edges_from.iterator(),to=edges_to.iterator();
			while(from.hasNext()) {
				newF.edges_from.add(from.next());
				newF.edges_to.add(to.next());
			}

			for(ArrayList<Integer> tr:active_trees) {
				ArrayList<Integer>copy=new ArrayList<Integer>(tr.size());
				for(Integer i:tr) {
					copy.add(i);
				}
				newF.active_trees.add(copy);
			}
			return newF;
		}

		public void initialize(ArrayList<DNode>nodes) {

			//clear the inspt field of all edges of nodes
			for(int i=0;i<nodes.size();i++) {
				DEdge e=nodes.get(i).edge;
				while(e!=null) {
					e.inSPT=false;
					e=e.next;
				}
			}

			//make each vertex a group
			for(int i=0;i<tree_ID.length;i++) {
				tree_ID[i]=active_trees.size();
				ArrayList<Integer> tree=new ArrayList<Integer>(nodeNum);
				tree.add(i);
				ranks[i]=0;//first element in this tree
				active_trees.add(tree);				
			}		
			Arrays.fill(degrees,0);

		}

		public void insert_edge(DEdge e) {
			//ensure that the two ends belong to two trees
			//	System.out.println("insert edge "+e.from.id+" -> "+e.to.id);

			int from=e.from.id,to=e.to.id;			
			if(tree_ID[from]==tree_ID[to]) {
				System.out.print("error: the ends of the edge are in the same tree ");
			}
			edges_from.addLast(from);edges_to.addLast(to);
			degrees[from]++;degrees[to]++;

			e.inSPT=true;e.pair.inSPT=true;



			//book keep, update nodes belonging to to_trees
			ArrayList<Integer> small_tree,large_tree;
			int small_treeID,large_tree_ID;			
			if(tree_ID[from]<tree_ID[to]) {
				small_treeID=tree_ID[from];large_tree_ID=tree_ID[to];				
			}else {
				small_treeID=tree_ID[to];large_tree_ID=tree_ID[from];
			}
			small_tree=active_trees.get(small_treeID);
			large_tree=active_trees.get(large_tree_ID);

			int removed_tree_size=large_tree.size();
			small_tree.addAll(large_tree);
			for(int i=0;i<large_tree.size();i++) {
				int id=large_tree.get(i);
				tree_ID[id]=small_treeID;
				ranks[id]=i+small_tree.size()-removed_tree_size;
			}
			active_trees.remove(large_tree);//delete the tree with large tree ID; which automatically moves subsequent trees ahead
			for(int j=large_tree_ID;j<active_trees.size();j++) {
				for(int node:active_trees.get(j)) {
					tree_ID[node]=j;
				}
			}			
		}


		public int get_tree_ID(int node_ID) {
			return tree_ID[node_ID];
		}

		public int get_rank(int node_ID) {
			return ranks[node_ID];
		}

		public int get_node_degree(int node_ID) {
			return degrees[node_ID];
		}

		public int get_tree_size(int tree_ID) {
			return active_trees.get(tree_ID).size();
		}
		public int get_node_ID(int treeID,int rank) {
			return active_trees.get(treeID).get(rank);
		}

		public int[][] get_edges(){
			//return the set of edges in the forest
			int[][] edges=new int[edges_from.size()][2];
			Iterator<Integer> from=edges_from.iterator(),to=edges_to.iterator();
			for(int i=0;i<edges_from.size();i++) {
				edges[i][0]=from.next();
				edges[i][1]=to.next();
			}
			return edges;
		}

		public int get_forest_size() {
			return this.active_trees.size();
		}



		public boolean insert_bridges(ArrayList<DNode> nodes) {
			//find all bridges connecting different trees, insert them to the graph	
			//return value indicates whether an insertion is performed
			boolean ret=false;
			boolean[] visited=new boolean[nodes.size()];//trees being visited
			LinkedList<Integer> queue=new LinkedList<Integer>();//for traversing the graph
			for(int i=0;i<nodes.size();i++) {
				DEdge e=nodes.get(i).edge;				
				while(e!=null) {
					int from_tree_ID=get_tree_ID(i);//has to be here, since it may be changed after one iteration, debugged out 2019-1-28 9:03
					int to_tree_ID=get_tree_ID(e.to.id);
					if(e.from.id>e.to.id||from_tree_ID==to_tree_ID) {//only examine edges along one direction
						e=e.next;
						continue;
					}
					//possible, check whether e is a bridge; check whether we can reach the tree containing e.to 
					boolean is_bridge=true;
					queue.clear();
					Arrays.fill(visited,false);
					visited[from_tree_ID]=true;
					queue.add(from_tree_ID);
					while(!queue.isEmpty()&&is_bridge) {
						ArrayList<Integer> tree=active_trees.get(queue.removeFirst());
						for(Integer nodeID:tree) {
							//nodes belonging to this tree
							DNode node=nodes.get(nodeID);
							DEdge ee=node.edge;								
							while(ee!=null&&is_bridge) {
								int to=get_tree_ID(ee.to.id);
								if(ee!=e&&to==to_tree_ID) {
									//not a bridge
									is_bridge=false;										
								}
								if(ee!=e&&!visited[to]) {
									visited[to]=true;
									queue.addLast(to);										
								}
								ee=ee.next;
							}
							if(!is_bridge) {
								break;
							}
						}		
					}
					if(is_bridge) {
						ret=true;
						insert_edge(e);
					}	
					e=e.next;					
				}
			}
			return ret;

		}

	}







	class MyCacheTable{
		ArrayList<ArrayList<BigInteger[][][]>> tb;

		Forest C;
		ArrayList<DNode> nodes;
		int vertexNum,lengthNum, numOftrees;
		public MyCacheTable(ArrayList<DNode>nodes1,Forest f) {
			C=f;
			nodes=nodes1;
			vertexNum=nodes.size();
			lengthNum=f.get_forest_size();
			numOftrees=f.get_forest_size();
			tb=new ArrayList<ArrayList<BigInteger[][][]>>(numOftrees);
			for(int j=0;j<numOftrees;j++) {		
				ArrayList<BigInteger[][][]> tree_j=new ArrayList<BigInteger[][][]>(f.get_tree_size(j)); 
				for(int rank=0;rank<f.get_tree_size(j);rank++) {
					int nodeID=f.get_node_ID(j,rank);
					int diffence_in_degree=nodes.get(nodeID).deg_G-f.get_node_degree(nodeID);
					BigInteger[][][] tmp=new BigInteger[lengthNum][f.get_tree_size(j)][diffence_in_degree+2];//+1 is fine for most cases;+2 is for the initial call at 0_0 diff+1

					tree_j.add(tmp);
				}
				tb.add(tree_j);
			}
		}		
		public BigInteger get(int j,int i,int l,int k,int g) {			

			BigInteger[][][] t=tb.get(j).get(i);

			return t[l][k][g];		
		}

		public BigInteger set(int j,int i,int l,int k,int g,BigInteger val) {
			tb.get(j).get(i)[l][k][g]=val;
			return val;
		}		
	};



	BigInteger  dF(MyCacheTable tb, int j,int i,int l,int k,int g, ArrayList<DNode> nodes,int []Bs, boolean[]inG,Forest f){				

		if(timeout)return BigInteger.valueOf(0);

		//special case

		if(g<0) {
			return BigInteger.valueOf(0);
		}

		if(l==0&&(g>=1||(g==0&&i!=k))) {
			return tb.set(j,i,l,k,g,BigInteger.valueOf(1));
		}

		if((g==0&&i==k)) {
			return tb.set(j,i,l,k,g, BigInteger.valueOf(0));
		}

		if(tb.get(j,i,l,k,g)!=null) 
			return tb.get(j,i,l,k,g);


		BigInteger count=BigInteger.valueOf(0);
		if(i+1<f.get_tree_size(j)) {//the case for i is dealt with here
			int nextNodeID=f.get_node_ID(j,i+1);
			int degree_difference=Bs[nextNodeID]-f.get_node_degree(nextNodeID);
			count=count.add(dF(tb,j,i+1,l,k,degree_difference,nodes,Bs,inG,f));
		}


		int iID=f.get_node_ID(j,i);
		DEdge e=nodes.get(iID).edge;	

		while(e!=null&&timeout==false) {
			DNode t=e.to;
			int s=f.get_tree_ID(t.id);
			if((inG[s]||s==0)&&s!=j) {//if it equals j, then the edge does not belong to mu_(j,i)
				//should add the test that s==0, which is the only tree that is in the graph but inG[0]=false
				for(int l1=0;l1<=l-1;l1++) {
					if(timeout)return BigInteger.valueOf(0);
					int h=f.get_rank(t.id);
					int s0ID=f.get_node_ID(s,0);
					int degree_diff=Bs[s0ID]-f.get_node_degree(s0ID);
					BigInteger local=dF(tb,s,0,l1,h,degree_diff,nodes,Bs,inG,f).multiply(dF(tb,j,i,l-1-l1,k,g-1,nodes,Bs,inG,f) );
					count=count.add(local);					
				}				
			}
			e=e.next;
		}		
		return tb.set(j,i,l,k,g, count);
	}

	enum COLOR{
		WHITE,BLACK,GRAY;
	}



	class RetIF{
		ArrayList<DNode> newgraph;
		Forest newForest;
		int[] newBs;	
		int[][] edges_between_whites;//edges connecting white vertices
		int[] f;//for mapping old vertex to new vertex
	}

	RetIF initialize_forest(ArrayList<DNode>nodes1,int[]Bs,boolean record) {
		ArrayList<DNode> nodes=GenRWUtils.make_a_copy(nodes1);//must use a different copy; otherwise, the original graph is lost		

		RetIF ret=new RetIF();
		//initialize color
		COLOR[] color=new COLOR[nodes.size()];//0-white 1-black 2-grey-has been deleted
		color[0]=COLOR.WHITE;
		for(int i=1;i<color.length;i++) {
			color[i]=Bs[i]>=nodes.get(i).deg_G?COLOR.WHITE:COLOR.BLACK;
		}
		//edge contraction, node mapping f: from old to new
		int[] f=new int[nodes.size()];
		int node_new_label=0;

		//perform merging
		ArrayList<DEdge> edges_between_whites=new ArrayList<DEdge>();
		for(int i=0;i<color.length;i++) {
			if(color[i]==COLOR.BLACK) {
				//black node, no merging, so assign it a new label now
				f[i]=node_new_label++;
			}
			if(color[i]!=COLOR.WHITE)
				continue;			
			//now for a white vertex


			LinkedList<DNode> queue=new LinkedList<DNode>();
			LinkedList<DNode> black_neighbors=new LinkedList<DNode>();
			LinkedList<DEdge> edges_to_be_deleted=new LinkedList<DEdge>();
			f[i]=node_new_label;//before adding to the queue, assign new label: remember to plus node_new_label at the end
			queue.offer(nodes.get(i));
			color[nodes.get(i).id]=COLOR.GRAY;
			while(!queue.isEmpty()) {
				DNode v=queue.poll();
				DEdge e=v.edge;				
				while(e!=null) {
					if(color[e.to.id]==COLOR.WHITE) {
						f[e.to.id]=node_new_label;//map to the same new vertex
						color[e.to.id]=COLOR.GRAY;
						queue.offer(e.to);
						edges_to_be_deleted.add(e);
						edges_between_whites.add(e);
					}else if(color[e.to.id]==COLOR.BLACK) {
						edges_to_be_deleted.add(e);
						if(!black_neighbors.contains(e.to)) {
							black_neighbors.add(e.to);							
						}							
					}
					e=e.next;
				}
			}//now  white vertices become gray. 

			//delete all edges
			for(DEdge e:edges_to_be_deleted) {
				DNode.deleteEdge(e);DNode.deleteEdge(e.pair);				
			}

			//create new edges, from node i to all black neighbors
			for(DNode t:black_neighbors) {						
				DEdge e=new DEdge(),pair=new DEdge();
				e.from=nodes.get(i);e.to=t;e.previous=e.next=null;
				pair.from=t;pair.to=nodes.get(i);pair.previous=pair.next=null;
				e.pair=pair;pair.pair=e;
				DNode.insertEdge(e); DNode.insertEdge(pair);						
			}			
			//change the color of node i to white; but it should never be visited again
			color[i]=COLOR.WHITE;
			node_new_label++;//must plus label
		}
		//before changing the id of nodes, record edges between whites
		if(record) {
			int[][] whites=new int[edges_between_whites.size()][2];
			Iterator<DEdge> de=edges_between_whites.iterator();
			for(int i=0;i<edges_between_whites.size();i++) {
				DEdge te=de.next();
				whites[i][0]=te.from.id;
				whites[i][1]=te.to.id;
			}
			ret.f=f;
			ret.edges_between_whites=whites;
		}

		//now remove deleted nodes and relabel vertex

		ArrayList<DNode> newgraph=new ArrayList<DNode>();
		ArrayList<Integer> newbounds=new ArrayList<Integer>();
		for(int i=0;i<nodes.size();i++) {
			if(color[i]!=COLOR.GRAY) {
				//add it to the new graph
				DNode cnode=nodes.get(i);
				cnode.id=f[i];
				newgraph.add(cnode);
				if(color[i]==COLOR.WHITE) {
					newbounds.add(nodes.size());
				}else {
					newbounds.add(Bs[i]);
				}
			}
		}

		int[] newBs=new int[newgraph.size()];
		for(int i=0;i<newBs.length;i++) {
			DNode v=newgraph.get(i);
			DEdge e=v.edge;
			v.deg_G=0;
			while(e!=null) {
				v.deg_G++;
				e=e.next;
			}			
			newBs[i]=newbounds.get(i);
			if(newBs[i]>v.deg_G) {
				newBs[i]=v.deg_G;
			}
		}



		//build forest
		Forest forest=new Forest(newgraph.size());
		forest.initialize(newgraph);
		if(debug)System.out.println("reduce nodes # from "+nodes.size()+" to "+newgraph.size());

		//find the white vertex with largest out degree
		//compute the degree of nodes in the new graph
		DNode largestwhite=null;
		for(DNode dn:newgraph) {					
			if(newBs[dn.id]>=dn.deg_G) {
				//white vertex
				if(largestwhite==null||dn.deg_G>largestwhite.deg_G) {
					largestwhite=dn;
				}
			}
		}

		if(largestwhite!=null) {
			DEdge de=largestwhite.edge;
			while(de!=null) {
				forest.insert_edge(de);
				de=de.next;
			}
			if(debug)System.out.println("largest white node is "+largestwhite.id);
		}

		//delete redundant edges
		Refine_Graph(newgraph,newBs,forest);		
		ret.newgraph=newgraph;
		ret.newBs=newBs;
		ret.newForest=forest;

		return ret;
	}

	public void Refine_Graph(ArrayList<DNode> graph,int[] Bs,Forest forest) {
		boolean a=true,b=true;
		while(a||b) {		
			for(int i=0;i<Bs.length;i++) {
				if(Bs[i]>graph.get(i).deg_G) {
					Bs[i]=graph.get(i).deg_G;
				}
			}
			a=remove_redundant_edges(graph,Bs,forest);
			b=forest.insert_bridges(graph);
		}
	}

	private boolean remove_redundant_edges(ArrayList<DNode> newgraph,int[] newBs,Forest forest) {
		//warning: it changes the structure of the input graph, it deletes some edges
		boolean ret=false;
		LinkedList<DEdge> toRemove=new LinkedList<DEdge>();
		for(int i=0;i<newgraph.size();i++) {
			DEdge e=newgraph.get(i).edge;
			while(e!=null) {
				if(e.from.id<e.to.id&&e.inSPT==false) {
					//only examine in one direction
					if(forest.get_tree_ID(e.from.id)==forest.get_tree_ID(e.to.id)) {
						toRemove.add(e);
					}else if(newBs[e.from.id]<=forest.get_node_degree(e.from.id)||newBs[e.to.id]<=forest.get_node_degree(e.to.id)) {
						toRemove.add(e);
					}
				}
				e=e.next;
			}
		}
		for(DEdge e:toRemove) {
			DNode.deleteEdge(e.pair);DNode.deleteEdge(e);
			e.from.deg_G--;e.to.deg_G--;
			ret=true;
		}
		return ret;
	}


	int[] degree_bounds(ArrayList<DNode> nodes,double load) {
		//preconditon: ensure that deg_G field is correct for all nodes including the sink
		//find compute degree upper bounds Bi
		return GenRWUtils.degree_bounds(nodes, load);

	}

	private boolean BDST_Forest(ArrayList<DNode>nodes,int[] Bs,Forest C) {
		// TODO Auto-generated method stub

		if(this.enable_print)
			System.out.println(GenRWUtils.cID+" "+graph.size()+" "+C.nodeNum+" "+C.get_forest_size());

		if(!GenRWUtils.is_connected(nodes)) {
			return false;
		}

		//perform basic check
		for(DNode n:nodes) {
			if(Bs[n.id]<C.get_node_degree(n.id)) {//already infeasible
				return false;
			}

			if(Bs[n.id]>n.deg_G) {
				Bs[n.id]=n.deg_G;
			}
		}


		BigInteger count=BigInteger.valueOf(0);

		//enumerating all possibilities of a forest
		boolean[] inG=new boolean[C.get_forest_size()];//inG[0] is unused
		int current_F=C.get_forest_size()-1;//all trees but T_0 are not in the graph
		Arrays.fill(inG,false);
		while(inG[0]==false&&timeout==false) {
			//check current set  int j,int i,int l,int k,int g, ArrayList<DNode> nodes,int []Bs, boolean[]inG,Forest f
			MyCacheTable tb=new MyCacheTable(nodes, C);
			BigInteger local_count=dF(tb,0,0,C.get_forest_size()-1,0,Bs[0]+1-C.get_node_degree(0), nodes,Bs,inG,C);
			if(current_F%2==0) {
				count=count.add(local_count);
			}else {
				count=count.subtract(local_count);
			}

			//next set; plus one
			boolean carry=true;
			int cur=inG.length-1;
			while(carry) {
				if(inG[cur]) {
					inG[cur]=false;
					current_F++;
					cur--;
				}else {
					inG[cur]=true;
					current_F--;
					carry=false;
				}				
			}			
		}

		if(count.compareTo(BigInteger.valueOf(0))<0) {
			if(!timeout)System.out.println("error! count is negative @BDST()");
		}
		return count.compareTo(BigInteger.valueOf(0))==1;		
	}

	public void print_BDST_forest_problem(ArrayList<DNode> nodes,int[]Bs,Forest C) {
		//debug purpose only
		System.out.println("BDST instance ");
		for(int i=0;i<nodes.size();i++) {
			DEdge e=nodes.get(i).edge;
			while(e!=null) {
				if(e.from.id<e.to.id) {
					System.out.println(e.from.id+" "+e.to.id);
				}
				e=e.next;
			}			
		}
		for(int i=0;i<Bs.length;i++) {
			System.out.println("B[ "+i+"]="+Bs[i]+" deg_C("+i+")="+C.get_node_degree(i));
		}

		for(int i=0;i<Bs.length;i++) {
			System.out.println("vertex "+i+" belongs to tree "+C.get_tree_ID(i)+" with rank "+C.get_rank(i));
		}		

	}


	boolean debug=false;
	boolean enable_print=true;

	RetIF ret=null;//to get a spanning tree if timeout

	@Override
	public void compute_optimal() {
		// TODO Auto-generated method stub
		timeout=false;phase=1;
		this.numcallsFirstStep=this.numcallsSecondStep=0;
		double[] possible_loads=generate_possible_loads(graph);
		this.optobj=possible_loads[possible_loads.length-1];

		int high=possible_loads.length-1;//always feasible
		//test whether apx is optimal
		int test=high-1;
		{
			if(test<0) {
				//one feasilb load, apx is optimal
				if(debug) {
					System.out.println("one feasible load, apx is optimal");
				}
				optobj=possible_loads[high];		
				phase=3;
				return;	
			}
			double load=possible_loads[test];
			int []Bs=degree_bounds(graph,load);

			if(debug) {
				System.out.print("Testing load "+load+": ");
				if(Bs==null) {
					System.out.print(" infeasible");
				}else {
					for(int i=0;i<Bs.length;i++) {
						System.out.print("("+Bs[i]+","+graph.get(i).deg_G+") ");
					}
				}

				System.out.println();				
			}

			boolean feasible;
			if(Bs==null)//degree bound 0 appears
				feasible=false;
			else {
				this.numcallsFirstStep++;
				RetIF re=this.initialize_forest(graph, Bs, false);

				feasible=BDST_Forest(re.newgraph,re.newBs,re.newForest);			
			}
			if(timeout)return;

			if(feasible) {
				//test is feasible
				if(debug)System.out.println("feasible");
				high=test;				
			}else{
				//test is infeasible;apx is optimal
				if(debug)System.out.println("infeasible");	
				optobj=possible_loads[high];		
				phase=3;
				return;				
			}	
		}
		//apx is not optimal
		//test whether the lower bound is feasible
		int low=0;		
		if(timeout==false){
			double load=possible_loads[low];
			int []Bs=degree_bounds(graph,load);

			if(debug) {
				System.out.print("Testing load "+load+": ");
				if(Bs==null) {
					System.out.print(" infeasible degree bounds");
				}else {
					for(int i=0;i<Bs.length;i++) {
						System.out.print("("+Bs[i]+","+graph.get(i).deg_G+") ");
					}
				}

				System.out.println();				
			}

			boolean feasible;
			if(Bs==null)//degree bound 0 appears
				feasible=false;
			else {
				this.numcallsFirstStep++;
				RetIF re=this.initialize_forest(graph, Bs, false);

				feasible=BDST_Forest(re.newgraph,re.newBs,re.newForest);			
			}
			if(timeout)return;

			if(feasible) {
				//low is feasible; i.e., we found the optimal value
				if(debug)System.out.println("feasible");				
				high=low;				
			}else {
				//low is infeasible
				if(debug)System.out.println("infeasible");						
			}	
		}
		while(low<high-1&&timeout==false) {
			int mid=(low+high)/2;
			double load=possible_loads[mid];
			int []Bs=degree_bounds(graph,load);
			//	if(debug)
			//		System.out.println("Testing lifetime "+1/load);
			boolean feasible;
			if(Bs==null)//degree bound 0 appears
				feasible=false;
			else {
				if(debug) {
					System.out.print("Testing load "+load+": ");
					for(int i=0;i<Bs.length;i++) {
						System.out.print("("+Bs[i]+","+graph.get(i).deg_G+") ");
					}
					System.out.println();
				}
				this.numcallsFirstStep++;
				RetIF re=this.initialize_forest(graph, Bs, false);	

				feasible=BDST_Forest(re.newgraph,re.newBs,re.newForest);			
			}
			if(timeout)return;

			if(feasible) {
				//mid is feasible
				if(debug) System.out.println("feasible");
				high=mid;
			}else {
				//mid is infeasible
				if(debug)	System.out.println("infeasible");
				low=mid;
			}			
		}			

		if(timeout)return;

		optobj=possible_loads[high];		
		phase=2;

		//now it is time to find one feasible tree
		int []Bs=degree_bounds(graph,possible_loads[high]);
		ret=this.initialize_forest(graph, Bs, true);

		ArrayList<DNode> cgraph=ret.newgraph;
		int[] nBs=ret.newBs;
		Forest C=ret.newForest;

		while(C.get_forest_size()!=1) {
			//make a copy of graph and forest
			ArrayList<DNode> backupgraph=GenRWUtils.make_a_copy(cgraph);
			Forest backupforest=C.clone();		
			int[] backupBs=new int[nBs.length];
			for(int i=0;i<nBs.length;i++) {
				backupBs[i]=nBs[i];
			}

			//find an edge connecting two trees
			DEdge e=null;
			for(DNode dn:cgraph) {
				DEdge ee=dn.edge;
				while(ee!=null) {
					if(C.get_tree_ID(ee.from.id)!=C.get_tree_ID(ee.to.id)) {
						e=ee;//found
						break;
					}
					ee=ee.next;
				}
				if(e!=null) break;
			}


			if(e==null) {//wrong
				//error print out the topology
				System.out.println("cannot find an edge between two trees");
				this.print_BDST_forest_problem(cgraph, nBs, C);
			}
			C.insert_edge(e);
			Refine_Graph(cgraph,nBs,C);

			this.numcallsSecondStep++;
			if(!BDST_Forest(cgraph,nBs,C)) {
				if(timeout)return;

				C=backupforest;
				cgraph=backupgraph;
				nBs=backupBs;
				DEdge toDel=find_edge(cgraph.get(e.from.id),e.to.id);
				DNode.deleteEdge(toDel);
				DNode.deleteEdge(toDel.pair);
				toDel.from.deg_G--;
				toDel.to.deg_G--;
				Refine_Graph(cgraph,nBs,C);						
			}	
			if(timeout) return;
		}

		//recover a tree

		//re-set in_spt to false
		for(DNode dn:graph) {
			DEdge e=dn.edge;
			while(e!=null) {
				e.inSPT=false;
				e=e.next;
			}
		}

		//first, connect white vertices
		int[][] whites=ret.edges_between_whites;
		for(int[] ed:whites) {
			int from=ed[0],to=ed[1];
			//find the edge in G
			DEdge e=find_edge(graph.get(from),to);
			if(e==null) {
				System.out.println("the edge does not exist in the original graph");
			}
			e.inSPT=true;
			e.pair.inSPT=true;
		}

		//now, the edges in the forest

		//inverse function f
		ArrayList<ArrayList<DNode>> invf=new ArrayList<ArrayList<DNode>>(cgraph.size());
		for(int i=0;i<cgraph.size();i++) {
			invf.add(new ArrayList<DNode>());
		}
		for(DNode dn:graph) {
			invf.get(ret.f[dn.id]).add(dn);
		}	


		int[][] edges_from_new_graph=C.get_edges();
		for(int[] ed:edges_from_new_graph) {
			DEdge e=null;
			int from=ed[0],to=ed[1];//index in the new graph
			ArrayList<DNode> fromList=invf.get(from),toList=invf.get(to);
			for(DNode u:fromList) {
				for(DNode v:toList) {
					DEdge ee=find_edge(u,v.id);
					if(ee!=null) {
						e=ee;
						break;
					}
				}
				if(e!=null) break;
			}

			//now insert e into the tree
			e.inSPT=true;e.pair.inSPT=true;
		}
		phase=3;
		/*
		//check whether it is a tree and its lifetime
		double life=GenRWUtils.check_tree_compute_lifetime(graph);
		if(Math.abs(life-optlife)>MIN_VALUE) {
			System.out.println("wrong+ optlife= "+optlife+" current tree life="+life);
		}*/
	}

	DEdge find_edge(DNode v,int to) {
		DEdge e=v.edge;
		while(e!=null) {
			if(e.to.id==to) {
				return e;
			}
			e=e.next;
		}
		return null;
	}
}



