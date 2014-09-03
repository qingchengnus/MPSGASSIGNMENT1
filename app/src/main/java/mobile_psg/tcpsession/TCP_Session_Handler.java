package mobile_psg.tcpsession;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import mobile_psg.mpsgStarter.MPSG;
import mobile_psg.mpsgStarter.MpsgStarter;
import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.util.Log;

public class TCP_Session_Handler {
	public static Socket socket = null;
	static InetAddress serverAddr = null;
	static int serverPort = 5000;
	int registerDataUsage = 0;
	
	public TCP_Session_Handler connectServer(InetAddress serverAddr, int serverPort) {
		try {
			socket = new Socket(serverAddr, serverPort);
			serverAddr = socket.getInetAddress();
			serverPort = socket.getPort();
		} catch (IOException e) {
			Log.d("MPSG", "Unable to create Socket. Error: " + e.toString());
			return null;
		}
		return this;
	}
	
	@SuppressLint("NewApi")
	private void monitorTCP() {
		Log.d("MPSG", "Starting Monitor TCP");
		PrintWriter out = null;
		BufferedReader in = MPSG.datain;
		
		try {
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		} catch (Exception e) {
			Log.d("MPSG", "Error in creating reader/writer to socket");
			e.printStackTrace();
		}
		
		/*Thread monitorLastHandShake = new Thread() {
			public void run() {
				while (true) {
					Log.d("MPSG", "Time since last handshake: " + (System.currentTimeMillis() - MPSG.lastHandshake)/1000);
					if ((MPSG.lastHandshake > 0) && (System.currentTimeMillis() - MPSG.lastHandshake)/1000 > 5) {
						Log.d("MPSG", "Probable hung connection. Send refresh update to proxy");
						Thread refreshConnection = new Thread() {
							public void run () {
								MPSG.connect();
							}
						};
						refreshConnection.start();
						return;
					}
					try { 
						Thread.sleep(1000); 
					} catch(Exception e) {
						Log.d("MPSG", "Error in thread monitoring handshake");
					}
				}
			}
		};
		monitorLastHandShake.start();*/
		
		try {
			while(Boolean.TRUE) { // wait for any request from server
				String data = in.readLine();
				//Log.d("MPSG", data + "..");
				if (data != null) {
					if (data.startsWith("update")) { // query format "update::person.location;person.mood"
						Log.d("MPSG", "Server looking for updated data");
						Log.d("MPSG", "Update req: " + data);
						String temp[] = data.split("::");
						
						String attrib[] = temp[1].split(";");
						String response = "update::";
						Log.d("MPSG", "Attribs: " + attrib);

						MPSG.DynamicContextData.put("person.acceleration", "slow");
						MPSG.DynamicContextData.put("person.gravity", "high");
						MPSG.DynamicContextData.put("person.light", "medium");
						MPSG.DynamicContextData.put("person.magnetism", "positive");
						
						for (String attribute : attrib) {
							Log.d("MPSG", "Request for attrib: " + attribute);
							response = response.concat(attribute+"::");
							// Get the updated context information from MPSG class
							response = response.concat((String) MPSG.DynamicContextData.get(attribute));
							response = response.concat(",");
						}
					
						Log.d("MPSG", "Reply response: " + response);
						// Send the response to the proxy (empty if not found)
						if (response != "update::") {
							Log.d("MPSG", "Responding to proxy for update req: " + response);
							out.println(response);  // response format "update::person.location:soclab;person.mood:happy"
						} else {
							out.println("empty");
						}
						
						// Continue listening to proxy requests
						continue; 
					}
					else if (data.startsWith("there")) {
						out.println("yes there");
					//	Log.d("MPSG", "Replied for presence request");
						//MPSG.lastHandshake = System.currentTimeMillis();
						//Log.d("MPSG", "Last handshake: " + MPSG.lastHandshake);
					}
					else if (data.contains(":::")) {
						MPSG.queryData += data.getBytes().length;
						Log.d("MPSG", "Got response for this query");
						String result[] = data.split(":::");
						Log.d("EXPERIMENTAL_RESULTS", "Data usage for query send: " + MPSG.queryData);
						if (!result[1].isEmpty()) {
							MpsgStarter.setQueryResult("Query:"+ result[0] +", Result:" + result[1]);
						}
					}
				}
			}
		} catch (Exception e) {
			Log.d("MPSG", "Error in monitoring socket");
			MPSG.state = "disconnected";
			return;
			//e.printStackTrace();
		}
	}
	
	public String registerWithProxy(String mpsgName, String contextType, String contextData) {
		registerDataUsage += MPSG.discoveryUsage; // Add data usage from discovery to total registration usage
		Log.d("MPSG", "Waiting for response from proxy" );
		BufferedReader in = null;
		int timeout = 20; // add 5 seconds as timeout for Proxy to respond
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			int i = 0;
			while(i < timeout) { // wait for confirmation reply from server
				String line = in.readLine();
				if (line != null) {
					registerDataUsage += (line.getBytes().length + 20); // Add received data to usage
					Log.d("MPSG", "Register with proxy: Received data from proxy: " + line); 
					if (line.equalsIgnoreCase("send name and context")) {
						Log.d("MPSG", "Proxy asking for name and context information");
						PrintWriter out = null;
						try {
							out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
						} catch (IOException e) {
							Log.d("MPSG", "Unable to get write socket");
							e.printStackTrace();
						}
						registerDataUsage += ((mpsgName+";"+contextType +";"+ contextData).getBytes().length+20); // Add sent bytes to dataUsage
						out.println(mpsgName+";"+contextType +";"+ contextData ); // Send context information to proxy
						int j = 0;
						while (j < timeout) {
							String line1 = in.readLine();
							if (line1 != null) {
								registerDataUsage += (line1.getBytes().length+20); // Add received data to usage
								if (line1.startsWith("Success:")) {
									Log.d("MPSG", "MPSG Registration " + line1);
									MPSG.datain = in;

									MPSG.lastHandshake = 0;
									// Start a thread to monitor the socket connection and handle replies
									Thread monitorTCPThread = new Thread() {
										public void run() {
											monitorTCP();
										}
									};
									monitorTCPThread.start();
									Log.d("EXPERIMENTAL_RESULTS", "Data usage for Proxy Discovery: " + MPSG.discoveryUsage);
									Log.d("EXPERIMENTAL_RESULTS", "Total Data usage for Registration: " + registerDataUsage);
									
									return("MPSG Registration " + line1);
								} else if (line1.startsWith("Fail")) {
									Log.d("MPSG", "Registration with Coalition failed");
									return "Registration with Coalition failed in proxy";
								}
							}
							try {
							    Thread.sleep(1000);
							    j++;
							} catch(InterruptedException e) {
								Log.d("MPSG", "Thread sleep interrupted");
							    Thread.currentThread().interrupt();
							}
						}
						Log.d("MPSG", "Registration with Coalition failed");
						return "Registration with Coalition failed in proxy";
					}
				}
				try {
				    Thread.sleep(1000);
				    i++;
				} catch(InterruptedException e) {
					Log.d("MPSG", "Thread sleep interrupted");
				    Thread.currentThread().interrupt();
				}
			}
		} catch (IOException e) {
			Log.d("MPSG", "Error in registering with proxy");
			e.printStackTrace();
		}
		Log.d("MPSG", "Failed in getting response from proxy");
		return "Failed in getting response from proxy";
	}
	
	/**
	 * Closes the session with old proxy gracefully
	 * @param mpsgName
	 * @return
	 */
	public boolean closeSessionWithOldProxy(String mpsgName, InetAddress newProxy, InetAddress oldProxy) {
		Socket socket = null;
		BufferedReader in = null;
		Log.d("MPSG", "Connecting to old proxy: " + oldProxy);
		try {
			socket = new Socket(oldProxy, serverPort);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			Log.d("MPSG", "Unable to create Socket. Error: " + e.toString());
		}
		Log.d("MPSG", "Connected to old proxy for session close");
		Log.d("MPSG", "Waiting for response from old proxy");
		Log.d("MPSG", "Old Proxy Socket details: remoteIP="+socket.getInetAddress()+",remotePort:"+socket.getPort());
		int timeout = 5; // add 5 seconds as timeout for Proxy to respond
		try {
			int i = 0;
			while(i < timeout) { // wait for confirmation reply from server
				String line = in.readLine();
				if (line != null) {
					Log.d("MPSG", "Close session: Received data from proxy: " + line); 
					if (line.equalsIgnoreCase("send name and context")) {
						Log.d("MPSG", "Old Proxy asking for name and context information");
						PrintWriter out = null;
						try {
							out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
						} catch (IOException e) {
							Log.d("MPSG", "Unable to get write socket");
							e.printStackTrace();
						}
						out.println("close:" + mpsgName +":" + newProxy.getHostAddress()); // Send close request to old proxy
						Log.d("MPSG", "Wait for any more information request from Proxy");
						int j = 0;
						while (j<timeout) {
							String line2 = in.readLine();
							PrintWriter out2 = null;
							try {
								out2 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
							} catch (IOException e) {
								Log.d("MPSG", "Unable to get write socket");
								e.printStackTrace();
							}
							if (line2 != null) {
								if (line2.startsWith("update")) { // query format "update::person.location;person.mood"
									Log.d("MPSG", "Server looking for updated data");
									Log.d("MPSG", "Update req: " + line2);
									Log.d("MPSG", "Server looking for updated data");
									String temp[] = line2.split("::");
									
									String attrib[] = temp[1].split(";");
									String response = "update::";
									Log.d("MPSG", "Attribs: " + attrib);
									
									for (String attribute : attrib) {
										Log.d("MPSG", "Request for attrib: " + attribute);
										response = response.concat(attribute+":");
										// Get the updated context information from MPSG class
										response = response.concat((String) MPSG.DynamicContextData.get(attribute));
										response = response.concat(";");
									}
									Log.d("MPSG", "Reply response: " + response);
									// Send the response to the proxy (empty if not found)
									if (response != "update::") {
										Log.d("MPSG", "Responding to proxy for update req: " + response);
										out2.println(response);  // response format "update::person.location:soclab;person.mood:happy"
									} else {
										out2.println("empty");
									}
								} else if(line2.startsWith("closeok")) {
									Log.d("MPSG", "Got closeOK from old Proxy");
									return true;
								}
							}
							try {Thread.sleep(500);} catch (Exception e){}
						}
					}
				}
				try {
				    Thread.sleep(500);
				    i++;
				} catch(InterruptedException e) {
					Log.d("MPSG", "Thread sleep interrupted");
				    Thread.currentThread().interrupt();
				}
			}
		} catch (IOException e) {
			Log.d("MPSG", "Error in closing with old proxy");
			e.printStackTrace();
		}
		
		/*// Start a thread to monitor the socket connection and handle replies
		Thread monitorTCPThread = new Thread() {
			public void run() {
				monitorTCP();
			}
		};
		monitorTCPThread.start();*/
		return Boolean.TRUE;
	}
	
	/**
	 * Send the query to proxy and wait for reply
	 * @param query
	 */
	public void sendQuery(String query) {
		long startTime = System.currentTimeMillis();
		final String queryStr = query;
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		} catch (IOException e) {
			Log.d("MPSG", "Unable to get write socket");
			e.printStackTrace();
		}
		MPSG.queryData = query.getBytes().length;
		out.println(query); // Send query string to proxy
		MPSG.queryStart = System.currentTimeMillis();
		Log.d("EXPERIMENTAL_RESULTS", "Time to send query from MPSG:" + Math.abs(System.currentTimeMillis() - startTime));
		Log.d("MPSG", "Query string sent to proxy");
		Log.d("MPSG", "Starting thread to listen for query response");
	}
	


	/**
	 * Send request to leave from coalition
	 * @param 
	 * @return
	 */
	public String removeFromCoalition() {
		Log.d("MPSG", "Into remove from coalition");
		if (MPSG.datain.equals(null)) {
			return "leavefailed:socket empty";
		}
		BufferedReader in = MPSG.datain;
		PrintWriter out = null;
		int timeout = 10; // Wait for 10 seconds to get leave reply from proxy
		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		} catch (IOException e) {
			Log.d("MPSG", "Unable to get write socket");
			e.printStackTrace();
		}
		out.println("leaveCoalition"); // Send query string to proxy
		return "leavesuccess";
	}

	public void enableKeepalive() {
		try {
			socket.setKeepAlive(Boolean.TRUE);
		} catch (SocketException e) {
			Log.d("MPSG", "Unable to start Keeapalives");
			e.printStackTrace();
		}
		return;
	}
	
	public boolean isAlive() {
		while (Boolean.TRUE) {
			if (!socket.isClosed() && !socket.isInputShutdown() && !socket.isOutputShutdown()) {
				try {
					this.wait(2000);
				} catch (InterruptedException e) {
					Log.d("MPSG", "Exception in wait.");
					e.printStackTrace();
					return Boolean.FALSE;
				}
				continue;
			} else {
				break;
			}		
		}
		return Boolean.FALSE;
	}

	/**
	 * This reconnects to same proxy ip and modifies the socket 
	 */
	public static void reconnect() {
		// Connect to old proxy and get if there is any ongoing session
		
		
	}
}
