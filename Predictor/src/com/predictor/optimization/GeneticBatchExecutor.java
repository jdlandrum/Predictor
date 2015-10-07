/**
 * 
 */
package com.predictor.optimization;

import java.io.IOException;
import java.util.ArrayList;

import com.predictor.components.Datastore;
import com.predictor.genes.BinaryGene;
import com.predictor.genes.EquationGene;
import com.predictor.io.CSVReader;
import com.predictor.io.FileIO;
import com.predictor.io.IOTemplate;
import com.predictor.io.ReaderTemplate;

/**
 * @author jland_000
 *
 */
public class GeneticBatchExecutor implements Runnable{

	private ArrayList<EquationGene> myEquations;
	private ArrayList<Double> fitness;
	private int bestFitIdx;
	
	private ArrayList<ArrayList<Double>> masterData;
	private ArrayList<ArrayList<Double>> publicData;
	private boolean haveData;
	private int currentResolution;
	private int currentReaders;
	private boolean writingData;
	
	private ArrayList<GeneticOptimizer> myWorkers;
	private int minWorkers;
	private boolean haveWorkers;
	private double targetFitness;
	private int targetMinPopulation;
	private int targetMaxIterations;
	private int targetMinIterations;
	private double targetDeviations;
	private boolean disableHex;
	
	private Thread myThread;
	private String myName;
	private boolean keepRunning;
	private boolean started;	
	
	private Object dataLock;
	private Object equationsLock;
	private Object workersLock;
	private Object fitnessLock;
	
	public GeneticBatchExecutor() {
		this(0.9, 250, 250, 25, 1, false);
	}
	
	public GeneticBatchExecutor(double target, int minPop, int maxRuns, int minRuns, double deviations, boolean disableHex) {
		myEquations = new ArrayList<EquationGene>();
		fitness = new ArrayList<Double>();
		bestFitIdx = -1;
		
		masterData = new ArrayList<ArrayList<Double>>();
		publicData = new ArrayList<ArrayList<Double>>();
		haveData = false;
		currentResolution = 1;
		currentReaders = 0;
		writingData = false;
		
		myWorkers = new ArrayList<GeneticOptimizer>();
		minWorkers = 4;
		haveWorkers = false;
		targetFitness = target;
		targetMinPopulation = minPop;
		targetMaxIterations = maxRuns;
		targetMinIterations = minRuns;
		targetDeviations = deviations;
		
		this.disableHex = disableHex;
		
		dataLock = new Object();
		equationsLock = new Object();
		workersLock = new Object();
		fitnessLock = new Object();
		
		myName = "GeneticBatchExecutor";
		keepRunning = true;
		started = false;
		myThread = new Thread(this, myName);
		myThread.start();
	}
	
	private void createWorkerSeeds() {
		GeneticOptimizer temp = null;
		
		if(myWorkers.size() > 0)
			return;
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "Linear", this);
		temp.setMyEquation(new EquationGene("a*x+b"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());
		
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*x+b"));
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "Quadratic", this);
		temp.setMyEquation(new EquationGene("a*x*x+b*x+c"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());
		
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*x*x+b*x+c"));
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		if(!disableHex) {
			temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "Hex", this);
			temp.setMyEquation(new EquationGene("a*x*x*x*x*x*x+b*x*x*x*x*x+c*x*x*x*x+d*x*x*x+e*x*x+f*x+g"));
			temp.autodetectVariables(0);
			temp.buildValues();
	//		System.out.println(temp.getVarsString());
			
			synchronized(fitnessLock) {
				fitness.add(0.0);
			}
			synchronized(equationsLock) {
				myEquations.add(new EquationGene("a*x*x*x*x*x*x+b*x*x*x*x*x+c*x*x*x*x+d*x*x*x+e*x*x+f*x+g"));
			}
			synchronized(workersLock) {
				myWorkers.add(temp);
			}
		}
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "Log", this);
		temp.setMyEquation(new EquationGene("a*Lx+b"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());
		
		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*Lx+b"));
		}
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}

		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "ModLog", this);
		temp.setMyEquation(new EquationGene("a*L(b*x)+c"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());
		
		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*L(b*x)+c"));
		}
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "Exponential", this);
		temp.setMyEquation(new EquationGene("a*(E(b*x))"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());

		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*(E(b*x))"));
		}
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "ModExponential", this);
		temp.setMyEquation(new EquationGene("a*(E(b*x))+c"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());

		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*(E(b*x))+c"));
		}
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "Power", this);
		temp.setMyEquation(new EquationGene("a*(x^b)"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());

		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*(x^b)"));
		}
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, "ModPower", this);
		temp.setMyEquation(new EquationGene("a*(x^b)+c"));
		temp.autodetectVariables(0);
		temp.buildValues();
//		System.out.println(temp.getVarsString());

		synchronized(equationsLock) {
			myEquations.add(new EquationGene("a*(x^b)+c"));
		}
		synchronized(fitnessLock) {
			fitness.add(0.0);
		}
		synchronized(workersLock) {
			myWorkers.add(temp);
		}
		
		haveWorkers = true;
	}
	
	private void createRandomEquations() {
		EquationGene temp = null;
		
//		System.out.println("Making " + (targetMinPopulation/10) + " equations");
		
		synchronized(equationsLock) {
			for(int i=0;i<targetMinPopulation/10;i++) {
				temp = new EquationGene();
				temp.randomize(60);
				while(!temp.toString().contains("x")) {
					temp.randomize(60);
				}
				myEquations.add(temp);
			}
		}
	}
	
	private void spawnRandomWorkers() {
		GeneticOptimizer temp = null;
		
		for(int i=0;i<myEquations.size();i++) {
			temp = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations, targetMaxIterations, targetFitness, myEquations.get(i).toString(), this);
			synchronized(equationsLock) {
				temp.setMyEquation(myEquations.get(i));
			}
			temp.autodetectVariables(0);
			temp.buildValues();
//			System.out.println(temp.getVarsString());
			
			synchronized(fitnessLock) {
				fitness.add(0.0);
			}
			
			synchronized(workersLock) {
				myWorkers.add(temp);
			}
		}
		haveWorkers = true;
	}
	
	//They all borrow the master data now.
/*	private void pushDataToWorkers() {
		for(int i=0;i<myWorkers.size();i++) {
			myWorkers.get(i).setData(masterData);
		}
	}*/
	
	private boolean areAllWorkersStarted() {
		boolean result;
		
		synchronized(workersLock) {
			if(myWorkers.size() > 0) {
				result = myWorkers.get(0).isStarted();
			} else {
				return false;
			}
		}
		
		synchronized(workersLock) {
			for(int i=1;i<myWorkers.size();i++) {
				result = result && myWorkers.get(i).isStarted();
			}
		}
		
		return result;
	}
	
	private boolean areAllWorkersFinished() {
		boolean result = false;
		boolean worker;
		
		synchronized(workersLock) {
			for(int i=0;i<myWorkers.size();i++) {
				//compare with NOT isRunning because if we are NOT running, we're finished!
				worker = myWorkers.get(i).isRunning();
				result = result || worker;
			}
		}
		result = !result;
		return result;
	}
	
	private void doFitnessCheck() {
		String temp;
		double max = 0;
		
		//try to short circuit (0 fitness entries)
		if(fitness.size() == 0) {
			bestFitIdx = -1;
			return;
		}
		
		for(int i=0;i<myWorkers.size();i++) {
			temp = myWorkers.get(i).getActiveEquation();
			synchronized(equationsLock) {
				for(int j=0;j<myEquations.size();j++) {
					if(myEquations.get(j).toString().equals(temp)) {
						synchronized(fitnessLock) {
							fitness.set(j, myWorkers.get(i).getBestFitness());
						}
						j = myEquations.size();
					}
				}
			}
		}
		max = fitness.get(0);
		bestFitIdx = 0;
		for(int i=1;i<fitness.size();i++) {
			if(fitness.get(i) > max) {
				max = fitness.get(i);
				bestFitIdx = i;
			}
		}
		
	}
	
	public double getBestFitness() {
		if(bestFitIdx == -1) {
			return -1;
		} else {
			return fitness.get(bestFitIdx);
		}
	}
	
	public EquationGene getBestFitEquation() {
		if(bestFitIdx == -1) { 
			return null;
		} else {
			return myEquations.get(bestFitIdx);
		}
	}
	
	public String getBestFitEquationStr() {
		return myEquations.get(bestFitIdx).toString();
	}
	
	public ArrayList<Double> getValues(int idx) {
		ArrayList<Double> result = new ArrayList<Double>();
		ArrayList<BinaryGene> temp;
		
		temp = myWorkers.get(idx).getBestFitValues();
		if(temp!=null) {
			for(int j=0;j<temp.size();j++) {
				result.add(temp.get(j).getDouble());
			}
		}
		
		return result;	
	}
	
	public String getVariables(int idx) {
		return myWorkers.get(idx).getVarsString();
	}
	
	public ArrayList<Double> getBestFitValues() {
		ArrayList<Double> result = new ArrayList<Double>();
		ArrayList<BinaryGene> temp;
		String bestFitEq;
		
		if(bestFitIdx != -1) {
			bestFitEq = myEquations.get(bestFitIdx).toString();
		} else {
			bestFitEq = null;
		}
		
		for(int i=0;i<myWorkers.size();i++) {
			if(myWorkers.get(i).getActiveEquation().equals(bestFitEq)) {
				temp = myWorkers.get(i).getBestFitValues();
				if(temp!=null) {
					for(int j=0;j<temp.size();j++) {
						result.add(temp.get(j).getDouble());
					}
				}
			}
		}
		
		return result;		
	}
	
	public String getBestFitVariablesStr() {
		if(bestFitIdx==-1) {
			return null;
		} else {
			return myWorkers.get(bestFitIdx).getVarsString();
		}
	}
	
	public ArrayList<Character> getBestFitVariables() {
		if(bestFitIdx==-1) {
			return null;
		} else {
			return myWorkers.get(bestFitIdx).getVars();
		}
	}
	
	public GeneticOptimizer getBestFitWorker() {
		if(bestFitIdx==-1) {
			return null;
		} else {
			return myWorkers.get(bestFitIdx);
		}
	}
	
	public int getNumEquations() {
		return myEquations.size();
	}
	
	public double getFitness(int idx) {
		return fitness.get(idx);
	}
	
	public String getEquationStr(int idx) {
		synchronized(equationsLock) {
			return myEquations.get(idx).toString();
		}
	}
	
	public EquationGene getEquation(int idx) {
		synchronized(equationsLock) {
			return myEquations.get(idx);
		}
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public boolean isFinished() {
		return !keepRunning;
	}
	
	private void prepPublicData() {
		int rows;
		
		//how many rows do we have?
		synchronized(dataLock) {
			rows = masterData.size();
		}
		
		/*
		 * lets start at 1/Nth resolution where...
		 *  N = 10 if rows < 1000
		 *  N = 100 if rows < 10000
		 *  etc.
		 */
		rows /= 10;
		while(rows>=10) {
			rows /= 10;
			currentResolution *= 10;
		}
		loadPublicData();
	}
		//reset rows;
		
	private void loadPublicData() {
		int rows, i;
		double xAverage;
		double yAverage;
		ArrayList<Double> temp;
		
		synchronized(dataLock) {
			rows = masterData.size();		
			i = 0;
			
			while(i<rows) {
				temp = new ArrayList<Double>();
				xAverage = 0;
				yAverage = 0;
				for(int j=0;j<currentResolution;j++) {
					if(i<rows) {
						xAverage = masterData.get(i).get(0);
						yAverage = masterData.get(i).get(1);
						i++;
					} else {
						xAverage = xAverage / (j+1);
						yAverage = yAverage / (j+1);
						j=currentResolution;
					}				
				}
				if(i<rows) {
					xAverage/=currentResolution;
					yAverage/=currentResolution;
				}
				temp.add(xAverage);
				temp.add(yAverage);
				publicData.add(temp);
			}
		}
	}
	
	public void increaseResolution() {
		currentResolution = currentResolution / 10;
		
		loadPublicData();
	}
	
	public boolean setData(double[][] newData) {
		ArrayList<Double> row = null;
		
		if(haveData) {
			//TODO Support rewriting the data object when desired
			return false;
		} else {
			
			writingData = true;
			
			while(this.currentReaders > 0) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			synchronized(dataLock) {
				if(masterData==null) { 
					masterData = new ArrayList<ArrayList<Double>>();
				}
				for(int i=0;i<newData.length;i++) {
//					System.out.println("Building Row " + i);
					row = new ArrayList<Double>();
					for(int j=0;j<newData[i].length;j++) {
						row.add(newData[i][j]);
					}
					masterData.add(row);
//					System.out.println("Added Row" + row);
				}
				haveData = true;
			}
//			prepPublicData();
			
			writingData = false;
			
			return true;
		}
	}
	
	public ArrayList<ArrayList<Double>> getData() {
		
		while(writingData) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return masterData;
	}
	
	public ArrayList<Double> getDataRow(int row) {
		ArrayList<Double> aRow;
		
		while(writingData) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		currentReaders++;
		aRow = masterData.get(row);
		currentReaders--;
		
		return aRow;
	}
	
	public ArrayList<Double> getDataColumn(int col) {
		ArrayList<Double> result = new ArrayList<Double>();
		int rows;
		
		while(writingData) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		currentReaders++;
		
		rows = masterData.size();
		
		for(int i=0;i<rows;i++) {
			result.add(masterData.get(i).get(col));
		}
		
		currentReaders--;
		
		return result;
	}
	
	public boolean hasData() {
		if(haveData) {
			return true;
		} else {
			return false;
		}
	}
	
	public void addWorker(GeneticOptimizer aWorker) {
		synchronized(workersLock) {
			myWorkers.add(aWorker);
			synchronized(equationsLock) {
				this.myEquations.add(myWorkers.get(myWorkers.size() - 1).getEquations().get(0));
			}
			synchronized(fitnessLock) {
				fitness.add(0.0);
			}
			
			haveWorkers = true;
		}
	}
	
	public void run() {
		int cycles = 0;
		while(keepRunning) {
			if(!started) {
				if(!haveData) {
					//no data to work on -- sleep
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					//have Data to work on. Spin up subprocesses, flag as started, and start.
					if(!haveWorkers) {
						createWorkerSeeds();
						//pushDataToWorkers();
						//createRandomEquations();
						//spawnRandomWorkers();
					} else {
						if(areAllWorkersStarted()) {
							started = true;
							System.out.print("Working\n");
							//pause briefly
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			} else {
				//we're started!
				cycles++;
//				System.out.print(".");
				doFitnessCheck();
				if(!areAllWorkersFinished()) {
					if(cycles%10 == 0) {
/*						if(currentResolution > 1) {
							
							increaseResolution();
							System.out.println("##### PUBLIC RESOLUTION INCREASE TO 1/" + currentResolution + "th #####");
						}*/
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					keepRunning = false;
				}
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GeneticBatchExecutor test = new GeneticBatchExecutor();
		
		Datastore data = null;
		IOTemplate input = null;
		ReaderTemplate reader = null;
		
		try {
			input = new FileIO(".\\", "9_day_order_v_transactions.csv", 'r');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		data = new Datastore();
		reader = new CSVReader();
		reader.readAllData(input, data);

		data.trimToNDeviations(2);

		test.setData(data.getMatrix());
//		test.setData(testData);
		while(!test.isFinished()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("\nBest EQ: " + test.getBestFitEquation());
		System.out.println("R-Squared: " + test.getBestFitness());
		System.out.println("Vars: " + test.getBestFitVariablesStr());
		System.out.println("Best Values: " + test.getBestFitValues());
		System.out.println("##########");
		System.out.println("All Equations & R2");
		for(int i=0;i<test.getNumEquations();i++) {
			System.out.println("EQ  : " + test.getEquationStr(i));
			System.out.println("R^2 : " + test.getFitness(i));
			if(test.getFitness(i) != -1) {
				System.out.println("Vars: " + test.getVariables(i));
				System.out.println("Vals: " + test.getValues(i));
			}
		}
	}

}
