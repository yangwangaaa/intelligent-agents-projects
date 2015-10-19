package template;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {
	public City city;
	public TaskSet carriedTasks;
	public TaskSet deliveredTasks;
	public State(City city, TaskSet carriedTasks, TaskSet deliveredTasks) {
		this.city = city;
		this.carriedTasks = carriedTasks;
		this.deliveredTasks = deliveredTasks;
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