package template;

import java.util.Comparator;


public class Node implements Comparator<Node>, Comparable<Node>{
	private State state;
	private Node parent;
	private double cost;
	
	public Node(State state, Node parent, double cost){
		this.state = state;
		this.parent = parent;
		this.cost = cost;
	}

	
	////////////////////////////////////////////////////////
	//													  //
	//						   UTILS   	  	 			  //
	//													  //
	////////////////////////////////////////////////////////
	
	public boolean equals(Node n){
		return (this.state.equals(n.state));
	}

	@Override
	public int compareTo(Node o) {
		return compare(this,o);
	}
	
	@Override
	public int compare(Node n1, Node n2) {

		return naiveTie(n1, n2);
	}
	
	////////////////////////////////////////////////////////
	//													  //
	//					   HEURISTICS   	   			  //
	//													  //
	////////////////////////////////////////////////////////
	
	public int naiveTie(Node n1, Node n2) {
		double f1 = f(n1);
		double f2 = f(n2);
		if(f1 > f2) return 1;
		if(f1 < f2) return -1;
		return 0;
	}
	
	public int bestGTie(Node n1, Node n2) {
		double f1 = f(n1);
		double f2 = f(n2);
		if(f1 > f2) return 1;
		if(f1 < f2) return -1;
		if(g(n1) > g(n2)) return 1;
		if(g(n1) < g(n2)) return -1;
		// compare on delivered task size puis carried task size ? pas besoin si on en prend compte dans h
		return 0;
	}
	
	public double f(Node n) {
		return g(n) + naiveH(n);
	}
	
	public double g(Node n) {
		return n.getCost();
	}
	
	public double naiveH(Node n) {
		return 0.0;
	}
	
	public double h1(Node n) {
		return 0.0;
	}
	
	
	////////////////////////////////////////////////////////
	//													  //
	//					   	GETSET       	   			  //
	//													  //
	////////////////////////////////////////////////////////
	
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}	
}
