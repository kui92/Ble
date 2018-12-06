package com.linglong.ble;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * @author : kui
 * date   : 2018/11/30  13:50
 * desc   :
 * version: 1.0
 */
public class ReadThread extends HandlerThread {

    public static final String TAG = "ReadTag";
    private Handler readHandler;
    private ReadRunable readRunable;

    public ReadThread(String name) {
        super(name);
    }


    public void startRead(BluetoothSocket socket){
        if (!isAlive()){
            start();
        }
        if (readHandler==null){
            readHandler = new Handler(getLooper());
        }
        if (readRunable == null){
            readRunable = new ReadRunable(socket);
        }
        readHandler.post(readRunable);
    }

    @Override
    public boolean isInterrupted() {
        return super.isInterrupted();
    }

    @Override
    public boolean quit() {
        if (readRunable!=null){
            readRunable.relase();
            readRunable = null;
        }
        return super.quit();
    }

    @Override
    public void interrupt() {
        if (readRunable!=null){
            readRunable.relase();
            readRunable = null;
        }
        super.interrupt();
    }
}
