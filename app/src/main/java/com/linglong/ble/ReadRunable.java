package com.linglong.ble;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author : kui
 * date   : 2018/11/30  11:23
 * desc   :
 * version: 1.0
 */
public class ReadRunable implements Runnable {
    public static final String TAG = "ReadTag";
    private BluetoothSocket socket;
    private DataInputStream dataInputStream;

    public ReadRunable(BluetoothSocket socket){
        init(socket);
    }

    public void init(BluetoothSocket socket){
        this.socket = socket;
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true){
                String text = dataInputStream.readUTF();
                Log.i(TAG,"读取到内容:"+text);
            }
        }catch (Exception e){
            Log.i(TAG,"run Exception:"+e.getMessage());
        }finally {
            Log.i(TAG,"结束读取:" );
            relase();
        }
    }

    public void relase(){
        Log.i(TAG,"释放:" );
        try {
            if (dataInputStream!=null){
                dataInputStream.close();
                dataInputStream = null;
            }
            if (socket!=null){
                socket.close();
                socket = null;
            }
        }catch (Exception e){
            Log.i(TAG,"relase Exception:"+e.getMessage());
        }
    }
}
