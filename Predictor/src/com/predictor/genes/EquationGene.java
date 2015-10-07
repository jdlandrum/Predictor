/**
 * 
 */
package com.predictor.genes;

import java.util.ArrayList;
import java.util.Random;

import com.predictor.tools.EquationHelper;

/**
 * @author jlandrum
 *
 */
public class EquationGene implements Comparable<EquationGene>{

	private String value;
	private String equation;
	private String interpretedAs;
	private String usableEquation;
	private ArrayList<String> parentheticals;
	private int basesPerGene;
	
	public EquationGene() {
		this("x");
	}
	
	public EquationGene(String initialValue) {
		this(initialValue, -1);
	}
	
	public EquationGene(String initialValue, int maxGenes) {
		interpretedAs = "";
		value = this.equationToBinary(initialValue);
		usableEquation = null;
		parentheticals = null;
		basesPerGene = 5;
		
		if(maxGenes!=-1) {
			if(maxGenes > initialValue.length()) {
				equation = initialValue;
			} else {
				equation = initialValue.substring(0, maxGenes);
			}
		} else {
			equation = initialValue;
		}
		
		if(maxGenes != -1) {
			//ensure the value is the correct number of digits long
			if(value.length() > maxGenes*basesPerGene) {
				value = value.substring(0, maxGenes*basesPerGene);
			} else {
				while(value.length() < maxGenes*basesPerGene) {
					//11111 is the "nothing" value
					value+="11111";
				}
			}
		}
	}
	
	public String equationToBinary(String equation) {
		String result = "";
				
		for(int i=0;i<equation.length();i++) {
			switch(equation.charAt(i)) {
				case 'x': result += "00000";
					break;
				case 'a': result += "00001";
					break;
				case 'b': result += "00010";
					break;
				case 'c': result += "00011";
					break;
				case 'd': result += "00100";
					break;
				case 'e': result += "00101";
					break;
				case 'f': result += "00110";
					break;
				case 'g': result += "00111";
					break;
				case '+': result += "01000";
					break;
				case '*': result += "01001";
					break;
				case '/': result += "01010";
					break;
				case '^': result += "01011";
					break;
				case 'L': result += "01100";
					break;
				case 'E': result += "01101";
					break;
				case '(': result += "01110";
					break;
				case ')': result += "01111";
					break;
				default: result += "";
					break;
			}
		}
		
		return result;
	}
	
	/**
	 * Converts the passed in equation-gene into 1-symbol chunks
	 * @param binEq
	 * @return a chunked version of the  equation
	 */
	private ArrayList<String> toChunks(String binEq) {
		ArrayList<String> result = new ArrayList<String>();
		String temp;
		
		for(int i=0;i<binEq.length();i+=basesPerGene) {
			temp = "";
			for(int j=0;j<basesPerGene;j++) {
				temp += binEq.charAt(i+j);
			}
			result.add(temp);
		}
		
		return result;
	}
	
	public String binaryToEquation() {
		return getEquation();
	}
	
	public String binaryToEquation(String binEq) {
		String result = "";
		ArrayList<String> chunks = toChunks(binEq);
		
		for(int i=0;i<chunks.size();i++) {
			if(chunks.get(i).equals("00000")) {
				result += 'x';
			} else if(chunks.get(i).equals("00001")) {
				result += 'a';
			} else if(chunks.get(i).equals("00010")) {
				result += 'b';
			} else if(chunks.get(i).equals("00011")) {
				result += 'c';
			} else if(chunks.get(i).equals("00100")) {
				result += 'd';
			} else if(chunks.get(i).equals("00101")) {
				result += 'e';
			} else if(chunks.get(i).equals("00110")) {
				result += 'f';
			} else if(chunks.get(i).equals("00111")) {
				result += 'g';
			} else if(chunks.get(i).equals("01000")) {
				result += '+';
			} else if(chunks.get(i).equals("01001")) {
				result += '*';
			} else if(chunks.get(i).equals("01010")) {
				result += '/';
			} else if(chunks.get(i).equals("01011")) {
				result += '^';
			} else if(chunks.get(i).equals("01100")) {
				result += 'L';
			} else if(chunks.get(i).equals("01101")) {
				result += 'E';
			} else if(chunks.get(i).equals("01110")) {
				result += '(';
			} else if(chunks.get(i).equals("01111")) {
				result += ')';
			}
		}
		
		return result;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getBinary() {
		return value;
	}
	
	public String getEquation() {
		return equation;
	}
	
	public String getInterpretation() {
		return interpretedAs;
	}
	
	public void setInterpretation(String eq) {
//		System.out.println("Equation: " + equation + "\nInterpreted as: " + eq);
		interpretedAs = eq;
	}
	
	/**
	 * @return the usableEquation
	 */
	public String getUsableEquation() {
		return usableEquation;
	}

	/**
	 * @return the number of bases in a gene
	 */
	public int getBasesPerGene() {
		return basesPerGene;
	}
	
	/**
	 * @param usableEquation the usableEquation to set
	 */
	public void setUsableEquation(String usableEquation) {
		this.usableEquation = usableEquation;
	}

	/**
	 * @return the parentheticals
	 */
	public ArrayList<String> getParentheticals() {
		return parentheticals;
	}

	/**
	 * @param parentheticals the parentheticals to set
	 */
	public void setParentheticals(ArrayList<String> parentheticals) {
		this.parentheticals = parentheticals;
	}

	public String toString() {
		return equation;
	}
	
	public boolean equals(EquationGene anEquation) {
		if(equation.equals(anEquation.toString()))
			return true;
		else
			return false;
	}
	
	public int compareTo(EquationGene anEquation) {
		
		return this.toString().compareTo(anEquation.toString());
	}
	
	public char charAt(int idx) {
		return value.charAt(idx);
	}
	
	public int length() {
		return value.length();
	}
	
	public int numElements() {
		return value.length()/basesPerGene;
	}
	
	public void setValue(String eq) {
		value = equationToBinary(eq);
		equation = eq;
		usableEquation = "";
		parentheticals = null;
	}
	
	public boolean flipIndex(int i) {
		String temp;
		if(i < value.length()) {
			temp = value.substring(0, i);
			if(value.charAt(i) == '0') {
				temp += '1';
			} else {
				temp += '0';
			}
			i++;
			if(i < value.length()) {
				temp += value.substring(i);
			}
			value = temp;
			equation = binaryToEquation(value);
			return true;
		} else {
			return false;
		}
	}
	
	public void randomize() {
		if(value.equals("") || value == null) {
			randomize(16);
		} else {
			randomize(value.length());
		}
	}
	
	public void randomize(int length) {
		Random generator = new Random();
		
		value = "";
		
		for(int i=0;i<length;i++) {
			if(generator.nextBoolean()) {
				value += "1";
			} else {
				value += "0";
			}
		}

		equation = binaryToEquation(this.value);
		usableEquation = "";
		parentheticals = null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EquationGene test = new EquationGene("a*x+b", 5);
		ArrayList<Character> testVars = new ArrayList<Character>();
		ArrayList<Double> testVals = new ArrayList<Double>();
		
		testVars.add('a');
		testVars.add('b');
		testVars.add('c');
		testVars.add('d');
		testVars.add('e');
		testVars.add('f');
		testVars.add('x');
		
		testVals.add(1.0);
		testVals.add(2.0);
		testVals.add(3.0);
		testVals.add(4.0);
		testVals.add(5.0);
		testVals.add(6.0);
		testVals.add(7.0);
		
		System.out.println("Binary: " + test.getValue());
		System.out.println("Equation: " + test);
		System.out.println("Elements: " + test.numElements());
		System.out.println("Solved: " + EquationHelper.solve(test.toString(), testVars, testVals));

		System.out.println("\n ### Randomizing ### \n");		
		test.randomize(40);
		
		System.out.println("Binary: " + test.getValue());
		System.out.println("Equation: " + test);
		System.out.println("Elements: " + test.numElements());
		System.out.println("Solved: " + EquationHelper.solve(test.toString(), testVars, testVals));
		
	}

}
