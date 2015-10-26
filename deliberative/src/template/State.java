package template;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {
	private City city;
	private TaskSet carriedTasks;
	private TaskSet deliveredTasks;
	private TaskSet tasks;
	private int availableCapacity;
	
	public State(City city, TaskSet carriedTasks, TaskSet deliveredTasks, TaskSet tasks, int availableCapacity) {
		this.city = city;
		this.carriedTasks = carriedTasks;
		this.deliveredTasks = deliveredTasks;
		this.availableCapacity = availableCapacity;
		this.tasks = tasks;
	}
	
	@Override
    public int hashCode() {
        return this.toString().hashCode();
    }
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        State s = (State) o;
	    return this.toString().equals(s.toString());
	}
	
	@Override
    public String toString() {
		return city.toString() + carriedTasks.toString() + deliveredTasks.toString() + availableCapacity;
    }
		
	public TaskSet getTasks() {
		return tasks;
	}

	public void setTasks(TaskSet tasks) {
		this.tasks = tasks;
	}

	public int getAvailableCapacity() {
		return availableCapacity;
	}

	public void setAvailableCapacity(int availableCapacity) {
		this.availableCapacity = availableCapacity;
	}

	public City getCity() {
		return city;
	}
	
	public TaskSet getCarriedTasks() {
		return carriedTasks;
	}
	
	public TaskSet getDeliveredTasks() {
		return deliveredTasks;
	}
	
	public void setCity(City city) {
		this.city = city;
	}
	
	public void setCarriedTasks(TaskSet carriedTasks) {
		this.carriedTasks = carriedTasks;
	}
	
	public void setDeliveredTasks(TaskSet deliveredTasks) {
		this.deliveredTasks = deliveredTasks;
	}
}