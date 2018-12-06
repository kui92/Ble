package com.linglong.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author : kui
 * date   : 2018/12/3  10:26
 * desc   :
 * version: 1.0
 */
public class BleServerActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter adapter;

    private String TAG = "BleServerActivity";
    public static final UUID UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000001"); //自定义UUID
    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000");
    public static final UUID UUID_DESC_NOTITY = UUID.fromString("11100000-0000-0000-0000-000000000000");
    public static final UUID UUID_CHAR_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000");
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser; // BLE广播
    private BluetoothGattServer mBluetoothGattServer; // BLE服务端

    private Button btnStartServer;
    private EditText edtInputAd,edtInput;
    private BluetoothDevice connectDevice;
    //添加可读+通知characteristic
    private BluetoothGattCharacteristic characteristicRead;
    private BluetoothGattCharacteristic characteristicWrite;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_server);
        btnStartServer = findViewById(R.id.btnStartServer);
        edtInputAd = findViewById(R.id.edtInputAd);
        edtInput = findViewById(R.id.edtInput);
        adapter = BluetoothAdapter.getDefaultAdapter();
        findViewById(R.id.btnSend).setOnClickListener(this);
        btnStartServer.setOnClickListener(this);
        adapter.setName("TestServer");
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnStartServer:
                startServer();
                break;
            case R.id.btnSend:
                send("");
                break;
        }
    }

    private void receve(BluetoothGattCharacteristic characteristic){
        byte[] values = characteristic.getValue();
        String string = new String(values);
        Toast.makeText(this,string,Toast.LENGTH_SHORT).show();
    }

    private void send(String text){
        BluetoothGattCharacteristic characteristic = mBluetoothGattServer.getService(UUID_SERVICE).getCharacteristic(UUID_DESC_NOTITY);
        characteristic.setValue("sssa".getBytes());
        mBluetoothGattServer.notifyCharacteristicChanged(connectDevice,characteristic,false);
    }

    private void startServer(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            // ============启动BLE蓝牙广播(广告) =================================================================================
            //广播设置(必须)
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
                    .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
                    .build();
            //广播数据(必须，广播启动就会发送)
            AdvertiseData advertiseData = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true) //包含蓝牙名称
                    .setIncludeTxPowerLevel(true) //包含发射功率级别
                    .addManufacturerData(1, new byte[]{23, 33}) //设备厂商数据，自定义
                    .build();
            //扫描响应数据(可选，当客户端扫描时才发送)
            AdvertiseData scanResponse = new AdvertiseData.Builder()
                    .addManufacturerData(2, new byte[]{66, 66}) //设备厂商数据，自定义
                    .addServiceUuid(new ParcelUuid(UUID_SERVICE)) //服务UUID
//                .addServiceData(new ParcelUuid(UUID_SERVICE), new byte[]{2}) //服务数据，自定义
                    .build();
            mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);

            // 注意：必须要开启可连接的BLE广播，其它设备才能发现并连接BLE服务端!
            // =============启动BLE蓝牙服务端=====================================================================================
            BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            characteristicRead = new BluetoothGattCharacteristic(UUID_CHAR_READ_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
            characteristicRead.addDescriptor(new BluetoothGattDescriptor(UUID_DESC_NOTITY, BluetoothGattCharacteristic.PERMISSION_WRITE));
            service.addCharacteristic(characteristicRead);


            //添加可写characteristic
            characteristicWrite = new BluetoothGattCharacteristic(UUID_CHAR_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            service.addCharacteristic(characteristicWrite);
            if (bluetoothManager != null){
                mBluetoothGattServer = bluetoothManager.openGattServer(this, mBluetoothGattServerCallback);
                mBluetoothGattServer.addService(service);
            }
        }
    }

    // BLE服务端Callback
    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            connectDevice = device;
            Log.i(TAG, String.format("onConnectionStateChange:name:%s,%s,%s,%s", device.getName(), device.getAddress(), status, newState));
            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), device));
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.i(TAG, String.format("onServiceAdded:%s,%s", status, service.getUuid()));
            logTv(String.format(status == 0 ? "添加服务[%s]成功" : "添加服务[%s]失败,错误码:" + status, service.getUuid()));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, String.format("onCharacteristicReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, characteristic.getUuid()));
            byte [] bytes = characteristic.getValue();
            String response = "回复_"; //模拟数据
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());// 响应客户端
            logTv("客户端读取Characteristic[" + characteristic.getUuid() + "]:\n" + response);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            // 获取客户端发过来的数据
            String requestStr = new String(requestBytes);
            Log.i(TAG, String.format("onCharacteristicWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
                    preparedWrite, responseNeeded, offset, requestStr));
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);// 响应客户端
            //receve(characteristic);
            //logTv("客户端写入Characteristic[" + characteristic.getUuid() + "]:\n" + requestStr);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.i(TAG, String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
            String v = Arrays.toString(descriptor.getValue());
            String response = "回复_" + v; //模拟数据
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes()); // 响应客户端
            logTv("客户端读取Descriptor[" + descriptor.getUuid() + "]:\n" + response);
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            // 获取客户端发过来的数据
            String valueStr = Arrays.toString(value);
            Log.i(TAG, String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, descriptor.getUuid(),
                    preparedWrite, responseNeeded, offset, valueStr));
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);// 响应客户端
            logTv("客户端写入Descriptor[" + descriptor.getUuid() + "]:\n" + valueStr);
            final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            characteristic.setValue("noty return".getBytes());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                }
            },1000);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.i(TAG, String.format("onExecuteWrite:%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, execute));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.i(TAG, String.format("onNotificationSent:%s,%s,%s", device.getName(), device.getAddress(), status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            Log.i(TAG, String.format("onMtuChanged:%s,%s,%s", device.getName(), device.getAddress(), mtu));
        }
    };


    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG,"AdvertiseCallback onStartSuccess:"+settingsInEffect.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.i(TAG,"AdvertiseCallback onStartFailure:"+errorCode);
        }
    };

    private void logTv(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                APP.toast(msg, 0);
            }
        });
    }


    private void stopServer(){
        if (mBluetoothLeAdvertiser != null){
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
        if (mBluetoothGattServer != null){
            mBluetoothGattServer.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }
}
