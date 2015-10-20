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

import com.sun.corba.se.spi.orbutil.fsm.Action;

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
		PrintTaskSet(tasks); //TODO assure toi que les task deja remplies et celle enlevé ne sont plus dedans
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
			Print("pas de plan");
			return null;}//TODO failure??re
		else{
			Plan plan = new Plan(current);
			return computePlan(plan , n);
		}
	}
	
	
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
	
	
		

	private Plan AStar(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		State initialState = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), tasks.noneOf(tasks),vehicle.capacity());
		Node root = new Node(initialState, null, 0);

		HashMap<State, Double> C = new HashMap<State, Double>();
		ArrayList<Node> Q = new ArrayList<Node>(); //fais une priority queue!!!!
		Q.add(root);

		while(Q.isEmpty()) {
			Node currentNode = Q.get(0);
			State currentState = currentNode.getState();

			if(isFinal(currentState)) return computeFinalPlan(currentNode);

			
			if( !C.containsKey(currentState) || currentNode.getCost()<C.get(currentState)) {
				C.put(currentState, currentNode.getCost());

				ArrayList<Node> S = getSuccessors(currentNode, tasks);

				Q.addAll(S);

				Collections.sort(Q);
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
		Set<TaskSet> powerTaskSet = powerSet(availableTasks);

		
		for (City neighbor : currentState.getCity().neighbors()) {
			
			TaskSet deliveredTasks = TaskSet.intersect(currentState.getCarriedTasks(), tasksToBeDelivered);
			TaskSet carriedTasks = TaskSet.intersectComplement( currentState.getCarriedTasks(), deliveredTasks);//bof bof comme nom 
			TaskSet allDeliveredTasks = TaskSet.union(currentState.getDeliveredTasks(), deliveredTasks);
			
			double availableCapacity = currentState.getAvailableCapacity() - deliveredTasks.weightSum(); //TODO pour moi c'est plus
			State newState = new State(neighbor, carriedTasks, allDeliveredTasks, availableCapacity);
			
			
			double cost = currentNode.getCost() + currentState.getCity().distanceTo(neighbor);
			Node newNode = new Node(newState, currentNode, cost);
			
			successors.add(newNode);
			
			for(TaskSet taskSet : powerTaskSet) {//TODO lecas ou taskSet est vide est deja pris en compte ci dessus

			}
		}

		return successors;
	}


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

	public Plan computePlan(Plan plan, Node n){
		
		Stack<Node> stack = new Stack();
		stack.push(n);
		while(n.getParent() != null){
			n = n.getParent();
			stack.push(n);
		}

		
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
	
	//TODO static?
	public static boolean isFinal(State s, TaskSet tasks, Vehicle v){
		return(s.getDeliveredTasks().containsAll( TaskSet.union(v.getCurrentTasks(), tasks)));
	}
}
