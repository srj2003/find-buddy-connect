package com.example.authapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class NextActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private BluetoothAdapter bluetoothAdapter;
    private MediaPlayer mediaPlayer;
    private boolean isAlarmPlaying = false; // Track alarm status

    Button btnLogOut, btnBluetoothOn, btnBluetoothOff;
    TextView txtUserUID;  // TextView to display UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btnBluetoothOn = findViewById(R.id.btnOn);
        btnBluetoothOff = findViewById(R.id.btnOff);
        btnLogOut = findViewById(R.id.btnLogOut);
        txtUserUID = findViewById(R.id.txtUserUID);

        // Display the current user's UID if signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            txtUserUID.setText("Unique user id:- " + uid);
            UserApiClient apiClient = new UserApiClient();
            apiClient.sendUIDToServer(uid, new UserApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> Toast.makeText(NextActivity.this, "Server Response: " + response, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> Toast.makeText(NextActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show();
        }

        // Bluetooth ON button click handler
        btnBluetoothOn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(NextActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(NextActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            } else {
                turnOnBluetooth();
            }
        });

        // Bluetooth OFF button click handler (alarm triggers when Bluetooth is turned off)
        btnBluetoothOff.setOnClickListener(v -> {
            stopAlarm();  // Stop alarm before turning Bluetooth off
            bluetoothAdapter.disable(); // Turn Bluetooth off
            Toast.makeText(NextActivity.this, "Bluetooth is turned off", Toast.LENGTH_SHORT).show();
            playAlarm();  // Play alarm when Bluetooth is turned off
        });

        // Logout button click handler
        btnLogOut.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        });
    }

    private void turnOnBluetooth() {
        try {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            } else if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            } else {
                Toast.makeText(this, "Bluetooth is already on", Toast.LENGTH_SHORT).show();

                // Stop alarm if Bluetooth is turned on
                stopAlarm();

                // Start connection monitoring
                monitorConnectionStatus();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Security Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothSocket bluetoothSocket;

    private void monitorConnectionStatus() {
        new Thread(() -> {
            try {
                BluetoothDevice espDevice = bluetoothAdapter.getRemoteDevice("D8:BC:38:FC:99:F6"); // Replace with ESP32 MAC Address
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothSocket = espDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                bluetoothSocket.connect();

                runOnUiThread(() -> Toast.makeText(NextActivity.this, "Connected to ESP32", Toast.LENGTH_SHORT).show());

                // Monitor connection
                while (true) {
                    if (!bluetoothSocket.isConnected()) {
                        runOnUiThread(() -> {
                            Toast.makeText(NextActivity.this, "Connection lost with ESP32", Toast.LENGTH_SHORT).show();
                            showNotification("Bluetooth Connection Lost", "Connection with ESP32 has been lost.");
                            playAlarm(); // Play the alarm if connection lost
                        });
                        break;
                    }
                    Thread.sleep(1000); // Check connection every second
                }
            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> {
                    Toast.makeText(NextActivity.this, "Error in Bluetooth connection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showNotification("Bluetooth Error", "Failed to maintain connection with ESP32.");
                    playAlarm(); // Play the alarm if connection error
                });
            }
        }).start();
    }

    // Show notification function
    private void showNotification(String title, String message) {
        String channelId = "Bluetooth_Channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Create a notification channel for Android 8.0 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Bluetooth Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_bluetooth) // Replace with your app's Bluetooth icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(1, builder.build());
    }

    // Play alarm sound function
    private void playAlarm() {
        if (!isAlarmPlaying) {
            isAlarmPlaying = true;
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm); // Use your alarm sound file
            }

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }
    }

    // Stop alarm sound function
    private void stopAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer = null;
            isAlarmPlaying = false; // Reset alarm status
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                turnOnBluetooth();
            } else {
                Toast.makeText(this, "Permission denied. Bluetooth cannot be enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
