package template;

import java.util.Comparator;

public class Node implements Comparator<Node>{
	public State state;
	public Node parent;
	public double cost;
	
	public Node(State state, Node parent, int cost){
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
	
	public double H(State s){
		return 1;
	}
//	public boolean equals(Node n){
//		return (this.state.equals(n.state));
//	}
}
