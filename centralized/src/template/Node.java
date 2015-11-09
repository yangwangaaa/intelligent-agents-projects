package template;

import java.util.ArrayList;

public class Node {
	private ArrayList<Integer> nextAction;
	private ArrayList<Integer> time;
	private ArrayList<Integer> vehicle;
	private double OValue;

	private int Nt;
	private int Nv;
	
	public Node(ArrayList<Integer> nextAction, ArrayList<Integer> time, ArrayList<Integer> vehicle, int Nt, int Nv) {
		this.nextAction = nextAction;
		this.time = time;
		this.vehicle = vehicle;
		this.Nt = Nt;
		this.Nv = Nv;
		this.OValue = -1;
	}
	
	private Node(ArrayList<Integer> nextAction, ArrayList<Integer> time, ArrayList<Integer> vehicle, int Nt, int Nv, double v) {
		this.nextAction = nextAction;
		this.time = time;
		this.vehicle = vehicle;
		this.Nt = Nt;
		this.Nv = Nv;
		this.OValue = v;
	}
	
	public Node clone() {
		return new Node((ArrayList<Integer>)nextAction.clone(), (ArrayList<Integer>)time.clone(), (ArrayList<Integer>)vehicle.clone(), Nt, Nv, OValue);
	}
	
	
	
	public double getOValue() {
		if (OValue == -1) return this.computeOValue();
		else return OValue;
	}
	
	private double computeOValue() {
		// objective function
		double value = 0;
		
		
		
		return value;
	}
	
	public ArrayList<Integer> getNextAction() {
		return nextAction;
	}


	public void setNextAction(ArrayList<Integer> nextAction) {
		this.nextAction = nextAction;
	}


	public ArrayList<Integer> getTime() {
		return time;
	}


	public void setTime(ArrayList<Integer> time) {
		this.time = time;
	}


	public ArrayList<Integer> getVehicle() {
		return vehicle;
	}


	public void setVehicle(ArrayList<Integer> vehicle) {
		this.vehicle = vehicle;
	}
	
	public int getNt() {
		return Nt;
	}

	public void setNt(int nt) {
		Nt = nt;
	}

	public int getNv() {
		return Nv;
	}

	public void setNv(int nv) {
		Nv = nv;
	}
}
