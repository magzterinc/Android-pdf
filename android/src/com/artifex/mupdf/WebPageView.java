package com.artifex.mupdf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;


import com.artifex.mupdf.R;

import java.lang.reflect.Field;

@SuppressLint({ "SetJavaScriptEnabled", "ViewConstructor" })
public class WebPageView extends ViewGroup
{
	private final Context mContext;
	protected int mPageNumber;                                                                                                                                 
	private Point mParentSize;
	protected Point mSize; // Size of page at minimum zoom
	protected float mSourceScale;

	private WebView mEntire; // Image rendered at minimum zoom

	private ViewGroup parent;
	boolean isFullScreen;
	boolean isViewSettled;
	private boolean loadingFinished;
	
	private MyWebChromeClient mChromeClient;
	private WebChromeClient.CustomViewCallback 	mCustomViewCallback;
	
    private static final String DESKTOP_USERAGENT = "Mozilla/5.0 (Android; Tablet; rv:20.0) Gecko/20.0 Firefox/20.0";
//	private static final String DESKTOP_USERAGENT = "Mozilla/5.0 (Linux; Android 4.4; Nexus 4 Build/KRT16H) AppleWebKit/537.36(KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36";

	private int w,h;

	public WebPageView(Context c, Point parentSize, ViewGroup parent) {
		super(c);
		mContext = c;
		mParentSize = new Point();
		mParentSize.x = getResources().getDisplayMetrics().widthPixels;
		mParentSize.y = getResources().getDisplayMetrics().heightPixels;
		this.parent = parent;
		
		mEntire = new WebView(mContext);
		addView(mEntire);
	}

	public void blank(/*int page*/)
	{
		isViewSettled = false;
	}

	public void settled()
	{
		if(!isViewSettled)
		{
			isViewSettled = true;
		}
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void setPage(String url)
	{
		mSize = mParentSize;
		
		mEntire.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((ReaderView) parent).getGestureDetector().onTouchEvent(event);
				/*
				 * if (event.getAction() == MotionEvent.ACTION_MOVE) {
				 * Log.i("WEBVIew",
				 * "oN moveeeee"+event.getX()+", "+event.getY()); return
				 * parent.onTouchEvent(event); } else
				 */
				return false;
			}
		});
		
		mEntire.setWebViewClient(new HtmlViewWebClient());
		mChromeClient = new MyWebChromeClient();
		mEntire.setWebChromeClient( mChromeClient );
		
		mEntire.setInitialScale(1);
		mEntire.clearFormData();
		mEntire.clearHistory();
		mEntire.clearCache(true);
		
		mEntire.getSettings().setBuiltInZoomControls(true);
		mEntire.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		mEntire.getSettings().setLoadWithOverviewMode(true);
		mEntire.getSettings().setUseWideViewPort(true);
		mEntire.getSettings().setAppCacheEnabled(true);
		mEntire.getSettings().setDatabaseEnabled(true);
		mEntire.getSettings().setAllowFileAccess(true);
		mEntire.getSettings().setLoadsImagesAutomatically(true);
		
		mEntire.getSettings().setSupportMultipleWindows(true);
		
		mEntire.getSettings().setAppCachePath(mContext.getDir("appcache", 0).getPath());
		mEntire.getSettings().setUserAgentString(DESKTOP_USERAGENT);
		mEntire.getSettings().setDatabasePath(mContext.getDir("databases", 0).getPath());
		mEntire.getSettings().setPluginState(PluginState.ON);
		
		mEntire.getSettings().setJavaScriptEnabled(true);
		mEntire.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		mEntire.getSettings().setDomStorageEnabled(true);
		mEntire.getSettings().setAllowContentAccess(false);
		
		mEntire.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mEntire.setScrollbarFadingEnabled(false);
	
		// Calculate scaled size that fits within the screen limits
		// This is the size at minimum zoom
		loadingFinished = false;
		
		if(url.equals("0"))
			mEntire.loadData("<html><head><style>html { display:table;text-align:center; width:100%; height:100%; border:0px solid red;}body { display:table-row; vertical-align:middle;font-size:2em; text-align:center }html, body { }</style></head><body><div style='display:table-cell; vertical-align:middle; text-align:center;'>Loading..</div></body></html>",
					"text/html", "utf-8");
		else
			mEntire.loadUrl(url.replace("#", "%23"));
		
//		mEntire.loadUrl("file:///mnt/sdcard/HTML/nw/index.html");
		
		requestLayout();
	}
	
	public void hideCustomView()
	{
		if(isFullScreen)
		{
			isFullScreen = false;
			mChromeClient.onHideCustomView();
		}
	}
	
	private class MyWebChromeClient extends WebChromeClient
	{
		private Bitmap mDefaultVideoPoster;
		private View mVideoProgressView;

		@Override
		public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback)
		{
	   		try
	   		{
	   			@SuppressWarnings("rawtypes")
                Class c1 = Class.forName("android.webkit.HTML5VideoFullScreen$VideoSurfaceView");
	   			Field f1 = c1.getDeclaredField("this$0");
	   			f1.setAccessible(true);

	   			@SuppressWarnings("rawtypes")
                Class c2 = f1.getType().getSuperclass();
	   			Field f2 = c2.getDeclaredField("mPlayer");

	   			f2.setAccessible(true);

	   			Field f3 = f1.getType().getSuperclass().getDeclaredField("mUri");
	   			f3.setAccessible(true);

	   			Object html5VideoViewInstance = f1.get(((FrameLayout)view).getChildAt(0)); // Look at the code in my other answer to this same question to see whats focusedChild
	   			Object mpInstance = f2.get(html5VideoViewInstance);

	   			int playerPosition;
	   			MediaPlayer mp = (MediaPlayer)mpInstance;

	   			if(mp.isPlaying())
	   			{
	   				mp.stop();
	   				playerPosition = mp.getCurrentPosition();
	   			}
	   			else if(mp.getCurrentPosition() == mp.getDuration())
	   				playerPosition = 0;
	   			else if(mp.getCurrentPosition() > 0)
	   				playerPosition = mp.getCurrentPosition();
	   			else
	   				playerPosition = 0;

	   			Object uri = f3.get(html5VideoViewInstance);

	   			Uri urlllllll = (Uri) uri;

	   			mCustomViewCallback = callback;

	   			isFullScreen = true;

	   			Intent videointent = new Intent(mContext, VideoPlayer.class);
	   			videointent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	   			videointent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	   			videointent.putExtra("path", urlllllll.getPath());
	   			videointent.putExtra("duration", playerPosition);
	   			videointent.putExtra("source", 1);
	   			((Activity) mContext).startActivityForResult(videointent, 1);

	   		}
	   		catch(Exception e)
	   		{
	   			e.printStackTrace();
	   		}
		}

		@Override
		public void onHideCustomView()
		{
			try
			{
				mCustomViewCallback.onCustomViewHidden();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public Bitmap getDefaultVideoPoster()
		{
			if (mDefaultVideoPoster == null)
				mDefaultVideoPoster = BitmapFactory.decodeResource(getResources(), R.drawable.magshadow);
			return mDefaultVideoPoster;
		}

		@Override
		public View getVideoLoadingProgressView()
		{
	        if (mVideoProgressView == null)
	        {
	            LayoutInflater inflater = LayoutInflater.from(mContext);
	            mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, null);
	        }
	        return mVideoProgressView;
		}

    	 @Override
         public void onReceivedTitle(WebView view, String title)
    	 {
            ((Activity) mContext).setTitle(title);
         }

         @Override
         public void onProgressChanged(WebView view, int newProgress)
         {
        	 ((Activity) mContext).getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress*100);
         }
	}

	class HtmlViewWebClient extends WebViewClient
	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
//			if(url.startsWith("http") || url.startsWith("www"))
			if(url.endsWith(".pdf") || url.startsWith("market") || url.contains("youtube.com"))
			{
				if(loadingFinished)
				{
					Uri uri = Uri.parse(url);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					mContext.startActivity(intent);

					return true;
				}
				else
					view.loadUrl(url);
			}
			else
				view.loadUrl(url);

			return false;
		}

		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			 super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
				loadingFinished = true;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int x, y;

		switch (View.MeasureSpec.getMode(widthMeasureSpec))
		{
		case View.MeasureSpec.UNSPECIFIED:
			x = mParentSize.x;
			break;
		default:
			x = View.MeasureSpec.getSize(widthMeasureSpec);
		}

		switch (View.MeasureSpec.getMode(heightMeasureSpec))
		{
		case View.MeasureSpec.UNSPECIFIED:
			y = mParentSize.y;
			break;
		default:
			y = View.MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		w = right - left;
		h = bottom - top;

		if (mEntire != null)
			mEntire.layout(0, 0, w, h);
	}

	static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
	        new FrameLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
}