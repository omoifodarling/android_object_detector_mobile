package com.robustaoy.omoifo.videoobjectdetectorapp;

import android.content.Context;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class FileVideoStream extends AppCompatActivity {
private static int framesCount = 0;
    VideoView vidView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_video_stream);
        vidView = findViewById(R.id.myVideo);
        vidView.setVisibility(View.VISIBLE);
    }

    private void streamFile(VideoView vidView){
        String vidAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
        Uri vidUri = Uri.parse(vidAddress);
        vidView.setVideoURI(vidUri);
        vidView.start();

    }

    private class MyScreen extends JavaCameraView {

        public MyScreen(Context context, int cameraId) {
            super(context, cameraId);
        }

        public MyScreen(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected boolean initializeCamera(int width, int height) {
            return super.initializeCamera(width, height);
        }

        @Override
        protected void releaseCamera() {
            super.releaseCamera();
        }

        @Override
        protected synchronized void deliverAndDrawFrame(CvCameraViewFrame frame) {
            super.deliverAndDrawFrame(frame);

        }

        protected void processFrame(){

        }

        protected synchronized void drawFrame(Mat frame){
            FileVideoStream.framesCount++;
        }

        @Override
        protected boolean connectCamera(int width, int height) {
            return super.connectCamera(width, height);
        }

        @Override
        public void onPreviewFrame(byte[] frame, Camera arg1) {
            super.onPreviewFrame(frame, arg1);
        }

    }

    private class DNNNetworker extends Thread{
        Net net;
        Mat frame;
        MyScreen screen;
        VideoCapture stream;

        public DNNNetworker(ThreadGroup group, String name,MyScreen screen,  Net net, VideoCapture cap) {
            super(group, name);
            this.net = net;
            this.stream = cap;
            this.screen = screen;
        }

        private Mat processFrame(Mat fr){
            final int IN_WIDTH = 300;
            final int IN_HEIGHT = 300;
            final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
            final double IN_SCALE_FACTOR = 0.007843;
            final double MEAN_VAL = 127.5;
            final double THRESHOLD = 0.2;
            //process the frame
            Mat frame = fr;
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
            // Forward image through network.
            Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                    new Size(IN_WIDTH, IN_HEIGHT),
                    new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), true,false);
            net.setInput(blob);
            Mat detections = net.forward();
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
                    String label = classNames[classId] + ": " + String.format("%.2f%",confidence*100.00);
                    int[] baseLine = new int[1];
                    Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
                    // Draw background for label.
                    Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom - labelSize.height),
                            new Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0]),
                            new Scalar(255, 255, 255), Core.FILLED);
                    // Write class name and confidence.
                    Imgproc.putText(subFrame, label, new Point(xLeftBottom, yLeftBottom),
                            Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
                }
            }
            return frame;
        }

        @Override
        public void run() {
            super.run();
            int lastFrame = framesCount;
            while (framesCount < 108000){
                try {
                    stream.read(frame);
                    frame = this.processFrame(frame);
                    while (lastFrame == framesCount)
                        this.wait();
                }catch (InterruptedException i){}
                screen.drawFrame(frame);
                notifyAll();
            }
        }

        private static final String TAG = "OpenCV/Sample/MobileNet";

    }

    protected void mediaController(VideoView vidView){
        VideoCapture capture = new VideoCapture(0);
        MediaController vidControl = new MediaController(this);
        vidControl.setAnchorView(vidView);
        vidView.setMediaController(vidControl);
    }
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    private Net net;
}
