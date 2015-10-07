/**
 * 
 */
package com.predictor.io;

import java.util.ArrayList;

import com.predictor.components.Datastore;

/**
 * @author jland_000
 *
 */
public interface ReaderTemplate {

	public ArrayList<String> readRow(IOTemplate input);
	public String[] readRowStringArray(IOTemplate input);
	public String readRowString(IOTemplate input);
	public void readAllData(IOTemplate input, Datastore data);
	public Datastore readAllData(IOTemplate input);
}
