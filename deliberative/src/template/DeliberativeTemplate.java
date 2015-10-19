package template;

/* import table */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}
	public void Print(String s){
		System.out.println(s);
	}
	public void PrintI(int i){
		System.out.println(i);
	}
	public void PrintTaskSet(TaskSet tasks){
		for( Task t : tasks){
			System.out.println(t);
		}
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		PrintTaskSet(tasks);
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}


	private Plan AStar(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), tasks.noneOf(tasks));
		Node root = new Node(initialState, ...);

		HashMap<State, Integer> C = new HashMap<State, Integer>();
		ArrayList<Node> Q = new ArrayList<Node>();
		Q.add(root);

		while(Q.isEmpty()>0) {
			Node currentNode = Q.get(0);
			State currentState = currentNode.getState();

			if(isFinal(currentState)) return computeFinalPlan(currentNode);

			if( !C.containsKey(currentState) || currentNode.getCost()<C.get(currentState)) {
				C.put(currentState, currentNode.getCost());
				
				ArrayList<Node> S = getSuccessors(currentState);
				
				Q.add(S);
				
				Collections.sort(Q);
			}	
		}

		return null;
	}

	private ArrayList<Node> getSuccessors(Node node) {
		ArrayList<Node> successors = new ArrayList<Node>();
		Set<TaskSet>
		return successors;
	}
	
	
	/**
	 * http://stackoverflow.com/questions/1670862/obtaining-a-powerset-of-a-set-in-java
	 */
	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
	    Set<Set<T>> sets = new HashSet<Set<T>>();
	    if (originalSet.isEmpty()) {
	    	sets.add(new HashSet<T>());
	    	return sets;
	    }
	    List<T> list = new ArrayList<T>(originalSet);
	    T head = list.get(0);
	    Set<T> rest = new HashSet<T>(list.subList(1, list.size())); 
	    for (Set<T> set : powerSet(rest)) {
	    	Set<T> newSet = new HashSet<T>();
	    	newSet.add(head);
	    	newSet.addAll(set);
	    	sets.add(newSet);
	    	sets.add(set);
	    }		
	    return sets;
	}

	//TODO static?
	public static boolean isFinalState(State s, Vehicle v, TaskSet tasks){
		return(s.deliveredTasks.containsAll( TaskSet.union(v.getCurrentTasks(), tasks)));
	}
}
