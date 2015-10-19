package template;

import java.util.Comparator;


public class Node implements Comparator<Node>{
	private State state;
	private Node parent;
	private double cost;
	
	public Node(State state, Node parent, double cost){
		this.state = state;
		this.parent = parent;
		this.cost = cost;
	}

	@Override
	public int compare(Node n1, Node n2) {
		if(n1.cost + H(n1.state) > n2.cost + H(n2.state)) return 1;
		else if(n1.cost + H(n1.state) == n2.cost + H(n2.state)){// We trust more g than h
			if(n1.cost>n2.cost) return 1;
		}
		return -1;
	}
	
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

	public double H(State s){
		return 1;
	}
	public boolean equals(Node n){
		return (this.state.equals(n.state));
	}


	
	
}
