package AgentMouche;

public class Strategy {
	private double factor;
	private double a; //in [0,0.5]
	private double uf; //start smaller than one
	private double uuf; //<=1
	private double ufl;
	private double uufl;
	private int id;

	public Strategy(double factor, double a, double uf, double uuf,
			double ufl, double uufl,int id) {
		super();
		this.factor = factor;
		this.a = a;
		this.uf = uf;
		this.uuf = uuf;
		this.ufl = ufl;
		this.uufl = uufl;
		this.id = id;
		
	}
	public Strategy(){//basic strategy
		factor = 0.99;
		a = 0;
		uf = 0;
		uuf = 0;
		ufl = 0;
		uufl = 0;
		id = 0;
	}
	

	public double estimate(double mc1, double mc2){
		return mc2*factor;
	}

	public void update(boolean win, double bid1, double bid2, double mc1, double mc2, int proposed ){
		double f = bid1/mc2;
		
	}
}
