package mobile_psg.mpsgStarter;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mobile_psg.networkmanagement.NetworkManager;
import mobile_psg.proxysearch.DNSProxySearch;
import mobile_psg.proxysearch.SearchSubnet_PSG;
import mobile_psg.tcpsession.TCP_Session_Handler;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Sathiya
 * Class MPSG handles searching of proxy and initiation of connection with Proxy
 */

public class MPSG {
	
	public static boolean inWifi = true;
	public static long lastHandshake = 0;
	public static String statusString = "Connecting"; 
	public static String leaveStatusString = "Disconnecting";
	public static boolean ongoingSession = false; // Change it when a session is ongoing
	public static TCP_Session_Handler conn;
	public static BufferedReader datain;
	static int serverPort = 5000;  // Proxy listens to connections at this port
	static Context basecontext;
	public static boolean sessionStatusFlag = false; // Set to TRUE when there is a ongoing communication with current proxy 

	// Start searching for proxy within subnet
	final static SearchSubnet_PSG search_first_try = new SearchSubnet_PSG();
	static int next = -1;

	// Context information during registration of MPSG
	public static String mpsgName = "MPSGSathiya1";
	public static String StaticContextData = "person.name::testmpsgname1,person.preference::pc,person.location::ion,person.isBusy::yes,person.speed::nil,person.action::eating,person.power::low,person.mood::happy,person.acceleration::nil,person.gravity::nil,person.magnetism::nil";
	public static String ContextType = "PERSON";
	public static HashMap DynamicContextData = new HashMap(); // All updates to sensor information are pushed into this 
	
	// Set by discovery mechanism
	//public static InetAddress[] iplist = new InetAddress[10]; // List of IP returned by DNS
	public static InetAddress[] iplist = new InetAddress[10];
	public static int dnsResultCount = 0;
	public static InetAddress proxyIp = null;
	public static InetAddress prevProxyIP = null; 
	public static boolean subnetSearchStatus = false;
	public static boolean dnsSearchStatus = false;
	public static int discoveryUsage = 0;
	
	static int proxyIndex = 0;
	public static String state = "init";
	public static long queryStart;
	public static int queryData;
	
	// Temporary query string to be sent to the proxy
	String queryString = mpsgName + ";query:select person.preference from person where person.name = \"testmpsgname1\"";
	//String queryString = mpsgName + ";query:select person.magnetism from person where person.acceleration = \"fast\" and person.gravity=\"medium\"";
//	String queryString = mpsgName + ";query:select person.location,person.magnetism from person where person.acceleration = \"fast\" and person.name = \"testmpsgname2\"";// ) or ( person.acceleration = \"fast\" and person.magnetism = \"positive\" )";
	
	MPSG(Context context, int port) {
		serverPort = port;
		basecontext = context;
		conn = new TCP_Session_Handler();
		// Keep searching for proxy whenever there is a network change detected in the mobile
		Log.d("MPSG", "Starting a service for monitoring network changes");
		Intent networkManager = new Intent(basecontext, NetworkManager.class);
		basecontext.startService(networkManager);

		// Temporarily assign ip of proxy for testing
		/*try {
			proxyIp = InetAddress.getByName("192.168.43.244");
		} catch (Exception e) {}*/
	} 
	
	/**
	 * Perform DNS Proxy search and selects the proxy 
	 * Starts a new thread to monitor connection status with proxy
	 * @return void
	 */
	public static void searchProxy() {
		
		// Reset all values before starting search
		proxyIp = null;
		subnetSearchStatus = false;
		proxyIndex = 0;
		statusString = "Connecting"; 
		leaveStatusString = "Disconnecting";
		//ongoingSession = false; // Change it when a session is ongoing
		conn = null;
		datain = null;
		discoveryUsage = 0; // reset discovery data usage to zero
		//sessionStatusFlag = false; // Set to TRUE when there is a ongoing communication with current proxy 
		
		// Time parameters
		long dnsStart = 0;
		long subnetStart = 0;
		long dnsSearchTime = 0;
		long subnetSearchTime = 0;
		long totalSearchTime = 0;
		
		boolean proxySelectedUsing = true; // true if selected using UDP subnet search
		Log.d("MPSG", "Starting proxy search");
    	
    	Log.d("MPSG", "Starting Subnet search");
    	Thread subnetSearch = new Thread() {
    		public void run() {
    			search_first_try.init();
    		}
    	};
    	subnetStart = System.currentTimeMillis();
    	subnetSearch.start();	
    	while (!subnetSearchStatus) {
    		if (!inWifi) {
    			inWifi = true;
    			return;
    		}
    	} // Wait until subnet search completes
    	inWifi = true;
     	Log.d("MPSG", "Subnet search done. Checking if proxyIP got set");
    	if (proxyIp == null) {
    		proxySelectedUsing = false; // Change selection through DNS
    		// Subnet search failed. Trigger DNS search 
    		Log.d("MPSG", "No proxy within subnet. Starting DNS search");
    		Thread dnsSearch = new Thread() {
    			public void run() {
    				DNSProxySearch.search("coalition.yjwong.name");
    			}
    		};
    	   	dnsStart = System.currentTimeMillis();
    		dnsSearch.start();
    		while (!dnsSearchStatus) {} // Wait until DNS search completes
    		//TODO: Hack for changing selected Proxy
    		next++;
    		if (next >= dnsResultCount) { next = 0; }
    		Log.d("MPSG", "Value of next:" + next + ", iplist length: " + iplist.length);
    		Log.d("MPSG", "Selected proxy from DNS: " + iplist[next]);
    		Log.d("EXPERIMENTAL_RESULTS", "Selected proxy from DNS: " + iplist[next]);
    		proxyIp = iplist[next];
    		if (proxyIp == null) {
    			statusString = "Failed in discovering proxy";
    		}
    	   	dnsSearchTime = Math.abs(System.currentTimeMillis() - dnsStart);
    	   	totalSearchTime = Math.abs(System.currentTimeMillis() - subnetStart);
    		
    	} else {
    		Log.d("MPSG", "Selected proxy from subnet: " + proxyIp);
    		Log.d("EXPERIMENTAL_RESULTS", "Selected proxy from subnet: " + proxyIp);
    	   	subnetSearchTime = Math.abs(System.currentTimeMillis() - subnetStart);
    	}
    	
    	if (proxySelectedUsing) {
    		Log.d("MPSG", "Discovery time using UDP subnet search : " +  subnetSearchTime);
    		Log.d("EXPERIMENTAL_RESULTS", "Discovery time subnet search : " +  subnetSearchTime);
    	} else {
    		Log.d("MPSG", "Discovery time from DNS search: " +  dnsSearchTime);
    		Log.d("MPSG", "Total search time: " +  totalSearchTime);
    		Log.d("EXPERIMENTAL_RESULTS", "Discovery time from DNS search: " +  dnsSearchTime);
    		Log.d("EXPERIMENTAL_RESULTS", "Total search time: " +  totalSearchTime);
    	}
    	
    	if (prevProxyIP != null) {
    		Log.d("MPSG", "Have a previous proxy");
	    	if (prevProxyIP.getHostAddress().equals(proxyIp.getHostAddress())) {
	    		Log.d("MPSG", "New proxy and old proxy same. Reconnect");
	    	}
	    	
	    	else {
		    	// Start a new thread & communicate with old proxy to perform graceful connection close
		    	Thread closeOld = new Thread() {
		    		public void run() {
		    			long closeOldStart = System.currentTimeMillis();
		    			TCP_Session_Handler oldconn = new TCP_Session_Handler();
		    			oldconn.closeSessionWithOldProxy(mpsgName, proxyIp, prevProxyIP);
		    			long closeOldEnd = System.currentTimeMillis();
		    			Log.d("EXPERIMENTAL_RESULTS", "Time for closing session with old proxy: " + Math.abs(closeOldEnd - closeOldStart));
		    		}
		    	};
		    	closeOld.start();
	    	}
    	}
    	
    	// Set prevProxy as this proxy & connect to new proxy
    	prevProxyIP = proxyIp;
    	Thread startConnection = new Thread() {
    		public void run() {
    			connect();		
    		}
    	};
    	startConnection.start();
    	
		return;
	}
	
	/**
	 * Initiate a connection with the Proxy and return the status
	 * @return boolean 
	 */
	public static void connect() {
		long registerStartWithProxy = System.currentTimeMillis();
		long registerEndWithProxy = 0;
		conn = null;
		conn = new TCP_Session_Handler();
		/*try {
		proxyIp = InetAddress.getByName("192.168.43.244");
		} catch(Exception e) {}*/
		
		// Create socket connection to the proxy
        try {
            Log.d("MPSG", "serverAddr " + proxyIp + ", port: " + serverPort);
            conn.connectServer(proxyIp, serverPort);
        } catch(Exception e) {
        	Log.d("MPSG", "Unable to connect to server address " + proxyIp + ", error: " + e.toString());
        }
        
        // Register context data with proxy
        try {
        	Log.d("MPSG", "Registering with proxy for the context");
        	String res = conn.registerWithProxy(mpsgName, ContextType, StaticContextData); 
        	if (res.startsWith("MPSG Registration")) {
        		Log.d("MPSG", res);
        		statusString = "Connected";
        	} else {
        		Log.d("MPSG", res);
        		statusString = res + ", proxy: " + proxyIp;
        	}
        } catch (Exception e) {
        	Log.d("MPSG", "Error in receiving 'OK' response from proxy for registration request");
        	statusString = "Failed in registering with proxy " + proxyIp;
        }
        registerEndWithProxy = System.currentTimeMillis();
        Log.d("EXPERIMENTAL_RESULTS", "Time to Register:" + Math.abs(registerEndWithProxy - registerStartWithProxy));
	}
	
	/**
	 * Initiate a query request to the proxy
	 * 
	 */
	public void sendQuery(ArrayList<String> queryAttr) {


		// conn object will be set during connect call
		String qString = mpsgName + ";query:select ";
        //";query:select person.preference from person where person.name = \"testmpsgname1\"";

        for (String attr : queryAttr) {
            qString = qString + "person." + attr.toLowerCase() + ",";
        }

        qString = qString.substring(0, qString.length() - 1);

        qString += " from person where person.name = \"testmpsgname1\"";

		// Send the query through the socket connection with proxy
        Log.d("qs", qString);
		try {
			Log.d("MPSG", "Sending the query to the proxy");
			conn.sendQuery(qString);
		} catch (Exception e) {
			Log.d("MPSG", "Error in sending query to the proxy");
		}
	}
	
	/**
	 * Initiate a connection removal with the Proxy and return the status
	 * @return boolean 
	 */
	public static void disconnect() {
		long startLeave = System.currentTimeMillis();
        // De-Register context data with proxy
        try {
        	Log.d("MPSG", "De-Registering with coalition through proxy");
        	String res = conn.removeFromCoalition(); 
        	if (res.startsWith("leavesuccess")) {
        		Log.d("MPSG", res);
        		leaveStatusString = "Disconnected";
        	} else {
        		Log.d("MPSG", res);
        		leaveStatusString = res + ", proxy: " + proxyIp;
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        	Log.d("MPSG", "Error in de-registration request");
        	leaveStatusString = "Failed in de-registering with coalition, proxy:" + proxyIp;
        }
        long endLeave = System.currentTimeMillis();
        Log.d("EXPERIMENTAL_RESULTS", "Time to Deregister: "+Math.abs(endLeave - startLeave));
	}
	
	/**
	 * Starts connection status monitor of MPSG
	 *
	 */
	private void start() {
        try {
        	conn.enableKeepalive();
        	if (!conn.isAlive()) {
        		searchProxy();
        	}
        } catch (Exception e) {
        	Log.d("MPSG", "Unable to start Keepalive and status check");
        }
    }
}