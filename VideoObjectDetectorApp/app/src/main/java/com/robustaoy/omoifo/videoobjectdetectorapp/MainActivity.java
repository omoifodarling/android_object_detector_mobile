package com.robustaoy.omoifo.videoobjectdetectorapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button photo =  findViewById(R.id.photo_button);
        Button video =  findViewById(R.id.video_button);

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,ChoosePhoto.class);
                startActivity(intent);
            }
        });

        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.photo_display);
                Intent intent = new Intent(MainActivity.this,LiveVideoStream.class);
                startActivity(intent);
            }
        });
        /*
        Button live_video =findViewById(R.id.live_stream_button);
        Button  media = findViewById(R.id.web_button);
        Button threaded_stream = findViewById(R.id.threaded_button);

        media.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this,PlayFromMedia.class);
                startActivity(intent);
            }
        });

        live_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,LiveVideoStream.class);
                startActivity(intent);
            }
        });

        threaded_stream.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this,LiveVideoStream.class);
                startActivity(intent);
            }
        });*/
    }
}
