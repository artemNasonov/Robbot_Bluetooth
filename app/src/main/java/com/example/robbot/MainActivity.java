package com.example.robbot;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Button find_robbot;
    private Button button_up;
    private Button button_down;
    private Button button_left;
    private Button button_right;

    ArrayAdapter<String> pairedDevicesArrayAdapter;

    BluetoothAdapter bluetoothAdapter;

    Set<BluetoothDevice> pairedDevices;

    BluetoothConnectThread bluetoothConnectThread = null;

    private Handler handler;

    private final static int REQUEST_ENABLE_BT = 1;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                pairedDevicesArrayAdapter.add(device.getName()+"\n"+device.getAddress());
            }
        }
    };

    private ListView mListViewPairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initButtonsAndOther();
        addListenersToButtons();
        initBluetooth();
        pairedDevicesArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        askForTurnBluetoothUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        bluetoothConnectThread.cancel();
    }

    @SuppressLint("HandlerLeak")
    private void initButtonsAndOther(){
        find_robbot = findViewById(R.id.find_robbot);
        button_up = findViewById(R.id.up_button);
        button_down = findViewById(R.id.down_button);
        button_left = findViewById(R.id.left_button);
        button_right = findViewById(R.id.right_button);
        mListViewPairedDevices = findViewById(R.id.pairedlist);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                if(msg.what == 1){
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private void addListenersToButtons(){
        find_robbot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkBluetoothEnabled()) showPairedDevices();
                else {
                    Toast.makeText(MainActivity.this, "Turn bluetooth on!!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        button_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.go_back, Toast.LENGTH_SHORT).show();
                if(bluetoothConnectThread!=null) bluetoothConnectThread.inOutBluetoothThread.write("с".getBytes());
            }
        });
        button_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.go_straight, Toast.LENGTH_SHORT).show();
            }
        });
        button_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.go_left, Toast.LENGTH_SHORT).show();
            }
        });
        button_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.go_right, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            Toast.makeText(MainActivity.this, "Your device don't support bluetooth, sorry", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @SuppressLint("HardwareIds")
    private void askForTurnBluetoothUp(){
        if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(MainActivity.this, "Turn your bluetooth device on!", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(MainActivity.this, bluetoothAdapter.getAddress()+"; "+bluetoothAdapter.getState(), Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkBluetoothEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    private void showPairedDevices(){
        if(pairedDevicesArrayAdapter!=null && !pairedDevicesArrayAdapter.isEmpty()){
            pairedDevicesArrayAdapter.clear();
        }
        bluetoothAdapter.startDiscovery();
        Toast.makeText(MainActivity.this, "discovery started", Toast.LENGTH_SHORT).show();
        pairedDevices = bluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bd: pairedDevices){
            pairedDevicesArrayAdapter.add(bd.getName()+";\n"+bd.getAddress());
        }
        mListViewPairedDevices.setAdapter(pairedDevicesArrayAdapter);
        mListViewPairedDevices.setVisibility(View.VISIBLE);
        mListViewPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                Toast.makeText(MainActivity.this, "discovery canceled", Toast.LENGTH_SHORT).show();
//                    mListViewPairedDevices.setVisibility(View.GONE);
                String robbotNameAndAddress = pairedDevicesArrayAdapter.getItem(position);
                BluetoothDevice deviceRobbot = bluetoothAdapter.getRemoteDevice(robbotNameAndAddress.substring(robbotNameAndAddress.length()-17));
                bluetoothConnectThread = new BluetoothConnectThread(deviceRobbot, handler);
                bluetoothConnectThread.start();
            }
        });
    }
}