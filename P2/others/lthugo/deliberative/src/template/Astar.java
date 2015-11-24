package template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Astar{
    Queue<TreeNode> openSet = new LinkedList<TreeNode>();
    Queue<TreeNode> closedSet = new LinkedList<TreeNode>();
	List<City> cities = new LinkedList<City>();
	List<Task> tasklist = new LinkedList<Task>();
	Map<Integer, Integer> came_from = new HashMap<Integer, Integer>();
	Map<Integer, TreeNode> all_states = new HashMap<Integer, TreeNode>();
	Topology topology;
	TaskSet tasks;
	Vehicle vehicle;
	double min_cost = Double.MAX_VALUE;
	int res_index = -1;
	int[] goal; 
	int[] start;
	
	public Astar(Topology topology, TaskSet tasks,  Vehicle vehicle, int[] start,  int[] goal){
		this.topology = topology;
		this.vehicle = vehicle;
		this.tasks = tasks;
		this.start = start;
		this.goal = goal;
		this.cities = topology.cities();
	}
	
	//============= initialize start state=================
	public TreeNode getStartState(){		
		int i = 1;
		for(Task task: tasks){
			tasklist.add(task);
		}				
		
		TreeNode startState = new TreeNode(start);
		startState.g_score = 0;
		startState.f_score = startState.g_score + heuristic_cost_estimate(startState.location, goal);

		// print start state
		System.out.println("Start state: ");
		for(i = 0; i < startState.location.length; i++)
			System.out.print(startState.location[i] + " ");
		System.out.println();

		// print goal state
		System.out.println("Goal state: ");
		for(i = 0; i < startState.location.length; i++)
			System.out.print(goal[i] + " ");
		System.out.println();

		return startState;
	}
	
	//=============use Astar algorithm to get shortest path===========
	public List<TreeNode> getShortestPath(){
		TreeNode startState = getStartState();
		openSet.add(startState);
		all_states.put(startState.index, startState);

		while(!openSet.isEmpty()){
			TreeNode current = lowestScore();
			
			if(goal_reached(current, goal) == true){
				return reconstruct_path(current);
			}
			
			openSet.remove(current);
			closedSet.add(current);
			List<TreeNode> neighbors = getNeighborStates(current);

			for(TreeNode neighbor: neighbors){
				if(checkContains(neighbor, closedSet) != null) {
					pruning(neighbor);
					continue;
				}
				
				double tentative_g_score = current.g_score + cities.get(current.location[0]).distanceTo(cities.get(neighbor.location[0])) * vehicle.costPerKm();
				
				TreeNode existed = checkContains(neighbor, openSet);
				if(existed == null || tentative_g_score < existed.g_score){
					if(existed != null) {
						pruning(existed);
						openSet.remove(existed);
						closedSet.add(existed);
					}
					came_from.put(neighbor.index, current.index);
					neighbor.g_score = tentative_g_score;
					neighbor.f_score = neighbor.g_score + heuristic_cost_estimate(neighbor.location, goal);
					openSet.add(neighbor);									
				}
			}
		}
		return null;
	}
	
	//================check if has reached goal==================
	public boolean goal_reached(TreeNode current, int[] goal){
		int[] location = current.location.clone();
		for(int i = 1; i < location.length; i++)
			if(location[i] != goal[i])
				return false;
		return true;
	}
	
	//============ get node with lowest f score in openSet==========
	public TreeNode lowestScore(){
		double min_f_score = Double.MAX_VALUE;
		TreeNode res = null;
		for(TreeNode node: openSet){
			if(node.f_score < min_f_score){
				min_f_score = node.f_score;
				res = node;
			}
		}
		return res;
	}

	//=================== heuristic function=====================
	public double heuristic_cost_estimate(int[] current_location, int[] goal){
		double h_score = 0;
		for(int i = 1; i < goal.length; i++){
			if(goal[i] == current_location[i]) continue;
			double tmp = cities.get(current_location[0]).distanceTo(cities.get(current_location[i])) + cities.get(current_location[i]).distanceTo(cities.get(goal[i]));
			h_score = Math.max(tmp,  h_score);
		}
//		return h_score * vehicle.costPerKm();
		return 0;
	}
	
	//================ delete node and its children================
	public void pruning(TreeNode node){
		for(Entry entry: came_from.entrySet()){
			if(entry.getValue().equals(node.index)){
				pruning(all_states.get(entry.getKey()));
				openSet.remove(all_states.get(entry.getKey()));
				closedSet.add(all_states.get(entry.getKey()));
			}
		}
	}
	
	//=================check if contained in set====================
	public TreeNode checkContains(TreeNode current, Queue<TreeNode> set){
		for(TreeNode node: set){
			if(Arrays.equals(current.location, node.location))
				return node;
		}
		return null;
	}
	
	//============= reconstruct path from start to goal=============
	public List<TreeNode> reconstruct_path(TreeNode current){
		List<TreeNode> res = new LinkedList<TreeNode>();
		if(came_from.containsKey(current.index)){
			res = reconstruct_path(all_states.get(came_from.get(current.index)));
		}
		res.add(current);
		return res;
	}
	
	//============== get valid neighbor states====================
	public List<TreeNode> getNeighborStates(TreeNode current){
		List<TreeNode> res = new ArrayList<TreeNode>();
		City currentCity = cities.get(current.location[0]);
		
		for(City neighbor: currentCity.neighbors()){
			int neighbor_id = neighbor.id;
			int[] new_location = current.location.clone();
			Set<Task> new_carried_tasks = new HashSet<Task>(current.carried_tasks);
			new_location[0] = neighbor.id;	
			
			// initiate the first result, move to a neighboring city without carrying new tasks
			List<Task> delivered = new ArrayList<Task>();
			for(Task task: new_carried_tasks){
				if(new_location[tasklist.indexOf(task)+1] == goal[tasklist.indexOf(task)+1]){
					if(new_location[tasklist.indexOf(task)+1] == currentCity.id)
						delivered.add(task);
					continue;
				}
				else
					new_location[tasklist.indexOf(task)+1] = neighbor_id;
			}
			for(Task task: delivered)
				new_carried_tasks.remove(task);

			TreeNode new_state = new TreeNode(new_location);
			new_state.carried_tasks = new_carried_tasks;
			
			List<TreeNode> res0 = new ArrayList<TreeNode>();
			res0.add(new_state);
			all_states.put(new_state.index, new_state);		
			
			// add other neighboring states with new carries tasks
			for(int i = 1; i < goal.length; i++){
				if(new_location[i] == currentCity.id && new_location[i] != goal[i]){
					int size = res0.size();
					for(int j = 0; j < size; j++){
						TreeNode tmp = res0.get(j);
						int sum = 0;
						for(Task task: tmp.carried_tasks)  // the id in carried_task is correspondent to task ids, and (i-1) for location[i]
							sum += task.weight;
						sum += tasklist.get(i-1).weight;
						
						if(sum <= vehicle.capacity()){
							int[] temp_location = tmp.location.clone();	// pick up the task, and change its location
							temp_location[i] = neighbor_id;
							Set<Task> temp_carried_tasks = new HashSet<Task>(tmp.carried_tasks);   // pick up the task, and add to carried_tasks
							temp_carried_tasks.add(tasklist.get(i-1));
							TreeNode temp = new TreeNode(temp_location);
							temp.carried_tasks = temp_carried_tasks;
							res0.add(temp);
							all_states.put(temp.index, temp);
						}
					}
				}
			}
			res.addAll(res0);
		}
		return res;
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
}
