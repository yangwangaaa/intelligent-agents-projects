package auction1;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {
	private City city;
	private TaskSet carried;
	private TaskSet delivered;
	private TaskSet tasks;

	public State(City city, TaskSet carried, TaskSet delivered, TaskSet tasks) {
		this.city = city;
		this.carried = carried;
		this.delivered = delivered;
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
		return city.toString() + carried.toString() + delivered.toString();
	}

	public TaskSet getTasks() {
		return tasks;
	}

	public void setTasks(TaskSet tasks) {
		this.tasks = tasks;
	}

	public City getCity() {
		return city;
	}

	public TaskSet getCarried() {
		return carried;
	}

	public TaskSet getDelivered() {
		return delivered;
	}

	public void setCity(City city) {
		this.city = city;
	}

	public void setCarried(TaskSet carried) {
		this.carried = carried;
	}

	public void setDelivered(TaskSet delivered) {
		this.delivered = delivered;
	}
}