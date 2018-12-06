package com.linglong.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author : kui
 * date   : 2018/12/3  17:02
 * desc   :
 * version: 1.0
 */
public class BleClientActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "BleClientActivity";
    public static final UUID UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000001"); //自定义UUID
    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000");
    public static final UUID UUID_DESC_NOTITY = UUID.fromString("11100000-0000-0000-0000-000000000000");
    public static final UUID UUID_CHAR_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000");

    private Button btnStartServer,btnSend;
    private EditText edtInputAd,edtInput;
    private BluetoothAdapter adapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    private TextView tvText;

    public static final int CONNECT = 1;

    private Handler handler= new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CONNECT:
                    startClient((BluetoothDevice) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_client);
        adapter = BluetoothAdapter.getDefaultAdapter();
        btnStartServer = findViewById(R.id.btnStartServer);
        edtInputAd = findViewById(R.id.edtInputAd);
        edtInput = findViewById(R.id.edtInput);
        btnSend =findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
        btnStartServer.setOnClickListener(this);
        tvText = findViewById(R.id.tvText);
        findViewById(R.id.btnReq).setOnClickListener(this);
        findViewById(R.id.btnNoty).setOnClickListener(this);
        findViewById(R.id.btnLogServer).setOnClickListener(this);
        adapter.setName("TestClient");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnStartServer:
                startScan();
                break;
            case R.id.btnSend:
                String text = edtInput.getText().toString().trim();
                write(text);
                break;
            case R.id.btnReq:
                read();
                break;
            case R.id.btnNoty:
                setNotify();
                break;
            case R.id.btnLogServer:
                logService(mBluetoothGatt);
                break;
        }
    }

    // 设置通知Characteristic变化会回调->onCharacteristicChanged()
    public void setNotify() {
        BluetoothGattService service = mBluetoothGatt.getService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            // 设置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);

            // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleServerActivity.UUID_DESC_NOTITY);
            // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//和通知类似,但服务端不主动发数据,只指示客户端读取数据
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    private void logService(BluetoothGatt gatt){
        if (gatt==null){
            return;
        }
        for (BluetoothGattService service : gatt.getServices()) {
            if (UUID_SERVICE.equals(service.getUuid())){
                Log.i(TAG,"UUID_SERVICE:"+UUID_SERVICE.toString());
                Log.i(TAG,"UUID_CHAR_READ_NOTIFY:"+UUID_CHAR_READ_NOTIFY.toString());
                Log.i(TAG,"UUID_DESC_NOTITY:"+UUID_DESC_NOTITY.toString());
                Log.i(TAG,"UUID_CHAR_WRITE:"+UUID_CHAR_WRITE.toString());
                List<BluetoothGattService> services = service.getIncludedServices();
                for (BluetoothGattService service1:services){
                    Log.i(TAG,"service1:"+service1.getUuid().toString());
                }
            }
           /* StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + service.getUuid().toString());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                allUUIDs.append(",\nC=").append(characteristic.getUuid());
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors())
                    allUUIDs.append(",\nD=").append(descriptor.getUuid());
            }
            allUUIDs.append("}");
            Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());*/
        }
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 读取数据成功会回调->onCharacteristicChanged()
    public void read() {
        BluetoothGattService service = mBluetoothGatt.getService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可读的Characteristic
            characteristic.setValue("readas".getBytes());
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESC_NOTITY);
            descriptor.setValue("readas".getBytes());
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 写入数据成功会回调->onCharacteristicWrite()
    public void write(String text) {
        if (TextUtils.isEmpty(text)){
            text = "asad";
        }
        BluetoothGattService service = mBluetoothGatt.getService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_WRITE);//通过UUID获取可写的Characteristic
            characteristic.setValue(text.getBytes()); //单次最多20个字节
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    private void receve(BluetoothGattCharacteristic characteristic){
        if (characteristic==null){
            return;
        }
        byte[] da = characteristic.getValue();
        String text = new String(da);
        tvText.setText(text);
    }

    private void startScan(){
        /*if (adapter!=null){
            UUID[] serviceUuids = new UUID[1];
            serviceUuids[0] = UUID_SERVICE;
            adapter.startLeScan(serviceUuids,mLeScanCallback);
        }*/
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if (bluetoothLeScanner==null){
                bluetoothLeScanner = adapter.getBluetoothLeScanner();
            }
            ParcelUuid parcelUuid = new ParcelUuid(UUID_SERVICE);
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(parcelUuid)
                    .build();
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .build();
            List<ScanFilter> scanFilters = new ArrayList<>();
            scanFilters.add(filter);
            bluetoothLeScanner.startScan(scanFilters,scanSettings,scanCallback);
        }
    }


    private void stopScan(){
        if (bluetoothLeScanner!=null){
            bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner = null;
        }
        if (adapter!=null){
           adapter.stopLeScan(mLeScanCallback);
        }
    }

    private void startClient(BluetoothDevice device){
        if (device == null){
            return;
        }
        closeConn();
        mBluetoothGatt = device.connectGatt(this,false,mBluetoothGattCallback);
    }

    // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133
    private void closeConn() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }

    private void taost(String text){
        if (TextUtils.isEmpty(text)){
            Toast.makeText(this,text,Toast.LENGTH_SHORT).show();
        }
    }

    // 与服务端连接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "连接成功");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gatt.discoverServices(); //启动服务发现
                    }
                },2000);
            } else {
                Log.i(TAG, "连接不成功");
                closeConn();
            }
            //logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), dev));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            for (BluetoothGattService service : gatt.getServices()) {
                StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + service.getUuid().toString());
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    allUUIDs.append(",\nC=").append(characteristic.getUuid());
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors())
                        allUUIDs.append(",\nD=").append(descriptor.getUuid());
                }
                allUUIDs.append("}");
                Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            //logTv("写入Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));
            //logTv("通知Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            //logTv("读取Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            //logTv("写入Descriptor[" + uuid + "]:\n" + valueStr);
        }
    };


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            stopScan();
            Message message = Message.obtain();
            message.what = CONNECT;
            message.obj = result.getDevice();
            handler.sendMessageDelayed(message,1000);
            Log.i(TAG,"onScanResult:"+result.getDevice().getName());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i(TAG,"onBatchScanResults:"+results.size());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG,"onScanFailed:"+errorCode);
        }
    };


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        //当搜索到一个设备，这里就会回调,注意这里回调到的是子线程。
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            //在这里可以把搜索到的设备保存起来0
            //device.getName();获取蓝牙设备名字
            //device.getAddress();获取蓝牙设备mac地址。
            //这里的rssi即信号强度，即手机与设备之间的信号强度。
            //注意，这里不是搜索到1个设备后就只回调一次这个设备，可能过个几秒又搜索到了这个设备，还会回调的
            //所以，这里可以实时刷新设备的信号强度rssi,但是保存的时候就只保存一次就行了。
            Log.i(TAG, device.getName() + "");
            Log.i(TAG, device.getAddress() + "");
            Log.i(TAG, "信号:" + rssi);
            readAdMsg(scanRecord);
        }

    };

    private void readAdMsg(byte[] scanRecord){
        try{
            ParsedAd parsedAd = parseData(scanRecord);
            int i = 0;
        }catch (Exception e){
            Log.i(TAG,"Exception:"+e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        closeConn();
    }


    public class ParsedAd{
        byte flags;
        List<UUID> uuids = new ArrayList<>();
        String localName;
        short manufacturer;
    }
    public  ParsedAd parseData(byte[] adv_data) {
        ParsedAd parsedAd = new ParsedAd();
        ByteBuffer buffer = ByteBuffer.wrap(adv_data).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0)
                break;
            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    parsedAd.flags = buffer.get();
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getInt())));
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        parsedAd.uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    parsedAd.localName = new String(sb).trim();
                    break;
                case (byte) 0xFF: // Manufacturer Specific Data
                    parsedAd.manufacturer = buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                buffer.position(buffer.position() + length);
            }
        }
        return parsedAd;
    }

}
