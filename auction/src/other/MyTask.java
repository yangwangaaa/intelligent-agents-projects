package other;

import java.util.ArrayList;
import java.util.Comparator;

import logist.task.Task;
import logist.topology.Topology.City;


public class MyTask implements Comparator<MyTask>, Comparable<MyTask>{
	private City deliveryCity;
	private int id;
	private City pickupCity;
	private long reward;
	private int weight;
	private double proba;
	
	public MyTask(int id, City pickupCity, City deliveryCity, long reward,
			int weight, double proba) {
		super();
		this.deliveryCity = deliveryCity;
		this.id = id;
		this.pickupCity = pickupCity;
		this.reward = reward;
		this.weight = weight;
		this.proba = proba;
	}
	
	public boolean notIn(ArrayList<Task> tasks) {
		for(Task t: tasks) {
			if(t.pickupCity.equals(pickupCity) && t.deliveryCity.equals(deliveryCity)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean equals(MyTask n){
		return (this.id == n.getId());
	}

	@Override
	public int compareTo(MyTask o) {
		return compare(this,o);
	}
	
	@Override
	public int compare(MyTask n1, MyTask n2) {
		double c = n1.getProba() - n2.getProba();
		if(c<0) return 1;
		if(c>0) return -1;
		else return 0;
	}
	
	public Task generateTask(int id) {
		return new Task(id, pickupCity, deliveryCity, reward, weight);
	}
	
	
	public City getDeliveryCity() {
		return deliveryCity;
	}
	public void setDeliveryCity(City deliveryCity) {
		this.deliveryCity = deliveryCity;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public City getPickupCity() {
		return pickupCity;
	}
	public void setPickupCity(City pickupCity) {
		this.pickupCity = pickupCity;
	}
	public long getReward() {
		return reward;
	}
	public void setReward(long reward) {
		this.reward = reward;
	}
	public int getWeight() {
		return weight;
	}
	public void setWeight(int weight) {
		this.weight = weight;
	}
	public double getProba() {
		return proba;
	}
	public void setProba(double proba) {
		this.proba = proba;
	}
}
