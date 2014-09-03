package mobile_psg.proxysearch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import mobile_psg.mpsgStarter.MPSG;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

/**
 * @author Sathiya
 * Proxy code for performing Datagram based subnet search for proxy
 */
public class SearchSubnet_PSG {
	static InetAddress proxyIP = null;
	boolean noProxy = true; // status to check if atleast potential proxy was identified
	boolean gaveUp = false; // status to check if algorithm gave up checking within subnet
	static boolean newSearch = true;
	
	InetAddress src = null;
	InetAddress srcBcast = null;
	Context myContext;
	int THRESHOLD = 3; // Number of seconds to search for proxy within subnet
	
	int JOIN_SENDPORT = 12345;
	int PROXY_ADPORT = 12344;
	int PROXY_AD_LISTENPORT = 12346;
	int JOIN_OK_LISTENPORT = 12347;
	
	static DatagramSocket startProxyListener_socket = null; 
	static DatagramSocket searchProxy_socket = null;
	static DatagramSocket startJoinOKListener_socket = null;
	
	public SearchSubnet_PSG () {
		try {
			searchProxy_socket = new DatagramSocket();
			startProxyListener_socket = new DatagramSocket(PROXY_AD_LISTENPORT);
			startJoinOKListener_socket = new DatagramSocket(JOIN_OK_LISTENPORT);
		} catch (Exception e) {
			Log.d("MPSG", "Error in creating sockets");
		}
	}
	
	/**
	 * Initialize the broadcast address required
	 * @param none 
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("NewApi")
	public void init() {
		
		// Reset all variables
		proxyIP = null;
		noProxy = true;
		gaveUp = false;
		newSearch = true;
		src = null;
		srcBcast = null;
		
		// Initialize caller object
		String tempStr = null;
	    System.setProperty("java.net.preferIPv4Stack", "true"); 
	    try {
	    	Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
	        while (niEnum.hasMoreElements()) {
	        	NetworkInterface ni = niEnum.nextElement();
	            if(!ni.isLoopback()){
	            	for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses())
	                {
	            		if (interfaceAddress.getBroadcast() != null) {
		            		tempStr = interfaceAddress.getBroadcast().toString();
		            		tempStr = tempStr.substring(1);
		            		srcBcast = InetAddress.getByName(tempStr);
		            		break;
	            		}
	                }
	            }
	            if (srcBcast != null) break;
	         }
	    }
	    catch (SocketException e) { 
	    	e.printStackTrace();
	    }
	    catch (UnknownHostException e) {
	    	e.printStackTrace();
	    }
		Log.d("MPSG", "Bcast ip: " + srcBcast);
		Log.d("TerseLog", "Bcast ip: " + srcBcast);
		
		if (srcBcast == null) {
			Log.d("MPSG", "Please switch on wifi");
			Log.d("TerseLog", "Please switch on wifi");
			MPSG.inWifi = false;
			return;
		}
		
		// Start the search process
		start();
		/*
		// Reset all variables
		proxyIP = null;
		noProxy = true;
		gaveUp = false;
		newSearch = true;
		src = null;
		srcBcast = null;*/
	}
	
	/**
	 * Creates a custom UDP packet for the given message string
	 * @parameters message and destination
	 * @return UDP Packet which was created
	 *
	 */
	private DatagramPacket createUDP(String message, InetAddress dst, int port) {
		byte[] sendData = message.getBytes();
		
		DatagramPacket udp = null;
		
		try {
			udp = new DatagramPacket(sendData, sendData.length, dst, port);
		} catch (Exception e) {
			Log.d("MPSG", "Error in creating UDP packet to send");
			e.printStackTrace();
		}
		// Add size of udp packet to the data usage
		MPSG.discoveryUsage += (udp.getLength() + 42); // UDP(8)+IP(20)+Ethernet(14) header length
		//Log.d("EXPERIMENTAL_RESULTS", "Sending UDP data length(1): " + udp.getData()+42);
		Log.d("EXPERIMENTAL_RESULTS", "Sending UDP data length(2): " + (udp.getLength() + 42));
        return udp;
	}
	
	public void start() {
		// Start proxy listener in passive mode; wait for IAMPROXY message
		Log.d("MPSG", "Started proxy search");
		Log.d("TerseLog", "Started proxy search");
		for (int i=0; i<2; i++) {
			startProxyListener(false); 
			if (!noProxy) { // Resolved proxy
				MPSG.subnetSearchStatus = true;
				return;
			}
		} 
		// If proxy not resolved yet
		searchProxy();
		MPSG.subnetSearchStatus = true;
	}
	
	/**
	 * Starts the proxy search and runs continuously till Proxy is resolved 
	 * Sends 1 advertisement per 1 second
	 */
	private void searchProxy() {
		newSearch = false;
		// Start Proxy listener in loop mode
		Log.d("MPSG", "Going to start proxy listener");
		Thread proxyListener = new Thread() {
			public void run() {
				startProxyListener(true); 
			}
		};
		proxyListener.start();
		
		try {
			searchProxy_socket.setBroadcast(true);
		} catch (SocketException e) {
			Log.d("MPSG", "Error in creating socket to send packet");
			e.printStackTrace();
		}

		int counter = 0;
		while(true) {
			if (noProxy && !gaveUp) { // Check if algorithm still searching for IAMPROXY message
				DatagramPacket udp = null;
				udp = createUDP("MOBILE_REG_REQ_IAMPSG", srcBcast, PROXY_ADPORT);
				try {
					searchProxy_socket.send(udp);
				} catch (IOException e) {
					Log.d("MPSG", "Error in sending MOBILE_REG_REQ_IAMPSG");
					e.printStackTrace();
				}	
				try {
					Thread.sleep(1000);
					counter++;
				} catch (InterruptedException e) {
					Log.d("MPSG", "Error in thread sleep");
					e.printStackTrace();
				}
				if (counter > THRESHOLD) 
					break;
				if (newSearch) {
					Log.d("MPSG", "New search process started, as network changed");
					return;
				}
			} else {
				break;
			}
		} 
		Log.d("MPSG", "Subnet search done! Contact MPSG object to find out status of subnet search");
		
		return;
	}
	
	private void startProxyListener(boolean loop) {
		Log.d("MPSG", "Proxy listener started");
		byte[] recvBuf;
		DatagramPacket udprcvd = null;
		DatagramPacket udpsend = null;

		try {
            Log.d("MPSG", "Creating datagram socket");
            startProxyListener_socket.setSoTimeout(2000); // Wait for 2 seconds to get 'IAMPROXY' message
		} catch (SocketException e) {
			Log.d("MPSG", "Error in creating socket to send packet");
			e.printStackTrace();
		}

		int counter = 0;
		do {
			recvBuf = new byte[15000];
			Log.d("MPSG", "Initializing receiver buffer");
			udprcvd = new DatagramPacket(recvBuf, recvBuf.length);

			if (udprcvd != null) {
				Log.d("MPSG", "Initializing udp receiver");
				try {
					Log.d("MPSG", "Waiting for udp packet");
					startProxyListener_socket.receive(udprcvd);
					MPSG.discoveryUsage += (udprcvd.getLength() + 42); // Add received length to data usage
					//Log.d("EXPERIMENTAL_RESULTS", "Received UDP data length (1) : " + udprcvd.getData()+42);
					Log.d("EXPERIMENTAL_RESULTS", "Received UDP data length (2) : " + (udprcvd.getLength() + 42));
					Log.d("MPSG", "Received a udp frame");
				} catch (Exception e) {
					Log.d("MPSG", "Error in receiving UDP packet");
					e.printStackTrace();
					break;
				}
				String message = new String(udprcvd.getData()).trim();
				Log.d("MPSG", "Looking for proxy message");
				if (message.equalsIgnoreCase("MOBILE_REG_REQ_IAMPROXY")) {
					Log.d("MPSG", "Got i am proxy message from ip " + udprcvd.getAddress());
					Log.d("TerseLog", "Got i am proxy message from ip " + udprcvd.getAddress());
					udpsend = createUDP("MOBILE_REG_REQ_JOIN", udprcvd.getAddress(), JOIN_SENDPORT);
					Log.d("MPSG", "Sending join request");
					try {
						startProxyListener_socket.send(udpsend);
						Log.d("MPSG", "Join request sent");
						Log.d("TerseLog", "Join request sent");
					} catch (IOException e) {
						Log.d("MPSG", "Error in sending MOBILE_REG_REQ_JOIN");
						e.printStackTrace();
					}
					udpsend = null;
					udprcvd = null;
					noProxy = false;
					break;
				}
			}
			
			if (newSearch) {
				Log.d("MPSG", "New search process started, as network changed");
				Log.d("TerseLog", "New search process started, as network changed");
			//	if (sender != null) {
				//	sender.close();
				//}
				return;
			}
			
			// Unable to resolve proxy within subnet, giveup
			if (counter > 4) { 
				MPSG.proxyIp = null;
				gaveUp = true;
				return;
			}
			recvBuf = null;
		} while (loop);
		
		if (!noProxy) {
			// Got a potential proxy. Lookout for JOINOK from proxy
			startJoinOKListener();
		}
		
		//if (sender != null) {
			//Log.d("MPSG", "Socket closed: " + sender.toString());
			//sender.close();

		//}
		
		// Unable to resolve proxy within subnet
		return;
	}
	
	private void startJoinOKListener() {
		byte[] recvBuf;
		DatagramPacket udprcvd = null;
		try {
			startJoinOKListener_socket.setSoTimeout(2000); // Get the JOINOK response from the proxy within a second
		} catch (SocketException e) {
			Log.d("MPSG", "Error in creating socket to send packet");
			e.printStackTrace();
		}
		
		int counter = 0;
		while (true) {
			Log.d("MPSG", "Waiting for JoinOK");
			Log.d("TerseLog", "Waiting for JoinOK");
			recvBuf = new byte[15000];
			udprcvd = new DatagramPacket(recvBuf, recvBuf.length);
			if (udprcvd != null) {
				try {
					startJoinOKListener_socket.receive(udprcvd);
					MPSG.discoveryUsage += (udprcvd.getLength() + 42); // Add received length to data usage
					//Log.d("EXPERIMENTAL_RESULTS", "Received UDP data length (1) : " + udprcvd.getData()+42);
					Log.d("EXPERIMENTAL_RESULTS", "Received UDP data length (2) : " + (udprcvd.getLength() + 42));
				} catch (IOException e) {
					Log.d("MPSG", "Error in receiving UDP packet");
					e.printStackTrace();
					break;
				}
				String message = new String(udprcvd.getData()).trim();
				if (message.equalsIgnoreCase("MOBILE_REG_RESP_JOINOK")) {
					// Resolved proxy within subnet, return
					noProxy = false;
					proxyIP = udprcvd.getAddress();
					Log.d("MPSG", "Got proxy at ip " + proxyIP);
					Log.d("TerseLog", "Got proxy at ip " + proxyIP);
					MPSG.proxyIp = proxyIP;
					return;
				}
			}
			
			if (newSearch) {
				Log.d("MPSG", "New search process started, as network changed");
				Log.d("TerseLog", "New search process started, as network changed");
				return;
			}
			// Unable to resolve proxy within subnet, giveup
			if (counter > 3) { 
				MPSG.proxyIp = null;
				gaveUp = true;
				return;
			}
			recvBuf = null;
		}
		return;
	}
}

