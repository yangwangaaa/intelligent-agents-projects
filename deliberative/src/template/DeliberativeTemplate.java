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

/**
 * TODO
 * @author Vladar
 * costPerKm V
 * priority queue V
 * tester plusieurs agents V
 * what if recompute path : possible que carried tasks pour city actuelle? V
 * virer capacity V
 * powerset in reverse order! X
 * 
 * Astar plus lent que BFS car : 
 * 1) collections.sort 
 * 2) mauvaise data structure (queue + rapide) 
 * 3) reexpand si cost plus petit 
 * 4) BFS ne trouve pas solu optimal
 * 
 * don't add successor to the list Q if already in it?
 * quoi si meme noeud mais meilleur et l'autre moins bon encore dans la liste
 * 
 * virer debut successors
 * optimiser successors + power set
 * 
 * repenser states + verifier data structures
 */

/**
 * PERFORMANCES
 * @author Vladar
 * check delivered apres : 18-19sec sur 10tasks
 * check delivered directement : 14sec sur 10 tasks
 * 
 * old add successor :
 * BFS : 15.9
 * STAR : 24.0
 * 
 * new old successor :
 * BFS
 * ASTAR
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

		// ...
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		print(tasks); //TODO assure toi que les task deja remplies et celle enlevé ne sont plus dedans
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

		//TODO d'autres data structures		
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
	
	private Plan AStar(Vehicle vehicle, TaskSet tasks) {//TODO ça serait pas mal que tu split en deux fonctions comme moi et que tu fasses du return failure

		//Create first node of the search.
		Plan plan = new Plan(vehicle.getCurrentCity());
		State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), TaskSet.noneOf(tasks), tasks);
		Node root = new Node(initialState, null, 0);

		HashMap<State, Double> C = new HashMap<State, Double>();		
		PriorityQueue<Node> Q = new PriorityQueue<Node>();
		Q.add(root);

		while(!Q.isEmpty()){ //While the queue is not empty
			
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
	
	
	//			if( !C.containsKey(currentState) || currentNode.getCost()<C.get(currentState)) {
	//				C.put(currentState, currentNode.getCost());
	//
	//				ArrayList<Node> S = getSuccessors(currentNode, tasks, vehicle);
	//
	//				//				if (count<10) {
	//				//					print("ASTAR SUCC " + S.size());
	//				//					for(Node n : S) {
	//				//						print(n);
	//				//					}
	//				//					print("ASTAR SUCC END");
	//				//				}
	//
	//				Q.addAll(S);
	//
	//				//Collections.sort(Q);
	//			}

	////////////////////////////////////////////////////////
	//													  //
	//					   SUBMETHODS   	   			  //
	//													  //
	////////////////////////////////////////////////////////


//	private ArrayList<Node> getSuccessors(Node currentNode, TaskSet tasks, Vehicle vehicle) {
//		ArrayList<Node> successors = new ArrayList<Node>();
//		State currentState = currentNode.getState();
//
//		TaskSet availableTasks = TaskSet.noneOf(tasks);
//		TaskSet tasksToBeDelivered = TaskSet.noneOf(tasks);
//		for(Task task : tasks) {
//			if(task.pickupCity == currentState.getCity() && !currentState.getDeliveredTasks().contains(task) && !currentState.getCarriedTasks().contains(task)) availableTasks.add(task);
//			if(task.deliveryCity == currentState.getCity()) tasksToBeDelivered.add(task);
//		}
//
//
//		Set<Set<Task>> powerTaskSet = powerSet(availableTasks);		
//
//		for (City neighbor : currentState.getCity().neighbors()) {
//
//
//			TaskSet deliveredTasks = TaskSet.intersect(currentState.getCarriedTasks(), tasksToBeDelivered);
//			TaskSet carriedTasks = TaskSet.intersectComplement( currentState.getCarriedTasks(), deliveredTasks);
//			TaskSet allDeliveredTasks = TaskSet.union(currentState.getDeliveredTasks(), deliveredTasks);
//
//			double cost = currentNode.getCost() + currentState.getCity().distanceTo(neighbor)*vehicle.costPerKm();
//
//			for(Set<Task> tasksSubset : powerTaskSet) {	
//				TaskSet availableTasksSubset = TaskSet.noneOf(tasks);
//				TaskSet alreadyDeliveredTasksSubset = TaskSet.noneOf(tasks);
//
//				for (Task task : tasksSubset) {
//					availableTasksSubset.add(task);
//				}
//
//				TaskSet allCarriedTasks = TaskSet.union(carriedTasks, availableTasksSubset);
//				if(allCarriedTasks.weightSum() <= vehicle.capacity()) {
//					TaskSet allDeliveredTasks2 = TaskSet.copyOf(allDeliveredTasks);
//					TaskSet allCarriedTasks2 = TaskSet.copyOf(allCarriedTasks); // obligé? (on parcoure en meme temps qu'on remove)
//					for (Task task : allCarriedTasks) {
//						if(task.deliveryCity==neighbor) {
//							allCarriedTasks2.remove(task);
//							allDeliveredTasks2.add(task);
//						}
//					}
//
//					State state = new State(neighbor, allCarriedTasks2, allDeliveredTasks2, tasks);
//
//					Node node = new Node(state, currentNode, cost);
//					successors.add(node);
//				}
//			}
//		}
//
//		return successors;
//	}


//	private ArrayList<Node> getSuccessors(Node currentNode, TaskSet tasks, Vehicle vehicle) {
//		ArrayList<Node> successors = new ArrayList<Node>();
//		State currentState = currentNode.getState();
//
//		TaskSet availableTasks = TaskSet.noneOf(tasks);
//		TaskSet tasksToBeDelivered = TaskSet.noneOf(tasks);
//		for(Task task : tasks) {
//			if(task.pickupCity == currentState.getCity() && !currentState.getDeliveredTasks().contains(task) && !currentState.getCarriedTasks().contains(task)) availableTasks.add(task);
//			if(task.deliveryCity == currentState.getCity()) tasksToBeDelivered.add(task);
//		}
//		Set<Set<Task>> powerTaskSet = powerSet(availableTasks);
//
//		for (City neighbor : currentState.getCity().neighbors()) {
//
//
//			TaskSet deliveredTasks = TaskSet.intersect(currentState.getCarriedTasks(), tasksToBeDelivered);
//			TaskSet carriedTasks = TaskSet.intersectComplement( currentState.getCarriedTasks(), deliveredTasks);
//			TaskSet allDeliveredTasks = TaskSet.union(currentState.getDeliveredTasks(), deliveredTasks);
//
//			double cost = currentNode.getCost() + currentState.getCity().distanceTo(neighbor)*vehicle.costPerKm();
//
//			for(Set<Task> tasksSubset : powerTaskSet) { //For all possible PickUp.
//				TaskSet availableTasksSubset = TaskSet.noneOf(tasks);
//				TaskSet alreadyDeliveredTasksSubset = TaskSet.noneOf(tasks);
//
//				for (Task task : tasksSubset) {
//					availableTasksSubset.add(task);
//				}
//
//				TaskSet allCarriedTasks = TaskSet.union(carriedTasks, availableTasksSubset);
//				if(allCarriedTasks.weightSum() <= vehicle.capacity()){
//					TaskSet allDeliveredTasks2 = TaskSet.copyOf(allDeliveredTasks);
//					TaskSet allCarriedTasks2 = TaskSet.copyOf(allCarriedTasks); // obligé? (on parcoure en meme temps qu'on remove)
//					for (Task task : allCarriedTasks) {
//						if(task.deliveryCity==neighbor) {
//							allCarriedTasks2.remove(task);
//							allDeliveredTasks2.add(task);
//						}
//					}
//
//					State state = new State(neighbor, allCarriedTasks2, allDeliveredTasks2, tasks, allCarriedTasks2.weightSum());
//
//					Node node = new Node(state, currentNode, cost);
//					successors.add(node);
//				}
//			}
//		}
//
//		return successors;
//	}
	
	private ArrayList<Node> getSuccessors(Node currentNode, TaskSet tasks, Vehicle vehicle) {
		ArrayList<Node> successors = new ArrayList<Node>();
		State currentState = currentNode.getState();
		TaskSet deliveredOld = currentState.getDeliveredTasks();
		TaskSet carriedOld = currentState.getCarriedTasks();
		TaskSet availableTasks = TaskSet.noneOf(tasks);
		
		for (Task task : tasks) {
			if(task.pickupCity == currentState.getCity() && !deliveredOld.contains(task) && !carriedOld.contains(task)) {
				availableTasks.add(task);
			}
		}

		// TODO?
		Set<Set<Task>> powerTaskSet = powerSet(availableTasks);	

		for (Set<Task> tasksSubset : powerTaskSet) {

			TaskSet pickedUp = TaskSet.noneOf(tasks);
			for (Task task : tasksSubset) {
				pickedUp.add(task);
			}

			TaskSet deliveredNew = TaskSet.copyOf(deliveredOld);
			TaskSet carriedNew = TaskSet.union(carriedOld, pickedUp);

			for (Task task : carriedOld) {
				if (task.deliveryCity.equals(currentState.getCity())) {
					deliveredNew.add(task);
				}
				else {
					carriedNew.remove(task);
				}
			}
			
			if (carriedNew.weightSum() <= vehicle.capacity()) {
				for (City neighbor : currentState.getCity().neighbors()) {
					double cost = currentNode.getCost() + currentState.getCity().distanceTo(neighbor)*vehicle.costPerKm();
					State state = new State(neighbor, carriedNew, deliveredNew, tasks);
					Node node = new Node(state, currentNode, cost);
					
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
		}//TODO failure
		
		//reorder nodes
		Stack<Node> stack = new Stack<Node>();
		stack.push(n);
		while(n.getParent() != null){
			n = n.getParent();
			stack.push(n);
		}

		Node n1 = stack.pop();
		Node n2 = n1;
		//print("THE PLAN IS :");
		while(!stack.isEmpty()){
			//print(n1);
			n2 = stack.pop();
			addActions(plan, n1, n2);
			n1 = n2;
		}
		//print(n2);
		return plan;
	}

	public void addActions(Plan plan, Node n1, Node n2){
		
		//Deliver tasks
		TaskSet delivery = TaskSet.intersectComplement(n2.getState().getDeliveredTasks(), n1.getState().getDeliveredTasks());
		for(Task task : delivery)
			plan.appendDelivery(task);

		//Pickup tasks
		TaskSet pickup = TaskSet.intersectComplement(n2.getState().getCarriedTasks(), n1.getState().getCarriedTasks());
		for(Task task : pickup)
			plan.appendPickup(task);

		//Move city
		plan.appendMove(n2.getState().getCity());
	}

	//TODO static?
	public static boolean isFinal(State s, TaskSet tasks, Vehicle v){
		return(s.getDeliveredTasks().containsAll( TaskSet.union(v.getCurrentTasks(), tasks)));
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
		System.out.println(", State : city=" + s.getCity().name + ", carriedTasks=" + s.getCarriedTasks().toString() + ", deliveredTasks=" + s.getDeliveredTasks().toString());

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
//	StopWatch stopWatch = new StopWatch("My Stop Watch");
//
//	stopWatch.start("initializing");
//	Thread.sleep(2000); // simulated work
//	stopWatch.stop();
//
//	stopWatch.start("processing");
//	Thread.sleep(5000); // simulated work
//	stopWatch.stop();
//
//	stopWatch.start("finalizing");
//	Thread.sleep(3000); // simulated work
//	stopWatch.stop();
//
//	System.out.println(stopWatch.prettyPrint());
//}
