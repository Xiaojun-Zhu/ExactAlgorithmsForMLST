import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;


public class AlgMerge extends Solver{

	public static final double MIN_VALUE=0.0000000001;

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
		nina.set_graph(nodes);
		nina.compute_optimal();

		double a=nina.get_optimal(),lower=a;
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
		loads.add(lower-1);//ensure there is at least one infeasible value

		DNode sink=nodes.get(0);
		sink.deg_G=0;
		DEdge e=sink.edge;
		while(e!=null) {
			sink.deg_G++;
			e=e.next;
		}
		for(int i=1;i<nodes.size();i++) {
			DNode n=nodes.get(i);
			for(int j=1;j<=n.deg_G;j++) {
				double cur=f(n.E,j);
				if(cur>=lower-MIN_VALUE&&cur<=a+MIN_VALUE)//again, numerical instability problem
					loads.add(cur);
			}
		}
		Collections.sort(loads);
		double[] t=new double[loads.size()];
		for(int i=0;i<t.length;i++) {
			t[i]=loads.get(i);
	//	    System.out.print(" "+t[i]);
		}
		//System.out.println(" lower="+1/lower+" upper "+1/a);
		return t;	
	}


	class MyCacheTable{
		ArrayList<BigInteger[][]> tb;

		int vertexNum,lengthNum;
		public MyCacheTable(ArrayList<DNode>nodes) {
			vertexNum=nodes.size();
			lengthNum=nodes.size();
			tb=new ArrayList<BigInteger[][]>(vertexNum);
			for(int i=0;i<vertexNum;i++) {				
				BigInteger[][] len=new BigInteger[lengthNum][nodes.get(i).deg_G+1];
				for(BigInteger[] t:len) {
					for(int j=0;j<t.length;j++) {
						t[j]=null;
					}					
				}				
				tb.add(len);
			}
		}		
		public BigInteger get(int i,int j,int k) {
			if(i<0||i>=vertexNum||j<0||j>=lengthNum) {
				System.out.println("error@MyCacheTable get("+i+","+j+","+k+") vertexNum "+vertexNum+", lengthNum "+lengthNum);
				return null;
			}

			return tb.get(i)[j][k];			
		}

		public BigInteger set(int i,int j,int k,BigInteger val) {
			tb.get(i)[j][k]=val;
			return val;
		}		
	};



	BigInteger  dF(MyCacheTable tb, int i,int j,int k, ArrayList<DNode> nodes,int []Bs, boolean[]inG){			
		if(timeout)return BigInteger.valueOf(0);

		if(tb.get(i, j, k)!=null) return tb.get(i, j, k);

		//special case
		if(j==0) {
			return tb.set(i, j, k,BigInteger.valueOf(1));
		}
		if(j>=1&&k==0) {
			return tb.set(i, j, k, BigInteger.valueOf(0));
		}

		DEdge e=nodes.get(i).edge;
		BigInteger count=BigInteger.valueOf(0);
		while(e!=null&&timeout==false) {
			DNode t=e.to;
			if(inG[t.id]) {
				//t is in the graph				
				for(int j1=0;j1<=j-1;j1++) {
					if(timeout)return BigInteger.valueOf(0);
					BigInteger local=dF(tb,t.id,j1,Bs[t.id]-1,nodes,Bs,inG).multiply(dF(tb,i,j-1-j1,k-1,nodes,Bs,inG) );

					count=count.add(local);					
				}				
			}
			e=e.next;
		}		
		return tb.set(i, j, k, count);
	}

	enum COLOR{
		WHITE,BLACK,GRAY;
	}
	boolean enable_print=false;
	boolean BDSTwithWhiteVertexMerge(ArrayList<DNode>nodes1, int[] Bs) {
		//assumption: Bs are all positive
		if(Bs==null)return false;

		ArrayList<DNode> nodes=GenRWUtils.make_a_copy(nodes1);//must use a different copy; otherwise, the original graph is lost		

		//initialize color
		COLOR[] color=new COLOR[nodes.size()];//0-white 1-black 2-grey-has been deleted
		color[0]=COLOR.WHITE;
		for(int i=1;i<color.length;i++) {
			color[i]=Bs[i]>=nodes.get(i).deg_G?COLOR.WHITE:COLOR.BLACK;
		}

		//perform merging
		for(int i=0;i<color.length;i++) {
			if(color[i]!=COLOR.WHITE)
				continue;			
			//now for a white vertex
			LinkedList<DNode> queue=new LinkedList<DNode>();
			LinkedList<DNode> black_neighbors=new LinkedList<DNode>();
			LinkedList<DEdge> edges_to_be_deleted=new LinkedList<DEdge>();
			queue.offer(nodes.get(i));
			color[nodes.get(i).id]=COLOR.GRAY;
			while(!queue.isEmpty()) {
				DNode v=queue.poll();
				DEdge e=v.edge;				
				while(e!=null) {

					if(color[e.to.id]==COLOR.WHITE) {
						color[e.to.id]=COLOR.GRAY;
						queue.offer(e.to);
						edges_to_be_deleted.add(e);
					}else if(color[e.to.id]==COLOR.BLACK) {
						edges_to_be_deleted.add(e);
						if(!black_neighbors.contains(e.to))
							black_neighbors.add(e.to);
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
		}

		//now remove deleted nodes and relabel vertex
		int j=0;
		ArrayList<DNode> newgraph=new ArrayList<DNode>();
		ArrayList<Integer> newbounds=new ArrayList<Integer>();
		for(int i=0;i<nodes.size();i++) {
			if(color[i]!=COLOR.GRAY) {
				//add it to the new graph
				DNode cnode=nodes.get(i);
				cnode.id=j++;
				newgraph.add(cnode);
				if(color[i]==COLOR.WHITE) {
					newbounds.add(nodes.size());
				}else {
					newbounds.add(Bs[i]);
				}
			}			
		}
		if(newgraph.size()==1)
			return true;
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
	/*	for studying the exponent*/
	  if(this.enable_print)
		System.out.println(GenRWUtils.cID+" "+nodes.size()+" "+newgraph.size());

		return BDST(newgraph,newBs);

	}


	int[] degree_bounds(ArrayList<DNode> nodes,double load) {
		//preconditon: ensure that deg_G field is correct for all nodes including the sink
		//find compute degree upper bounds Bi
		return GenRWUtils.degree_bounds(nodes, load);

	}

	boolean BDST(ArrayList<DNode> nodes,int[] Bs ) {
		//assumption: all elements of Bs are positive

		BigInteger count=BigInteger.valueOf(0);

		//enumerating all possibilities of a set
		boolean[] inG=new boolean[nodes.size()];//inF[0] is unused
		int current_F=nodes.size()-1;//all n-1 nodes are not in the figure
		Arrays.fill(inG,false);
		while(inG[0]==false&&timeout==false) {
			//check current set			
			MyCacheTable tb=new MyCacheTable(nodes);
			BigInteger local_count=dF(tb,0,nodes.size()-1,nodes.get(0).deg_G,nodes,Bs,inG);
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




	@Override
	public void compute_optimal() {
		// TODO Auto-generated method stub
		timeout=false;
		phase=1;
		this.numcallsFirstStep=this.numcallsSecondStep=0;
		double[] possible_loads=generate_possible_loads(graph);
		int low=0;//infeasible
		int high=possible_loads.length-1;//always feasible
		while(low<high-1) {
			int mid=(low+high)/2;
			double load=possible_loads[mid];
			int []Bs=degree_bounds(graph,load);
			boolean feasible;
			if(Bs==null)//degree bound 0 appears
				feasible=false;
			else {
				this.numcallsFirstStep++;
				feasible=BDSTwithWhiteVertexMerge(graph,Bs);
				if(timeout)return;
				// feasible=BDST(graph,Bs);
			}

			if(feasible) {
				//mid is feasible
				high=mid;
			}else {
				//mid is infeasible
				low=mid;
			}			
		}

		optobj=possible_loads[high];		
        
		phase=2;		
		//now it is time to find one feasible tree
		LinkedList<DEdge> edges=new LinkedList<DEdge>();
		for(int i=0;i<graph.size();i++) {
			DEdge e=graph.get(i).edge;
			while(e!=null) {
				if(e.from.id<=e.to.id) {
					edges.add(e);
				}
				e=e.next;
			}
		}

		for(DEdge e:edges) {			
			DNode.deleteEdge(e);DNode.deleteEdge(e.pair);
			e.from.deg_G--;e.to.deg_G--;
			this.numcallsSecondStep++;
			if(!BDSTwithWhiteVertexMerge(graph,degree_bounds(graph,possible_loads[high]))) {
				if(timeout)return;
				DNode.insertEdge(e);DNode.insertEdge(e.pair);
				e.from.deg_G++;e.to.deg_G++;
			}
			if(timeout)return;
		}
		phase=3;
	}
	
	


}
