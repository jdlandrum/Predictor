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
public class BinaryGene {

	private String value;
	private double dblValue;
	private int geneCount;
	
	public BinaryGene() {
		this(0, -1);
	}
	
	public BinaryGene(double initialValue) {
		this(initialValue, -1);
	}
	
	public BinaryGene(double initialValue, int maxGenes) {
		
		value = this.doubleToBinary(initialValue);
		geneCount = maxGenes;
		if(maxGenes != -1) {
			//ensure the value is the correct number of digits long
			if(value.length() > maxGenes*4) {
				value = value.substring(0, maxGenes*4);
				dblValue = binaryToDouble(value);
			} else {
				while(value.length() < maxGenes*4) {
					value+="0";
				}
				dblValue = binaryToDouble(value);
			}
		}
	}
	
	public BinaryGene(String initialValue) {
		this(initialValue, -1);
	}
	
	public BinaryGene(String initialValue, int maxGenes) {
		
		value = this.doubleToBinary(initialValue);
		geneCount = maxGenes;
		if(maxGenes != -1) {
			//ensure the value is the correct number of digits long
			if(value.length() > maxGenes*4) {
				value = value.substring(0, maxGenes*4);
				dblValue = binaryToDouble(value);
			} else {
				while(value.length() < maxGenes*4) {
					value+="0";
				}
				dblValue = binaryToDouble(value);
			}
		}
	}
	
	public String doubleToBinary(String dbl) {
		String result = "";
		String temp, exp;
		boolean negExp;
		boolean hasExp = false;
		int expIdx = dbl.indexOf('E');
		int expVal;
		//are we in scientific notation?
		if(expIdx != -1) {
			//yes. Fix it.
			hasExp = true;
			temp = "";
			temp = dbl.substring(0, expIdx);
			exp = dbl.substring(expIdx+1);
			negExp = false;
			if(Integer.valueOf(exp) < 0) {
				negExp = true;
				exp = exp.substring(1);
			}
			
			expVal = Integer.valueOf(exp);
			
			if(Double.valueOf(dbl) < 0) {
				dbl = dbl.substring(0, 1);
			} else {
				dbl = "";
			}
			if(negExp) {
				dbl += "0.";
				expVal--;
			} else {
				for(int i=0;i<temp.length();i++) {
					if(Character.isDigit(temp.charAt(i))) {
						dbl+=temp.charAt(i);
					}
				}
			}
			
			for(int i=0;i<expVal;i++) {
				dbl+="0";
			}
			
			if(negExp) {
				for(int i=0;i<temp.length();i++) {
					if(Character.isDigit(temp.charAt(i))) {
						dbl+=temp.charAt(i);
					}
				}
			}
		}
		
		for(int i=0;i<dbl.length();i++) {
			switch(dbl.charAt(i)) {
				case '-': result += "1111";
					break;
				case '0': result += "0000";
					break;
				case '1': result += "0001";
					break;
				case '2': result += "0010";
					break;
				case '3': result += "0011";
					break;
				case '4': result += "0100";
					break;
				case '5': result += "0101";
					break;
				case '6': result += "0110";
					break;
				case '7': result += "0111";
					break;
				case '8': result += "1000";
					break;
				case '9': result += "1001";
					break;
				case '.': result += "1110";
					break;
				default: result += "";
					break;
			}
		}
		
		return result;
	}
	
	public String doubleToBinary(double dbl) {
		return doubleToBinary(Double.toString(dbl));
	}
	
	/**
	 * Converts the passed in binary-number-gene into 1-symbol chunks
	 * @param bNum
	 * @return a chunked version of the  equation
	 */
	private static ArrayList<String> toChunks(String bNum) {
		ArrayList<String> result = new ArrayList<String>();
		String temp;
		
		for(int i=0;i<bNum.length();i+=4) {
			temp = "";
			for(int j=0;j<4;j++) {
				temp += bNum.charAt(i+j);
			}
			result.add(temp);
		}
		
		return result;
	}
	
	public double binaryToDouble() {
		return dblValue;
	}
	
	public double binaryToDouble(String bNum) {
		return Double.valueOf(binaryToDoubleStr(bNum));
	}
	
	public String binaryToDoubleStr() {
		return binaryToDoubleStr(value);
	}
	
	public static String binaryToDoubleStr(String bNum) {
		String result = "";
		Boolean foundDecimal = false;
		ArrayList<String> chunks = toChunks(bNum);
		
		for(int i=0;i<chunks.size();i++) {
			if(chunks.get(i).equals("0000")) {
				result += '0';
			} else if(chunks.get(i).equals("0001")) {
				result += '1';
			} else if(chunks.get(i).equals("0010")) {
				result += '2';
			} else if(chunks.get(i).equals("0011")) {
				result += '3';
			} else if(chunks.get(i).equals("0100")) {
				result += '4';
			} else if(chunks.get(i).equals("0101")) {
				result += '5';
			} else if(chunks.get(i).equals("0110")) {
				result += '6';
			} else if(chunks.get(i).equals("0111")) {
				result += '7';
			} else if(chunks.get(i).equals("1000")) {
				result += '8';
			} else if(chunks.get(i).equals("1001")) {
				result += '9';
			} else if(chunks.get(i).equals("1111")) {
				if(i == 0) {
					result += '-';
				}
			} else if(chunks.get(i).equals("1110")) {
				if(!foundDecimal) {
					result += '.';
					foundDecimal = true;
				}
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
	
	public double getDouble() {
		return dblValue;
	}
	
	public int getGeneCount() {
		return geneCount;
	}
	
	public String toString() {
		return binaryToDoubleStr();
	}
	
	public char charAt(int idx) {
		return value.charAt(idx);
	}
	
	public int length() {
		return value.length();
	}
	
	public int numElements() {
		return value.length()/4;
	}
	
	public void setValue(String dbl) {
		value = doubleToBinary(dbl);
		while(value.length() < geneCount * 4) {
			value += "0";
		}
		dblValue = Double.valueOf(dbl);
	}
	
	public void setValue(double dbl) {
		value = doubleToBinary(dbl);
		while(value.length() < geneCount * 4) {
			value += "0";
		}
		dblValue = dbl;
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
			dblValue = binaryToDouble(value);
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
		
		dblValue = binaryToDouble(this.value);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Testing main
		
		BinaryGene test = new BinaryGene(-123.456);
		
		System.out.println("Binary: " + test.getValue());
		System.out.println("Double: " + test.getDouble());
		System.out.println("Elements: " + test.numElements());
		
		System.out.println("\n ### Randomizing ### \n");		
		test.randomize(40);
		
		System.out.println("Binary: " + test.getValue());
		System.out.println("Double: " + test.getDouble());
		System.out.println("Elements: " + test.numElements());
	}

}
