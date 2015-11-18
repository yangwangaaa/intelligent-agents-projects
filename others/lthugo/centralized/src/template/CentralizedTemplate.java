package template;

//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	Map<Integer, City> tasks_map = new HashMap<Integer, City>();
	List<Task> taskslist = new ArrayList<Task>();

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		for(Task task: tasks){
			taskslist.add(task);
			tasks_map.put(task.id, task.pickupCity);
			tasks_map.put(task.id + tasks.size(), task.deliveryCity);
		}
		
		long start_time = System.currentTimeMillis();

		List<Plan> plans = new ArrayList<Plan>();
		generatePlan generation = new generatePlan(vehicles, tasks);
		TaskPlan results = generation.SLS();
		
		long end_time = System.currentTimeMillis();
		long difference = end_time-start_time;
		System.out.println("Time: " + difference/1000.00);
		
		for(int i = 0; i < vehicles.size(); i++){
			Plan planVehicle = naivePlan(vehicles.get(i), results.vehicleTasks.get(i));
			plans.add(planVehicle);
		}
		return plans;
	}
	
	private Plan naivePlan(Vehicle vehicle, List<Integer> list) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		if(list.size() == 0) 
			return Plan.EMPTY;
		
		for(int i = 0; i < list.size(); i++){
			City next_city = tasks_map.get(list.get(i));
			for (City city : current.pathTo(next_city))
				plan.appendMove(city);
			
			if(list.get(i) < taskslist.size())
				plan.appendPickup(taskslist.get(list.get(i)));
			else
				plan.appendDelivery(taskslist.get(list.get(i) - taskslist.size()));
			current = next_city;
		}
		
		return plan;
	}
}
