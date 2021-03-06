package template;

import java.util.Comparator;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;


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

		return betterTie2(n1, n2);
	}
	
	////////////////////////////////////////////////////////
	//													  //
	//					   HEURISTICS   	   			  //
	//													  //
	////////////////////////////////////////////////////////
	
	public int naiveTie(Node n1, Node n2) { // 41148 ; 60278
		double f1 = f(n1);
		double f2 = f(n2);
		if(f1 > f2) return 1;
		if(f1 < f2) return -1;
		return 0;
	}
	
	public int betterTie(Node n1, Node n2) { // 42521 ; 61636
		double f1 = f(n1);
		double f2 = f(n2);
		if(f1 > f2) return 1;
		if(f1 < f2) return -1;
		if(g(n1) > g(n2)) return 1;
		if(g(n1) < g(n2)) return -1;
		return 0;
	}
	
	public int betterTie2(Node n1, Node n2) { // 40937 ; 59939
		double f1 = f(n1);
		double f2 = f(n2);
		if(f1 > f2) return 1;
		if(f1 < f2) return -1;
		if(g(n1) > g(n2)) return -1; // cela revient a regarder qui a le plus grand h, plutot que le plus grand g
		if(g(n1) < g(n2)) return 1;
		return 0;
	}
	
	// Always same results as betterTie
	public int bestTie(Node n1, Node n2) { // 40937 ; 59939
		double f1 = f(n1);
		double f2 = f(n2);
		if(f1 > f2) return 1;
		if(f1 < f2) return -1;
		if(g(n1) > g(n2)) return -1;
		if(g(n1) < g(n2)) return 1;
		if(n1.getState().getDelivered().size() < n2.getState().getDelivered().size()) return 1;
		if(n1.getState().getDelivered().size() > n2.getState().getDelivered().size()) return -1;
		if(n1.getState().getCarried().size() < n2.getState().getCarried().size()) return 1;
		if(n1.getState().getCarried().size() > n2.getState().getCarried().size()) return -1;
		return 0;
	}
	
	public double f(Node n) {
		return g(n) + h1(n);
	}
	
	public double g(Node n) {
		return n.getCost();
	}
	
	// Best heuristics : naiveH <<< h4 << h2 < h5 <<< h3 < h1
	
	public double naiveH(Node n) { // 193230 ; 215233
		return 0.0;
	}
	
	public double h1(Node n) { // 40937 ; 59939
		// maximal distance between the distances to travel for each carried tasks (= current city to delivery city) and
		// the distances to travel for each not already picked up tasks (= current city to pickup city + pickup city to delivery city)
		double max = 0.0;
		State s = n.getState();
		City current = s.getCity();
		TaskSet carried = s.getCarried();
		
		for (Task task : carried) {
			double dist = current.distanceTo(task.deliveryCity);
			max = Math.max(dist, max);
		}
		
		TaskSet delivered = s.getDelivered();
		TaskSet alreadyPickedUp = TaskSet.union(carried, delivered);
		TaskSet toBePickedUp = TaskSet.intersectComplement(s.getTasks(), alreadyPickedUp);
		
		for (Task task : toBePickedUp) {
			double dist = current.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
			max = Math.max(dist, max);
		}
		
		return max;
	}
	
	public double h2(Node n) { // 103443 ; 136544
		// maximal distance between the distances to travel for each carried tasks (= current city to delivery city)
		double max = 0.0;
		State s = n.getState();
		City current = s.getCity();
		TaskSet carried = s.getCarried();
		
		for (Task task : carried) {
			double dist = current.distanceTo(task.deliveryCity);
			max = Math.max(dist, max);
		}
		
		return max;
	}
	
	public double h3(Node n) { // 44390 ; 63647
		// maximal distance between the distances to travel for each not already picked up tasks (= current city to pickup city + pickup city to delivery city)
		double max = 0.0;
		State s = n.getState();
		City current = s.getCity();
		TaskSet carried = s.getCarried();
		TaskSet delivered = s.getDelivered();
		TaskSet alreadyPickedUp = TaskSet.union(carried, delivered);
		TaskSet toBePickedUp = TaskSet.intersectComplement(s.getTasks(), alreadyPickedUp);
		
		for (Task task : toBePickedUp) {
			double dist = current.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
			max = Math.max(dist, max);
		}
		
		return max;
	}
	
	public double h4(Node n) { // 120680 ; 150821
		// maximal distance between the distances to travel to each not already picked up tasks (= current city to pickup city)
		double max = 0.0;
		State s = n.getState();
		City current = s.getCity();
		TaskSet carried = s.getCarried();
		TaskSet delivered = s.getDelivered();
		TaskSet alreadyPickedUp = TaskSet.union(carried, delivered);
		TaskSet toBePickedUp = TaskSet.intersectComplement(s.getTasks(), alreadyPickedUp);
		
		for (Task task : toBePickedUp) {
			double dist = current.distanceTo(task.pickupCity);
			max = Math.max(dist, max);
		}
		
		return max;
	}
	
	public double h5(Node n) { // 91514 ; 118535
		// maximal distance between the distances to travel to deliver each not already picked up tasks (= pickup city to delivery city)
		double max = 0.0;
		State s = n.getState();
		TaskSet carried = s.getCarried();
		TaskSet delivered = s.getDelivered();
		TaskSet alreadyPickedUp = TaskSet.union(carried, delivered);
		TaskSet toBePickedUp = TaskSet.intersectComplement(s.getTasks(), alreadyPickedUp);
		
		for (Task task : toBePickedUp) {
			double dist = task.pickupCity.distanceTo(task.deliveryCity);
			max = Math.max(dist, max);
		}
		
		return max;
	}
	
	
	////////////////////////////////////////////////////////
	//													  //
	//					   	GET-SET       	   			  //
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
