# ExactAlgorithmsForMLST
exact algorithms for minimum load spanning tree problem

Data and code for the paper: Xiaojun Zhu, Shaojie Tang. Exact Algorithms for the Minimum Load Spanning Tree Problem. to appear in INFORMS Journal on Computing. 

CPLEX is required to run MILP.

## 1. Problem Instances

File 'networks10to100.txt' contains all problems used in the paper. In the file, networks are encoded as follows:

Each network consists of four lines. 
-  The first line is of the form: ProblemID, NumNode, NumOfDirectedEdges, TransmissionEnergy, ReceivingEnergy. For example, `0 10 32 6.66E-4 3.33E-4` means that Problem 0 has 10 nodes and 32 directed edges, transmitting a message consumes energy 6.66\*10^-4J and receiving a message consumes energy 3.33*10^-4.
- The second line has NumNode real values, indicating the initial energy of all nodes. Note that node 0 (the sink), though has a value of 0, has infinite energy. 
- The third line lists NumOfDirectedEdges/2 pairs of integers, describing undirected edges. For each undirected edge, we list it only once, from smaller node ID to larger node ID. For example, `0 27 0 26 0 22 0 17 0 13 0 2 1 9 1 8 2 29` means 9 undirected edges, (0,27), (0,26), (0,22), (0,17), (0,13), (0,2), (1,9), (1,8) and (2,29). We will insert 18 directed edges in the network. 
- The fourth line lists NumNode pairs of real values, indicating the positions of all nodes. This data is only used for illustration, and is not used in our algorithm. 

The code that generates these networks is `GenRWUtils.java`

## 2. Computational Results
The computational results are in file `computational_results.xlsx`. 

There are four methods (Basic, Merge, Forest, MILP) and three load functions (Linear, Cubic, Logarithmic), leading to 12 combinations. For each combination, there are four columns, (ProblemID,  Status, ObjectiveValue, CPUTimeInSeconds). Status 3 means the problem is solved to optimality, and other status values mean that the problem is not solved. In general, Status<3 is equivalent to CPUTimeInSeconds>=1200. 


## 3. Implemented Algorithms

Algorithms are implemented in java classes with prefix 'Alg'. 

| File | Implemented Algorithm|
|---|---|
|AlgBasic.java	| Basic |
|AlgMerge.java| Merge|
|AlgForest.java| Forest|
|AlgMILP.java| MILP|
|AlgApx.java| approximation algorithm proposed by (Zhu et al. 2016), used as a subroutine of Forest|
|DEdge.java| directed edge structure|
|Solver.java| abstract class describing common interface of solvers|

EvalScript.java contains script code evaluating all algorithms.

## Notes
Feel free to send me an email for any problems.  gxjzhu@gmail.com
