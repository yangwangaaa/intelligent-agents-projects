package agents;

//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cern.colt.Arrays;
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
import other.MyTask;
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

	private double timeout_setup;
	private double timeout_plan;
	private double time_start;
	private double timeout_bid;

	private List<MyVehicle> vehiclesList; 

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
	private double totalReward1 = 0;
	private double totalReward2 = 0;
	private int minCarried = 5;
	private int carriedSize1 = 0;
	private int carriedSize2 = 0;

	private ArrayList<Double> mc1 = new ArrayList<Double>(); // v
	private ArrayList<Double>[] mc2 = (ArrayList<Double>[]) new ArrayList[Nconf]; // v

	private ArrayList<Double> last1 = new ArrayList<Double>(); // v
	private ArrayList<Double>[] last2 = (ArrayList<Double>[]) new ArrayList[Nconf]; // v

	private ArrayList<Double> best1 = new ArrayList<Double>(); // v
	private ArrayList<Double>[] best2 = (ArrayList<Double>[]) new ArrayList[Nconf]; // v

	private ArrayList<Task> tasks1 = new ArrayList<Task>(); // v
	private ArrayList<Task> tasks2 = new ArrayList<Task>(); // v

	private ArrayList<Task> proposedTasks = new ArrayList<Task>(); // v
	private ArrayList<Integer> listWinner = new ArrayList<Integer>(); // v

	private ArrayList<Double> bid1= new ArrayList<Double>() ; // v
	private ArrayList<Double> bid2= new ArrayList<Double>() ; // v

	private ArrayList<Double>[] ratio = (ArrayList<Double>[]) new ArrayList[Nconf]; // v
	private ArrayList<Double>[][] allBid1 = (ArrayList<Double>[][]) new ArrayList[Nconf][Nst]; // v

	private Strategy[][] strategies = new Strategy[Nconf][Nst];	 // v
	private Configuration[] configurations = new Configuration[Nconf]; // v

	ArrayList<MyTask> allPossibleTasks = new ArrayList<MyTask>(); // v


	private double[][] weights = new double[Nconf][Nst];
	private double[] weightConf = new double[Nconf];
	private double bestBid1;
	private double bestMc2;



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
			ls = Parsers.parseSettings("config/settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		timeout_bid*=0.95*timeout_bid;

		//print("setup");
		// Init strategy structures
		for(int conf=0 ; conf<Nconf ; conf++){
			Nst = 0; // 49
			for(double factor1 = (double) 0.85; factor1 <= 1.15 ; factor1 +=0.05){ // 7
				//print("a");
				for(double factor2 = (double) 0.85; factor2 <= 1.15 ; factor2 +=0.05){ //7
					//print("b");
					for(double a=0; a<= 0; a+=0.25){ // 0
						//print("c");
						for(double uf = 0,  ufl = 0; uf <= 0; uf+=0.5 , ufl+=0.5){ // 0
							//print("d");
							for(double uuf = 0,  uufl = 0; uuf <= 0;   uufl+=0.5){
								uuf= uuf + 0.5;
								//print(uuf);
								//print("lol");
								//print(Nst);
								strategies[conf][Nst] = new Strategy(factor1,factor2,a,uf,ufl,uuf,uufl,Nst);
								Nst+=1;

							}
						}
					}
				}
			}
		}
		//print("setup");
		for(int i=0 ; i<Nconf ; i++){
			weightConf[i]=(double) Nconf/2;
			ratio[i] = new ArrayList<Double>();
			mc2[i] = new ArrayList<Double>();
			for(int j=0 ; j<Nst ; j++){				
				allBid1[i][j] = new ArrayList<Double>(); //TODO on doit faire ca?
			}
		}	
		//print("setup");
		print("");
		
		
		// Init marginal cost estimation
		best1.add(0.0);
		for(int i=0; i<Nconf; i++) {
			last2[i] = new ArrayList<Double>();
			best2[i] = new ArrayList<Double>();
			best2[i].add(0.0);
		}

		createMostProbableTasks();
	}


	//////////////////////////////////////
	//          FINAL PLANNING          //
	//////////////////////////////////////

	@Override
	public List<Plan> plan(List<Vehicle> vcls, TaskSet tasksSet) {
		print("----------- PLAN --------- ");
		// init
		this.vehiclesList = MyVehicle.transform(agent.vehicles());
		List<Plan> plans;
		Task[] tasks = tasksSet.toArray(new Task[tasksSet.size()]);

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
		proposed++;
		print("----------- ASK PRICE proposed = "+((Integer) proposed).toString()+" agent is "+((Integer)agent.id()).toString()+" ----------");
		print("askPrice for "+task.toString());
		double actualTime = System.currentTimeMillis();
		Double b = (double) 0;

		//		// pipeline
		//		//print("before ");
		//		//print("after ");
		//		//print("before updateMc1");
		//		updateMc1(task);
		//		printMc1();
		//		print("mc1 = "+((Double) mc1.get(proposed)).toString());
		//		//print("after updateMc1");
		//		//print("before updateMc2");
		//		updateMc2(task);
		//		//print("after updateMc2");
		//		printMc2();

		computeMarginalCost(task);
		//STRATE1
		//updateAllBids();
		//findBestStrategy();
		//updateBestBid1general();

		//STRATE2
		//print("before updateBestMarginalCost2");	
		updateBestMc2();
		printWeightConf();
		print("best Mc2 =" + ((Double)bestMc2).toString());
		//print("after updateBestMarginalCost2");

		//print("before updateBestBid1");
		updateBestBid1();
		//print("after updateBestBid1");
		print("bestBid1 ="+((Double)bestBid1).toString());

		//b = computeFinalBid();
		//

		double duration = System.currentTimeMillis() - actualTime;
		print("AGENTMERGE : BIDDING TASK " + task.id + ", Bid = " + Math.round(b) + ", in " + duration + " sec");
		print("");
		return (long) bestBid1;

	}


	//////////////////////////////////////
	// 	  COMPUTE MARGINAL COST OF 1    // 
	//////////////////////////////////////

	private void updateMc1(Task task) {
		mc1.add(780.0);
	}

	//////////////////////////////////////
	//    COMPUTE MARGINAL COST OF 2    //
	//////////////////////////////////////

	private void updateMc2(Task task) {
		for(int conf = 0; conf< Nconf ; conf++){
			if(conf == 0){mc2[conf].add( 1000.0);}
			else{ 
				if(proposed %2 == 0){
					mc2[conf].add(500.0);

				}else{
					mc2[conf].add( 1500.0);
				}
			}
		}
	}

	/*
	private ArrayList<Double>[] lastSolutionsValue = null; // last1 last2
	private ArrayList<Double>[] bestSolutionsValue = null; // best1 best 2
	private ArrayList<Double>[] marginalCosts = null; // mc1 mc2
	private ArrayList<Task>[] listTasks = null; // tasks1 tasks2
	*/

	private void computeMarginalCost(Task task) {
		double timeout_agent = timeout_bid/nA;
		double timeout_opponent = timeout_bid/nA/Nconf;
		ArrayList<Task> supp = createSuppTasks(minCarried);
		
		
		// Agent
		ArrayList<Task> tasks1Clone = (ArrayList<Task>) tasks1.clone();
		NodePD bestSolution1;
		if(carriedSize1 +1 <= minCarried) {
			timeout_agent = timeout_agent/2;
			addSuppTasks(tasks1Clone, supp, minCarried-carriedSize1-1);
			bestSolution1 = sls.RunSLS(vehiclesList, tasks1Clone.toArray(new Task[tasks1Clone.size()]), timeout_agent, null);
			best1.set(proposed, bestSolution1.getOValue());
		}

		tasks1Clone.add(task);
		NodePD lastSolution1 = sls.RunSLS(vehiclesList, tasks1Clone.toArray(new Task[tasks1Clone.size()]), timeout_agent, null);
		last1.add(lastSolution1.getOValue());
		mc1.add(last1.get(proposed) - best1.get(proposed));
		
		
		// Opponent		
		for(int j=0; j<Nconf; j++) {
			ArrayList<City> cities = generateCities();
			ArrayList<Integer> capacities = generateCapacities(j, Nconf);
			List<MyVehicle> vl = generateRandomVehicles(cities, capacities);

			ArrayList<Task> tasks2Clone = (ArrayList<Task>) tasks2.clone();
			// tasks.add(task);	
			if(carriedSize2+1 <= minCarried) {
				addSuppTasks(tasks2Clone, supp, minCarried-carriedSize2-1);
				NodePD bestSolution2 = sls.RunSLS(vl, tasks2Clone.toArray(new Task[tasks2Clone.size()]), timeout_opponent/2, null);
				tasks2Clone.add(task);	
				NodePD lastSolution2 = sls.RunSLS(vl, tasks2Clone.toArray(new Task[tasks2Clone.size()]), timeout_opponent/2, null);
				best2[j].set(proposed, bestSolution2.getOValue());
				last2[j].add(lastSolution2.getOValue());
				mc2[j].add(last2[j].get(proposed) - best2[j].get(proposed));
			}
			else {
				tasks2Clone.add(task);	
				NodePD lastSolution2 = sls.RunSLS(vl, tasks2Clone.toArray(new Task[tasks2Clone.size()]), timeout_opponent, null);
				last2[j].add(lastSolution2.getOValue());
				mc2[j].add(last2[j].get(proposed) - best2[j].get(proposed));
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
		List<MyVehicle> vl = new ArrayList<MyVehicle>();
		for(int i=0; i<Nv; i++) {
			MyVehicle randV = new MyVehicle(capacities.get(i), biggestVehicle.costPerKm(), cities.get(i), i);
			vl.add(randV);
		}
		return vl;
	}

	private void createMostProbableTasks() {
		int i = 0;
		for(City c1 : topology.cities()) {
			for(City c2 : topology.cities()) {
				if(!c1.equals(c2)) {
					double proba = distribution.probability(c1, c2);
					//print("C1=" + c1.name + " to C2=" + c2.name + " have proba : " + proba);
					MyTask t = new MyTask(i, c1, c2, distribution.reward(c1, c2), distribution.weight(c1, c2), proba);
					allPossibleTasks.add(t);
					i++;
				}
			}
		}
		Collections.sort(allPossibleTasks);
	} 

	private ArrayList<Task> createSuppTasks(int n) {
		ArrayList<Task> tasks = new ArrayList<Task>();
		int i = 0;
		if(n<1) return tasks;
		else {
			//print("SUPP=");
			int count = 0;
			while(count!=n) {
				MyTask t = allPossibleTasks.get(i);
				if(t.notIn(proposedTasks)) {
					Task tk = t.generateTask(proposed+1+count);
					//print(tk + " proba=" + t.getProba());
					tasks.add(tk);
					count++;
				}
				i++;
			}
			return tasks;
		}

	}

	private void addSuppTasks(ArrayList<Task> tasks, ArrayList<Task> supp, int n) {
		int i = 0;
		while(i<n) {
			tasks.add(supp.get(i));
			i++;
		}
	}





















	//////////////////////////////////////
	//   COMPUTE BEST MARGINAL COST     //
	//////////////////////////////////////

	private void updateBestMc2(){
		bestMc2 = 0;
		double sumWeights = 0;
		for(int conf = 0; conf<Nconf ; conf++){
			bestMc2 += weightConf[conf]*mc2[conf].get(proposed);
			sumWeights += weightConf[conf];
		}
		bestMc2 = bestMc2/sumWeights;
	}


	private void updateBestBid1() {
		double m1 = mc1.get(proposed);
		double m2 = bestMc2;
		double factor1 = 1;
		double factor2 = 1;

		if(m1<=m2){
			bestBid1 = factor1*(m1+(m2-m1)/2);
		}else{
			bestBid1 = factor2*(m1+(m2-m1)/2);
		}
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		print("---------- auctionResult proposed = "+((Integer) proposed).toString()+" agent is "+((Integer)agent.id()).toString()+" ----------");
		updateStuctures(previous, winner, bids);
		//updateStrategies();
		updateWeightConf();
		print("");
	}

	//////////////////////////////////////
	//             UPDATES              //
	//////////////////////////////////////
	
	private void updateStuctures(Task previous, int winner, Long[] bids) {

		proposedTasks.add(previous);
		int ennemy = 0;
		for(int i = 0; i<bids.length ; i++){
			if(i == agent.id()){
				print("my bid was "+((Long)bids[agent.id()]).toString());
				bid1.add((double)bids[agent.id()]); //TODO cast en double problème?
				//print("Array bid1 = "+bid1.toString());
			}else{
				print("ennemy's bid = "+((Long)bids[i]).toString());
				ennemy = i;
				bid2.add((double)bids[i]);
			}
		}

		//winner
		if (winner == agent.id()) {
			carriedSize1++;
			best1.add(last1.get(proposed));
			for(int i=0; i<Nconf; i++) {
				best2[i].add(best2[i].get(proposed));
			}
			tasks1.add(previous);
			listWinner.add(agent.id());
		}else{
			carriedSize2++;
			best1.add(best1.get(proposed));
			for(int i=0; i<Nconf; i++) {
				best2[i].add(last2[i].get(proposed));
			}
			tasks2.add(previous);
			listWinner.add(ennemy);
		}
		//ratio
		for(int conf = 0; conf<Nconf ; conf++){
			ratio[conf].add( bid2.get(proposed)/mc2[conf].get(proposed));
		}
	}


	private void updateWeightConf(){
		if(proposed!=0){
			int bestConf =0;
			double bestVar = Double.MAX_VALUE;
			//compute mean
			double[] mean = new double[Nconf] ;
			for(int conf = 0; conf<Nconf ; conf++){
				mean[conf]=0;
			}
			for(int conf = 0; conf<Nconf ; conf++){
				for(int prop =0; prop<ratio[conf].size() ;prop++){
					mean[conf] += ratio[conf].get(prop);
				}
			}
			for(int conf = 0; conf<Nconf ; conf++){
				mean[conf] = mean[conf]/ratio[conf].size();
			}
			//print("mean ratio conf 1");
			//print(mean[0]);
			//compute variance
			double[] var = new double[Nconf] ;
			for(int conf = 0; conf<Nconf ; conf++){
				var[conf]=0;
			}
			for(int conf = 0; conf<Nconf ; conf++){
				for(int prop =0; prop<ratio[conf].size() ;prop++){
					double square = (double) Math.pow( (ratio[conf].get(prop) - mean[conf]), 2);
					var[conf] += square;
				}
			}
			for(int conf = 0; conf<Nconf ; conf++){
				var[conf] =  (double) (var[conf]/(ratio[conf].size()-1.0));
				//find best variance
				if(var[conf]<=bestVar){
					bestVar = var[conf];
					bestConf = conf;
				}
			}
			//print("----");
			printRatio();
			print("----");
			print("variances = "+Arrays.toString(var));

			//update weights
			for(int conf = 0; conf<Nconf ; conf++){

				if(conf == bestConf){
					//print("best conf");
					//print(conf);
					weightConf[conf] += ((double)Nconf)/2.0 + 1.0;//éviter d'augmenter que de 0
				}else{
					//print("not best conf");
					//print(conf);
					//print(weightConf[conf]);
					weightConf[conf] -= ((double)Nconf/2.0)/(((double)Nconf-1.0));
					//print( (((double)Nconf)/2.0)/(((double)Nconf)-1.0) );
				}
				if(weightConf[conf]<=0){
					weightConf[conf] = 0;
				}
			}
			print("weights for the conf = "+Arrays.toString(weightConf));
		}

	}


	private double computeFinalBid() {
		double bid = (double) 0;

		return bid;
	}


	//////////////////////////////////////
	// 		 COMPUTE MARGINAL COST 		//
	//////////////////////////////////////

	private void updateAllBids() {

		for(int conf = 0; conf <Nconf ; conf++){
			double m2 = mc2[conf].get(proposed);
			for(int strat = 0; strat < Nst ; strat++){
				allBid1[conf][strat].add(strategies[conf][strat].estimate(mc1.get(proposed), m2));
			}
		}
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
		return new int[] {configuration, strategy};// au début renvoie une straté inté
	}


	public void updateStrategies(){

	}




	//////////////////////////////////////
	//              UTILS               //
	//////////////////////////////////////

	public void print(String s){
		System.out.println(s);
	}
	public void print(double s){
		System.out.println(s);
	}
	public void print(int s){
		System.out.println(s);
	}
	public void print(long d){
		System.out.println(d);
	}
	public void printMc2(){
		print("TABLE MC2:");
		for(int conf = 0; conf <Nconf ; conf++){
			Integer x = (Integer)conf;
			print("configuration "+x.toString());
			print(mc2[conf].toString());
		}
	}
	public void printMc1(){
		System.out.println("table mc1 = "+mc1.toString());
	}
	public void printRatio(){
		print("TABLE RATIO");
		for(int conf = 0; conf <Nconf ; conf++){
			Integer x = (Integer)conf;
			print("configuration "+x.toString());
			print(ratio[conf].toString());
		}
	}
	public void printWeightConf(){
		System.out.println("weightConf = "+Arrays.toString(weightConf));
	}

}

/*
for(int conf = 0; conf <Nconf ; conf++){
	for(int strat = 0; strat < Nst ; strat++){

	}
}
 */
