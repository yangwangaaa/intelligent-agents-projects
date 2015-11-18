package sls;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import sls.NodePD;
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
public class SLS {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_plan;
	private long time_start;

	private List<Vehicle> vehiclesList; 
	private Task[] tasks;

	private int Nt;
	private int Nv;
	private int Na;

	private double p = 0.5; // probability used for localChoice
	private int numIt = 5000;
	private Random random;
	private int n = 5;
	private int firstV = 7;
	private int lastV = 10;

	private NodePD bestGlobal = null;

	//////////////////////////////////////
	//            CONSTRUCTOR           //
	//////////////////////////////////////

	public SLS(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;


		random = new Random();
	}

	//////////////////////////////////////
	//               SLS                //
	//////////////////////////////////////

	public NodePD RunSLS(List<Vehicle> vehicles, Task[] tasksSet, long timeout_plan) {
		
		// TODO handle bestSolutions
		this.time_start = System.currentTimeMillis();
		this.timeout_plan = timeout_plan;
		
		this.vehiclesList = vehicles;
		this.tasks = tasksSet;
		this.Nt = this.tasks.length;
		this.Nv = vehiclesList.size();
		this.Na = 2*Nt;
		bestGlobal = null;
		
		for(int v = firstV; v<lastV; v++) {
			//print("SLS for #" + v);
			NodePD A = selectInitialSolution(v);
			if(bestGlobal==null) bestGlobal = A;
			//A.getOValue(tasks, vehiclesList);
			//A.print();

			NodePD localBest = A;
			int i = 0;
			while(i < numIt) {
				long duration = System.currentTimeMillis() - time_start;
				if(duration>0.95*timeout_plan) {
					//print("!!!!!!!!!!!!! TIMEOUT, WE SHOULD RETURN FINAL RESULT !!!!!!!!!!!!");
					return bestGlobal;
				}
				NodePD Aold = A;
				ArrayList<NodePD> N = chooseNeighbours(A);
				A = localChoice4(N, Aold); 
				if(i%100==0) { 
					//print("SLS while #" + i + " ; DURATION = " + duration/1000 + "s");
					//print("BEST CHOOSEN AT IT " + i);
					//A.print();
				}
				if(A.getOValue(tasks, vehiclesList) < localBest.getOValue(tasks, vehiclesList)) localBest = A;
				i++;
			}

			//print("BEST CHOOSEN AT V = " + v);
			//localBest.print();
			if(localBest.getOValue(tasks, vehiclesList) < bestGlobal.getOValue(tasks, vehiclesList)) bestGlobal = localBest;
		}
		return bestGlobal;
	}
	
	
	public NodePD RunSLS(List<Vehicle> vehicles, Task[] tasksSet, long timeout_plan, NodePD bestSolution) {
		
		// TODO handle bestSolutions
		this.time_start = System.currentTimeMillis();
		this.timeout_plan = timeout_plan;
		
		this.vehiclesList = vehicles;
		this.tasks = tasksSet;
		this.Nt = this.tasks.length;
		this.Nv = vehiclesList.size();
		this.Na = 2*Nt;
		bestGlobal = null;
		
		for(int v = firstV; v<lastV; v++) {
			//print("SLS for #" + v);
			NodePD A = selectInitialSolution(v);
			if(bestGlobal==null) bestGlobal = A;
			//A.getOValue(tasks, vehiclesList);
			//A.print();

			NodePD localBest = A;
			int i = 0;
			while(i < numIt) {
				long duration = System.currentTimeMillis() - time_start;
				if(duration>0.95*timeout_plan) {
					//print("!!!!!!!!!!!!! TIMEOUT, WE SHOULD RETURN FINAL RESULT !!!!!!!!!!!!");
					return bestGlobal;
				}
				NodePD Aold = A;
				ArrayList<NodePD> N = chooseNeighbours(A);
				A = localChoice4(N, Aold); 
				if(i%100==0) { 
					//print("SLS while #" + i + " ; DURATION = " + duration/1000 + "s");
					//print("BEST CHOOSEN AT IT " + i);
					//A.print();
				}
				if(A.getOValue(tasks, vehiclesList) < localBest.getOValue(tasks, vehiclesList)) localBest = A;
				i++;
			}

			//print("BEST CHOOSEN AT V = " + v);
			//localBest.print();
			if(localBest.getOValue(tasks, vehiclesList) < bestGlobal.getOValue(tasks, vehiclesList)) bestGlobal = localBest;
		}
		return bestGlobal;
	}


	private NodePD localChoice4(ArrayList<NodePD> N, NodePD Aold) {
		if(N.size()==0){
			return Aold;
		}else{
			N.add(Aold);
			NodePD bestNodePD = N.get(0);
			for(NodePD n : N){
				if(n.getOValue(tasks, vehiclesList)<=bestNodePD.getOValue(tasks, vehiclesList))
					bestNodePD = n;
			}
			if(Math.random()<= 0.7){
				return bestNodePD;
			}
			else {
				return N.get(random.nextInt(N.size()));
			}
		}	
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
	private ArrayList<NodePD> chooseNeighbours(NodePD Aold) {
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
		for (int p1=0; p1<length/2; p1++) {
			for (int vj=0; vj<Nv; vj++) {
				if(vj!=vi) {
					int lv2 = 0;
					t = Aold.nextAction(vj+Na); // current task in the list
					while(t!=-1) {
						t = Aold.nextAction(t);
						lv2++;
					}
					for (int tIdx1=0; tIdx1<lv2+1; tIdx1++) {
						for (int tIdx2=tIdx1+1; tIdx2<lv2+2; tIdx2++) {
							int a = Aold.nextAction(vi+Na);
							if(tasks[a].weight <= vehiclesList.get(vi).capacity()) { // no vehicle change if first task too heavy for all other vehicles
								NodePD A = changingVehicle(Aold, vi, vj, p1, tIdx1, tIdx2);
								if(A!=null) {
									if(A.getOValue(tasks, vehiclesList) < bestGlobal.getOValue(tasks, vehiclesList)) bestGlobal = A;
									//add(Pr, A);
									N.add(A);
									//A.print();
								}
							}
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
						//add(Pr, A);
						N.add(A);
						//A.print();
					}
				}
			}
		}

		return N;
	}

	// vladman
	private NodePD changingVehicle(NodePD A, int v1, int v2, int aIndex, int pIndex, int dIndex) {
		NodePD A1 = A.clone();

		int p = A1.nextAction(v1 + Na); // task1
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

		int d = p+Nt;

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


		// update nextAction and previousAction v2 P
		int length = 0;
		int last = v2+Na;
		int posP = A1.nextAction(v2+Na);
		while(length!=pIndex) {
			last = posP;
			posP = A1.nextAction(posP);
			length++;
		}
		if(posP!=-1) {
			int previousPosP = A1.previousAction(posP);

			A1.nextAction(previousPosP, p);
			A1.nextAction(p, posP);

			A1.previousAction(p, previousPosP);
			A1.previousAction(posP, p);


			// update nextAction and previousAction v2 D
			length = 0;
			int posD = A1.nextAction(v2+Na); 
			int last2 = v2+Na;
			while(length!=dIndex) {
				last2 = posD;
				posD = A1.nextAction(posD);
				length++;
			}
			if(posD!=-1) {
				int previousPosD = A1.previousAction(posD);

				A1.nextAction(previousPosD, d);
				A1.nextAction(d, posD);

				A1.previousAction(d, previousPosD);
				A1.previousAction(posD, d);
			}
			else {
				A1.nextAction(last2, d);
				A1.previousAction(d, last2);

				A1.nextAction(d, -1);
			}
		}
		else {
			A1.nextAction(last, p);
			A1.previousAction(p, last);

			A1.nextAction(p, d);
			A1.previousAction(d, p);

			A1.nextAction(d, -1);
		}


		// update time v1 and v2
		updateTime(A1, v1);
		updateTime(A1, v2);

		// update vehicle of p and d
		A1.setVehicle(p, v2);
		A1.setVehicle(d, v2);

		// update load v1
		if(!following) { // remove weight of task pd for actions that were between p and d
			int a = pNext;
			while(a!=dNext) {
				A1.setLoad(a, A1.getLoad(a)-tasks[p].weight);
				a = A1.nextAction(a);
			}
		}

		// update load v2
		int a = A1.nextAction(v2+Na); 
		int b = -1;
		while(a!=-1) {
			int loadB = 0;
			if(b!=-1) loadB = A1.getLoad(b);
			int loadA;
			if(a<Nt) loadA = tasks[a].weight;
			else loadA = -tasks[a-Nt].weight;


			if(loadB+loadA > vehiclesList.get(v2).capacity()) {
				return null;
			}

			A1.setLoad(a, loadB+loadA);

			b = a;
			a = A1.nextAction(a);
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
				delta = -tasks[a1].weight - tasks[a2-Nt].weight; //delta is negative load is ok

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


		Vehicle biggestV = vehiclesList.get(0);
		for (Vehicle v : vehiclesList) {
			if(biggestV.capacity()<v.capacity()) biggestV = v;
		}

		int[] lastAction = new int[Nv];
		Arrays.fill(lastAction, -1);

		int biggestI = biggestV.id();

		for (int i = 0; i<Nt; i++) {
			if(this.tasks[i].weight>biggestV.capacity()) {
				System.out.println("ERROR : One task is too heavy for the vehicles");
				return null;
			}

			int v = index;
			if(index>=vehiclesList.size()) v = random.nextInt(vehiclesList.size()); 
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
	public List<Plan> computeFinalPlan(NodePD n) {
		List<Plan> plans = new ArrayList<Plan>();
		for(int v = 0 ; v<Nv ; v++){ // for each vehicle
			City current = vehiclesList.get(v).getCurrentCity();
			Plan plan = new Plan(current);
			if(n!=null) {
				for(int t = Na + v; n.nextAction(t) != -1 ; t = n.nextAction(t)){ // for each task of the vehicle
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
