package com.bytedance.videoplayer;


import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class VideoPlay extends AppCompatActivity {

    private static final String TAG = "VideoPlay";

    private SurfaceView surfaceView;
    private MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder;
    private SeekBar seekBar;

    private boolean isSeekingBarChanging;
    private int progressPosition;  //out值
    private int currentPosition;
    private boolean isPlaying;  //out值

    private Timer timer;
    private static final String PRG = "progress";
    private static final String ST = "state";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle onSaveInstanceState){
        super.onCreate(onSaveInstanceState);
        Log.d(TAG, "VideoPlay: onCreate");

        setContentView(R.layout.video_play);
        Log.d(TAG, "insert 0");
        try{
            if (onSaveInstanceState == null){
                progressPosition = 0;
                isPlaying = false;
            }else{
                progressPosition = onSaveInstanceState.getInt(PRG, 0);
                isPlaying = onSaveInstanceState.getBoolean(ST, false);
            }
        }catch(NullPointerException e){
            Log.d(TAG, e.toString());
        }

        seekBar = findViewById(R.id.seekBar);
        surfaceView = findViewById(R.id.surfaceView);
        try{
            mediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaPlayer.setDataSource(getResources().openRawResourceFd(R.raw.bytedance));
                surfaceHolder = surfaceView.getHolder();
                surfaceHolder.addCallback(new MyCallBack());
                mediaPlayer.prepare();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(true);
                    }
                });
                mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
                        changeVideoSize(mediaPlayer, i, i1);
                    }
                });
                mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        Log.d(TAG, "setOnBufferingUpdateListener: " + String.valueOf(percent));
                    }
                });

            }
        }catch (IOException e){
            Log.d(TAG, e.toString());
        }
        findViewById(R.id.playBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mediaPlayer.isPlaying()){
                    mediaStart();
                }
            }
        });
        findViewById(R.id.stopBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mediaPlayer.isPlaying()){
                    mediaStop();
                }
            }
        });
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setProgress(progressPosition);
        seekBar.setOnSeekBarChangeListener(new MySeekBarListener());

        isSeekingBarChanging = false;

        mediaStart();
        mediaStop();

        if(isPlaying)
            mediaStart();  // 之前为播放状态，则设置为播放状态
        else{
            mediaStart();
            mediaStop();
        }
        Log.d(TAG, "-->3 mediaPlayer progress: " + mediaPlayer.getCurrentPosition());

    }

    public void onRestart(){
        /* 处理app暂停恢复 */
        super.onRestart();
        Log.d(TAG, "A## onRestart");

        if(isPlaying)
            mediaStart();
        else{
            mediaStart();
            mediaStop();
        }
    }

    public void changeVideoSize(MediaPlayer mediaPlayer, int i, int i1){
        int surfaceWidth = surfaceView.getWidth();
        int surfaceHeight = surfaceView.getHeight();
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();

        if(getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
            //竖屏模式
            float max_rate = Math.max((float)videoWidth/surfaceWidth, (float)videoHeight/surfaceHeight);
            videoHeight = (int)Math.ceil((float)videoHeight/max_rate);
            videoWidth = (int)Math.ceil((float)videoWidth/max_rate);
            surfaceView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth, videoHeight));
        }
        else{
            //横屏模式
            ViewGroup vg = (ViewGroup)surfaceView.getParent();
            int vgw = vg.getWidth();
            int vgh = vg.getHeight();
            LinearLayout.LayoutParams lyp;
            if ((float)vgw/vgh > (float)videoWidth/videoHeight){
                int actualWidth = (int)Math.ceil((float)videoWidth * vgh / videoHeight);
                lyp = new LinearLayout.LayoutParams(actualWidth, LinearLayout.LayoutParams.MATCH_PARENT);
                lyp.leftMargin = (vgw - actualWidth) / 2;
            }
            else
                lyp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)Math.ceil((float)videoHeight * vgw / videoWidth));
            surfaceView.setLayoutParams(lyp);
        }
    }

    protected void onPause(){
        super.onPause();
        Log.d(TAG, "## onPause");
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                isPlaying = true;
                mediaStop();
            }else{
                isPlaying = false;
            }
            currentPosition = mediaPlayer.getCurrentPosition();
            Log.d(TAG, "current position: " + currentPosition + "  is playing: " + isPlaying);
        }
    }

    protected void onStop(){
        super.onStop();
        Log.d(TAG, "## onStop");
    }

    protected void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSavedInstanceState");
        savedInstanceState.putInt(PRG, currentPosition);
        savedInstanceState.putBoolean(ST, isPlaying);
    }

    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "## onDestroy");
    }

    public class MyCallBack implements SurfaceHolder.Callback{
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            mediaPlayer.setDisplay(surfaceHolder);
            Log.d(TAG, "MyCallBack: surfaceCreated");
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            Log.d(TAG, "MyCallBack: surfaceChanged");
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.d(TAG, "MyCallBack: surfaceDestroyed");
        }
    }

    public class MySeekBarListener implements SeekBar.OnSeekBarChangeListener{

        public void onProgressChanged(SeekBar seekBar, int i, boolean b){
            // Log.d(TAG, "MySeekBarListener: onProgressChanged");
        }

        public void onStartTrackingTouch(SeekBar seekBar){
            Log.d(TAG, "MySeekBarListener: onStartTrackingTouch");
            isSeekingBarChanging = true;
        }

        public void onStopTrackingTouch(SeekBar seekBar){
            isSeekingBarChanging = false;
            mediaPlayer.seekTo(seekBar.getProgress());
        }
    }

    public void mediaStart(){
        /* 播放时需要完成的 */
        mediaPlayer.seekTo(progressPosition);
        mediaPlayer.start();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isSeekingBarChanging && mediaPlayer.isPlaying()){
                    // Log.d(TAG, "media player current position: " + mediaPlayer.getCurrentPosition());
                    progressPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(progressPosition);
                }
            }
        }, 0 ,  200);  // 5hz
    }

    public void mediaStop(){
        /* 视频暂停时需要完成的 */
        mediaPlayer.pause();
        timer.cancel();
    }
}
