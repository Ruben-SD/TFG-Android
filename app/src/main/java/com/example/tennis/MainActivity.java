package com.example.tennis;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
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
    private int sampleRate = 44100;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_8BIT;

    AudioTrack audioTrack;


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
        fpsText = (TextView) findViewById(R.id.textViewFPS);
        showChessPatternButton = (Button) findViewById(R.id.showChessPatternButton);
        connectedToText.setText("Sending data to " + pcIp.toString());
        submitIPButton.setOnClickListener(v -> {
            try {
                pcIp = InetAddress.getByName(ipAddrInput.getText().toString());
                connectedToText.setText("Sending data to " + pcIp.toString());
            } catch (UnknownHostException e) {}
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

        playSound();
        startStreaming();
    }



    public void startStreaming() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            private long startTime = System.currentTimeMillis();
            public void run() {
                try {
                    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");
                    Log.d("VS", String.valueOf(minBufSize));
                    byte[] buffer = new byte[minBufSize + 4];

                    Log.d("VS", "Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channelConfig, audioFormat, minBufSize);

                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();

                    byte[] sizeBytes = ByteBuffer.allocate(4).putInt(minBufSize + 4).array();
                    buffer[0] = sizeBytes[0];
                    buffer[1] = sizeBytes[1];
                    buffer[2] = sizeBytes[2];
                    buffer[3] = sizeBytes[3];


                    while(true) {
                        long start = System.currentTimeMillis();
                        //reading data from MIC into buffer
                        int bytesRead = recorder.read(buffer, 4, buffer.length - 4);
                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer, buffer.length, pcIp,5555);
                        socket.send(packet);
                        handler.post(new Runnable() {
                            public void run() {
                                long now = System.currentTimeMillis() - start;
                                fpsText.setText("FPS: " + String.valueOf(1000/now));
                        }});
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

    private void playSound() {
        byte soundSamples[] = genTone(18000);
        byte soundSamples2[] = genTone(19000);
        for (int i = 0; i < soundSamples.length; i += 2) {
            byte b0 = soundSamples[i];
            byte b1 = soundSamples[i + 1];

            byte b2 = soundSamples2[i];
            byte b3 = soundSamples2[i + 1];

            short a = (short) (b0 | (b1 << 8));
            short b = (short) (b2 | (b3 << 8));

            short res = (short) (a + b);
            soundSamples[i] = (byte) (res & 0xff);
            soundSamples[i + 1] = (byte) ((res & 0xff00) >> 8);
        }
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, soundSamples.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(soundSamples, 0, soundSamples.length);
        audioTrack.setLoopPoints(0, soundSamples.length/4, -1);
        audioTrack.play();
        Log.d("TAG", "Playing sound");
    }

    byte[] genTone(double freqOfTone){
        int sampleRate = 44100;
        int numSamples = sampleRate;
        double sample[] = new double[numSamples];
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        byte soundSamples[] = new byte[2 * numSamples];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 100));
            // in 16 bit wav PCM, first byte is the low order byte
            soundSamples[idx++] = (byte) (val & 0x00ff);
            soundSamples[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return soundSamples;
    }
}