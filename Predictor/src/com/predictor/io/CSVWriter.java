/**
 * 
 */
package com.predictor.io;

import java.util.ArrayList;

import com.predictor.components.Datastore;

/**
 * @author jlandrum
 *
 */
public class CSVWriter implements WriterTemplate {

	/* (non-Javadoc)
	 * @see com.predictor.io.WriterTemplate#writeString(com.predictor.io.IOTemplate, java.lang.String)
	 */
	@Override
	public boolean writeString(IOTemplate output, String str) {
		boolean success = false;
		
		success = output.write(str);
		
		return success;
	}

	/* (non-Javadoc)
	 * @see com.predictor.io.WriterTemplate#writeStringLn(com.predictor.io.IOTemplate, java.lang.String)
	 */
	@Override
	public boolean writeStringLn(IOTemplate output, String str) {
		return writeString(output, (str + "\n"));
	}

	/**
	 * Renamed method to make the class easier to use. Simply
	 *  chains to writeAllData
	 * @param output
	 * @param data
	 * @return true on success, false otherwise
	 */
	public boolean writeCSV(IOTemplate output, Datastore data) {
		return this.writeAllData(output, data);
	}
	
	/* (non-Javadoc)
	 * @see com.predictor.io.WriterTemplate#writeAllData(com.predictor.io.IOTemplate, com.predictor.components.Datastore)
	 */
	@Override
	public boolean writeAllData(IOTemplate output, Datastore data) {
		boolean success = false;
		int rows = data.rows();
		ArrayList<Double> row;
		String temp;
		
		//write headers
		output.writeln(data.getHeaderString());
		
		for(int i=0;i<rows;i++) {
			temp = "";
			row = data.getRow(i);
			for(int j=0;j<row.size();j++) {
				temp += row.get(j) + ", ";
			}
			//truncate the trailing ", "
			temp = temp.substring(0, temp.length()-2);
			success = output.writeln(temp);
			//short circuit
			if(!success)
				return false;
		}
		
		return success;
	}

	/* (non-Javadoc)
	 * @see com.predictor.io.WriterTemplate#writeDataRow(com.predictor.io.IOTemplate, com.predictor.components.Datastore, int)
	 */
	@Override
	public boolean writeDataRow(IOTemplate output, Datastore data, int row) {
		boolean success = false;
		String out = "";
		ArrayList<Double> temp = data.getRow(row);
		
		for(int i=0;i<temp.size();i++) {
			out += temp.get(i) + ", ";
		}
		
		//truncate the trailing ", "
		out = out.substring(0, out.length()-2);
		
		success = output.writeln(out);
		
		return success;
	}

	@Override
	public boolean writeDataHeader(IOTemplate output, Datastore data) {
		boolean success = false;
		
		success = output.writeln(data.getHeaderString());
		
		return success;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
