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
public interface WriterTemplate {
	public boolean writeString(IOTemplate output, String str);
	public boolean writeStringLn(IOTemplate output, String str);
	public boolean writeAllData(IOTemplate output, Datastore data);
	public boolean writeDataRow(IOTemplate output, Datastore data, int row);
	public boolean writeDataHeader(IOTemplate output, Datastore data);
}
