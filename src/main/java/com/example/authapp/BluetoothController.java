package com.example.authapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BluetoothController extends AppCompatActivity {
    private static final String TAG = "BluetoothController";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice esp32Device;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected = false;

    private Button btnConnect, btnSend;
    private EditText messageInput;
    private TextView receivedDataText;
    private ListView pairedDevicesList;
    private ArrayAdapter<String> btArrayAdapter;
    private ArrayList<BluetoothDevice> deviceAddressArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_controller);

        // Initialize UI components
        btnConnect = findViewById(R.id.btnConnect);
        btnSend = findViewById(R.id.btnSend);
        messageInput = findViewById(R.id.messageInput);
        receivedDataText = findViewById(R.id.receivedDataText);
        pairedDevicesList = findViewById(R.id.pairedDevicesList);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        pairedDevicesList.setAdapter(btArrayAdapter);

        // Check if device supports Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up button click listeners
        btnConnect.setOnClickListener(v -> listPairedDevices());
        btnSend.setOnClickListener(v -> sendMessage());

        // Set up device selection listener
        pairedDevicesList.setOnItemClickListener((parent, view, position, id) -> {
            esp32Device = deviceAddressArray.get(position);
            connectToDevice();
        });
    }

    private void listPairedDevices() {
        if (checkBluetoothPermissions()) {
            btArrayAdapter.clear();
            deviceAddressArray.clear();

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    deviceAddressArray.add(device);
                }
            }
            btArrayAdapter.notifyDataSetChanged();
        }
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private void connectToDevice() {
        if (esp32Device == null) {
            Toast.makeText(this, "Please select a device", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                if (checkBluetoothPermissions()) {
                    bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    isConnected = true;

                    runOnUiThread(() -> {
                        Toast.makeText(BluetoothController.this,
                                "Connected to " + esp32Device.getName(),
                                Toast.LENGTH_SHORT).show();
                        btnConnect.setText("Disconnect");
                        startListeningForData();
                    });
                }
            } catch (IOException e) {
                isConnected = false;
                runOnUiThread(() -> Toast.makeText(BluetoothController.this,
                        "Connection failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close socket", closeException);
                }
            }
        }).start();
    }

    private void startListeningForData() {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    String received = new String(buffer, 0, bytes);
                    runOnUiThread(() -> {
                        receivedDataText.append(received + "\n");
                    });
                } catch (IOException e) {
                    break;
                }
            }
        });
        thread.start();
    }

    private void sendMessage() {
        if (!isConnected) {
            Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            outputStream.write(message.getBytes());
            messageInput.setText("");
        } catch (IOException e) {
            Toast.makeText(this, "Error sending message: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                isConnected = false;
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }
}