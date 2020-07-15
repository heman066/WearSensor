package com.example.wearsensor;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;

public class ClientTask implements Runnable {
    private String IP;
    private int PORT;
    private Socket socket;
    private DatagramPacket info;
    byte[] b = null;

    public ClientTask(String IP, int PORT) {
        this.IP = IP;
        this.PORT = PORT;
    }

    @Override
    public void run() {
        try {
            Log.e("Client","Socket opened");
            File f = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Recorded Data");
            File[] files = f.listFiles();
            socket = new Socket(IP,PORT);
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeInt(files.length);

            for(File file : files)
            {
                long length = file.length();
                dos.writeLong(length);

                String name = file.getName();
                dos.writeUTF(name);

                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);

                int theByte = 0;
                while((theByte = bis.read()) != -1) bos.write(theByte);

                FileUtils.forceDelete(file);
                bis.close();
            }

            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket!=null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
