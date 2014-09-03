package mobile_psg.tcpsession;

import mobile_psg.mpsgStarter.MPSG;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.mobile_psg.R;

public class DataComm extends Activity {

	private Button sendStaticInfo;
	private Button sendUpdateInfo;
	private TCP_Session_Handler conn = null;
	private static TextView status;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MPSG", "Created new intent Datacomm");
		setContentView(R.layout.activity_data_comm);
		sendStaticInfo = (Button) findViewById(R.id.button1);
        sendStaticInfo.setOnClickListener(staticSender);
        
        sendUpdateInfo = (Button) findViewById(R.id.button2);
        sendUpdateInfo.setOnClickListener(updateSender);
        
        conn = MPSG.conn;
	}

	 private OnClickListener staticSender = new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	Log.d("MPSG", "Invoking static info send..");
	        	//conn.send("Static");
	        }
	 };

	 private OnClickListener updateSender = new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	Log.d("MPSG", "Invoking update info send..");
	        	//conn.send("Update");
	        }
	 };
	 
	 public static void sendResult(int result) {
    	Log.d("MPSG", "Got reply from update send..");
    	if (result == 1) {
    		Log.d("MPSG", "Reply NOK from update send");
    		status.setText("Send Not Ok. Please Resend!");
    	} else if (result == 0) {
    		Log.d("MPSG", "Reply OK from update send");
    		status.setText("File sent!");
    	}
	}	
}
