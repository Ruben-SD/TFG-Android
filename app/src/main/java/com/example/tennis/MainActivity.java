package com.example.tennis;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.PowerManager;
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

    private InetAddress pcIp = InetAddress.getByName("192.168.1.36");
    EditText ipAddrInput;
    Button submitIPButton, showChessPatternButton;
    TextView fpsText, connectedToText;
    long start;
    int fps;

    AudioRecord recorder;
    private int sampleRate = 48000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;


    public MainActivity() throws SocketException, UnknownHostException {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        String size = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        Log.d("Buffer Size aple rate", "Size :" + size + " & Rate: " + rate);
        ipAddrInput = (EditText) findViewById(R.id.editTextIPAddress);
        submitIPButton = (Button) findViewById(R.id.submitIpButton);
        connectedToText = (TextView) findViewById(R.id.connectedToText);
        fpsText = (TextView) findViewById(R.id.textViewFPS);
        showChessPatternButton = (Button) findViewById(R.id.showChessPatternButton);
        connectedToText.setText("Sending data to " + pcIp.toString());
        submitIPButton.setOnClickListener(v -> {
            try {
                pcIp = InetAddress.getByName(ipAddrInput.getText().toString());
                connectedToText.setText("Sending data to " + pcIp.toString());
            } catch (UnknownHostException e) {
            }
        });
        Intent intent = new Intent(this, ChessPattern.class);
        showChessPatternButton.setOnClickListener(v -> {
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            if (layout.screenBrightness == 0F)
                layout.screenBrightness = -1;
            else layout.screenBrightness = 0F;
            getWindow().setAttributes(layout);
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET}, 1);

        startStreaming();
    }


    public void startStreaming() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            private long startTime = System.currentTimeMillis();

            @RequiresApi(api = Build.VERSION_CODES.M)
            public void run() {
                try {
                    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");
                    Log.d("VS", String.valueOf(minBufSize));
                    byte[] buffer = new byte[minBufSize + 4];

                    Log.d("VS", "Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);
                    if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                        // Oops looks like it was not initalized correctly
                        Log.d("TAG", "ERROR");
                        return;
                    }
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();

                    //byte[] sizeBytes = ByteBuffer.allocate(4).putInt(1920 + 4).array();


                    //byte[] sizeBytes = ByteBuffer.allocate(4).putLong(start).array();

                    float[] floatsBuffer = new float[1920 + 1];

                    int i = -1;
                    while(true) {
                        long start = System.currentTimeMillis();
                        floatsBuffer[0] = ++i;

                        //reading data from MIC into buffer
                        int bytesRead = recorder.read(floatsBuffer, 1, 1920, AudioRecord.READ_BLOCKING);

                        //putting buffer in the packet
                        ByteBuffer bytesBuffer = ByteBuffer.allocate(floatsBuffer.length * (Float.SIZE/Byte.SIZE));
                        bytesBuffer.asFloatBuffer().put(floatsBuffer);

                        packet = new DatagramPacket(bytesBuffer.array(), bytesBuffer.array().length, pcIp,5555);
                        socket.send(packet);

                        handler.post(() -> {
                            long now = System.currentTimeMillis() - start;

                            if (now != 0)
                                fpsText.setText("FPS: " + 1000/now);
                        });
                    }
                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    Log.e("VS", "IOException");
                }
            }
        };
        new Thread(runnable).start();
    }
}