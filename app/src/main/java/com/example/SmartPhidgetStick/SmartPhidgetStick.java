// Search CSCM79 Advice for test modification
package com.example.SmartPhidgetStick;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.*;

import com.phidget22.*;

import java.text.DecimalFormat;
import java.util.Locale;

import static android.util.FloatMath.cos;
import static android.util.FloatMath.sin;
import static android.util.FloatMath.sqrt;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
public class SmartPhidgetStick extends Activity {

	VoltageRatioInput ch;
	VoltageRatioInput ch2;
	RCServo ch3;

	SensorManager sensorManager;
	Sensor Gyrosensor;;

	public static float EPSILON;
	boolean isHubPort;
	TextToSpeech tts;
	String text;
	Toast errToast;
	public Vibrator v;
	float a = 0.1f;
	float mLowPassX ;
	float mLowPassY ;
	float mLowPassZ ;

	int servoValue = 0; // for servo angle
	int readingsCount = 0; // for the number of readings
	double distanceReading = 0.0f;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Gyrosensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);


		try
		{
			ch = new VoltageRatioInput();
			ch2 = new VoltageRatioInput();
			ch3 = new RCServo();

			//Allow direct USB connection of Phidgets
			if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
				com.phidget22.usb.Manager.Initialize(this);

			//Enable server discovery to list remote Phidgets
			this.getSystemService(Context.NSD_SERVICE);
			Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

			//CSCM79 Advice
			//Add a specific network server to communicate with Phidgets remotely
			Net.addServer("", "10.65.2.244", 5661, "", 0);

			//CSCM79 Advice
			//Set addressing parameters to specify which channel to open (if any)
			ch.setIsRemote(true);
			ch.setChannel(3);
			ch.setDeviceSerialNumber(39830);

			ch2.setIsRemote(true);
			ch2.setDeviceSerialNumber(39830);
			ch2.setChannel(4);

			ch3.setIsRemote(true);
			ch3.setDeviceSerialNumber(19875);
			ch3.setChannel(0);

			//Remember isHubPort setting
			if (savedInstanceState != null) {
				isHubPort = savedInstanceState.getBoolean("isHubPort");
			} else {
				isHubPort = false;
			}

			ch.setIsHubPortDevice(isHubPort);

			ch.addAttachListener(new AttachListener() {
				public void onAttach(final AttachEvent attachEvent) {
					AttachEventHandler handler = new AttachEventHandler(ch);
					runOnUiThread(handler);
				}
			});

			ch.addDetachListener(new DetachListener() {
				public void onDetach(final DetachEvent detachEvent) {
					DetachEventHandler handler = new DetachEventHandler(ch);
					runOnUiThread(handler);

				}
			});

			ch.addErrorListener(new ErrorListener() {
				public void onError(final ErrorEvent errorEvent) {
					ErrorEventHandler handler = new ErrorEventHandler(ch, errorEvent);
					runOnUiThread(handler);

				}
			});

			ch.addVoltageRatioChangeListener(new VoltageRatioInputVoltageRatioChangeListener() {
				public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent) {
					double pressureReading = voltageRatioChangeEvent.getVoltageRatio();
				//System.out.println("Pressure reading : " + pressureReading);
					getDistanceSensor();
					if(pressureReading > 0.7 ) {

						System.out.println("readingCount: "+ readingsCount++ + " Distance : " + distanceReading + " servo : " + servoValue);

						while( distanceReading < 0.15 && readingsCount < 5 ) {
							StopObstacle();
							v.vibrate(50);
							int currentServoValue = servoValue != 180 ? servoValue + 45 : 0;
							setServoMotor(currentServoValue);
							servoValue = currentServoValue;
							getDistanceSensor();
							System.out.println("readingCount: "+ readingsCount++ + " Distance : " + distanceReading + " servo : " + servoValue);
							try {
								sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						}
					}

					if(readingsCount > 5 ) {
						try {
							readingsCount = 0;
							distanceReading = 0.0f;
							servoValue = 90;
							ch3.setTargetPosition(0);
							ch3.setEngaged(true);

							System.out.println("-");
							System.out.println("-");
							System.out.println("-");
							System.out.println("-");
							ch2.close();
							ch3.close();
						} catch (PhidgetException pe) {
							pe.printStackTrace();
						}
					}
				}
			});

			ch.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
				public void onSensorChange(VoltageRatioInputSensorChangeEvent sensorChangeEvent) {
					VoltageRatioInputSensorChangeEventHandler handler = new VoltageRatioInputSensorChangeEventHandler(ch, sensorChangeEvent);
					runOnUiThread(handler);
				}
			});

			ch.open();


		} catch (PhidgetException pe) {
			pe.printStackTrace();
		}
		tts=new TextToSpeech(SmartPhidgetStick.this, new TextToSpeech.OnInitListener() {

			@Override
			public void onInit(int status) {
				// TODO Auto-generated method stub
				if(status == TextToSpeech.SUCCESS){
					StartWalk();
					int result=tts.setLanguage(Locale.US);
					if(result==TextToSpeech.LANG_MISSING_DATA ||
							result==TextToSpeech.LANG_NOT_SUPPORTED){
						android.util.Log.e("error", "This Language is not supported");
					}
				}
				else
					Log.e("error", "Initilization Failed!");
			}
		});
	}

	class AttachEventHandler implements Runnable {
		Phidget ch;

		public AttachEventHandler(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			System.out.println("Attached");
		}
	}
	class DetachEventHandler implements Runnable {
		Phidget ch;

		public DetachEventHandler(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			System.out.println("Detached");
		}
	}

	class ErrorEventHandler implements Runnable {
		Phidget ch;
		ErrorEvent errorEvent;

		public ErrorEventHandler(Phidget ch, ErrorEvent errorEvent) {
			this.ch = ch;
			this.errorEvent = errorEvent;
		}

		public void run() {
			if (errToast == null)
				errToast = Toast.makeText(getApplicationContext(), errorEvent.getDescription(), Toast.LENGTH_SHORT);

			//replace the previous toast message if a new error occurs
			errToast.setText(errorEvent.getDescription());
			errToast.show();
		}
	}

	class VoltageRatioInputSensorChangeEventHandler implements Runnable {
		Phidget ch;
		VoltageRatioInputSensorChangeEvent sensorChangeEvent;

		public VoltageRatioInputSensorChangeEventHandler(Phidget ch, VoltageRatioInputSensorChangeEvent sensorChangeEvent) {
			this.ch = ch;
			this.sensorChangeEvent = sensorChangeEvent;
		}

		public void run() {
			System.out.println(sensorChangeEvent.getSensorValue());
		}
	}

	class RCServoPositionChangeEventHandler implements Runnable {
		Phidget ch;
		RCServoPositionChangeEvent positionChangeEvent;

		public RCServoPositionChangeEventHandler(Phidget ch, RCServoPositionChangeEvent positionChangeEvent) {
			this.ch = ch;
			this.positionChangeEvent = positionChangeEvent;
		}

		public void run() {
			DecimalFormat numberFormat = new DecimalFormat("#.##");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		savedInstanceState.putBoolean("isHubPort", isHubPort);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {

			ch2.close();
			ch3.close();

		} catch (PhidgetException e) {
			e.printStackTrace();
		}

		//Disable USB connection to Phidgets
		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
			com.phidget22.usb.Manager.Uninitialize();
	}

	public void getDistanceSensor ( ) {

		try {
			ch2.open();
		} catch (PhidgetException e) {
			e.printStackTrace();
		}
		ch2.addAttachListener(new AttachListener() {
			public void onAttach(final AttachEvent attachEvent) {
				AttachEventHandler handler = new AttachEventHandler(ch2);
				runOnUiThread(handler);
			}
		});

		ch2.addDetachListener(new DetachListener() {
			public void onDetach(final DetachEvent detachEvent) {
				DetachEventHandler handler = new DetachEventHandler(ch2);
				runOnUiThread(handler);

			}
		});

		ch2.addErrorListener(new ErrorListener() {
			public void onError(final ErrorEvent errorEvent) {
				ErrorEventHandler handler = new ErrorEventHandler(ch2, errorEvent);
				runOnUiThread(handler);

			}
		});

		ch2.addVoltageRatioChangeListener(new VoltageRatioInputVoltageRatioChangeListener() {
			public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent) {
				distanceReading = voltageRatioChangeEvent.getVoltageRatio();

			}
		});

		ch2.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
			public void onSensorChange(VoltageRatioInputSensorChangeEvent sensorChangeEvent) {
				VoltageRatioInputSensorChangeEventHandler handler = new VoltageRatioInputSensorChangeEventHandler(ch2, sensorChangeEvent);
				runOnUiThread(handler);
			}
		});


	}

	public void setServoMotor (int value) {

			if(value > 45){
				v.vibrate(50);
				StopObstacle();}

			try{
				ch3.open(500);
				//set position
				ch3.setTargetPosition(value);
				ch3.setEngaged(true);

				sleep(2000);

				//  0 - 90, 1 - 45,  2 - 0, 3 - 135, 4 - 180
				System.out.println("current servo motor positon " + servoValue + " thread: " + currentThread().getId());
				ch3.setEngaged(true);
				ch3.addAttachListener(new AttachListener() {
					public void onAttach(final AttachEvent attachEvent) {
						AttachEventHandler handler = new AttachEventHandler(ch3);
						runOnUiThread(handler);
					}
				});

				ch3.addDetachListener(new DetachListener() {
					public void onDetach(final DetachEvent detachEvent) {
						DetachEventHandler handler = new DetachEventHandler(ch3);
						runOnUiThread(handler);

					}
				});

				ch3.addErrorListener(new ErrorListener() {
					public void onError(final ErrorEvent errorEvent) {
						ErrorEventHandler handler = new ErrorEventHandler(ch3, errorEvent);
						runOnUiThread(handler);

					}
				});

				ch3.addPositionChangeListener(new RCServoPositionChangeListener() {
					public void onPositionChange(RCServoPositionChangeEvent positionChangeEvent) {
						RCServoPositionChangeEventHandler handler = new RCServoPositionChangeEventHandler(ch3, positionChangeEvent);
					}
				});


				if( distanceReading > 0.1 || readingsCount > 4) {

					System.out.println("process stops at distance: " + distanceReading + " reading count : " + readingsCount);
					Walk();
					readingsCount = 0;
					distanceReading = 0.0f;
					servoValue = 90;
					ch3.setTargetPosition(0);
					ch3.setEngaged(true);
					System.out.println("-");
					System.out.println("-");
					System.out.println("-");
					System.out.println("-");
					ch2.close();
					ch3.close();

				}

			} catch (PhidgetException pe) {
				pe.printStackTrace();
			} catch (InterruptedException interruptedException) {
				interruptedException.printStackTrace();
			}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(tts != null){
			tts.stop();
			tts.shutdown();
		}
		super.onPause();
	} //voice commands

	private void StopObstacle() {
		// TODO Auto-generated method stub
		text = "Please stop there is an obstecale this side";
		tts.setLanguage(Locale.US);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	private void StartWalk() {
		// TODO Auto-generated method stub
		text = "Start Walking";
		tts.setLanguage(Locale.US);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	private void Walk() {
		// TODO Auto-generated method stub
		text = "Continue walking there is no obstacles";
		tts.setLanguage(Locale.US);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}


	public void onResume() {
		super.onResume();
		sensorManager.registerListener(SensorListener, Gyrosensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void onStop() {
		super.onStop();
		sensorManager.unregisterListener(SensorListener);
	}

	// Create a constant to convert nanoseconds to seconds.
	private static final float NS2S = 1.0f / 1000000000.0f;
	private final float[] deltaRotationVector = new float[4];
	private float timestamp;

	public SensorEventListener SensorListener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor sensor, int acc) {
		}

		@SuppressLint("LongLogTag")
		public void onSensorChanged(SensorEvent event) {

			float axisX= 0.0f;
			float axisY= 0.0f;
			float axisZ= 0.0f;
			float dT =0.0f;
			// This timestep's delta rotation to be multiplied by the current rotation
			// after computing it from the gyro sample data.
				if (timestamp != 0) {
					dT = (event.timestamp - timestamp) * NS2S;
					axisX = event.values[0];
					axisY = event.values[1];
					axisZ = event.values[2];
					// Calculate the angular speed of the sample
					float omegaMagnitude = sqrt(axisX *axisX + axisY* axisY + axisZ * axisZ);
					// Normalize the rotation vector if it's big enough to get the axis
					// (that is, EPSILON should represent your maximum allowable margin of error)
					if (omegaMagnitude > EPSILON) {
						axisX /= omegaMagnitude;
						axisY /= omegaMagnitude;
						axisZ /= omegaMagnitude;
					}
					// Integrate around this axis with the angular speed by the timeste in order to get a delta rotation from this sample over the timestep
					// We will convert this axis-angle representation of the delta rotation into a quaternion before turning it into the rotation matrix.
					float thetaOverTwo = omegaMagnitude * dT / 2.0f;
					float sinThetaOverTwo = sin(thetaOverTwo);
					float cosThetaOverTwo = cos(thetaOverTwo);
					deltaRotationVector[0] = cosThetaOverTwo;
					deltaRotationVector[1] = sinThetaOverTwo * axisX;
					deltaRotationVector[2] = sinThetaOverTwo * axisY;
					deltaRotationVector[3] = sinThetaOverTwo * axisZ;
				}
				timestamp = event.timestamp;
				float[] deltaRotationMatrix = new float[9];
				SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
//				User code should concatenate the delta rotation we computed with the current rotation in order to get the updated rotation
				float	X= (float) ((deltaRotationMatrix[1]+axisX*dT)*180/Math.PI);
				float	Y= (float) ((deltaRotationMatrix[2]+axisY*dT)*180/Math.PI);
				float	Z= (float) ((deltaRotationMatrix[3]+axisZ*dT)*180/Math.PI);
				float Angle =(float) ((deltaRotationMatrix[0])*180/Math.PI);
				mLowPassX = lowpass(X, mLowPassX);
				mLowPassY = lowpass(Y, mLowPassY);
				mLowPassZ = lowpass(Z, mLowPassZ);

				Log.i("Sensor Orientation GyroScope", "X: " + (int)(mLowPassX)  + //
					" Y: " + (int) (mLowPassY)+ //
					" Z: " + (int)(mLowPassZ) +" Angle: "+ (int) Angle);

					setServoMotor((int)Angle);
			}
	};
//	 filter function for the orientation readings in gyroscope
	float lowpass(float current ,float last ){

	return last * (1.0f - a) + current*a;
	}

}

