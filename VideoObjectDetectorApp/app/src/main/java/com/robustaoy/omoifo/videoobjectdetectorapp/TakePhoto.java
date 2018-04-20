package com.robustaoy.omoifo.videoobjectdetectorapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.hardware.camera2.CameraManager.*;
import static com.robustaoy.omoifo.videoobjectdetectorapp.ChoosePhoto.*;
import static com.robustaoy.omoifo.videoobjectdetectorapp.ChoosePhoto.PHOTO_REQUEST_CODE;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class TakePhoto extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {
    String mCurrentPhotoPath;
    final static String TAG = "TAKE_PHOTO";
    protected CameraManager cameraManager;
    static  final  int SNAP_REQUEST_CODE = 1;
    protected File photoFile;
    static Uri photoURI = null;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_display);
        takeAndSavePic();
        addPic2Gallery();
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void addPic2Gallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void takeAndSavePic() {
        //ACTION_IMAGE_CAPTURE
        Intent takePictureIntent = new Intent(MediaStore.
                ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
         if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                System.err.println(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                 photoURI = FileProvider.getUriForFile(this,
                        "com.robustaoy.omoifo.videoobjectdetectorapp.android.fileprovider",
                        photoFile);
                Log.i("URL PATH",photoFile.toString());
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                Log.i("START ACTIVITY CAPTURE",photoFile.getName());
                startActivityForResult(takePictureIntent, SNAP_REQUEST_CODE);
            }else Log.i("NULL PATH","Photo path was NULL");
        }else Log.i("NULL PKGMGR","NULL");
    }

    private  Mat processFrame(Mat frame){
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
                String label = classNames[classId] + ": " + String.format("%.2f",confidence*100.00)+"%";
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 1.5, 4, baseLine);
                // Draw background for label.
                Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom - labelSize.height),
                        new Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0]),
                        new Scalar(255, 255, 255), Core.FILLED);
                // Write class name and confidence.
                Imgproc.putText(subFrame, label, new Point(xLeftBottom, yLeftBottom),
                        Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0, 0, 0));
            }
        }
        return frame;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == SNAP_REQUEST_CODE &&  data != null) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK  && data.getData() != null) {
                Log.i("GOT IT: ","Image Received");
                Uri photo = data.getData();
                try {
                    initNet();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photo);
                    System.out.println(String.format("BITMAP Image is H: %d, W:%d", bitmap.getHeight(),bitmap.getWidth()));
                    Mat mat = new Mat();
                    Utils.bitmapToMat(bitmap,mat);
                    mat = processFrame(mat);
                    Utils.matToBitmap(mat,bitmap);
                    showImage(bitmap);
                    FileOutputStream fos = new FileOutputStream(photoFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                    fos.flush();
                    fos.close();
                    //addPic2Gallery();
                    System.out.println("Image Written TO FILE");
                }catch (IOException e){
                    System.err.println(e.getMessage());
                }
            }else Log.i("DATA HAD","RsC: "+resultCode+", Data: "+data.toString()+", Nothing it seems!!!");
        }else Log.d("WRONG: ","No Data RECEIVED!!!, RC:"+requestCode+", RsC: "+resultCode);
    }



    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        /*Environment.DIRECTORY_PICTURES*/
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",  // suffix
                storageDir      // directory
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.i("NEW IMAGE PATH","P:"+mCurrentPhotoPath+", PF:"+storageDir.getAbsolutePath());
        return image;
    }

    public void showImage(Bitmap photo){
        // Get the dimensions of the View
        TouchImageView mImageView =  findViewById(R.id.photoView);
        mImageView.setImageBitmap(photo);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

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

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return null;
    }
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    private Net net;
}
