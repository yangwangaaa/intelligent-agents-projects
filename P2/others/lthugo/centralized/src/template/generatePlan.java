package template;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class generatePlan {
	TaskPlan bestPlan = new TaskPlan(); 
	double prob = 0.35;
	int max_round = 10000;
	List<Vehicle> vehicles;
	List<Task> taskslist = new ArrayList<Task>();
	Map<Integer, City> tasks_map = new HashMap<Integer, City>();
	
	public generatePlan(List<Vehicle> vehicles, TaskSet tasks){
		this.vehicles = vehicles;
		for(Task task: tasks){
			taskslist.add(task);
			tasks_map.put(task.id, task.pickupCity);
			tasks_map.put(task.id + tasks.size(), task.deliveryCity);
		}
	}
	
	//========================main function to generate the best plan===================
	public TaskPlan SLS(){
		TaskPlan A_old = new TaskPlan();
		TaskPlan A = InitialPlan();
		if(A == null) return null;
		
		for(int round = 0; round < max_round; round++){
			A_old = A;
			A = ChooseNeighbors(A_old);
			
			if(A.cost < bestPlan.cost)
				bestPlan = A;
		}
		System.out.println("Best plan: ");
		printPlan(bestPlan);
		return bestPlan;
	}
	
	
	//=====================initialization, generating an initial plan==================
	public TaskPlan InitialPlan(){
		TaskPlan initialPlan = new TaskPlan();
		
		/*
		// get biggest vehicle
		Vehicle biggest_vehicle = vehicles.get(0);
		for(Vehicle vehicle: vehicles){
			if(vehicle.capacity() > biggest_vehicle.capacity())
				biggest_vehicle = vehicle;
		}
		
		// initialize plan
		for(Vehicle vehicle: vehicles){
			List<Integer> list = new ArrayList<Integer>();
			if(vehicle == biggest_vehicle){
				for(int k = 0; k < taskslist.size(); k++){
					if(taskslist.get(k).weight > vehicle.capacity()) return null;
					list.add(k);
					list.add(k + taskslist.size());
				}
			}
			initialPlan.vehicleTasks.add(list);
		}
		*/

		Random rand = new Random();
        int index = rand.nextInt(vehicles.size());
        
		for(int i = 0; i < vehicles.size(); i++)
			initialPlan.vehicleTasks.add(i, new ArrayList<Integer>());
        
		for(int k = 0; k < taskslist.size(); k++){
			index = rand.nextInt(vehicles.size());
	        while(taskslist.get(k).weight > vehicles.get(index).capacity())
	        	index = rand.nextInt(vehicles.size());
	        initialPlan.vehicleTasks.get(index).add(0, k);
	        initialPlan.vehicleTasks.get(index).add(k + taskslist.size());
		}
		
		initialPlan.cost = calculateCost(initialPlan);
		bestPlan = initialPlan;
		
		System.out.println("Initial plan: ");
		printPlan(initialPlan);
		
		return initialPlan;
	}
	
	// =======================calculate cost for each plan=======================
	public double calculateCost(TaskPlan plan){
		double cost = 0;
		List<List<Integer>> vehicle_list = plan.vehicleTasks;
		
		// for each vehicle list, sum up their costs
		for(int vehicle_index = 0; vehicle_index < vehicle_list.size(); vehicle_index++){
			Vehicle vehicle = vehicles.get(vehicle_index);
			List<Integer> list = vehicle_list.get(vehicle_index);
			
			if(list.size() == 0) continue;
			int first_city = list.get(0);
						
			// get the distance of vehicle location to first place
			cost += (vehicle.getCurrentCity().distanceTo(tasks_map.get(first_city))) * vehicle.costPerKm();
			
			// sum the distances from last city to current city
			for(int i  = 1; i < list.size(); i++){
				City last = tasks_map.get(list.get(i-1));
				cost += (last.distanceTo(tasks_map.get(list.get(i)))) * vehicle.costPerKm();
			}
		}
		
		return cost;
	}
	

	//========================== return the next plan according to prob=======================
	public TaskPlan LocalChoice(TaskPlan A_old, Queue<TaskPlan> N){
        TaskPlan A = N.poll();        
        
        double rand = Math.random();
		Random rd = new Random();

		if(rand < prob)
			return A;
		else if((rand > 2 * prob && N.size() <= 0) || (rand >= prob && rand <= 2 * prob))
			return A_old;
		else
			return (TaskPlan) N.toArray()[rd.nextInt(N.size())];
	}
	
    //======================Comparator anonymous class implementation=====================
    public static Comparator<TaskPlan> costComparator = new Comparator<TaskPlan>(){
         
        @Override
        public int compare(TaskPlan c1, TaskPlan c2) {
            return (int) (c1.cost - c2.cost);
        }
    };
    
    
    //=============================Choose neighbors function============================

    public TaskPlan ChooseNeighbors(TaskPlan A_old){
        Queue<TaskPlan> N = new PriorityQueue<TaskPlan>(10000, costComparator);

        // pick a vehicle with non-empty tasks
        Random rand = new Random();
        int index = rand.nextInt(vehicles.size());
        while(A_old.vehicleTasks.get(index).size() == 0)
        	index = rand.nextInt(vehicles.size());
        
        // change vehicles
		Random rd = new Random();
        int t = A_old.vehicleTasks.get(index).get(rd.nextInt(A_old.vehicleTasks.get(index).size()));
        if(t >= taskslist.size())
        	t = t - taskslist.size();
		
        for(Vehicle vehicle: vehicles){
        	if(vehicle.id() == index) continue;
        	if(taskslist.get(t).weight <= vehicle.capacity()){
        		TaskPlan A = ChangeVehicle(A_old, index, vehicle.id(), t);
        		N.add(A);
        	}
        }
        
        // change order
        int length = A_old.vehicleTasks.get(index).size();
        if(length >= 2){
        	for(int id1 = 0; id1 <= length-2; id1++){
        		for(int id2 = id1+1; id2 <= length-1; id2++){
        			if(id1 == id2) continue;
        			TaskPlan A = ChangeOrder(A_old, index, id1, id2);
        			if(A != null) {
        				N.add(A);
        			}
        		}
        	}
        }
        
		return LocalChoice(A_old, N); 
	}
	
	//=================================Change vehicles===================================
	public TaskPlan ChangeVehicle(TaskPlan A_old, int vid1, int vid2, int t){
		TaskPlan A = new TaskPlan();
    	for(int i = 0; i < A_old.vehicleTasks.size(); i++)
    		A.vehicleTasks.add(new ArrayList<Integer>(A_old.vehicleTasks.get(i)));

    	int pickup = t;
		int delivery = pickup + taskslist.size();
    	
		A.vehicleTasks.get(vid2).add(pickup);
		A.vehicleTasks.get(vid2).add(delivery);
		A.vehicleTasks.get(vid1).remove(A.vehicleTasks.get(vid1).indexOf(pickup));
		A.vehicleTasks.get(vid1).remove(A.vehicleTasks.get(vid1).indexOf(delivery));

		A.cost = calculateCost(A);
		return A;
	}
	
	//=================================Change tasks===================================
	public TaskPlan ChangeOrder(TaskPlan A_old, int index, int id1, int id2){
		TaskPlan A = new TaskPlan();
    	for(int i = 0; i < A_old.vehicleTasks.size(); i++)
    		A.vehicleTasks.add(new ArrayList<Integer>(A_old.vehicleTasks.get(i)));		
		
    	List<Integer> list = A.vehicleTasks.get(index);
		
    	// swap
    	int tmp = list.get(id1);
    	list.set(id1, list.get(id2));
    	list.set(id2, tmp);
    	
    	// test constraints
    	Set<Integer> set = new HashSet<Integer>();
    	int weight = 0;
    	
    	for(int i = 0; i <= Math.max(id1, id2); i++){
    		if(list.get(i) < taskslist.size()){
    			if(set.contains(list.get(i) + taskslist.size())) return null;
    			if((weight + taskslist.get(list.get(i)).weight) > vehicles.get(index).capacity()) return null;
    			
    			set.add(list.get(i));
    			weight += taskslist.get(list.get(i)).weight;
    		}
    		else{
    			if(! set.contains(list.get(i) - taskslist.size())) return null;
    			set.remove(list.get(i) - taskslist.size());
    			weight -= taskslist.get(list.get(i) - taskslist.size()).weight;
    		}
    	}
		A.cost = calculateCost(A);
		return A;
	}
	
	//======================print the tasks taken by each vehicle in the plan=====================
	public void printPlan(TaskPlan plan){
		System.out.println("Plan cost = " + plan.cost);
		for(int vehicle_index = 0; vehicle_index < plan.vehicleTasks.size(); vehicle_index++){
			Vehicle vehicle = vehicles.get(vehicle_index);
			List<Integer> list = plan.vehicleTasks.get(vehicle_index);
			
			System.out.print("Vehicle " + vehicle_index + " has tasks: ");
			for(int i  = 0; i < list.size(); i++)
				System.out.print(list.get(i) + ", ");
			System.out.println();
		}
	}
}
