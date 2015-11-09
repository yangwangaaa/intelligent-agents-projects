package template;

public class Node {
	private int[] nextAction;
	private int[] times;
	private int[] vehicles;
	private double OValue;

	private int Nt;
	private int Nv;
	
	///////////////////////////////
	//        CONSTRUCTORS       //
	///////////////////////////////
	
	public Node(int[] nextAction, int[] times, int[] vehicles, int Nt, int Nv) {
		this.nextAction = nextAction;
		this.times = times;
		this.vehicles = vehicles;
		this.Nt = Nt;
		this.Nv = Nv;
		this.OValue = -1;
	}
	
	private Node(int[] nextAction, int[] times, int[] vehicles, int Nt, int Nv, double v) {
		this.nextAction = nextAction;
		this.times = times;
		this.vehicles = vehicles;
		this.Nt = Nt;
		this.Nv = Nv;
		this.OValue = v;
	}
	
	///////////////////////////////
	//           UTILS           //
	///////////////////////////////
	
	public Node clone() {
		return new Node((int[]) nextAction.clone(), (int[])times.clone(), (int[])vehicles.clone(), Nt, Nv, OValue);
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
	
	public void setAction(int index, int value) {
		nextAction[index] = value;
	}
	
	public int getAction(int index) {
		return nextAction[index];
	}
	
	public void setTime(int index, int value) {
		times[index] = value;
	}
	
	public int getTime(int index) {
		return times[index];
	}
	
	public void setVehicle(int index, int value) {
		vehicles[index] = value;
	}
	
	public int getVehicle(int index) {
		return vehicles[index];
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
}
