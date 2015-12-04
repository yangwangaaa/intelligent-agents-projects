package AgentMouche;

public class Strategy {
	private double factor1;
	private double factor2;
	private double a; //in [0,0.5]
	private double uf; //start smaller than one
	private double uuf; //<=1
	private double ufl;
	private double uufl;
	private int id;

	public Strategy(double factor1, double factor2, double a, double uf, double uuf,
			double ufl, double uufl,int id) {
		super();
		this.factor1 = factor1;
		this.factor2 = factor2;
		this.a = a;
		this.uf = uf;
		this.uuf = uuf;
		this.ufl = ufl;
		this.uufl = uufl;
		this.id = id;
		
	}
	public Strategy(){//basic strategy
		factor1 = 1;
		factor2 = 1;
		a = 0;
		uf = 0;
		uuf = 0;
		ufl = 0;
		uufl = 0;
		id = 0;
	}
	


	public double estimate(double mc1, double mc2){
		if(mc1<=mc2){
			return factor1*(mc1+(mc2-mc1)/2);
		}else{
			return factor2*(mc1+(mc2-mc1)/2);
		}
		
	}

	public void update(boolean win, double bid1, double bid2, double mc1, double mc2, int proposed ){
		double f = bid1/mc2;
		
	}
	public Double estimate(Double double1, double m2) {
		// TODO Auto-generated method stub
		return null;
	}
}
