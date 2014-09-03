package mobile_psg.networkmanagement;

import java.net.Socket;

import mobile_psg.mpsgStarter.MPSG;
import mobile_psg.tcpsession.TCP_Session_Handler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkManager extends BroadcastReceiver {
	static int count = 0;
	static long lastChangeTime = 0;
	Socket socket = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (action.equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION))
	    {
	        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	        NetworkInfo.State state = networkInfo.getState();
	        if(state == NetworkInfo.State.CONNECTED)
	        {
	        	count++; // hack to counter two updates for one wifi network change, can be removed too if needed
				if (count == 2) {
					Log.d("MPSG", "Detected network change");
					ConnectivityManager cm = (ConnectivityManager) 
		        			context.getSystemService(Context.CONNECTIVITY_SERVICE);
		        	NetworkInfo netInfo = cm.getActiveNetworkInfo();

		        	// Check if current socket connection is lost
		        	socket = TCP_Session_Handler.socket;
		        	//if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
		        	if (MPSG.state.equals("disconnected")) {
		        		Log.d("MPSG", "Proxy connection lost");
		        		//if ((System.currentTimeMillis() - lastChangeTime)/1000 > 10) { 
			        		//lastChangeTime =  System.currentTimeMillis();
				        	// Start new search only if network is connected
				        	if (netInfo != null && netInfo.isConnected()) {
				        		// Wait for 5 seconds for letting WiFi to stabilise, before trying reconnect
				        		try { Thread.sleep(5000); } catch (Exception e) {}
				        		// Start searching for new proxy
				               	Thread reconnect = new Thread() {
				            		public void run() {
				            			long startReconnect = System.currentTimeMillis();
				            			MPSG.searchProxy();
				            			long endReconnect = System.currentTimeMillis();
				            			Log.d("EXPERIMENTAL_RESULTS", "Reconnect Time: " + Math.abs(endReconnect - startReconnect));
				            		}
				            	};
				            	reconnect.start();
				        	}
				            count = 0;
		        		//}
			        }
		            return;	
				}
	        }
	    }
	}	
}
