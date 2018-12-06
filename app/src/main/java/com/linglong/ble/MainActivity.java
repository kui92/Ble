package com.linglong.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 12;
    private BluetoothAdapter adapter;
    public static final int REQUEST_ENABLE = 1023;
    public static final String TAG = Constant.TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = BluetoothAdapter.getDefaultAdapter();
        checkPermissions();
    }


    public void click(View view){
        Intent intent = new Intent();
        switch (view.getId()){
            case R.id.check1:
                checkBleOpen();
            break;
            case R.id.server1:
                intent.setClass(this,ClassicBleActivity.class);
                intent.putExtra(ClassicBleActivity.IS_SERVER,true);
                startActivity(intent);
                break;
            case R.id.client1:
                intent.setClass(this,ClassicBleActivity.class);
                intent.putExtra(ClassicBleActivity.IS_SERVER,false);
                startActivity(intent);
                break;
            case R.id.bleServer:
                intent.setClass(this,BleServerActivity.class);
                intent.putExtra(ClassicBleActivity.IS_SERVER,false);
                startActivity(intent);
                break;
            case R.id.bleClient:
                intent.setClass(this,BleClientActivity.class);
                intent.putExtra(ClassicBleActivity.IS_SERVER,false);
                startActivity(intent);
                break;
        }
    }

    /**
     * 判断是否支持4.0 ble
     */
    private void isBle(){
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG, "支持BLE");
        } else {
            Log.i(TAG, "不支持BLE");
        }
    }


    /**
     * 检查权限
     */
    private void checkPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                //onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }


    private void checkBleOpen(){
        if (!adapter.isEnabled()) {
            openBle();
        }
    }

    private void openBle(){
        //弹出对话框提示用户是后打开
        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(enabler, REQUEST_ENABLE);
        //不做提示，强行打开，此方法需要权限&lt;uses-permissionandroid:name="android.permission.BLUETOOTH_ADMIN" /&gt;
         adapter.enable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE){
            Log.i(TAG,"onActivityResult resultCode:"+resultCode);
        }
    }
}
