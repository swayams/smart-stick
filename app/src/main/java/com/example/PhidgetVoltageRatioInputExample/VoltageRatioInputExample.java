// Search CSCM79 Advice for test modification
package com.example.PhidgetVoltageRatioInputExample;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.phidget22.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;



public class VoltageRatioInputExample extends Activity {

	VoltageRatioInput ch;
	VoltageRatioInput ch2;
	RCServo ch3;

	SeekBar dataIntervalBar;
	Spinner sensorTypeSpinner;
	Spinner bridgeGainSpinner;
	CheckBox bridgeEnabledBox;
	CheckBox isHubPortBox;

	boolean isHubPort;

	Toast errToast;

	int minDataInterval;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//Hide device information and settings until one is attached
		LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData);
		settingsAndData.setVisibility(LinearLayout.GONE);

		//set data interval seek bar functionality
		dataIntervalBar = (SeekBar) findViewById(R.id.dataIntervalBar);
		dataIntervalBar.setOnSeekBarChangeListener(new dataIntervalChangeListener());

		//set sensor type spinner functionality
		sensorTypeSpinner = (Spinner) findViewById(R.id.sensorTypeSpinner);
		sensorTypeSpinner.setOnItemSelectedListener(new sensorTypeChangeListener());
		LinearLayout sensorTypeSection = (LinearLayout) findViewById(R.id.sensorTypeSection);
		sensorTypeSection.setVisibility(LinearLayout.GONE);

		//set bridge gain spinner functionality
		bridgeGainSpinner = (Spinner) findViewById(R.id.bridgeGainSpinner);
		bridgeGainSpinner.setOnItemSelectedListener(new bridgeGainChangeListener());
		LinearLayout bridgeSection = (LinearLayout) findViewById(R.id.bridgeSection);
		bridgeSection.setVisibility(LinearLayout.GONE);

		//set up bridge enabled functionality
		bridgeEnabledBox = (CheckBox) findViewById(R.id.bridgeEnabledBox);
		bridgeEnabledBox.setOnCheckedChangeListener(new bridgeEnabledChangeListener());

		//Voltage ratio visible and sensor value not by default
		((LinearLayout) findViewById(R.id.voltageRatioInfo)).setVisibility(LinearLayout.VISIBLE);
		((LinearLayout) findViewById(R.id.sensorInfo)).setVisibility(LinearLayout.GONE);

		//set up "is hub port" functionality
		isHubPortBox = (CheckBox) findViewById(R.id.isHubPortBox);
		isHubPortBox.setOnCheckedChangeListener(new isHubPortChangeListener());

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
			ch.setChannel(4);
			ch.setDeviceSerialNumber(30686);





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

//					  VoltageRatioInputVoltageRatioChangeEventHandler handler = new VoltageRatioInputVoltageRatioChangeEventHandler(ch, voltageRatioChangeEvent);
					System.out.println("Joystick " + voltageRatioChangeEvent.getVoltageRatio());
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

			ch.open();

		} catch (PhidgetException pe) {
			pe.printStackTrace();
		}

	}

	private class dataIntervalChangeListener implements SeekBar.OnSeekBarChangeListener {
		public void onProgressChanged(SeekBar seekBar, int progress,
									  boolean fromUser) {
			try {
				TextView dataIntervalTxt = (TextView) findViewById(R.id.dataIntervalTxt);
				int dataInterval = progress + minDataInterval;
				dataIntervalTxt.setText(String.valueOf(dataInterval));
				ch.setDataInterval(dataInterval);
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}

		public void onStartTrackingTouch(SeekBar seekBar) {}

		public void onStopTrackingTouch(SeekBar seekBar) {}
	}

	private class sensorTypeChangeListener implements Spinner.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
			try {
				ch.setSensorType(VoltageRatioSensorType.valueOf(parentView.getItemAtPosition(position).toString()));
				if(VoltageRatioSensorType.valueOf(parentView.getItemAtPosition(position).toString()) == VoltageRatioSensorType.VOLTAGE_RATIO) {
					((LinearLayout) findViewById(R.id.voltageRatioInfo)).setVisibility(LinearLayout.VISIBLE);
					((LinearLayout) findViewById(R.id.sensorInfo)).setVisibility(LinearLayout.GONE);
				} else {
					((LinearLayout) findViewById(R.id.voltageRatioInfo)).setVisibility(LinearLayout.GONE);
					((LinearLayout) findViewById(R.id.sensorInfo)).setVisibility(LinearLayout.VISIBLE);
				}
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
			// your code here
		}

	}

	private class bridgeGainChangeListener implements Spinner.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
			try {
				ch.setBridgeGain(BridgeGain.valueOf(parentView.getItemAtPosition(position).toString()));
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
			// your code here
		}

	}

	private class bridgeEnabledChangeListener implements CheckBox.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			try {
				ch.setBridgeEnabled(isChecked);
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}
	}

	private class isHubPortChangeListener implements CheckBox.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			try {
				runOnUiThread(new DetachEventHandler(ch));
				ch.close();
				ch.setIsHubPortDevice(isChecked);
				ch.open();
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}
	}

	class AttachEventHandler implements Runnable {
		Phidget ch;

		public AttachEventHandler(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData);
			settingsAndData.setVisibility(LinearLayout.VISIBLE);

			TextView attachedTxt = (TextView) findViewById(R.id.attachedTxt);

			attachedTxt.setText("Attached");

			try {
				TextView nameTxt = (TextView) findViewById(R.id.nameTxt);
				TextView serialTxt = (TextView) findViewById(R.id.serialTxt);
				TextView versionTxt = (TextView) findViewById(R.id.versionTxt);
				TextView channelTxt = (TextView) findViewById(R.id.channelTxt);
				TextView hubPortTxt = (TextView) findViewById(R.id.hubPortTxt);
				TextView labelTxt = (TextView) findViewById(R.id.labelTxt);

				nameTxt.setText(ch.getDeviceName());
				serialTxt.setText(Integer.toString(ch.getDeviceSerialNumber()));
				versionTxt.setText(Integer.toString(ch.getDeviceVersion()));
				channelTxt.setText(Integer.toString(ch.getChannel()));
				hubPortTxt.setText(Integer.toString(ch.getHubPort()));
				labelTxt.setText(ch.getDeviceLabel());

				TextView dataIntervalTxt = (TextView) findViewById(R.id.dataIntervalTxt);
				dataIntervalTxt.setText(String.valueOf(((VoltageRatioInput)ch).getDataInterval()));

				minDataInterval = ((VoltageRatioInput)ch).getMinDataInterval();

				SeekBar dataIntervalBar = (SeekBar) findViewById(R.id.dataIntervalBar);
				dataIntervalBar.setProgress(((VoltageRatioInput)ch).getDataInterval() - minDataInterval);

				//Limit the maximum dataInterval on the SeekBar to 5000 so it remains usable
				if(((VoltageRatioInput)ch).getMaxDataInterval() >= 5000)
					dataIntervalBar.setMax(5000 - minDataInterval);
				else
					dataIntervalBar.setMax(((VoltageRatioInput)ch).getMaxDataInterval() - minDataInterval);

				List<BridgeGain> supportedBridgeGains;
				switch (ch.getChannelSubclass()) { //initialize form elements based on detected device
					case VOLTAGE_RATIO_INPUT_BRIDGE:
						if(ch.getDeviceID() == DeviceID.PN_1046) {
							supportedBridgeGains = new ArrayList<BridgeGain>();
							supportedBridgeGains.add(BridgeGain.GAIN_1X);
							supportedBridgeGains.add(BridgeGain.GAIN_8X);
							supportedBridgeGains.add(BridgeGain.GAIN_16X);
							supportedBridgeGains.add(BridgeGain.GAIN_32X);
							supportedBridgeGains.add(BridgeGain.GAIN_64X);
							supportedBridgeGains.add(BridgeGain.GAIN_128X);

							bridgeGainSpinner.setAdapter(new ArrayAdapter<BridgeGain>(getApplicationContext(),
									android.R.layout.simple_spinner_item, supportedBridgeGains));

							//128x by default
							bridgeGainSpinner.setSelection(5);
						} else { //PN_DAQ1500:
							supportedBridgeGains = new ArrayList<BridgeGain>();
							supportedBridgeGains.add(BridgeGain.GAIN_1X);
							supportedBridgeGains.add(BridgeGain.GAIN_2X);
							supportedBridgeGains.add(BridgeGain.GAIN_64X);
							supportedBridgeGains.add(BridgeGain.GAIN_128X);

							bridgeGainSpinner.setAdapter(new ArrayAdapter<BridgeGain>(getApplicationContext(),
									android.R.layout.simple_spinner_item, supportedBridgeGains));

							//128x by default
							bridgeGainSpinner.setSelection(3);
						}

						((CheckBox)findViewById(R.id.bridgeEnabledBox)).setChecked(((VoltageRatioInput)ch).getBridgeEnabled());

						((LinearLayout)findViewById(R.id.bridgeSection)).setVisibility(LinearLayout.VISIBLE);
						break;

					default: //standard 5V sensor port
						if (ch.getChannelSubclass() == ChannelSubclass.VOLTAGE_RATIO_INPUT_SENSOR_PORT) {
							sensorTypeSpinner.setAdapter(new ArrayAdapter<VoltageRatioSensorType>(getApplicationContext(),
									android.R.layout.simple_spinner_item, VoltageRatioSensorType.values()));

							//VoltageRatio Sensor by default
							sensorTypeSpinner.setSelection(0);

							((LinearLayout) findViewById(R.id.sensorTypeSection)).setVisibility(LinearLayout.VISIBLE);
						}
						break;
				}
			} catch (PhidgetException e) {
				e.printStackTrace();
			}

		}
	}

	class DetachEventHandler implements Runnable {
		Phidget ch;

		public DetachEventHandler(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData);

			settingsAndData.setVisibility(LinearLayout.GONE);

			TextView attachedTxt = (TextView) findViewById(R.id.attachedTxt);
			attachedTxt.setText("Detached");

			TextView nameTxt = (TextView) findViewById(R.id.nameTxt);
			TextView serialTxt = (TextView) findViewById(R.id.serialTxt);
			TextView versionTxt = (TextView) findViewById(R.id.versionTxt);
			TextView channelTxt = (TextView) findViewById(R.id.channelTxt);
			TextView hubPortTxt = (TextView) findViewById(R.id.hubPortTxt);
			TextView labelTxt = (TextView) findViewById(R.id.labelTxt);

			nameTxt.setText(R.string.unknown_val);
			serialTxt.setText(R.string.unknown_val);
			versionTxt.setText(R.string.unknown_val);
			channelTxt.setText(R.string.unknown_val);
			hubPortTxt.setText(R.string.unknown_val);
			labelTxt.setText(R.string.unknown_val);

			((LinearLayout)findViewById(R.id.sensorTypeSection)).setVisibility(LinearLayout.GONE);
			((LinearLayout)findViewById(R.id.bridgeSection)).setVisibility(LinearLayout.GONE);

			//reset voltage ratio visibility
			((LinearLayout) findViewById(R.id.voltageRatioInfo)).setVisibility(LinearLayout.VISIBLE);
			((LinearLayout) findViewById(R.id.sensorInfo)).setVisibility(LinearLayout.GONE);

			//clear voltage ratio information
			((TextView)findViewById(R.id.voltageRatioTxt)).setText("");
			((TextView)findViewById(R.id.sensorValueTxt)).setText("");
			((TextView)findViewById(R.id.sensorUnits)).setText("");
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
			TextView sensorValueTxt = (TextView)findViewById(R.id.sensorValueTxt);
			TextView sensorUnits = (TextView)findViewById(R.id.sensorUnits);

			sensorValueTxt.setText(String.valueOf(sensorChangeEvent.getSensorValue()));
			sensorUnits.setText(sensorChangeEvent.getSensorUnit().symbol);
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
				ch2.setDeviceSerialNumber(30686);
				ch2.setChannel(3);


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

						System.out.println(voltageRatioChangeEvent.getVoltageRatio());

						if(voltageRatioChangeEvent.getVoltageRatio() > 0.8) {
							setServoMotor(voltageRatioChangeEvent.getVoltageRatio() );
						}

					}
				});

				ch2.addSensorChangeListener(new VoltageRatioInputSensorChangeListener() {
					public void onSensorChange(VoltageRatioInputSensorChangeEvent sensorChangeEvent) {
						VoltageRatioInputSensorChangeEventHandler handler = new VoltageRatioInputSensorChangeEventHandler(ch2, sensorChangeEvent);
						runOnUiThread(handler);
					}
				});

				ch2.open();
			} catch (PhidgetException pe) {
				pe.printStackTrace();
			}
		}
	}


	public void setServoMotor (double e  ) {


			//servo motor
		    // TODOS:  values -  0 - 180 where  0 ~ 0.8+ 180 ~ <0.1

			try{
				ch3 = new RCServo();
				ch3.setIsRemote(true);
				ch3.setDeviceSerialNumber(30686);
				ch3.setChannel(6);
				ch3.setEngaged(true);

				//set position

				ch3.setTargetPosition(e);


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


				ch3.open();
			} catch (PhidgetException pe) {
				pe.printStackTrace();
			}
		}


}

