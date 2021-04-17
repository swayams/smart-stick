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

	int servoValue = 0;
	float[] distanceArray = {};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

	

//		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//		Gyrosensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);



		try
		{
			ch = new VoltageRatioInput();

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
			//ch.setDataInterval(3000);


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
					//change to pressure sensor not joystick
//					  VoltageRatioInputVoltageRatioChangeEventHandler handler = new VoltageRatioInputVoltageRatioChangeEventHandler(ch, voltageRatioChangeEvent);
					//System.out.println("Pressure ( > 0.7 )  " + voltageRatioChangeEvent.getVoltageRatio());
					getDistanceSensor(voltageRatioChangeEvent.getVoltageRatio());
//					  runOnUiThread(handler);

				}
			});

			ch.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
				public void onSensorChange(VoltageRatioInputSensorChangeEvent sensorChangeEvent) {
					VoltageRatioInputSensorChangeEventHandler handler = new VoltageRatioInputSensorChangeEventHandler(ch, sensorChangeEvent);
					runOnUiThread(handler);
				}
			});

			ch.open(500);

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
			// remove all keep only print System.out.println("Attached");
			System.out.println("Attached");

		}
	}
	class DetachEventHandler implements Runnable {
		Phidget ch;

		public DetachEventHandler(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			// remove all keep only print System.out.println("Detached");
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
			// remove and put System.out.println(sensorChangeEvent.getSensorValue());
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
			System.out.println("RC Position "+ positionChangeEvent.getPosition());
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
			ch.close();

		} catch (PhidgetException e) {
			e.printStackTrace();
		}

		//Disable USB connection to Phidgets
		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
			com.phidget22.usb.Manager.Uninitialize();
	}

	public void getDistanceSensor (double e ) {
		if(e > 0.7) {
			//distance
			try{
				ch2 = new VoltageRatioInput();
				ch2.setIsRemote(true);
				ch2.setDeviceSerialNumber(39830);
				ch2.setChannel(3);
                //ch2.setDataInterval(2000);
				sleep (3000);

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
//
//						VoltageRatioInputVoltageRatioChangeEventHandler handler = new VoltageRatioInputVoltageRatioChangeEventHandler(ch2, voltageRatioChangeEvent);
//						runOnUiThread(handler);

						System.out.println("Distance ( < 0.3 ) " + voltageRatioChangeEvent.getVoltageRatio());

						setServoMotor(voltageRatioChangeEvent.getVoltageRatio() );
					}
				});

				ch2.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
					public void onSensorChange(VoltageRatioInputSensorChangeEvent sensorChangeEvent) {
						VoltageRatioInputSensorChangeEventHandler handler = new VoltageRatioInputSensorChangeEventHandler(ch2, sensorChangeEvent);
						runOnUiThread(handler);
					}
				});

				ch2.open(5000);
				
			} catch (PhidgetException pe) {
				pe.printStackTrace();
			} catch (InterruptedException interruptedException) {
				interruptedException.printStackTrace();
			}
		}
	}


	public void setServoMotor (double e) {

			if(e < 0.15) {
				try{
					ch3 = new RCServo();
					ch3.setIsRemote(true);
					ch3.setDeviceSerialNumber(19875);
					ch3.setChannel(0);
                    //ch3.setDataInterval(2000);
					ch3.open(500);


					//set position
					if(servoValue != 180 ) {
						servoValue += 45;
					} else {
						servoValue = 0;
					}

					ch3.setTargetPosition(servoValue);
					ch3.setEngaged(true);

					sleep(3000);

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
							runOnUiThread(handler);
						}
					});


				} catch (PhidgetException pe) {
					pe.printStackTrace();
				} catch (InterruptedException interruptedException) {
					interruptedException.printStackTrace();
				}
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

//	public void onResume() {
//		super.onResume();
//		sensorManager.registerListener(gyroListener, Gyrosensor, SensorManager.SENSOR_DELAY_NORMAL);
//	}
//
//	public void onStop() {
//		super.onStop();
//		sensorManager.unregisterListener(gyroListener);
//	}

// gyroscope listener is initialized inside the distance ch2 where if there is an obstacle
// it will get orientation readings and then send it to motor
	public SensorEventListener gyroListener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor sensor, int acc) {
		}

		public void onSensorChanged(SensorEvent event) {

//			float[] rotationMatrix = new float[16];
//			sensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
//			float[] remappedRotationMatrix = new float[16];
//			sensorManager.remapCoordinateSystem(rotationMatrix, sensorManager.AXIS_X, sensorManager.AXIS_Z, remappedRotationMatrix);
//
//			float[] orientations = new float[3];
//			sensorManager.getOrientation(remappedRotationMatrix, orientations);
//
//			// Convert values in radian to degrees
//			for (int i = 0; i < 3; i++) {
//				orientations[i] = (float) (Math.toDegrees(orientations[i]));
//				System.out.println(orientations[i]);
//				mLowPassi = lowpass(orientations[i],mLowPassi);
//			}
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			mLowPassX = lowpass(x,mLowPassX);
			mLowPassY = lowpass(y,mLowPassY);
			mLowPassZ = lowpass(z,mLowPassZ);

			System.out.println("X : " + (int) Math.toDegrees(mLowPassX) + " degrees");
			System.out.println("Y : " + (int) Math.toDegrees(mLowPassY) + " degrees");
			System.out.println("Z : " + (int) Math.toDegrees(mLowPassZ) + " degrees");
//			setServoMotor (mLowPassX, mLowPassY,mLowPassZ );
			if (y > 45){
				StopObstacle();
				v.vibrate(50);
			}
		}
	};

// filter function for the orientation readings in gyroscope
	float lowpass(float current ,float last ){

	return last * (1.0f - a) + current*a;
	}
}

