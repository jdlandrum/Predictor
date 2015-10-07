/**
 * 
 */
package com.predictor.optimization;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.predictor.components.Datastore;
import com.predictor.genes.EquationGene;
import com.predictor.io.CSVReader;
import com.predictor.io.CSVWriter;
import com.predictor.io.FileIO;
import com.predictor.io.IOTemplate;
import com.predictor.io.NetIO;
import com.predictor.io.ReaderTemplate;
import com.predictor.io.WriterTemplate;
import com.predictor.tools.EquationHelper;

/**
 * @author jlandrum
 *
 */
public class GeneticMultiPopulation implements Runnable {

	private ArrayList<GeneticBatchExecutor> subPopulations;
	private ArrayList<Boolean> usedData;
	private int numPopulations;
	private GeneticBatchExecutor solution;
	
	private Datastore myData;
	private boolean hasData;
	private boolean disableHex;
	
	private double targetFitness;
	private int targetMinPopulation;
	private int targetMaxIterations;
	private int targetMinIterations;
	private double targetDeviations;
	
	private Object populationLock;
	private Object dataLock;
	
	private Thread myThread;
	private boolean finished;
	
	public GeneticMultiPopulation() {
		this(0.9, 50, 240, 30, 10, 30, false);
	}
	
	public GeneticMultiPopulation(double target, int minPop, int maxRuns, int minRuns, double deviations, int subPops, boolean disableHex) {
		numPopulations = subPops;
		
		targetFitness = target;
		targetMinPopulation = minPop;
		targetMaxIterations = maxRuns;
		targetMinIterations = minRuns;
		targetDeviations = deviations;
		
		subPopulations = new ArrayList<GeneticBatchExecutor>();
		usedData = new ArrayList<Boolean>();
		solution = null;
		
		populationLock = new Object();
		dataLock = new Object();
		
		myData = null;
		
		this.disableHex = disableHex;
		
		myThread = new Thread(this, "GeneticMultiPop");
		finished = false;
		myThread.start();
	}
	
	public void setData(Datastore someData) {
		synchronized(dataLock) {
			myData = someData;
			hasData = true;
		}
	}
	
	public Datastore getData() {
		synchronized(dataLock) {
			return myData;
		}
	}
	
	public GeneticBatchExecutor getSolution() {
		return solution;
	}
	
	public double getSolutionFit() {
		if(solution==null) {
			return -1;
		} else {
			return solution.getBestFitness();
		}
	}
	
	public GeneticOptimizer getBestFitWorker() {
		GeneticOptimizer result = null;
		int idx;
		
		synchronized(populationLock) {
			if(subPopulations.size() > 0) {
				if(subPopulations.get(0).isFinished()) {
					idx = 0;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		
		synchronized(populationLock) {
			for(int i=1;i<subPopulations.size();i++) {
				if(subPopulations.get(i).isFinished()) {
					if(subPopulations.get(i).getBestFitness() > subPopulations.get(idx).getBestFitness()) {
						idx = i;
					}
				}
			}
		}
				
		result = subPopulations.get(idx).getBestFitWorker();
		
		return result;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	private void initialize() {
		synchronized(populationLock) {
			for(int i=0;i<numPopulations;i++) {
				subPopulations.add(new GeneticBatchExecutor(targetFitness, targetMinPopulation, targetMaxIterations, targetMinIterations, targetDeviations, disableHex));
				usedData.add(new Boolean(false));
			}
		}
	}
	
	private boolean subPopsAreRunning() {
		boolean result = false;
		
		for(int i=0;i<numPopulations;i++) {
			synchronized(populationLock) {
				result = result || !subPopulations.get(i).isFinished();
			}
		}
		
		return result;
	}
	
	private int numPopsRunning() {
		int result = 0;
		
		for(int i=0;i<numPopulations;i++) {
			synchronized(populationLock) {
				if(subPopulations.get(i).isStarted() && !subPopulations.get(i).isFinished()) {
					result++;
				}
			}
		}
		
		return result;
	}
	
	private int numPopsFinished() {
		int result = 0;
		
		for(int i=0;i<numPopulations;i++) {
			synchronized(populationLock) {
				if(subPopulations.get(i).isFinished()) {
					result++;
				}
			}
		}
		
		return result;
	}
	
	private int numPopsWithUnusedData() {
		int result = 0;
		
		for(int i=0;i<numPopulations;i++) {
			synchronized(populationLock) {
				if(subPopulations.get(i).isFinished() && usedData.get(i) == false) {
					result++;
				}
			}
		}
		
		return result;
	}
	
	private ArrayList<GeneticOptimizer> buildSeededWorkers(GeneticBatchExecutor seededRun) {
		ArrayList<GeneticOptimizer> result = new ArrayList<GeneticOptimizer>();
		GeneticOptimizer tempOptimizer = null;
		ArrayList<EquationGene> equations = new ArrayList<EquationGene>();
		//yo dawg, I heard you liked ArrayLists so I put an ArrayList in your ArrayList in your ArrayList...
		//3D ArrayList -- first IDX matches up with the equations ArrayList, 2nd the sub-seed, 3rd the variable value 
		ArrayList<ArrayList<ArrayList<Double>>> seedValues = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<Double> tempValues = null;
		EquationGene tempEq = null;
		int subEquations = 0;
		int tempIdx = -1;
		boolean alreadyExists;
		
		//Grab all of the finished equations & the seed values in them--ignore anything with a sub-0.5 fit.
		for(int i=0;i<numPopulations;i++) {
			if(subPopulations.get(i).isFinished() && !usedData.get(i)) {
				subEquations = subPopulations.get(i).getNumEquations();
//				System.out.println("SubEq" + i + ": " + subEquations);
				for(int j=0;j<subEquations;j++) {
//					if(subPopulations.get(i).getFitness(j) >= 0.5) {
						alreadyExists = false;
						tempIdx = -1;
						tempValues = subPopulations.get(i).getValues(j);
						tempEq = subPopulations.get(i).getEquation(j);		
						
						for(int k=0;k<equations.size();k++) {
							if(!alreadyExists && equations.get(k).equals(tempEq)) {
								alreadyExists = true;
								tempIdx = k;
							}
						}
						if(!alreadyExists) {
							if(subPopulations.get(i).getFitness(j)!=-99999 && subPopulations.get(i).getFitness(j)!=0) {
//								System.out.println(tempEq + "\n" + subPopulations.get(i).getFitness(j) + "\n" + tempValues);
								equations.add(tempEq);
								seedValues.add(new ArrayList<ArrayList<Double>>());
								seedValues.get(seedValues.size() - 1).add(tempValues);
							}
						} else if(subPopulations.get(i).getFitness(j)!=-99999 && subPopulations.get(i).getFitness(j)!=0) {
//							System.out.println("EQ " + tempEq + " found in list");
							if(tempIdx!=-1) {
								seedValues.get(tempIdx).add(tempValues);
							}
						}
//					}
				}
				//we're done with this entry; flag that we've used its data
				usedData.set(i, true);
			}
		}
		
		//Build & Seed workers
		for(int i=0;i<equations.size();i++) {
			//one worker per equation
			tempOptimizer = new GeneticOptimizer(targetMinPopulation, targetDeviations, targetMinIterations,
					 targetMaxIterations, targetFitness, equations.get(i).toString(), seededRun);
			tempOptimizer.setMyEquation(equations.get(i));
			tempOptimizer.autodetectVariables(0);
			tempOptimizer.buildValuesWithSeeds(seedValues.get(i));
						
			result.add(tempOptimizer);
		}
		
		return result;
	}
	
	public void run() {
		GeneticBatchExecutor seededRun;
		ArrayList<GeneticOptimizer> seededWorkers = null;
		int cpuCores = Runtime.getRuntime().availableProcessors();
		int availablePopulations = (3*cpuCores)/8;	//run N simultaneous populations where N = 3*CPUs/WorkThreads (8 work threads by default)
		int runningPopulations = 0;
		int secondGenSize = Math.min(Math.max(5, (int)(numPopulations/20)), 10); //2nd gen sizes are 5-10 batches in size 
		
		System.out.println("Running " + availablePopulations + " populations simultaneously");
		
		if(availablePopulations <= 1) {
			//always run 2 worker population at once (minimum).
			availablePopulations = 2;
		}
		
		while(!hasData) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//prep workers
		initialize();
		
		//start the first batch of workers
		for(int i=0;i<availablePopulations;i++) {
			synchronized(dataLock) {
				synchronized(populationLock) {
					subPopulations.get(i).setData(myData.getMatrix());
					while(!subPopulations.get(i).isStarted()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		runningPopulations = availablePopulations;
		//start the rest as threads are available
		for(int i=availablePopulations;i<numPopulations;i++) {
			runningPopulations = numPopsRunning();
			if(numPopsWithUnusedData() >= secondGenSize) {
				seededRun = new GeneticBatchExecutor(targetFitness, targetMinPopulation, targetMaxIterations, targetMinIterations, targetDeviations, disableHex);
				seededWorkers = buildSeededWorkers(seededRun);
				for(int j=0;j<seededWorkers.size();j++) {
					seededRun.addWorker(seededWorkers.get(j));
				}
				
				synchronized(populationLock) {
					subPopulations.add(seededRun);
					usedData.add(new Boolean(false));
					numPopulations = subPopulations.size();
				}
			} 
			if(runningPopulations >= availablePopulations) {
				i--;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				synchronized(dataLock) {
					synchronized(populationLock) {
						subPopulations.get(i).setData(myData.getMatrix());
						while(!subPopulations.get(i).isStarted()) {
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				runningPopulations++;
			}
		}
		
		while(subPopsAreRunning()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		//adjust min/max iterations & fitness targets for final run
		targetMinIterations = targetMaxIterations/2;
		targetMaxIterations *= 2;
		targetFitness = (1-targetFitness)/2 + targetFitness;
		
		seededRun = new GeneticBatchExecutor(targetFitness, targetMinPopulation*2, targetMaxIterations, targetMinIterations, targetDeviations, disableHex);
		seededWorkers = buildSeededWorkers(seededRun);
		
		for(int i=0;i<seededWorkers.size();i++) {
			seededRun.addWorker(seededWorkers.get(i));
		}
		
		seededRun.setData(myData.getMatrix());
		
		while(!seededRun.isFinished()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		System.out.println("\nBest EQ: " + seededRun.getBestFitEquation());
		System.out.println("R-Squared: " + seededRun.getBestFitness());
		System.out.println("Vars: " + seededRun.getBestFitVariablesStr());
		System.out.println("Best Values: " + seededRun.getBestFitValues());
		System.out.println("##########");
		System.out.println("All Equations & R2");
		for(int i=0;i<seededRun.getNumEquations();i++) {
			System.out.println("EQ  : " + seededRun.getEquationStr(i));
			System.out.println("R^2 : " + seededRun.getFitness(i));
			if(seededRun.getFitness(i) != -1) {
				System.out.println("Vars: " + seededRun.getVariables(i));
				System.out.println("Vals: " + seededRun.getValues(i));
			}
		}
		
		solution = seededRun;
		finished = true;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GeneticMultiPopulation test = new GeneticMultiPopulation();
		double minX;
		double maxX;
		int minInt, maxInt, step;
		Datastore data = null;
		ArrayList<Double> newRow, values;
		ArrayList<Character> vars;
		ArrayList<String> headers;
		IOTemplate input = null;
		IOTemplate output = null;
		ReaderTemplate reader = null;
		WriterTemplate writer = null;
		SimpleDateFormat timestamp = new SimpleDateFormat("hhmm");
		
		try {
//			input = new FileIO(".\\", "9_day_order_v_transactions.csv", 'r');
//			input = new FileIO(".\\", "orders_v_transactions.csv", 'r');
//			input = new FileIO(".\\", "8day_orders.csv", 'r');
			input = new FileIO(".\\", "14day_orders.csv", 'r');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		data = new Datastore();
		reader = new CSVReader();
		reader.readAllData(input, data);

//		data.trimToNDeviations(3);

		//get the min & max X values;
		minX = data.getColumnMin(0);
		maxX = data.getColumnMax(0);
		
		minInt = (int)minX;
		maxInt = (int)maxX;
		step = (maxInt-minInt)/100;
		headers = data.getHeaders();
		test.setData(data);
//		test.setData(testData);
		while(!test.isFinished()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Finished with maths");
		data = new Datastore();
		data.createHeaders(headers);
		vars = test.getSolution().getBestFitVariables();
		vars.add('x');
		for(int i=minInt;i<maxInt;i+=step) {
			newRow = new ArrayList<Double>();
			values = test.getSolution().getBestFitValues();
			values.add(i + 0.0);
			newRow.add(i + 0.0);
			newRow.add(EquationHelper.solve(test.getSolution().getBestFitEquation(), vars, values));
			data.addRow(newRow);
		}

		System.out.print(data.toJSON());
		
		try {
			output = new FileIO(".\\", "resultCSV_" + timestamp.format(new Date()) + ".csv", 'w');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer = new CSVWriter();
		writer.writeAllData(output, data);
	}


}
