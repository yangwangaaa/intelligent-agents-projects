package template;

import java.util.List;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import org.jblas.DoubleMatrix;

public class Computation {
	public DoubleMatrix computeMatrix(Topology topology, TaskDistribution td, Agent agent, Double discount){
		
		int numCities = topology.size();
		int numActions = numCities + 1;
		int numStates = numCities * numCities;
		List<City> cities = topology.cities();
		
		DoubleMatrix mprobability = new DoubleMatrix(numCities, numCities);
		double[][][] tranTable = new double[numStates][numStates][numActions];
		
		DoubleMatrix mreward = new DoubleMatrix(numCities, numCities);
		DoubleMatrix R = new DoubleMatrix(numStates, numActions);
		
		//=============== get p(i, j) and r(i, j)=================
		for(int i = 0; i < numCities; i++){
			double taskProb = 0;
			for(int j = 0; j < numCities; j++){
				double prob = td.probability(cities.get(i), cities.get(j));
				double rwd = td.reward(cities.get(i), cities.get(j));
				mreward.put(i, j, rwd);
				mprobability.put(i, j, prob);
				taskProb += prob;
			}
			mprobability.put(i, i, 1 - taskProb);
		}
		
		//================== get R(s,a)====================
		for(int i = 0; i < numStates; i ++){
			int fromcity = i/numCities;
			
			// if in state (i, i), i.e. no delivery tasks, set reward to Double.NEGATIVE_INFINITY
			if(i % (numCities + 1) == 0)
				R.put(i, 0, Double.NEGATIVE_INFINITY);
			else
				R.put(i, 0, mreward.get(fromcity, i%numCities));

			for(int j = 1; j < numActions; j++){
				if(cities.get(fromcity).hasNeighbor(cities.get(j-1)))
					R.put(i, j, 0);
				else
					R.put(i, j, Double.NEGATIVE_INFINITY);
			}
		}
		//================== get city's neighbors====================
		double [][]neighbor=new double[numCities][numCities+1];
		for(int i=0;i<numCities;i++){
			for(int j=0;j<numCities;j++){
				if(cities.get(i).hasNeighbor(cities.get(j))){
					neighbor[i][j]=1;
					neighbor[i][numCities]=neighbor[i][numCities]+neighbor[i][j];
				}
			}
		}
		//================== get T(s,a,s')====================
		for(int i=0;i<numStates;i++){
			int fromcity=i/numCities;
			int tocity=i%numCities;
			if(i%(numCities+1)!=0){	
				for(int j=(tocity*numCities+1);j<tocity*(numCities+1);j++){
					//give the T value of pick up
					tranTable[i][j][0]=mprobability.get(fromcity,tocity);
					//give the T value of movement
					for(int k = 1; k < numActions; k++){
						tranTable[i][j][k]=neighbor[fromcity][k-1]/neighbor[fromcity][numCities];
					}
				}
			}
		}
		
		//============ RLA algorithm to obtain V(s)===========
		DoubleMatrix Vlast = new DoubleMatrix(numStates);
		DoubleMatrix Vcurrent = new DoubleMatrix(numStates);
		DoubleMatrix BestActions = new DoubleMatrix(numStates);

		// initialize Vlast
		for(int s = 0; s < numStates; s++)	{
			Vcurrent.put(s, 0);
			Vlast.put(s, 0);
		}
		int count=0;
		// RLA algorithm
		while(true){
			count++;
			for(int s = 0; s < numStates; s++){
				Double max = Double.NEGATIVE_INFINITY, tmp = Double.NEGATIVE_INFINITY;
				for(int a = 0; a < numActions; a++){
					int fromCity = s/numCities, toCity;
					if(a == 0) toCity = s % numCities;
					else toCity = a - 1;

					// calculate T(s, a, s')*V(s')
					Double sumV = 0.0;
					for(int ss = 0; ss < numCities; ss++){
						sumV += mprobability.get(toCity, ss) * Vlast.get(toCity*numCities + ss);
					}
					
					tmp = R.get(s, a) + discount * sumV - agent.vehicles().get(0).costPerKm() * cities.get(fromCity).distanceTo(cities.get(toCity));

					if(tmp > max){
						max = tmp;
						BestActions.put(s, a);
					}
				}
				Vcurrent.put(s, max);
			}

			// test if the algorithm has converged
			int k = 0;
			for(k = 0; k < numStates; k++)
				if(Vlast.get(k) != Vcurrent.get(k)) 
					break;
			if(k == numStates)
				break;
			Vlast.copy(Vcurrent);
		}
		System.out.println("The Algorithm converged after "+count+" loops!");
		
		// ======print best actions for each city=============
		System.out.println("--------Print out the strategy table!----------");
		for(int i = 0; i < numStates; i++){
			System.out.print(BestActions.get(i) + ",  ");
			if(i % 9 == 8)
			System.out.println();
		}
		System.out.println("--------Print out the movement situations----------");
		
		return BestActions;
	}
}
