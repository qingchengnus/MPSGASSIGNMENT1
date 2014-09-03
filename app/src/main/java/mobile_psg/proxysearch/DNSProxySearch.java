package mobile_psg.proxysearch;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobile_psg.mpsgStarter.MPSG;

import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;

import android.util.Log;


public class DNSProxySearch {
	static String proxy = "coalition.yjwong.name";
	
	public static void search(String dnsquery) {
		int i = 0;
		InetAddress[] ip = new InetAddress[10];
		//List<List<InetAddress>> ip_list = new ArrayList<List<InetAddress>>();
		try {
			org.xbill.DNS.Name name = Name.fromString(dnsquery, Name.root);
			int type = 1;
			int class_id = 1;
			
			Record recordobj = Record.newRecord(name, type, class_id);
			
			String nameserver = "8.8.8.8";
			SimpleResolver resolver = new SimpleResolver(nameserver);
			Message reply = resolver.send(Message.newQuery(recordobj));
			Log.d("MPSG", reply.toString());
			String replyStr = reply.toString().substring(reply.toString().indexOf("ANSWERS:"), reply.toString().indexOf("AUTHORITY"));
			String IPADDRESS_PATTERN = 
			        "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

			Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
			Matcher matcher = pattern.matcher(replyStr);
			
	        while (matcher.find()) {
	            Log.d("MPSG", "Detected proxy: " + matcher.group());
//	        	List<InetAddress> thisIp = new ArrayList<InetAddress>();
//	        	thisIp = (List<InetAddress>) InetAddress.getByName(matcher.group());
//	            ip_list.add(thisIp);
	            ip[i] = InetAddress.getByName(matcher.group());
	            i++;
	        }
		}
		catch (Exception e) {
			Log.d("MPSG", "Error in SimpleResolver");
			e.printStackTrace();
		}
		
		MPSG.iplist = ip;
		MPSG.dnsResultCount = i;
		MPSG.dnsSearchStatus = true;
    }
}
