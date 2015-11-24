package template;

import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Reactive implements ReactiveBehavior {

	//private Random random;
	//private double pPickup;
	DoubleMatrix BestActions;
	List<City> cities;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		cities = topology.cities();
		
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
	
		if(discount > 1 || discount <= 0){
			discount = 0.95;
			System.out.println("The discount is not a acceptable value, we use the default discount = 0.95 instead");
		}
		
		Computation matrix = new Computation();
		BestActions = matrix.computeMatrix(topology, td, agent, discount);
		
		//this.random = new Random();
		//this.pPickup = discount;
	}

	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = null;
		
		int BestActionIndex = returnBestAction(vehicle, availableTask);
		
		if(BestActionIndex == 0)
			action = new Pickup(availableTask);
		else
			action = new Move(cities.get(BestActionIndex - 1));

		return action;
	}
	
	public int returnBestAction(Vehicle vehicle, Task availableTask){
		int stateIndex, actionIndex, currentCityIndex = cities.indexOf(vehicle.getCurrentCity());
		if (availableTask == null || availableTask.pickupCity != vehicle.getCurrentCity()) {
			stateIndex = currentCityIndex * 9 + currentCityIndex;
		} else {
			int toCityIndex = cities.indexOf(availableTask.deliveryCity);
			stateIndex = currentCityIndex * 9 + toCityIndex;
		}
		actionIndex = (int) BestActions.get(stateIndex);
        if(actionIndex==0){
        	System.out.println("currentCity: " + vehicle.getCurrentCity() + ", BestAction = Pick up task and Move to City: "+availableTask.deliveryCity);
            //System.out.println("currentCityIndex = " + currentCityIndex + ", BestAction = Pick up task and Move to City: "+availableTask.deliveryCity);
        }
        else {
            System.out.println("currentCity: " + vehicle.getCurrentCity() + ", BestAction = Move to Neighborhood City: " + cities.get(actionIndex-1));        	
            //System.out.println("currentCityIndex = " + currentCityIndex + ", BestAction = Move to Neighborhood City: " + cities.get(actionIndex-1));            
        }
		return actionIndex;
	}
}
