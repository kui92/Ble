package com.linglong.ble;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author : kui
 * date   : 2018/11/30  14:02
 * desc   :
 * version: 1.0
 */
public class WriteThread extends HandlerThread {
    public static final String TAG = "writeTag";
    private Handler writeHadler;
    private BluetoothSocket socket;
    private DataOutputStream dataOutputStream;
    public WriteThread(String name,BluetoothSocket socket) {
        super(name);
        this.socket = socket;
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isInterrupted() {
        return super.isInterrupted();
    }

    @Override
    public void interrupt() {
        super.interrupt();
    }

    public void startWrite(String string){
        if (!isAlive()){
            start();
        }
        if (writeHadler==null){
            writeHadler = new Handler(getLooper()){
                @Override
                public void handleMessage(Message msg) {
                    write((String) msg.obj);
                }
            };
        }
        Message message = Message.obtain();
        message.obj = string;
        writeHadler.sendMessage(message);
    }

    private void write(String msg){
        Log.i(TAG,"write msg:"+msg);
        try {
            dataOutputStream.writeUTF(msg);
            dataOutputStream.flush();
        }catch (Exception e){
            Log.i(TAG,"write Exception:"+e.getMessage());
        }

    }

    @Override
    public boolean quit() {
        relase();
        return super.quit();
    }

    private void relase(){
        if (dataOutputStream!=null){
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataOutputStream = null;
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
