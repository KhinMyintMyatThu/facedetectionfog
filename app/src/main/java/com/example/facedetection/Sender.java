package com.example.facedetection;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class Sender extends AsyncTask<byte[], Void, Void> {
    Socket s;
 //   ObjectOutputStream objectOutputStream;
   DataOutputStream dataOutputStream;
//    PrintWriter pw;


    @Override
    protected Void doInBackground(byte[]... voids) {
        System.out.print("win tl");
        //List<Double> imageData= voids[0];
        byte[] imageData= voids[0];
        System.out.print("hello "+imageData.length);


        try{
            s=new Socket("192.168.10.111", 6667);


            DataOutputStream dOut = new DataOutputStream(s.getOutputStream());

            dOut.writeInt(imageData.length); // write length of the message
            dOut.write(imageData);           // write the message


        }catch (Exception e){

        }


        return null;
    }
}
