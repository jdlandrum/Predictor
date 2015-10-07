/**
 * 
 */
package com.predictor.optimization;

import java.util.ArrayList;
import java.util.Random;

import com.predictor.genes.BinaryGene;
import com.predictor.genes.EquationGene;
import com.predictor.tools.EquationHelper;

/**
 * @author jlandrum
 *
 */
public class GeneticOptimizer implements Runnable {

	private GeneticBatchExecutor myParent;
	private boolean hasData;
	
	private ArrayList<ArrayList<BinaryGene>> myValues;
	private ArrayList<Character> myVariables;
	private ArrayList<Double> fitnessValues;
	private int maxGenesInValue;
	private int numVariables;
	private int extraSelectionWeight = 3;
	private boolean haveVariables;
	private boolean haveValues;
	
	private ArrayList<EquationGene> myEquations;
	private int activeEquation;
	private ArrayList<Double> equationFitness;
	private int equationPopulation;
	private boolean haveEquation;
	private int maxGenesInEquation;
	
	//these are the 1-in-N chances of crossover or mutation
	private double crossoverChance;	//random set during creation
	private int mutationChance;		//randomly set during creation (50% mutation chances will be pretty messed up though)
	
	private int minSeeds;
	private int seedTargetMultiplier = 1;
	private double fitnessDeviations;
	private int minCycles;
	private int maxCycles;
	private double fitnessTarget;
	private int decimateCount;
	private int fitnessRepeats;
	
	private String myName;
	private boolean keepRunning;
	private boolean started;
	private Thread myThread;
	
	private Object valuesLock;
	private Object variablesLock;
	private Object fitnessLock;
	private Object equationLock;
	
	public GeneticOptimizer() {
		this(250, 1, 10, 250, 0.9);
	}
	
	/**
	 * Constructor.
	 * @param newMinSeeds
	 * @param newFitnessDeviations
	 * @param newMinCycles
	 * @param newMaxCycles
	 * @param newFitnessTarget
	 */
	public GeneticOptimizer(int newMinSeeds, double newFitnessDeviations, int newMinCycles, int newMaxCycles, double newFitnessTarget) {
		this(newMinSeeds, newFitnessDeviations, newMinCycles, newMaxCycles, newFitnessTarget, "GeneralGeneticOptimizer", null);
	}
	
	public GeneticOptimizer(int newMinSeeds, double newFitnessDeviations, int newMinCycles, int newMaxCycles, double newFitnessTarget, String aName, GeneticBatchExecutor aParent) {
		myParent = aParent;
		
		minSeeds = newMinSeeds;
		fitnessDeviations = newFitnessDeviations;
		minCycles = newMinCycles;
		maxCycles = newMaxCycles;
		fitnessTarget = newFitnessTarget;
		
		myValues = new ArrayList<ArrayList<BinaryGene>>();
		myVariables = new ArrayList<Character>();
		fitnessValues = new ArrayList<Double>();
		numVariables = 0;
		haveVariables = false;
		haveValues = false;
		maxGenesInValue = 32;
		
		myEquations = new ArrayList<EquationGene>();
		activeEquation = 0;
		equationFitness = new ArrayList<Double>();
		haveEquation = false;
		maxGenesInEquation = 32;
		equationPopulation = newMinSeeds;
		decimateCount = 0;
		fitnessRepeats = 0;
		
		hasData = false;
		
		//set up the two types of mutations chances of occurring
		//crossover chance is 1/DOUBLE because this will generate values 1.0->2 50% of the time, which is what we typically want
		crossoverChance = 1 / new Random().nextDouble();
		//mutation chance needs to be 32-256
		mutationChance = new Random().nextInt(235) + 32;
		
		valuesLock = new Object();
		variablesLock = new Object();
		fitnessLock = new Object();
		equationLock = new Object();
		
		myName = aName;
		keepRunning = true;
		started = false;
		myThread = new Thread(this, aName);
		myThread.start();
		
		seedEquations();
	}
	
	public boolean isRunning() {
		return keepRunning;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void forceStop() {
		keepRunning = false;
	}
	
	protected void shutdownSafely() {
		return;
	}
	
	protected BinaryGene generateRandom(int ceiling, int numGenes) {
		double result=0;
		Random generator = new Random();
		
		//generate a random number
		result = generator.nextDouble();
		
		//multiply or divide by random multiple of 10 (up to 10^(this.maxGenesInValue/2))
		if(generator.nextBoolean()) {
			result = result * Math.pow(10, generator.nextInt((int)(this.maxGenesInValue/2)));
		} else {
			result = result / Math.pow(10, generator.nextInt((int)(this.maxGenesInValue/2)));
		}
		
		//ensure it's smaller than our ceiling
		result = result % (ceiling+1);
		
		//randomly make negative
		if(generator.nextBoolean()) {
			result *= -1;
		}
		
		return new BinaryGene(result, numGenes);
	}
	
	public boolean buildValues() {
		return this.buildValues(this.maxGenesInValue);
	}
	
	public boolean buildValues(int max) {
		return this.buildValues(max, minSeeds*seedTargetMultiplier, null);
	}
	
	public boolean buildValuesWithSeed(ArrayList<Double> seedValues) {
		ArrayList<ArrayList<Double>> temp = new ArrayList<ArrayList<Double>>();
		temp.add(seedValues);
		return this.buildValues(this.maxGenesInValue, minSeeds*seedTargetMultiplier, temp);
	}
	
	public boolean buildValuesWithSeeds(ArrayList<ArrayList<Double>> seedValues) {
		return this.buildValues(this.maxGenesInValue, minSeeds*seedTargetMultiplier, seedValues);
	}
	
	public boolean buildValues(int max, int num) {
		return this.buildValues(max, num, null);
	}
	
	public boolean buildValues(int max, int num, ArrayList<ArrayList<Double>> seeds) {
		ArrayList<BinaryGene> row = null;
		ArrayList<BinaryGene> seed = null;
//		if(!haveVariables || haveValues) {
		if(!haveVariables) {
			return false;
		}
		
		//if this is the first run, GENERATE MOAR SEEDS!
		if(!haveValues) {
			if(seedTargetMultiplier > 1) {
				num = num * seedTargetMultiplier;
			} else {
				num = num * 10;
			}
		}
		
		if(seeds == null || seeds.size() == 0) {

			synchronized(valuesLock) {
				for(int i=0;i<num;i++) {
					row = new ArrayList<BinaryGene>();
					fitnessValues.add(new Double(0));
					for(int j=0;j<numVariables;j++) {
						row.add(generateRandom(max, maxGenesInValue));
					}
					myValues.add(row);
				}
				haveValues = true;
			}
		} else if(seeds != null && seeds.size() > 0) {
//			System.out.println(myThread.getName() + " seeding with " + seeds);
			//we have specific seeds to start with -- start with 10 copies of each in the population
			for(int i=0;i<seeds.size();i++) {
				seed = new ArrayList<BinaryGene>();
				for(int j=0;j<seeds.get(i).size();j++) {
					seed.add(new BinaryGene(seeds.get(i).get(j), maxGenesInValue));
				}
//				System.out.println(myThread.getName() + " Seed #" + i + ": " + seed);
				num -= 10;	//we need 10 fewer total
				for(int j=0;j<10;j++) {
					synchronized(valuesLock) {
						fitnessValues.add(new Double(0));
						myValues.add(seed);
					}
				}
			}
//			System.out.println(myThread.getName() + " has " + myValues.size() + " entries after seeding");
			
			haveValues = true;
		}

		return true;
	}
	
	public void setValues(ArrayList<ArrayList<BinaryGene>> newValues) {
		synchronized(valuesLock) {
			myValues = newValues;
			haveValues = true;
		}
	}
	
	public boolean setVariables(ArrayList<Character> newVariables) {
		synchronized(variablesLock) {
			myVariables = newVariables;
//			System.out.println("Variables Set");
			haveVariables = true;
			numVariables = myVariables.size();
		}
		return true;
	}

	public boolean getHasData() {
		return myParent.hasData();
	}
	
	public String getDataString() {
		String result = "{";
		ArrayList<Double> cachedRow;
		
		int dataSize = myParent.getData().size();
		
		
		for(int i=0;i<dataSize;i++) {
			result+="[";
			cachedRow = myParent.getDataRow(i);
			for(int j=0;j<cachedRow.size();j++) {
				result+=cachedRow.get(j);
				result+=",";
			}
			result+="]";
		}
		
		result += "}";
		
		return result;
	}
	
	public String getVarsString() {
		String result = "[";
		
		synchronized(variablesLock) {
			if(myVariables.size() > 0) {
				result += myVariables.get(0);
				for(int i=1;i<myVariables.size();i++) {
					result += ", " + myVariables.get(i);
				}
			}
		}
		
		result += "]";
		
		return result;
	}
	
	public ArrayList<Character> getVars() {
		return myVariables;
	}
	
	public int getNumValues () {
		int result = 0;
		
		synchronized(valuesLock) {
			result = myValues.size();
		}
		
		return result;
	}
	
	private double rowProduct(int row) {
		double result;
		ArrayList<Double> rowCache = myParent.getDataRow(row);
		result = rowCache.get(0);
	
		for(int i=1;i<rowCache.size();i++) {
			result *= rowCache.get(i);
		}

		return result;
	}
	
	private double columnSum(int column) {
		double result = 0;
		ArrayList<Double> colCache = myParent.getDataColumn(column);
		
		for(int i=0;i<colCache.size();i++) {
			result += colCache.get(i);
		}

		return result;
	}
	
	public double getBestFitness() {
		double max = -99999;
		
		synchronized(fitnessLock) {
			for(int i=0;i<fitnessValues.size();i++) {
				if(max < fitnessValues.get(i))
					max = fitnessValues.get(i);
			}
		}
		return max;
	}
	
	public ArrayList<BinaryGene> getBestFitValues() {
		ArrayList<BinaryGene> result = null;
		int idx = -1;
		double bestFit = this.getBestFitness();
		
		synchronized(valuesLock) {
			idx = fitnessValues.indexOf(bestFit);
			if(idx != -1) {
				result = myValues.get(idx);
			}
		}
		return result;
	}
	
	public double getAverageFit() {
		double result = 0;
		
		for(int i=0;i<fitnessValues.size();i++) {
			result += fitnessValues.get(i);
		}
		
		result = result / fitnessValues.size();
		
		return result;
	}

	public String getActiveEquation() {
		
		return myEquations.get(activeEquation).binaryToEquation();
	}
	
	public ArrayList<EquationGene> getEquations() {
		return myEquations;
	}
	
	public int getActiveEquationNumber() {
		return activeEquation;
	}
	
	public int getNumEquations() {
		return myEquations.size();
	}
	
	public void setActiveEquation(int id) {
		synchronized(equationLock) {
			activeEquation = id;
		}
	}
	
	public void setMyEquations(ArrayList<EquationGene> newEquations) {
		synchronized(equationLock) {
			myEquations = newEquations;
		}
	}
	
	public void setMyEquation(EquationGene newEquation) {
		synchronized(equationLock) {
			myEquations = new ArrayList<EquationGene>();
			myEquations.add(newEquation);
			activeEquation = 0;
		}
	}
	
	protected double findColMean(int column) {
		double result = 0;
		
		ArrayList<Double> colCache = myParent.getDataColumn(column);

		for(int i=0;i<colCache.size();i++) {
			result += colCache.get(i);
		}
		
		result = result / colCache.size();
		
		return result;
	}
	

	protected double findMean(ArrayList<Double> values) {
		double result = 0;
		
		synchronized(valuesLock) {
			for(int i=0;i<values.size();i++) {
				result += values.get(i);
			}
			result = result / values.size();
		}
		
		return result;
	}
	
	protected double findStdDev(ArrayList<Double> values, double mean) {
		double result = 0;
		
		//sum the squares of the differences
		synchronized(valuesLock) {
			for(int i=0;i<values.size();i++) {
				result += Math.pow(values.get(i) - mean, 2); 
			}
		}
		//divide by the number of samples
		synchronized(valuesLock) {
			result = result / values.size();
		}
		
		//take the square root of the result
		result = Math.sqrt(result);
		
		return result;
	}
	
	protected double checkFit(ArrayList<BinaryGene> values) {
		double rSquared = 0;
		double residualSS = 0;
		double totalSS = 0;
		double observedYMean = 0;
		double temp = 0;
		int dataSize;
		ArrayList<Double> colCache = myParent.getDataColumn(0);
		ArrayList<Double> predictedY = new ArrayList<Double>();
		
		ArrayList<Character> testVariables = new ArrayList<Character>();
//		ArrayList<BinaryGene> testValues = new ArrayList<BinaryGene>();
		ArrayList<Double> testValuesDbl = new ArrayList<Double>();
		
		testVariables.addAll(myVariables);
		testVariables.add('x');
//		testValues.addAll(values);
		for(int i=0;i<values.size();i++) {
			testValuesDbl.add(values.get(i).getDouble());
		}
		
		//hmm... also inefficient. I should only calc this once
		synchronized(valuesLock) {
			observedYMean = findColMean(1);
		}
		
//		System.out.println("Checking Gene: " + values.toString());
		
		
		for(int i=0;i<colCache.size();i++) {
//				if(testValues.size() == values.size()) {
			if(testValuesDbl.size() == values.size()) {
				// add x
				// this seems REALLY inefficient.....
//					testValues.add(new BinaryGene(myData.get(i).get(0)));
				testValuesDbl.add(colCache.get(i));
			} else {
//					testValues.set(testValues.size() - 1, new BinaryGene(myData.get(i).get(0)));
				testValuesDbl.set(testValuesDbl.size() - 1, colCache.get(i));
			}
//				temp = EquationHelper.solveGenes(myEquations.get(activeEquation).toString(), testVariables, testValues);
			temp = EquationHelper.solve(myEquations.get(activeEquation), testVariables, testValuesDbl);
//				System.out.println("Y:" + temp);
//				temp = values.get(0).binaryToDouble() * myData.get(i).get(0) + values.get(1).binaryToDouble();
			predictedY.add(temp);
		}
		
		colCache = myParent.getDataColumn(1);
		
		for(int i=0;i<predictedY.size();i++) {
			totalSS += Math.pow((colCache.get(i) - observedYMean), 2);
			residualSS += Math.pow((colCache.get(i) - predictedY.get(i)), 2);
		}
		
		if(!Double.isFinite(residualSS))
			return 0;
		
		rSquared = 1 - (residualSS / totalSS);
		
//		if(Math.abs(rSquared) < 1) {
	//		System.out.println(rSquared);
		//}
		
/*		if(values.get(0).getDouble() < 0.0005 && values.get(0).getDouble() > 0) { 
			System.out.println(values);
			System.out.println(rSquared);
		}*/
		
		if(!Double.isFinite(rSquared)) {
			return 0;
		}
		
//		if(Math.abs(rSquared) > 1) {
//			return 0;
//		}
		
		if(Math.abs(rSquared) > 5) {
			return 0;
		}

		
//		if(rSquared > 1 || rSquared < 0) {
//			return 0;
//		}
		
		return (rSquared);
	}
	
	protected void testFitness() {
		synchronized(valuesLock) {
			fitnessValues = new ArrayList<Double>();
			for(int i=0;i<myValues.size();i++) {
				fitnessValues.add(i, checkFit(myValues.get(i)));
			}
		}
	}
	
	protected int cullZeroes() {
		int culled = 0;
		
		synchronized(valuesLock) {
			for(int i=0;i<myValues.size();i++) {
				synchronized(fitnessLock) {
					if(fitnessValues.get(i)==0 || fitnessValues.get(i)==Double.NaN || Double.isInfinite(fitnessValues.get(i))) {
						myValues.remove(i);
						fitnessValues.remove(i);
						culled++;
						i--; //move back one step in the loop because we just culled one from the input
					}
				}
			}
		}
		
		return culled;
	}
	
	protected int cullPoorFits() {
		double mean;
		double stdDev;
		double minSafe;
		int totalCulled = 0;
		
		totalCulled += cullZeroes();
		
		mean = findMean(fitnessValues);
		stdDev = findStdDev(fitnessValues, mean);
		
		//set stDev to a minimum of 0.01--we want to allow for SOME deviation
		if(stdDev < 0.01) {
			stdDev = 0.01;
		}
		
		minSafe = mean - this.fitnessDeviations * stdDev;
		
		/*System.out.println("Mean: " + mean);
		System.out.println("StdDev: " + stdDev);
		System.out.println("MinSafe: " + minSafe);*/
		
		if(minSafe < 0) {
			minSafe = 0;
		}
		
		
		
		synchronized(valuesLock) {
			for(int i=0;i<myValues.size();i++) {
				synchronized(fitnessLock) {
					if(fitnessValues.get(i)<minSafe) {
						myValues.remove(i);
						fitnessValues.remove(i);
						totalCulled++;
						i--; //move back one step in the loop because we just culled one from the input
					}
				}
			}
		}
		
		/*
		System.out.println("SURVIVORS: " + myValues.size());
		for(int i=0;i<myValues.size();i++) {
			System.out.println("" + i + ")" + myValues.get(i) + fitnessValues.get(i));
		}*/

		return totalCulled;
	}
	
	protected ArrayList<BinaryGene> doMutate(ArrayList<BinaryGene> values) {
		return doMutate(values, -1);
	}
	
	protected ArrayList<BinaryGene> doMutate(ArrayList<BinaryGene> values, int varToModify) {
		ArrayList<BinaryGene> result = new ArrayList<BinaryGene>();
		Random generator = new Random();
		char[] temp;
		
		if(varToModify==-1) {
			for(int i=0;i<values.size();i++) {
				/*
				 * No need to convert -- new objects can handle this internally
				 *convert this value to a string and check to see if we mutate each digit
				 *temp = values.get(i).toString();
				 *result = "";
				 */
				temp = values.get(i).toString().toCharArray();
				for(int j=0;j<temp.length;j++) {
					//walk each character and try to mutate
					if(generator.nextInt() % mutationChance == 0) {
						//yep, mutation happens.
						if(temp[j]=='0') {
							temp[j] = 1;
						} else {
							temp[j] = 0;
						}
	//					values.get(i).flipIndex(i);
					}
					
				}
				result.add(new BinaryGene(String.valueOf(temp), maxGenesInValue));
				//finished with that block of genes -- next one!
			}
		} else {
			
			for(int i=0;i<varToModify;i++) {
				result.add(values.get(i));
			}
			
			temp = values.get(varToModify).toString().toCharArray();
			for(int j=0;j<temp.length;j++) {
				//walk each character and try to mutate
				if(generator.nextInt() % mutationChance == 0) {
					//yep, mutation happens.
					if(temp[j]=='0') {
						temp[j] = 1;
					} else {
						temp[j] = 0;
					}
//					values.get(i).flipIndex(i);
				}
				
			}
			
			result.add(new BinaryGene(String.valueOf(temp), maxGenesInValue));
			
			for(int i=varToModify+1;i<values.size();i++) {
				result.add(values.get(i));
			}
			
		}
		
		return result;		
	}
	
	protected ArrayList<BinaryGene> doCrossover(ArrayList<BinaryGene> a, ArrayList<BinaryGene> b) {
		return doCrossover(a, b, -1);
	}
	
	protected ArrayList<BinaryGene> doCrossover(ArrayList<BinaryGene> a, ArrayList<BinaryGene> b, int varToModify) {
		ArrayList<BinaryGene> result = new ArrayList<BinaryGene>();
		String temp = "";
		String storage = "";
		int crossPoint = 0;
		Random generator = new Random();
		
		if(varToModify==-1) {
			for(int i=0;i<a.size();i++) {
				//iterate through the individual genes
				//pick a crossover point for each
				crossPoint = Math.abs(generator.nextInt() % a.get(i).length());
	/*			System.out.println("LenA: " + a.get(i).length());
				System.out.println("LenB: " + b.get(i).length());
				System.out.println("CrossPoint: " + crossPoint);
				System.out.println("A: " + a.get(i).toString());
				System.out.println("B: " + b.get(i).toString());*/
				storage = a.get(i).getValue();
				temp = storage.substring(0, crossPoint);
				storage = b.get(i).getValue();
				temp += storage.substring(crossPoint);
				result.add(new BinaryGene(BinaryGene.binaryToDoubleStr(temp), maxGenesInValue));
				temp = storage.substring(0, crossPoint);
				storage = a.get(i).getValue();
				temp += storage.substring(crossPoint);
				result.add(new BinaryGene(BinaryGene.binaryToDoubleStr(temp), maxGenesInValue));
			}
		} else {
			for(int i=0;i<varToModify;i++) {
				result.add(a.get(i));
			}
			crossPoint = Math.abs(generator.nextInt() % a.get(varToModify).length());
			
			try{
				storage = a.get(varToModify).getValue();
				temp = storage.substring(0, crossPoint);
				storage = b.get(varToModify).getValue();
				temp += storage.substring(crossPoint);
			} catch (Exception e) {
				System.out.println("A: " + a + "\nB: " + b);
				System.out.println("CROSSPOINT: " + crossPoint + "\nA: " + a.get(varToModify).getValue() + " (" + a.get(varToModify).getGeneCount() + ")\nB: " + b.get(varToModify).getValue() + " (" + b.get(varToModify).getGeneCount() + ")");
				throw e;
			}
			result.add(new BinaryGene(BinaryGene.binaryToDoubleStr(temp), maxGenesInValue));
			
			for(int i=varToModify+1;i<a.size();i++) {
				result.add(a.get(i));
			}
		}
				
		return result;
	}
	
	protected ArrayList<BinaryGene> generateNewChild(ArrayList<BinaryGene> geneA, ArrayList<BinaryGene> geneB, int varToModify) {

		Random generator = new Random();
		
		//MUTATE AFTER BREEDING
		//we ALWAYS have a chance to mutate
//		doMutate(geneA);
//		doMutate(geneB);
		
//		System.out.println("NEWA: " + geneA.toString());
//		System.out.println("NEWB: " + geneB.toString());
		
		//do we cross over?
		if(generator.nextDouble() < crossoverChance) {
//			System.out.println("CROSSOVER");
			return doMutate(doCrossover(geneA, geneB, varToModify), varToModify);
		} else {
//			System.out.println("RANDOM");
//			System.out.println("A: " + geneA.toString());
//			System.out.println("B: " + geneB.toString());
			//pick one randomly to return as a child
			if(generator.nextBoolean()) {
//				System.out.println("Chose A");
				return doMutate(geneA, varToModify);
			} else {
//				System.out.println("Chose B");
				return doMutate(geneB, varToModify);
			}
		}

	}
	
	protected ArrayList<BinaryGene> randomWeightedSelect() {
		ArrayList<Integer> simplifiedFitness = new ArrayList<Integer>();
		int fitnessSum = 0;
		int pick = 0;
		int idx = 0;
		double fitTemp = 0;
		Random generator = new Random();
		
		synchronized(fitnessLock) {
			for(int i=0;i<fitnessValues.size();i++) {
				fitTemp = fitnessValues.get(i);
				if(fitTemp < 0) {
					if(fitTemp > -5) {
						simplifiedFitness.add((int)(1000*Math.exp(1.3816*fitTemp)));
					} else {
						simplifiedFitness.add(0);
					}
				} else if(fitTemp < 1) {
					simplifiedFitness.add((int)(1000*Math.exp(6.2146*fitTemp)));
				} else if(fitTemp == 1) {
					simplifiedFitness.add(50000);
				} else if(fitTemp <= 1.1) {
					simplifiedFitness.add((int)(500000*Math.pow(fitTemp, -65.2)));
				} else if(fitTemp < 5) {
					simplifiedFitness.add((int)(7017*Math.exp(-1.771*fitTemp)));
				} else if(fitTemp == 0) {
					simplifiedFitness.add(1000);
				} else {
					simplifiedFitness.add(0);
				}
//				simplifiedFitness.add((int)(fitTemp * 100 * extraSelectionWeight));
//				simplifiedFitness.add((int)(Math.pow(fitTemp, extraSelectionWeight) * 100));
				fitnessSum += simplifiedFitness.get(i);
			}
		}
		if(fitnessSum > 0) {
			pick = generator.nextInt() % fitnessSum;
		} else {
			idx = generator.nextInt(myValues.size());
			
			return myValues.get(idx);
		}
		
		while(idx < simplifiedFitness.size() && pick > simplifiedFitness.get(idx)) {
//		while(idx < simplifiedFitness.size() && pick > 1) {
			pick -= simplifiedFitness.get(idx);
			idx++;
		}
			
		synchronized(valuesLock) {
			if(idx < simplifiedFitness.size()) {
				return myValues.get(idx);
			} else {
//				System.out.println("Idx Out of Range in random weighted select");
				return myValues.get(generator.nextInt(myValues.size()));
			}
		}
	}
	
	protected void generateNewSet() {
		generateNewSet(-1);
	}
	
	protected void generateNewSet(int varToModify) {
		ArrayList<ArrayList<BinaryGene>> newValues = new ArrayList<ArrayList<BinaryGene>>();
		ArrayList<BinaryGene> parentA = null;
		ArrayList<BinaryGene> parentB = null;
		int addlNeeded = 0;
		int targetChildren = (int)Math.max(minSeeds*this.seedTargetMultiplier*0.9, myValues.size());
		int bestFit = fitnessValues.indexOf(this.getBestFitness());
		Random generator = new Random();
		int temp;
		
		//preserve the BEST fitness value (if it exists)
		if(bestFit != -1) {
			newValues.add(myValues.get(bestFit));
		}
		
//		newValues.addAll(myValues);
//		System.out.println(myThread.getName() + " saved values: " + newValues.size() + " : " + this.fitnessValues.size());
		synchronized(valuesLock) {
			if(myValues.size() != 0) {
				while(newValues.size() < targetChildren) {
					//select two parents (via fitness based weighted random select) & preserve them & child
					parentA = randomWeightedSelect();
					parentB = randomWeightedSelect();
					
					//generate a new 'child' set of values (can be a clone)
					newValues.add(generateNewChild(parentA, parentB, varToModify));

					//random chance for parent A to survive
/*					if(generator.nextBoolean()) {
						newValues.add(parentA);
					}
					//random chance for parent B to survive
					if(generator.nextBoolean()){
						newValues.add(parentB);
					}*/
				}
			}
		}
//		System.out.println(myThread.getName() + " saved values: " + newValues.size() + " : " + this.fitnessValues.size());
		
/*		System.out.println("New Generation after Children");
		for(int i=0;i<newValues.size();i++) {
			System.out.println("" + i + ")" + newValues.get(i).toString());
		}*/
		
		if(newValues.size() < (minSeeds * seedTargetMultiplier)) {
			addlNeeded = minSeeds*seedTargetMultiplier - newValues.size();
		}
		synchronized(valuesLock) {
			haveValues = false;
			myValues = new ArrayList<ArrayList<BinaryGene>>();
			if(decimateCount == (int)maxCycles/10 || fitnessRepeats == (int)maxCycles/10) {
				//we've potentially gotten stuck, haven't we... generate a TON of extra values once
				buildValues(this.maxGenesInValue, addlNeeded*10);
			} else {
				if((int)maxCycles/10 - decimateCount < 3 || (int)maxCycles/10 - fitnessRepeats < 3) {
					//We're so stuck that we should try hard each time to break out
					buildValues(this.maxGenesInValue, addlNeeded*10);
				} else {
					buildValues(this.maxGenesInValue, addlNeeded);
				}
			}
			if(newValues.size() > 0) {
				myValues.addAll(newValues);
			}
			haveValues = true; 
		}
	}
	
	private String generateRandomEquation(int size) {
		Random generator = new Random();
		String result = "";
		for(int i=0;i<size;i++) {
			if(generator.nextBoolean()) {
				result+="1";
			} else {
				result+="0";
			}
		}
		return result;
	}
	
	private void seedEquations() {
		int half = equationPopulation / 2;		//how much will be set in stone (initially)?
		int initials = half/4;					//how many equations of the above will be of each type (linear, power, log, exp)
		EquationGene temp = null;
		
		//build initial cohort

		myEquations.add(new EquationGene("a*x+b", maxGenesInEquation));
		myEquations.add(new EquationGene("a*(Lx+b)", maxGenesInEquation));
		myEquations.add(new EquationGene("a*(E(b*x))", maxGenesInEquation));
		myEquations.add(new EquationGene("a*(x^b)", maxGenesInEquation));
		
		/*
		for(int i=half;i<equationPopulation;i++) {
			temp = new EquationGene();
			temp.setValue(generateRandomEquation(maxGenesInEquation*4));
			myEquation.add(temp);
		}*/
	}
	
	private double checkEqFit(int eqNum) {
		double result = 0;
		
		
		
		return result;		
	}
	
	
	private void testEquationFitness() {
		synchronized(equationLock) {
			equationFitness = new ArrayList<Double>();
			for(int i=0;i<myEquations.size();i++) {
				equationFitness.add(i, checkEqFit(i));
			}
		}
	}
	
	public int autodetectVariables(int equation) {
		String eq;
		
		if(equation >= myEquations.size())
			return -1;
		
		eq = myEquations.get(equation).toString();
		
		myVariables = new ArrayList<Character>();
		
		for(int i=0;i<eq.length();i++) {
			if(Character.isLowerCase(eq.charAt(i))) {
				if(eq.charAt(i) != 'x') {
					//prevent dupes!
					if(!myVariables.contains(eq.charAt(i))) {
						myVariables.add(eq.charAt(i));
					}
				}
			}
		}
		haveVariables = true;
		numVariables = myVariables.size();
		return numVariables;
	}
	
	//mergesort implementation
	private void sort(int low, int high, ArrayList<Double>tempFits, ArrayList<ArrayList<BinaryGene>>tempValues) {
		int pivot;
		
		if(tempFits == null) {
			tempFits = new ArrayList<Double>();
			for(int i=0;i<fitnessValues.size();i++) {
				tempFits.add(fitnessValues.get(i));
			}
		}
		
		if(tempValues == null) {
			tempValues = new ArrayList<ArrayList<BinaryGene>>();
			for(int i=0;i<myValues.size();i++) {
				tempValues.add(myValues.get(i));
			}
		}
		
		if(low < high) {
			pivot = low + (high - low) / 2;
			sort(low, pivot, tempFits, tempValues);
			sort(pivot + 1, high, tempFits, tempValues);
			doMerge(low, pivot, high, tempFits, tempValues);
		}
	}
	
	private void doMerge(int low, int pivot, int high, ArrayList<Double>tempFits, ArrayList<ArrayList<BinaryGene>>tempValues) {

		int i;
		int j;
		int k;		

		for(i=low;i<high;i++) {
			tempValues.set(i, myValues.get(i));
			tempFits.set(i, fitnessValues.get(i));
		}
		i = low;
		j = pivot+1;
		k = low;
		
		while(i<=pivot && j<=high) {
			if(tempFits.get(i) <= tempFits.get(j)) {
				myValues.set(k, tempValues.get(i));
				fitnessValues.set(k, tempFits.get(i));
				i++;
			} else {
				myValues.set(k, tempValues.get(j));
				fitnessValues.set(k, tempFits.get(j));
				j++;
			}
			k++;
		}
		while(i<=pivot) {
			myValues.set(k, tempValues.get(i));
			fitnessValues.set(k, tempFits.get(i));
			i++;
			k++;
		}
	}
	
	private void decimate() {
		int target = minSeeds / 2;
		double bestFit = this.getBestFitness();
		ArrayList<BinaryGene> bestValues = this.getBestFitValues();
		
		sort(0, myValues.size()-1, null, null);
//		System.out.println(fitnessValues);
		decimateCount++;
		while(myValues.size() > target) {
			myValues.remove(0);
			fitnessValues.remove(0);
		}
		//ensure we keep the best fit (it keeps dropping :-( )
		myValues.add(bestValues);
		fitnessValues.add(bestFit);
	}
	
	public void run() {
		int cycles = 0;
		int culled = 0;
		int zeroes = 0;
		double lastFit = -1;
		double currentFit = 0;
		
		while(keepRunning) {
			//do we have values & variables to test?
			if(myParent.hasData()) {
				hasData = true;
			}
			if(haveValues && haveVariables && hasData) {
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				started = true;

				//start tracking the number of cycles
				cycles++;
				//we do! Start work.
				
				//test all values fitness
				testFitness();

				currentFit = this.getBestFitness();
				if(currentFit == lastFit && currentFit != 0) {
					fitnessRepeats++;
				} else {
					if(currentFit == 0) {
						zeroes++;
					} else {
						fitnessRepeats = 0;
						lastFit = currentFit;
					}
				}
				
				//cull & report number of poor fits
				// ONLY CULL NONZEROES EVERY 3 CYCLES
				if(cycles%3 == 0) {
//					culled = cullPoorFits();
					culled = cullZeroes();
				} else {
					culled = cullZeroes();
				}
				
				//report to console
				if(cycles%30 == 0) {
					if(getBestFitness() > 0) {
//						System.out.println(this.myName + ": " + cycles + " :: R-Squared: " + getBestFitness() + " :: Pop: " + myValues.size() + " :: Mean: " + this.findMean(fitnessValues) + " :: Target: " + this.fitnessTarget);
					}
				}
				if(myValues.size() > minSeeds) {
					decimate();
				}
				//are we above the min number of cycles we can run?
				if(cycles > minCycles) {
					//yes--check to see if we have a fitness value above the threshold
					if(cycles % 5 == 0) {
						testEquationFitness();
					}
//					System.out.println(this.getBestFitness());
					if(this.getBestFitness() > fitnessTarget || decimateCount > maxCycles/10 || fitnessRepeats > maxCycles/10 || zeroes > maxCycles/10) {
						keepRunning = false;
//						System.out.println(this.myName + ": Stopping :: " + decimateCount + " :: " + fitnessRepeats + " :: " + zeroes + " :: of: " + maxCycles/10);
					}
				}
				
				//check to see if we can run another cycle 
				if(cycles >= maxCycles) {
					keepRunning = false;
				} else {
					if(keepRunning) {
						//only modify 1 variable at a time, but cycle through them
						generateNewSet((cycles-1)%myVariables.size());
						//generateNewSet();
					}
				}
			} else {
				//no, we don't.
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		shutdownSafely();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//TESTING MAIN
		//Use GeneticBatchExecutor to test -- depends on master dataset being available.
		

	}

}
