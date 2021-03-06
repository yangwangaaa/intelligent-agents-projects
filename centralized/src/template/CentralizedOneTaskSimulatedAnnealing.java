
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
@SuppressWarnings("unused")
public class CentralizedOneTaskSimulatedAnnealing implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	private List<Vehicle> vehiclesList; 
	private Task[] tasks;

	private int Nt;
	private int Nv;

	private double p = 0; // probability used for localChoice probability of peaking best neighbor
	private int numIt = 100000;
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


		//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		NodeT bestSolution = SLS();
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

	private NodeT SLS() {
		NodeT A = selectInitialSolution();
		NodeT bestNodeT = A;

		A.getOValue(tasks, vehiclesList);
		A.print();

		int i = 0;
		while(i < numIt) { // TODO
			print("SLS iteration #" + i);
			NodeT Aold = A;
			ArrayList<NodeT> N = chooseNeighbours(A);
			A = localChoice(N, Aold, i); // only keep bestNodeT in chooseNeighbours : more efficient? = rename "chooseBestNeigbours"
			if(A.getOValue(tasks, vehiclesList) < bestNodeT.getOValue(tasks, vehiclesList)) bestNodeT = A;
			i++;
		}

		bestNodeT.getOValue(tasks, vehiclesList);
		bestNodeT.print();

		return bestNodeT;
	}

	// mouche
	private NodeT localChoice(ArrayList<NodeT> N, NodeT Aold, int iter) {
		if(N == null){
			return Aold;
		}else{

			double ran = Math.random();
			int ranIndex = (int) (ran*(N.size()-1));
			if(iter< 0.8* this.numIt){ 
				if( ran > p+(iter/this.numIt)){ print("truuuuuuuuuuue");
				print(iter);
				print(this.numIt);
				print(p+(iter/this.numIt));
					/*print("SIZE");
					print(N.size());
					print("ran");
					print(ran);
					print("index");
					print((int) (ranIndex));*/

					double delta = N.get(ranIndex).getOValue(tasks, vehiclesList) - Aold.getOValue(tasks, vehiclesList) ;
					if(delta < 0){ // new solution is bether
						print("SPEEEEEEEEEEEEEEEEEEEEEEECIAL");
						return N.get(ranIndex);
					}else{
						if(Math.random()>(delta/5)){
							print("TRRRRRRRRRRRRRRRRRRRRRRRS SPECIAL");
							return N.get(ranIndex);
						}
					}

				}		
			}
			NodeT bestNodeT = N.get(0);
			for(NodeT n : N){
				if(n.getOValue(tasks, vehiclesList)<=bestNodeT.getOValue(tasks, vehiclesList))
					bestNodeT = n;
			}
			print("NNNNNNNNNNNNNNNNNNNNNNN");
			return bestNodeT;
		}	
	}
	
	//////////////////////////////////////
	//         SLS : NEIGHBOURS         //
	//////////////////////////////////////

	// vladman
	private ArrayList<NodeT> chooseNeighbours(NodeT Aold) {
		ArrayList<NodeT> N = new ArrayList<NodeT>(); // TODO : ArrayList?
		int vi = random(Aold);

		// Applying the changing vehicle operator :
		for (int vj=0; vj<Nv; vj++) {
			if(vj!=vi) {
				int t = Aold.nextTask(vi+Nv);
				if(tasks[t].weight <= vehiclesList.get(vi).capacity()) { // no vehicle change if first task too heavy for all other vehicles
					NodeT A = changingVehicle(Aold, vi, vj);
					N.add(A);
					//A.print();
				}
			}
		}

		// Applying the changing task order operator :
		// compute the number of tasks of the vehicle
		int length = 0;
		int t = Aold.nextTask(vi+Nt); // current task in the list
		while(t!=-1) {
			t = Aold.nextTask(t);
			length++;
		}
		if(length>=2) {
			for (int tIdx1=0; tIdx1<length-1; tIdx1++) {
				for (int tIdx2=tIdx1+1; tIdx2<length; tIdx2++) {
					NodeT A = changingTaskOrder(Aold, vi, tIdx1, tIdx2);
					N.add(A);
					//A.print();
				}
			}
		}

		return N;
	}

	// vladman
	private NodeT changingVehicle(NodeT A, int v1, int v2) {
		NodeT A1 = A.clone();
		int t = A.nextTask(v1+Nt);

		A1.nextTask(v1+Nt, A1.nextTask(t));
		A1.nextTask(t, A1.nextTask(v2+Nt));
		A1.nextTask(v2+Nt, t);

		updateTime(A1, v1);
		updateTime(A1, v2);

		A1.setVehicle(t, v2);

		return A1;
	}

	// mouche
	private NodeT changingTaskOrder(NodeT A, int v, int aI1, int aI2) {
		NodeT A1 = A.clone();
		int tPre1 = v + Nt; // getNextTask(tPre1) = aI1
		int t1 = A1.nextTask(tPre1); // task1
		int count = 0;
		while(count < aI1){
			tPre1 = t1;
			t1 = A1.nextTask(t1);
			count++;
		}
		int tPost1 = A1.nextTask(t1); // the task delivered after t1
		int tPre2 = t1; // previous task of task2
		int t2 = A1.nextTask(tPre2); // task2
		count++;

		while(count < aI2){
			tPre2 = t2;
			t2 = A1.nextTask(t2);
			count++;
		}
		int tPost2 = A1.nextTask(t2);
		if(tPost1 == t2){
			// the task t2 is delivered immediately after t1
			A1.nextTask(tPre1, t2);
			A1.nextTask(t2, t1);
			A1.nextTask(t1, tPost2);
		}else{
			A1.nextTask(tPre1, t2);
			A1.nextTask(tPre2, t1);
			A1.nextTask(t2, tPost1);
			A1.nextTask(t1, tPost2);
		}
		updateTime(A1,v);
		return A1;
	}

	// mouche
	private void updateTime(NodeT A, int v) {
		int ti = A.nextTask(v+Nt);
		if(ti != -1){
			A.setTime(ti, 0); //Time starts at 0
			int tj = A.nextTask(ti);
			while(tj != -1){
				A.setTime(tj, A.getTime(ti) + 1);
				ti = tj;
				tj = A.nextTask(ti);
			}
		}
	}

	//////////////////////////////////////
	//           SLS : OTHERS           //
	//////////////////////////////////////

	// vladman
	private NodeT selectInitialSolution() {
		// use global variables: vehicles, tasks, ...
		NodeT initial = new NodeT(vehiclesList, tasks);
		int Nt = tasks.length;
		int Nv = vehiclesList.size();

		Vehicle biggestV = vehiclesList.get(0);
		for (Vehicle v : vehiclesList) {
			if(biggestV.capacity()<v.capacity()) biggestV = v;
		}

		int index = biggestV.id();
		initial.nextTask(index+Nt, 0);

		for (int i = 0; i<Nt; i++) {
			if(this.tasks[i].weight>biggestV.capacity()) {
				System.out.println("ERROR : One task is too heavy for the vehicles");
				return null;
			}

			int next;
			if(i==Nt-1) next = -1;
			else next = i+1;

			initial.nextTask(i, next);
		}

		for(int i=0; i<Nv; i++) {
			updateTime(initial, i);
		}

		for(int i=0; i<Nt; i++) {
			initial.setVehicle(i, index);
		}

		return initial;
	}



	// mouche

	private List<Plan> computeFinalPlan(NodeT n) {
		List<Plan> plans = new ArrayList<Plan>();
		for(int v = 0 ; v<Nv ; v++){ // for each vehicle
			City current = vehiclesList.get(v).getCurrentCity();
			Plan plan = new Plan(current);
			for(int t = Nt + v; n.nextTask(t) != -1 ; t = n.nextTask(t)){ // for each task of the vehicle //TODO 2*Nt
				Task task = tasks[n.nextTask(t)];				
				// move: current city => pickup location
				for (City city : current.pathTo(task.pickupCity)) {
					plan.appendMove(city);
				}

				plan.appendPickup(task);

				// move: pickup location => delivery location
				for (City city : task.path()) {
					plan.appendMove(city);
				}

				plan.appendDelivery(task);

				// set current city
				current = task.deliveryCity;
			}
			plans.add(plan);
		}
		return plans;
	}



	//////////////////////////////////////
	//              UTILS               //
	//////////////////////////////////////

	private int random(NodeT Aold) {
		ArrayList<Integer> vehicles = new ArrayList<Integer>();
		for(int v=0; v<Nv; v++) {
			if(Aold.nextTask(v+Nt) != -1) vehicles.add(v);
		}
		int v = random.nextInt(vehicles.size());
		return vehicles.get(v);
	}

	public void print(String s){
		System.out.println(s);
	}
	public void print(int i){
		System.out.println(i);
	}
	public void print(double i){
		System.out.println(i);
	}
}
