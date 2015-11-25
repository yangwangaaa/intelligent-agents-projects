package agents;

//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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
import AgentMouche.Configuration;
import AgentMouche.Strategy;
import astar.Astar;

/**
 * TODO :
 * 
 */


@SuppressWarnings("unused")
public class AgentMerge implements AuctionBehavior {

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

	//////////////////////////////////////
	//         VARS AND STRUCTS         //
	//////////////////////////////////////

	private SLS sls;
	private Astar astar;
	private int meanCapa;
	private int nA = 2;
	private int Nv;
	private int id;
	private int proposed = -1; //number of task proposed-1
	private int Nst = 36;      //number of strategies
	private int Nconf = 3;    //number of configurations
	private long totalReward1 = 0;
	private long totalReward2 = 0;

	private ArrayList<Long> mc1 = new ArrayList<Long>(); // v
	private ArrayList<Long>[] mc2 = (ArrayList<Long>[]) new ArrayList[Nconf]; // x

	private ArrayList<Long> last1 = new ArrayList<Long>(); // v
	private ArrayList<Long>[] last2 = (ArrayList<Long>[]) new ArrayList[Nconf]; // x

	private ArrayList<Long> best1 = new ArrayList<Long>(); // v
	private ArrayList<Long>[] best2 = (ArrayList<Long>[]) new ArrayList[Nconf]; // x

	private ArrayList<Task> tasks1 = new ArrayList<Task>(); // v
	private ArrayList<Task> tasks2 = new ArrayList<Task>(); // v

	private ArrayList<Task> proposedTasks = new ArrayList<Task>(); // v
	private ArrayList<Integer> listWinner = new ArrayList<Integer>(); // v

	private ArrayList<Long> bid1= new ArrayList<Long>() ; // v
	private ArrayList<Long> bid2= new ArrayList<Long>() ; // v

	private ArrayList<Double>[][] ratio = (ArrayList<Double>[][]) new ArrayList[Nconf][Nst]; // v
	private ArrayList<Long>[][] allBid1 = (ArrayList<Long>[][]) new ArrayList[Nconf][Nst]; // v

	private Strategy[][] strategies = new Strategy[Nconf][Nst];	 // v
	private Configuration[] configurations = new Configuration[Nconf]; // v

	private double[][] weights = new double[Nconf][Nst];


	//////////////////////////////////////
	//              MAIN                //
	//////////////////////////////////////

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		// Init basic structures
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.id = agent.id();
		this.vehiclesList = MyVehicle.transform(agent.vehicles());
		this.Nv = vehiclesList.size();
		this.biggestVehicle = vehiclesList.get(0);
		for (MyVehicle v : vehiclesList) {
			if(v.capacity() > biggestVehicle.capacity()) biggestVehicle = v;
			meanCapa = meanCapa + v.capacity()/Nv;
		}
		long seed = -9019554669489983951L * biggestVehicle.hashCode() * agent.id();
		this.random = new Random(seed);
		sls = new SLS(topology, distribution, agent);
		astar = new Astar(topology, distribution, agent);


		// Init timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_default.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		timeout_bid*=0.95*timeout_bid;


		// Init strategy structures
		for(int conf=0 ; conf<Nconf ; conf++){
			Nst = 0; // 36?
			for(double factor = 1; factor <= 1.5 ; factor +=0.1){ // 6
				for(double a=0; a<= 0.5; a+=0.25){ // 3
					for(double uf = 0,  ufl = 0; uf <= 1; uf+=0.5 , ufl+=0.5){ // 3
						for(double uuf = 0,  uufl = 0; uuf <= 1; uuf+=0.5 , uufl+=0.5){
							strategies[conf][Nst] = new Strategy(factor,a,uf,ufl,uuf,uufl,Nst);
							Nst+=1;
						}
					}
				}
			}
		}

		for(int i=0 ; i<Nconf ; i++){
			for(int j=0 ; j<Nst ; j++){
				ratio[i][j] = new ArrayList<Double>();
				allBid1[i][j] = new ArrayList<Long>();
			}
		}	
	}


	//////////////////////////////////////
	//          FINAL PLANNING          //
	//////////////////////////////////////

	@Override
	public List<Plan> plan(List<Vehicle> vcls, TaskSet tasksSet) {
		// init
		this.vehiclesList = MyVehicle.transform(agent.vehicles());
		List<Plan> plans;
		this.tasks = tasksSet.toArray(new Task[tasksSet.size()]);

		// handle limit cases
		if(vcls.size()==0) {
			plans = new ArrayList<Plan>();
			return plans;
		}

		if(tasksSet.size() == 0) {
			print("TASKS SIZE == 0");
			plans = new ArrayList<Plan>();
			for(int v = 0 ; v<Nv ; v++){
				City current = vehiclesList.get(v).getCurrentCity();
				Plan plan = new Plan(current);
				plans.add(plan);
			}
			return plans;
		}


		// compute final plan
		NodePD bestSolution = sls.RunSLS(vehiclesList, tasks, timeout_plan, null);
		plans = sls.computeFinalPlan(bestSolution);


		// Display final results
		int totalCost = 0;
		int totalDist = 0;
		int v = 0;
		for (Plan plan : plans) {
			totalDist+=plan.totalDistance();
			totalCost+=plan.totalDistance()*vehiclesList.get(v).costPerKm();
			v++;
		}

		print("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		print("AGENT MERGE : number of tasks = " + tasksSet.size());
		print("FINAL DISTANCE = " + totalDist);
		print("FINAL COST = " + bestSolution.getOValue());
		print("FINAL REWARD = " + tasksSet.rewardSum());
		print("FINAL PROFIT = " + (tasksSet.rewardSum()-totalCost));
		print("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

		print("");
		print("FINAL BIDS : ");
		System.out.print("Agent merge:");
		for(int b=0; b<bid1.size(); b++) {
			System.out.print(" T"+ b + ":" + bid2.get(b));
		}
		System.out.println("");

		for(int b=0; b<bid2.size(); b++) {
			System.out.print(" T"+ b + ":" + bid2.get(b));
		}
		System.out.println("");


		return plans;
	}


	//////////////////////////////////////
	//              BIDING              //
	//////////////////////////////////////

	@Override
	public Long askPrice(Task task) {
		long actualTime = System.currentTimeMillis();
		Long b = (long) 0;

		// pipeline
		proposed++;
		updateMarginalCost(task);
		updateAllBids();
		findBestStrategy();
		b = computeFinalBid();
		//

		long duration = System.currentTimeMillis() - actualTime;
		print("AGENTMERGE : BIDDING TASK " + task.id + ", Bid = " + Math.round(b) + ", in " + duration + " sec");
		return b;
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		updateStuctures(previous, winner, bids);
		updateStrategies();
		
//		////////////////////////
//		proposedTasks.add(previous);
//		bid1.add(bids[agent.id()]);
//		bid2.add(bids[ennemy.id()]);
//
//		if (winner == agent.id()) {
//			listWinner.add(true);
//		}else{
//			listWinner.add(false);
//		}
//		updateStrategies();

	}

	private long computeFinalBid() {
		long bid = (long) 0;




		return bid;
	}


	//////////////////////////////////////
	// 		 COMPUTE MARGINAL COST 		//
	//////////////////////////////////////

	private void updateMarginalCost(Task task) {

	}


	//////////////////////////////////////
	//			 COMPUTE BIDS	    	//
	//////////////////////////////////////

	private void updateAllBids() {

	}


	//////////////////////////////////////
	//			   STRATEGY 			//
	//////////////////////////////////////

	public int findBestConfiguration(){
		return configurations[1].getId();
	}

	public int[] findBestStrategy(){
		int bestConf = findBestConfiguration();
		int configuration = 0;
		int strategy = 0;
		return new int[] {configuration, strategy};
	}


	//////////////////////////////////////
	//             UPDATES              //
	//////////////////////////////////////

	private void updateStuctures(Task previous, int winner, Long[] bids) {

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