package com.linglong.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.linglong.ble.R;
import com.linglong.ble.ReadThread;
import com.linglong.ble.WriteThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author : kui
 * date   : 2018/11/30  10:35
 * desc   :
 * version: 1.0
 */
public class ClassicBleActivity extends AppCompatActivity {
    public static final String TAG = "ClassicBleActivity";
    public static final String IS_SERVER = "isServer";
    private BluetoothAdapter adapter;
    private BluetoothServerSocket serverSocket;
    private HandlerThread handlerThread;
    private ReadThread readThread;
    private WriteThread writeThread;
    private BluetoothSocket socket;
    private String serverName = "kuiServer";
    private EditText edtInput;
    private Button button;
    private boolean isServer = true;
    private MyRecever recever;
    public static final String NAME = "kui";
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serrver1);
        adapter = BluetoothAdapter.getDefaultAdapter();
        edtInput = findViewById(R.id.edtInput);
        button = findViewById(R.id.btnServer);
        Intent intent = getIntent();
        if (intent!=null){
            isServer = intent.getBooleanExtra(IS_SERVER,true);
        }
        if(!isServer){
            button.setText("启动客户端");
        }
    }


    private void registerRecever(){
        recever = new MyRecever();
        IntentFilter filter1 = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        IntentFilter filter2 = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(recever,filter1);
        registerReceiver(recever,filter2);
        registerReceiver(recever,filter3);

        if (adapter.isDiscovering()){
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();
    }

    public void click(View view){
        switch (view.getId()){
            case R.id.btnSend:
                String text = edtInput.getText().toString().trim();
                sendString(text);
                break;
            case R.id.btnServer:
                if (isServer){
                    openServer();
                }else {
                    registerRecever();
                }
                break;
        }
    }

    private void openClient(final BluetoothDevice device){
        Log.e(TAG,"openClient device:"+device.getName());
        Log.e(TAG,"openClient device:"+device.getAddress());
        Log.e(TAG,"openClient device:"+device.toString());
        ParcelUuid[] uuids = device.getUuids();
        readThread = new ReadThread("readThread");
        readThread.start();
        Handler handler = new Handler(readThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    if (!socket.isConnected()){
                        socket.connect();
                    }
                    readThread.startRead(socket);
                } catch (IOException e) {
                    Log.e(TAG,"openClient IOException:"+e.getMessage());
                }
            }
        });
    }

    private void openServer(){
        //UUID格式一般是"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"可到
        //http://www.uuidgenerator.com 申请
        readThread = new ReadThread("MyServer");
        readThread.start();
        Handler handler = new Handler(readThread.getLooper());
        adapter.setName(NAME);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (serverSocket==null){
                    try {
                        serverSocket = adapter.listenUsingRfcommWithServiceRecord(serverName,uuid);
                        socket = serverSocket.accept();
                        Log.e(TAG,"服务端收到连接请求:");
                        serverSocket.close();//只监听一个
                        if (!socket.isConnected()){
                            socket.connect();
                        }
                        readThread.startRead(socket);
                    } catch (IOException e) {
                        Log.e(TAG,"openServer IOException:"+e.getMessage());
                    }
                }
            }
        });
    }

    public void sendString(String string){
        if (socket==null){
            return;
        }
        if (writeThread==null){
            writeThread = new WriteThread("writeThread",socket);
        }
        writeThread.startWrite(string);
    }

    class MyRecever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action:" + action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "开始扫描...");
                    //callBack.onScanStarted();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "结束扫描...");
                    //callBack.onScanFinished();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    Log.d(TAG, "发现设备:"+device.getName());
                    if (NAME.equals(device.getName())){
                        adapter.cancelDiscovery();
                        openClient(device);
                    }
                    //callBack.onScanning(device);
                    break;
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recever!=null){
            unregisterReceiver(recever);
        }
        if (readThread!=null){
            readThread.quit();
            readThread = null;
        }
        if (writeThread!=null){
            writeThread.quit();
            writeThread = null;
        }
        if (socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                socket = null;
            }
        }
    }
}
