package agents;

//the list of imports
import java.util.ArrayList;
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
public class AuctionAgent2 implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private MyVehicle biggestVehicle;

	//TIME
	private long timeout_setup;
	private long timeout_plan;
	private long time_start;
	private long timeout_bid;

	private List<MyVehicle> vehiclesList; 
	private Task[] tasks;

	private int Nt;
	private int Nv;
	private int Na;

	private double p = 0.5; // probability used for localChoice
	private int numIt = 5000;
	private int n = 5;
	private Random rand;
	private int firstV = 7;
	private int lastV = 10;


	private int id;
	private int totalTasks = 0;
	private long[] totalReward;
	private int[] meanBid;
	private int numA = -1;
	private ArrayList<Long>[] allBids = null;
	private NodePD[] lastSolutions = null;
	private NodePD lastSolution = null;
	private NodePD[] bestSolutions = null;
	private ArrayList<Task>[] listTasks = null;
	private int[] marginalCosts = null;

	private ArrayList<Integer> LSV;
	private ArrayList<Integer> BST;
	private ArrayList<Integer> MC;
	private ArrayList<Integer> BID;
	private ArrayList<Integer> TASKS;
	
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
		this.biggestVehicle = vehiclesList.get(0);
		for (MyVehicle v : vehiclesList) {
			if(v.capacity() > biggestVehicle.capacity()) biggestVehicle = v;
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

		// int numA = ls.get(LogistSettings.SizeKey.NUMBER_OF_AGENTS);
		// int numT = ls.get(LogistSettings.SizeKey.NUMBER_OF_TASKS);
		//print("number of tasks = " + numT + " and number of agents = " + numA);

		rand = new Random();

		id = agent.id();

		sls = new SLS(topology, distribution, agent);
		astar = new Astar(topology, distribution, agent);
		
		LSV = new ArrayList<Integer>();
		BST = new ArrayList<Integer>();
		MC = new ArrayList<Integer>();
		BID = new ArrayList<Integer>();
		TASKS = new ArrayList<Integer>();
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
		if (biggestVehicle.capacity() < task.weight) {
			return null;
		}
		totalTasks++;
		if(numA==-1) {
			ArrayList<Task> newTasks = new ArrayList<Task>();
			newTasks.add(task);
			lastSolution = sls.RunSLS(vehiclesList, newTasks.toArray(new Task[newTasks.size()]), timeout_bid, null);
			LSV.add((int) lastSolution.getOValue());

			double marginalCost = lastSolution.getOValue();
			MC.add((int) marginalCost);
			BST.add(0);
			
			double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
			double bid = ratio * marginalCost;

			//print("AGENT 2 : BIDDING TASK " + task.id + ", Bid = " + Math.round(bid));
			BID.add((int) Math.round(bid));
			return (long) Math.round(bid);
		}
		else {
			ArrayList<Task> newTasks;
			newTasks = (ArrayList<Task>) listTasks[id].clone();
			newTasks.add(task);
			NodePD npd = bestSolutions[id];
				
			lastSolutions[id] = sls.RunSLS(vehiclesList, newTasks.toArray(new Task[newTasks.size()]), timeout_bid, npd);
			LSV.add((int) lastSolutions[id].getOValue());
			
			if(lastSolutions[id]==null) {
				print("ERROR : lastSolutions[id]==null");
			}
			double marginalCost;
			if(bestSolutions[id]!=null) {
				marginalCost= lastSolutions[id].getOValue() - bestSolutions[id].getOValue();
				BST.add((int) bestSolutions[id].getOValue());
			}
			else {
				marginalCost= lastSolutions[id].getOValue();
				BST.add(0);
			}

			MC.add((int) marginalCost);
			
			double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
			double bid = ratio * marginalCost;

			//print("AGENT 2 : BIDDING TASK " + task.id + ", Bid = " + Math.round(bid));

			BID.add((int) Math.round(bid));
			
			return (long) Math.round(bid);
		}
	}
	
	private void computeMarginalCost(Task task) {
		for(int i=0; i<numA; i++) {
			ArrayList<Task> tasks = listTasks[i];
			tasks.add(task);
			List<MyVehicle> vl = generateRandomVehicles();
			lastSolutions[i] = sls.RunSLS(vl, tasks.toArray(new Task[tasks.size()]), timeout_bid, null);
		}
	}
	
	private void computeBiding() {
		
	}
	
	private List<MyVehicle> generateRandomVehicles() {
		/** parameters to handle :
		 * - number of vehicles = same
		 * - costPerKm = same
		 * - home city = random
		 * - capacity = same total capacity
		 * - tasks : already acquired tasks + new task
		 * - 
		 */
		
		List<MyVehicle> vl = new ArrayList<MyVehicle>();
		for(MyVehicle v : vehiclesList) {
			//MyVehicle randV = new MyVehicle();
		}
		return vl;
	}

	//////////////////////////////////////
	//             PLANNING             //
	//////////////////////////////////////

	@Override
	public List<Plan> plan(List<Vehicle> vcls, TaskSet tasksSet) {

		print("SLS Agent2 for " + tasksSet.size() +  " tasks");
		//System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		
		if(vcls.size()==0) {
			List<Plan> plans = new ArrayList<Plan>();
			return plans;
		}
		
		List<MyVehicle> vehicles = MyVehicle.transform(vcls);


		time_start = System.currentTimeMillis();
		this.vehiclesList = vehicles;
		this.tasks = tasksSet.toArray(new Task[tasksSet.size()]);
		this.Nt = this.tasks.length;
		this.Nv = vehiclesList.size();
		this.Na = 2*Nt;
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
		
		bestSolutions[id] = sls.RunSLS(vehicles, tasks, timeout_plan, bestSolutions[id]);

		plans = sls.computeFinalPlan(bestSolutions[id]);


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
//		print("Bids : ");
//		int c = 0;
//		for(ArrayList<Long> bids : allBids) {
//			System.out.print("Agent " + c + ":");
//			for(Long bid : bids) {
//				System.out.print("  " + bid);
//			}
//			System.out.println("");
//			c++;
//		}
		
		print("");
		print("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		print("AGENT2 : number of tasks = " + tasksSet.size());
		print("FINAL DISTANCE = " + totalDist);
		if(tasksSet.size() == 0) print("FINAL COST2 = 0");
		else print("FINAL COST2 = " + bestSolutions[id].getOValue());
		print("FINAL REWARD = " + tasksSet.rewardSum());
		print("FINAL PROFIT = " + (tasksSet.rewardSum()-totalCost));
		
		System.out.print("Tasks");
		for(int a=0; a<TASKS.size(); a++) {
			System.out.print(", T" + a + ":" + TASKS.get(a));
		}
		print("");
		
		System.out.print("lastSolutionsValue");
		for(int a=0; a<LSV.size(); a++) {
			System.out.print(", T" + a + ":" + LSV.get(a));
		}
		print("");
		
		System.out.print("BestSolutionValue");
		for(int a=0; a<BST.size(); a++) {
			System.out.print(", T" + a + ":" + BST.get(a));
		}
		print("");
		
		System.out.print("MarginalCost");
		for(int a=0; a<MC.size(); a++) {
			System.out.print(", T" + a + ":" + MC.get(a));
		}
		print("");
		
		System.out.print("Bid");
		for(int a=0; a<BID.size(); a++) {
			System.out.print(", T" + a + ":" + BID.get(a));
		}
		print("");
		print("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		print("");
		return plans;
	}
	
	//////////////////////////////////////
	//              OTHERS              //
	//////////////////////////////////////

	private void updateStuctures(Task previous, int winner, Long[] bids) {
		if (numA==-1) {
			numA = bids.length;
			marginalCosts = new int[numA];
			lastSolutions = new NodePD[numA];
			lastSolutions[id] = lastSolution;
			bestSolutions = new NodePD[numA];
			totalReward = new long[numA];
			listTasks = (ArrayList<Task>[]) new ArrayList[numA];
			allBids = (ArrayList<Long>[]) new ArrayList[numA];
			for(int i=0; i<numA; i++) {
				listTasks[i] = new ArrayList<Task>();
				allBids[i] = new ArrayList<Long>();
			}
		}

		for(int i=0; i<numA; i++) {
			allBids[i].add(bids[i]);
		}

		if(winner!=id) TASKS.add(0);
		else TASKS.add(1);
		
		totalReward[winner] += bids[winner];
		listTasks[winner].add(previous);
		bestSolutions[winner] = lastSolutions[winner];
	}

	//////////////////////////////////////
	//              UTILS               //
	//////////////////////////////////////

	public void print(String s){
		System.out.println(s);
	}
	public void print(int s){
		System.out.println(s);
	}
	public void print(double s){
		System.out.println(s);
	}
}
