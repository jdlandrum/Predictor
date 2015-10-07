/**
 * 
 */
package com.predictor.io;

import java.io.IOException;

/**
 * @author jland_000
 *
 */
public interface IOTemplate {
	
	public boolean write(String output);
	public boolean writeln(String output);
	
	public int read();
	public String readln();
	
	public boolean open() throws IOException;
	public boolean close();
	
}
