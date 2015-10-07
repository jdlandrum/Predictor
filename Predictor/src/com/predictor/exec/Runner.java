/**
 * 
 */
package com.predictor.exec;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.predictor.components.Datastore;
import com.predictor.io.CSVReader;
import com.predictor.io.CSVWriter;
import com.predictor.io.FileIO;
import com.predictor.io.IOTemplate;
import com.predictor.io.ReaderTemplate;
import com.predictor.io.WriterTemplate;
import com.predictor.optimization.GeneticMultiPopulation;
import com.predictor.optimization.GeneticOptimizer;
import com.predictor.tools.EquationHelper;

/**
 * @author jlandrum
 *
 */
public class Runner {

	private GeneticMultiPopulation optimizer;
	private GeneticOptimizer liveSolution;
	private Datastore data;
	private int activeRows;
	private int feedRate;
	private IOTemplate input;
	private IOTemplate output;
	private ReaderTemplate reader;
	private WriterTemplate writer;
	private int population;
	private int minIterations;
	private int maxIterations;
	private int populations;
	private double targetFit;
	private String inFile;
	private String outFile;
	private double minX;
	private double maxX;
	private double xStep;
	private int datasets;
	private boolean disableHex;

	
	public Runner() {
		disableHex = false;
		optimizer = null;
		liveSolution = null;
		data = null;
		input = null;
		output = null;
		reader = null;
		writer = null;
		datasets = 0;
		population = -1;
		minIterations = -1;
		maxIterations = -1;
		populations = -1;
		targetFit = -1;
		minX = -1;
		maxX = -1;
		xStep = -1;
		activeRows = -1;
		feedRate = 10;
		inFile = "";
		outFile = "";
	}
	
	private boolean processTemplate(String arg) {
		
		if(arg.equals("xfast") || arg.equals("demo")) {
			population = 15;
			minIterations = 15;
			maxIterations = 180;
			populations = 10;
			targetFit = 0.9;
			disableHex = true;
			return true;
		} else if(arg.equals("fast") || arg.equals("ok")) {
			population = 25;
			minIterations = 30;
			maxIterations = 300;
			populations = 10;
			targetFit = 0.9;
			disableHex = true;
			return true;
		} else if(arg.equals("normal") || arg.equals("norm") || arg.equals("balanced") || arg.equals("default")) {
			population = 25;
			minIterations = 63;
			maxIterations = 315;
			populations = 50;
			targetFit = 0.9;
			return true;
		} else if(arg.equals("slow") || arg.equals("good")) {
			population = 25;
			minIterations = 63;
			maxIterations = 315;
			populations = 100;
			targetFit = 0.9;
			return true;
		} else if(arg.equals("xslow") || arg.equals("better")) {
			population = 50;
			minIterations = 126;
			maxIterations = 441;
			populations = 100;
			targetFit = 0.9;
			return true;
		} else if(arg.equals("xxslow") || arg.equals("best")) {
			population = 100;
			minIterations = 126;
			maxIterations = 441;
			populations = 250;
			targetFit = 0.9;
			return true;
		} else if(arg.equals("slowest")) {
			population = 100;
			minIterations = 126;
			maxIterations = 630;
			populations = 500;
			targetFit = 0.95;
			return true;
		} else if(arg.equals("large")) {
			population = 100;
			minIterations = 63;
			maxIterations = 315;
			populations = 50;
			targetFit = 0.9;
			return true;
		} else if(arg.equals("long")) {
			population = 50;
			minIterations = 252;
			maxIterations = 630;
			populations = 50;
			targetFit = 0.9;
			return true;
		} else if(arg.equals("xlong")) {
			population = 100;
			minIterations = 604;
			maxIterations = 1260;
			populations = 50;
			targetFit = 0.9;
			return true;
		}
		
		return false;
	}
	
	public boolean processArgs(String[] args) {
		boolean success = true;
		
		/*
		 * Process CLI arguments
		 */
		if(args.length == 1) {
			//we received a template config
			return processTemplate(args[0]);
		} else {
			
			for(int i=0;i<args.length;i++) {
				//have we failed at a step? If so, return false
				if(!success) {
					return false;
				}
				
				if(args[i].equals("-disablehex") || args[i].equals("-disableHex") || args[i].equals("-noHex") || args[i].equals("-nohex")) {
					//Disable x^6  equation
					disableHex = true;
				}
				
				//are we at the end? if so, just stop (no further flags make sense)
				if(i + 1 >= args.length) {
					break;	
				} else {
					//check each string as we reach it and process as follows...
					if(args[i].equals("-template")) {
						//next argument is a template config
						processTemplate(args[i+1]);
						i++;
					} else if(args[i].equals("-in")) {
						//next arg is an input data file
						inFile = args[i+1];
						i++;
					} else if(args[i].equals("-out")) {
						//next arg is an output file
						outFile = args[i+1];
						i++;
					} else if(args[i].equals("-r2")) {
						//next arg is target rSquared value
						targetFit = Double.valueOf(args[i+1]);
						if(targetFit < 0 || targetFit > 1) {
							success = false;
						}
						i++;
					} else if(args[i].equals("-pop")) {
						//next arg is the population for each run
						population = Integer.valueOf(args[i+1]);
						i++;
					} else if(args[i].equals("-minRuns")) {
						//next arg is the # of generations (minimum) per generation
						minIterations = Integer.valueOf(args[i+1]);
						i++;
					} else if(args[i].equals("-maxRuns")) {
						//next arg is the # of generations (max) per generation
						maxIterations = Integer.valueOf(args[i+1]);
						i++;
					} else if(args[i].equals("-pops")) {
						//next arg is the # of independent populations to run
						populations = Integer.valueOf(args[i+1]);
						i++;
					} else if(args[i].equals("-startrows")) {
						//next arg is the # of independent populations to run
						activeRows = Integer.valueOf(args[i+1]);
						i++;
					} else if(args[i].equals("-feedrate")) {
						//next arg is the # of independent populations to run
						feedRate = Integer.valueOf(args[i+1]);
						i++;
					} else {
						success = false;
					}
				}
			}
		}
		return success;
	}
	
	private void sanityCheckAndSetDefaults() {
		SimpleDateFormat timestamp = new SimpleDateFormat("HH_mm");
		
		if(inFile.equals("")) {
			inFile = "data.csv";
		}
		
		if(outFile.equals("")) {
			outFile = "result_" + timestamp.format(new Date()) + ".csv";
		}
		
		if(population == -1) {
			population = 25;
		} else if(population > 50) {
			System.out.println("Warning: Populations greater than 50 members in size can drastically increase runtime\n"
					         + "         (Your Population: " + population + ")");
		}
		
		if(minIterations == -1) {
			minIterations = 30;
		}
		
		if(maxIterations == -1) {
			maxIterations = 300;
		}
		
		if(targetFit == -1) {
			targetFit = 0.9;
		} else if(targetFit < 0.85) {
			System.out.println("Warning: Setting the desired value for R^2 too low can drastically impact result quality\n"
					         + "         (Your R^2: " + targetFit + ")");
		}
		
		if(populations == -1) {
			populations = 100;
		} else if(populations > 30) {
			System.out.println("Warning: Large numbers of populations will generate better results but can drastically increase runtime\n"
			                 + "         (Your Population Count: " + populations + ")");
		}
	}
	
	public String getConfigString() {
		String result = "";
		
		result += " * Input:       " + inFile + "\n";
		result += " * Output:      " + outFile + "\n";
		result += " * rSquared:    " + targetFit + "\n";
		result += " * Pop Size:    " + population + "\n";
		result += " * Populations: " + populations + "\n";
		result += " * Min Gens:    " + minIterations + "\n";
		result += " * Max Gens:    " + maxIterations + "\n";
		result += " * Disable Hex: " + disableHex + "\n";
		
		return result;
	}
	
	private boolean openInputFile() {
				
		try {
			input = new FileIO(inFile, 'r');
		} catch (IOException e) {
			System.out.println("Could not open file \'" + inFile + "\' for reading.\nExiting...");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private boolean openNewOutput(String outFileName) {

		try {
			output = new FileIO(outFileName, 'w');
		} catch (IOException e) {
			System.out.println("Could not open file \'" + outFileName + "\' for writing.\nExiting...");
			return false;
		}
		
		return true;
	}
	
	private void readData() {
		int temp = 0;
		data = new Datastore();
		reader = new CSVReader();
		reader.readAllData(input, data);
		
		datasets += data.columns();
		
		for(int i=0;i<datasets;i++) {
			temp += i;
		}
		
		datasets = temp;
		
	}
	
	public boolean initialize() {
		boolean success = false;
		
		sanityCheckAndSetDefaults();
		
		System.out.println("Run Configuration: \n" + getConfigString());
		
		success = openInputFile();
		if(success) { 
			readData();
		}
		
		return success;
	}
	
	public void doFinalOutput(Datastore selectedData) {
		Datastore outData = new Datastore();
		ArrayList<Character> vars = new ArrayList<Character>();
		ArrayList<Double> newRow;
		ArrayList<Double> values;
		double temp = minX;
		double tempY = 0;
		String headerA = null;
		String headerB = null;
		String tempStr = null;
		boolean write = false;
		
		//short circuit 1
		if(data==null || selectedData==null) {
			return;
		}
		
		/*
		 * Only create an output file if we're close enough to the target fit.
		 * We always want to write to console
		 */
		if(optimizer.getSolution().getBestFitness() > targetFit - 0.1) {
			write = true;
		}
		
		headerA = selectedData.getColumnHeader(0);
		headerB = selectedData.getColumnHeader(1);
		
		//short circuit 2
		if(write) {
			if(!openNewOutput(outFile.substring(0, outFile.length()-4) + "_" + headerA + "_" + headerB + outFile.substring(outFile.length()-4))) {
				return;
			}
		}
		
		System.out.println("Finished with comparison of " + headerA + " and " + headerB);
		outData.createHeaders(selectedData.getHeaders());
		vars = optimizer.getSolution().getBestFitVariables();
		vars.add('x');
		try {
			values = optimizer.getSolution().getBestFitValues();
			values.add(0.0);
			while(temp < maxX) {
				newRow = new ArrayList<Double>();
				values.remove(values.size()-1);
				values.add(temp);
				newRow.add(temp);
				tempY = EquationHelper.solve(optimizer.getSolution().getBestFitEquation(), vars, values);
				newRow.add(tempY);
				outData.addRow(newRow);
				temp += xStep;
			}
		} catch(IndexOutOfBoundsException e) {
			e.printStackTrace();
			return;
		}
	
		System.out.print(outData.toJSON());
		
		if(write) {
			writer = new CSVWriter();
			writer.writeStringLn(output, "Equation:," + optimizer.getSolution().getBestFitEquationStr());
			tempStr = optimizer.getSolution().getBestFitVariablesStr();
			writer.writeStringLn(output, "Variables:," + tempStr.substring(1, tempStr.length()-3));
			tempStr = optimizer.getSolution().getBestFitValues().toString();
			writer.writeStringLn(output, "Values:," + tempStr.substring(1, tempStr.length()-1));
			writer.writeStringLn(output, "Fitness:," + optimizer.getSolution().getBestFitness());
			writer.writeAllData(output, outData);
			output.close();
		}
	}
	
	public void run() {
		boolean finished = false;
		int colA = 0;
		int colB = 0;
		Datastore selectedData = new Datastore();
		ArrayList<Double> tempRow = null;
		ArrayList<String> tempHeader = null;
		
		if(data==null || datasets < 1) {
			//short circuit
			return;
		}
		
		System.out.println("STARTING WORK");
		
		for(int i=0;i<datasets;i++) {
			finished = false;
			if(datasets == 1) {
				System.out.println("Only 1 set");
				selectedData = data;
			} else {
				selectedData = new Datastore();
				//set up colA & colB
				
				//Leave Col A alone, increase Col B by 1
				colB++;
				
				//if ColB has maxed out....
				if(colB >= data.columns()) {
					//increment ColA
					colA++;
					//reset colB to colA+1
					colB = colA + 1;
				}
				//sanity check & short circuit
				if(colA >= data.columns()) {
					return;
				}
				
				//build 2-column dataset for this run
				
				//Create Headers & Add Headers
				tempHeader = new ArrayList<String>();
				tempHeader.add(data.getColumnHeader(colA));
				tempHeader.add(data.getColumnHeader(colB));
				selectedData.createHeaders(tempHeader);
				
				//Create Rows of data and add them.
				for(int j=0;j<data.rows();j++) {
					tempRow = new ArrayList<Double>();
					tempRow.add(data.get(j, colA));
					tempRow.add(data.get(j, colB));
					selectedData.addRow(tempRow);
				}
			}
			
			//Set up minX, maxX, and xStep values for output file
			minX = selectedData.getColumnMin(0);
			maxX = selectedData.getColumnMax(0);
			xStep = (maxX - minX)/100;
			
			System.out.println("DATASET READY");
			
			optimizer = new GeneticMultiPopulation(targetFit, population, maxIterations, minIterations, 1, populations, disableHex);
			
			if(activeRows < 1) {
				optimizer.setData(selectedData);
			} else {
				optimizer.setData(selectedData.getSubset(activeRows));
			}
			
			while(!finished) {
				if(optimizer.isFinished()) {
					finished = true;
				} else {
					
					try {
						Thread.sleep(2500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					}
					if(activeRows > 0 && activeRows < data.rows()) {
						activeRows += feedRate;
						optimizer.setData(data.getSubset(activeRows));
					}
					liveSolution = optimizer.getBestFitWorker();
					if(liveSolution != null && !finished) {
						System.out.println("Best Fit: " + liveSolution.getBestFitness());
						System.out.println("Equation: " + liveSolution.getActiveEquation());
						System.out.println("Values: " + liveSolution.getVarsString() + " :: " + liveSolution.getBestFitValues());
					}
				}
			}
			System.out.println("FINISHED");
			this.doFinalOutput(selectedData);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Runner myRunner = new Runner();
		boolean success;
		
		success = myRunner.processArgs(args);
		
		if(!success) {
			System.out.print("Malformed input: "); 
			for(int i=0;i<args.length;i++) {
				System.out.print(args[i] + " ");
			}
			System.out.println("\n\nInput flags should be limited to:");
			System.out.println("     -in          The input CSV file to use (must exist)");
			System.out.println("     -out         The file output should be writtent to");
			System.out.println("     -r2          The acceptable/target accuracy\n"
					         + "                       (legal values: [0.0,1.0])\n"
					         + "                       (Recommended values: [0.85-0.95])");
			System.out.println("     -pop         The size of each \'population\'\n"
					         + "                       (larger sizes can increase runtime drastically)");
			System.out.println("     -minRuns     The minimum number of generations a population must complete\n"
					         + "                       (Recommended values: [30-300])");
			System.out.println("     -maxRuns     The maximum number of generations a population may complete\n"
					         + "                       (Recommended value: 3-10x minRuns)");
			System.out.println("     -pops        The number of independant populations to run\n"
					         + "                       (Recommended values: [30-300]; legal values: [0-MaxInt31])");
			return;
		}
		
		success = myRunner.initialize();
		
		myRunner.run();		
	}

}
