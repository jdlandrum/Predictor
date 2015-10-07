/**
 * 
 */
package com.predictor.components;

import java.util.ArrayList;

/**
 * @author jland_000
 *
 */
public class Datastore {

	private ArrayList<String> dataHeaders;
	private ArrayList<ArrayList<Double>> myData;
	private int myRows;
	private int myColumns;
	
	public Datastore() {

		dataHeaders = null;
		myData = null;
		myRows = 0;
		myColumns = 0;
	}
	
	/**
	 * Helper method to initialize the data table
	 */
	private void initializeTable() {
	
		myData = new ArrayList<ArrayList<Double>>();
		if(dataHeaders == null) {
			dataHeaders = new ArrayList<String>();
		}
	}
	
	/**
	 * Private helper for addRow. Actually attepts to add the row
	 *  and increments the size variable.
	 * @param newRow
	 * @return true if the row is added, false otherwise
	 */
	private boolean add(ArrayList<Double> newRow) {
		
		myRows++;
		return myData.add(newRow);
	}
	
	/**
	 * Remove the indicated row
	 * @param aRow
	 * @return true if removed, false otherwise
	 */
	public boolean remove(int aRow) {
		if(aRow >= myRows) {
			return false;
		} else {
			myRows--;
			myData.remove(aRow);
			return true;
		}
	}
	
	/**
	 * Add a new row to the datastore. If the row is an incorrect
	 *  length, it will not be added. If the datastore has not yet
	 *  been initialized (created), adding a row will initialize it.
	 * @param newRow
	 * @return true if the row is added, false otherwise
	 */
	public boolean addRow(ArrayList<Double> newRow) {
		
		if(myData == null) {
			initializeTable();
			return this.add(newRow);
		} else if(myRows == 0) {
			if(myColumns == 0) {
				myColumns = newRow.size();
				return this.add(newRow);
			} else if(myColumns == newRow.size()) {
				return this.add(newRow);
			}
		} else if(myColumns == newRow.size()) {
			return this.add(newRow);
		}
		return false;
	}
	
	/**
	 * Add a new row to the datastore. If the row is an incorrect
	 *  length, it will not be added. If the datastore has not yet
	 *  been initialized (created), adding a row will initialize it.
	 * @param newRow
	 * @return true if the row is added, false otherwise
	 */
	public boolean addRow(Double[] newRow) {
		ArrayList<Double> temp = new ArrayList<Double>();
		
		for(int i=0;i<newRow.length;i++) {
			temp.add(newRow[i]);
		}
		
		return this.addRow(temp);
	}
	
	/**
	 * Add a new row to the datastore. If the row is an incorrect
	 *  length, it will not be added. If the datastore has not yet
	 *  been initialized (created), adding a row will initialize it.
	 * @param newRow
	 * @return true if the row is added, false otherwise
	 */
	public boolean addRow(double[] newRow) {
		ArrayList<Double> temp = new ArrayList<Double>();
		
		for(int i=0;i<newRow.length;i++) {
			temp.add(new Double(newRow[i]));
		}
		
		return this.addRow(temp);
	}
	
	/**
	 * Get the data stored in the requested row
	 * @param idx
	 * @return the ArrayList of the requested row
	 */
	public ArrayList<Double> getRow(int idx) {
		return myData.get(idx);
	}
	
	/**
	 * Get the data stored in the requested row
	 * @param idx
	 * @return the Double[] of the requested row
	 */
	public Double[] getRowDouble(int idx) {
		return (Double[]) myData.get(idx).toArray();
	}
	
	/**
	 * Returns a single specified value from the datastore
	 * @param row
	 * @param col
	 * @return the value
	 */
	public Double get(int row, int col) {
		return myData.get(row).get(col);
	}
	
	/**
	 * Returns number of rows in the table
	 * @return the number of rows
	 */
	public int rows() {
		return myRows;
	}
	
	/**
	 * Returns the number of columns in the table
	 * @return myColumns
	 */
	public int columns() {
		return myColumns;
	}
	
	/**
	 * Set up / create the headers for the datastore.
	 *  If headers already exist, the input is ignored.
	 * @param headers
	 * @return true if new headers are set up, false otherwise
	 */
	public boolean createHeaders(String[] headers) {
		
		if(dataHeaders == null || dataHeaders.size() == 0) {
			dataHeaders = new ArrayList<String>();
			for(int i=0;i<headers.length;i++) {
				dataHeaders.add(headers[i]);
			}
			myColumns = headers.length;
			return true;
		}
		return false;
	}
	
	/**
	 * Set up / create the headers for the datastore.
	 *  If headers already exist, the input is ignored.
	 * @param headers
	 * @return true if new headers are set up, false otherwise
	 */
	public boolean createHeaders(ArrayList<String> headers) {
		
		if(dataHeaders == null || dataHeaders.size() == 0) {
			dataHeaders = headers;
			myColumns = headers.size();
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the header ArrayList (can be null)
	 * @return headers
	 */
	public ArrayList<String> getHeaders() {
		return dataHeaders;
	}
	
	/**
	 * Returns the header ArrayList in a string format suitable
	 *  for use in a CSV
	 * @return CSV Formatted header string
	 */
	public String getHeaderString() {
		String result = "";
		
		for(int i=0;i<dataHeaders.size();i++) {
			result += dataHeaders.get(i) + ", ";
		}
		//truncate the trailing ", "
		result = result.substring(0, result.length()-2);
		
		return result;
	}
	
	/**
	 * Calculates the average of the specified column
	 * @param column
	 * @return the average value of the column
	 */
	public Double columnAvg(int column) {
		Double result = new Double(0);
		
		if(column == -1)
			return null;
		
		for(int i=0;i<myRows;i++) {
			result += myData.get(i).get(column);
		}
		
		result /= myRows;
		
		return result;
	}
	
	/**
	 * Calculates the average of the specified column
	 * @param column
	 * @return the average value of the column
	 */
	public Double columnAvg(String column) {
		int col = dataHeaders.indexOf(column);

		return this.columnAvg(col);
	}
	
	/**
	 * Calculates the Standard Deviation of the specified column
	 * @param column
	 * @return the standard deviation of the column
	 */
	public Double columnStdDev(int column) {
		Double result = new Double(0);
		Double avg = columnAvg(column);
		
		if(avg==null) {
			return null;
		} else {
			for(int i=0;i<myRows;i++) {
				result += Math.pow((myData.get(i).get(column) - avg), 2);
			}
			
			result /= myRows;
			result = Math.sqrt(result);
			
			return result;
		}
	}
	
	/**
	 * Calculates the Standard Deviation of the specified column
	 * @param column
	 * @return the standard deviation of the column
	 */
	public Double columnStdDev(String column) {
		int col = dataHeaders.indexOf(column);

		return this.columnStdDev(col);
	}
	
	/**
	 * Calculates the sum of the specified column
	 * @param column
	 * @return the sum of the column
	 */
	public Double columnSum(int column) {
		Double result = new Double(0);
		
		for(int i=0;i<myRows;i++) {
			result+=myData.get(i).get(column);
		}
		
		return result;
	}
	
	/**
	 * Calculates the sum of the specified column
	 * @param column
	 * @return the sum of the column
	 */
	public Double columnSum(String column) {
		int col = dataHeaders.indexOf(column);

		return this.columnSum(col);
	}
	
	/**
	 * Gets the maximum value in the passed in column
	 * @param column
	 * @return the maximum value
	 */
	public Double getColumnMax(String column) {
		int col = dataHeaders.indexOf(column);
		
		return this.getColumnMax(col);
	}
	
	/**
	 * Gets the maximum value in the passed in column
	 * @param col
	 * @return the maximum value
	 */
	public Double getColumnMax(int col) {
		int idx = 0;
		for(int i=1;i<myRows;i++) {
			if(myData.get(i).get(col) > myData.get(idx).get(col)) {
				idx = i;
			}
		}
		
		return myData.get(idx).get(col);
	}
	
	/**
	 * Gets the minimum value in the passed in column
	 * @param col
	 * @return the minimum value
	 */
	public Double getColumnMin(String column) {
		int col = dataHeaders.indexOf(column);
		
		return this.getColumnMin(col);
	}
	
	/**
	 * Gets the minimum value in the passed in column
	 * @param col
	 * @return the minimum value
	 */
	public Double getColumnMin(int col) {
		int idx = 0;
		
		for(int i=1;i<myRows;i++) {
			if(myData.get(i).get(col) < myData.get(idx).get(col)) {
				idx = i;
			}
		}
		
		return myData.get(idx).get(col);
	}
	
	/**
	 * Returns a datastore object that is rows [0, endRow). Functions like String.substring(int)
	 * @param endRow
	 * @return a datastore that is a subset of this datastore
	 */
	public Datastore getSubset(int endRow) {
		Datastore result = new Datastore();
		
		result.createHeaders(dataHeaders);
		for(int i=0;i<endRow;i++) {
			if(i >= myRows) {
				break;
			} else {
				result.addRow(myData.get(i));
			}
		}
		
		return result;
	}
	
	/**
	 * Returns a full column, not including the header.
	 * @param aColumn
	 * @return The column (an ArrayList<Double>) specified
	 */
	public ArrayList<Double> getColumn(int aColumn) {
		ArrayList<Double> result = new ArrayList<Double>();
		
		if(aColumn < 0 || aColumn >= myColumns) {
			return null;
		}
		
		for(int i=0;i<myRows;i++) {
			result.add(myData.get(i).get(aColumn));
		}
		
		return result;
	}
	
	/**
	 * Returns the header for a given column
	 * @param aColumn
	 * @return The header in question
	 */
	public String getColumnHeader(int aColumn) {
				
		return dataHeaders.get(aColumn);
	}
	
	/**
	 * Calculates the product of the specified row
	 * @param row
	 * @return the product of the row
	 */
	public Double rowProduct(int row) {
		Double result = new Double(myData.get(row).get(0));
		
		for(int i=1;i<myColumns;i++) {
			result *= myData.get(row).get(i);
		}

		return result;
	}
	
	/**
	 * Constructs & returns a string representation for the
	 *  datastore.
	 * @return the string value for the datastore
	 */
	public String toString(){
		String result = "";
		
		for(int i=-1;i<myRows;i++) {
			result += "[";
			for(int j=0;j<myColumns;j++) {
				if(j > 0) {
					result += ", ";
				}
				if(i == -1) {
					result += dataHeaders.get(j);
				} else {
					result += myData.get(i).get(j);
				}
			}
			result += "]\n";
		}
		
		return result;
	}
	
	/**
	 * Returns JSON representation of the data contained within
	 * @return JSON Representation of this datastore
	 */
	public String toJSON() {
		String result = "";
		String row;
		
		result += "{";
		result += "\"columns\":[";
		
		for(int i=0;i<dataHeaders.size();i++) {
			result += "\"";
			result += dataHeaders.get(i);
			result += "\",";
		}
		
		result = result.substring(0, result.length()-1);
		result += "],";
		result += "\"data\":[";
		
		for(int i=0;i<myData.size();i++) {
			if(myData.get(i).size() > 0) {
				result += "[";
				row = "" + myData.get(i).get(0); 
		
				for(int j=1;j<myData.get(i).size();j++) {
					row += "," + myData.get(i).get(j);
				}
				result += row + "],";
			}
		}
		
		result = result.substring(0, result.length()-1);
		result += "]";		
		result += "}";
		
		return result;
	}
	
	/**
	 * Returns a double[][] representation of the datastore
	 * @return a matrix (array) representation of the data
	 */
	public double[][] getMatrix() {
		double[][] result = null;
		
		result = new double[myRows][myColumns];
		for(int i=0;i<myRows;i++) {
			for(int j=0;j<myColumns;j++) {
				result[i][j] = myData.get(i).get(j);
			}
		}
		
		return result;
	}
	
	/**
	 * Trim the dataset to N Standard Deviations from Average
	 * @param deviations The number of deviations to trim to
	 */
	public void trimToNDeviations(double deviations) {
		double avg;
		double dev;
		double min;
		double max;
		
		System.out.println("Trimming to " + deviations + " Std.Deviations of average");
		System.out.println(" * Starting at " + myRows + " rows");
		
		for(int i=0;i<myColumns;i++) {
			avg = columnAvg(i);
			dev = columnStdDev(i);
			min = avg - deviations*dev;
			max = avg + deviations*dev;
			
			for(int j=0;j<myRows;j++) {
				if((myData.get(j).get(i) > max) || (myData.get(j).get(i) < min)) {
					this.remove(j);
					j--;
				}
			}
		}
		
		System.out.println(" * Finished at " + myRows + " rows");
		
	}
	
	/**
	 * Testing main. Arguments are ignored.
	 * @param args
	 */
	public static void main(String[] args) {
		Datastore test = new Datastore();
		String[] headers = {"x","y"};
		double[] row1 = {1,2};
		double[] row2 = {2,1};
		double[] row3 = {0,3};
		double[] ignoredRow = {1,2,3};
		
		
		test.createHeaders(headers);
		
		test.addRow(row1);
		test.addRow(row2);
		test.addRow(ignoredRow);
		test.addRow(row3);
		
		System.out.println(test.rows());
		System.out.println(test.columns());
		
		System.out.println(test);
		
		System.out.println(test.columnAvg(1));
		
		System.out.println(test.columnStdDev("x"));
		System.out.println(test.toJSON());

	}

}
