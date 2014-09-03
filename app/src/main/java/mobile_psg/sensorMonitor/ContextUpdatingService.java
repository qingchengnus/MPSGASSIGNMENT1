package mobile_psg.sensorMonitor;

import mobile_psg.mpsgStarter.MPSG;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ContextUpdatingService extends IntentService implements SensorEventListener{
	// data
	private SensorManager mSensorManager;
	private Sensor gravitySensor;
	private Sensor lightSensor;
	private Sensor acceleroSensor;
	private Sensor magneticSensor;
	
	// methods
	public ContextUpdatingService() {
		super("ContextUpdatingService");
		// TODO Auto-generated constructor stub
	}

	// methods
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		synchronized(this) {
			// get the value string
			String valueString = "";
			float[] values = event.values;
			for(int i=0; i<values.length-1; i++) {
				valueString += (values[i] + "----");
			}
			valueString += values[values.length-1];
			
			// update corresponding context attribute
			int sensorType = event.sensor.getType();
			switch(sensorType) {
			case Sensor.TYPE_ACCELEROMETER:
				MPSG.DynamicContextData.put("person.acceleration", valueString);
				break;
			case Sensor.TYPE_GRAVITY:
				MPSG.DynamicContextData.put("person.gravity", valueString);
				break;
			case Sensor.TYPE_LIGHT:
				MPSG.DynamicContextData.put("person.light", valueString);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:	
				MPSG.DynamicContextData.put("person.magnetism", valueString);
				break;
			}
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		try {
			gravitySensor = mSensorManager.getSensorList(Sensor.TYPE_GRAVITY).get(0);
			mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			lightSensor = mSensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			acceleroSensor = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			mSensorManager.registerListener(this, acceleroSensor, SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			magneticSensor = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0);
			mSensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}