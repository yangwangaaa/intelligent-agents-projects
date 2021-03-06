package logist.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import logist.Measures;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.topology.Topology.City;

/**
 * A sequence of actions for one vehicle.
 * 
 * @author Robin Steiger
 */
public class Plan implements Iterable<Action> {

    /** The empty plan */
    public static final Plan EMPTY = new Plan(null).seal();

    private final City initialCity;
    private List<Action> actions;
    private List<Action> immutableActions;

    /**
     * Creates a simple plan from an initial city and a fixed number of actions.
     * An empty plan can be created as follows:
     * <code>new Plan(initialCity)</code>
     * 
     * @param initialCity
     *            The initial city
     * @param actions
     *            (optional) A fixed number of actions
     */
    public Plan(City initialCity, Action... actions) {
        this.initialCity = initialCity;
        this.actions = new ArrayList<Action>(Arrays.asList(actions));
        this.immutableActions = Collections.unmodifiableList(this.actions);
    }
    
    /**
     * Creates a simple plan from an initial city and a list of actions.
     * An empty plan can be created as follows:
     * <code>new Plan(initialCity)</code>
     * 
     * @param initialCity
     *            The initial city
     * @param actions
     *            The list of actions
     */
    public Plan(City initialCity, List<Action> actions) {
        this.initialCity = initialCity;
        this.actions = new ArrayList<Action>(actions);
        this.immutableActions = Collections.unmodifiableList(actions);
    }

    @Override
    public String toString() {
        return Arrays.toString(actions.toArray()) + ", " + totalDistance();
    }

    /**
     * Appends an action to the plan
     * 
     * @param action
     *            The action to append
     */
    public void append(Action action) {
        actions.add(action);
    }

    /**
     * Appends a move action to the plan
     * 
     * @param city
     *            The target of the move
     */
    public void appendMove(City city) {
        append(new Move(city));
    }

    /**
     * Appends a pickup action to the plan
     * 
     * @param task
     *            The task to pick up
     */
    public void appendPickup(Task task) {
        append(new Pickup(task));
    }

    /**
     * Appends a delivery action to the plan
     * 
     * @param task
     *            The task to deliver
     */
    public void appendDelivery(Task task) {
        append(new Delivery(task));
    }

    /** Prevent any future modification to this plan */
    public Plan seal() {
        actions = immutableActions = Collections
                .unmodifiableList(new ArrayList<Action>(actions));
        return this;
    }

    /** Whether this plan has been sealed */
    public boolean isSealed() {
        return (actions == immutableActions);
    }

    @Override
    public Iterator<Action> iterator() {
        return immutableActions.iterator();
    }

    /**
     * Computes the total distance (in km) of this plan
     */
    public double totalDistance() {
        return Measures.unitsToKM(totalDistanceUnits());
    }

    /**
     * Computes the total distance (in units) of this plan
     * 
     * @see Measures
     */
    public long totalDistanceUnits() {
        return new PathLength().compute(initialCity, actions);
    }

    private static class PathLength implements ActionHandler<Void> {

        private City current;
        private long length;

        long compute(City initial, Iterable<Action> actions) {
            this.current = initial;
            this.length = 0L;
            for (Action action : actions)
                action.accept(this);
            return length;
        }

        @Override
        public Void deliver(Task task) {
            return null;
        }

        @Override
        public Void moveTo(City target) {
            length += current.distanceUnitsTo(target);
            current = target;
            return null;
        }

        @Override
        public Void pickup(Task task) {
            return null;
        }
    }

}
