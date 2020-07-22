import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import ilog.cplex.*;
import ilog.cplex.IloCplex.CplexStatus;

public class AlgMILP extends Solver{
	IloCplex solver = null;

	int numEdge = -1;// for storing the number of directed edges

	private void numberEdge() {
		numEdge = 0;// the # of labeled edges
		for (DNode d : graph) {
			//if (d.id == 0)
			//	continue;
			d.deg_G = 0;
			DEdge e = d.edge;
			while (e != null) {
				e.num = numEdge;//begins with 0, which is different from lp_solve
				numEdge++;
				d.deg_G++;
				e = e.next;
			}
		}
	}

	/* x_ij is x(e.num)
	 * y_ij is y(e.num)
	 * z_k
	
	 */
	double []O=null;
	int [][]B=null;
	
	void construct_O_B() {
		//generate the set of possible loads
		ArrayList<Double> ls=new ArrayList<Double>();
		graph.get(0).E=Double.MAX_VALUE;
		for(int i=0;i<graph.size();i++) {
			for(int j=1;j<=graph.size();j++) {
				double t=GenRWUtils.f(graph.get(i).E, j);
				if(Double.isFinite(t))
					ls.add(t);
			}			
		}
		O=new double[ls.size()];
		for(int i=0;i<O.length;i++)
			O[i]=ls.get(i);
		B=new int[graph.size()][O.length];
		for(int i=0;i<graph.size();i++)
			B[i]=new int[O.length];
		for(int j=0;j<O.length;j++) {
			int[] tp=GenRWUtils.milp_bounds(graph,O[j]);
			for(int i=0;i<graph.size();i++) {
				B[i][j]=tp[i];
			}
		}
	}

	@Override
	public void compute_optimal() {
		// TODO Auto-generated method stub
		phase=-1;
		optobj=-1;
		
		start_time=System.nanoTime();
		numberEdge();
		construct_O_B();

		try {
			solver=new IloCplex();

			//setup the set of variables.
			IloIntVar[] x=solver.boolVarArray(numEdge);//x_ij in the model is x[e.num]			
			IloIntVar[] y=solver.intVarArray(numEdge,0,graph.size());//y_ij is y[e.num]; changing integer to real causes out_of_memory error, do not know why
			IloIntVar[] z=solver.boolVarArray(O.length);//z_k variables
			solver.addMinimize(solver.scalProd(O, z));//objective function		

			//(2)sum_j x_ij=1 for all i\ne 0 
			//(3)sum_j y_0j=0
			//(4)sum_j y_ij- sum_j y_ji=1 for i\ne 0
			//(5)y_ij-(n-1)*xij<=0, x_ij-y_ij<=0
			//(7)sum_j x_ji+1<=sum_k z_kb_ik
			//(8) sum_k zk=1
			
			//(2)
			for(int i=1;i<graph.size();i++) {
				DNode node=graph.get(i);
				IloLinearNumExpr cons=solver.linearNumExpr();
				DEdge e=node.edge;
				while(e!=null) {
					cons.addTerm(1,x[e.num]);
					e=e.next;
				}
				solver.addEq(cons,1);
			}
			
			//(3)
			{
				IloLinearNumExpr cons=solver.linearNumExpr();
				DNode node=graph.get(0);
				DEdge e=node.edge;
				while(e!=null) {
					cons.addTerm(1,y[e.num]);
					e=e.next;
				}
				solver.addEq(cons,0);
			}
			
			//(4)
			for(int i=1;i<graph.size();i++) {
				DNode node=graph.get(i);
				IloLinearNumExpr cons=solver.linearNumExpr();
				DEdge e=node.edge;
				while(e!=null) {
					cons.addTerm(1,y[e.num]);
					cons.addTerm(-1,y[e.pair.num]);
					e=e.next;
				}
				solver.addEq(cons, 1);
			}
			
			//(5)
			for(int i=0;i<graph.size();i++) {
				DNode node=graph.get(i);
				DEdge e=node.edge;
				while(e!=null) {
					solver.addLe(x[e.num],y[e.num]);
					solver.addLe(y[e.num],solver.prod(graph.size()-1, x[e.num]));
					e=e.next;
				}
			}
			
			//(7)
			for(int i=0;i<graph.size();i++) {
				DNode node=graph.get(i);
				IloLinearNumExpr cons=solver.linearNumExpr();
				DEdge e=node.edge;
				while(e!=null) {
					cons.addTerm(1, x[e.pair.num]);
					e=e.next;
				}
				cons.setConstant(1);
				solver.addLe(cons,solver.scalProd(B[i],z));				
			}
			//(8)
			int []ones=new int[O.length];
			Arrays.fill(ones,1);
			solver.addEq(solver.scalProd(ones, z),1);
			
			solver.setParam(IloCplex.Param.Threads,1);//single threaded, for fairness
			solver.setOut(new FileOutputStream("cplexlog.txt"));

			int rest_time=(int)(time_limit - (long) ((System.nanoTime()-start_time) * 1.0 / 1000000000));
			solver.setParam(IloCplex.Param.TimeLimit,rest_time);//time limit in seconds
			
			solver.setParam(IloCplex.Param.Threads,1);//restrict the number of threads		
			solver.setParam(IloCplex.Param.MIP.Tolerances.MIPGap,0);//set the gap tolerance, otherwise numerical instability problem arises.
			solver.setParam(IloCplex.Param.MIP.Tolerances.AbsMIPGap,0);		
			solver.setParam(IloCplex.Param.Preprocessing.Presolve,false);//for mem issue
			solver.setParam(IloCplex.Param.Preprocessing.Aggregator, 0);		
			solver.setParam(IloCplex.Param.Emphasis.MIP, IloCplex.MIPEmphasis.Optimality);
			solver.setParam(IloCplex.Param.MIP.Limits.EachCutLimit,0);
			solver.setParam(IloCplex.Param.MIP.Cuts.Gomory, -1);
			solver.setParam(IloCplex.Param.MIP.Cuts.LiftProj, -1);
			solver.setParam(IloCplex.Param.MIP.Cuts.Cliques, -1);
			solver.setParam(IloCplex.Param.MIP.Strategy.Search,1);//traditional bb strategy
			solver.setParam(IloCplex.Param.MIP.Strategy.NodeSelect,0);//for memory
			solver.setParam(IloCplex.Param.Emphasis.Memory, true);//to deal with out_of_mem error
			
			solver.setParam(IloCplex.Param.WorkMem, 1024);//restrict central memory
			solver.setParam(IloCplex.Param.MIP.Limits.TreeMemory,512);//for out memory error
			solver.setParam(IloCplex.Param.MIP.Strategy.File, 3);//node file on disk and compressed; to deal with out_of_memory errors
			
			boolean feasible=solver.solve();
			CplexStatus status=solver.getCplexStatus();
			if(status==CplexStatus.Optimal) {
				optobj=solver.getObjValue();
				phase=3;
			}else if(status==CplexStatus.AbortTimeLim) {
				if(feasible==false) {
					//no feasible solution is found
					phase=1;
					optobj=-1;
					System.out.println(" no feasible solution");
				}else {
					//feasible solution is found
					phase=2;
					optobj=solver.getObjValue();
					System.out.println("found solution "+solver.getObjValue()+ " Relative Gap "+solver.getMIPRelativeGap()+" best bound "+solver.getBestObjValue());
				}
			}else {
				phase=-1;
				solver.exportModel("debugCplex.lp");
				System.out.println(status.toString());
			}
			solver.endModel();

		}
		catch(ilog.cplex.CpxException e2) {
			//out of memory exception
			phase=-3;			
		//	solver.exportModel("out_of_memory.lp");
			try {
				e2.printStackTrace();
				optobj=solver.getObjValue();
				solver.endModel();
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		catch (IloException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
