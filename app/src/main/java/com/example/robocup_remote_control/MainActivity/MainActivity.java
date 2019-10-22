package com.example.robocup_remote_control.MainActivity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.robocup_remote_control.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;


import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {


    private TextView mTextViewAngleLeft;
    private TextView mTextViewStrengthLeft;

    private TextView mTextViewAngleRight;
    private TextView mTextViewStrengthRight;
    private TextView mTextViewCoordinateRight;

    short bodyX = 0, bodyY = 0, bodyW = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewAngleLeft = (TextView) findViewById(R.id.textView_angle_left);
        mTextViewStrengthLeft = (TextView) findViewById(R.id.textView_strength_left);

        new Thread(new UDP_Client()).start();

        final JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mTextViewAngleLeft.setText(angle + "°");
                mTextViewStrengthLeft.setText(strength + "%");
            }
        });


        mTextViewAngleRight = (TextView) findViewById(R.id.textView_angle_right);
        mTextViewStrengthRight = (TextView) findViewById(R.id.textView_strength_right);
        mTextViewCoordinateRight = findViewById(R.id.textView_coordinate_right);

        final JoystickView joystickRight = (JoystickView) findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onMove(int angle, int strength) {
                bodyX = (short) (100 * joystickRight.getNormalizedX() - 5000);
                bodyY = (short) -(100 * joystickRight.getNormalizedY() - 5000);
                mTextViewCoordinateRight.setText(
                        String.format("%d, %d",
                                bodyX,
                                bodyY)
                );
            }
        });
    }

    public class UDP_Client implements Runnable {
        public String Message;

        @Override
        public void run() {
            boolean run = true;
            boolean recieving = true;
            int port = 25565;
            int dest_port = 25566;
            InetAddress dest_addr = null;

            try {
                DatagramSocket udpSocket = new DatagramSocket(port);
                while (recieving) {
                    try {
                        byte[] message = new byte[8000];
                        DatagramPacket packet = new DatagramPacket(message, message.length);
                        Log.i("UDP", "about to wait to receive");
                        udpSocket.setSoTimeout(10000);
                        udpSocket.receive(packet);
                        dest_addr = packet.getAddress();
                        Log.d("UDP", "Received from " + packet.getSocketAddress());
                    } catch (IOException e) {
                        Log.e("UDP", "error: ", e);
                        recieving = false;
                        udpSocket.close();
                        break;
                    }

                    if (dest_addr != null) {
                        while (run) {
                            byte[] buf = new byte[9];
                            buf[0] = (byte) (bodyX >> 8);
                            buf[1] = (byte) bodyX;
                            buf[2] = (byte) (bodyY >> 8);
                            buf[3] = (byte) bodyY;
                            buf[4] = (byte) (bodyW >> 8);
                            buf[5] = (byte) bodyW;
                            Log.d("UDP", "Sending packet to " + dest_addr + ":" + dest_port);
                            DatagramPacket packet = new DatagramPacket(buf, 0, buf.length, dest_addr, dest_port);
                            udpSocket.send(packet);
                            Log.d("UDP", "Sent packet");
                        }
                    }
                }
            } catch (SocketException e) {
                Log.e("Socket Open:", "Error:", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
