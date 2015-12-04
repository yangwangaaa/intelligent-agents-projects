package agents;

//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
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
import sls.NodePD;
import sls.SLS;
import AgentMouche.Configuration;
import AgentMouche.Strategy;
import astar.Astar;

/**
 * TODO :
 * peut utiliser agent size et task size?
 * need test agents
 */
@SuppressWarnings("unused")
public class AgentMouche implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle biggestVehicle;
	private ArrayList<Task> tasksWon;

	//TIME
	private long timeout_setup;
	private long timeout_plan;
	private long time_start;
	private long timeout_bid;

	private List<Vehicle> vehiclesList; 
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
	private long totalReward = 0;
	private int meanBid = 0;

	private ArrayList<Long[]> allBids = null;

	private SLS sls;
	private Astar astar;

	NodePD lastSolution = null;
	NodePD bestSolution = null;


	///////  LISTS   ////////
	private int proposed; //number of task proposed-1
	private int Nst;      //number of strategies
	private int Nconf;    //number of configurations

	private LinkedList<Long> mc1 =   new LinkedList<Long>();;
	private ArrayList<LinkedList<Long>> mc2 = null ;

	private LinkedList<Task> proposedTasks = new LinkedList<Task>() ;
	private  LinkedList<Boolean> listWinner = new LinkedList<Boolean>() ;

	private LinkedList<Long> bid1= new LinkedList<Long>() ;
	private LinkedList<Long> bid2= new LinkedList<Long>() ;


	private ArrayList<ArrayList<LinkedList<Long>>> ratio;
	private ArrayList<ArrayList<LinkedList<Long>>> allBid1;

	private ArrayList<ArrayList<Strategy>> strategies;	
	private ArrayList<Configuration> configurations;
	
	//////////////////////////////////////
	//              MAIN                //
	//////////////////////////////////////

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		print("here");
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehiclesList = agent.vehicles();
		this.biggestVehicle = agent.vehicles().get(0);
		for (Vehicle v : agent.vehicles()) {
			if(v.capacity() > biggestVehicle.capacity()) biggestVehicle = v;
		}


		long seed = -9019554669489983951L * biggestVehicle.hashCode() * agent.id();
		this.random = new Random(seed);

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

		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);

		print(timeout_setup);
		print(timeout_plan);
		print(timeout_bid);
		// int numA = ls.get(LogistSettings.SizeKey.NUMBER_OF_AGENTS);
		// int numT = ls.get(LogistSettings.SizeKey.NUMBER_OF_TASKS);
		//print("number of tasks = " + numT + " and number of agents = " + numA);

		rand = new Random();

		sls = new SLS(topology, distribution, agent);
		astar = new Astar(topology, distribution, agent);

		tasksWon = new ArrayList<Task>();
		allBids = new ArrayList<Long[]>();


		/////// PARAMS MOUCHE  //////
		proposed = -1;
		Nconf = 0;


		mc2 = new ArrayList<LinkedList<Long>>(Nconf);
		configurations = new ArrayList<Configuration>(Nconf);

		ratio = new ArrayList<ArrayList<LinkedList<Long>>>(Nconf);
		allBid1 = new ArrayList<ArrayList<LinkedList<Long>>>(Nconf);

		strategies = new ArrayList<ArrayList<Strategy>>(Nconf);

		for(int conf=0 ; conf<Nconf ; conf++){
			Nst = 0;
			strategies.set(conf, new ArrayList<Strategy>());
			for(double factor = 1; factor <= 1.5 ; factor +=0.1){
				for(double a=0; a<= 0.5; a+=0.25){
					for(double uf = 0,  ufl = 0; uf <= 1; uf+=0.5 , ufl+=0.5){
						for(double uuf = 0,  uufl = 0; uuf <= 1; uuf+=0.5 , uufl+=0.5){
							//strategies.get(conf).set(Nst, new Strategy(factor,a,uf,ufl,uuf,uufl,Nst));
							Nst+=1;
						}
					}
				}
			}

		}

		for(int i=0 ; i<Nconf ; i++){
			ratio.set(i,  new ArrayList<LinkedList<Long>>(Nst));
			allBid1.set(i, new ArrayList<LinkedList<Long>>(Nst));
		}		
	}

	//////////////////////////////////////
	//             BIDING              //
	//////////////////////////////////////

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		allBids.add(bids);

		if (winner == agent.id()) {
			totalReward += bids[winner];
			tasksWon.add(previous);
			bestSolution = lastSolution;
		}

		////////////////////////
		proposedTasks.add(previous);
		bid1.add(bids[agent.id()]);
		//bid2.add(bids[ennemy.id()]);

		if (winner == agent.id()) {
			listWinner.add(true);
		}else{
			listWinner.add(false);
		}
		updateStrategies();


	}

	@Override
	public Long askPrice(Task task) {

		// parfois bids négatifs
		if (biggestVehicle.capacity() < task.weight) {
			return null;
		}

		ArrayList<Task> newTasks = (ArrayList<Task>) tasksWon.clone();
		newTasks.add(task);
		//lastSolution = sls.RunSLS(vehiclesList, newTasks.toArray(new Task[newTasks.size()]), timeout_bid, bestSolution);
		if(lastSolution == null) {
			print("ERROR : LASTSOLUTION == NULL");
			return null;
		}


		double marginalCost = lastSolution.getOValue();
		if(bestSolution!=null) marginalCost -= bestSolution.getOValue();

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;

		meanBid += bid;
		print("AGENT 3 : BIDDING TASK " + task.id + ", Bid = " + Math.round(bid));

		///////////////////////////////
		proposed +=1;
		int bestConfiguration = findBestConfiguration();
		updateMc2(task);
		double bestMc2 = mc2.get(bestConfiguration).get(proposed);
		int[] bestStrategy = findBestStrategy(bestConfiguration);

		updateAllBid1();
		return allBid1.get(bestStrategy[0]).get(bestStrategy[1]).get(proposed);



		//return (long) Math.round(bid);



	}

	//////////////////////////////////////
	//             PLANNING             //
	//////////////////////////////////////

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasksSet) {

		if(vehicles.size()==0) {
			List<Plan> plans = new ArrayList<Plan>();//TODO??
			return plans;
		}

		time_start = System.currentTimeMillis();
		this.vehiclesList = vehicles;
		this.tasks = tasksSet.toArray(new Task[tasksSet.size()]);
		this.Nt = this.tasks.length;
		this.Nv = vehiclesList.size();
		this.Na = 2*Nt;
		List<Plan> plans;

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

		print("SLS Agent2 for " + tasksSet.size() +  " tasks");
		//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		//bestSolution = sls.RunSLS(vehicles, tasks, timeout_plan, bestSolution);

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

		print("MEAN BID = " + meanBid/allBids.size());
		print("Bids : ");
		int c = 0;
		for(Long[] bids : allBids) {
			System.out.print("Task " + c);
			for(Long bid : bids) {
				System.out.print(", " + bid);
			}
			System.out.println("");
			c++;
		}

		print("FINAL DISTANCE = " + totalDist);
		print("FINAL COST = " + totalCost);
		print("FINAL COST2 = " + bestSolution.getOValue());
		print("FINAL REWARD = " + tasksSet.rewardSum());
		print("FINAL REWARD2 = " + totalReward);
		print("FINAL PROFIT = " + (tasksSet.rewardSum()-totalCost));

		return plans;
	}

	//////////////////////////////////////
	//		AUXILIARY METHODS			//
	//////////////////////////////////////

	public int findBestConfiguration(){
		return configurations.get(1).getId();
	}

	public void updateMc2(Task task){

	}

	public int[] findBestStrategy(int bestConf){
		int configuration = 0;
		int strategy = 0;
		return new int[] {configuration, strategy};
	}

	public void updateAllBid1(){

	}

	public void updateStrategies(){

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
