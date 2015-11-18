package rabbit;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.activation.DataSource;

import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.OpenHistogram;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @Tao Lin
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {	
	//Default Values
	private static final int NUMOFINITIALRABBITS=50;
	private static final int WORLDXSIZE=20;
	private static final int WORLDYSIZE=20;
	private static final int BIRTHTHRESHOLDOFRABBITS=10;
	private static final int GROWTHRATEOFGRASS=100;
	private static final int INITIALGRASS=200;
	private static final int ENERGYGIVINGOFGLASS=2;
	private static final int ENERGYCONSUMINGOFRABBITS=1;
	
	private int numOfInitialRabbits=NUMOFINITIALRABBITS;
	private int worldXSize=WORLDXSIZE;
	private int worldYSize=WORLDYSIZE;
	private int birthThresholdOfRabbits=BIRTHTHRESHOLDOFRABBITS;
	private int growthRateOfGrass=GROWTHRATEOFGRASS;
	private int energyGivingOfGlass=ENERGYGIVINGOFGLASS;
	
	private int energyConsumingOfRabbits=ENERGYCONSUMINGOFRABBITS;
	//private int rabbitMaxEnergy=birthThresholdOfRabbits*2;
	//private int rabbitMinEnergy=birthThresholdOfRabbits;
	private int rabbitInitialEnergy=birthThresholdOfRabbits-1;
			
	private Schedule schedule;
	
	private RabbitsGrassSimulationSpace rgsSpace;
	
	private ArrayList rabbitsList;
	
	private DisplaySurface displaySurf;
	
	private OpenSequenceGraph amountOfGrassInSpace,rabbitsEnergyDistribution;
	//private OpenHistogram rabbitsEnergyDistribution;
	
	class RabbitsInSpace implements DataSource,Sequence {
		public Object execute(){
			return new Double(getSValue());
		}
		
		public double getSValue() {
			return (double)rgsSpace.getTotalRabbits();
		}
		
		
		public String getContentType() {
			return null;
		}

		public InputStream getInputStream() throws IOException {
			return null;
		}

		public String getName() {
			return null;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return null;
		}	
		
	}
	
	
	class grassInSpace implements DataSource,Sequence {
		public Object execute(){
			return new Double(getSValue());
		}
		
		public double getSValue() {
			return (double)rgsSpace.getTotalGrass();
		}
		
		
		public String getContentType() {
			return null;
		}

		public InputStream getInputStream() throws IOException {
			return null;
		}

		public String getName() {
			return null;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return null;
		}	
		
	}
	
	class rabbitsEnergy implements BinDataSource{
		public double getBinValue(Object o){
			RabbitsGrassSimulationAgent rgs = (RabbitsGrassSimulationAgent) o;
			return (double) rgs.getEnergy();
		}
	}
	
	public static void main(String[] args) {
		
		System.out.println("Rabbit skeleton");
		SimInit init=new SimInit();
		RabbitsGrassSimulationModel model=new RabbitsGrassSimulationModel();
		init.loadModel(model,"", false);
	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();	
		
		displaySurf.display();
		amountOfGrassInSpace.display();
		rabbitsEnergyDistribution.display();
	}

	private void buildModel() {
		System.out.println("Running BuildModel");
		rgsSpace = new RabbitsGrassSimulationSpace(worldXSize,worldYSize);
		rgsSpace.spreadGrass(INITIALGRASS);
		
		for(int i=0;i<numOfInitialRabbits;i++){
			addNewRabbits();
		}
		for(int i=0;i<rabbitsList.size();i++){
			RabbitsGrassSimulationAgent rgs=(RabbitsGrassSimulationAgent) rabbitsList.get(i);
			rgs.report();
		}		
	}
	
	private void addNewRabbits() {
		RabbitsGrassSimulationAgent ra = new RabbitsGrassSimulationAgent(rabbitInitialEnergy);
		
		rabbitsList.add(ra);
		rgsSpace.addRabbits(ra);		
	}

	private void buildSchedule() {
		System.out.println("Running BuildSchedule");
		
		class RabbitsGrassStep extends BasicAction{
			public void execute() {
				SimUtilities.shuffle(rabbitsList);
				
				rgsSpace.spreadGrass(growthRateOfGrass);
				for(int i=0;i<rabbitsList.size();i++){
					RabbitsGrassSimulationAgent rgsa=(RabbitsGrassSimulationAgent) rabbitsList.get(i);
					rgsa.step(energyConsumingOfRabbits,energyGivingOfGlass);
				}
				
				int reproducedRabbits = reapReproducedRabbits();
				for(int i=0;i<reproducedRabbits;i++){
					addNewRabbits();
				}
									
				displaySurf.updateDisplay();
			}	
		}
		
		schedule.scheduleActionBeginning(0,new RabbitsGrassStep());
		
		class RabbitsGrassCountingLiving extends BasicAction{
			public void execute() {
				countLivingRabbits();
			}	
		}	
		
		schedule.scheduleActionAtInterval(10, new RabbitsGrassCountingLiving());
		
		class RabbitsGrassUpdateGrassInSpace extends BasicAction{
			public void execute(){
				amountOfGrassInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateGrassInSpace());
		
		class RabbitsGrassUpdateRabbitsEnergy extends BasicAction{
			public void execute(){
				rabbitsEnergyDistribution.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateRabbitsEnergy());
		
	}

	private void buildDisplay() {
		System.out.println("Running BuildingDisplay");
		
		ColorMap map=new ColorMap();
		
		map.mapColor(0,Color.black);  //Blackground
		map.mapColor(1,Color.red);	  //Rabbits
		map.mapColor(2,Color.green);  //Grass
		
		Value2DDisplay displayGrass = new Value2DDisplay(rgsSpace.getCurrentGrassSpace(), map);
		
		Object2DDisplay displayRabbits = new Object2DDisplay(rgsSpace.getCurrentRabbitsSpace());
		displayRabbits.setObjectList(rabbitsList);
		
		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");
		
		amountOfGrassInSpace.addSequence("Grass In Space", new grassInSpace());
		rabbitsEnergyDistribution.addSequence("Rabbitss In Space", new RabbitsInSpace());
		//createHistogramItem("Rabbits Energy", rabbitsList, new rabbitsEnergy());
	}

	public String[] getInitParam() {
		String[] initParams={"numOfInitialRabbits","WorldXSize","WorldYSize","BirthThresholdOfRabbits","GrowthRateOfGrass","EnergyGivingOfGlass"};
		return initParams;
	}

	public String getName() {
		
		return "RabbitsGrass Demo @ TL";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setup() {
		System.out.println("Running setup");
		rgsSpace = null;
		rabbitsList = new ArrayList();
		schedule = new Schedule(1);
		
		//Tear Down Displays
		if(displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;
		
		if(amountOfGrassInSpace!=null){
			amountOfGrassInSpace.dispose();
		}
		amountOfGrassInSpace=null;
		
		if(rabbitsEnergyDistribution!=null){
			rabbitsEnergyDistribution.dispose();
		}
		rabbitsEnergyDistribution=null;
		
		// Create Displays
		displaySurf = new DisplaySurface(this, "RabbitsGrass Model Window");
	    amountOfGrassInSpace = new OpenSequenceGraph("Amount Of Grass In Space",this);
	    rabbitsEnergyDistribution= new OpenSequenceGraph("Rabbits Energy",this);
	    //rabbitsEnergyDistribution = new OpenHistogram("Rabbits Energy",8,0);
	    
	    // Register Displays
	    registerDisplaySurface("RabbitsGrass Model Window",displaySurf);
	    this.registerMediaProducer("Plot", amountOfGrassInSpace);	
	    this.registerMediaProducer("Plot", rabbitsEnergyDistribution);	
	}
	
	public int getNumOfInitialRabbits(){
		return numOfInitialRabbits;
	}
	
	public void setNumOfInitialRabbits(int na){
		numOfInitialRabbits=na;
	}
	
	public int getWorldXSize(){
		return worldXSize;
	}
	
	public void setWorldXSize(int wxs){
		worldXSize=wxs;
	}
	
	public int getWorldYSize(){
		return worldYSize;
	}
	
	public void setWorldYSize(int wys){
		worldYSize=wys;
	}
	
	public int getBirthThresholdOfRabbits(){
		return birthThresholdOfRabbits;
	}
	
	public void setBirthThresholdOfRabbits(int btr){
		birthThresholdOfRabbits=btr;
	}
	
	public int getGrowthRateOfGrass(){
		return growthRateOfGrass;
	}
	
	public void setGrowthRateOfGrass(int grg){
		growthRateOfGrass=grg;
	}
	
	public int getEnergyGivingOfGlass(){
		return energyGivingOfGlass;
	}
	
	public void setEnergyGivingOfGlass(int egg){
		energyGivingOfGlass=egg;
	}
	
	private int countLivingRabbits(){
		int livingRabbits = 0;
		for(int i=0;i<rabbitsList.size();i++){
			RabbitsGrassSimulationAgent rgsa=(RabbitsGrassSimulationAgent) rabbitsList.get(i);
			if(rgsa.getEnergy()>0)  livingRabbits++;
		}
		System.out.println("Number of living rabbits is: "+livingRabbits);
		
		return livingRabbits;		
	}
	
	private int reapReproducedRabbits(){
		int count=0;
		for (int i=(rabbitsList.size()-1);i>=0;i--){
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) rabbitsList.get(i);
			if (rgsa.getEnergy()>=birthThresholdOfRabbits){
				//System.out.println(new Integer(1111));
				//System.out.println(rgsa.getEnergy());
				rgsa.changeEnergy(birthThresholdOfRabbits);	
				//System.out.println(rgsa.getEnergy());
				count++;
				//System.out.println(new Integer(1112));
				
			}
			else if (rgsa.getEnergy()<1){
				rgsSpace.removeRabbitsAt(rgsa.getX(), rgsa.getY());
				rabbitsList.remove(i);
				//System.out.println(new Integer(3333));
				//System.out.println(rabbitsList.size());
			}
		}
		return count;
	}	
		
}
