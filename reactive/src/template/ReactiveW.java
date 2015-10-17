package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class ReactiveW implements ReactiveBehavior {

	////////////////////////////////////////////////////////
	//													  //
	//					GLOBAL VARIABLES				  //
	//													  //
	////////////////////////////////////////////////////////

	private int numberOfCities;
	private int numberOfStates;
	private int numberOfActions;
	private ArrayList<Integer>[] S;
	private HashMap<Integer,Double>[] R;
	private HashMap<Integer,HashMap<Integer,Double>>[] TP;
	private double[][][] T;
	private double[] V;
	private int[] Best;
	private Double discount;
	private List<City> cities;
	private Agent agent;

	// ADDED CODE - this variable counts how many actions have passed so far
	int counterSteps = 0;


	////////////////////////////////////////////////////////
	//													  //
	//						ACTIONS						  //
	//													  //
	////////////////////////////////////////////////////////

	/**
	 * We compute the current state from available informations :
	 * - current city 
	 * - deliver city of the available task if it exists
	 * - available task weight
	 * - vehicle capacity
	 * After computing this state, we look for the best action to take in the best action table (= "Best")
	 */
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {		


		// This output gives information about the "goodness" of your agent (higher values are preferred)
		if ((counterSteps > 0)&&(counterSteps%100 == 0)) {
			System.out.println("discount = " + discount + "  The total profit after "+counterSteps+" steps is "+agent.getTotalProfit()+".");
			System.out.println("The profit per action after "+counterSteps+" steps is "+((double)agent.getTotalProfit() / counterSteps)+".");
		}

		counterSteps++;
		
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
	//						SETUP						  //
	//													  //
	////////////////////////////////////////////////////////
	
	/**
	 * Initialize the structures used in order to compute the strategy table
	 * by value iteration
	 */

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		System.out.println("!!!!!!!!!!! FINAL REACTIVEW AGENT !!!!!!!!!!!!");

		this.agent = agent;

		initVars(topology, agent);

		computeActionTable(topology, td, agent);

	}

	////////////////////////////////////////////////////////
	//													  //
	//					SETUP METHODS					  //
	//													  //
	////////////////////////////////////////////////////////

	/**
	 * Initialize the structures and launch the value iteration algorithm
	 * in order to build the strategy table used in the simulation to choose
	 * the action maximizing the expected profit.
	 */
	private void computeActionTable(Topology topology, TaskDistribution td, Agent agent) {

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

	/**
	 * Initialize principal variables
	 */
	private void initVars(Topology topology, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		
		System.out.println("DISCOUNT = " + discount);

		numberOfCities = topology.size();
		numberOfStates =  numberOfCities * numberOfCities;
		numberOfActions = numberOfCities+1;

		cities = topology.cities();
	}

	/**
	 * Initialize the State Table.
	 * The state table is a array of n*n states (where n is the number of cities)
	 * An agent in state "i" is an agent whose vehicle is in city i%n and where 
	 * there's an available task with city i/n as destination
	 * For each state, we keep an Arraylist of possible actions
	 * An action is a number ranging from 0 to n.
	 * The action "i" means moving on city i when i < n
	 * The action "i" means taking the task of the current city when i = n
	 */
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
			if(to!=from && td.weight(from, to) < agent.vehicles().get(0).capacity()) {  
				S[s].add(numberOfCities); // id of the 'pick up' action
			}
		}
	}

	/**
	 * Initialize the Reward Table.
	 * The reward table maintains the reward corresponding to each possible pair of state-action.
	 * It has the same size as the State table computed above.
	 * It's a list of Hashmaps, where each entry of the list corresponds to a state (from 0 to (n*n)-1)
	 * and each state maintains an Hashmap which contains (key,value) pairs. The key corresponds to the action 
	 * number and the value to the corresponding reward.
	 */
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
			if(to!=from && td.weight(from, to) < agent.vehicles().get(0).capacity()) { 
				double reward = td.reward(from, to) - from.distanceTo(to)*agent.vehicles().get(0).costPerKm();
				R[s].put(numberOfCities, reward);
			}
		}
	}

	/**
	 * Initialization of the probability transition table
	 * This probability transition table is exactly the same as the one described in the project statement
	 */
	private void initT(TaskDistribution td) {
		// T = new double[numberOfStates][numberOfActions][numberOfStates]; // all init to zeros
		TP = (HashMap<Integer,HashMap<Integer,Double>>[]) new HashMap[numberOfStates];
		
		for(int s = 0; s<numberOfStates; s++) {
			TP[s] = new HashMap<Integer,HashMap<Integer,Double>>();
			City to = cities.get(indexCityTo(s));
			for(int action : S[s]) {
				HashMap<Integer,Double> states = new HashMap<Integer,Double>();
				City dest = to;
				if (action!=numberOfCities) dest = cities.get(action);
				for(int c = 0; c<numberOfCities; c++) {
					int newState = c*numberOfCities+dest.id;
					if(c!=dest.id) {
						states.put(newState, td.probability(dest, cities.get(c)));
						// T[s][action][newState] = td.probability(dest, cities.get(c));
					}
					else {
						states.put(newState, td.probability(dest, null));
						// T[s][action][newState] = td.probability(dest, null);
					}
				}
				TP[s].put(action, states);
			}
		}
	}
	
	/**
	 * Initialization of the best policy table (Best) and its corresponding best expected reward table (V).
	 */
	private void initV() {
		V = new double[numberOfStates];
		Best = new int[numberOfStates];

		for(int v=0; v<V.length; v++) {
			V[v] = 1.0;
		}
	}
	
	
	/**
	 * Compute the best policy table using the Value Iteration algorithm.
	 */
	private void valueIteration() {
		boolean again = true;
		while(again) {
			again = false;
			for(int s = 0; s<S.length; s++) { // Loop on all states
				double Q, maxQ=Integer.MIN_VALUE;
				int bestAction = 0;
				for(int a : S[s]) { // Try to find best action
					Q = R[s].get(a);
					int to = a;
					if(a==numberOfCities) to = indexCityTo(s);
					for(int sp = to; sp<numberOfStates; sp+=numberOfCities) { // Expected future reward
						//Q += discount*T[s][a][sp]*V[sp];
						Q += discount*TP[s].get(a).get(sp)*V[sp];
					}
					if(Q>maxQ) { 
						maxQ = Q;
						bestAction = a;
					}
				}
				if(Math.abs(V[s]-maxQ)>0.001) again = true; //Stopping criteria
				V[s] = maxQ; //update V(S)
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
}
