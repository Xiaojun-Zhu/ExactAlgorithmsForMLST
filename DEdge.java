
public class DEdge {
DNode from,to;
DEdge previous,next;

int num;//for use in ILP solver 

DEdge pair;//the directed edge in the reverse direction

boolean inSPT=false;//whether in the spanning tree
}
