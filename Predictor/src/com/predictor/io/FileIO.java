/**
 * 
 */
package com.predictor.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author jland_000
 *
 */
public class FileIO implements IOTemplate {

	private String myPath;
	private String myFilename;
	
	/**
	 * File IO Mode variable.
	 *  Values can be:
	 *  * c - closed
	 *  * w - write
	 *  * a - append
	 *  * r - read
	 */
	private char myMode;
	private BufferedReader myReader;
	private BufferedWriter myWriter;
	private boolean fileOpen;
	
	public FileIO() throws IOException {
		this("." + File.pathSeparator, null, 'c');
	}
	
	public FileIO(String newName, char newMode) throws IOException {
		this("." + File.separatorChar, newName, newMode);
	}
	
	public FileIO(String newPath, String newName, char newMode) throws IOException {
		
		fileOpen = false;

		myPath = newPath;
		myFilename = newName;
		myMode = newMode;
		
		if(myPath == null) {
			myPath = "." + File.separator;
		} else {
			
			myPath = this.sanitizePath(myPath, File.separatorChar);
			
			if(myPath.charAt(myPath.length()-1) != File.separatorChar) {

				myPath += File.separator;
			}
		}

		
		if(myFilename == null) {
			myFilename = "FileIO_Log.txt";
		}
		
		this.initialize();
	}
	
	private String sanitizePath(String aPath, char sepChar) {
		String result = "";
		char badSepChar = ' ';
		
		if(sepChar == '\\') {
			badSepChar = '/';
		} else if(sepChar == '/') {
			badSepChar = '\\';
		} else {
			return aPath;
		}
		
		for(int i=0;i<aPath.length();i++) {
			if(aPath.charAt(i) == badSepChar) {
				result += sepChar;
			} else {
				result += aPath.charAt(i);
			}
		}
		
		return result;
	}
	
	/**
	 * Helper method for constructor--initializes read/write objects properly
	 *  and performs exception handling.
	 * @throws IOException 
	 */
	private void initialize() throws IOException {
		if(myMode == 'w') {
			this.openForWrite();
		} else if(myMode == 'a') {
			this.openForAppend();
		} else if(myMode == 'r') {
			this.openForRead();
		} else {
			myMode='c';
		}
	}
		
	/**
	 * @return the myPath
	 */
	public String getMyPath() {
		return myPath;
	}

	/**
	 * @param myPath the myPath to set
	 */
	public void setMyPath(String myPath) {
		this.myPath = myPath;
	}

	/**
	 * @return the myFilename
	 */
	public String getMyFilename() {
		return myFilename;
	}

	/**
	 * @param myFilename the myFilename to set
	 */
	public void setMyFilename(String myFilename) {
		this.myFilename = myFilename;
	}

	/**
	 * Accessor to read the file access state variable
	 * @return the file access mode
	 */
	public char getMyMode() {
		return myMode;
	}
	
	/**
	 * Convenience method; returns the file access state in a string
	 * @return the file access mode
	 */
	public String getMyModeStr() {
		
		switch(myMode) {
		case 'a': return "appending";
		case 'r': return "reading";
		case 'w': return "writing";
		default: return "closed";
		}
	}

	/**
	 * @param myMode the myMode to set
	 */
	public void setMyMode(char myMode) {
		if(this.isOpen()) {
			this.close();
		}
		this.myMode = myMode;
	}
	
	/**
	 * Check to see whether the file is open for use
	 * @return true if open, false otherwise
	 */
	public boolean isOpen() {
		return fileOpen;
	}

	/* (non-Javadoc)
	 * @see com.predictor.io.IOTemplate#write(java.lang.String)
	 */
	@Override
	public boolean write(String output) {
		
		try {
			myWriter.write(output);
			myWriter.flush();
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see com.predictor.io.IOTemplate#writeln(java.lang.String)
	 */
	@Override
	public boolean writeln(String output) {
		
		try {
			myWriter.write(output);
			myWriter.newLine();
			myWriter.flush();
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see com.predictor.io.IOTemplate#read()
	 */
	@Override
	public int read() {
		int in = -1;

		try {
			in = myReader.read();
		} catch (IOException e) {
			// TODO DO SOMETHING OTHER THAN NOTHING
		}
		
		return in;
		
	}

	/* (non-Javadoc)
	 * @see com.predictor.io.IOTemplate#readln()
	 */
	@Override
	public String readln() {
		
		try {
			return myReader.readLine();
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Helper method. Tries to open a file for reading.
	 * @return True if file is open for read, false otherwise
	 * @throws IOException
	 */
	private boolean openForRead() throws IOException {
		try {
			myReader = Files.newBufferedReader(Paths.get(myPath, myFilename));
			fileOpen = true;
		} catch (IOException e) {
			throw e;
		}
		return fileOpen;
	}
	
	/**
	 * Helper method. Tries to open a file for Writing.
	 * @return True if file is open for writing, false otherwise
	 * @throws IOException
	 */
	private boolean openForWrite() throws IOException {
		try {
			myWriter = Files.newBufferedWriter(Paths.get(myPath, myFilename),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			fileOpen = true;
		} catch (IOException e) {
			throw e;
		}
		return fileOpen;
	}
	
	/**
	 * Helper method. Tries to open a file such that writes are appended to it.
	 * @return True if file is open for appending, false otherwise
	 * @throws IOException
	 */
	private boolean openForAppend() throws IOException {
		try {
			myWriter = Files.newBufferedWriter(Paths.get(myPath, myFilename), 
					StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			fileOpen = true;
		} catch (IOException e) {
			throw e;
		}
		return fileOpen;
	}

	@Override
	public boolean open() throws IOException {
		if(!fileOpen) {
			if(myMode=='r') {
				return this.openForRead();
			} else if(myMode=='w') {
				return this.openForWrite();
			} else if(myMode=='a') {
				return this.openForAppend();
			}
		}
		return fileOpen;
	}

	@Override
	public boolean close() {

		if(myMode != 'c') {
			try {
				if(myReader != null) {
					myReader.close();
					myReader=null;
				} else if(myWriter != null) {
					myWriter.close();
					myWriter=null;
				}
				fileOpen = false;
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Testing main. Validates basic functionality.
	 * @param args
	 */
	public static void main(String[] args) {
		FileIO tester = null;
		int dummy = 0;
		
		/* 
		 * test writing to a dummy file
		 */
		try {
			tester = new FileIO(".\\", "test.txt", 'w');
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(tester.isOpen()) {
			tester.write("Hello World!\n");
			tester.writeln("A quick brown fox jumped over the lazy dog.");
			tester.close();
		}
		
		/*
		 * test appending to the dummy file
		 */
		tester.setMyMode('a');
		
		try {
			tester.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(tester.isOpen()) {
			tester.writeln("This is an appended line");
			tester.close();
		}
		
		/*
		 * test reading from dummy file
		 */
		tester.setMyMode('r');
		
		try {
			tester.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(tester.isOpen()) {
			System.out.println("This line is read in all at once:");
			System.out.println(tester.readln());
			System.out.println("\nEverything else in the file is read in char by char:");
			while((dummy = tester.read()) != -1) {
				System.out.print(Character.toChars(dummy));
			}
			tester.close();
		}
		
	}

}
