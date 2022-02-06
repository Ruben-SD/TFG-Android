package com.example.tennis;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorListener;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextView mTextViewAzimuth;
    private TextView mTextViewPitch;
    private TextView mTextViewRoll;

    private DatagramSocket socket = new DatagramSocket();

    private String ipAddr = "192.168.1.40"; //pc
    EditText ipAddrInput;
    Button submitIPButton;
    TextView fpsText;
    long start;
    int fps;

    AudioRecord recorder;
    private int sampleRate = 44100;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_8BIT;


    public MainActivity() throws SocketException {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipAddrInput = (EditText) findViewById(R.id.editTextIPAddress);
        submitIPButton = (Button) findViewById(R.id.buttonSubmitIP);
        submitIPButton.setOnClickListener(v -> ipAddr = ipAddrInput.getText().toString());
        fpsText = (TextView) findViewById(R.id.fpsText);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET}, 1);
        startStreaming();

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update TextView here!

//                                fpsText.setText("FPS: " + (start - SystemClock.elapsedRealtime()));
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }

    public void startStreaming() {
        Thread streamThread = new Thread(() -> {
            try {
                int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                DatagramSocket socket = new DatagramSocket();
                Log.d("VS", "Socket Created");
                Log.d("VS", String.valueOf(minBufSize));
                byte[] buffer = new byte[minBufSize + 4];

                Log.d("VS", "Buffer created of size " + minBufSize);
                DatagramPacket packet;

                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);
                Log.d("VS", "Recorder initialized");

                recorder.startRecording();

                byte[] sizeBytes = ByteBuffer.allocate(4).putInt(minBufSize + 4).array();
                buffer[0] = sizeBytes[0];
                buffer[1] = sizeBytes[1];
                buffer[2] = sizeBytes[2];
                buffer[3] = sizeBytes[3];


                while(true) {
                    //reading data from MIC into buffer
                    int bytesRead = recorder.read(buffer, 4, buffer.length - 4);
                    //putting buffer in the packet
                    InetAddress destination = InetAddress.getByName(ipAddr);
                    packet = new DatagramPacket (buffer,buffer.length,destination,5555);
                    socket.send(packet);
                }
            } catch(UnknownHostException e) {
                Log.e("VS", "UnknownHostException");
            } catch (IOException e) {
                Log.e("VS", "IOException");
            }
        });
        streamThread.start();
    }
}