import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;


public class AlgBasic extends Solver{

	public static final double MIN_VALUE=0.000001;//for solving the numerical instability problem
	
	
	double [] generate_possible_loads(ArrayList<DNode> nodes) {
		//return possible loads in ascending order
		//also establish the deg_G field
		ArrayList<Double> loads=new ArrayList<Double>();
		DNode sink=nodes.get(0);
		sink.deg_G=0;
		DEdge e=sink.edge;
		while(e!=null) {
			sink.deg_G++;
			e=e.next;
		}
		for(int i=1;i<nodes.size();i++) {
			DNode n=nodes.get(i);
			n.deg_G=0;
			e=n.edge;
			while(e!=null) {
				n.deg_G++;
				e=e.next;
			}
			for(int j=1;j<=n.deg_G;j++) {
				loads.add(f(n.E,j));
			}
		}
		Collections.sort(loads);
		double[] t=new double[loads.size()];
		for(int i=0;i<t.length;i++) {
			t[i]=loads.get(i);
	//		System.out.print(" "+t[i]);
		}
		//System.out.println();
		return t;	
	}
	

	double f(double ei,int j) {		
		return GenRWUtils.f(ei, j);	
	}
	
	int[] degree_bounds(ArrayList<DNode> nodes,double load) {
		return GenRWUtils.degree_bounds(nodes, load);
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



	
	boolean BDST(ArrayList<DNode> nodes,int[] Bs) {
		//preconditon: ensure that deg_G field is correct for all nodes including the sink
		//find compute degree upper bounds Bi
		
		if(Bs==null) return false;//add this corner case when updating functions, weird.
				

		BigInteger count=BigInteger.valueOf(0);
		
		//enumerating all possibilities of a set
		boolean[] inG=new boolean[nodes.size()];//inF[0] is unused
		int current_F=nodes.size()-1;//all n-1 nodes are not in the figure
		Arrays.fill(inG,false);
		while(inG[0]==false&&timeout==false) {
			//check current set
			MyCacheTable tb=new MyCacheTable(nodes);
			BigInteger local_count=dF(tb,0,nodes.size()-1,nodes.get(0).deg_G,nodes,Bs,inG);
			if(timeout)return false;
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
		if(timeout)return false;
		if(count.compareTo(BigInteger.valueOf(0))<0) {
			System.out.println("error! count is negative @BDST()");
		}
		
		return count.compareTo(BigInteger.valueOf(0))==1;		
	}
	

	
   double first_step_seconds,second_step_seconds;
	@Override
	public void compute_optimal() {
		// TODO Auto-generated method stub
		start_time=System.nanoTime();
		timeout=false;
		phase=1;
		this.numcallsFirstStep=this.numcallsSecondStep=0;
		double[] possible_loads=generate_possible_loads(graph);		
		int low=0;//infeasible
		int high=possible_loads.length-1;//always feasible
		while(low<high-1&&timeout==false) {
			int mid=(low+high)/2;
			double load=possible_loads[mid];
			
			int []Bs=degree_bounds(graph,load);
			boolean feasible;
			if(Bs==null)//degree bound 0 appears
				feasible=false;
			else {
				this.numcallsFirstStep++;
				feasible=BDST(graph,Bs);
				if(timeout) {
					return;
				}
				this.numcallsFirstStep++;
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
		
		
		first_step_seconds=(System.nanoTime()-start_time)*1.0/1000000000;
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
			if(!BDST(graph,degree_bounds(graph,possible_loads[high]))) {
				if(timeout)return;
				DNode.insertEdge(e);DNode.insertEdge(e.pair);
				e.from.deg_G++;e.to.deg_G++;
			}
			if(timeout)return;
		}
		phase=3;
		second_step_seconds=(System.nanoTime()-start_time)*1.0/1000000000-first_step_seconds;
		
	}
	
	
	
	public void print_num_of_whites(PrintStream ps,int k) {//for use in the evaluation section of the paper		
		double[] possible_loads=generate_possible_loads(graph);	
		for(int i=0;i<possible_loads.length;i++) {
			double load=possible_loads[i];
			int []Bs=degree_bounds(graph,load);
			if(Bs==null) continue;
			
			//count whites
			int c=0;
			for(int j=0;j<graph.size();j++) {
				if(graph.get(j).deg_G<=Bs[j])c++;
			}
			
			ps.println(load+" "+c+" "+k);
			
		}
	}
	
}
