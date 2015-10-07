/**
 * 
 */
package com.predictor.io;

import com.predictor.components.Datastore;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author jland_000
 *
 */
public class CSVReader implements ReaderTemplate{

	private char myDelimiter;
	
	public CSVReader() {
		this(',');
	}
	
	public CSVReader(char aDelimiter) {
		myDelimiter = aDelimiter;
	}

	/**
	 * Overloaded method to split a String into an ArrayList of Strings based on a delimiter.
	 *  Uses preconfigured delimiter specified during CSVReader creation (default: ',').
	 * @param input
	 * @return ArrayList<String> of substrings
	 */
	private ArrayList<String> splitSubstrings(String input) {
		return this.splitSubstrings(input, myDelimiter);
	}
	
	/**
	 * Overloaded method to split a String into an ArrayList of Strings based on a delimiter.
	 *  Uses passed in delimiter to split input string into substrings.
	 *  Trims leading & trailing white-space from results.
	 * @param input
	 * @param delimiter
	 * @return ArrayList<String> of substrings
	 */
	private ArrayList<String> splitSubstrings(String input, char delimiter) {
		ArrayList<String> result = new ArrayList<String>();
		String temp = "";
		
		for(int i=0;i<input.length();i++) {
			if(input.charAt(i) != delimiter) {
				temp += input.charAt(i);
			} else {
				result.add(temp.trim());
				temp = "";
			}
		}
		if(!temp.equals("")) {
			result.add(temp.trim());
		}
		
		return result;
	}
	
	private ArrayList<Double> splitSubstringsDouble(String input) {
		return this.splitSubstringsDouble(input, myDelimiter);
	}
	
	private ArrayList<Double> splitSubstringsDouble(String input, char delimiter) {
		ArrayList<Double> result = new ArrayList<Double>();
		
		String temp = "";

		for(int i=0;i<input.length();i++) {
			if(input.charAt(i) != delimiter) {
				temp += input.charAt(i);
			} else {
				result.add(Double.valueOf(temp.trim()));
				temp = "";
			}
		}
		if(!temp.equals("")) {
			result.add(Double.valueOf(temp.trim()));
		}
		
		return result;
	}
	
	/**
	 * Implementation of readRow for the CSVReader
	 */
	public ArrayList<String> readRow(IOTemplate input) {
		
		return this.splitSubstrings(input.readln(), ',');
	}
	
	/**
	 * Implementation of readRowStringArray for the CSVReader
	 */
	public String[] readRowStringArray(IOTemplate input) {
		String[] result = null;
		ArrayList<String> in = this.splitSubstrings(input.readln());
		
		result = new String[in.size()];
		
		for(int i=0;i<in.size();i++) {
			result[i] = in.get(i);
		}
		
		return result;
	}
	
	/**
	 * Read a row of a CSV in and convert it to an ArrayList of Doubles
	 * @param IOTemplate object
	 * @return the resulting row 
	 */
	public ArrayList<Double> readRowDouble(IOTemplate input) {
		
		return splitSubstringsDouble(input.readln());
	}
	
	/**
	 * Implementation of readRowString for CSVReader
	 */
	public String readRowString(IOTemplate input) {
		return input.readln();
	}	
	
	/**
	 * Read the entire CSV into the datastore.
	 *  Expects data to be in the format:
	 *  header1, header2
	 *  data1.1, data2.1
	 *  data1.2, data2.2
	 *  etc.
	 * @param input
	 * @param data
	 */
	public void readAllData(IOTemplate input, Datastore data) {
		String in = input.readln();
		
		if(in != null) {
			data.createHeaders(this.splitSubstrings(in));
		}
		
		while((in = input.readln()) != null) {
			data.addRow(this.splitSubstringsDouble(in));
		}
	}
	
	/**
	 * Overloaded option allowing for no Datastore to be passed in
	 *  (instead, one is returned). Expects the input to be in the
	 *  following format:
	 *  header1, header2
	 *  data1.1, data2.1
	 *  data1.2, data2.2
	 *  etc.
	 * @param input
	 * @return datastore containing the CSV
	 */
	public Datastore readAllData(IOTemplate input) {
		String in = input.readln();
		Datastore data = new Datastore();
		
		if(in != null) {
			data.createHeaders(this.splitSubstrings(in));
		}
		
		while((in = input.readln()) != null) {
			data.addRow(this.splitSubstringsDouble(in));
		}
		
		return data;
	}
	
	/**
	 * Testing main
	 * @param args
	 */
	public static void main(String[] args) {
		FileIO testFile = null;
		CSVReader reader = new CSVReader();
		ArrayList<String> row = new ArrayList<String>();
		String[] rowStrArray = null;
		String rowStr = "";
		
		try {
			testFile = new FileIO("./", "test_table.txt", 'r');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(testFile!=null) {
			row = reader.readRow(testFile);
		}
		
		System.out.println(row.toString());
		
		for(int i=0;i<row.size();i++) {
			System.out.println(row.get(i));
		}
		
		if(testFile!=null) {
			rowStrArray = reader.readRowStringArray(testFile);
		}
		
		for(int i=0;i<rowStrArray.length;i++) {
			System.out.println(rowStrArray[i]);
		}
		
		if(testFile!=null) {
			rowStr = reader.readRowString(testFile);
		}
		
		System.out.println(rowStr);
		
		if(testFile!=null) {
			System.out.println(reader.readRowDouble(testFile));
		}
		
	}

}
