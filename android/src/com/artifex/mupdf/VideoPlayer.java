package com.artifex.mupdf;

import java.io.IOException;

import com.artifex.mupdf.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.RelativeLayout;

public class VideoPlayer extends Activity implements OnPreparedListener, OnCompletionListener, MediaController.MediaPlayerControl, SurfaceHolder.Callback
{
    //private static final String TAG = "AudioPlayer";

    public static final String AUDIO_FILE_NAME = "audioFileName";

    private MediaPlayer mediaPlayer;
    private MediaController mediaController;
    private AudioManager mAudio;
    private String videoPath;
    private SurfaceView mSurfaceView;
    private SurfaceHolder holder;
    private RelativeLayout mTopBar;
    private Handler handler = new Handler();
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;
    public int VideoPauseDuration;
	/*private Runnable runnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (mediaController.isShowing())
			{
				mediaController.setEnabled(true);
				mediaController.hide();

			}
		}
	};*/

    private int width = 0;
    private int height = 0;
    private int duration;
    private int fromWeb; // 1 from webview 0 for default MUPDF ACTIVITY
    private Button btnClose;
    private Button videobtnClose;

    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        setContentView(R.layout.videoplayer_audio_player);
        //callLog("on create..", "calling");
        WindowManager.LayoutParams params = getWindow().getAttributes();

        //Log.i("x", "" + params.x);// params.x=300; Log.i("y",""+params.y);
/*		params.x = 0;
		params.y = 0;
		params.width = 331;
		params.height = 230;*/
        params.gravity = Gravity.CENTER;
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setAttributes(params);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		this.getWindow().setBackgroundDrawableResource(android.R.color.darker_gray);

//		((TextView)findViewById(R.id.now_playing_text)).setText(videoPath);

/*		RelativeLayout main = (RelativeLayout)findViewById(R.id.main_audio_view);

		RelativeLayout.LayoutParams rParams = new RelativeLayout.LayoutParams(500, 500);
		rParams.leftMargin = 0;
		rParams.rightMargin = 0;
//		rParams.setMargins(148, 557, 354, 728);
////		rParams.leftMargin = 30;
		main.setLayoutParams(rParams);*/

        mAudio = (AudioManager) getSystemService(VideoPlayer.AUDIO_SERVICE);

        videoPath = getIntent().getStringExtra("path");
        duration  = getIntent().getIntExtra("duration",0);
        fromWeb   = getIntent().getIntExtra("source", 0);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mTopBar = (RelativeLayout) findViewById(R.id.topBar);
        videobtnClose = (Button) findViewById(R.id.close_video);

        if( fromWeb == 1 )
        {
            mTopBar.setVisibility(View.GONE);
//			videobtnClose.setVisibility(View.VISIBLE);
            videobtnClose.setVisibility(View.GONE);
        }
        else
        {
//			mTopBar.setVisibility(View.VISIBLE);
            mTopBar.setVisibility(View.GONE);
            videobtnClose.setVisibility(View.GONE);
        }

        btnClose = (Button) findViewById(R.id.btnClose);
        mediaController = new MediaController(this);
        holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);
        try
        {
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        catch (Exception e)
        {
            if(fromWeb == 1)
                setResult(220);
            finish();
        }

        videobtnClose.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    if(mediaController.isShowing())
                    {
                        mediaController.setEnabled(false);
                        mediaController.hide();
                    }
                    setResult(220);
                    finish();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        btnClose.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    if(mediaController.isShowing())
                    {
                        mediaController.setEnabled(false);
                        mediaController.hide();
                    }
                    finish();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

/*	@Override
	protected void onStop()
	{
		super.onStop();
		System.out.println("On stop");
		mediaPlayer.stop();
		mediaPlayer.release();

	}*/

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        if(mediaController.isShowing())
        {
            mediaController.setEnabled(false);
            mediaController.hide();
        }
        startPlayerWithAspectRatio();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // the MediaController will hide after 3 seconds - tap the screen to
        // make it appear again
        mediaController.show();
        return false;
    }

    // --MediaPlayerControl
    // methods----------------------------------------------------
    public void start() {
        //callLog("on start..", "calling");
        mediaPlayer.start();
    }

    public void pause() {
        //callLog("on pause..", "calling");
        mediaPlayer.pause();
    }

    public int getDuration() {
        //callLog("get duration", "calling");
        try
        {
            int mDuration = mediaPlayer.getDuration();
            return mDuration;
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    public int getCurrentPosition()
    {
        //callLog("get position", "calling");
        try
        {
            int mPosition = mediaPlayer.getCurrentPosition();
            return mPosition;
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    public void seekTo(int i) {
        //callLog("seek to", "calling");
        mediaPlayer.seekTo(i);
    }

    public boolean isPlaying() {
        //callLog("is playing", "calling");
        try
        {
            return mediaPlayer.isPlaying();
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public int getBufferPercentage() {
        //callLog("get buff %", "calling");
        return 0;
    }

    public boolean canPause() {
        //callLog("can pause", "calling");
        return true;
    }

    public boolean canSeekBackward() {
        //callLog("can seek back", "calling");
        return true;
    }

    public boolean canSeekForward() {
        //callLog("can seek fwd", "calling");
        return true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //callLog("surface created", "calling");
        mediaPlayer.setDisplay(holder);
    }

    // --------------------------------------------------------------------------------

    public void onPrepared(MediaPlayer mediaPlayer)
    {
        mediaPlayer.seekTo(duration);
        startPlayerWithAspectRatio();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer)
    {
        quitActivity();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        //callLog("surface changed", "calling");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        //callLog("surface destroyed", "calling");

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            case KeyEvent.KEYCODE_VOLUME_UP:
                mAudio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mAudio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_BACK:
                quitActivity();
                return true;
            default:
                return false;
        }
    }

    private void quitActivity()
    {
        super.onStop();
        if( mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if(mediaController.isShowing())
        {
            mediaController.setEnabled(false);
            mediaController.hide();
        }

        if(fromWeb == 1)
            setResult(220);
        finish();
    }

    public void startPlayerWithAspectRatio()
    {
        mediaController.setMediaPlayer(this);
        View v = (View)findViewById(R.id.main_audio_view);

        android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();

        mediaController.setAnchorView(v);

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int screenWidth  = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        width = mediaPlayer.getVideoWidth();
        height = mediaPlayer.getVideoHeight();

        if (width != 0 && height != 0)
        {
            if(width > height)
            {
                if(this.getResources().getConfiguration().orientation == 1)
                {
                    lp.width  = screenWidth;
                    lp.height = screenWidth * height / width;
                }
                else
                {
                    lp.width  = screenHeight * width / height;
                    lp.height = screenWidth * height / width;
//					lp.height = screenHeight;
                }
            }
            else
            {
                lp.width = screenHeight * width / height;
                lp.height = screenWidth * height / width;
//				lp.height = screenHeight;
            }
            mSurfaceView.setLayoutParams(lp);
        }

        handler.post(new Runnable() { public void run() {
            mediaController.setEnabled(true); mediaController.show(); } });
    }

    @Override
    public int getAudioSessionId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(isPlaying())
        {
            VideoPauseDuration = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();

        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

            try
            {
                mediaPlayer.prepare();
            }
            catch (IllegalStateException e)
            {

            }
            catch (IOException e)
            {

            }
            mediaPlayer.seekTo(VideoPauseDuration);
            mediaPlayer.start();



    }
}