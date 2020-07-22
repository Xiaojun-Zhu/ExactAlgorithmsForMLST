import java.util.ArrayList;

public abstract class Solver {
	
	boolean timeout=false;

	long time_limit;//in seconds, time limit on the running time
	long start_time;//System.nanotime();
	ArrayList<DNode> graph;
	double optobj=0;
	
	int phase=-1;//indicate which phase the solver is in: 1 means the phase of finding optimal objective value, 2 means the phase of finding an optimal solution.
	//used when the algorithm is terminated because of timeout.
	
		
	int numcallsFirstStep,numcallsSecondStep;//only used in inclusion-exclusion based algorithms
	
	void set_graph(ArrayList<DNode> nodes) {
		graph=GenRWUtils.make_a_copy(nodes);
		
	}
	abstract public void compute_optimal();
	public double get_optimal() {
		return optobj;
	}

	public double get_phase() {
		//only useful if timeout event happens and the optimal solution is not found
		return phase;
	}
	public void set_time_limit(long seconds) {
		time_limit=seconds;
	}
}
