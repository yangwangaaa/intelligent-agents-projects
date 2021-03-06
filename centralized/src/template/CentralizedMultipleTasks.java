package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
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

/**
 * 
 * multiple start
 * neighbour changing vehicle pour chaque task
 *
 */
@SuppressWarnings("unused")
public class CentralizedMultipleTasks implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	private List<Vehicle> vehiclesList; 
	private Task[] tasks;

	private int Nt;
	private int Nv;
	private int Na;

	private double p = 0.5; // probability used for localChoice
	private int numIt = 5000;
	private Random random;

	//////////////////////////////////////
	//              MAIN                //
	//////////////////////////////////////

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_default.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;


		random = new Random();
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		this.vehiclesList = vehicles;
		this.tasks = tasks.toArray(new Task[tasks.size()]);
		this.Nt = this.tasks.length;
		this.Nv = vehiclesList.size();
		this.Na = 2*Nt;

		//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		NodePD bestSolution = SLS();
		System.out.println("FINAL SOLUTION:");
		bestSolution.print();

		List<Plan> plans = computeFinalPlan(bestSolution);



		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in "+duration+" milliseconds.");

		return plans;
	}


	//////////////////////////////////////
	//               SLS                //
	//////////////////////////////////////

	private NodePD SLS() {
		NodePD A = selectInitialSolution();
		NodePD bestNodePD = A;

		A.getOValue(tasks, vehiclesList);
		A.print();

		int i = 0;
		while(i < numIt) {
			print("SLS iteration #" + i);
			NodePD Aold = A;
			ArrayList<NodePD> N = chooseNeighbours(A);
			A = localChoice(N, Aold); // only keep bestNodePD in chooseNeighbours : more efficient? = rename "chooseBestNeigbours"
			//print("BEST CHOOSEN AT IT " + i);
			//A.print();
			if(A.getOValue(tasks, vehiclesList) < bestNodePD.getOValue(tasks, vehiclesList)) bestNodePD = A;
			i++;
		}

		bestNodePD.getOValue(tasks, vehiclesList);

		return bestNodePD;
	}

	//////////////////////////////////////
	//         SLS : NEIGHBOURS         //
	//////////////////////////////////////

	// vladman
	private ArrayList<NodePD> chooseNeighbours(NodePD Aold) {
		ArrayList<NodePD> N = new ArrayList<NodePD>();
		int vi = random(Aold);

		// Applying the changing vehicle operator :
		//print("CHANGING VEHICLE");
		for (int vj=0; vj<Nv; vj++) {
			if(vj!=vi) {
				int t = Aold.nextAction(vi+Na);
				if(tasks[t].weight <= vehiclesList.get(vi).capacity()) { // no vehicle change if first task too heavy for all other vehicles
					NodePD A = changingVehicle(Aold, vi, vj);
					N.add(A);
					//A.getOValue(tasks, vehiclesList);
					//A.print();
				}
			}
		}

		// Applying the changing task order operator :
		// compute the number of tasks of the vehicle
		int length = 0;
		int t = Aold.nextAction(vi+Na); // current task in the list
		while(t!=-1) {
			t = Aold.nextAction(t);
			length++;
		}
		//print("CHANGING TASK ORDER");
		if(length>=2) {
			for (int tIdx1=0; tIdx1<length-1; tIdx1++) {
				for (int tIdx2=tIdx1+1; tIdx2<length; tIdx2++) {
					NodePD A = changingTaskOrder(Aold, vi, tIdx1, tIdx2);
					if(A!=null) {
						N.add(A);
						//A.getOValue(tasks, vehiclesList);
						//A.print();
					}
				}
			}
		}

		return N;
	}

	// vladman
	private NodePD changingVehicle(NodePD A, int v1, int v2) {
		NodePD A1 = A.clone();

		int p = A.nextAction(v1+Na);
		int d = p+Nt;
		int dNext = A1.nextAction(d);
		int v2Next = A1.nextAction(v2+Na);

		Boolean following = A1.nextAction(p)==d;

		// update nextAction and previousAction v1
		if(!following) {
			A1.nextAction(v1+Na, A1.nextAction(p));
			A1.previousAction(A1.nextAction(v1+Na), v1+Na);

			A1.nextAction(A1.previousAction(d), A1.nextAction(d));
			if(dNext!=-1) A1.previousAction(A1.nextAction(d), A1.previousAction(d));
		}
		else {
			A1.nextAction(v1+Na, A1.nextAction(d));
			if(dNext!=-1) A1.previousAction(A1.nextAction(d), v1+Na);
		}

		// update nextAction and previousAction v2
		A1.nextAction(d, A1.nextAction(v2+Na));
		if(v2Next!=-1) A1.previousAction(v2Next, d);

		A1.nextAction(p, d);
		A1.previousAction(d, p);

		A1.nextAction(v2+Na, p);
		A1.previousAction(p, v2+Na);

		// update time v1 and v2
		updateTime(A1, v1);
		updateTime(A1, v2);

		// update vehicle of p and d
		A1.setVehicle(p, v2);
		A1.setVehicle(d, v2);

		// update load v1 and v2
		A1.setLoad(d, 0);

		if(!following) { // remove weight of task pd for actions that were between p and d
			int a = A1.nextAction(v1+Na);
			while(a!=dNext) {
				A1.setLoad(a, A1.getLoad(a)-tasks[p].weight);
				a = A1.nextAction(a);
			}
		}

		return A1;
	}

	// mouche
	private NodePD changingTaskOrder(NodePD A, int v, int t1, int t2) {
		NodePD A1 = A.clone();

		// find id a1 of the t1th action and find the id a2 of the t2th action.
		int a1 = A1.nextAction(v + Na); // task1
		int count = 0;
		while(count < t1){
			a1 = A1.nextAction(a1);
			count++;
		}
		int a2 = A1.nextAction(a1); // task2
		count++;

		while(count < t2){
			a2 = A1.nextAction(a2);
			count++;
		}

		// Checks if we can exchange the order of a1 and a2 then update the time and load variables.
		int time1 = A1.getTime(a1);
		int capacity = vehiclesList.get(A1.getVehicle(a1)).capacity();
		//D stands for delivery, P stands for pickup.
		if(a1>=Nt){// a1 is delivery D
			if(a2 >= Nt){ // D1....D2 to D2....D1

				if(time1 <= A1.getTime(a2-Nt)) //D2 before P2
					return null;
				int delta = tasks[a1-Nt].weight - tasks[a2-Nt].weight;
				if(delta>0){
					for(int i = a1; i!= a2; i = A1.nextAction(i)){
						if(A1.getLoad(i) + delta > capacity)
							return null;
					}
				}
				updateTimeAndLoad(A1, a1, a2, time1, delta);

			}else{ //D1....P2 to P2....D1

				int delta = tasks[a1-Nt].weight + tasks[a2].weight;
				for(int i = a1; i!= a2; i = A1.nextAction(i)){
					if(A1.getLoad(i) + delta > capacity)
						return null;
				}
				updateTimeAndLoad(A1, a1, a2, time1, delta);

			}
		}
		else{// a1 is pick up
			if(a2 >= Nt){ // P1....D2 to D2....P1

				if(time1 <= A1.getTime(a2-Nt) ||        //D2 before P2 or
						A1.getTime(a2) >= A1.getTime(a1+Nt) )//P1 after D1
					return null;
				int delta = -tasks[a1].weight - tasks[a2-Nt].weight; //delta is negative load is ok //TODO le -?
				updateTimeAndLoad(A1, a1, a2, time1, delta);

			}else{ //P1....P2 to P2....P1
				if(A1.getTime(a2) >= A1.getTime(a1+Nt)) //P1 after D1
					return null;
				int delta = tasks[a2].weight - tasks[a1].weight;
				if(delta>0){
					for(int i = a1; i!= a2; i = A1.nextAction(i)){
						if(A1.getLoad(i) + delta > capacity)
							return null;
					}
				}
				updateTimeAndLoad(A1, a1, a2, time1, delta);
			}
		}

		// Exchange the order of a1 and a2.
		boolean a2Last = A1.nextAction(a2)==-1;
		if(A1.nextAction(a1) == a2){ // Action a1 is just before a2

			A1.nextAction(A1.previousAction(a1),a2);

			if(!a2Last){
				A1.previousAction(A1.nextAction(a2), a1);
			}
			int pre = A1.previousAction(a1);

			//a1
			A1.previousAction(a1, a2);
			A1.nextAction(a1,A1.nextAction(a2));

			//a2
			A1.previousAction(a2, pre);
			A1.nextAction(a2, a1);	
		}else{
			int pre = A1.previousAction(a1);
			int post = A1.nextAction(a1);

			//neighbor of a1
			A1.nextAction(A1.previousAction(a1),a2);
			A1.previousAction(A1.nextAction(a1),a2);

			//neighbor of a2
			A1.nextAction(A1.previousAction(a2),a1);
			if(!a2Last){
				A1.previousAction(A1.nextAction(a2),a1);
			}
			//a1
			A1.previousAction(a1, A1.previousAction(a2));
			A1.nextAction(a1, A1.nextAction(a2));

			//a2
			A1.previousAction(a2, pre);
			A1.nextAction(a2, post);
		}
		return A1;
	}

	//mouche
	private void updateTimeAndLoad(NodePD A1, int a1, int a2, int time1, int delta ){
		//Update time
		A1.setTime(a1, A1.getTime(a2));
		A1.setTime(a2, time1);
		//Update Load
		for(int i = a1; i!= a2; i = A1.nextAction(i)){
			A1.setLoad(i, A1.getLoad(i) + delta);
		}
	}

	// mouche

	private void updateTime(NodePD A, int v) {
		int ti = A.nextAction(v+Na);
		if(ti != -1){
			A.setTime(ti, 0); //Time starts at 0
			int tj = A.nextAction(ti);
			while(tj != -1){
				A.setTime(tj, A.getTime(ti) + 1);
				ti = tj;
				tj = A.nextAction(ti);
			}
		}
	}

	//////////////////////////////////////
	//           SLS : OTHERS           //
	//////////////////////////////////////

	// vladman
	private NodePD selectInitialSolution() { // 
		// use global variables: vehicles, tasks, ...
		NodePD initial = new NodePD(vehiclesList, tasks);

		Vehicle biggestV = vehiclesList.get(0);
		for (Vehicle v : vehiclesList) {
			if(biggestV.capacity()<v.capacity()) biggestV = v;
		}

		int index = biggestV.id();
		initial.nextAction(index+Na, 0);
		initial.previousAction(0, index+Na);
		initial.previousAction(index+Na, -1);

		for (int i = 0; i<Nt; i++) {
			if(this.tasks[i].weight>biggestV.capacity()) {
				System.out.println("ERROR : One task is too heavy for the vehicles");
				return null;
			}

			int next;
			if(i==Nt-1) next = -1;
			else next = i+1;

			initial.nextAction(i, Nt+i);
			initial.nextAction(Nt+i, next);

			initial.previousAction(Nt+i, i);
			if(next!=-1) initial.previousAction(next, Nt+i);

			// load :
			initial.setLoad(i, tasks[i].weight);
			initial.setLoad(Nt+i, 0);
		}

		updateTime(initial, index);

		/*
		for(int i=0; i<Nv; i++) {
			updateTime(initial, i);
		}
		 */

		for(int i=0; i<Na; i++) {
			initial.setVehicle(i, index);
		}

		return initial;
	}

	// mouche
	private NodePD localChoice(ArrayList<NodePD> N, NodePD Aold) {
		if(N == null){
			return Aold;
		}else{
			NodePD bestNodePD = N.get(0);
			for(NodePD n : N){
				if(n.getOValue(tasks, vehiclesList)<=bestNodePD.getOValue(tasks, vehiclesList))
					bestNodePD = n;
			}
			if(Math.random()<= p){
				return bestNodePD;
			}
			return Aold;
		}	
	}

	// mouche
	private List<Plan> computeFinalPlan(NodePD n) {
		List<Plan> plans = new ArrayList<Plan>();

		for(int v = 0 ; v<Nv ; v++){ // for each vehicle
			City current = vehiclesList.get(v).getCurrentCity();
			Plan plan = new Plan(current);

			for(int t = Na + v; n.nextAction(t) != -1 ; t = n.nextAction(t)){ // for each task of the vehicle //TODO 2*Nt
				Task task = tasks[n.nextAction(t) % Nt];				

				if(n.nextAction(t) < Nt){ // action is Pickup
					// move: current city => pickup location
					for (City city : current.pathTo(task.pickupCity)) {
						plan.appendMove(city);
					}
					plan.appendPickup(task);
					current = task.pickupCity;
				}else{ // action is Delivery
					// move: current city => delivery location
					for (City city : current.pathTo(task.deliveryCity)) {
						plan.appendMove(city);
					}
					plan.appendDelivery(task);
					current = task.deliveryCity;
				}
			}
			plans.add(plan);
		}
		return plans;
	}

	//////////////////////////////////////
	//              UTILS               //
	//////////////////////////////////////

	private int random(NodePD Aold) {
		ArrayList<Integer> vehicles = new ArrayList<Integer>();
		for(int v=0; v<Nv; v++) {
			if(Aold.nextAction(v+Na) != -1) vehicles.add(v);
		}
		int v = random.nextInt(vehicles.size());
		return vehicles.get(v);
	}

	public void print(String s){
		System.out.println(s);
	}
}
