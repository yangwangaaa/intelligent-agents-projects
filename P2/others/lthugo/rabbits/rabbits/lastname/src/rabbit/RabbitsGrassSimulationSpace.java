package rabbit;

import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @Tao Lin
 */

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid rabbitsSpace;
	
	public RabbitsGrassSimulationSpace(int xSize, int ySize){
		grassSpace = new Object2DGrid(xSize, ySize);
		rabbitsSpace = new Object2DGrid(xSize, ySize);
		
	    for(int i = 0; i < xSize; i++){
	      for(int j = 0; j < ySize; j++){
	    	  grassSpace.putObjectAt(i,j,new Integer(0));
	      }
	    }
	}
	
	public void spreadGrass(int growthRateOfGrass){
	    // Randomly place grass in grassSpace
	    for(int i = 0; i < growthRateOfGrass; i++){
	      boolean retVal=false;
	      int count = 0;
	      int countLimit=10*grassSpace.getSizeX()*grassSpace.getSizeY();
	      
		      while((retVal==false) && (count<countLimit)){
		      // Choose coordinates
		      int x = (int)(Math.random()*(grassSpace.getSizeX()));
		      int y = (int)(Math.random()*(grassSpace.getSizeY()));
	
		      // Get the value of the object at those coordinates      
		      if(isCellOccupied(x,y)==false){  	
		    	  grassSpace.putObjectAt(x,y,new Integer(2));
		    	  retVal=true;
		      }
		      count++;
		 }
	   }    
	}
	
	public Object2DGrid getCurrentGrassSpace() {
		return grassSpace;
	}
	
	public boolean isCellOccupied(int x,int y)
	{
		boolean retVal = false;
		if (rabbitsSpace.getObjectAt(x, y)!=null)  retVal = true;
		return retVal;
	}

	public boolean addRabbits(RabbitsGrassSimulationAgent ra) {
		boolean retVal = false;
		int count = 0;
		int countLimit = 10*rabbitsSpace.getSizeX()*rabbitsSpace.getSizeY();
		
		while((retVal==false) && (count<countLimit)){
			int x = (int)(Math.random()*(rabbitsSpace.getSizeX()));
			int y = (int)(Math.random()*(rabbitsSpace.getSizeY()));
			if(isCellOccupied(x,y)==false){
				rabbitsSpace.putObjectAt(x, y, ra );
				ra.setXY(x,y);
				ra.setRabbitsGrassSimulationSpace(this);
				retVal=true;
			}
			count++;
		}
		return retVal;
	}

	public Object2DGrid getCurrentRabbitsSpace() {
		return rabbitsSpace;
	}

	public void removeRabbitsAt(int x, int y) {
		rabbitsSpace.putObjectAt(x, y, null);		
	}
	
	public int takeEnergyAt(int x,int y,int energyGivingOfGlass){
		int i = 0;
		
		if(((Integer) grassSpace.getObjectAt(x,y)).intValue()==2){  	
	    	  i=i+energyGivingOfGlass;
	    	  //delete grass
	    	  grassSpace.putObjectAt(x, y, new Integer(0));
	      }
		else {
			i=0;
		}
			
		return i;
	}
	
	public boolean moveRabbitsAt(int x,int y,int newX,int newY){
		boolean retVal =false;
		if(!isCellOccupied(newX, newY)){
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) rabbitsSpace.getObjectAt(x, y);
			removeRabbitsAt(x,y);
			rgsa.setXY(newX, newY);
			rabbitsSpace.putObjectAt(newX, newY, rgsa);
		    retVal = true;
		}
		return retVal;
	}
		
	public int getTotalGrass(){
		int totalGrass=0;
		for(int i=0;i<grassSpace.getSizeX();i++){
			for(int j=0;j<grassSpace.getSizeY();j++){
				if(((Integer) grassSpace.getObjectAt(i,j)).intValue()==2)
				totalGrass+=1;
			}
		}
		return totalGrass;
	}

	public double getTotalRabbits() {
		int totalRabbits=0;
		for(int i=0;i<rabbitsSpace.getSizeX();i++){
			for(int j=0;j<rabbitsSpace.getSizeY();j++){
				if(rabbitsSpace.getObjectAt(i,j) != null)
					totalRabbits+=1;
			}
		}
		return totalRabbits;
	}
}
