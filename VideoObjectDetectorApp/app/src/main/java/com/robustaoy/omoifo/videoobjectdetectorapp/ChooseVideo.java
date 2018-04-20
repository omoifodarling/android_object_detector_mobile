package com.robustaoy.omoifo.videoobjectdetectorapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.VideoView;

public class ChooseVideo extends AppCompatActivity {
private static final int VIDEO_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_video);
        selectVideo();
    }

    private void selectVideo(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("video/mp4");
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(intent , VIDEO_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == VIDEO_CODE && data != null) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK  && data.getData() != null) {
                Log.i("GOT IT: ","Image Received");
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                Uri video = data.getData();

                showVideo(video);

                // Do something with the contact here (bigger example below)
            }else Log.i("DATA HAD","Nothing it seems!!!");

        }else Log.d("WRONG: ","No Data RECEIVED!!!");

        super.onActivityResult(requestCode,resultCode,data);
    }

    public void showVideo(Uri  filePath){
        // Get the dimensions of the View
        //JavaCameraView view =  findViewById(R.id.showPhotoCamView);
        VideoView videoFrame =  findViewById(R.id.videoDisplayView);
        videoFrame.setVideoURI(filePath);
        videoFrame.start();


        //Bitmap image = BitmapFactory.decodeFile(filePath.getPath(),bmOptions);
    }
}
