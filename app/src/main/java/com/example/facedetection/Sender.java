package com.example.facedetection;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Sender extends AsyncTask<String, Void, Void> {
    Socket s;
    DataOutputStream dataOutputStream;


    @Override
    protected Void doInBackground(String... voids) {

        String imageData= voids[0];
        try{
            s=new Socket("192.168.99.173", 6666);
//            pw = new PrintWriter(s.getOutputStream());
//            pw.write(imageData);
//            pw.flush();
//            pw.close();
            dataOutputStream = new DataOutputStream(s.getOutputStream());
            dataOutputStream.writeUTF(imageData);
            dataOutputStream.flush();
            dataOutputStream.close();
            s.close();

        }catch (Exception e){

        }


        return null;
    }
}
