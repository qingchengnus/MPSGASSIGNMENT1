package mobile_psg.mpsgStarter;

import java.util.ArrayList;

import mobile_psg.sensorMonitor.ContextUpdatingService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import com.example.mobile_psg.R;

public class MpsgStarter extends Activity {
    private int[] selectedAttributes;
    public static MPSG mpsg = null;
    private Button connect;
    private Button query;
    private Button leave;
    private Button navigationButton;
    private Boolean isUserAtConditionPage;
    private static TextView errorStr;

    private int timeout = 10; //10 seconds timeout for connecting
    private static final int SERVERPORT = 5000;

    private Context myContext = this;
    private static Handler mHandler;
    private static String resultStr = "";
    private static String connStatus = "Start MPSG";
    private static String resultString = "";
    private static String queryStatus = "invisible";
    private ListView attributesListView;
    private ScrollView conditionsScrollView;
    private ArrayList<String> attributes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpsg_starter);

        errorStr = (TextView) findViewById(R.id.textView1);
        errorStr.setText("");
        errorStr.setVisibility(View.VISIBLE);


        connect = (Button) findViewById(R.id.submit);
        connect.setOnClickListener(connectListener);

        query = (Button) findViewById(R.id.query);
        query.setOnClickListener(querySendListener);
        query.setVisibility(View.INVISIBLE);

        leave = (Button) findViewById(R.id.leave);
        leave.setOnClickListener(leaveSendListener);
        leave.setVisibility(View.INVISIBLE);

        navigationButton = (Button) findViewById(R.id.navigation_button);
        navigationButton.setOnClickListener(navigationButtonListener);
        isUserAtConditionPage = false;
        navigationButton.setVisibility(View.INVISIBLE);

        mHandler = new Handler();
        mHandler.post(updateText);

        if (savedInstanceState != null) {
            Log.d("MPSG", "Saved state not null");
            String myResultString = savedInstanceState.getString("resultString");
            Log.d("MPSG", "resultstring="+myResultString);
            if (!myResultString.contentEquals("")) {
                errorStr.setText(myResultString);
            }

            String myConnectStatus = savedInstanceState.getString("connStatus");
            Log.d("MPSG", "connstatus="+myConnectStatus);
            if (myConnectStatus.contentEquals("invisible")) {
                connect.setVisibility(View.INVISIBLE);
            }

            String myQueryStatus = savedInstanceState.getString("queryStatus");
            Log.d("MPSG", "querystatus="+myQueryStatus);
            if (myQueryStatus.contentEquals("visible")) {
                query.setVisibility(View.VISIBLE);
                leave.setVisibility(View.VISIBLE);
            }
        }
        attributes = new ArrayList<String>();
        attributes.add("Name");
        attributes.add("Preference");
        attributes.add("Location");
        attributes.add("IsBusy");
        attributes.add("Speed");
        attributes.add("Action");
        attributes.add("Power");
        attributes.add("Mood");
        attributes.add("Acceleration");
        attributes.add("Gravity");
        attributes.add("Magnetism");

        selectedAttributes = new int[attributes.size()];
        ArrayAdapter<String> arrayAdapter = new ContextAttributeAdapter(MpsgStarter.this, R.layout.list_item_layout, attributes);
        attributesListView = (ListView) findViewById(R.id.attribute_list);
        attributesListView.setAdapter(arrayAdapter);
        attributesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedAttributes[i] = 1 - selectedAttributes[i];
                view.setSelected(true);
                Log.d("Log", "Item clicked!");
                Toast.makeText(MpsgStarter.this, "Item: " + i + " selected.", Toast.LENGTH_LONG).show();
            }
        });

        conditionsScrollView = (ScrollView) findViewById(R.id.conditions_view);
        conditionsScrollView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d("MPSG", "Saving state");
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString("connStatus", connStatus);
        savedInstanceState.putString("queryStatus", queryStatus);
        savedInstanceState.putString("resultString", resultString);
    }

    private OnClickListener connectListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
//        	mProgress.setVisibility(View.VISIBLE);
            mpsg = new MPSG(getBaseContext(), SERVERPORT);

            long registerStartTime = System.currentTimeMillis();
            long registerEndTime = 0;

            // Search for a proxy and connect to the best proxy
            MPSG.searchProxy(); // Commented temporarily to test the MPSG-proxy-coalition flow
        	
        	/*// Connect to the selected proxy
        	Thread mpsgconnect = new Thread() {
        		public void run() {
        			mpsg.connect();
        		}
        	};
        	mpsgconnect.start();*/

            int i = 0;
            // Wait for a result in registering through proxy
            while (MPSG.statusString.contentEquals("Connecting")) {
                if (i > timeout) {
                    errorStr.setText("Error in waiting for result in registration with proxy");
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    errorStr.setText("Error in waiting for result in registration with proxy");
                }
            }

            if (MPSG.statusString.contentEquals("Connected")) {

//                android.content.Intent intent = new android.content.Intent(MpsgStarter.this, SelectAttributesActivity.class);
//                startActivity(intent);

                registerEndTime = System.currentTimeMillis();
                // Start the Service which updates the context information for the MPSG
                Intent contextUpdater = new Intent(myContext, ContextUpdatingService.class);
                startService(contextUpdater);

                connStatus = "invisible";
                connect.setText(connStatus);
                connect.setVisibility(View.INVISIBLE);
                queryStatus = "visible";
                query.setVisibility(View.VISIBLE);
                leave.setVisibility(View.VISIBLE);
                attributesListView.setVisibility(View.VISIBLE);
                navigationButton.setVisibility(View.VISIBLE);

            } else {
                registerEndTime = System.currentTimeMillis();
                errorStr.setText("FAILED: "+ MPSG.statusString);
                // TODO: Code for starting MPSG old directly without proxy
            }
            Log.d("EXPERIMENTAL_RESULTS", "Total response time for registration: " + Math.abs(registerEndTime - registerStartTime));
        }
    };

    private OnClickListener navigationButtonListener = new OnClickListener() {
        @Override
        public void onClick(View view) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isUserAtConditionPage){
                        updateViewOnConditionPage();
                    } else {
                        updateViewOnAttributePage();
                    }
                }
            });

            isUserAtConditionPage = !isUserAtConditionPage;
        }
    };

    private OnClickListener querySendListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // Start a new thread to send out the query
            Thread queryThread = new Thread() {
                public void run() {
                    mpsg.sendQuery(generateQueryString());

                }
            };

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    conditionsScrollView.setVisibility(View.INVISIBLE);
                }
            });
            queryThread.start();
        }
    };

    private String generateQueryString(){

        ArrayList<String> selectedAttr = new ArrayList<String>();
        for (int i = 0; i < selectedAttributes.length; i ++) {
            if (selectedAttributes[i] == 1) {
                selectedAttr.add(attributes.get(i));
            }
        }

        // conn object will be set during connect call
        String qString = ";query:select ";
        //";query:select person.preference from person where person.name = \"testmpsgname1\"";

        for (String attr : selectedAttr) {
            qString = qString + "person." + attr.toLowerCase() + ",";
        }

        qString = qString.substring(0, qString.length() - 1);

        qString += " from person where ";

        ArrayList<String> ids = new ArrayList<String>();

        ids.add("name_field");
        ids.add("preference_field");
        ids.add("Location_field");
        ids.add("is_busy_field");
        ids.add("speed_field");
        ids.add("action_field");
        ids.add("power_field");
        ids.add("mood_field");
        ids.add("acceleration_field");
        ids.add("Gravity_field");

        for(int i = 0; i < ids.size(); i++){
            int resID = getResources().getIdentifier(ids.get(i), "id", getPackageName());
            EditText textField = (EditText)findViewById(resID);

            if (textField.getText().length() > 0){
                qString += "person." + attributes.get(i).toLowerCase() + " = \"" + textField.getText() + "\" and ";
            }
        }

        qString = qString.substring(0, qString.length() - 4);

        // Send the query through the socket connection with proxy
        Log.d("qs", qString);

        return qString;
    }

    private void updateViewOnConditionPage(){
        Button navigationButton = (Button)findViewById(R.id.navigation_button);
        navigationButton.setText("Back");
        findViewById(R.id.attribute_list).setVisibility(View.INVISIBLE);
        findViewById(R.id.conditions_view).setVisibility(View.VISIBLE);
    }

    private void updateViewOnAttributePage(){
        Button navigationButton = (Button)findViewById(R.id.navigation_button);
        navigationButton.setText("Next");
        findViewById(R.id.attribute_list).setVisibility(View.VISIBLE);
        findViewById(R.id.conditions_view).setVisibility(View.INVISIBLE);
    }

    public static void setQueryResult (String result) {
        Log.d("MPSG", "Setting query result to " + result);
        resultStr = result;
        Log.d("EXPERIMENTAL_RESULTS", "Time for getting query response:" + Math.abs(System.currentTimeMillis() - MPSG.queryStart));
    }

    private Runnable updateText = new Runnable() {
        public void run() {
            resultString = errorStr.getText() + resultStr;
            errorStr.setText(errorStr.getText() + resultStr);
            resultStr = "";
            mHandler.postDelayed(this, 1000);
        }
    };

    private OnClickListener leaveSendListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // Start a new thread to send out the query
            Thread leaveThread = new Thread() {
                public void run() {
                    mpsg.disconnect();
                }
            };
            leaveThread.start();
            int i = 0;
            // Wait for a result in registering through proxy
            while (MPSG.leaveStatusString.contentEquals("Disconnecting")) {
                if (i > timeout) {
                    errorStr.setText("Error in waiting for result in de-registration with coalition");
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    errorStr.setText("Error in waiting for result in de-registration with coalition");
                }
            }
            if (MPSG.leaveStatusString.contentEquals("Disconnected")) {
                connStatus = "visible";
                connect.setText("Start Mobile PSG");
                connect.setVisibility(View.VISIBLE);
                queryStatus = "invisible";
                query.setVisibility(View.INVISIBLE);
                leave.setVisibility(View.INVISIBLE);
                resultString = "";

                attributesListView.setVisibility(View.GONE);
                conditionsScrollView.setVisibility(View.GONE);
                navigationButton.setVisibility(View.INVISIBLE);


                // Do cleanup of data structures
                MPSG.conn = null;
                MPSG.datain = null;
                MPSG.dnsSearchStatus = false;
                MPSG.ongoingSession = false;
                MPSG.sessionStatusFlag = false;
                MPSG.DynamicContextData = null;
                MPSG.iplist = null;
                MPSG.proxyIp = null;
                MPSG.prevProxyIP = null;
                MPSG.subnetSearchStatus = false;
            }
        }
    };

    private class ContextAttributeAdapter extends ArrayAdapter<String> {
        private int resource;
        public ContextAttributeAdapter(Context context, int resourceId, ArrayList<String> attributes) {
            super(context, resourceId, attributes);
            resource = resourceId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {


            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);
            }

            TextView attributeName = (TextView) convertView.findViewById(R.id.attribute_name);
            attributeName.setText(getItem(position));

            return convertView;
        }
    }
}
