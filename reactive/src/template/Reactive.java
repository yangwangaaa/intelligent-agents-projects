package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Reactive implements ReactiveBehavior {


	/**
	 * TODO:
	 * initial value of V?  V
	 * proba : voir si correspond  V
	 * un agent = plusieurs voitures !!! : rajouter un state par costPerKm  V
	 * 
	 * peut pas prendre tache si sup a weight V
	 * tenir compte de capacity-weight 
	 * optimiser T
	 */

	////////////////////////////////////////////////////////
	//													  //
	//					GLOBAL VARIABLES				  //
	//													  //
	////////////////////////////////////////////////////////

	private Random random;

	private int numberOfCities;
	private int numberOfStates;
	private int numberOfActions;
	private ArrayList<Integer>[] S;
	private HashMap<Integer,Double>[] R;
	private double[][][] T;
	private double[] V;
	private int[] Best;
	private Double discount;
	private List<City> cities;
	private Topology topology;
	private TaskDistribution td;
	private Agent agent;
	private double val = 0.0;

	// ADDED CODE - this variable counts how many actions have passed so far
	int counterSteps = 0;


	////////////////////////////////////////////////////////
	//													  //
	//						SETUP						  //
	//													  //
	////////////////////////////////////////////////////////

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		System.out.println("!!!!!!!!!!! REACTIVE AGENT !!!!!!!!!!!!");

		this.agent = agent;
		this.td = td;
		this.topology = topology;


		initVars(topology, agent);

		computeActionTable(topology, td, agent);

	}

	////////////////////////////////////////////////////////
	//													  //
	//						ACTIONS						  //
	//													  //
	////////////////////////////////////////////////////////

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {		


		// ADDED CODE - this output gives information about the "goodness" of your agent (higher values are preferred)
		if ((counterSteps > 0)&&(counterSteps%100 == 0)) {
			System.out.println("The total profit after "+counterSteps+" steps is "+agent.getTotalProfit()+".");
			System.out.println("The profit per action after "+counterSteps+" steps is "+((double)agent.getTotalProfit() / counterSteps)+".");
		}

		/*
		if ((counterSteps > 0)&&(counterSteps%200 == 0)) {
			print("DISCOUNT = " + discount);
			computeActionTable(topology, td, agent);
		}
		 */

		counterSteps++;
		// END OF ADDED CODE

		City from = vehicle.getCurrentCity();
		City to = null;
		if(availableTask!=null && availableTask.weight<vehicle.capacity()) to = availableTask.deliveryCity;

		int state = from.id*numberOfCities + from.id;
		if (to!=null) state = to.id*numberOfCities + from.id;

		int bestAction = Best[state];

		if(bestAction == numberOfCities) return new Pickup(availableTask);
		else return new Move(cities.get(bestAction));
	}

	////////////////////////////////////////////////////////
	//													  //
	//					SETUP METHODS					  //
	//													  //
	////////////////////////////////////////////////////////

	private void computeActionTable(Topology topology, TaskDistribution td, Agent agent) {

		//discount = Math.min(0.84+val, 1.0);
		//val = val+0.01;

		initS(td, agent);
		//print(S);

		initR(td, agent);
		//print(R);

		initT(td);
		//print(T);

		initV();
		//print(V);


		valueIteration();

		print(V);
		print(Best);
	}

	private void initVars(Topology topology, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();

		numberOfCities = topology.size();
		numberOfStates =  numberOfCities * (numberOfCities);
		numberOfActions = numberOfCities+1;

		cities = topology.cities();
	}

	private void initS(TaskDistribution td, Agent agent) {
		S = (ArrayList<Integer>[]) new ArrayList[numberOfStates];

		for(int s = 0; s<numberOfStates; s++) {
			S[s] = new ArrayList<Integer>();			

			City from = cities.get(indexCityFrom(s));
			City to = cities.get(indexCityTo(s));

			for(City neighbor : from) {
				S[s].add(neighbor.id); // id of the city on which to move
			}

			// if to==from, cannot do the 'pick up' action in this state
			if(to!=from) { 
				S[s].add(numberOfCities); // id of the 'pick up' action
			}
		}
	}

	private void initR(TaskDistribution td, Agent agent) {
		R = (HashMap<Integer,Double>[]) new HashMap[numberOfStates];

		for(int s = 0; s<numberOfStates; s++) {
			R[s] = new HashMap<Integer,Double>();

			City from = cities.get(indexCityFrom(s));
			City to = cities.get(indexCityTo(s));


			for(City neighbor : from) {
				R[s].put(neighbor.id, -from.distanceTo(neighbor)*agent.vehicles().get(0).costPerKm()); // id of the city on which to move
			}

			// if to==from, cannot do the 'pick up' action in this state
			if(to!=from) { 
				double reward = td.reward(from, to) - from.distanceTo(to)*agent.vehicles().get(0).costPerKm();
				R[s].put(numberOfCities, reward);
			}
		}
	}

	private void initT(TaskDistribution td) {
		T = new double[numberOfStates][numberOfActions][numberOfStates]; // all init to zeros
		for(int s = 0; s<numberOfStates; s++) {
			City to = cities.get(indexCityTo(s));
			for(int action : S[s]) {
				City dest = to;
				if (action!=numberOfCities) dest = cities.get(action);
				for(int c = 0; c<numberOfCities; c++) {
					int newState = c*numberOfCities+dest.id;
					if(c!=dest.id) T[s][action][newState] = td.probability(dest, cities.get(c));
					else T[s][action][newState] = td.probability(dest, null);
				}
			}
		}
	}

	private void initV() {
		V = new double[numberOfStates];
		Best = new int[numberOfStates];

		for(int v=0; v<V.length; v++) {
			V[v] = 1.0;
		}
	}

	private void valueIteration() {
		boolean again = true;
		int count = 0;
		while(again) {
			count++;
			//System.out.println(count);
			again = false;
			for(int s = 0; s<S.length; s++) {
				double Q, maxQ=Integer.MIN_VALUE;
				int bestAction = 0;
				for(int a : S[s]) {
					Q = R[s].get(a);
					for(int sp = 0; sp<numberOfStates; sp++) { // optimiser
						Q += discount*T[s][a][sp]*V[sp];
					}
					if(Q>maxQ) {
						maxQ = Q;
						bestAction = a;
					}
				}
				if(Math.abs(V[s]-maxQ)>10) again = true;
				V[s] = maxQ;
				Best[s] = bestAction;
			}
		}

		print("Terminated");
	}


	////////////////////////////////////////////////////////
	//													  //
	//						UTILS						  //
	//													  //
	////////////////////////////////////////////////////////

	private void print(String s) {
		System.out.println(s);
	}

	private void print(ArrayList<Integer>[] S) {
		for (int c=0; c<cities.size(); c++) {

			System.out.println(cities.get(c).name + " : " + cities.get(c).id + " : " + c);
		}
		System.out.println("Number of cities = " + numberOfCities);
		System.out.println("Number of actions = " + numberOfActions);
		System.out.println("STATE ARRAY ; length=" + numberOfStates);

		for (int s=0; s<S.length; s++) {
			System.out.print("S=" + s + "; from=" + indexCityFrom(s) + ", to=" + indexCityTo(s) + "; a=");
			for (int a : S[s]) {
				System.out.print(a + ",");
			}
			System.out.println("");
		}
	}

	private void print(HashMap<Integer,Double>[] R) {
		System.out.println("REWARD ARRAY :");
		for (int r=0; r<R.length; r++) {
			System.out.print("S=" + r + "; from=" + indexCityFrom(r) + ", to=" + indexCityTo(r) + "; ");
			for (int a : S[r]) {
				System.out.print("(a=" + a + ", r=" + R[r].get(a) + "); ");
			}
			System.out.println("");
		}
	}

	private void print(double[][][] T) {
		System.out.println("T ARRAY :");
		for (int s=0; s<numberOfStates; s++) {
			System.out.println("State=" + s + "; from=" + indexCityFrom(s) + ", to=" + indexCityTo(s));
			for (int a=0; a<numberOfActions; a++) {
				System.out.print("action=" + a + " S=");
				for (int sp=0; sp<numberOfStates; sp++) {
					System.out.print("(" + indexCityFrom(sp) + "->" + indexCityTo(sp) + ";" + T[s][a][sp] + "),");
				}
				System.out.println("");
			}
			System.out.println("");
		}
	}

	private void print(double[] V) {
		System.out.println("V ARRAY :");
		for(double v : V) {
			System.out.print(v + " ");
		}
		System.out.println("");
	}

	private void print(int[] Best) {
		System.out.println("BEST ARRAY :");
		for(int b : Best) {
			System.out.print(b + " ");
		}
		System.out.println("");
	}


	private int indexCityFrom(int state) {
		return state%numberOfCities;
	}

	private int indexCityTo(int state) {
		return state/numberOfCities;
	}



	////////////////////////////////////////////////////////
	//													  //
	//						  OLD 						  //
	//													  //
	////////////////////////////////////////////////////////

	private void initStructures(Topology topology, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		this.random = new Random();

		// Init structures :

		numberOfCities = topology.size();
		numberOfStates =  numberOfCities * (numberOfCities+1);
		numberOfActions = numberOfCities+1;
		S = (ArrayList<Integer>[]) new ArrayList[numberOfStates];
		R = (HashMap<Integer,Double>[]) new HashMap[numberOfStates];
		T = new double[numberOfStates][numberOfActions][numberOfStates]; // all init to zeros
		V = new double[numberOfStates];
		Best = new int[numberOfStates];

		cities = topology.cities();
	}

	private void fillStructures(Topology topology, TaskDistribution td) {
		for(int s = 0; s<numberOfStates; s++) {
			S[s] = new ArrayList<Integer>();
			R[s] = new HashMap<Integer,Double>();


			List<City> cities = topology.cities();
			City from = cities.get(indexCityFrom(s));
			City to = null;
			if(s<numberOfCities*numberOfCities) to = cities.get(indexCityTo(s));

			for(City neighbor : from) {
				S[s].add(neighbor.id); // id of the city on which to move
				R[s].put(neighbor.id, -from.distanceTo(neighbor));

				double probaNoTask = 1.0;
				for(int sp = neighbor.id; sp<numberOfStates; sp+=numberOfCities) {
					if(sp<numberOfCities*numberOfCities) { 
						City toto = cities.get(indexCityTo(sp));
						double proba = td.probability(neighbor, toto);
						probaNoTask *= (1-proba);
						T[s][neighbor.id][sp] = proba;
					}
					else {
						T[s][neighbor.id][sp] = probaNoTask; // FALSE : mettre null
					}
				}
			}

			// if to==null, cannot do the 'pick up' action in this state
			if(to!=null) { 
				S[s].add(numberOfCities); // id of the 'pick up' action

				double reward = td.reward(from, to); // ????
				R[s].put(numberOfCities, reward);
			}
		}
	}
}
