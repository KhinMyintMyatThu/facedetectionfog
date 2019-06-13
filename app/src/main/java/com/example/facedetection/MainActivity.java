package com.example.facedetection;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.opencv.android.OpenCVLoader;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout takeEvidence;
    Dialog dialog;
    public String dir, currentPhotoPath;
    public Uri photoURI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/facedetection/";
        File newdir = new File(dir);
        newdir.mkdirs();

        if (OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "OpenCV loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Could nod load OpenCv", Toast.LENGTH_SHORT).show();
        }

        takeEvidence = (LinearLayout) findViewById(R.id.btnTakeEvidence);
        takeEvidence.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnTakeEvidence:
                dialog = new Dialog(this);
                showPopUp();
        }

    }

    public void showPopUp() {
        dialog.setContentView(R.layout.dialog);
        ImageButton capturePhoto = dialog.findViewById(R.id.capturePhoto);
        ImageButton fromFiles = dialog.findViewById(R.id.files);
        Button cancel = dialog.findViewById(R.id.cancel);
        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePicture.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(getApplicationContext(), "com.example.facedetection", photoFile);
                        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        dialog.dismiss();
                        startActivityForResult(takePicture, 0);
                    }
                }

            }
        });

        fromFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                dialog.dismiss();
                startActivityForResult(pickPhoto, 1);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent(this, ImagePreview.class);
                    intent.putExtra("imagePath", photoURI.toString());
                    dialog.dismiss();
                    startActivity(intent);
                    finish();
                 }
                break;
            case 1:
                if (resultCode == RESULT_OK && imageReturnedIntent != null) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    Intent intent = new Intent(this, ImagePreview.class);
                    intent.putExtra("imagePath", selectedImage.toString());
                    dialog.dismiss();
                    startActivity(intent);
                    finish();
                }
                break;
        }
    }
}
