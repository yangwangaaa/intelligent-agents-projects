package template;

/* import table */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.PriorityQueue;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import org.springframework.util.StopWatch;

/**
 * An optimal planner for one vehicle.
 */

@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, NAIVE }

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	int count = 0;
	int numNodesVisited = 0;
	int numNodesCreated = 1;


	////////////////////////////////////////////////////////
	//													  //
	//						MAINS						  //
	//													  //
	////////////////////////////////////////////////////////

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

	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		print(tasks); 
		Plan plan;
		StopWatch stopWatch = new StopWatch("My Stop Watch");

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			print("++++++++ ASTAR ++++++++");
			stopWatch.start("ASTAR");
			plan = AStar(vehicle, tasks);
			break;
		case BFS:
			print("++++++++ BFS ++++++++");
			stopWatch.start("BFS");
			plan = BFS(vehicle, tasks);
			break;
		case NAIVE:
			print("++++++++ NAIVE ++++++++");
			stopWatch.start("NAIVE");
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		stopWatch.stop();

		print(stopWatch.prettyPrint());

		print(plan.toString());
		print("numNodesVisited = " + numNodesVisited);
		print("numNodesCreated = " + numNodesCreated);
		
		
		
//		numNodesVisited = 0;
//		numNodesCreated = 1;
//		
//		print("++++++++++++++++++++++++BFS+++++++++++++++++++++++++++");
//		stopWatch.start("BFS");
//		plan = BFS(vehicle, tasks);
//		stopWatch.stop();
//		print(stopWatch.prettyPrint());
//		print("numNodesVisited = " + numNodesVisited);
//		print("numNodesCreated = " + numNodesCreated);
//		print("Total distance = " + plan.totalDistance());
		
//		stopWatch = new StopWatch("My Stop Watch");
//		numNodesVisited = 0;
//		numNodesCreated = 1;
//		print("");
//		print("");
//		print("++++++++++++++++++++++++ASTAR+++++++++++++++++++++++++++");
//		print("HEURSITIC : h1");
//		stopWatch.start("ASTAR");
//		plan = AStar(vehicle, tasks);
//		stopWatch.stop();
//		print(stopWatch.prettyPrint());
//		print("numNodesVisited = " + numNodesVisited);
//		print("numNodesCreated = " + numNodesCreated);
//		print("Total distance = " + plan.totalDistance());
//		
//		numNodesVisited = 0;
//		numNodesCreated = 1;
//		print("");
//		print("");
//		print("++++++++++++++++++++++++NAIVE+++++++++++++++++++++++++++");
//		plan = naivePlan(vehicle, tasks);
//		print("Total distance = " + plan.totalDistance());
		
		
		
		
		
		return plan;
	}


	@Override
	public void planCancelled(TaskSet carried) {

		if (!carried.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carried when the next
			// plan is computed.
		}
	}

	////////////////////////////////////////////////////////
	//													  //
	//					    STRATEGIES  	   			  //
	//													  //
	////////////////////////////////////////////////////////

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


	private Plan BFS(Vehicle vehicle, TaskSet tasks){

		//Create first node of the search.
		City current = vehicle.getCurrentCity();

		TaskSet delivered = TaskSet.noneOf(tasks);
		State s = new State(current, vehicle.getCurrentTasks(), delivered, tasks);
		Node first = new Node(s, null, 0);

		Queue<Node> Q = new LinkedList<Node>(); 
		HashSet<State> C = new  HashSet<State>();
		//Search a plan.
		Node n = BFS_search(first, Q, C, tasks, vehicle);

		// Compute the plan.
		Plan plan = new Plan(current);
		return computeFinalPlan(plan , n);
	}
	
	private Node BFS_search(Node first, Queue<Node> Q,  HashSet<State> C ,TaskSet tasks, Vehicle vehicle){
		Q.add(first);
		while(!Q.isEmpty()){ //While the queue is not empty

			numNodesVisited++;
			Node current = Q.remove();
			if(isFinal(current.getState(), tasks, vehicle)){ //If node is final, return it.
				return current;
			}
			if(!C.contains(current.getState())){ //If there is no cycle.
				C.add(current.getState());
				ArrayList<Node> S = getSuccessors(current, tasks, vehicle);
				Q.addAll(S);
			}
		}
		return null;
	}
	
	private Plan AStar(Vehicle vehicle, TaskSet tasks) {

		//Create first node of the search.
		Plan plan = new Plan(vehicle.getCurrentCity());
		State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), TaskSet.noneOf(tasks), tasks);
		Node root = new Node(initialState, null, 0);

		HashMap<State, Double> C = new HashMap<State, Double>();		
		PriorityQueue<Node> Q = new PriorityQueue<Node>();
		Q.add(root);

		while(!Q.isEmpty()){ //While the queue is not empty
			
			numNodesVisited++;
			Node currentNode = Q.poll();
			State currentState = currentNode.getState();

			//If node is final, return it.
			if(isFinal(currentState, tasks, vehicle)) return computeFinalPlan(plan, currentNode);

			//If there is no cycle or the cost to arrive in equivalent state is smaller.
			if( !C.containsKey(currentState) || currentNode.getCost()<C.get(currentState)){
				C.put(currentState, currentNode.getCost());
				ArrayList<Node> S = getSuccessors(currentNode, tasks, vehicle);
				Q.addAll(S);
			}			
		}

		return null;
	}

	////////////////////////////////////////////////////////
	//													  //
	//					   SUBMETHODS   	   			  //
	//													  //
	////////////////////////////////////////////////////////
	
	private ArrayList<Node> getSuccessors(Node currentNode, TaskSet tasks, Vehicle vehicle) {
		ArrayList<Node> successors = new ArrayList<Node>();
		State currentState = currentNode.getState();
		TaskSet deliveredOld = currentState.getDelivered();
		TaskSet carriedOld = currentState.getCarried();
		TaskSet availableTasks = TaskSet.noneOf(tasks);
		
		for (Task task : tasks) {
			if(task.pickupCity == currentState.getCity() && !deliveredOld.contains(task) && !carriedOld.contains(task)) {
				availableTasks.add(task);
			}
		}

		Set<Set<Task>> powerTaskSet = powerSet(availableTasks);	

		for (Set<Task> tasksSubset : powerTaskSet) { //For all PickedUp
			TaskSet pickedUp = TaskSet.noneOf(tasks);
			for (Task task : tasksSubset) {
				pickedUp.add(task);
			}

			TaskSet deliveredNew = TaskSet.copyOf(deliveredOld);  //deliveredNew = deliveredOld
			TaskSet carriedNew = TaskSet.union(carriedOld, pickedUp); //carriedNew = carriedOld U pickedUp

			for (Task task : carriedOld) { //For all task in carried_old
				if (task.deliveryCity.equals(currentState.getCity())) {//If task.deliveryCity = city_old
					//The task has been delivered
					deliveredNew.add(task);
					carriedNew.remove(task);
				}
			}
			
			if (carriedNew.weightSum() <= vehicle.capacity()) {//Abort if carried > capacity //TODO remonter la condi
				for (City neighbor : currentState.getCity().neighbors()) {//For all neighbors
					double cost = currentNode.getCost() + currentState.getCity().distanceTo(neighbor);
					State state = new State(neighbor, carriedNew, deliveredNew, tasks); //Create new state
					Node node = new Node(state, currentNode, cost);
					
					numNodesCreated++;
					successors.add(node);
				}
			}
		}
		return successors;
	}


	public Plan computeFinalPlan(Plan plan, Node n){
		if(n == null){
			print("no plan");
			return null;
		}
				
		//reorder nodes
		Stack<Node> stack = new Stack<Node>();
		stack.push(n);
		while(n.getParent() != null){
			n = n.getParent();
			stack.push(n);
		}

		Node n1 = stack.pop();
		Node n2 = n1;
		while(!stack.isEmpty()){
			n2 = stack.pop();
			//deliver in city_old, pick up in city_old, move to city_new
			addActions(plan, n1, n2);
			n1 = n2;
		}
		//deliver the tasks on the last city.
		TaskSet lastDelivery = n1.getState().getCarried();
		for(Task task : lastDelivery)
			plan.appendDelivery(task);
		return plan;
	}

	public void addActions(Plan plan, Node n1, Node n2){
		
		//Deliver tasks
		TaskSet delivery = TaskSet.intersectComplement(n2.getState().getDelivered(), n1.getState().getDelivered());
		for(Task task : delivery)
			plan.appendDelivery(task);

		//Pickup tasks
		TaskSet pickup = TaskSet.intersectComplement(n2.getState().getCarried(), n1.getState().getCarried());
		for(Task task : pickup)
			plan.appendPickup(task);

		//Move city
		plan.appendMove(n2.getState().getCity());
	}

	public boolean isFinal(State s, TaskSet tasks, Vehicle v){
		for(Task task : s.getCarried()){ // For all task in carried
			if(!task.deliveryCity.equals(s.getCity())) // If task.deliveryCity != city
				return false;
		}
		return(TaskSet.union(s.getDelivered(),s.getCarried()).containsAll( TaskSet.union(v.getCurrentTasks(), tasks)));
	}


	////////////////////////////////////////////////////////
	//													  //
	//						UTILS						  //
	//													  //
	////////////////////////////////////////////////////////


	/**
	 * http://stackoverflow.com/questions/1670862/obtaining-a-powerset-of-a-set-in-java
	 */
	public static Set<Set<Task>> powerSet(Set<Task> originalSet) {
		Set<Set<Task>> sets = new HashSet<Set<Task>>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<Task>());
			return sets;
		}
		List<Task> list = new ArrayList<Task>(originalSet);
		Task head = list.get(0);
		Set<Task> rest = new HashSet<Task>(list.subList(1, list.size())); 
		for (Set<Task> set : powerSet(rest)) {
			Set<Task> newSet = new HashSet<Task>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}		
		return sets;
	}

	public void print(Node n){

		State s = n.getState();
		System.out.print("Node " + count + " : cost=" + n.getCost());
		System.out.println(", State : city=" + s.getCity().name + ", carried=" + s.getCarried().toString() + ", delivered=" + s.getDelivered().toString());

		count++; 
	}

	public void print(String s){
		System.out.println(s);
	}
	public void print(int i){
		System.out.println(i);
	}
	public void print(TaskSet tasks){
		for( Task t : tasks){
			System.out.println(t);
		}
	}
}	

