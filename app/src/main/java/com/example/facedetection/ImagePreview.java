package com.example.facedetection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.graphics.BitmapCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.example.facedetection.LBP.printMatrix;
import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC;

public class ImagePreview extends AppCompatActivity implements View.OnClickListener {


    ImageView imageView;
    Button btnSend, btnBack;
    String imagePath;
    Uri imageUri;
    Bitmap orgImgBitmap;
    CascadeClassifier mJavaDetector;
    Mat mat;

    static {
        System.loadLibrary("opencv_java");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);


        imageView = findViewById(R.id.imagePreview);
        btnSend = findViewById(R.id.send);
        btnBack = findViewById(R.id.back);


        String imagePath = getIntent().getStringExtra("imagePath");

        Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show();

        imageUri = Uri.parse(imagePath);

        try {
            orgImgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int bitmapByteCount = BitmapCompat.getAllocationByteCount(orgImgBitmap);


        System.out.println("Original Image size " + bitmapByteCount);

        System.out.println("Image resoulution " + orgImgBitmap.getWidth() + " x " + orgImgBitmap.getHeight());

        //to test grey scale
        Bitmap copyBitmap1 = orgImgBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat orgImgMat1 = new Mat();
        Utils.bitmapToMat(copyBitmap1, orgImgMat1);
        Imgproc.cvtColor(orgImgMat1, orgImgMat1, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(orgImgMat1, copyBitmap1);
        int bitmapByteCount1 = BitmapCompat.getAllocationByteCount(copyBitmap1);
        System.out.print("Grayscale Image size " + bitmapByteCount1);


        imageView.setImageBitmap(orgImgBitmap);

        btnBack.setOnClickListener(this);
        btnSend.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.back: {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.send: {
                Mat orgImgMat;
                System.out.println("\nRunning FaceDetector");


                try {
                    /**
                     * Haarcascade classifier
                     */
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
                    FileOutputStream os = new FileOutputStream(mCascadeFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    os.close();


                    mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    if (mJavaDetector.empty()) {
                        Log.e("Fail", "Failed to load cascade classifier");
                        mJavaDetector = null;
                    } else
                        Log.i("Success", "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());


                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("error", "Failed to load cascade. Exception thrown: " + e);
                }

                Bitmap copyBitmap = orgImgBitmap.copy(Bitmap.Config.ARGB_8888, true);
                orgImgMat = new Mat();
                Utils.bitmapToMat(copyBitmap, orgImgMat);


                MatOfRect faceDetections = new MatOfRect();
                mJavaDetector.detectMultiScale(orgImgMat, faceDetections);
                System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));


                Rect[] facesArray = faceDetections.toArray();

                if (facesArray.length > 0) {
                    mat = new Mat();
                    Rect rect = facesArray[0];

                    mat = orgImgMat.submat(rect);

                    Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mat, bitmap);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imageBytes = baos.toByteArray();

//                    List<Byte> finalMatrix = new ArrayList<Byte>();
//                    for(byte d: imageBytes){
//                        finalMatrix.add(d);
//                    }



                    Sender sender= new Sender();
                    sender.execute(imageBytes);


//                    /**
//                     * Changing Mat from rgb to gray scale
//                     */
//                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY, 3);
//
//                    /*
//                     * Histogram Equalization
//                     */
//                    List<Mat> channels = new ArrayList<Mat>();
//                    Core.split(mat, channels);
//                    Imgproc.equalizeHist(channels.get(0), channels.get(0));
//                    Core.merge(channels, mat);
//

                    /**
                     * Converting mat to base64encoded string
                     */
//                    int cols = mat.cols();
//                    int rows = mat.rows();
//                    int elemSize = (int) mat.elemSize();
//
//                    byte[] data = new byte[cols * rows * elemSize];
//
//                    mat.get(0, 0, data);


                    /**
                     * ToDo
                     */
                    // We cannot set binary data to a json object, so:
                    // Encoding data byte array to Base64.
//                    String dataString = new String(Base64.encode(data, Base64.DEFAULT));
//                    System.out.println("Data Encoded String : " + dataString);

//                    List<Byte> finalMatrix = new ArrayList<Byte>();
//                    for(byte d: data){
//                        finalMatrix.add(d);
//                    }



                    /*
                     * Change Mat to Bitmap
                     */


//                    /**
//                     * Calculate LBP
//                     */
//                    int width = bitmap.getWidth();
//                    int height = bitmap.getHeight();
//                    double[][] lbpInputArray = new double[width][height];
//                    for (int i = 0; i < width; i++) {
//                        for (int j = 0; j < height; j++) {
//                            lbpInputArray[i][j] = (double) bitmap.getPixel(i, j);
//                        }
//                    }
//
//                    LBP lbp = new LBP(8, 1);
//
//                    byte[][] resultLBP = lbp.getLBP(lbpInputArray);
//
//                    System.out.println("VarianceImage");
//                    printMatrix(resultLBP);
//
//                    double[] histogramArray = changeOneDimension(resultLBP);
//
//                    System.out.println("HistogramArray");
//                    printMatrix(histogramArray);
//
//                    double[] normalizedHistogram = lbp.histc(histogramArray);
//                    System.out.println("Normalized histogram");
//                    printMatrix(normalizedHistogram);
//
//                    List<Double> finalMatrix = new ArrayList<Double>();
//                    for(double d: normalizedHistogram){
//                        finalMatrix.add(d);
//                    }
//
//                    for(int i=0; i< finalMatrix.size(); i++){
//                        System.out.print("List" + finalMatrix.get(i));
//                    }
//
//                    Sender sender =new Sender();
//                    sender.execute(finalMatrix);


                    imageView.setImageBitmap(bitmap);
                    btnSend.setVisibility(View.INVISIBLE);
                }
                break;
            }
        }

    }

    public double[] changeOneDimension(byte[][] arr) {
        List<Double> list = new ArrayList<Double>();
        for (int i = 0; i < arr.length; i++) {
            // tiny change 1: proper dimensions
            for (int j = 0; j < arr[i].length; j++) {
                // tiny change 2: actually store the values
                list.add((double) arr[i][j]);
            }
        }

        // now you need to find a mode in the list.

        // tiny change 3, if you definitely need an array
        double[] vector = new double[list.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = list.get(i);
        }

        return vector;
    }
}

class MyTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
