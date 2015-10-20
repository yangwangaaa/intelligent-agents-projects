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

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			print("Start A*");
			plan = AStar(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = BFS(vehicle, tasks);
			break;
		case NAIVE:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
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

		//Create first node of the search
		City current = vehicle.getCurrentCity();
		//tasks.a
		TaskSet delivered = TaskSet.noneOf(tasks);//sur et chez vlad? vehi . capa
		State s = new State(current, vehicle.getCurrentTasks(), delivered, vehicle.capacity());
		Node first = new Node(s, null, 0);

		// check bfs implem
		Queue<Node> Q = new LinkedList(); //linded lest ou arrayList? linked mieux pour moi
		HashSet<Node> C = new  HashSet();
		/////hash puis final
		Node n = BFS_search(first, Q, C, tasks, vehicle);

		if(n == null){
			print("pas de plan");
			return null;}//TODO failure??re
		else{
			Plan plan = new Plan(current);
			return computeFinalPlan(plan , n);
		}
	}

	private Plan AStar(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), TaskSet.noneOf(tasks),vehicle.capacity());
		Node root = new Node(initialState, null, 0);

		HashMap<String, Double> C = new HashMap<String, Double>();
		ArrayList<Node> Q = new ArrayList<Node>(); //fais une priority queue!!!!
		Q.add(root);

		ArrayList<Node> Test1 = getSuccessors(root, tasks);
		ArrayList<Node> Test2 = getSuccessors(root, tasks);
		
		Node n1 = Test1.get(0);
		Node n2 = Test2.get(0);
		print(n1);
		print(n2);
		
		print(n1.getState().toString());
		print(n2.getState().toString());
		
		C.put(n1.getState().toString(), n1.getCost());
		
		print("C contains n1 : " + C.containsKey(n1.getState().toString()));
		print("C contains n2 : " + C.containsKey(n2.getState().toString()));

		while(!Q.isEmpty()) {
			Node currentNode = Q.remove(0);

			State currentState = currentNode.getState();

			if(isFinal(currentState, tasks, vehicle)) return computeFinalPlan(plan, currentNode);


			if( !C.containsKey(currentState.toString()) || currentNode.getCost()<C.get(currentState)) {
				C.put(currentState.toString(), currentNode.getCost());

				ArrayList<Node> S = getSuccessors(currentNode, tasks);

				Q.addAll(S);

				Collections.sort(Q);
			}
			else {
				print("ALREADY VISITED");
			}
			
		}

		return null;
	}

	////////////////////////////////////////////////////////
	//													  //
	//					   SUBMETHODS   	   			  //
	//													  //
	////////////////////////////////////////////////////////

	private Node BFS_search(Node first, Queue<Node> Q,  HashSet<Node> C ,TaskSet tasks, Vehicle vehicle){
		while(!Q.isEmpty()){
			Node current = Q.remove();
			if(isFinal(current.getState(), tasks, vehicle)){
				return current;
			}
			if(!C.contains(current)){
				C.add(current);
				ArrayList<Node> S = getSuccessors(current, tasks);
				Q.addAll(S);
			}
		}
		return null;
	}

	private ArrayList<Node> getSuccessors(Node currentNode, TaskSet tasks) {
		ArrayList<Node> successors = new ArrayList<Node>();
		State currentState = currentNode.getState();

		TaskSet availableTasks = TaskSet.noneOf(tasks);
		TaskSet tasksToBeDelivered = TaskSet.noneOf(tasks);
		for(Task task : tasks) {
			if(task.pickupCity == currentState.getCity()) availableTasks.add(task);
			if(task.deliveryCity == currentState.getCity()) tasksToBeDelivered.add(task);
		}
		Set<Set<Task>> powerTaskSet = powerSet(availableTasks);


		for (City neighbor : currentState.getCity().neighbors()) {


			TaskSet deliveredTasks = TaskSet.intersect(currentState.getCarriedTasks(), tasksToBeDelivered);
			TaskSet carriedTasks = TaskSet.intersectComplement( currentState.getCarriedTasks(), deliveredTasks);
			TaskSet allDeliveredTasks = TaskSet.union(currentState.getDeliveredTasks(), deliveredTasks);

			int availableCapacity = currentState.getAvailableCapacity() + deliveredTasks.weightSum(); 
			double cost = currentNode.getCost() + currentState.getCity().distanceTo(neighbor);

			for(Set<Task> tasksSubset : powerTaskSet) {
				TaskSet availableTasksSubset = TaskSet.noneOf(tasks);
				for (Task task : tasksSubset) {
					availableTasksSubset.add(task);
				}

				if(availableTasksSubset.weightSum() < currentState.getAvailableCapacity()) {
					TaskSet allCarriedTasks = TaskSet.union(carriedTasks, availableTasksSubset);

					int capacity = availableCapacity - availableTasksSubset.weightSum(); 
					State state = new State(neighbor, allCarriedTasks, allDeliveredTasks, capacity);

					Node node = new Node(state, currentNode, cost);
					successors.add(node);
				}
			}
		}

		return successors;
	}


	public Plan computeFinalPlan(Plan plan, Node n){

		//reorder nodes
		Stack<Node> stack = new Stack();
		stack.push(n);
		while(n.getParent() != null){
			n = n.getParent();
			stack.push(n);
		}

		Node n1 = stack.pop();
		Node n2 = n1;
		while(!stack.isEmpty()){
			n2 = stack.pop();
			addActions(plan, n1, n2);
			n1 = n2;
		}
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
		System.out.println(", State : city=" + s.getCity().name + ", capa=" + s.getAvailableCapacity() +", carriedTasks=" + s.getCarriedTasks().toString() + ", deliveredTasks=" + s.getDeliveredTasks().toString());

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
