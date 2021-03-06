package template;

import java.util.Arrays;
import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class NodeT {
	private int[] nextTask;
	private int[] times;
	private int[] vehicles;
	private double OValue;

	private int Nt;
	private int Nv;

	///////////////////////////////
	//        CONSTRUCTORS       //
	///////////////////////////////

	public NodeT(List<Vehicle> vehiclesList, Task[] tasks) {
		this.Nt = tasks.length;
		this.Nv = vehiclesList.size();
		this.nextTask = new int[Nv+Nt];
		this.times = new int[Nt];
		this.vehicles = new int[Nt];
		Arrays.fill(this.nextTask, -1);
	}


	public NodeT(int[] nextTask, int[] times, int[] vehicles, int Nt, int Nv) {
		this.nextTask = nextTask;
		this.times = times;
		this.vehicles = vehicles;
		this.Nt = Nt;
		this.Nv = Nv;
		this.OValue = -1;
	}

	private NodeT(int[] nextTask, int[] times, int[] vehicles, int Nt, int Nv, double v) {
		this.nextTask = nextTask;
		this.times = times;
		this.vehicles = vehicles;
		this.Nt = Nt;
		this.Nv = Nv;
		this.OValue = v;
	}

	///////////////////////////////
	//           UTILS           //
	///////////////////////////////

	public NodeT clone() {
		return new NodeT((int[]) nextTask.clone(), (int[])times.clone(), (int[])vehicles.clone(), Nt, Nv, OValue);
	}


	public double getOValue(Task[] tasks, List<Vehicle> vehicles) {
		if (OValue == -1) return this.computeOValue(tasks, vehicles);
		else return OValue;
	}

	// vladman
	private double computeOValue(Task[] tasks, List<Vehicle> vehicles) {
		// objective function
		double C = 0;

		for (int t = 0; t<Nt; t++) {
			if (this.nextTask(t)!=-1) {
				Task t1 = tasks[t];
				Task t2 = tasks[this.nextTask(t)];
				Vehicle vehicle = vehicles.get(this.getVehicle(t));
				City delivery1 = t1.deliveryCity;
				City pickUp2 = t2.pickupCity;
				City delivery2 = t2.deliveryCity;
				C += ( delivery1.distanceTo(pickUp2) + pickUp2.distanceTo(delivery2) )*vehicle.costPerKm();
			}
		}


		for (int v = 0; v<Nv; v++) {
			if (this.nextTask(v+Nt)!=-1) {
				Task firstTask = tasks[this.nextTask(v+Nt)];
				Vehicle vehicle = vehicles.get(v);
				City vCity = vehicle.getCurrentCity();
				City pickUp = firstTask.pickupCity;
				City delivery = firstTask.deliveryCity;
				C += ( vCity.distanceTo(pickUp) + pickUp.distanceTo(delivery) )*vehicle.costPerKm();
			}
		}

		this.OValue = C;
		return C;
	}

	public void nextTask(int index, int value) {
		nextTask[index] = value;
		this.OValue = -1;
	}

	public int nextTask(int index) {
		return nextTask[index];
	}

	public void setTime(int index, int value) {
		times[index] = value;
		this.OValue = -1;
	}

	public int getTime(int index) {
		return times[index];
	}

	public void setVehicle(int index, int value) {
		vehicles[index] = value;
		this.OValue = -1;
	}

	public int getVehicle(int index) {
		return vehicles[index];
	}

	public void print() {
		int max = 27;

		System.out.println("Objective function value = " + OValue);
		System.out.println("- Nt = " + Nt);

		System.out.println("- Nv = " + Nv);

		int lineReturn = 0;
		System.out.print("- nextTask = [");
		for(int i=0; i<Nt; i++) {
			System.out.print("T"+ i + ":" + nextTask[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		for(int i=0; i<Nv; i++) {
			if(i==Nv-1) System.out.print("V"+ i + ":" + nextTask[i+Nt]);
			else System.out.print("V"+ i + ":" + nextTask[i+Nt] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		System.out.println("]");

		lineReturn = 0;
		System.out.print("- times = [");
		for(int i=0; i<Nt; i++) {
			if(i==Nt-1) System.out.print("T"+ i + ":" + times[i]);
			else System.out.print("T"+ i + ":" + times[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                   ");
			}
		}
		System.out.println("]");

		lineReturn = 0;
		System.out.print("- vehicles = [");
		for(int i=0; i<Nt; i++) {
			if(i==Nt-1) System.out.print("T"+ i + ":" + vehicles[i]);
			else System.out.print("T"+ i + ":" + vehicles[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                      ");
			}
		}
		System.out.println("]");
	}

	///////////////////////////////
	//          GET-SET          //
	///////////////////////////////

	public int[] getNextTask() {
		return nextTask;
	}


	public void setNextTask(int[] nextTask) {
		this.OValue = -1;
		this.nextTask = nextTask;
	}


	public int[] getTimes() {
		return times;
	}


	public void setTimes(int[] times) {
		this.OValue = -1;
		this.times = times;
	}


	public int[] getVehicles() {
		return vehicles;
	}


	public void setVehicles(int[] vehicles) {
		this.OValue = -1;
		this.vehicles = vehicles;
	}

	public int getNt() {
		return Nt;
	}

	public void setNt(int nt) {
		this.OValue = -1;
		Nt = nt;
	}

	public int getNv() {
		return Nv;
	}

	public void setNv(int nv) {
		this.OValue = -1;
		Nv = nv;
	}
}
