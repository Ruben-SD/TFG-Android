package com.example.tennis;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;


public class MainActivity extends AppCompatActivity {

    private TextView mTextViewAzimuth;
    private TextView mTextViewPitch;
    private TextView mTextViewRoll;

    private DatagramSocket socket = new DatagramSocket();

    private InetAddress pcIp = InetAddress.getByName("192.168.1.40");
    EditText ipAddrInput;
    Button submitIPButton, showChessPatternButton;
    TextView fpsText, connectedToText;
    long start;
    int fps;

    AudioRecord recorder;
    private int sampleRate = 44100;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_8BIT;


    public MainActivity() throws SocketException, UnknownHostException {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipAddrInput = (EditText) findViewById(R.id.editTextIPAddress);
        submitIPButton = (Button) findViewById(R.id.submitIpButton);
        connectedToText = (TextView) findViewById(R.id.connectedToText);
        fpsText = (TextView) findViewById(R.id.fpsText);
        showChessPatternButton = (Button) findViewById(R.id.showChessPatternButton);
        connectedToText.setText("Sending data to " + pcIp.toString());
        submitIPButton.setOnClickListener(v -> {
            try {
                pcIp = InetAddress.getByName(ipAddrInput.getText().toString());
                connectedToText.setText("Sending data to " + pcIp.toString());
            } catch (UnknownHostException e) {}
        });
        Intent intent = new Intent(this, ChessPattern.class);
        showChessPatternButton.setOnClickListener(v -> this.startActivity(intent));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET}, 1);

        startStreaming();
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
                    packet = new DatagramPacket(buffer, buffer.length, pcIp,5555);
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