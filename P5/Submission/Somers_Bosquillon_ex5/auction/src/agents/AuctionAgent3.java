package agents;

//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import other.MyVehicle;
import sls.NodePD;
import sls.SLS;
import astar.Astar;

/**
 * QUESTION :
 * do we know the map? one of the given maps? don't know
 * total capacity always the same? +- meme capacity sum
 * cb de tasks en moyenne, upper bound lower bound? cas particuliers avec genre 1 seule task? ask other TA
 * cb de vehicles en moyenne? +- meme nombre
 * test agents fournis?
 * comment trouver agent size?
 * 
 * TODO : (see also SLS)
 * test all collected info
 * estimate others marginal cost : 
 * - generate i-1, i and finally i+1 vehicles, with same total capacity, average on 3 random start city
 */
@SuppressWarnings("unused")
public class AuctionAgent3 implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private MyVehicle biggestVehicle;

	private long timeout_setup;
	private long timeout_plan;
	private long time_start;
	private long timeout_bid;

	private List<MyVehicle> vehiclesList; 
	private Task[] tasks;

	private int Nv;

	private double p = 0.5; // probability used for localChoice
	private int numIt = 5000;
	private int n = 5;
	private Random rand;
	private int firstV = 7;
	private int lastV = 10;


	private int id;
	private int meanCapa;
	private int lastTask = -1;
	private long[] totalReward;
	private int[] meanBid;
	private int numA = 2;
	private NodePD firstSolution;
	private ArrayList<Long>[] allBids = null;
	private ArrayList<Double>[] lastSolutionsValue = null;
	private ArrayList<Double>[] bestSolutionsValue = null;
	private ArrayList<Double>[] marginalCosts = null;
	private ArrayList<Integer>[] tasksResult = null;
	private ArrayList<Task>[] listTasks = null;

	private SLS sls;
	private Astar astar;

	//////////////////////////////////////
	//              MAIN                //
	//////////////////////////////////////

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehiclesList = MyVehicle.transform(agent.vehicles());
		this.Nv = vehiclesList.size();
		this.biggestVehicle = vehiclesList.get(0);
		for (MyVehicle v : vehiclesList) {
			if(v.capacity() > biggestVehicle.capacity()) biggestVehicle = v;
			meanCapa = meanCapa + v.capacity()/Nv;
		}


		long seed = -9019554669489983951L * biggestVehicle.hashCode() * agent.id();
		this.random = new Random(seed);

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		//timeout_bid=0.95*timeout_bid;
		// int numA = ls.get(LogistSettings.SizeKey.NUMBER_OF_AGENTS);
		// int numT = ls.get(LogistSettings.SizeKey.NUMBER_OF_TASKS);
		//print("number of tasks = " + numT + " and number of agents = " + numA);

		rand = new Random();

		id = agent.id();

		sls = new SLS(topology, distribution, agent);
		astar = new Astar(topology, distribution, agent);

		tasksResult = (ArrayList<Integer>[]) new ArrayList[numA];
		marginalCosts = (ArrayList<Double>[]) new ArrayList[numA];
		lastSolutionsValue = (ArrayList<Double>[]) new ArrayList[numA];
		bestSolutionsValue = (ArrayList<Double>[]) new ArrayList[numA];
		totalReward = new long[numA];
		listTasks = (ArrayList<Task>[]) new ArrayList[numA];
		allBids = (ArrayList<Long>[]) new ArrayList[numA];
		for(int i=0; i<numA; i++) {
			tasksResult[i] = new ArrayList<Integer>();
			marginalCosts[i] = new ArrayList<Double>();
			lastSolutionsValue[i] = new ArrayList<Double>();
			bestSolutionsValue[i] = new ArrayList<Double>();
			bestSolutionsValue[i].add(0.0);
			listTasks[i] = new ArrayList<Task>();
			allBids[i] = new ArrayList<Long>();
		}
	}

	//////////////////////////////////////
	//             BIDING              //
	//////////////////////////////////////

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		updateStuctures(previous, winner, bids);
	}

	@Override
	public Long askPrice(Task task) {
		lastTask++;
		long actualTime = System.currentTimeMillis();

		Long b;

		computeMarginalCost(task);
		if (biggestVehicle.capacity() < task.weight) { // TODO HANDLE CASE SLS RETURN NULL
			return null;
		}
		b = computeBiding(task);

		long duration = System.currentTimeMillis() - actualTime;

		print("AGENT 3 : BIDDING TASK " + task.id + ", Bid = " + Math.round(b) + ", id = " + id + ", in " + duration + " sec");

		return b;
	}

	private Long computeBiding(Task task) {
		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCosts[id].get(lastTask);

		return (long) Math.round(bid);
	}

	private void computeMarginalCost(Task task) {
		long actualTime = System.currentTimeMillis();
		long duration;
		int num = 3;
		for(int i=0; i<numA; i++) {
			if(i!=id) {
				double mg = 0;
				for(int j=0; j<num; j++) {
					ArrayList<City> cities = generateCities();
					ArrayList<Integer> capacities = generateCapacities(j, num);
					ArrayList<Task> tasks = (ArrayList<Task>) listTasks[i].clone();
					tasks.add(task);
					List<MyVehicle> vl = generateRandomVehicles(cities, capacities);
					NodePD lastSolution = sls.RunSLS(vl, tasks.toArray(new Task[tasks.size()]), timeout_bid/numA/num, null);
					mg += lastSolution.getOValue();
					duration = System.currentTimeMillis() - actualTime;
					print("numA=" + i + ", num=" + j + ", duration=" + duration);
				}
				lastSolutionsValue[i].add((mg/num));
				marginalCosts[i].add(lastSolutionsValue[i].get(lastTask) - bestSolutionsValue[i].get(lastTask));
			}
			else {
				ArrayList<Task> newTasks;
				newTasks = (ArrayList<Task>) listTasks[id].clone();
				newTasks.add(task);

				NodePD lastSolution = sls.RunSLS(vehiclesList, newTasks.toArray(new Task[newTasks.size()]), timeout_bid/numA, null);
				lastSolutionsValue[i].add(lastSolution.getOValue());
				marginalCosts[i].add(lastSolutionsValue[id].get(lastTask) - bestSolutionsValue[id].get(lastTask));
				duration = System.currentTimeMillis() - actualTime;
				print("numA=" + i + ", duration=" + duration);
			}
		}
	}

	private ArrayList<City> generateCities() {
		ArrayList<City> cities = new ArrayList<City>();
		ArrayList<Integer> list = new ArrayList<Integer>();
		List<City> cl = topology.cities();

		for (int i=0; i<cl.size(); i++) {
			list.add(new Integer(i));
		}
		Collections.shuffle(list);

		for(int i=0; i<Nv; i++) {
			City c = cl.get(list.get(i));
			cities.add(c);
		}
		return cities;
	}

	private ArrayList<Integer> generateCapacities(int j, int num) {
		ArrayList<Integer> capacities = new ArrayList<Integer>();
		double delta;
		double maxDiff = 0.6;
		if(num>1) delta = maxDiff*j/(num-1);
		else delta = 0;
		for(int i=0; i<Nv; i++) {
			double capa = meanCapa + meanCapa*(delta*(2*i/(Nv-1)-1));
			capacities.add((int)capa);
		}
		return capacities;
	}

	private List<MyVehicle> generateRandomVehicles(ArrayList<City> cities, ArrayList<Integer> capacities) {
		/** parameters to handle :
		 * - number of vehicles = same
		 * - costPerKm = same
		 * - home city = random
		 * - capacity = same total capacity
		 * - tasks : already acquired tasks + new task
		 * - 
		 */

		List<MyVehicle> vl = new ArrayList<MyVehicle>();

		for(int i=0; i<Nv; i++) {
			MyVehicle randV = new MyVehicle(capacities.get(i), biggestVehicle.costPerKm(), cities.get(i), i);
			vl.add(randV);
		}
		return vl;
	}

	//////////////////////////////////////
	//             PLANNING             //
	//////////////////////////////////////

	@Override
	public List<Plan> plan(List<Vehicle> vcls, TaskSet tasksSet) {

		if(vcls.size()==0) {
			List<Plan> plans = new ArrayList<Plan>();
			return plans;
		}

		List<MyVehicle> vehicles = MyVehicle.transform(vcls);


		time_start = System.currentTimeMillis();
		this.vehiclesList = vehicles;
		this.tasks = tasksSet.toArray(new Task[tasksSet.size()]);
		this.Nv = vehiclesList.size();
		List<Plan> plans;

		/*
		if(tasksSet.size() == 0) {
			print("TASKS SIZE == 0");

			plans = new ArrayList<Plan>();

			for(int v = 0 ; v<Nv ; v++){ // for each vehicle
				City current = vehiclesList.get(v).getCurrentCity();
				Plan plan = new Plan(current);

				plans.add(plan);
			}
			return plans;
		}
*/

		NodePD bestSolution = sls.RunSLS(vehicles, tasks, timeout_plan, null);

		plans = sls.computeFinalPlan(bestSolution);


		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in "+duration+" milliseconds.");

		int totalCost = 0;
		int totalDist = 0;
		int v = 0;
		for (Plan plan : plans) {
			totalDist+=plan.totalDistance();
			totalCost+=plan.totalDistance()*vehicles.get(v).costPerKm();
			v++;
		}

		//print("MEAN BID = " + meanBid[id]/allBids[id].size());
		print("Bids : ");
		int c = 0;
		for(ArrayList<Long> bids : allBids) {
			System.out.print("Agent " + c + ":");
			for(Long bid : bids) {
				System.out.print("  " + bid);
			}
			System.out.println("");
			c++;
		}
		
		print("");
		print("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		print("AGENT3 : number of tasks = " + tasksSet.size());
		print("FINAL DISTANCE = " + totalDist);
		if(tasksSet.size() == 0) print("FINAL COST2 = 0");
		else print("FINAL COST2 = " + bestSolution.getOValue());
		print("FINAL REWARD = " + tasksSet.rewardSum());
		print("FINAL PROFIT = " + (tasksSet.rewardSum()-totalCost));

		for(int a=0; a<numA; a++) {
			printInfoAgent(a);
		}
		print("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		return plans;
	}

	//////////////////////////////////////
	//              OTHERS              //
	//////////////////////////////////////

	private void updateStuctures(Task previous, int winner, Long[] bids) {

		for(int i=0; i<numA; i++) {
			if(i==winner) bestSolutionsValue[winner].add(lastSolutionsValue[winner].get(lastTask));
			else bestSolutionsValue[i].add(bestSolutionsValue[i].get(lastTask));
		}


		for(int i=0; i<numA; i++) {
			allBids[i].add(bids[i]);
			if(i==winner) tasksResult[i].add(1);
			else tasksResult[i].add(0);
		}


		totalReward[winner] += bids[winner];
		listTasks[winner].add(previous);
	}

	//////////////////////////////////////
	//              UTILS               //
	//////////////////////////////////////

	public void print(String s){
		System.out.println(s);
	}

	public void printInfoAgent(int ag) {
		print("------------------------------------------------------------------------------");
		print("AGENT " + ag + " INFORMATIONS :");
		System.out.print("Tasks");
		for(int a=0; a<tasksResult[ag].size(); a++) {
			System.out.print(", T" + a + ":" + (int) tasksResult[ag].get(a));
		}
		print("");

		System.out.print("lastSolutionsValue");
		for(int a=0; a<lastSolutionsValue[ag].size(); a++) {
			System.out.print(", T" + a + ":" + lastSolutionsValue[ag].get(a).intValue());
		}
		print("");

		System.out.print("BestSolutionValue");
		for(int a=0; a<bestSolutionsValue[ag].size(); a++) {
			System.out.print(", T" + a + ":" + bestSolutionsValue[ag].get(a).intValue());
		}
		print("");

		System.out.print("MarginalCost");
		for(int a=0; a<marginalCosts[ag].size(); a++) {
			System.out.print(", T" + a + ":" + marginalCosts[ag].get(a).intValue());
		}
		print("");

		System.out.print("Bid");
		for(int a=0; a<allBids[ag].size(); a++) {
			System.out.print(", T" + a + ":" + allBids[ag].get(a));
		}
		print("");
		print("------------------------------------------------------------------------------");
	}
}
