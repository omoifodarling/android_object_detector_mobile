package com.robustaoy.omoifo.videoobjectdetectorapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import  com.robustaoy.omoifo.videoobjectdetectorapp.TouchImageView;

public class ChoosePhoto extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    protected static final  int PHOTO_REQUEST_CODE = 1;

    //private String mCurrentPhotoPath;
    Button gallery, takePhoto;
    static  final String TAG = "CHOOSE_PHOTO";

    private Mat mRgba, blob;
    private Bitmap bitmap;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_photo);
        gallery = findViewById(R.id.useGallery);
        takePhoto = findViewById(R.id.takePhoto);
        assert (gallery!=null && takePhoto!=null);

        mOpenCvCameraView = findViewById(R.id.showPhotoCamView);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.photo_display);
                selectFromGallery();
                //selectPicture();
            }
        });
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //setContentView(R.layout.activity_choose_photo);
                Intent intent = new Intent(ChoosePhoto.this,TakePhoto.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle b){
        super.onSaveInstanceState(b);

    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void selectFromGallery() {
        //Intent selectIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent newIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        newIntent.setType("image/*");
        newIntent.setAction(Intent.ACTION_GET_CONTENT);
        if (newIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(newIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    protected void selectPicture(){
        Intent newIntent = new Intent(Intent.ACTION_GET_CONTENT);
        //newIntent.setType("image/*");
        newIntent.addCategory(Environment.DIRECTORY_PICTURES);
        //newIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(newIntent.createChooser(newIntent,"Select Picture"),PHOTO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PHOTO_REQUEST_CODE && data != null) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK  && data.getData() != null) {
                Uri photo = data.getData();
               try {
                   //Uri imageUri = data.getData();
                   initNet();
                   bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photo);
                   //bitmap = BitmapFactory.decodeFile(photo.getPath());
                   Log.i("DATA URL IN",photo+", bitmap: "+bitmap.getByteCount());
                   //Bundle extras = data.getExtras();
                   //Bitmap imageBitmap = (Bitmap) extras.get("data");
                   TouchImageView iv = findViewById(R.id.photoView);
                   Log.i("CHOOSE VIEW","H: "+iv.getHeight()+" W:"+iv.getWidth());
                   Mat mat = new Mat();
                   //Mat mat = new Mat();
                   Utils.bitmapToMat(bitmap,mat);
                   mat = processFrame(mat);
                   Utils.matToBitmap(mat,bitmap);
                   iv.setImageBitmap(bitmap);
               } catch (IOException|IllegalStateException |IllegalArgumentException | ArrayIndexOutOfBoundsException e){
                   System.err.println(e.getMessage());
               }
            }else Log.i("DATA HAD","Nothing it seems!!!");

        }else Log.d("WRONG: ","No Data RECEIVED!!!");

        super.onActivityResult(requestCode,resultCode,data);
    }
private void initNet() {
    String proto = getPath("MobileNetSSD_deploy.prototxt", this);
    String weights = getPath("MobileNetSSD_deploy.caffemodel", this);
    if (proto == null || weights == null) {
        System.out.println("ERROR IN GETTING PATH!!!");
        Log.i(TAG, "ERROR IN GETTING PATH!!!");
    } else {
        net = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Network loaded successfully");
    }
}

    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            System.out.println("INPUT READ!!!");
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            System.out.println("OUTPUT FILE READ!!!");
            FileOutputStream os = new FileOutputStream(outFile);
            System.out.println("OUTPUT_STREAM CREATED!!!");
            os.write(data);
            System.out.println("OUTPUT_STREAM WRITTEN!!!");
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file "+file);
        }
        return "";
    }


    private Mat processFrame(Mat frame){
        final int IN_WIDTH = 300;//mOpenCvCameraView.getWidth();
        final int IN_HEIGHT = 300;// mOpenCvCameraView.getHeight();
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.2;
        //process the frame
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        // Forward image through network.
        blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), true, false);
        net.setInput(blob);
        Mat detections = net.forward();
        if (detections == null){ Log.i("NULL NET","Network was NULL");return frame;}
        int cols = frame.cols();
        int rows = frame.rows();
        Size cropSize;
        if ((float)cols / rows > WH_RATIO) {
            cropSize = new Size(rows * WH_RATIO, rows);
        } else {
            cropSize = new Size(cols, cols / WH_RATIO);
        }
        int y1 = (int)(rows - cropSize.height) / 2;
        int y2 = (int)(y1 + cropSize.height);
        int x1 = (int)(cols - cropSize.width) / 2;
        int x2 = (int)(x1 + cropSize.width);
        Mat subFrame = frame.submat(y1, y2, x1, x2);
        cols = subFrame.cols();
        rows = subFrame.rows();
        detections = detections.reshape(1, (int)detections.total() / 7);
        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                int classId = (int)detections.get(i, 1)[0];
                int xLeftBottom = (int)(detections.get(i, 3)[0] * cols);
                int yLeftBottom = (int)(detections.get(i, 4)[0] * rows);
                int xRightTop   = (int)(detections.get(i, 5)[0] * cols);
                int yRightTop   = (int)(detections.get(i, 6)[0] * rows);
                // Draw rectangle around detected object.
                Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom),
                        new Point(xRightTop, yRightTop),
                        new Scalar(0, 255, 0));
                String label = classNames[classId] + ": " + String.format("%.2f%s",confidence*100.00,"%");
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, .8, 1, baseLine);
                // Draw background for label.
                Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom - labelSize.height),
                        new Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0]),
                        new Scalar(255, 255, 255), Core.FILLED);
                // Write class name and confidence.
                Imgproc.putText(subFrame, label, new Point(xLeftBottom, yLeftBottom),
                        Core.FONT_HERSHEY_SIMPLEX, .8, new Scalar(255, 0, 3));
            }
        }
        return frame;
    }
    public void showImage(Uri  filePath){
        // Get the dimensions of the View
        Log.d("IMAGE URL Before:",filePath.toString());
        Mat rawImage = Imgcodecs.imread(filePath.toString());
        Log.d("IMAGE URL",filePath.toString());
        assert (rawImage!= null);
        ImageView mImageView =  findViewById(R.id.showPhotoCamView);
        int height = mImageView.getHeight();
        int width = mImageView.getWidth();
        Mat image = processFrame(rawImage);
        Bitmap bmp = captureBitmap(image);
        mImageView.setImageBitmap(bmp);
        //Bitmap image = BitmapFactory.decodeFile(filePath.getPath(),bmOptions);
    }

    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    private Net net;

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width, CvType.CV_8UC4);
    }

    private Bitmap captureBitmap(Mat mat){
        try{
            bitmap = Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat,bitmap);
            return bitmap;
        }catch(IllegalArgumentException | IllegalStateException | ArrayIndexOutOfBoundsException
                e){
            System.err.println("Error: "+e.getMessage());
        }
        return null;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return processFrame(inputFrame.rgba());
    }

    //@Override
    public Mat onCameraFrame(Mat inputFrame) {
        inputFrame.copyTo(mRgba);
        return mRgba;
    }

    private CameraBridgeViewBase mOpenCvCameraView;
}
