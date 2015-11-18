package template;

/* import table */
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
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
	List<City> cities = new LinkedList<City>();
	Plan plan;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "BFS");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		cities = topology.cities();
		// ...
	}
	
	//==================get start location===================
	public int[] getStart(Vehicle vehicle, TaskSet tasks, City current){
		
		int[] start_location = new int[tasks.size() + 1];
		start_location[0] = current.id;
		int i = 1;

		for(Task task: tasks)
			start_location[i++] = task.pickupCity.id;
		return start_location;
	}
	
	//==================get goal location===================
	public int[] getGoal(Vehicle vehicle, TaskSet tasks){
		int i = 1;
		int[] goal = new int[tasks.size() + 1];
		
		for(Task task: tasks)
			goal[i++] = task.deliveryCity.id;   // initialize goal state
		return goal;
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : vehicle.getCurrentTasks()) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.deliveryCity)){
				plan.appendMove(city);
			}
			plan.appendDelivery(task);
			current = task.deliveryCity;
		}
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks, plan, 0, current);
			break;
		case BFS:
			// ...
			plan = naivePlan(vehicle, tasks, plan, 1, current);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks, Plan plan, int type, City current) {
		
		//============== get shortest path from A* algorithm==============
		int[] start = getStart(vehicle, tasks, current);
		int[] goal = getGoal(vehicle, tasks);
		List<TreeNode> path = null;
		
		long startTime = System.currentTimeMillis();
		
		if(type == 0){
			System.out.println("A*");
			Astar astar = new Astar(topology, tasks,  vehicle, start, goal);		
			path = astar.getShortestPath();
		}
		else{
			System.out.println("BFS");
			BFS bfs = new BFS(topology, tasks,  vehicle, start, goal);		
			path = bfs.getShortestPath();			
		}
//		printSteps(path);
		
		long estimatedTime = System.currentTimeMillis() - startTime;
		System.out.println("Run time: " + estimatedTime + " milliseconds");
		
		//==================== initialize the plan=====================
		for(int k = 0; k < path.size(); k++){			
			// detect if current status has changed, if so, replan			
			Set<Task> pick_up = new HashSet<Task>();
			Set<Task> delivery = new HashSet<Task>();
			
			if(k < path.size() - 1){
				Set<Task> set_current = new HashSet<Task>();
				Set<Task> set_next = new HashSet<Task>();

				set_current.addAll(path.get(k).carried_tasks);
				set_next.addAll(path.get(k+1).carried_tasks);

				pick_up.addAll(path.get(k+1).carried_tasks);
				pick_up.addAll(path.get(k).carried_tasks);
			    pick_up.removeAll(set_current);
			    
			    delivery.addAll(path.get(k+1).carried_tasks);
			    delivery.addAll(path.get(k).carried_tasks);
			    delivery.removeAll(set_next);
			}
			else if(k == path.size() - 1){
				delivery.addAll(path.get(k).carried_tasks);
			}
			
			// deliver a task
			for(Task task: delivery)
				plan.appendDelivery(task);
			
			// pick up a task and change the position of this task in tasks_position
			for(Task task: pick_up)
				plan.appendPickup(task);
			
			// if not the last step, move to next city
			if(k < path.size() - 1)
				plan.appendMove(cities.get(path.get(k+1).location[0]));		
		}
		System.out.println(plan.totalDistance() * vehicle.costPerKm());
		return plan;
	}

	public void printState(TreeNode temp){
		System.out.print("new state " + temp.index + ": ");
		for(int x = 0; x < temp.location.length; x++)
			System.out.print(temp.location[x] + " ");
		System.out.print(" cost = " + temp.g_score);

		System.out.print(", carried task: ");
		for(Task task: temp.carried_tasks)
			System.out.print(task.id + ", ");
		System.out.println();
	}
	
	//================print the states found by A*==============
	public void printSteps(List<TreeNode> path){
		for(TreeNode node: path){
			for(int i = 0; i < node.location.length; i++)
				System.out.print(node.location[i] + " ");
			System.out.print(", carried tasks: ");
			for(Task task: node.carried_tasks)
				System.out.print(task.id + " ");
			System.out.println();
		}
	}	
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
	}
}
