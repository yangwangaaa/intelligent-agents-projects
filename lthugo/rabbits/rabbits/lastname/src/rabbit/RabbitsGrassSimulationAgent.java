package rabbit;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @Tao Lin
 */

public class RabbitsGrassSimulationAgent implements Drawable{
	
	private int energy;
	private int x;
	private int y;
	private int vX;
	private int vY;
	private static int IDNumber=0;
	private int ID;
	private RabbitsGrassSimulationSpace rgssSpace;

	public RabbitsGrassSimulationAgent(int initialEnergy) {
		//energy = (int)((Math.random()*(maxEnergy-minEnergy))+minEnergy);
		energy=initialEnergy;
		setVxVy();
		IDNumber++;
		ID=IDNumber;
	}

	public void setXY(int newX,int newY){
		x=newX;
		y=newY;
	}
	
	public void draw(SimGraphics G){
		G.drawFastRoundRect(Color.red);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public String getID(){
		return "A-"+ID;
	}
	
	public int getEnergy(){		
		return energy;
	}
	
	public void changeEnergy(int birthThresholdOfRabbits){
		energy=energy-birthThresholdOfRabbits;
	}
	
	public void report() {
		System.out.println(getID()+" at ("+x+","+y+") has "+getEnergy()+" energy.");	
	}

	public void step(int energyConsumingOfRabbits, int energyGivingOfGlass) {
		int newX=x+vX;
		int newY=y+vY;
		
		//Eating the grass that produced on the place of rabbits (before movement).
		Object2DGrid grid=rgssSpace.getCurrentRabbitsSpace();
		newX = (newX+grid.getSizeX()) % grid.getSizeX();
		newY = (newY+grid.getSizeY()) % grid.getSizeY();
		
		energy=energy+rgssSpace.takeEnergyAt(x,y,energyGivingOfGlass);
		
		if(tryMove(newX,newY)){
			energy=energy+rgssSpace.takeEnergyAt(newX,newY,energyGivingOfGlass);
		}
		else{
			setVxVy();
		}

			

		energy=energy-energyConsumingOfRabbits;	
		
			report();

		
	}
	
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgss){
		rgssSpace=rgss;
	}
	
	private void setVxVy(){
		vX=0;
		vY=0;
		while((Math.abs(vX))==Math.abs(vY)){
			vX= (int) Math.floor(Math.random()*3)-1;
			vY= (int) Math.floor(Math.random()*3)-1;
		}
		//System.out.println(vX);
		//System.out.println(vY);

	}
	
	private boolean tryMove(int newX,int newY){
		return rgssSpace.moveRabbitsAt(x,y,newX,newY);
	}
}
