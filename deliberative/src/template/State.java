package template;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {
	private City city;
	private TaskSet carriedTasks;
	private TaskSet deliveredTasks;
	private int availableCapacity;
	
	public State(City city, TaskSet carriedTasks, TaskSet deliveredTasks, int availableCapacity) {
		this.city = city;
		this.carriedTasks = carriedTasks;
		this.deliveredTasks = deliveredTasks;
		this.availableCapacity = availableCapacity;
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