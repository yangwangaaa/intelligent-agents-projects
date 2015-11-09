package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
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
    
    private List<Vehicle> vehicles; 
    private TaskSet tasks;
    
    
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
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        this.vehicles = vehicles;
        this.tasks = tasks;
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Node bestSolution = SLS();

        List<Plan> plans = computeFinalPlan(bestSolution);

        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");
        
        return plans;
    }
    
    
    //////////////////////////////////////
    //               SLS                //
    //////////////////////////////////////
    
    private Node SLS() {
    	Node A = selectInitialSolution();
    	Node bestNode = null;
    	
    	int numIt = 5000;
    	int i = 0;
    	while(i < numIt) { // TODO
    		Node Aold = A;
    		ArrayList<Node> N = chooseNeighbours(A);
    		A = localChoice(N); // only keep bestNode in chooseNeighbours : more efficient?
    		i++;
    		
    		if(A.getOValue() < bestNode.getOValue()) bestNode = A;
    	}
    	
    	return bestNode;
    }
    
    //////////////////////////////////////
    //            SUBMETHODS            //
    //////////////////////////////////////
    
    private Node selectInitialSolution() {
    	Node initial =null;
    	
    	return initial;
    	
    }
    
    private ArrayList<Node> chooseNeighbours(Node Aold) {
    	ArrayList<Node> neighbours = new ArrayList<Node>();
    	
    	return neighbours;
    }
    
    private Node localChoice(ArrayList<Node> N) {
    	Node bestNode = null;
    	
    	return bestNode;
    }
    
    private void changingVehicle(Node A, int v1, int v2, ArrayList<Node> neighbours) {
    	
    }
    
    private void changingTaskOrder(Node A, int v, int aI1, int aI2, ArrayList<Node> neighbours) {
    	
    }
    
    private void updateTime(Node A, int v) {
    	
    }
    
    private List<Plan> computeFinalPlan(Node n) {
    	List<Plan> plans = new ArrayList<Plan>();
    	
    	return plans;
    }
    
    //////////////////////////////////////
    //              UTILS               //
    //////////////////////////////////////
}
