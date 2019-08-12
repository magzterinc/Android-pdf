package com.artifex.mupdf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import com.artifex.mupdf.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@SuppressWarnings("deprecation")
class ThreadPerTaskExecutor implements Executor {
	public void execute(Runnable r) {
		new Thread(r).start();
	}
}

@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
public class MuPDFActivity extends Activity
{ 
	/* The core rendering instance */
	public  MuPDFCore core;
	private String mFileName;
	private ReaderView mDocView;
	private View mButtonsView;
	private boolean mButtonsVisible;
	public Button mInteractiveButton;
	private TextView mFilenameView;
	private int downloadingPercentage = 0;
	private TextView mPageNumberView;
	private ViewSwitcher mTopBarSwitcher;
	private boolean mTopBarIsSearch;
	private TextView mDownloadedPercentage;
	private AsyncTask<Void, Void, Void> mDestroyTask;

	//comments

	final Context context = this;

	private int page_number;
	private int mCurrentDisplayingPage = 0;

	private TextView no_of_count;
	//	private ArrayList<String> arrayLists = new ArrayList<String>();
	//	private String[] subLists;
	private String count="";
	private int page_no;

	private String urlfromnative;
	private RelativeLayout mBottomLayout;
	private float currentProgressLength;
	public  int screenWidth,screenHeight;
	public  int screenWidthForView,screenHeightForView;
	private int mCorrector;//,mCount;

	public  int noOfPages = 6;
	public  boolean isPreview;
	public String[] previewPageNumbers = null;
	private String magazineName,magazineId,editionId,mStrpreviewPages;
	private String path;
	private LoadPDFAsyncTask mPDFTask;
	private ImageLoader imageLoader;
	private ImageAdapter mImageAdapter;
	public static int addPagesCount = 0;
	private String[] thumbNailUrls;
	private String[] pageNumbers,pageNumbersL;
	private DisplayImageOptions options;
	private DisplayMetrics metrics;
	@SuppressWarnings("deprecation")
	private Gallery mthumbnailGallery;
	private Button mBtnSubscribe;
	private int isSubscribe; //0-Has to enable subscribe; 1-Has tot enable buy now
	private int isSpecialIssue; //0-Normal issue; 1-Special issue
	private int[] addPagesList;
	private String[] htmlUrls;
	private String pdfTitle, _isLib;
	private String editionPdfURL, pdfPages;//,editionPublishedDate;
	private MuPDFPageAdapter pdfAdapter;
	SharedPreferences prefs;
	private int currentPage = 1, pageNumberLandscape;
	MuPDFPageView pageView;
	private int currentInteractivePageCount;
	public  int screenOrientation ;
	public boolean isCoreNull;
	public boolean isThumbClicked;
	ProgressBar progress;
	Button mshareButton,mbookmarkButton;
	Bitmap mEntireBm;

	// subscribe
	private boolean isTaskRunning = false;
	private Dialog dialog;

	private GridView subscription_list;
	private ProgressDialog mProgressDialog;
	private ArrayList<String> arrayList = new ArrayList<String>();
	private String[] subList;
	private LayoutInflater mInflater;

	//Bookmark
	private ImageLoader bImageLoader;
	private DisplayImageOptions bOptions;
	private int lastDisplayedPage;
	private String user;
	private String /*price,mprice,*/mFormatType;
	private Dialog bookmark_dialog;

	BitmapHolder mEntire;

	//Landscape easymode
	public  boolean isEasyMode;
	private ToggleButton bModeSwitcher;

	//Autoplay parameters
	private int mCurrentDownloadingpage;
	private String issthree = "";
	private String bucketname = "";

	// Decrypting PDF downloading URL
	private AsyncTask<String, Void, Void> mDecryptURL;

	// Flurry
	private String editionPrice_Flurry = "";
	private String editionName_Flurry  = "";

	//paypal
	private Dialog paymentDialog;
	public static String PURCHASE_ISSUEID =  "";
	private static String PURCHASE_SUBSCRIPTION = "";
	private static String PURCHASE_ISSUEPRICE = "";
	public String priceInUsd;
	//private boolean isCurrencyTaskRunning = false;
	public static String ITEM_SKU = "";
	private boolean isSubscription = false;
	public static String PAYMENTMODE =  "";
	private String editionPrice;
	
	//Downloading HTML AD
	private int mCurrent_HTMLAD_Download = 0;
	private ArrayList<String> HTML_Page_no = new ArrayList<String>();
	
	//For HD Screen
	float sourceScale;
	PointF viewSize;

	// For XML generation and tracking
	File newxmlfile;
	FileOutputStream fileos;
	ArrayList<String> mainArray = new ArrayList<String>();
	ArrayList<String> pageNoArray /* = new ArrayList<String>() */;
	ArrayList<String> timeArray = new ArrayList<String>();
	ArrayList<String> pageTypeArray = new ArrayList<String>();
	ArrayList<String> titleArray = new ArrayList<String>();
	ArrayList<String> secondPageNoArray = new ArrayList<String>();
	ArrayList<String> VideoArray = new ArrayList<String>();
	ArrayList<String> videoNameArray = new ArrayList<String>();
	ArrayList<String> VideoMainArray = new ArrayList<String>();
	ArrayList<String> LinkArray = new ArrayList<String>();
	ArrayList<String> LinkNameArray = new ArrayList<String>();
	ArrayList<String> LinkTime=new ArrayList<String>();
	ArrayList<String> videoTapTime=new ArrayList<String>();
	ArrayList<String> LinkMainArray = new ArrayList<String>();
	ArrayList<String> OrientationArray = new ArrayList<String>();
	ArrayList<String> intervelArray = new ArrayList<String>();
	ArrayList<String> intervel = new ArrayList<String>();
	ArrayList<String> sessionIntervel = new ArrayList<String>();
	ArrayList<String> pageShareArray = new ArrayList<String>();
	ArrayList<String> OrientationArraySharedPage = new ArrayList<String>();
	ArrayList<String> sessionArray = new ArrayList<String>();
	ArrayList<String> orignalArray;
	HashMap<Integer, PageReadEnity> hashmap = new HashMap<Integer, PageReadEnity>();
	Integer key2;
	PageReadEnity values;
	float ssTime = 0;
	boolean pageContains = false;
	boolean VideoTapped = false;
	boolean linkTapped = false;
	boolean shareTapped = false;
	String url = "";
	int tempPageNo;
	int pause = 0;
	private MuPDFActivity myActivity;
	/** Called when the activity is first created. */

	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = lastSlashPos == -1 ? path : path.substring(lastSlashPos + 1);
		try 
		{
			core = new MuPDFCore(path, this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return core;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		isEasyMode = false;
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		bImageLoader = ImageLoader.getInstance();
		ImageLoaderConfiguration config1 = new ImageLoaderConfiguration.Builder(this)
		.threadPoolSize(3)
		.threadPriority(Thread.NORM_PRIORITY - 2)
		.memoryCacheSize(50000)// 50 kb
		.httpReadTimeout(10000) // 10 s
		.denyCacheImageMultipleSizesInMemory()
		.build();
		bImageLoader.init(config1);

		bOptions = new DisplayImageOptions.Builder()
		.showStubImage(R.drawable.magshadow).showImageForEmptyUrl(R.drawable.addmag).cacheInMemory().build();

		imageLoader = ImageLoader.getInstance();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).threadPoolSize(3).threadPriority(Thread.NORM_PRIORITY - 2)
				.memoryCacheSize(50000)// 50 kb
				.httpReadTimeout(10000) // 10 s
				.denyCacheImageMultipleSizesInMemory().build();
		imageLoader.init(config);
		options = new DisplayImageOptions.Builder().showStubImage(R.drawable.pageplaceholder).showImageForEmptyUrl(R.drawable.adddefault).cacheInMemory().build();

		myActivity = MuPDFActivity.this;
		mEntire    = new BitmapHolder();

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		screenHeight = metrics.heightPixels;
		screenWidth  = metrics.widthPixels;
		screenOrientation = getResources().getConfiguration().orientation;
		
		if(screenOrientation == 1)
		{
			screenWidthForView  = screenWidth;
			screenHeightForView = screenHeight;
		}
		else
		{
			screenWidthForView  = screenHeight;
			screenHeightForView = screenWidth;
		}

		String pdfURLEncrypted = "";
		pdfTitle 		  	 = getIntent().getStringExtra("title");
		mStrpreviewPages 	 = getIntent().getStringExtra("previewPages");
		isPreview 		  	 = true;
		magazineName	 	 = "Galatta Cinema";
		magazineId		 	 = "0";
		editionId 		     = "0";
		writeFiles();
		issthree		 	 = getIntent().getStringExtra("issthree");
		bucketname		 	 = getIntent().getStringExtra("bucketname");
		pdfURLEncrypted		 = getIntent().getStringExtra("editionpdf");
		pdfPages			 = getIntent().getStringExtra("pdfPages");
		mFormatType			 = getIntent().getStringExtra("FormatType");
		isSubscribe 		 = getIntent().getIntExtra("issubscribe", 0);
		isSpecialIssue 		 = getIntent().getIntExtra("isSpecialIssue", 0);
		_isLib 				 = getIntent().getStringExtra("isLib");

		if(_isLib == null)
			_isLib = "0";

		path = Environment.getExternalStorageDirectory().getAbsolutePath();
		if (core == null) 
		{
				core = openFile(path+"/");
		}

		mProgressDialog = new ProgressDialog(this);

		isCoreNull = false;


		File f = new File(path);
		if (!f.exists())
			f.mkdirs();
		if(!f.isDirectory())
		{
			f.delete();
			f.mkdirs();
		}

		int thumbAdjuster = 0;
		addPagesList = new int[0];

		if (isPreview)
		{
			addPagesCount = 0;
			addPagesList = new int[0];
			htmlUrls = new String[0];
		} 
		else 
		{

		}

		thumbNailUrls = new String[noOfPages+addPagesCount];
		pageNumbers = new String[noOfPages+addPagesCount];
		pageNumbersL = new String[noOfPages+addPagesCount];

		for (int i = 0; i < thumbNailUrls.length; i++) 
		{

				if (addPagesCount == 0)
				{
					thumbNailUrls[i] = "file:///" + path + "/" + (i) + "_1";
					pageNumbers[i] = "" + (i + 1);
					try
					{
						pageNumbersL[(i/2)+1] = ""+pageNumbers[i];
					}
					catch(Exception e)
					{

					}
				} 
				else
				{

				}

		}

		for (int k = 1; k < ((noOfPages/2)+addPagesCount+1); k++) 
		{
			try 
			{
				pageNumberLandscape = Integer.parseInt(pageNumbers[(k)*2-1-mCorrector]);
				pageNumbersL[k] = ""+pageNumberLandscape+"-"+(pageNumberLandscape+1);
			}
			catch (Exception e)
			{
				pageNumbersL[k] = ""+pageNumbers[(k)*2-1-mCorrector];
				mCorrector+=1;
			}
		}

		createUI(savedInstanceState);
		getDecryptedURL(pdfURLEncrypted, editionId);

		if(new File(path+"/0.pdf").exists())
			new MyCountDown(2000, 2000);

		// create a new file called "new.xml" in the SD card


	}

	private void writeFiles() {

	}

	@Override
	protected void onResume() 
	{
		super.onResume();
	}

	@Override
	protected void onStart() 
	{
		super.onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}


	private class MyCountDown extends CountDownTimer
	{
		public MyCountDown(long millisInFuture, long countDownInterval) 
		{
			super(millisInFuture, countDownInterval);
			start();
//			showButtons();
		}

		@Override
		public void onFinish() 
		{
			// textView1.setVisibility(View.INVISIBLE);
			hideButtons();
		}

		@Override
		public void onTick(long duration) 
		{ 
			// could set text for a timer here     
		}   
	}

	protected void getDecryptedURL(String encryptedURL, String editionId)
	{}


	protected void checkForAutoPlayOfVideo(String fileName, int pageNumber)
	{
		String mTempFileName = null;
		String mediaPath = null;

		mTempFileName = fileName.substring((fileName.indexOf(':'))+1);

		if(new File(path	+ "/" + mTempFileName).exists())
			mediaPath = path	+ "/" + mTempFileName;
		else
			mediaPath= core.writeMediaStream(path	+ "/", mTempFileName, pageNumber);

		Intent videointent = new Intent(MuPDFActivity.this, VideoPlayer.class);
		videointent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		videointent.putExtra("path", mediaPath);
		videointent.putExtra("duration", 0);
		videointent.putExtra("source", 0);
		startActivity(videointent);
	}

	protected void setSelectedPositionThumb()
	{
		final int adCount = 0;
		if(screenOrientation == 1 || (screenOrientation == 2  && isEasyMode))
			mthumbnailGallery.setSelection(mDocView.getDisplayedViewIndex());
		else
		{
			if(mDocView.getDisplayedViewIndex() < (noOfPages/2))
				mthumbnailGallery.setSelection((mDocView.getDisplayedViewIndex() * 2) - adCount);
			else
			{
				try
				{
					mthumbnailGallery.setSelection(((mDocView.getDisplayedViewIndex() * 2) - 1)-addPagesCount);
				}
				catch(Exception e)
				{

				}
			}
		}

		mImageAdapter.notifyDataSetChanged();
		mthumbnailGallery.post( new Runnable()
		{
			@Override
			public void run()
			{
				highLightGallery(adCount);
			}
		});
	}


	protected void highLightGallery(int adCount)
	{
		if(mButtonsVisible && !(mDocView.getDisplayedView() instanceof WebPageView))
		{
			int i = 0;
			int viewPosition = 0, totalPageCount = 0;
			int pagenumber = 0;
			int mTemp = 0;
			int mTemp1 = 0, mTemp2 = 0;
			int mTemp3 = 0, mTemp4 = 0;
			int mTemp5 = 0, mTemp6 = 0;

			int count = adCount;

			if(screenOrientation == 1 || (screenOrientation == 2  && isEasyMode))
			{
				i = mDocView.getDisplayedViewIndex();
				viewPosition = mDocView.getDisplayedViewIndex();
				mTemp = i;
				try
				{

						totalPageCount = pageNumbers.length;
						pagenumber = (Integer.parseInt((pageNumbers[i])) - 1 );

				}
				catch(NumberFormatException e)
				{

				}
				catch(Exception e)
				{

				}

			}
			else
			{
				i = (mDocView.getDisplayedViewIndex()*2) - count;
				viewPosition = (mDocView.getDisplayedViewIndex());
				mTemp = i;
				try
				{

						totalPageCount = (pageNumbers.length) / 2;
						pagenumber = (Integer.parseInt((pageNumbers[i])) - 1 );

				}
				catch(NumberFormatException e)
				{

				}
				catch(Exception e)
				{
						pagenumber = (Integer.parseInt((pageNumbers[i - 1])) - 1 );
				}
			}

			if(pagenumber % 2 == 0)
			{
				mTemp1 = (mTemp - 1);
				mTemp2 = mTemp;

				mTemp3 = (mTemp - 2);
				mTemp4 = (mTemp - 3);

				mTemp5 = (mTemp + 1);
				mTemp6 = (mTemp + 2);
			}
			else
			{
				mTemp1 = mTemp;
				mTemp2 = (mTemp + 1);

				mTemp3 = (mTemp - 1);
				mTemp4 = (mTemp - 2);

				mTemp5 = (mTemp + 2);
				mTemp6 = (mTemp + 3);
			}

			if(i == 0)
			{
				View child1 = mthumbnailGallery.getChildAt(mTemp2 - mthumbnailGallery.getFirstVisiblePosition());
				if (child1 != null)
				{
					ImageView borderImg = child1.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFF0000FF);
				}

				View childNext1 = mthumbnailGallery.getChildAt(mTemp5 - mthumbnailGallery.getFirstVisiblePosition());
				if (childNext1 != null)
				{
					ImageView borderImg = childNext1.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
				View childNext2 = mthumbnailGallery.getChildAt(mTemp6 - mthumbnailGallery.getFirstVisiblePosition());
				if (childNext2 != null)
				{
					ImageView borderImg = childNext2.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
			}
			else if(viewPosition == totalPageCount)
			{
				View child1 = mthumbnailGallery.getChildAt((mTemp1 - 1) - mthumbnailGallery.getFirstVisiblePosition());
				if (child1 != null)
				{
					ImageView borderImg = child1.findViewById(R.id.thumb_image);
						borderImg.setPadding(4, 4, 4, 4);
					borderImg.setBackgroundColor(0xFF0000FF);
				}

				View childNext1 = mthumbnailGallery.getChildAt((mTemp1 - 2) - mthumbnailGallery.getFirstVisiblePosition());
				if (childNext1 != null)
				{
					ImageView borderImg = childNext1.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
				View childNext2 = mthumbnailGallery.getChildAt((mTemp1 - 3) - mthumbnailGallery.getFirstVisiblePosition());
				if (childNext2 != null)
				{
					ImageView borderImg = childNext2.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
			}
			else
			{
				View child = mthumbnailGallery.getChildAt((mTemp1) - mthumbnailGallery.getFirstVisiblePosition());
				if (child != null)
				{
					ImageView borderImg = child.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFF0000FF);
				}
				View child1 = mthumbnailGallery.getChildAt((mTemp2) - mthumbnailGallery.getFirstVisiblePosition());
				if (child1 != null)
				{
					ImageView borderImg = child1.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFF0000FF);
				}

				View childPrevious1 = mthumbnailGallery.getChildAt(mTemp3 - mthumbnailGallery.getFirstVisiblePosition());
				if (childPrevious1 != null)
				{
					ImageView borderImg = childPrevious1.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
				View childPrevious2 = mthumbnailGallery.getChildAt(mTemp4 - mthumbnailGallery.getFirstVisiblePosition());
				if (childPrevious2 != null)
				{
					ImageView borderImg = childPrevious2.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}

				View childNext1 = mthumbnailGallery.getChildAt(mTemp5 - mthumbnailGallery.getFirstVisiblePosition());
				if (childNext1 != null)
				{
					ImageView borderImg = childNext1.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
				View childNext2 = mthumbnailGallery.getChildAt(mTemp6 - mthumbnailGallery.getFirstVisiblePosition());
				if (childNext2 != null)
				{
					ImageView borderImg = childNext2.findViewById(R.id.thumb_image);
					borderImg.setBackgroundColor(0xFFFFFFFF);
				}
			}
		}
	}

	@SuppressLint("DefaultLocale")
	public void createUI(Bundle savedInstanceState)
	{
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view making use of the ReaderView's
		// internal
		// gesture recognition
		mDocView = new ReaderView(this)
		{
//			private boolean showButtonsDisabled;
			private int htmlCount;
			public boolean onSingleTapUp(MotionEvent e)
			{
				if (/*!showButtonsDisabled &&*/ mDocView.getDisplayedView() instanceof PageView)
				{
					String[] link = null;
					pageView = (MuPDFPageView) mDocView.getDisplayedView();

					if (pageView != null) 
					{
						urlfromnative = "";
						link = pageView.hitLinkPage(e.getX(), e.getY());

						if(link != null && link.length > 0)
							urlfromnative = link[0];
					}

					if (!urlfromnative.equalsIgnoreCase(""))
					{
						if (urlfromnative.startsWith("MEDIA:"))
						{
							String mTempFileName = null;
							String mediaPath = null;

							mTempFileName = urlfromnative.substring((urlfromnative.indexOf(':')) + 1);

							if(new File(path	+ "/" + mTempFileName).exists())
								mediaPath = path	+ "/" + mTempFileName;
							else
								mediaPath = core.writeMediaStream(path	+ "/", mTempFileName, Integer.parseInt(link[1]));

							Intent videointent = new Intent(MuPDFActivity.this, VideoPlayer.class);
							videointent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
							videointent.putExtra("path", mediaPath);
							videointent.putExtra("duration", 0);
							videointent.putExtra("source", 0);
							startActivity(videointent);

						}
						else
						{
							if (urlfromnative.startsWith("https://") || urlfromnative.startsWith("http://") || urlfromnative.startsWith("www")||urlfromnative.startsWith("Http://")||urlfromnative.startsWith("Https://")) 
							{
								if (urlfromnative.startsWith("www")) 
									urlfromnative = "http://" + urlfromnative;
								else if(urlfromnative.startsWith("Http://")||urlfromnative.startsWith("Https://"))
									urlfromnative = urlfromnative.replace("Http", "http");

								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setData(Uri.parse(urlfromnative));
								startActivity(intent);
								linkTapped = true;
							}
							else if (urlfromnative.startsWith("pageto") || urlfromnative.startsWith("mailto") || isMailId(urlfromnative)) 
							{
								String[] interLinkStrings = urlfromnative.split(":");
								if (isMailId(interLinkStrings[1])) 
								{
									initShareItent(interLinkStrings[1]);
									linkTapped = true;
								}
								else
								{
									try 
									{
										htmlCount = 0;
										int mSetPageNumber = Integer.parseInt(interLinkStrings[1]);

										if(addPagesCount == 0)
											htmlCount = 0;
										else
										{
											for (int k = 0; k < mSetPageNumber; k++) 
											{
												try 
												{
													Integer.parseInt(pageNumbers[k]);
												}
												catch (Exception e2)
												{
													htmlCount+=1;
												}
											}
										}

										if(screenOrientation == 1 || (screenOrientation == 2 && isEasyMode))
											mSetPageNumber = mSetPageNumber - 1;
										else
											mSetPageNumber = mSetPageNumber / 2; 

										mDocView.setDisplayedViewIndex(mSetPageNumber + htmlCount);
										mthumbnailGallery.setSelection(mDocView.getDisplayedViewIndex() + htmlCount);
										linkTapped = true;
									}
									catch (NumberFormatException e1)
									{

									}
								}
							}

						}
					} 
					else 
					{
						if (!mButtonsVisible) 
							showButtons();
						else 
							hideButtons();
					}
				}
				return super.onSingleTapUp(e);
			}

			private boolean isMailId(String str)
			{
				String EMAIL_REGEX = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
				String email1 = str;
				Boolean b = email1.matches(EMAIL_REGEX);
				return b;
			}

			@SuppressLint("DefaultLocale")
			private void initShareItent(String mailAddress)
			{
				boolean found = false;
				Intent share = new Intent(android.content.Intent.ACTION_SEND);
				share.setType("plain/text");
				// gets the list of intents that can be loaded.
				List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(share, 0);
				if (!resInfo.isEmpty()) {
					for (ResolveInfo info : resInfo) {
						if (info.activityInfo.packageName.toLowerCase().contains("mail") || info.activityInfo.name.toLowerCase().contains("mail")) {
							share.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { mailAddress });
							share.setPackage(info.activityInfo.packageName);
							found = true;
							break;
						}
					}
					if (!found)
						return;
					startActivity(Intent.createChooser(share, "Select"));
				}
			}

			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			public boolean onScaleBegin(ScaleGestureDetector d)
			{
				// Disabled showing the buttons until next touch.
				// Not sure why this is needed, but without it
				// pinch zoom can make the buttons appear
//				showButtonsDisabled = true;
				return super.onScaleBegin(d);
			}

				/*public boolean onTouchEvent(MotionEvent event)
			{
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
					showButtonsDisabled = false;

				return super.onTouchEvent(event);
			}*/

			protected void onChildSetup(int i, View v)
			{
				if (v instanceof PageView)
				{/*
					((MuPDFPageView) v).setChangeReporter(new Runnable()
					{
						public void run()
						{
							mDocView.applyToChildren(new ReaderView.ViewMapper()
							{
								@Override
								void applyToView(View view) {
									((MuPDFPageView) view).update();
								}
							});
						}
					});
				 */}
			}

			@SuppressLint("NewApi")
			protected void onMoveToChild(int i)
			{
				if (core == null)
					return;

				mDocView.resetupChildren();
				updatePageNumView(i);
				mCurrentDisplayingPage = i;

				try
				{
					if(screenOrientation == 2 && isEasyMode && Build.VERSION.SDK_INT >= 11)
						mDocView.getDisplayedView().setTop(0);
				}
				catch(Exception e)
				{

				}
			}

			protected void onSettle(View v)
			{
				// When the layout has settled ask the page to render
				// in HQ
				if(v instanceof PageView)
				{
					if(mScale != MIN_SCALE)
					{
						if(screenOrientation == 2 && isEasyMode && ReaderView.mScale < (MIN_SCALE + 0.05));
						else
						{
							((PageView) v).addHq(false);
							return;
						}
					}

					((PageView) v).update();

					if(screenOrientation == 2 && isEasyMode)
						((PageView) v).addHq(false);

					mInteractiveButton.setVisibility(View.GONE);

					if(mButtonsVisible)
						setSelectedPositionThumb();
				}
				else
				{
					mScale = 1.0f;

					((WebPageView) v).settled();

					if(mButtonsVisible)
						setSelectedPositionThumb();
				}
			}

			protected void onUnsettle(View v)
			{
				// When something changes making the previous settled view
				// no longer appropriate, tell the page to remove HQ
				if (v instanceof PageView)
					((PageView) v).removeHq();
			}

			@Override
			protected void onNotInUse(View v)
			{
				if (v instanceof PageView)
					((PageView) v).releaseResources();
				else
					((WebPageView)v).setPage("0");
			}
		};
		pdfAdapter = new MuPDFPageAdapter(this, core, addPagesList, path, htmlUrls);
		mDocView.setAdapter(pdfAdapter);
		// Make the buttons overlay, and store all its
		// controls in variables
		ReaderView.MIN_SCALE = 1.0f;
		ReaderView.mScale    = 1.0f;
		makeButtonsView();

		// Set the file-name text
		mFilenameView.setText(pdfTitle);

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		int lastSavedOrientation = prefs.getInt("currentOrientation"+ magazineId + editionId, screenOrientation);

			currentInteractivePageCount = 0;

			if(screenOrientation == 1)
				mFilenameView.getLayoutParams().width=180;

		//check its from bookmark
		user=""+getIntent().getStringExtra("user_selected");
		if(user.equalsIgnoreCase("bookmark"))
		{

			if(prefs.getBoolean("isConfigChanged", false))
			{
				lastDisplayedPage = prefs.getInt(("page" + magazineId + editionId), 0);

				if((prefs.getBoolean("isConfigChanged", false) == false) && lastSavedOrientation == screenOrientation)
				{

						mDocView.setDisplayedViewIndex((lastDisplayedPage)+currentInteractivePageCount);
				}
				else
				{
					if(!prefs.getBoolean("isConfigChanged", false) && isPreview)
						mDocView.setDisplayedViewIndex(0);
					else
					{
						if(screenOrientation == 1)
							mDocView.setDisplayedViewIndex((lastDisplayedPage * 2)-1+currentInteractivePageCount);
						else
						{
							if(lastDisplayedPage % 2 == 0)
								mDocView.setDisplayedViewIndex(lastDisplayedPage/2+currentInteractivePageCount);
							else
								mDocView.setDisplayedViewIndex((lastDisplayedPage/2)+1+currentInteractivePageCount);
						}
					}
				}
			}

			else
			{
				lastDisplayedPage = Integer.parseInt(getIntent().getStringExtra("page"))-1;


				if(screenOrientation == 1){
					mDocView.setDisplayedViewIndex((lastDisplayedPage)+currentInteractivePageCount);
				}
				else{
					if(lastDisplayedPage % 2 == 0)
						mDocView.setDisplayedViewIndex(lastDisplayedPage/2+currentInteractivePageCount);
					else
						mDocView.setDisplayedViewIndex((lastDisplayedPage/2)+1+currentInteractivePageCount);
				}
			}
		}
		// not from bookmark
		else
		{
			if(new File(path+"/0.pdf").exists())
				lastDisplayedPage = prefs.getInt(("page" + magazineId + editionId), 0);
			else
				lastDisplayedPage = 0;

			if((prefs.getBoolean("isConfigChanged", false) == false) && lastSavedOrientation == screenOrientation)
			{

					mDocView.setDisplayedViewIndex((lastDisplayedPage)+currentInteractivePageCount);
			}
			else
			{
				if(!prefs.getBoolean("isConfigChanged", false) && isPreview)
					mDocView.setDisplayedViewIndex(0);
				else
				{
					if(screenOrientation == 1)
						mDocView.setDisplayedViewIndex((lastDisplayedPage * 2)-1+currentInteractivePageCount);
					else
					{
						if(lastDisplayedPage % 2 == 0)
							mDocView.setDisplayedViewIndex(lastDisplayedPage/2+currentInteractivePageCount);
						else
							mDocView.setDisplayedViewIndex((lastDisplayedPage/2)+1+currentInteractivePageCount);
					}
				}
			}
		}
		if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
			showButtons();

		/*		if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
			searchModeOn();
		 */
		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.setBackgroundResource(R.drawable.tiled_background);
		layout.addView(mDocView);
		layout.addView(mButtonsView);
		// layout.setBackgroundResource(R.color.canvas);
		setContentView(layout);
	}

	@Override

	public Object onRetainNonConfigurationInstance() {
		MuPDFCore mycore = core;
		core = null;
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		mDocView.setDisplayedViewIndex(prefs.getInt(("page" + magazineId + editionId), 0));
		SharedPreferences.Editor edit = prefs.edit();
		edit.putBoolean("isConfigChanged", true);
		edit.putInt("orientation", screenOrientation);
		edit.commit();
		return mycore;
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		int count = 0;
		int displayedNo = 0;
		if(mDocView != null)
		if((screenOrientation == 1) || (screenOrientation == 2 && isEasyMode))
		{
			displayedNo = mDocView.getDisplayedViewIndex();
			screenOrientation = 1;
		}
		else
			displayedNo = (mDocView.getDisplayedViewIndex()* 2)-1;


		{
			for (int k = 0; k < displayedNo/*((screenOrientation == 1)?mDocView.getDisplayedViewIndex():((mDocView.getDisplayedViewIndex()* 2)-1))*/; k++) {
				try
				{
					Integer.parseInt(pageNumbers[k]);
				}
				catch (Exception e)
				{
					count +=1;
				}
			}
		}

		if (mFileName != null && mDocView != null)
		{
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			if((screenOrientation == 1) || (screenOrientation == 2 && isEasyMode))
				edit.putInt(("page" + magazineId + editionId), displayedNo - count);
			else
				edit.putInt(("page" + magazineId + editionId), mDocView.getDisplayedViewIndex() - count);
			edit.putInt("currentAddPageCount", count);
			edit.putInt("currentOrientation"+ magazineId + editionId, screenOrientation);
			edit.commit();
		}

	}

	public void setInterval()
	{
		float diffSeconds = 0;
		String one;
		Date d1 = null,d2=null,d3 = null;
		String session = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-d  HH:mm:ss");
		String currentDateandTime = sdf.format(new Date());
		for (int i = 0; i < timeArray.size(); i++) {
			one = timeArray.get(i);
			String two = intervelArray.get(i);
			//Log.v("Final", one + "/" + two);
			session=timeArray.get(0);

			try {
				d1 = sdf.parse(one);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				d2 = sdf.parse(two);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			float diff = d1.getTime() - d2.getTime();
			diffSeconds = diff / 1000 % 60;
			//Log.v("Intervel@@@@", "" + diffSeconds);
			intervel.add("" + diffSeconds);

		}
		try {
			d3=sdf.parse(session);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		float sessionDiff=d1.getTime()-d3.getTime();
		ssTime=sessionDiff/1000%60;
		//Log.v("SESSION @@@@@", ""+ssTime);
		sessionArray.add(""+ssTime);
	}
	public void onDestroy()
	{
		/*    For XML Analytic code*/
		if(timeArray!=null && timeArray.size()>0)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-d  HH:mm:ss");
			String currentDateandTime = sdf.format(new Date());
			//Log.v("Destroy", currentDateandTime);
			intervel(currentDateandTime);
			setInterval();
			powerButtonPress(pause);
			pause++;
		}
		/*-------------*/

		isCoreNull = true;
		if(mDocView != null && mDocView.getDisplayedView() instanceof PageView){
			pageView = (MuPDFPageView) mDocView.getDisplayedView();
			pageView.releaseResources();
		}

		if (mPDFTask != null /*&& mPDFTask.getStatus() != AsyncTask.Status.FINISHED*/){
			mPDFTask.running = false;
			mPDFTask.cancel(true);
			mPDFTask = null;
		}

		if(mEntire != null)
			mEntire = null;

		if (core != null)
		{
			mDestroyTask= new AsyncTask<Void, Void, Void>()
			{
				@Override
				protected Void doInBackground(Void... params)
				{
					core.onDestroy();
					return null;
				}

				@Override
				protected void onPostExecute(Void result)
				{
					core = null;
					mDestroyTask = null;
				}
			};
			mDestroyTask.execute();
		}

		super.onDestroy();
	}

	void showButtons()
	{
		if (core == null)
			return;
		if (!mButtonsVisible)
		{
			mButtonsVisible = true;

			int index = mDocView.getDisplayedViewIndex();
			updatePageNumView(index);

			Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
			anim.setDuration(500);
			anim.setAnimationListener(new Animation.AnimationListener()
			{
				public void onAnimationStart(Animation animation)
				{
					mTopBarSwitcher.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, mthumbnailGallery.getHeight(), 0);
			anim.setDuration(500);
			anim.setAnimationListener(new Animation.AnimationListener()
			{
				public void onAnimationStart(Animation animation)
				{
					mthumbnailGallery.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation)
				{
					setSelectedPositionThumb();
				}
			});
			mBottomLayout.startAnimation(anim);
		}
	}

	void hideButtons()
	{
		if (mButtonsVisible)
		{
			mButtonsVisible = false;

			Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
			anim.setDuration(500);
			anim.setAnimationListener(new Animation.AnimationListener()
			{
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation)
				{
					mTopBarSwitcher.setVisibility(View.INVISIBLE);
				}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, 0, mthumbnailGallery.getHeight());
			anim.setDuration(500);
			anim.setAnimationListener(new Animation.AnimationListener()
			{
				public void onAnimationStart(Animation animation)
				{
					//mPageNumberView.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation)
				{
					//mBottomLayout.setVisibility(View.INVISIBLE);
					mthumbnailGallery.setVisibility(View.GONE);
				}
			});
			mBottomLayout.startAnimation(anim);
		}}

	void updatePageNumView(int index)
	{
		if (core == null)
			return;

		try
		{
			if(screenOrientation == 1 || (screenOrientation == 2 && isEasyMode))
				mPageNumberView.setText((pageNumbers[index])+"/"+(noOfPages));
			else
			{
				if(index == 0)
					mPageNumberView.setText("1/"+(noOfPages));
				else if(index == ((noOfPages/2)+addPagesCount))
					mPageNumberView.setText(noOfPages+"/"+(noOfPages));
				else
					mPageNumberView.setText(pageNumbersL[index]+"/"+noOfPages);
			}
		}
		catch(Exception e)
		{

		}
	}
	////////////

	@SuppressWarnings("deprecation")
	void makeButtonsView()
	{
			mButtonsView = getLayoutInflater().inflate(R.layout.mupdfactivity_buttons_mobile, null);
			mFilenameView = mButtonsView.findViewById(R.id.docNameText);
			mFilenameView.setEllipsize(TruncateAt.MARQUEE);
			mFilenameView.setSelected(true);

			if(screenOrientation == 1)
				mFilenameView.getLayoutParams().width=180;


		mPageNumberView = mButtonsView.findViewById(R.id.pageNumber1);
		mTopBarSwitcher = mButtonsView.findViewById(R.id.switcher);
		mTopBarSwitcher.setVisibility(View.INVISIBLE);
		mDownloadedPercentage = mButtonsView.findViewById(R.id.downloadShow);

		Button mCloseButton = mButtonsView.findViewById(R.id.closeButton);
		mthumbnailGallery = mButtonsView.findViewById(R.id.gallery_thumbnails);
		mBtnSubscribe= mButtonsView.findViewById(R.id.btn_subscribe);
		mImageAdapter = new ImageAdapter(this);
		mthumbnailGallery.setAdapter(mImageAdapter);
		prefs = getPreferences(Context.MODE_PRIVATE);
		mBottomLayout = mButtonsView.findViewById(R.id.lowerButtons);
		mInteractiveButton.setVisibility(View.GONE);

		bModeSwitcher = mButtonsView.findViewById(R.id.toggleButton1);
		bModeSwitcher.setChecked(false);

		if(screenOrientation == 2)
			bModeSwitcher.setVisibility(View.VISIBLE);
		else
			bModeSwitcher.setVisibility(View.GONE);


		mthumbnailGallery.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				isThumbClicked = true;
				ReaderView.mScale = ReaderView.MIN_SCALE;

				mDocView.resetupChildren();
				int mAdder = 0;
				int counter= 0;
				if(addPagesCount == 0)
					mAdder = 0;
				else
				{
					for (int k = 0; k < arg2; k++)
					{
						try
						{
							Integer.parseInt(pageNumbers[k]);
						}
						catch (Exception e)
						{
							counter +=1;
							if(counter % 2!= 0)
								mAdder+=1;
						}
					}
				}
				if(screenOrientation == 1 || (screenOrientation == 2  && isEasyMode))
					mDocView.setDisplayedViewIndex(arg2);
				else
				{
					if((arg2 % 2) != 0)// odd
						mDocView.setDisplayedViewIndex((arg2/2) +1+(counter>1?mAdder:0));
					else// even
						mDocView.setDisplayedViewIndex((arg2/2)+mAdder);
				}
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-d  HH:mm:ss");
				String currentDateandTime = sdf.format(new Date());
				intervel(currentDateandTime);
			}
		});


		mCloseButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putBoolean("isConfigChanged", false);
				edit.putInt("currentOrientation"+ magazineId + editionId, screenOrientation);
				edit.commit();
				finish();
			}
		});




		bModeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					// The toggle is enabled
					isEasyMode = true;
					ReaderView.MIN_SCALE = 2.1f;
				}
				else
				{
					// The toggle is disabled
					isEasyMode = false;
					ReaderView.MIN_SCALE = 1.0f;
				}
				refreshViewForEasyMode();
			}
		});
	}


	//comments


	public void timerDelayRemoveDialog(long time, final Dialog d){
		new Handler().postDelayed(new Runnable() {
			public void run() {                
				d.dismiss();         
			}
		}, time); 
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mButtonsVisible && !mTopBarIsSearch) {
			hideButtons();
		} else {
			showButtons();
			//			searchModeOff();
		}
		return super.onPrepareOptionsMenu(menu);
	}


	@SuppressLint("NewApi")
	public class LoadPDFAsyncTask extends AsyncTask<Void, String, Void>
	{
		boolean running = true;
		int tempPreviewPage = 0;

		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			for (int i = 0; i < (noOfPages); i++) 
			{
				if(running)
				{
					/********** CHECK FOR PDF ESXIST *****/
					File file = null;

						if(i == 6)
							publishProgress("-3");
						else if(i == (noOfPages)-1)
							publishProgress("-4");


						file = new File(path, "" + (i) + ".pdf");


					/********** CHECK FOR IMAGE ESXIST *****/
					if (file.exists())
					{
						try 
						{



								if (!(new File(path + "/" + i).exists()) && !(isCoreNull)) {
									//IMAGE DOESNT EXIST SO RENDER
									mCurrentDownloadingpage = i;

									createImageFromPDF(i);
								} else if (!(isCoreNull)) {
									//IMAGE EXIST
									File f = new File(path + "/" + i);
									if (f.length() == 0) {
										f.delete();

										createImageFromPDF(i);
									} else {
										core.getPageSize(i, 1);
										core.getPageLinks(i);

										if (!(new File(path + "/" + i + "_1").exists()) && !(isCoreNull))
											storeThumbImage(null, i);
									}
									f = null;

//									publishProgress("-2");
								}
							}

						catch (Exception e)
						{
//							publishProgress("-2");
						}
					}
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values)
		{
			super.onProgressUpdate(values);

			if(values != null && values.length > 1)
			{
				if(Integer.parseInt(values[0]) > currentPage)
				{
					if(mButtonsVisible)
					{
						mImageAdapter.notifyDataSetChanged();
						mthumbnailGallery.post( new Runnable()
						{
							@Override
							public void run()
							{
								//highLightGallery(adPageCount());
							}
						});
					}
				}

				currentProgressLength = Float.parseFloat(values[0]) *(((float)100/noOfPages));
				float mPercentage =(float) Integer.parseInt(values[1]) / Integer.parseInt(values[2]);
				float currentPercentage = mPercentage *(((float)100/noOfPages));
				if ((int)(currentPercentage+currentProgressLength) > downloadingPercentage)
				{
					mPageNumberView.setVisibility(View.INVISIBLE);
					downloadingPercentage = (int)(currentPercentage+currentProgressLength);
						mDownloadedPercentage.setText(""+ downloadingPercentage +" %");
				}

				setIndividualPageProgress(values[0], values[1], values[2]);

				currentPage = Integer.parseInt(values[0]);
				if(downloadingPercentage == 100 ||mDownloadedPercentage.getText().equals("Loading 99%"))
				{
					mDownloadedPercentage.setVisibility(View.INVISIBLE);
					mPageNumberView.setVisibility(View.VISIBLE);
				}

			}
			else if(values[0].equals("-1"))
			{
				if(screenOrientation == 1 || (screenOrientation == 2  && isEasyMode)) // portrait
					mDocView.refreshView(pdfAdapter,currentPage);
				else if(currentPage == noOfPages -1){ // last page
					mDocView.refreshView(pdfAdapter,(noOfPages/2));
				}
				else
				{
					if(currentPage % 2 ==0)
					{ // even number pages
						mDocView.refreshView(pdfAdapter, currentPage/2);
					}
				}
//				pdfAdapter.notifyDataSetChanged();


					page_no = mCurrentDownloadingpage;

				if(screenOrientation == 1 || (screenOrientation == 2  && isEasyMode))
				{
					if(page_no == mDocView.getDisplayedViewIndex() && !(mDocView.getDisplayedView() instanceof WebPageView))
						((MuPDFPageView)mDocView.getDisplayedView()).update();
				}
				else
				{
					if(page_no == (mDocView.getDisplayedViewIndex() * 2) && !(mDocView.getDisplayedView() instanceof WebPageView))
						((MuPDFPageView)mDocView.getDisplayedView()).update();
				}
			}
			else if (values[0].equals("-2"))
				pdfAdapter.notifyDataSetChanged();
			else if(values[0].equals("-3"))
			{
				/*String param[] = { "0","",editionId, pref.getString(Constants.PREF_COUNTRY_CODE, "US"),"Android","","",pref.getString(Constants.PREF_USER_ID, "0"),""};
				mSoapParseDownloadStarted = new SoapParseDownloadStarted();
				mSoapParseDownloadStarted.executeOnExecutor(new ThreadPerTaskExecutor(),param);*/
			}
			else if(values[0].equals("-4"))
			{
				/*String param[] = {  "0","",editionId, pref.getString(Constants.PREF_COUNTRY_CODE, "US"),"Android","","",pref.getString(Constants.PREF_USER_ID, "0"),""};
				mSoapParseDownloadCompleted = new SoapParseDownloadCompleted();
				mSoapParseDownloadCompleted.executeOnExecutor(new ThreadPerTaskExecutor(), param);*/
			}
		}

		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);

			mDownloadedPercentage.setVisibility(View.GONE);
			mPageNumberView.setVisibility(View.VISIBLE);

			pdfAdapter.notifyDataSetChanged();
			mImageAdapter.notifyDataSetChanged();

			if(mEntire != null)
				mEntire = null;

			HTML_Page_no.clear();
			HTML_Page_no = null;
		}

		private void setIndividualPageProgress(String values0, String values1, String values2)
		{
			if((!(mDocView.getDisplayedView() instanceof WebPageView)) && (mDocView.getDisplayedView() != null))
			{
				String s= ""+((mDocView.getDisplayedViewIndex()));

				float singlePage_currentProgressLength = Float.parseFloat(values0);//*(((float)100/2));
				float singlePage_mPercentage =(float) Integer.parseInt(values1) / Integer.parseInt(values2);
				float singlePage_currentPercentage = singlePage_mPercentage *(((float)100/1));

				int status = (int)(singlePage_currentPercentage+singlePage_currentProgressLength);
				status = status - 2;
				if(status <= 0)
					status = 0;

				if(screenOrientation == 1 || (screenOrientation == 2 && isEasyMode))
				{
					if(Integer.parseInt(s) == Integer.parseInt(values0))
						if(status < 100)
							((MuPDFPageView)mDocView.getDisplayedView()).updateProgress(status, 1, true, ((mDocView.getDisplayedViewIndex())));
				}
				else
				{
					int mLeftView = (mDocView.getDisplayedViewIndex() *2) - 1;
					int mRightView = (mDocView.getDisplayedViewIndex() *2);
					if((mLeftView) == Integer.parseInt(values0))
					{
						if(status < 100)
							((MuPDFPageView)mDocView.getDisplayedView()).updateProgress(status, 1, true, mLeftView);//1,true ==> left view progress
					}
					else if((mRightView) == Integer.parseInt(values0))
					{
						if(status < 100)
							((MuPDFPageView)mDocView.getDisplayedView()).updateProgress(status, 1, false, mLeftView);//1,false ==> right view progress
					}
				}
			}
		}

		private void createImageFromPDF(int i)
		{
			if(mEntire != null)
			{
				try
				{
					mEntire.setBm(null);
					PointF mSize = core.getPageSize(i, 1);
					
					sourceScale = Math.min(screenWidthForView/mSize.x, screenHeightForView/mSize.y);
					viewSize	= new PointF((int)(mSize.x * sourceScale), (int)(mSize.y * sourceScale));
					
					if(viewSize.x > mSize.x || viewSize.y > mSize.y)
						mSize = viewSize;
/*											if(magazineId.equals("1318") || magazineId. equals("4801")) //Handled for Modelz view
						core.drawPage(mEntire, i, (int) (mSize.x)/2, (int) (mSize.y)/2, 0, 0, (int) (mSize.x)/2, (int) (mSize.y)/2, 1);
					else
//*/											
					core.drawPage(mEntire, i, (int) (mSize.x), (int) (mSize.y), 0, 0, (int) (mSize.x) , (int) (mSize.y), 1);
					core.getPageLinks(i);

					storeImage(mEntire.getBm(), i);
					
					mEntire.setBm(null);
				}
				catch(Exception e)
				{
//					publishProgress("-2");
				}
			}
		}

		synchronized private void storeImage(Bitmap mEntireBm, int thumbnailName)
		{
			FileOutputStream fileOutputStream = null;
			File file = new File(path, "" + thumbnailName);
			try 
			{
				fileOutputStream = new FileOutputStream(file);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			mEntireBm.compress(CompressFormat.JPEG, 100, fileOutputStream);
			try 
			{
				fileOutputStream.flush();
				fileOutputStream.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}

			if(file.length() == 0)
				file.delete();

			File thumbImage = new File(path + "/" + thumbnailName + "_1");

			if(!(thumbImage.exists()))
				storeThumbImage(mEntireBm, thumbnailName);
			else
			{
				if(thumbImage.length() == 0)
				{
					thumbImage.delete();
					storeThumbImage(mEntireBm, thumbnailName);
				}
			}
			
			thumbImage = null;
			
			mEntireBm.recycle();
			mEntireBm = null;

			publishProgress("-1");
		}

		synchronized private void storeThumbImage(Bitmap mBitmap, int pageNo)
		{
			// For Thumbnail scale down to small image
			FileOutputStream fileOutputStream 	= null;
			Bitmap mTempBitmap		= null;
			Bitmap mThumb				= null;
			boolean              hastoRecycle		= false;

			if(mBitmap == null)
			{
				mTempBitmap = BitmapFactory.decodeFile(path + "/" + pageNo);
				hastoRecycle = true;
			}
			else
				mTempBitmap = mBitmap;

			File thumbimage = new File(path, "" + pageNo + "_1");
			try 
			{
				fileOutputStream = new FileOutputStream(thumbimage);
			} 
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}

			try
			{
				mThumb = Bitmap.createScaledBitmap(mTempBitmap, (int)(mTempBitmap.getWidth() / 7.0), (int)(mTempBitmap.getHeight() / 7.0), false);
			}
			catch(OutOfMemoryError e)
			{
				System.gc();
				return;
			}

			mThumb.compress(CompressFormat.WEBP, 100, fileOutputStream);
			try 
			{
				fileOutputStream.flush();
				fileOutputStream.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}

			if(thumbimage.length() == 0)
				thumbimage.delete();

			mThumb.recycle();
			mThumb = null;

			if(hastoRecycle)
			{
				mTempBitmap.recycle();
				mTempBitmap = null;
			}
		}


		private String getFileNameFromUrl(String url)
		{
			int index = url.lastIndexOf('?');
			String filename;
			if (index > 1)
				filename = url.substring(url.lastIndexOf('/') + 1, index);
			else
				filename = url.substring(url.lastIndexOf('/') + 1);

			return filename;
		}

	}

	class ImageAdapter extends BaseAdapter
	{
		private Context mContext;
		public LayoutInflater mInflater;
		ViewHolder holder;

		public ImageAdapter(Context c)
		{
			mContext = c;
			mInflater = LayoutInflater.from(mContext);
		}

		public int getCount()
		{
			return noOfPages + addPagesCount;
		}

		public Object getItem(int position)
		{
			return position;
		}

		public long getItemId(int position) 
		{
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null) 
			{
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.mupdfactivity_thumbnail, null);
				holder.layout_thumbnail_gallery = convertView.findViewById(R.id.layout_thumnail_gallery);
				holder.thumb_image = convertView.findViewById(R.id.thumb_image);
				holder.imageLayoutParams = new LinearLayout.LayoutParams((int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics())), (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics())));// (screenWidth/6,screenHeight/8);
				holder.imageLayoutParams.topMargin = 5;
				holder.layoutParams = new LinearLayout.LayoutParams((int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics())), (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 130, getResources().getDisplayMetrics()))); // ((screenWidth/6)+10,android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
				holder.page_no = convertView.findViewById(R.id.page_no1);

				convertView.setTag(holder);

				try
				{

						holder.page_no.setText("" + pageNumbers[position]);

					{ // handled mainly for interactive pages
						if (Integer.parseInt(pageNumbers[position]) % 2 != 0)
						{
							// Even
							if(position == 0)
							{
									holder.thumb_image.setPadding(5, 5, 5, 5);
							}
							else
							{
									holder.thumb_image.setPadding(0, 5, 5, 5);
							}
							holder.imageLayoutParams.gravity = Gravity.LEFT;
						} 
						else
						{
							// ODD
							if(position == (pageNumbers.length - 1))
							{
									holder.thumb_image.setPadding(5, 5, 5, 5);
							}
							else
							{
									holder.thumb_image.setPadding(5, 5, 0, 5);
							}
							holder.imageLayoutParams.gravity = Gravity.RIGHT;
						}
					}

					holder.layout_thumbnail_gallery.setLayoutParams(holder.layoutParams);
					holder.thumb_image.setLayoutParams(holder.imageLayoutParams);
					imageLoader.displayImage(thumbNailUrls[position], holder.thumb_image, options);
				}
				catch(Exception e)
				{

				}
			}

			return convertView;
		}
	}

	public static class ViewHolder
	{
		private LinearLayout layout_thumbnail_gallery;
		private ImageView thumb_image;
		private TextView page_no;
		private LinearLayout.LayoutParams imageLayoutParams;
		private LinearLayout.LayoutParams layoutParams;
		private TextView subscribeNoOfIssue,subscribeIssueTime;
        Button subscribeIssue ;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			ReaderView.mScale = ReaderView.MIN_SCALE;
			mDocView.moveToPrevious();
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			ReaderView.mScale = ReaderView.MIN_SCALE;
			mDocView.moveToNext();
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_BACK)
		{

			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		int viewToBeDisplayed = 0;
		int adcount			  = 0;
		int oldConfig		  = 0; //1 - Portrait //2 - Landscape
		int currentpos		  = 0;
		isEasyMode		      = false;
		ReaderView.MIN_SCALE    = 1.0f;
		ReaderView.mScale       = ReaderView.MIN_SCALE;

		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		screenHeight = metrics.heightPixels;
		screenWidth  = metrics.widthPixels;

		bModeSwitcher.setChecked(false);

		if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			screenOrientation = 1; //portrait
			oldConfig		  = 2;
			bModeSwitcher.setVisibility(View.GONE);
		}
		else
		{
			screenOrientation = 2;// landscape
			oldConfig         = 1;
			bModeSwitcher.setVisibility(View.VISIBLE);
		}


		{
			if(oldConfig == 2)
				currentpos = (mDocView.getDisplayedViewIndex() * 2) -1;
			else
				currentpos = mDocView.getDisplayedViewIndex();
			for (int k = 0; k < currentpos; k++)
			{
				try
				{
					Integer.parseInt(pageNumbers[k]);
				} 
				catch (Exception e)
				{
					adcount +=1;
				}
			}
		}

		if(mDocView.getDisplayedViewIndex() == 0)
		{
			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex());
			pdfAdapter.notifyDataSetChanged();

			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()+1);
			pdfAdapter.notifyDataSetChanged();
		}
		else
		{
			if(screenOrientation == 2)
			{
				if(mDocView.getDisplayedViewIndex() == (noOfPages+addPagesCount) / 2)
				{
					mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex());
					pdfAdapter.notifyDataSetChanged();

					mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()-1);
					pdfAdapter.notifyDataSetChanged();
				}
			}
			else
			{
				mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex());
				pdfAdapter.notifyDataSetChanged();

				mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()-1);
				pdfAdapter.notifyDataSetChanged();

				mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()+1);
				pdfAdapter.notifyDataSetChanged();
			}
		}

		if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			viewToBeDisplayed = ((mCurrentDisplayingPage * 2) - 1) - adcount;

			if(viewToBeDisplayed < 0)
				viewToBeDisplayed = 0;

			mDocView.setDisplayedViewIndex(viewToBeDisplayed);
		}
		else
		{
			if((mCurrentDisplayingPage - adcount) % 2 == 0)
				viewToBeDisplayed = (((mCurrentDisplayingPage - adcount) / 2)) + adcount;
			else
				viewToBeDisplayed = (((mCurrentDisplayingPage - adcount) / 2) + 1) + adcount;

			mDocView.setDisplayedViewIndex(viewToBeDisplayed);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-d  HH:mm:ss");
		String currentDateandTime = sdf.format(new Date());
		intervel(currentDateandTime);
	}

	private void refreshViewForEasyMode()
	{
		int viewToBeDisplayed = 0;

		ReaderView.mScale = ReaderView.MIN_SCALE;
		screenOrientation = 2;

		int adcount = 0;

		{
			for (int k = 0; k < (!(isEasyMode) ? mDocView.getDisplayedViewIndex():((mDocView.getDisplayedViewIndex()* 2)-1)); k++)
			{
				try
				{
					Integer.parseInt(pageNumbers[k]);
				} 
				catch (Exception e)
				{
					adcount +=1;
				}
			}
		}

		if(mDocView.getDisplayedViewIndex() == 0)
		{
			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex());
			pdfAdapter.notifyDataSetChanged();

			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()+1);
			pdfAdapter.notifyDataSetChanged();
		}
		else
		{
			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex());
			pdfAdapter.notifyDataSetChanged();

			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()-1);
			pdfAdapter.notifyDataSetChanged();

			mDocView.refreshView(pdfAdapter, mDocView.getDisplayedViewIndex()+1);
			pdfAdapter.notifyDataSetChanged();
		}

		if(isEasyMode)
		{
			viewToBeDisplayed = ((mCurrentDisplayingPage * 2) - 1) - adcount;
			if(viewToBeDisplayed < 0)
				viewToBeDisplayed = 0;

			mDocView.setDisplayedViewIndex(viewToBeDisplayed);
		}
		else
		{
			if((mCurrentDisplayingPage - adcount) % 2 == 0)
				viewToBeDisplayed = (((mCurrentDisplayingPage - adcount) / 2)) + adcount;
			else
				viewToBeDisplayed = (((mCurrentDisplayingPage - adcount) / 2) + 1) + adcount;

			mDocView.setDisplayedViewIndex(viewToBeDisplayed);
		}
	}




	public void intervel(String time) {

		intervelArray.add("" + time);
	}

	public void shareArrayList(String date) {
		pageShareArray.add("" + date);
		if (screenOrientation == 1 || (screenOrientation == 2 && isEasyMode))
			OrientationArraySharedPage.add("" + 1);
		else
			OrientationArraySharedPage.add("" + 2);

	}

	public void powerButtonPress(int key) {
		orignalArray = (ArrayList<String>) mainArray.clone();
		SimpleDateFormat sdf = new SimpleDateFormat(
				"yyyy-MMM-d  HH:mm:ss");
		String currentDateandTime = sdf.format(new Date());
		// hashmap = new HashMap<ArrayList<String>, ArrayList<String>>();
		PageReadEnity pageEnity = new PageReadEnity();
		pageEnity.interval = (ArrayList<String>) intervel.clone();
		pageEnity.FirstPage = (ArrayList<String>) orignalArray.clone();
		pageEnity.time=(ArrayList<String>) timeArray.clone();
		pageEnity.isInteractive=(ArrayList<String>) pageTypeArray.clone();
		pageEnity.InteractiveTitle=(ArrayList<String>)titleArray.clone();
		pageEnity.SecondPage=(ArrayList<String>)secondPageNoArray.clone();
		pageEnity.sessionTime=(ArrayList<String>)sessionArray.clone();
		pageEnity.sessionCurrentTime=currentDateandTime;
		pageEnity.mPageShare=(ArrayList<String>)pageShareArray.clone();
		pageEnity.mLinkTap=(ArrayList<String>)LinkMainArray.clone();
		pageEnity.mLinkTapTime=(ArrayList<String>)LinkTime.clone();
		pageEnity.mVideoTap=(ArrayList<String>)VideoMainArray.clone();
		pageEnity.mVideoTapTime=(ArrayList<String>)videoTapTime.clone();
		pageEnity.mOrientation=(ArrayList<String>)OrientationArray.clone();
		pageEnity.mOrientationSharedPage=(ArrayList<String>)OrientationArraySharedPage.clone();
		hashmap.put(key, pageEnity);
		clearArray();
		for (Map.Entry<Integer, PageReadEnity> entry : hashmap
				.entrySet()) {
			key2 = entry.getKey();
			values = entry.getValue();

		}

	}
	public void clearArray(){
		mainArray.clear();
		intervel.clear();
		timeArray.clear();
		pageTypeArray.clear();
		titleArray.clear();
		secondPageNoArray.clear();
		sessionArray.clear();
		pageShareArray.clear();
		LinkMainArray.clear();
		LinkTime.clear();
		VideoMainArray.clear();
		videoTapTime.clear();
		OrientationArray.clear();
		OrientationArraySharedPage.clear();
	}
}