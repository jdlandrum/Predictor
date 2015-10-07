/**
 * 
 */
package com.predictor.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * @author jlandrum
 *
 */
public class NetIO implements IOTemplate, Runnable {

	private Socket mySocket;
	private ServerSocket myServerSocket;
	private int myPort;
	private int myTimeout;
	private Object socketLock;

	private InetAddress myIP;
	private InetAddress remoteIP;

	private ArrayList<String> sendBuffer;
	private ArrayList<String> recvBuffer;
	private Object bufferLock;

	private PrintWriter outStream;
	private BufferedReader inStream;
	private Object ioLock;

	/**
	 * Net IO Mode variable. Values can be: * s - send * r - receive
	 */
	private char myMode;

	private String myName;
	private Thread myThread;

	public NetIO() {
		this(8080, 10000, "NetIO", 's');
	}

	public NetIO(int aPort) {
		this(aPort, 10000, "NetIO", 's');
	}

	public NetIO(char aMode) {
		this(8080, 10000, "NetIO", aMode);
	}

	public NetIO(int aPort, int aTimeout) {
		this(aPort, aTimeout, "NetIO", 's');
	}

	public NetIO(int aPort, int aTimeout, String aName) {
		this(aPort, aTimeout, aName, 's');
	}

	public NetIO(int aPort, int aTimeout, String aName, char aMode) {
		myName = aName;
		myTimeout = aTimeout;

		// set to receive if mode is 'r'--send if it is anything else
		if (aMode == 'r') {
			myMode = 'r';
			if (aPort < 1) {
				myPort = 8080;
			} else {
				myPort = aPort;
			}
		} else {
			myMode = 's';
			if (aPort < 1) {
				myPort = 8080;
			} else {
				myPort = aPort;
			}
		}

		mySocket = null;
		myServerSocket = null;
		outStream = null;
		inStream = null;
		sendBuffer = new ArrayList<String>();
		recvBuffer = new ArrayList<String>();

		try {
			myIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		socketLock = new Object();
		bufferLock = new Object();
		ioLock = new Object();

		// TODO Thread this component and enable it to be a server
		// myThread = new Thread(this, myName);
	}

	private boolean connect() {

		synchronized (socketLock) {
			if (myMode == 's') {
				try {
					if(remoteIP == null) {
						System.out.println("No Destination IP--Setting to loopback...");
						remoteIP = InetAddress.getLoopbackAddress();
					}
					mySocket = new Socket(remoteIP, myPort);
					mySocket.setSoTimeout(myTimeout);
				} catch (IOException e) {
					// TODO HANDLE IO EXCEPTION (aside from returning false)
					return false;
				}
			} else if (myMode == 'r') {
				// TODO HANDLE SERVER (a real server, not what I've temporarily
				// cobbled together)
				try {
					myServerSocket = new ServerSocket(myPort);
					myServerSocket.setSoTimeout(myTimeout);
					System.out.println("Server Waiting...");
					myThread = new Thread(this, myName);
					myThread.start();
				} catch (IOException e) {
					if (myServerSocket != null) {
						if (myServerSocket.isBound()) {
							try {
								myServerSocket.close();
							} catch (IOException e1) {
								// TODO DO NOTHING HERE (don't care--we're
								// already in a failure state)
							}
						}
						myServerSocket = null;
					}
					return false;
				}
			}
		}

		return true;
	}

	private boolean disconnect() {

		synchronized (ioLock) {
			synchronized (socketLock) {
				if (outStream != null) {
					outStream.close();
					outStream = null;
				}
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						// DO NOTHING FOR NOW
					}
					inStream = null;
				}
				if (mySocket != null) {
					try {
						mySocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						// DO NOTHING FOR NOW
					}
					mySocket = null;
				}
			}
		}

		return true;
	}

	public void setDestination(String ip) {
		try {
			remoteIP = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean setMode(char aMode) {
		boolean success = false;

		// only change mode if we don't have a socket open
		if (mySocket == null && myServerSocket == null) {
			myMode = aMode;
			success = true;
		}

		return success;
	}
	
	public void setTimeout(int millis) {
		myTimeout = millis;
	}

	/**
	 * Atomic send action. Sends the full string, flushes buffer, closes the
	 * output object, and nulls it.
	 * 
	 * @param output
	 * @return True on success, false otherwise.
	 */
	private boolean send(String output) {
		synchronized (ioLock) {
			try {
				outStream = new PrintWriter(mySocket.getOutputStream(), true);
			} catch (IOException e) {
				// TODO HANDLE IO EXCEPTION (aside from returning false)
				return false;
			}
			outStream.print(output);
			outStream.flush();
			outStream.close();
			outStream = null;
			disconnect();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.predictor.io.IOTemplate#write(java.lang.String)
	 */
	@Override
	public boolean write(String output) {
		if (myMode == 's') {
			if (mySocket == null) {
				if (connect()) {
					return send(output);
				}
			} else {
				return send(output);
			}
		} else {
			return false;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.predictor.io.IOTemplate#writeln(java.lang.String)
	 */
	@Override
	public boolean writeln(String output) {
		return write(output + "\n");
	}

	private int readCh() {
		int in;
		synchronized (ioLock) {
			try {
				inStream = new BufferedReader(new InputStreamReader(
						mySocket.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return -1;
			}
			try {
				in = inStream.read();
				disconnect();
				return in;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return -1;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.predictor.io.IOTemplate#read()
	 */
	@Override
	public int read() {
		int in = -1;

		if (myMode == 'r') {
			if (mySocket == null) {
				if (connect()) {
					in = readCh();
				}
			} else {
				in = readCh();
			}
		}

		return in;
	}

	private String readLine() {
		String in;
		synchronized (ioLock) {
			try {
				inStream = new BufferedReader(new InputStreamReader(
						mySocket.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return null;
			}
			try {
				in = inStream.readLine();
				disconnect();
				return in;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return null;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.predictor.io.IOTemplate#readln()
	 */
	@Override
	public String readln() {
		String line = "";

		if (myMode == 'r') {
			if (mySocket == null) {
				if (connect()) {
					line = readLine();
				}
			} else {
				line = readLine();
			}
		}
		disconnect();
		return line;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.predictor.io.IOTemplate#open()
	 */
	@Override
	public boolean open() throws IOException {
		return connect();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.predictor.io.IOTemplate#close()
	 */
	@Override
	public boolean close() {
		return disconnect();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		// We'll do something more here once the components are ready for this to be
		// a semi-real server. Just supports being a simple server that listens for 1
		// connection for now
				
		try {
			mySocket = myServerSocket.accept();
			mySocket.setSoTimeout(myTimeout);
			System.out.println("Connected!");
			myServerSocket.close();
			myServerSocket = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NetIO in, out;
		String temp = "";

		in = new NetIO();
		out = new NetIO();

		in.setMode('r');
		in.setTimeout(30000);

		in.connect();
		out.connect();
		
		out.writeln("Hello World!");

		temp = in.readln();

		System.out.println("Received: " + temp);
	}

}
