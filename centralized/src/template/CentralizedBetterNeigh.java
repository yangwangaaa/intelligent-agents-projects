package template;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
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
 * TODO :
 * 3local 
 * chaque vehicle avec bcp iterations
 * chaque vehicle avec bcp d'iterations
 * 
 * benchmark
 * report
 */
@SuppressWarnings("unused")
public class CentralizedBetterNeigh implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;
	private long time_start;

	private List<Vehicle> vehiclesList; 
	private Task[] tasks;

	private int Nt;
	private int Nv;
	private int Na;

	private double p = 0.5; // probability used for localChoice
	private int numIt = 10000000;
	private Random random;
	private int n = 5;
	private int firstV = 1;
	private int lastV = 2;

	private NodePD bestGlobal = null;

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
		for(Task t : tasks) {
			print("" + t);
		}
		time_start = System.currentTimeMillis();
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

		int totalCost = 0;
		int v = 0;
		for (Plan plan : plans) {
			totalCost+=plan.totalDistance();
			v++;
		}

		print("FINAL COST = " + totalCost);
		return plans;
	}


	//////////////////////////////////////
	//               SLS                //
	//////////////////////////////////////

	private NodePD SLS() {
		for(int v = firstV; v<lastV; v++) {
			print("SLS for #" + v);
			NodePD A = selectInitialSolution(v);
			if(bestGlobal==null) bestGlobal = A;
			A.getOValue(tasks, vehiclesList);
			A.print();

			NodePD localBest = A;
			int i = 0;
			while(i < numIt) {
				long duration = System.currentTimeMillis() - time_start;
				if(duration>0.95*timeout_plan) {
					print("!!!!!!!!!!!!! TIMEOUT, WE SHOULD RETURN FINAL RESULT !!!!!!!!!!!!");
					return bestGlobal;
				}
				if(i%10000==0) print("SLS while #" + i + " ; DURATION = " + duration/1000 + "s");
				NodePD Aold = A;
				PriorityQueue<NodePD> N = chooseNeighbours(A);
				A = localChoice3(N, Aold); 
				//print("BEST CHOOSEN AT IT " + i);
				//A.print();
				if(A.getOValue(tasks, vehiclesList) < localBest.getOValue(tasks, vehiclesList)) localBest = A;
				i++;
			}

			print("BEST CHOOSEN AT V = " + v);
			localBest.print();
			if(localBest.getOValue(tasks, vehiclesList) < bestGlobal.getOValue(tasks, vehiclesList)) bestGlobal = localBest;
		}
		return bestGlobal;
	}
	
	// mouche
	private NodePD localChoice3(PriorityQueue<NodePD> N, NodePD Aold) {
		if(N.size()==0){
			return Aold;
		}else{
			NodePD bestNodePD = N.poll();
			for(NodePD n : N){
				if(n.getOValue(tasks, vehiclesList)<=bestNodePD.getOValue(tasks, vehiclesList))
					bestNodePD = n;
			}
			
			double pp = Math.random();

			if(Aold.getOValue() > bestNodePD.getOValue()){
				if (pp<= p) {
					return bestNodePD;
				}
				else {
					return Aold;
				}
			}
			else {
				if (pp> p) {
					return bestNodePD;
				}
				else {
					return Aold;
				}
			}
		}	
	}

	// mouche
	private NodePD localChoice2(PriorityQueue<NodePD> N, NodePD Aold) {
		if(N.size()==0){
			return Aold;
		}else{
			
			NodePD bestNodePD = N.poll();
			int r = random.nextInt(N.size());
			int k = 0;
			while(k<r) {
				bestNodePD = N.poll();
				k++;
			}

			double pp = Math.random();

			if(Aold.getOValue() > bestNodePD.getOValue()){
				if (pp<= p) {
					return bestNodePD;
				}
				else {
					return Aold;
				}
			}
			else {
				if (pp> p) {
					return bestNodePD;
				}
				else {
					return Aold;
				}
			}
		}	
	}

	private NodePD localChoice1(PriorityQueue<NodePD> N, NodePD Aold) {
		if(N.size()==0){
			return Aold;
		}else{
			NodePD bestNodePD = N.poll();
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
	
	

	//////////////////////////////////////
	//         SLS : NEIGHBOURS         //
	//////////////////////////////////////

	// vladman
	private PriorityQueue<NodePD> chooseNeighbours(NodePD Aold) {
		ArrayList<NodePD> N = new ArrayList<NodePD>();
		PriorityQueue<NodePD> Pr = new PriorityQueue<NodePD>();
		int vi = random(Aold);

		// compute the number of tasks of the vehicle
		int length = 0;
		int t = Aold.nextAction(vi+Na); // current task in the list
		while(t!=-1) {
			t = Aold.nextAction(t);
			length++;
		}

		// Applying the changing vehicle operator :
		//print("CHANGING VEHICLE");
		for (int tIdx1=0; tIdx1<length/2; tIdx1++) {
			for (int vj=0; vj<Nv; vj++) {
				if(vj!=vi) {
					int a = Aold.nextAction(vi+Na);
					if(tasks[a].weight <= vehiclesList.get(vi).capacity()) { // no vehicle change if first task too heavy for all other vehicles
						NodePD A = changingVehicle(Aold, vi, vj, tIdx1, 0, 0);
						if(A!=null) {
							if(A.getOValue(tasks, vehiclesList) < bestGlobal.getOValue(tasks, vehiclesList)) bestGlobal = A;
							add(Pr, A);
							//A.print();
						}
					}
				}
			}
		}

		// Applying the changing task order operator :
		//print("CHANGING TASK ORDER");
		if(length>=2) {
			for (int tIdx1=0; tIdx1<length-1; tIdx1++) {
				for (int tIdx2=tIdx1+1; tIdx2<length; tIdx2++) {
					NodePD A = changingTaskOrder(Aold, vi, tIdx1, tIdx2);
					if(A!=null) {
						if(A.getOValue(tasks, vehiclesList) < bestGlobal.getOValue(tasks, vehiclesList)) bestGlobal = A;
						add(Pr, A);
						//A.print();
					}
				}
			}
		}

		return Pr;
	}

	// vladman
	private NodePD changingVehicle(NodePD A, int v1, int v2, int aIndex, int pIndex, int dIndex) {
		NodePD A1 = A.clone();

		int p = A1.nextAction(v1 + Na); // task1 P
		int count = 0;
		while(count < aIndex){
			p = A1.nextAction(p);
			if(p<Nt) {
				count++;
			}
		}

		if(tasks[p].weight>vehiclesList.get(v2).capacity()) {
			return null;
		}

		int d = p+Nt; // D

		int dNext = A1.nextAction(d);
		int dPrevious = A1.previousAction(d);

		int v2Next = A1.nextAction(v2+Na);

		int pPrevious = A1.previousAction(p);
		int pNext = A1.nextAction(p);

		Boolean following = A1.nextAction(p)==d;

		// update nextAction and previousAction v1
		if(!following) {
			A1.nextAction(pPrevious, pNext);
			A1.previousAction(pNext, pPrevious);

			A1.nextAction(dPrevious, dNext);
			if(dNext!=-1) A1.previousAction(dNext, dPrevious);
		}
		else {
			A1.nextAction(pPrevious, dNext);
			if(dNext!=-1) A1.previousAction(dNext, pPrevious);
		}

		
		
//		// compute the number of tasks of the vehicle
//		int length = 0;
//		int t = Aold.nextAction(vi+Na); // current task in the list
//		while(length!=pIndex) {
//			t = Aold.nextAction(t);
//			length++;
//		}
		
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
		A1.setLoad(p, tasks[p].weight);
		A1.setLoad(d, 0);

		if(!following) { // remove weight of task pd for actions that were between p and d
			int a = pNext;
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
		
		int delta;

		// Checks if we can exchange the order of a1 and a2 then update the time and load variables.
		int time1 = A1.getTime(a1);
		int capacity = vehiclesList.get(A1.getVehicle(a1)).capacity();
		//D stands for delivery, P stands for pickup.
		if(a1>=Nt){// a1 is delivery D
			if(a2 >= Nt){ // D1....D2 to D2....D1

				if(time1 <= A1.getTime(a2-Nt)) //D2 before P2
					return null;
				delta = tasks[a1-Nt].weight - tasks[a2-Nt].weight;
				if(delta>0){
					for(int i = a1; i!= a2; i = A1.nextAction(i)){
						if(A1.getLoad(i) + delta > capacity)
							return null;
					}
				}
				int a1Load = A1.getLoad(a1);
				int a2Load = A1.getLoad(a2);
				A1.setLoad(a2, a1Load + delta);
				A1.setLoad(a1, a2Load);

			}else{ //D1....P2 to P2....D1
				

				delta = tasks[a1-Nt].weight + tasks[a2].weight;
				for(int i = a1; i!= a2; i = A1.nextAction(i)){
					if(A1.getLoad(i) + delta > capacity)
						return null;
				}
				int a1Load = A1.getLoad(a1);
				int a2Load = A1.getLoad(a2);
				A1.setLoad(a2, a1Load + delta);
				A1.setLoad(a1, a2Load);
			}
		}
		else{// a1 is pick up
			if(a2 >= Nt){ // P1....D2 to D2....P1

				if(time1 <= A1.getTime(a2-Nt) ||        //D2 before P2 or
						A1.getTime(a2) >= A1.getTime(a1+Nt) )//P1 after D1
					return null;
				delta = -tasks[a1].weight - tasks[a2-Nt].weight; //delta is negative load is ok //TODO le -?
				
				int a1Load = A1.getLoad(a1);
				int a2Load = A1.getLoad(a2);
				A1.setLoad(a2, a1Load + delta);
				A1.setLoad(a1, a2Load);

			}else{ //P1....P2 to P2....P1
				if(A1.getTime(a2) >= A1.getTime(a1+Nt)) //P1 after D1
					return null;
				delta = tasks[a2].weight - tasks[a1].weight;
				if(delta>0){
					for(int i = a1; i!= a2; i = A1.nextAction(i)){
						if(A1.getLoad(i) + delta > capacity)
							return null;
					}
				}
				int a1Load = A1.getLoad(a1);
				int a2Load = A1.getLoad(a2);
				A1.setLoad(a2, a1Load + delta);
				A1.setLoad(a1, a2Load);
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
		
		updateTimeAndLoad(A1, a1, a2, time1, delta);
		
		return A1;
	}

	//mouche
	private void updateTimeAndLoad(NodePD A1, int a1, int a2, int time1, int delta ){
		//Update time
		A1.setTime(a1, A1.getTime(a2));
		A1.setTime(a2, time1);
		//Update Load
		for(int i = A1.nextAction(a2); i!= a1; i = A1.nextAction(i)){
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
	private NodePD selectInitialSolution(int index) { // 
		// use global variables: vehicles, tasks, ...
		NodePD initial = new NodePD(vehiclesList, tasks);


		// find vehicle with biggest capacity
		Vehicle biggestV = vehiclesList.get(0);
		for (Vehicle v : vehiclesList) {
			if(biggestV.capacity()<v.capacity()) biggestV = v;
		}

		int[] lastAction = new int[Nv];
		Arrays.fill(lastAction, -1);

		int biggestI = biggestV.id();
		
		for (int i = 0; i<Nt; i++) { // for all task
			if(this.tasks[i].weight>biggestV.capacity()) { // Checks if no task is too heavy
				System.out.println("ERROR : One task is too heavy for the vehicles");
				return null;
			}

			int v = index;
			if(index>=vehiclesList.size()) v = random.nextInt(vehiclesList.size()); // In this case we pick a random vehicle
			if(this.tasks[i].weight>vehiclesList.get(v).capacity()) v = biggestI; 

			if(lastAction[v]==-1) {
				initial.nextAction(v+Na, i);
				initial.previousAction(i, v+Na);
				initial.previousAction(v+Na, -1);
			}
			else {
				initial.nextAction(lastAction[v], i);
				initial.previousAction(i, lastAction[v]);
			}
			initial.nextAction(i, Nt+i);
			initial.nextAction(Nt+i, -1);
			initial.previousAction(Nt+i, i);

			lastAction[v] = Nt+i;

			// load :
			initial.setLoad(i, tasks[i].weight);
			initial.setLoad(Nt+i, 0);

			// vehicle
			initial.setVehicle(i, v);
			initial.setVehicle(Nt+i, v);
		}


		for(int i=0; i<Nv; i++) {
			updateTime(initial, i);
		}

		return initial;
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

	private void add(PriorityQueue<NodePD> P, NodePD A) {
		if(P.size() <= n) {
			
			P.add(A);
		}
		else if (A.getOValue()<P.peek().getOValue()){
			P.poll();
			P.add(A);
		}
	}

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
