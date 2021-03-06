package sls;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;
import other.MyVehicle;

public class NodePD implements Comparator<NodePD>, Comparable<NodePD>{
	private int[] nextAction;
	private int[] previousAction;
	private int[] times;
	private int[] vehicles;
	private int[] load;
	private double OValue;
	private List<MyVehicle> vehiclesList;
	private Task[] tasks;

	private int Nt;
	private int Nv;
	private int Na;

	///////////////////////////////
	//        CONSTRUCTORS       //
	///////////////////////////////

	public NodePD(List<MyVehicle> vehiclesList, Task[] tasks) {
		this.tasks = tasks;
		this.vehiclesList = vehiclesList;
		this.Nt = tasks.length;
		this.Nv = vehiclesList.size();
		this.Na = 2*Nt;
		this.nextAction = new int[Nv+Na];
		this.previousAction = new int[Nv+Na];
		this.times = new int[Na];
		this.vehicles = new int[Na];
		this.load = new int[Na];
		Arrays.fill(this.nextAction, -1);
		Arrays.fill(this.previousAction, -1);
	}


	public NodePD(int[] nextTask, int[] previousTask, int[] times, int[] vehicles, int[] load, int Nt, int Nv, List<MyVehicle> vehiclesList, Task[] tasks) {
		this.tasks = tasks;
		this.vehiclesList = vehiclesList;
		this.nextAction = nextTask;
		this.previousAction = previousTask;
		this.times = times;
		this.vehicles = vehicles;
		this.load = load;
		this.Nt = Nt;
		this.Nv = Nv;
		this.Na = 2*Nt;
		this.OValue = -1;
	}

	private NodePD(int[] nextTask, int[] previousTask, int[] times, int[] vehicles, int[] load, int Nt, int Nv, List<MyVehicle> vehiclesList, Task[] tasks, double v) {
		this.tasks = tasks;
		this.vehiclesList = vehiclesList;
		this.nextAction = nextTask;
		this.previousAction = previousTask;
		this.times = times;
		this.vehicles = vehicles;
		this.load = load;
		this.Nt = Nt;
		this.Nv = Nv;
		this.Na = 2*Nt;
		this.OValue = v;
	}

	///////////////////////////////
	//           UTILS           //
	///////////////////////////////

	public boolean equals(NodePD n){
		return (this.getOValue() == n.getOValue());
	}

	@Override
	public int compareTo(NodePD o) {
		return compare(this,o);
	}
	
	@Override
	public int compare(NodePD n1, NodePD n2) {
		return (int) -(n1.getOValue() - n2.getOValue());
	}
	
	public NodePD clone() {
		return new NodePD((int[]) nextAction.clone(), (int[]) previousAction.clone(), (int[])times.clone(), (int[])vehicles.clone(), (int[])load.clone(), Nt, Nv, vehiclesList, tasks, OValue);
	}


	public double getOValue(Task[] tasks, List<MyVehicle> vehicles) {
		if (OValue == -1) return this.computeOValue(tasks, vehicles);
		else return OValue;
	}
	
	public double getOValue() {
		if (OValue == -1) return this.computeOValue(tasks, vehiclesList);
		else return OValue;
	}

	// vladman
	private double computeOValue(Task[] tasks, List<MyVehicle> vehicles) {

		// objective function
		double C = 0;

		for (int t = 0; t<Na; t++) {
			if (this.nextAction(t)!=-1) {
				Task t1 = tasks[t%Nt];
				Task t2 = tasks[this.nextAction(t)%Nt];
				MyVehicle vehicle = vehicles.get(this.getVehicle(t));
				City c1 = t1.pickupCity;
				if(t>=Nt) c1 = t1.deliveryCity;
				City c2 = t2.pickupCity;
				if(this.nextAction(t)>=Nt) c2 = t2.deliveryCity;
				C += c1.distanceTo(c2)*vehicle.costPerKm();
			}
		}


		for (int v = 0; v<Nv; v++) {
			if (this.nextAction(v+Na)!=-1) {
				Task firstTask = tasks[this.nextAction(v+Na)];
				MyVehicle vehicle = vehicles.get(v);
				City vCity = vehicle.getCurrentCity();
				City pickUp = firstTask.pickupCity;
				C += vCity.distanceTo(pickUp)*vehicle.costPerKm();
			}
		}

		this.OValue = C;
		return C;
	}

	public void nextAction(int index, int value) {
		nextAction[index] = value;
		this.OValue = -1;
	}

	public int nextAction(int index) {
		return nextAction[index];
	}
	
	public void previousAction(int index, int value) {
		previousAction[index] = value;
		this.OValue = -1;
	}

	public int previousAction(int index) {
		return previousAction[index];
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
	
	public void setLoad(int index, int value) {
		load[index] = value;
		this.OValue = -1;
	}

	public int getLoad(int index) {
		return load[index];
	}

	public void print() {
		
		// TODO
		int max = 27;

		System.out.println("Objective function value = " + OValue);
		System.out.println("- Nt = " + Nt);

		System.out.println("- Nv = " + Nv);

		int lineReturn = 0;
		System.out.print("- nextAction = [");
		for(int i=0; i<Nt; i++) {
			System.out.print("P"+ i + ":" + nextAction[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		for(int i=Nt; i<Na; i++) {
			System.out.print("D"+ i + ":" + nextAction[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		for(int i=0; i<Nv; i++) {
			if(i==Nv-1) System.out.print("V"+ i + ":" + nextAction[i+Na]);
			else System.out.print("V"+ i + ":" + nextAction[i+Na] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		System.out.println("]");
		
		lineReturn = 0;
		System.out.print("- previousAction = [");
		for(int i=0; i<Nt; i++) {
			System.out.print("P"+ i + ":" + previousAction[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		for(int i=Nt; i<Na; i++) {
			System.out.print("D"+ i + ":" + previousAction[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		for(int i=0; i<Nv; i++) {
			if(i==Nv-1) System.out.print("V"+ i + ":" + previousAction[i+Na]);
			else System.out.print("V"+ i + ":" + previousAction[i+Na] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                       ");
			}
		}
		System.out.println("]");

		lineReturn = 0;
		System.out.print("- times = [");
		for(int i=0; i<Na; i++) {
			if(i==Na-1) System.out.print("T"+ i + ":" + times[i]);
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
		for(int i=0; i<Na; i++) {
			if(i==Na-1) System.out.print("T"+ i + ":" + vehicles[i]);
			else System.out.print("T"+ i + ":" + vehicles[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                      ");
			}
		}
		System.out.println("]");
		
		lineReturn = 0;
		System.out.print("- loads = [");
		for(int i=0; i<Na; i++) {
			if(i==Na-1) System.out.print("T"+ i + ":" + load[i]);
			else System.out.print("T"+ i + ":" + load[i] + ", ");
			lineReturn++;
			if(lineReturn%max==0) {
				System.out.println("");
				System.out.print("                   ");
			}
		}
		System.out.println("]");
	}

	///////////////////////////////
	//          GET-SET          //
	///////////////////////////////

	public int[] getNextAction() {
		return nextAction;
	}


	public void setNextAction(int[] nextAction) {
		this.OValue = -1;
		this.nextAction = nextAction;
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


	public int[] getPreviousAction() {
		return previousAction;
	}


	public void setPreviousAction(int[] previousAction) {
		this.previousAction = previousAction;
	}


	public int[] getLoad() {
		return load;
	}


	public void setLoad(int[] load) {
		this.load = load;
	}
	
	
}
