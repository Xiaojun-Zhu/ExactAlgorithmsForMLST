import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

//utility functions
//generate samples, store samples, read a network, etc.
public class GenRWUtils {
	static final double radius=20;//communication radius 20m
	static final double side_length=100;//100*100 square
	static final double max_energy=10;//maximum possible energy
	
	static enum LoadFuncType{
		Linear, Cubic, Logarithm;		
	}
	
	public static LoadFuncType functype;
	
	public static double f(double ei,int j) {
		double ret;		
		if(functype==GenRWUtils.LoadFuncType.Linear) {
			ret= ( (j-1)*DNode.Rx+DNode.Tx) /ei;
		}else if(functype==GenRWUtils.LoadFuncType.Cubic) {
			if(ei==Double.MAX_VALUE)
				ret=0;
			else {
				// ret=(j-2)*(j-2)*(j-2)/ei;
				//ret=(j-1)*(j-1)*(j-1)/ei;
				ret=(j+1)*(j+1)*(j+1)/ei/ei/ei;
			}
			   
		}else {
			ret=Math.log( j*DNode.Rx /ei);
		}		
		return ret;		
	}
	
	public static int[] degree_bounds(ArrayList<DNode> nodes,double load) {
		//preconditon: ensure that deg_G field is correct for all nodes including the sink
		//find compute degree upper bounds Bi
		
		int []Bs=new int[nodes.size()];//
		Bs[0]=nodes.get(0).deg_G;
		for(int i=1;i<Bs.length;i++) {
			int j=0;
			for(j=1;j<=nodes.get(i).deg_G;j++) {
				if(f(nodes.get(i).E,j)>load)
					break;
			}
			Bs[i]=j-1;
			
			if(Bs[i]==0) return null;
			
		}		
		return Bs;
	}
	
	public static int[] milp_bounds(ArrayList<DNode> nodes,double load) {
		//preconditon: ensure that deg_G field is correct for all nodes including the sink
		//find compute degree upper bounds Bi
		
		int []Bs=new int[nodes.size()];//	
		nodes.get(0).E=Double.MAX_VALUE;
		for(int i=0;i<Bs.length;i++) {
			int j=0;
			for(j=1;j<=nodes.size();j++) {
				if(f(nodes.get(i).E,j)>load)
					break;
			}
			Bs[i]=j-1;		
		}		
		return Bs;
	}

	private static ArrayList<DNode> generate_a_network(int n){  //n=total number of nodes (including the sink)
		ArrayList<DNode> gn=new ArrayList<DNode>();
		//the sink		
		double l=side_length;
		double El=1;
		double Eh=max_energy;
		DNode sink=new DNode();
		sink.id=0;
		sink.x=l/2;
		sink.y=l/2;
		sink.E=0;
		gn.add(sink);
		for(int i=1;i<n;i++){
			DNode g=new DNode();
			g.id=i;
			g.x=Math.random()*l;
			g.y=Math.random()*l;		
			g.E=El+Math.random()*(Eh-El);
			g.visited=false;
			gn.add(g);
		}

		for(int i=0;i<n-1;i++){
			DNode u=gn.get(i);
			for(int j=i+1;j<n;j++){
				DNode v=gn.get(j);
				double dist=Math.sqrt((u.x-v.x)*(u.x-v.x)+(u.y-v.y)*(u.y-v.y));
				if(dist<=radius){
					DEdge e=new DEdge();e.from=u;e.to=v;e.inSPT=false;e.previous=null;e.next=null;DNode.insertEdge(e);
					DEdge e2=new DEdge();e2.from=e.to;e2.to=e.from;e2.inSPT=false;e2.previous=null;e2.next=null;
					e2.pair=e;e.pair=e2;DNode.insertEdge(e2);		
				}
			}
		}
		if(is_connected(sink,gn)){
			return gn;
		}else{
			return null;
		}		
	}

	public static double compute_objective(ArrayList<DNode> nodes) {
		double l=-1;
		for(DNode n:nodes) {
			n.visited=false;
		}
		if(is_connected(nodes.get(0),nodes)) {
			//compute degree
			for(DNode n:nodes) {
				n.deg_G=0;
				DEdge e=n.edge;
				while(e!=null) {
					n.deg_G++;
					e=e.next;
				}
				if(n.id==0) continue;
				double local=f(n.E,n.deg_G);
				if(local>l||l<0) l=local;
			}
		}

		return l;
	}

	public static double check_tree_compute_objective(ArrayList<DNode> nodes) {
		//check whether inspt indicates a tree
		double l=-1;
		boolean [] visit=new boolean[nodes.size()];
		Arrays.fill(visit,false);    	
		LinkedList<DNode> queue=new LinkedList<DNode>();
		visit[0]=true;
		queue.add(nodes.get(0));
		while(!queue.isEmpty()) {
			DNode n=queue.poll();
			DEdge e=n.edge;
	
			while(e!=null) {
				if(e.inSPT) {

					if(!visit[e.to.id]) {
						visit[e.to.id]=true;
						queue.offer(e.to);
					}
				}
				e=e.next;
			}
			if(n.id==0) continue;
			double local=f(n.E,n.deg_G);
			if(local>l||l<0) l=local;		
		}

		//check whether all nodes are visited
		for(int i=0;i<visit.length;i++) {
			if(visit[i]==false)
				return -1;
		}

		return l;
	}

	static boolean is_connected(ArrayList<DNode>nodes) {
		//without assumption to any field value; without changing any field value
		boolean [] visit=new boolean[nodes.size()];
		Arrays.fill(visit,false);    	
		LinkedList<DNode> queue=new LinkedList<DNode>();
		visit[0]=true;
		queue.add(nodes.get(0));
		while(!queue.isEmpty()) {
			DNode n=queue.poll();
			DEdge e=n.edge;    	
			while(e!=null) {

				if(!visit[e.to.id]) {
					visit[e.to.id]=true;
					queue.offer(e.to);
				}

				e=e.next;
			}    			
		}

		//check whether all nodes are visited
		for(int i=0;i<visit.length;i++) {
			if(visit[i]==false)
				return false;
		}
		return true;
	}

	static boolean is_connected(DNode s,ArrayList<DNode>ns){
		DFS_test_connectivity(s);
		boolean b=true;
		for(DNode d:ns){
			if(d.visited==false){
				if(b)b=false;
			}else{
				d.visited=false;
			}
		}
		return b;
	}

	static public void DFS_test_connectivity(DNode v){
		v.visited=true;
		DEdge e=v.edge;
		while(e!=null){
			if(e.to.visited==false){
				DFS_test_connectivity(e.to);
			}
			e=e.next;
		}
	}


	private static ArrayList<DNode> generate_connected_network(int n){	
		ArrayList<DNode> nodes=null;
		while((nodes=generate_a_network(n))==null);	
		return nodes;
	}

	public static void store_samples(String fileName,int[] numsOfnodes,int numPer) {
		//generate numPer*numOfnodes.length networks
		FileOutputStream f ;PrintStream ps;
		try {
			f=new FileOutputStream(fileName, true);
			ps=new PrintStream(f);
			int label=0;
			for(int i=0;i<numsOfnodes.length;i++){
				int numNodes=numsOfnodes[i];
				for(int j=0;j<numPer;j++) {
					ArrayList<DNode> nodes=generate_connected_network(numNodes);	
					int dedge_count = 0;//number of directed edges
					for(DNode d:nodes){				
						DEdge e=d.edge;					
						while(e!=null){
							dedge_count++;					
							e=e.next;
						}
					}
					ps.println(label+" "+nodes.size()+" "+dedge_count+" "+DNode.Tx+" "+DNode.Rx);//first line; label numofnodes numofdirectededges Tx  Rx				

					//the second line, the energy of all nodes
					for(DNode g:nodes)
						ps.print(""+g.E+" ");
					ps.println();
					//the third line, edge| assuming undirected graph					
					for(DNode d:nodes){				
						DEdge e=d.edge;					
						while(e!=null){						
							if(e.to.id>e.from.id){
								ps.print(""+e.from.id+" "+e.to.id+" ");								
							}
							e=e.next;
						}
					}
					System.out.println(""+label+": "+nodes.size()+" "+dedge_count);
					label++;
					ps.println();
					//the fourth line, location; 
					for(DNode d:nodes){
						ps.print(""+d.x+" "+d.y+" ");
					}
					ps.println();				
					ps.flush();							
				}		

			}		
			ps.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}

	ArrayList<DNode> cgraph=null;
	static int cID=-1;
	int n=-1;
	int numDedge=-1;

	BufferedReader in;


	public boolean prepareRead(String filename) {
		try {
			in= new BufferedReader(new FileReader(filename));
			cgraph=null;
			cID=-1;
			numDedge=-1;
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	static public ArrayList<DNode> make_a_copy(ArrayList<DNode> inputnodes){	
		ArrayList<DNode> newnodes=new ArrayList<DNode>(inputnodes.size());
		for(DNode n:inputnodes){
			DNode nn=new DNode();
			nn.x=n.x;nn.y=n.y;nn.E=n.E;nn.id=n.id;nn.visited=false;nn.degree=n.degree;nn.deg_G=n.deg_G;
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


public static ArrayList<DNode> retrieve_network(int cID,String filename){
	GenRWUtils grwn=new GenRWUtils();
	grwn.prepareRead(filename);
	ArrayList<DNode> cgraph=null;
	while((cgraph=grwn.nextNetwork())!=null) {
		int tID=grwn.cID;
		if(tID==cID) {
			return cgraph;
		}
	}
	return null;
}


	public ArrayList<DNode> make_a_copy(){
		return make_a_copy(cgraph);
	}

	public ArrayList<DNode> nextNetwork(){
		//read and pass the next network
		String s;
		try {
			s = in.readLine();

			if(s==null)
				return null;
			String[] t = s.split("\\s+");
			cID=Integer.parseInt(t[0]);
			n=Integer.parseInt(t[1]);
			numDedge=Integer.parseInt(t[2]);
			DNode.Tx=Double.parseDouble(t[3]);
			DNode.Rx=Double.parseDouble(t[4]);

			String energy, edge, pos;

			energy = in.readLine();
			edge = in.readLine();
			pos = in.readLine();


			t = energy.split("\\s+");
			ArrayList<DNode> gr = new ArrayList<DNode>(t.length);
			for (int i = 0; i < t.length; i++) {
				DNode g = new DNode();
				g.E = Double.parseDouble(t[i]);
				g.id = i;
				gr.add(g);
			}
			t = edge.split("\\s+");
			for (int i = 0; i < t.length; i += 2) {
				DNode u = gr.get(Integer.parseInt(t[i])), v = gr.get(Integer.parseInt(t[i + 1]));
				DEdge e = new DEdge();
				e.from = u;
				e.to = v;
				e.inSPT = false;
				e.previous = null;
				e.next = null;
				DNode.insertEdge(e);
				DEdge e2 = new DEdge();
				e2.from = e.to;
				e2.to = e.from;
				e2.inSPT = false;
				e2.previous = null;
				e2.next = null;
				e2.pair = e;
				e.pair = e2;
				DNode.insertEdge(e2);
			}

			//set positions  
			t = pos.split("\\s+");
			for(int i=0;i<t.length;i+=2){
				//	System.out.println(t[i]);
				gr.get(i/2).x=Double.parseDouble(t[i]);
				gr.get(i/2).y=Double.parseDouble(t[i+1]);
			}
			return gr;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}	
	}



	public static void printTopoogy(ArrayList<DNode> graph) {
		for(DNode node:graph) {
			System.out.println(node.x+" "+node.y+" ");
		}

		for(DNode node:graph) {
			DEdge e=node.edge;
			while(e!=null) {
				System.out.println(e.from.id+" "+e.to.id);
				e=e.next;
			}
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


	public static void main(String[] args) {
		int [] nums= {10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};
		//int []nums= {50};
		String filename="aaaanetworks10to100nodes.txt";
		int numPer=20;
		store_samples(filename,nums,numPer);

	}




}
