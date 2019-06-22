package com.example.facedetection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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

import static com.example.facedetection.LBP.printMatrix;
import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;

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

                    mat = new Mat();

                  //  Mat grayscaleMat=new Mat();

                    System.out.println("RGB mat " + mat);

                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY); //Changing Mat from rgb to grayscale

                    System.out.println("Grayscale mat " + mat);

//                    Mat grayscaleMat=new Mat();
//                    grayscaleMat.convertTo(mat, CV_8UC3);
//
//                    System.out.println("3 channel grayscle mat"+ grayscaleMat);

                    /*
                     *JavaCv matvector
                     */
                    System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0");
                    System.setProperty("org.bytedeco.javacpp.maxbytes", "0");
                    MatVector images = new MatVector(facesArray.length);
                    org.bytedeco.opencv.opencv_core.Mat labels = new org.bytedeco.opencv.opencv_core.Mat(facesArray.length, 1, CV_32SC1);



                    /*
                     * Histogram Equalization
                     */
                    List<Mat> channels = new ArrayList<Mat>();
                    Core.split(mat, channels);
                    Imgproc.equalizeHist(channels.get(0), channels.get(0));
                    Core.merge(channels, mat);

                    System.out.println("Histogram mat " + mat);

                    /**
                     * Convert mat to two dimensional array
                     */
                    double[][] matArray = ImagePreview.getMultiChannelArray(mat);
                    System.out.println(Arrays.deepToString(matArray));

                    LBP lbp = new LBP(1, 1);

                    System.out.println("Neighbourhood");
                    printMatrix(lbp.neighbourhood);
                    byte[][] resultLBP = lbp.getLBP(matArray);

                    System.out.println("VarianceImage");
                    printMatrix(resultLBP);



                    /*
                     *Convert Opencv mat to javacv mat
                     */
                    org.bytedeco.opencv.opencv_core.Mat mat2 = new org.bytedeco.opencv.opencv_core.Mat((Pointer) null) {
                        {
                            address = mat.getNativeObjAddr();
                        }
                    };
                    images.put(mat2);

                    System.out.print("Java cv mat " + images.get(0));




                    /**
                     * LBPHFaceRecognizer
                     */
                    LBPHFaceRecognizer faceRecognizer = LBPHFaceRecognizer.create();
                    faceRecognizer.train(images, labels);


                    /**
                     * MatVector and mat data from facerecognizer
                     */

                    MatVector matVector= faceRecognizer.getHistograms();


                    org.bytedeco.opencv.opencv_core.Mat faceRecognizerMat= new org.bytedeco.opencv.opencv_core.Mat();

                    faceRecognizerMat= matVector.get(0);



                    String faceRecognizerString = matVector.toString();
                    System.out.println("Face Recognizer String " + faceRecognizerString);

                    System.out.println("face Recognizer mat col " + faceRecognizerMat.col(0));

                    org.bytedeco.opencv.opencv_core.Mat mat3 = faceRecognizerMat.row(0);
                    org.bytedeco.opencv.opencv_core.Mat mat4 = faceRecognizerMat.col(0);


                    /**
                     * Convert javacv mat to opencv mat
                     */
                    Mat faceRecognizerMat2= new Mat(faceRecognizerMat.rows(),faceRecognizerMat.cols(), CvType.CV_8UC1);

                    double[][] facerecogmatArray = ImagePreview.getMultiChannelArray(faceRecognizerMat2);
                    System.out.println(Arrays.deepToString(facerecogmatArray));

                    Mat faceRecognizerMat3 = new Mat(mat3.rows(), mat3.cols(), CvType.CV_8UC1);
                    Mat faceRecognizerMat4 = new Mat(mat4.rows(), mat4.cols(), CvType.CV_8UC1);
                    System.out.println("Face Recognizer Mat 2" + faceRecognizerMat2);
                    System.out.println("Face Recognizer Mat 3" + faceRecognizerMat3);
                    System.out.println("Face Recognizer Mat 4" + faceRecognizerMat4);


                    /**
                     * Mat to byteArray
                     */

//                    /**
//                     * ToDo test to show image and delete it later
//                     */
//                    mat= faceRecognizerMat2;

                    /**
                     * Convert javacv mat to bitmap
                     */
//                    AndroidFrameConverter convertToBitmap = new AndroidFrameConverter();
//                    OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
//                    Frame frame = converterToMat.convert(faceRecognizerMat);
//                    Bitmap javaCvBitmap = convertToBitmap.convert(frame);

                    /**
                     * Converting mat to base64encoded string
                     */
                    int cols = mat.cols();
                    int rows = mat.rows();
                    int elemSize = (int) mat.elemSize();

                    byte[] data = new byte[cols * rows * elemSize];

                    mat.get(0, 0, data);

                    System.out.println("Data String : " + data);

                    // We cannot set binary data to a json object, so:
                    // Encoding data byte array to Base64.
                    String dataString = new String(Base64.encode(data, Base64.DEFAULT));
                    System.out.println("Data Encoded String : " + dataString);

                    Sender sender= new Sender();
                    sender.execute(dataString);


                    /*
                     * Change Mat to Bitmap
                     */
                    Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mat, bitmap);
                    System.out.println("Image resoulution " + bitmap.getWidth() + " x " + bitmap.getHeight());

                    int bitmapByteCount = BitmapCompat.getAllocationByteCount(bitmap);

                    System.out.print("Cropped Image size " + bitmapByteCount);

                    imageView.setImageBitmap(bitmap);
                }
                break;
            }
        }

    }

    public static double[][] getMultiChannelArray(Mat m) {
        //first index is pixel, second index is channel
        int numChannels = m.channels();//is 3 for 8UC3 (e.g. RGB)
        int frameSize = m.rows() * m.cols();
        byte[] byteBuffer = new byte[frameSize * numChannels];
        m.get(0, 0, byteBuffer);

        System.out.print("number of channels " + numChannels);

        //write to separate R,G,B arrays
        double[][] out = new double[frameSize][numChannels];
        for (int p = 0, i = 0; p < frameSize; p++) {
            for (int n = 0; n < numChannels; n++, i++) {
                out[p][n] = byteBuffer[i];
            }
        }
        return out;
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
