package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;
    private double marginalCost;
    private long bid;
    private Logger log;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {

        this.log = Logger.getLogger(AuctionTemplate.class.getName());

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0);
        this.currentCity = vehicle.homeCity();

        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        String status;
        if (winner == agent.id()) {
            status = "win " + bid + " (" + (bid - marginalCost) + ")";
            currentCity = previous.deliveryCity;
        } else {
            status = "lost " + bid;
            bid = 0;
            marginalCost = 0;
        }
        log.info("[" + agent.id() + "] " + status);
    }

    @Override
    public Long askPrice(Task task) {

        if (vehicle.capacity() < task.weight)
            return null;

        double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
        long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
        long distanceSum = distanceTask
                + currentCity.distanceUnitsTo(task.pickupCity);

        marginalCost = Measures.unitsToKM(distanceSum * vehicle.costPerKm());
        bid = Math.round(ratio * marginalCost);

        return bid;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        Plan planVehicle1 = naivePlan(vehicle, tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size())
            plans.add(Plan.EMPTY);

        return plans;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }

        return plan;
    }
}
