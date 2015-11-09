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

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedOneTask implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    private List<Vehicle> vehiclesList; 
    private Task[] tasks;
    
    private int Nt;
    private int Nv;
    
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
    	NodeT bestNodeT = null;
    	
    	int numIt = 5000;
    	int i = 0;
    	while(i < numIt) { // TODO
    		NodeT Aold = A;
    		ArrayList<NodeT> N = chooseNeighbours(A);
    		A = localChoice(N, Aold); // only keep bestNodeT in chooseNeighbours : more efficient? = rename "chooseBestNeigbours"
    		if(A.getOValue(topology) < bestNodeT.getOValue(topology)) bestNodeT = A;
    		i++;
    	}
    	
    	return bestNodeT;
    }
    
    //////////////////////////////////////
    //         SLS : NEIGHBOURS         //
    //////////////////////////////////////
    
    // vladman
    private ArrayList<NodeT> chooseNeighbours(NodeT Aold) {
    	ArrayList<NodeT> N = new ArrayList<NodeT>(); // TODO : ArrayList?
    	int vi = random(); // TODO
    	
    	// Applying the changing vehicle operator :
    	for (int vj=0; vj<Nv; vj++) {
    		if(vj!=vi) {
    			int t = Aold.nextTask(vi+Nv);
    			if(tasks[t].weight <= vehiclesList.get(vi).capacity()) { // no vehicle change if first task too heavy for all other vehicles
    				NodeT A = changingVehicle(Aold, vi, vj);
    				N.add(A);	
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
    	
    	
    	return A1;
    }
    
    // mouche
    private void updateTime(NodeT A, int v) {
    	
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
    	
    	initial.print();
    	return initial;
    }
    
    // mouche
    private NodeT localChoice(ArrayList<NodeT> N, NodeT Aold) {
    	NodeT bestNodeT = null;
    	
    	return bestNodeT;
    }
    
    // mouche
    private List<Plan> computeFinalPlan(NodeT n) {
    	List<Plan> plans = new ArrayList<Plan>();
    	
    	return plans;
    }
    
    //////////////////////////////////////
    //              UTILS               //
    //////////////////////////////////////
    
    private int random() {
    	return 0;
    }
}
