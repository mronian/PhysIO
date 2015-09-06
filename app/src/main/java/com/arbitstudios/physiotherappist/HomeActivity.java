package com.arbitstudios.physiotherappist;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/* Reference taken from
 *  http://simena86.github.io/blog/2013/04/30/logging-accelerometer-from-android-to-pc/
 */

public class HomeActivity extends Activity {
    private SensorManager _sensorManager;
    private Sensor _linearAccelerationSensor;
    private Sensor _accelerometer;

    private static String _serverIP = "10.132.90.180";
    private static int _serverPort = 8000;

    private TextView _dataText;
    private EditText _serverIPField;
    private EditText _serverPortField;
    private Button _sendBtn;

    private float _linX, _linY, _linZ;
    private float _accX, _accY, _accZ;

    private boolean _connected = false;
    boolean _isStreaming = false;

    PrintWriter out;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize UI items
        _dataText = (TextView)findViewById(R.id.data_text);
        _serverPortField = (EditText)findViewById(R.id.server_port);
        _serverIPField = (EditText)findViewById(R.id.server_ip);


        _sendBtn = (Button)findViewById(R.id.send_btn);
        _sendBtn.setOnClickListener(_connectListener);

        // Initialize the Linear acceleration sensor
        _sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _linearAccelerationSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        _accelerometer = _sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        _dataText.setText("Press send to stream acceleration measurement");
        _isStreaming = false;

        Log.d("begin", "first log");
    }

    private Button.OnClickListener _connectListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!_connected) {
                if (!_serverIP.equals("")) {
                    _sendBtn.setText("Stop Streaming");
                    Thread cThread = new Thread(new ClientThread());
                    cThread.start();
                }
            } else {
                _sendBtn.setText("Start Streaming");
                _connected = false;
                _isStreaming = false;
            }
        }
    };

    public class ClientThread implements Runnable {
        Socket socket;
        public void run() {
            try {
                _isStreaming = true;
                _serverPort = Integer.parseInt(_serverPortField.getText().toString());
                _serverIP = _serverIPField.getText().toString();
                Log.d("ServerIP", _serverIP);
                InetAddress serverAddr = InetAddress.getByName(_serverIP);
                socket = new Socket(serverAddr, _serverPort);
                _connected = true;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                while (_connected) {
                    out.printf("lin,%.2f,%.2f,%.2f\n", _linX, _linY, _linZ);
                    out.printf("acc,%.2f,%.2f,%.2f\n", _accX, _accY, _accZ);
                    out.flush();
                    Thread.sleep(2);
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getStackTrace().toString(), e.getCause());
            }
            finally{
                try{
                    Log.d("Somehow","wth");
                    _isStreaming = false;
                    _sendBtn.setText("Start Streaming");
                    //out.close();
                    socket.close();
                }catch(Exception a){
                }
            }
        }
    };

    private SensorEventListener _linearAccelerationListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            _linX = event.values[0];
            _linY = event.values[1];
            _linZ = event.values[2];
            refreshDisplay();
        }
    };

    private SensorEventListener _accelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            _accX = event.values[0];
            _accY = event.values[1];
            _accZ = event.values[2];
            refreshDisplay();
        }
    };

    private void refreshDisplay() {
        if(_isStreaming == true){
            String output = String.format("X:%.2f m/s^2  |  Y:%.2f m/s^2  |   Z:%.2f m/s^2\n", _linX, _linY, _linZ);
            output += String.format("X:%.2f m/s^2  |  Y:%.2f m/s^2  |   Z:%.2f m/s^2", _accX, _accY, _accZ);
            _dataText.setText(output);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        _sensorManager.registerListener(_linearAccelerationListener, _linearAccelerationSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
        _sensorManager.registerListener(_accelerometerListener, _accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        _sensorManager.unregisterListener(_linearAccelerationListener);
        _sensorManager.unregisterListener(_accelerometerListener);
        super.onStop();
    }
}
