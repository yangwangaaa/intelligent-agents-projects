package other;

import java.util.ArrayList;
import java.util.List;

import logist.simulation.Vehicle;
import logist.topology.Topology.City;

public class MyVehicle {

	private int capacity;
	private int costPerKm;
	private City currentCity;
	private int id;
	private double speed; //not used
	private Vehicle v;
	
	public MyVehicle(int capacity, int costPerKm, City currentCity, int id) {
		this.capacity = capacity;
		this.costPerKm = costPerKm;
		this.currentCity = currentCity;
		this.id = id;
		this.v = null;
	}
	
	
	
	public MyVehicle(Vehicle v) {
		this.capacity = v.capacity();
		this.costPerKm = v.costPerKm();
		this.currentCity = v.homeCity();
		this.id = v.id();
		this.v = v;
	}
	
	
	public static List<MyVehicle> transform(List<Vehicle> vl) {
		List<MyVehicle> l = new ArrayList<MyVehicle>();
		for (Vehicle v : vl) {
			l.add(new MyVehicle(v));
		}
		return l;
	}

	public String toString() {
		return "V" + id + ": capa=" + capacity + ", currentCity=" + currentCity;
	}

	public int capacity() {
		return capacity;
	}
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	public int costPerKm() {
		return costPerKm;
	}
	public void setCostPerKm(int costPerKm) {
		this.costPerKm = costPerKm;
	}
	public City getCurrentCity() {
		return currentCity;
	}
	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}
	public int id() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Vehicle getV() {
		return v;
	}

	public void setV(Vehicle v) {
		this.v = v;
	}
}
