// Search CSCM79 Advice for test modification
package com.example.SmartPhidgetStick;

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

import com.phidget22.*;

import java.text.DecimalFormat;
import java.util.Locale;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
public class SmartPhidgetStick extends Activity {

	VoltageRatioInput ch;
	VoltageRatioInput ch2;
	RCServo ch3;

	SensorManager sensorManager;
	Sensor Gyrosensor;
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
			Net.addServer("", "192.168.0.210", 5661, "", 0);

			//CSCM79 Advice
			//Set addressing parameters to specify which channel to open (if any)
			ch.setIsRemote(true);
			ch.setChannel(0);
			ch.setDeviceSerialNumber(39830);

			ch2.setIsRemote(true);
			ch2.setDeviceSerialNumber(39830);
			ch2.setChannel(3);

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
//					System.out.println("-");
//					System.out.println("-");
					System.out.println("Pressure reading : " + pressureReading);

					if(pressureReading > 0.7 && false) {

						System.out.println("readingCount: "+ readingsCount++ + " Distance : " + distanceReading + " servo : " + servoValue);

						while( distanceReading < 0.15 && readingsCount < 5 ) {
							int currentServoValue = servoValue != 180 ? servoValue + 45 : 0;
							setServoMotor(currentServoValue);
							servoValue = currentServoValue;

							getDistanceSensor();
							System.out.println("readingCount: "+ readingsCount++ + " Distance : " + distanceReading + " servo : " + servoValue);
							StopObstacle();
							v.vibrate(50);
							try {
								sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						} if( distanceReading > 0.15 || readingsCount > 5) {
							distanceReading = 0.0f;
							readingsCount = 0;
							Walk();
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
			ch2.open(500);
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


			try{
				ch3.open(500);

				//set position
				ch3.setTargetPosition(value);
				ch3.setEngaged(true);

				sleep(1000);

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


				if( distanceReading > 0.15 || readingsCount > 4) {

					System.out.println("process stops at distance: " + distanceReading + " reading count : " + readingsCount);

					readingsCount = 0;
					distanceReading = 0.0f;
					servoValue = -45;
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

	private void Walk() {
		// TODO Auto-generated method stub
		text = "Continue walking there is no obstacles";
		tts.setLanguage(Locale.US);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}
	public void onResume() {
		super.onResume();
		sensorManager.registerListener(gyroListener, Gyrosensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void onStop() {
		super.onStop();
		sensorManager.unregisterListener(gyroListener);
	}

	public SensorEventListener gyroListener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor sensor, int acc) {
		}

		public void onSensorChanged(SensorEvent event) {

			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			mLowPassX = lowpass(x,mLowPassX);
			mLowPassY = lowpass(y,mLowPassY);
			mLowPassZ = lowpass(z,mLowPassZ);

			System.out.println("X : " + (int) Math.toDegrees(mLowPassX) + " degrees");
			System.out.println("Y : " + (int) Math.toDegrees(mLowPassY) + " degrees");
			System.out.println("Z : " + (int) Math.toDegrees(mLowPassZ) + " degrees");
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (y > 45){
				v.vibrate(50);
				StopObstacle();
				setServoMotor((int) y);
			}
		}
	};

// filter function for the orientation readings in gyroscope
	float lowpass(float current ,float last ){

	return last * (1.0f - a) + current*a;
	}
}

