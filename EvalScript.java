import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;


public class EvalScript {


	public static class MyThread extends Thread{
		Solver solver;
		public MyThread(Solver s) {
			solver=s;
		}
		public void run() {			
			solver.compute_optimal();			
		}
	}

	public static void evaluate(Solver solver, ArrayList<DNode> cgraph, PrintStream ps,int cID) throws InterruptedException {
		long startTime, e4;
		solver.set_graph(cgraph);	
		MyThread th=new MyThread(solver);				
		startTime = System.nanoTime();
		th.start();
		th.join(solver.time_limit * 1000);// wait for a fixed time
		if (th.isAlive()) {// time out
			solver.timeout=true;				
			while(th.isAlive()) {
				//		System.out.println("wait 1 second");
				Thread.sleep(1000);	

			}
			// write to file a negative time duration

			// i++;
		}
		//write to file the actual time
		e4 = System.nanoTime() - startTime;
		String str=cID+" " + solver.get_phase()+" "+solver.get_optimal() + " " + (e4 * 1.0 / 1000000000)+" ";
		System.out.println(str);
		ps.println(str);
		ps.flush();

	}



	public static void evaluateOneMethod(String network_filename,String output_filename,Solver solver,long time_limit_in_seconds,int cID_from) throws FileNotFoundException, InterruptedException {
		FileOutputStream f;
		PrintStream ps;
		f = new FileOutputStream(output_filename, true);
		ps = new PrintStream(f);
		GenRWUtils grwn=new GenRWUtils();
		grwn.prepareRead(network_filename);
		ArrayList<DNode> cgraph=null;

		solver.set_time_limit(time_limit_in_seconds);		
		while ( (cgraph=grwn.nextNetwork())!= null) {		
			int cID=grwn.cID;
			if(cID<cID_from)continue;		


			evaluate(solver,cgraph,ps,cID);
		}
		ps.close();
	}


	public static void evaluateBasic(String network_filename,long time_limit_in_seconds) throws FileNotFoundException, InterruptedException {

		Solver solver=new AlgBasic();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Linear;
		String outfilename="out/Eval20MinBasicLinear.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);	

		solver=new AlgBasic();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Cubic;
		outfilename="out/Eval20MinBasicCubic.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);

		solver=new AlgBasic();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
		outfilename="out/Eval20MinBasicLogarithm.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);


	}

	public static void evaluateMerge(String network_filename,long time_limit_in_seconds) throws FileNotFoundException, InterruptedException {

		Solver solver=new AlgMerge();
		String outfilename;
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Linear;
		outfilename="out/RemainingNodesLinear.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);

		solver=new AlgMerge();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Cubic;
		outfilename="out/Eval20MinExactMergeCubic.txt";
		outfilename="out/RemainingNodesCubic.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);

		solver=new AlgMerge();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
		outfilename="out/Eval20MinExactMergeLogarithm.txt";
		outfilename="out/RemainingNodesLogarithm.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);


	}



	public static void evaluateForest(String network_filename,long time_limit_in_seconds) throws FileNotFoundException, InterruptedException {

		Solver solver; String outfilename="tmp.txt";

		solver=new AlgForest();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Linear;
		outfilename="out/Eval20MinForestLinear.txt"; 
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);

		solver=new AlgForest();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Cubic;
		outfilename="out/Eval20MinForestCubic.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);	

		solver=new AlgForest();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
		outfilename="out/Eval20MinForestLogarithm.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);	

	}

	public static void debug(String network_filename,long time_limit_in_seconds) throws FileNotFoundException, InterruptedException {
		Solver solver; String outfilename;
		solver=new AlgForest();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
		outfilename="out/debugForestLog.txt";
		int target_cID=322;
		FileOutputStream f;
		PrintStream ps;
		f = new FileOutputStream(outfilename, true);
		ps = new PrintStream(f);
		GenRWUtils grwn=new GenRWUtils();
		grwn.prepareRead(network_filename);
		ArrayList<DNode> cgraph=null;

		solver.set_time_limit(time_limit_in_seconds);		
		while ( (cgraph=grwn.nextNetwork())!= null) {		
			int cID=grwn.cID;
			if(cID!=target_cID)continue;
			evaluate(solver,cgraph,ps,cID);
		}
		ps.close();

	}

	public static void evaluateMILP(String network_filename,long time_limit_in_seconds) throws FileNotFoundException, InterruptedException {

		Solver solver; String outfilename;

		solver=new AlgMILP();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Linear;
		outfilename="out/Eval20MinMILPLinear.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0 );

		solver=new AlgMILP();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Cubic;
		outfilename="out/Eval20MinMILPCubic.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);	

		solver=new AlgMILP();
		GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
		outfilename="out/Eval20MinMILPLogarithm.txt";
		evaluateOneMethod(network_filename,outfilename,solver,time_limit_in_seconds,0);	

	}


	public static void comparingTwoSteps() throws FileNotFoundException, InterruptedException {
		AlgBasic solver=new AlgBasic();

		GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
		String network_filename="networks10to100nodes.txt";
		String output_filename="comparingTwoStepsExactLogarithm.txt";
		int cID_from=20,cID_end=39;

		FileOutputStream f;
		PrintStream ps;
		f = new FileOutputStream(output_filename, true);
		ps = new PrintStream(f);
		GenRWUtils grwn=new GenRWUtils();
		grwn.prepareRead(network_filename);
		ArrayList<DNode> cgraph=null;

		solver.set_time_limit(60);		
		while ( (cgraph=grwn.nextNetwork())!= null) {		
			int cID=grwn.cID;
			if(cID<cID_from||cID>cID_end)continue;			
			evaluatePrintTimeTwoSteps(solver,cgraph,ps,cID);
		}
		ps.close();		

	}

	public static void evaluatePrintTimeTwoSteps(AlgBasic solver, ArrayList<DNode> cgraph, PrintStream ps,int cID) throws InterruptedException {

		solver.set_graph(cgraph);	
		MyThread th=new MyThread(solver);				

		th.start();
		th.join(solver.time_limit * 1000);// wait for a fixed time
		if (th.isAlive()) {// time out
			solver.timeout=true;				
			while(th.isAlive()) {
				System.out.println("wait 1 second");
				Thread.sleep(1000);	

			}
			// write to file a negative time duration
			System.out.println(cID+" " +solver.get_phase()+" "+ -1 + " " + (-solver.time_limit));
			ps.println(cID +" "+ solver.get_phase()+" "+-1 + " "+ (-solver.time_limit)+ "\r\n");
			ps.flush();
			// i++;
		}else {
			// finished within time ; write to file the actual time
			String s=cID +" "+ solver.first_step_seconds+"  "+solver.second_step_seconds+" "+solver.numcallsFirstStep+" "+solver.numcallsSecondStep;
			ps.println(s);
			System.out.println(s);
			ps.flush();
		}
	}

	public static void computeNumOfWhites() throws FileNotFoundException, InterruptedException {
		AlgBasic solver=new AlgBasic();

		GenRWUtils.functype=GenRWUtils.LoadFuncType.Linear;
		String network_filename="networks10to100nodes.txt";
		String output_filename="numberofwhites.txt";
		int cID_from=164,cID_end=164;

		FileOutputStream f;
		PrintStream ps;
		f = new FileOutputStream(output_filename, true);
		ps = new PrintStream(f);
		GenRWUtils grwn=new GenRWUtils();
		grwn.prepareRead(network_filename);
		ArrayList<DNode> cgraph=null;

		solver.set_time_limit(60);		
		while ( (cgraph=grwn.nextNetwork())!= null) {		
			int cID=grwn.cID;
			if(cID<cID_from||cID>cID_end)continue;	
			GenRWUtils.functype=GenRWUtils.LoadFuncType.Linear;
			solver.set_graph(cgraph);  solver.print_num_of_whites(ps,1);
			GenRWUtils.functype=GenRWUtils.LoadFuncType.Cubic;
			solver.set_graph(cgraph);  solver.print_num_of_whites(ps,2);
			GenRWUtils.functype=GenRWUtils.LoadFuncType.Logarithm;
			solver.set_graph(cgraph);  solver.print_num_of_whites(ps,3);

		}
		ps.close();		
	}

	public static void compute_cplex_gap() throws FileNotFoundException, InterruptedException {
		String filename="networks10to100nodes.txt";		
		long time_limit_in_seconds=20*60;//20 minutes

		int [] failed_cids= {240,300,337,291,325,327};
		GenRWUtils.LoadFuncType [] funcs= {GenRWUtils.LoadFuncType.Linear,GenRWUtils.LoadFuncType.Linear,GenRWUtils.LoadFuncType.Linear,GenRWUtils.LoadFuncType.Logarithm,GenRWUtils.LoadFuncType.Logarithm,GenRWUtils.LoadFuncType.Logarithm};
		for(int i=0;i<failed_cids.length;i++) {
			int cID=failed_cids[i];
			GenRWUtils.functype=funcs[i];
			ArrayList<DNode> cgraph=GenRWUtils.retrieve_network(cID, filename);
			String outfilename="out/CplexGapTmp.txt";
			Solver solver=new AlgMILP();
			solver.set_time_limit(time_limit_in_seconds);
			EvalScript.evaluate(solver, cgraph, new PrintStream(new FileOutputStream(outfilename, true)), cID);			
		}

	}


	public static void main(String[] args)
			throws Exception {		
		String filename="networks10to100nodes.txt";		
		long time_limit_in_seconds=20*60;//20 minutes

		evaluateForest(filename,time_limit_in_seconds);

		//	comparingTwoSteps();
		//	computeNumOfWhites();		

	}
}
