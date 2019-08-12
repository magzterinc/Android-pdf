package com.artifex.mupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artifex.mupdf.R;

import java.io.File;

class PatchInfo 
{
	public BitmapHolder bm;
	public Point patchViewSize;
	public Rect patchArea;
	public boolean completeRedraw;

	public PatchInfo(Point aPatchViewSize, Rect aPatchArea, BitmapHolder aBm, boolean aCompleteRedraw)
	{
		bm = aBm;
		patchViewSize = aPatchViewSize;
		patchArea = aPatchArea;
		completeRedraw = aCompleteRedraw;
	}
}

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends android.support.v7.widget.AppCompatImageView
{
	public OpaqueImageView(Context context)
	{
		super(context);
	}

	@Override
	public boolean isOpaque() 
	{
		return true;
	}
}

public abstract class PageView extends ViewGroup
{
	private static final int HIGHLIGHT_COLOR = 0x802572FF;
	private static final int BACKGROUND_COLOR = 0xFF000000;
	private final Context mContext;
	protected     int       mPageNumber;
	protected 	  int       mPosition;
	private Point mParentSize;
	protected Point mSize;   // Size of page at minimum zoom
	protected     float     mSourceScale;
	public ImageView mEntire; // Image rendered at minimum zoom
	private       PageAnnots mGetLinkInfo;
	private AsyncTask<Void, Void,LinkInfo[]> mGetLinkInfoLeft;
	private       AsyncTask<Void, Void, Bitmap> mRenderLowQualityTask;
	private Point mPatchViewSize; // View size on the basis of which the patch was created
	private Rect mPatchArea;
	private ImageView mPatch;
	public	      BitmapHolder mEntireBm;
	private       BitmapHolder mPatchBm;
	private       AsyncTask<PatchInfo, Void, PatchInfo> mDrawPatch;
	private       LinkInfo  mLinks[];
	private       LinkInfo  mLinksLeft[];
	private       boolean   mHighlightLinks;
	private 	  boolean   mLowQualityFlag, mPageRendered, mHighQualityTaskInterruptedFlag, mHighQualityTaskStarted;
	private View mHighLightLink;
	
	private       RenderHighQuality mRenderHighQualityTask;

	private LinearLayout mLayout;
	
	private TextView txtProgressLft, txtProgressRht;
	private MuPDFActivity mActivity;
	private MuPDFCore mCore;
	
	private BitmapFactory.Options mOptions;

	private String mPath;
	
	private highLightLinkTimer highLightTimer;
	private Paint paint;
	
	@SuppressWarnings("deprecation")
	public PageView(Context c, MuPDFCore core, Point parentSize)
	{
		super(c);
		mContext    = c;
		mParentSize = new Point();
		mCore = core;
		setBackgroundColor(BACKGROUND_COLOR);
		mActivity = (MuPDFActivity) c;

		mEntireBm = new BitmapHolder();
		mPatchBm  = new BitmapHolder();
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Config.RGB_565;
		mOptions.inDither = true;

		mLayout = new LinearLayout(mContext);
		mLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
		mLayout.setOrientation(LinearLayout.VERTICAL);
		mLayout.setGravity(Gravity.CENTER | Gravity.CENTER);
		
		paint = new Paint();
		
		if(txtProgressLft == null)
		{
			txtProgressLft=new TextView(mContext);
			txtProgressLft.setText(" Waiting to Download ");
			txtProgressLft.setTextColor(Color.WHITE);
			txtProgressLft.setPadding(7, 2, 7, 3);
			txtProgressLft.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			
			mLayout.addView(txtProgressLft);
		}
		
		if(txtProgressRht == null)
		{
			txtProgressRht=new TextView(mContext);
			txtProgressRht.setText(" Waiting to Download ");
			txtProgressRht.setTextColor(Color.WHITE);
			txtProgressRht.setPadding(7, 2, 7, 3);
			txtProgressRht.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			
			mLayout.addView(txtProgressRht);
		}
		highLightViewLayout();
		addView(mLayout);
	}
	
	protected abstract void updateProgress(int status, int visible, boolean isLft, int page);
	protected abstract void drawSinglePage(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight, String path);
	protected abstract void drawPage(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);
	protected abstract void drawPageForLandscape(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight, int position, String path);
	protected abstract void drawPageForLandscapeZoom(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight, int position);
	protected abstract void getLinkInfo(int pageNumber);

	public void releaseResources()
	{
		// Cancel pending render task
		cancelRunningTask();

		if(mRenderLowQualityTask != null)
		{
			mRenderLowQualityTask.cancel(true);
			mRenderLowQualityTask = null;
		}
		
		if(highLightTimer != null)
			highLightTimer.cancel();
		
		if(mEntire != null)
			recycleImageViewBitmap();
		
		mEntireBm.setBm(null);
		
		mPageNumber = 0;
	}

	public void blankPage(final int page, final int viewPosition) 
	{
		mParentSize.x = getResources().getDisplayMetrics().widthPixels;
		mParentSize.y = getResources().getDisplayMetrics().heightPixels;
		
		// Cancel pending render task
		cancelRunningTask();
		
		if(mRenderLowQualityTask != null)
		{
			mRenderLowQualityTask.cancel(true);
			mRenderLowQualityTask = null;
		}
		
		mEntireBm.setBm(null);
		
		if(highLightTimer != null)
			highLightTimer.cancel();
		
		if(mEntire != null)
			recycleImageViewBitmap();
		
		txtProgressLft.setText(" Waiting to Download ");
		txtProgressRht.setText(" Waiting to Download ");
		
		mPageRendered 					= false;
		mHighlightLinks					= false;
		mLowQualityFlag 				= false;
		mHighQualityTaskInterruptedFlag = false;
		mHighQualityTaskStarted         = false;
		
		mPageNumber 				= page;
		mPosition                   = viewPosition;
		
		if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
			txtProgressRht.setVisibility(View.GONE);
		else
		{
			txtProgressRht.setVisibility(View.VISIBLE);
			
			if(page == 0)
				txtProgressLft.setVisibility(View.GONE);
			else if(viewPosition == (mActivity.noOfPages/2)+MuPDFActivity.addPagesCount)
			{
				txtProgressLft.setVisibility(View.VISIBLE);
				txtProgressRht.setVisibility(View.GONE);
			}
			else
				txtProgressLft.setVisibility(View.VISIBLE);
		}
		
		mHighLightLink.invalidate();
	}
	
	private void cancelRunningTask()
	{
		if (mRenderHighQualityTask != null) 
		{
			mRenderHighQualityTask.cancel(true);
			mRenderHighQualityTask = null;
		}
		
		if (mDrawPatch != null)
		{
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}
		
		if(mGetLinkInfo != null)
		{
			mGetLinkInfo.cancel(true);
			mGetLinkInfo = null;
		}

		if(mGetLinkInfoLeft != null)
		{
			mGetLinkInfoLeft.cancel(true);
			mGetLinkInfoLeft = null;
		}
		
		if (mPatch != null)
		{
			mPatchBm.setBm(null);
			recyclePatchViewBitmap();
		}
		
		if(mLinks != null)
			mLinks = null;
		
		if(mLinksLeft != null)
			mLinksLeft = null;
	}
	
	private void imageViewLayout()
	{
		mEntire = new OpaqueImageView(mContext);
		mEntire.setScaleType(ImageView.ScaleType.FIT_CENTER);
		addView(mEntire);
	}
	
	public void setPageTempImage(final String path, final int page, final int position)
	{
		mPath = path;
		
		mRenderLowQualityTask = new AsyncTask<Void, Void, Bitmap>()
		{
			PointF mPoint;
			@Override
			protected void onPreExecute() 
			{
				super.onPreExecute();
				mLowQualityFlag = false;
				if(mEntire == null)
					imageViewLayout();
			}

			protected Bitmap doInBackground(Void... v)
			{
				if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
				{
					if((new File(path+"/"+page).exists()))
					{
						if(mCore.mPageSizes.get(page) == null)
							mPoint = mCore.getPageSize(page, 1);
						else
							mPoint = mCore.mPageSizes.get(page);

							mOptions.inSampleSize = 7;

						return BitmapFactory.decodeFile(path + "/" + page, mOptions);
					}
					else
						return null;
				}
				else
				{
					Bitmap tempBitmap1 = null;
					Bitmap tempBitmap2 = null;
					Bitmap tempLeftBitmap = null;
					Bitmap tempRightBitmap = null;
					
					float mTempSourceScale;
					Point newSize = null;
					
					int mPage  = 0;
					int mPage1 = 0;
					
					if(mActivity.isPreview)
					{
						int mTempPage1 = 0;
						try 
						{
							mTempPage1 = Integer.parseInt(mActivity.previewPageNumbers[(mPosition*2)]) - 1;
						}
						catch (Exception e)
						{
							try
							{
								mTempPage1 = Integer.parseInt(mActivity.previewPageNumbers[(mPosition*2)-1]);
							}
							catch(Exception e1)
							{
								mTempPage1 = Integer.parseInt(mActivity.previewPageNumbers[(mPosition)-1]);
							}
						}
						
						mPage  = mTempPage1 - 1;
						mPage1 = mTempPage1;
					}
					else
					{
						mPage  = (page * 2) - 1;
						mPage1 = page * 2;
					}
					
					if(page == 0)
					{
						if((new File(path+"/"+page).exists()))
						{
							PointF tempLeft;
							if(mCore.mPageSizes.get(page) == null)
								tempLeft = mCore.getPageSize(page, 1);
							else
								tempLeft = mCore.mPageSizes.get(page);
							
							mPoint = new PointF((tempLeft.x * 2), tempLeft.y);
							
								mOptions.inSampleSize = 9;

							mTempSourceScale = Math.min(mParentSize.x/mPoint.x, mParentSize.y/mPoint.y);
							newSize = new Point((int)(mPoint.x * mTempSourceScale), (int)(mPoint.y * mTempSourceScale));
							
							if((newSize.x < 0 || newSize.y <0) || (newSize.x > mParentSize.x || newSize.y > mParentSize.y))
								return null;
							
							tempBitmap1 = Bitmap.createBitmap(newSize.x/2, newSize.y, Config.RGB_565);
							tempRightBitmap = BitmapFactory.decodeFile(path + "/" + page, mOptions);
							
							if(tempRightBitmap == null)
								tempBitmap2 = Bitmap.createBitmap(newSize.x/2, newSize.y, Config.RGB_565);
							else
							{
								tempBitmap2 = Bitmap.createScaledBitmap(tempRightBitmap, newSize.x/2, newSize.y, true);
								
								tempRightBitmap.recycle();
								tempRightBitmap = null;
							}
						}
						else
							return null;
					}
					else if(position == ((mActivity.noOfPages/2)+MuPDFActivity.addPagesCount)) 
					{
						if((new File(path+"/"+mPage).exists()))
						{
							PointF tempLeft;
							if(mCore.mPageSizes.get(mPage) == null)
								tempLeft = mCore.getPageSize(mPage, 1);
							else
								tempLeft = mCore.mPageSizes.get(mPage);
							
							mPoint = new PointF((tempLeft.x * 2), tempLeft.y);

							mOptions.inJustDecodeBounds = false;
							
								mOptions.inSampleSize = 9;

							mTempSourceScale = Math.min(mParentSize.x/mPoint.x, mParentSize.y/mPoint.y);
							newSize = new Point((int)(mPoint.x*mTempSourceScale), (int)(mPoint.y*mTempSourceScale));
								
							if((newSize.x < 0 || newSize.y <0) || (newSize.x > mParentSize.x || newSize.y > mParentSize.y))
								return null;
							
							tempLeftBitmap = BitmapFactory.decodeFile(path + "/" + mPage, mOptions);
							tempBitmap2 = Bitmap.createBitmap(newSize.x/2, newSize.y, Config.RGB_565);
							
							if(tempLeftBitmap == null)
								tempBitmap1 = Bitmap.createBitmap(newSize.x/2, newSize.y, Config.RGB_565);
							else
							{
								tempBitmap1 = Bitmap.createScaledBitmap(tempLeftBitmap, newSize.x/2, newSize.y, true);
								
								tempLeftBitmap.recycle();
								tempLeftBitmap = null;
							}
						}
						else
							return null;
					}
					else
					{                                                    
						if((new File(path+"/"+mPage).exists()) && (new File(path+"/"+mPage1).exists()))
						{
							PointF tempLeft;
							PointF tempRight;
							
							if(mCore.mPageSizes.get(mPage) == null)
								tempLeft = mCore.getPageSize(mPage, 1);
							else
								tempLeft =mCore.mPageSizes.get(mPage);
							
							if(mCore.mPageSizes.get(mPage1) == null)
								tempRight = mCore.getPageSize(mPage1, 1);
							else
								tempRight = mCore.mPageSizes.get(mPage1);
							
							mPoint = new PointF((tempLeft.x + tempRight.x), tempLeft.y);
							
								mOptions.inSampleSize = 9;

							mTempSourceScale = Math.min(mParentSize.x / mPoint.x, mParentSize.y / mPoint.y);
							newSize = new Point((int)(mPoint.x * mTempSourceScale), (int)(mPoint.y * mTempSourceScale));
							
							if((newSize.x < 0 || newSize.y < 0) || (newSize.x > mParentSize.x || newSize.y > mParentSize.y))
								return null;
							
							tempLeftBitmap  = BitmapFactory.decodeFile(path + "/" + mPage, mOptions);
							tempRightBitmap = BitmapFactory.decodeFile(path + "/" + mPage1, mOptions);
							
							if(tempLeftBitmap == null)
								tempBitmap1 = Bitmap.createBitmap(newSize.x / 2, newSize.y, Config.RGB_565);
							else
							{
								tempBitmap1 = Bitmap.createScaledBitmap(tempLeftBitmap, newSize.x / 2, newSize.y, true);
								tempLeftBitmap.recycle();
								tempLeftBitmap = null;
							}
							
							if(tempRightBitmap == null)
								tempBitmap2 = Bitmap.createBitmap(newSize.x / 2, newSize.y, Config.RGB_565);
							else
							{
								tempBitmap2 = Bitmap.createScaledBitmap(tempRightBitmap, newSize.x / 2, newSize.y, true);
								tempRightBitmap.recycle();
								tempRightBitmap = null;
							}
						}
						else
							return null;
					}
					
					if(tempBitmap1 == null || tempBitmap2 == null)
						return null;
						
					return mergeBitmaps(tempBitmap1, tempBitmap2, newSize.x, newSize.y);
				}
			}
			
	
			@Override
			protected void onPostExecute(Bitmap result)
			{
				super.onPostExecute(result);
				
				if(result != null)
				{
					setPageSize(mPoint);
					mEntire.setImageBitmap(result);
					mLowQualityFlag = true;
					if(mHighQualityTaskInterruptedFlag)
						update();
				}
				else
					setPageSize(new PointF(mParentSize.x, mParentSize.y));
				
				mRenderLowQualityTask = null;
			}
		};
		
		if(mActivity.isThumbClicked && (mActivity.screenOrientation == 1 || (mActivity.screenOrientation ==  2 && mActivity.isEasyMode)))
		{
			if(!mActivity.isCoreNull)
			{
				mActivity.isThumbClicked = false;
				if(new File(path + "/" + page).exists())
				{
					PointF mPoint;
					if(mEntire == null)
						imageViewLayout();
					recycleImageViewBitmap();

					if(mCore.mPageSizes.get(page) == null)
						mPoint = mCore.getPageSize(page, 1);
					else
						mPoint = mCore.mPageSizes.get(page);

					mOptions.inJustDecodeBounds = false;

						mOptions.inSampleSize = 7;

					Bitmap b = BitmapFactory.decodeFile(path + "/" + page, mOptions);

					if(b != null)
					{
						setPageSize(mPoint);

						mEntire.setImageBitmap(b);
						mLowQualityFlag = true;

						if(mHighQualityTaskInterruptedFlag)
							update();
					}
				}
			}
		}
		else
			if(!mActivity.isCoreNull)
				mRenderLowQualityTask.executeOnExecutor(new ThreadPerTaskExecutor());
	}
	
	private void recycleImageViewBitmap()
	{
		Drawable drawable = mEntire.getDrawable();
		if (drawable instanceof BitmapDrawable)
		{
		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		    Bitmap bitmap = bitmapDrawable.getBitmap();
		    if(bitmap != null)
		    {
		    	bitmap.recycle();
		    	bitmap = null;
		    }
		}
		mEntire.setImageBitmap(null);
	}
	
	private void recyclePatchViewBitmap()
	{
		Drawable drawable = mPatch.getDrawable();
		if (drawable instanceof BitmapDrawable)
		{
		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		    Bitmap bitmap = bitmapDrawable.getBitmap();
		    if(bitmap != null)
		    {
		    	bitmap.recycle();
		    	bitmap = null;
		    }
		}
		mPatch.setImageBitmap(null);
	}
	
	private void setPageSize(PointF size)
	{
		mSourceScale = Math.min(mParentSize.x / size.x, mParentSize.y / size.y);
		Point newSize = new Point((int)(size.x * mSourceScale), (int)(size.y * mSourceScale));
		
		mSize = newSize;
		
		requestLayout();
	}
	
	private void highLightViewLayout()
	{
		if (mHighLightLink == null)
		{
			mHighLightLink = new View(mContext)
			{
				@Override
				protected void onDraw(Canvas canvas)
				{
					super.onDraw(canvas);
					float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
					
					if( mHighlightLinks )
						paint.setColor(HIGHLIGHT_COLOR);
					else
						paint.setColor(Color.TRANSPARENT);
					
					if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
					{
						if (mLinks != null) 
						{
							// Work out current total scale factor
							// from source to view
							for (int i = 0; i < mLinks.length; i++)
							{
								RectF rect = mLinks[i];
								canvas.drawRect(rect.left*scale, rect.top*scale, rect.right*scale, rect.bottom*scale, paint);
								
								if(mLinks[i].url.startsWith("MEDIA") && mLinks[i].url.contains("mgautoplay"))
									mActivity.checkForAutoPlayOfVideo(mLinks[i].url, mPageNumber);
							}
						}
					}
					else
					{
						if(mLinksLeft != null)
						{
							for (RectF rect : mLinksLeft)
								canvas.drawRect(rect.left*scale, rect.top*scale, rect.right*scale, rect.bottom*scale, paint);
						}
						if(mLinks != null)
						{
							for (int i = 0; i < mLinks.length; i++)
							{
								RectF rect = mLinks[i];
								canvas.drawRect((mSize.x / 2) + rect.left*scale, rect.top*scale, (mSize.x / 2) + rect.right*scale, rect.bottom*scale, paint);
								
								if(mLinks[i].url.startsWith("MEDIA") && mLinks[i].url.contains("mgautoplay"))
									mActivity.checkForAutoPlayOfVideo(mLinks[i].url, mPageNumber * 2);
							}
						}
					}
				}
			};

			addView(mHighLightLink);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{
		if (mSize == null)
			mSize = mParentSize;
		
		int x, y;
		switch(View.MeasureSpec.getMode(widthMeasureSpec))
		{
		case View.MeasureSpec.UNSPECIFIED:
			x = mSize.x;
			break;
		default:
			x = View.MeasureSpec.getSize(widthMeasureSpec);
		}

		switch(View.MeasureSpec.getMode(heightMeasureSpec))
		{
		case View.MeasureSpec.UNSPECIFIED:
			y = mSize.y;
			break;
		default:
			y = View.MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);

		if( txtProgressLft != null )
		{
			int  limit = Math.min(mParentSize.x, mParentSize.y)/2;
			txtProgressLft.measure(limit, View.MeasureSpec.AT_MOST | limit);
		}

		if (txtProgressRht != null)
		{
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			txtProgressRht.measure(limit, View.MeasureSpec.AT_MOST | limit);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		int w  = right-left;
		int h = bottom-top;

		if(mLayout != null)
			mLayout.layout(0,0,w,h);

		if (mHighLightLink != null)
			mHighLightLink.layout(0, 0, w, h);

		if(txtProgressLft != null)
		{
			int bw1 = txtProgressLft.getMeasuredWidth();
			int bh1 = txtProgressLft.getMeasuredHeight();

			if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
				txtProgressLft.layout(((w-bw1)/2), ((h-bh1)/2), ((w+bw1)/2), ((h+bh1)/2));
			else
				txtProgressLft.layout((w/4-bw1/2), (h/2-bh1/2), (w/4+bw1/2), (h/2+bh1/2));
		}

		if(txtProgressRht != null)
		{
			int bw1 = txtProgressRht.getMeasuredWidth();
			int bh1 = txtProgressRht.getMeasuredHeight();

			txtProgressRht.layout((3*w/4 - bw1/2), (h/2-bh1/2), (3*w/4+bw1/2), (h/2+bh1/2));
		}

		if (mEntire != null)
			mEntire.layout(0, 0, w, h);

		if (mPatchViewSize != null)
		{
			if (mPatchViewSize.x != w || mPatchViewSize.y != h)
			{
				// Zoomed since patch was created
				mPatchViewSize = null;
				mPatchArea     = null;
				if (mPatch != null)
				{
					mPatchBm.setBm(null);
//					mPatch.setImageBitmap(null);
					recyclePatchViewBitmap();
				}
			}
			else
				mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
		}
	}

	public void addHq(boolean update)
	{
		Rect viewArea = new Rect(getLeft(),getTop(),getRight(),getBottom());
		// If the viewArea's size matches the unzoomed size, there is no need for an hq patch
		if (viewArea.width() != mSize.x || viewArea.height() != mSize.y)
		{
			Point patchViewSize = new Point(viewArea.width(), viewArea.height());
			Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

			// Intersect and test that there is an intersection
			if (!patchArea.intersect(viewArea))
				return;

			// Offset patch area to be relative to the view top left
			patchArea.offset(-viewArea.left, -viewArea.top);

			boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

			// If being asked for the same area as last time and not because of an update then nothing to do
			if (area_unchanged && !update)
				return;

			boolean completeRedraw = !(area_unchanged && update);

			// Stop the drawing of previous patch if still going
			if (mDrawPatch != null)
			{
				mDrawPatch.cancel(true);
				mDrawPatch = null;
			}

			if (completeRedraw) {
				// The bitmap holder mPatchBm may still be rendered to by a
				// previously invoked task, and possibly for a different
				// area, so we cannot risk the bitmap generated by this task
				// being passed to it
				mPatchBm.setBm(null);
				mPatchBm = new BitmapHolder();
			}

			// Create and add the image view if not already done
			if (mPatch == null) {
				mPatch = new OpaqueImageView(mContext);
				mPatch.setScaleType(ImageView.ScaleType.FIT_CENTER);
				addView(mPatch);
			}

			mDrawPatch = new AsyncTask<PatchInfo, Void, PatchInfo>()
			{
				protected PatchInfo doInBackground(PatchInfo... v)
				{
					if (v[0].completeRedraw)
					{
						if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
							drawPage(v[0].bm, v[0].patchViewSize.x, v[0].patchViewSize.y, v[0].patchArea.left, v[0].patchArea.top, v[0].patchArea.width(), v[0].patchArea.height());
						else
							drawPageForLandscapeZoom(v[0].bm, v[0].patchViewSize.x, v[0].patchViewSize.y, v[0].patchArea.left, v[0].patchArea.top, v[0].patchArea.width(), v[0].patchArea.height(), mPosition);
					}

					return v[0];
				}

				protected void onPostExecute(PatchInfo v)
				{
					if (mPatchBm == v.bm)
					{
						mPatchViewSize = v.patchViewSize;
						mPatchArea     = v.patchArea;
						if(v.bm.getBm() != null)
							mPatch.setImageBitmap(v.bm.getBm());
						//requestLayout();
						// Calling requestLayout here doesn't lead to a later call to layout. No idea
						// why, but apparently others have run into the problem.
						mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
					}
				}
			};

			if(!(mActivity.isCoreNull) && mLowQualityFlag)
				mDrawPatch.execute(new PatchInfo(patchViewSize, patchArea, mPatchBm, completeRedraw));
		}
	}

	public void checkForPageSwipe()
	{
		if(mPageRendered)
			highLighter();
	}

	private void highLighter()
	{
		mLinks     = null;
		mLinksLeft = null;

		if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
		{
			if((mCore.mPageAnnots.get(mPageNumber) != null))
			{
				mLinks = mCore.mPageAnnots.get(mPageNumber);
				if(mLinks.length > 0)
					highLightTimer = new highLightLinkTimer(2000, 2000);
			}
			else
			{
				mGetLinkInfo = new PageAnnots();
				mGetLinkInfo.executeOnExecutor(new ThreadPerTaskExecutor());
			}
		}
		else
		{
			int mPage = 0;
			int mPage1 = 0;
			if(mActivity.isPreview)
			{
				int mTempPage;
				try
				{
					mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(mPosition*2)]) - 1;
				}
				catch (Exception e)
				{
					mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(mPosition*2)-1]);
				}

				mPage  = mTempPage - 1;
				mPage1 = mTempPage;
			}
			else
			{
				mPage  = (mPageNumber*2)-1;
				mPage1 = mPageNumber*2;
			}

			if(mPosition == 0)
			{
				if((mCore.mPageAnnots.get(mPage1) != null))
				{
					mLinks = mCore.mPageAnnots.get(mPage1);
					if(mLinks.length > 0)
						highLightTimer = new highLightLinkTimer(2000, 2000);
				}
				else
				{
					mGetLinkInfo = new PageAnnots();
					mGetLinkInfo.executeOnExecutor(new ThreadPerTaskExecutor());
				}
			}
			else if(mPosition == ((mActivity.noOfPages/2)+MuPDFActivity.addPagesCount))
			{
				if((mCore.mPageAnnots.get(mPage) != null))
				{
					mLinksLeft = mCore.mPageAnnots.get(mPage);
					if(mLinksLeft.length > 0)
						highLightTimer = new highLightLinkTimer(2000, 2000);
				}
				else
				{
					mGetLinkInfo = new PageAnnots();
					mGetLinkInfo.executeOnExecutor(new ThreadPerTaskExecutor());
				}
			}
			else
			{
				if((mCore.mPageAnnots.get(mPage) != null))
					mLinksLeft = mCore.mPageAnnots.get(mPage);

				if((mCore.mPageAnnots.get(mPage1) != null))
					mLinks = mCore.mPageAnnots.get(mPage1);
				else
				{
					mGetLinkInfo = new PageAnnots();
					mGetLinkInfo.executeOnExecutor(new ThreadPerTaskExecutor());
				}

				if((mLinks != null && mLinks.length > 0) || (mLinksLeft != null && mLinksLeft.length > 0))
					highLightTimer = new highLightLinkTimer(2000, 2000);
			}
		}
	}

	class highLightLinkTimer extends CountDownTimer
	{
		long duration, interval;
		public highLightLinkTimer(long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
			start();

			mHighlightLinks = true;
			mHighLightLink.invalidate();
			mHighLightLink.bringToFront();
		}

		@Override
		public void onFinish()
		{
			mHighlightLinks = false;
			mLinks = null;
			mLinksLeft = null;
			mHighLightLink.invalidate();
		}

		@Override
		public void onTick(long duration)
		{

		}
	}

	class PageAnnots extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
				getLinkInfo(mPageNumber);
			else
			{
				int mPage  = 0;
				int mPage1 = 0;
				if(mActivity.isPreview)
				{
					int mTempPage;
					try
					{
						mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(mPosition*2)]) - 1;
					}
					catch (Exception e)
					{
						try
						{
							mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(mPosition*2)-1]);
						}
						catch(Exception e1)
						{
							mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(mPosition)-1]);
						}
					}

					mPage  = mTempPage - 1;
					mPage1 = mTempPage;
				}
				else
				{
					mPage  = (mPageNumber*2)-1;
					mPage1 = mPageNumber*2;
				}

				if(mPosition == 0)
					getLinkInfo(0);
				else if(mPosition == (mActivity.noOfPages/2)+MuPDFActivity.addPagesCount)
					getLinkInfo(mPage);
				else
				{
					getLinkInfo(mPage);
					getLinkInfo(mPage1);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			mGetLinkInfo = null;
			highLighter();
		}
	}

	public void update()
	{
		if(mLowQualityFlag == false)
			mHighQualityTaskInterruptedFlag = true;
		else if((!mPageRendered) && (!mHighQualityTaskStarted) && (!mActivity.isCoreNull) && mLowQualityFlag)
		{
			cancelRunningTask();
			highLighter();

			mRenderHighQualityTask = new RenderHighQuality();
			mRenderHighQualityTask.executeOnExecutor(new ThreadPerTaskExecutor());
		}
	}

	public void removeHq()
	{
		// Stop the drawing of the patch if still going
		if (mDrawPatch != null)
		{
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}

		// And get rid of it
		mPatchViewSize = null;
		mPatchArea = null;
		if (mPatch != null)
		{
			mPatchBm.setBm(null);
			recyclePatchViewBitmap();
		}
	}

	public int getPage()
	{
		return mPageNumber;
	}

	public TextView gettxtProgressLft()
	{
		return txtProgressLft;
	}
	public TextView gettxtProgressRht()
	{
		return txtProgressRht;
	}

	public Bitmap mergeBitmaps(Bitmap value1, Bitmap value2, int width, int height)
	{
		try
		{
			Bitmap mergedBitmap = null;
			try
			{
				mergedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			}
			catch(OutOfMemoryError e)
			{
				System.gc();
				return null;
			}

			Canvas temp = new Canvas(mergedBitmap);
			temp.drawBitmap(value1, 0f, 0f, null);
			temp.drawBitmap(value2, width / 2, 0f, null);

			value1.recycle();
			value1 = null;

			value2.recycle();
			value2 = null;

			return mergedBitmap;
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	@Override
	public boolean isOpaque()
	{
		return true;
	}

	class RenderHighQuality extends AsyncTask<Void, Void, Void>
	{
		protected void onPreExecute() 
		{
			mHighQualityTaskStarted = true;
			if(mEntire == null)
				imageViewLayout();
			mEntireBm.setBm(null);
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2  && mActivity.isEasyMode))
				drawSinglePage(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y, mPath);
			else
				drawPageForLandscape(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y, mPosition, mPath);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			if(mEntireBm.getBm() != null)
			{
				recycleImageViewBitmap();
				mPageRendered = true;
				mEntire.setImageBitmap(mEntireBm.getBm());
				mHighLightLink.bringToFront();
				
				if(mActivity.screenOrientation == 2 && mActivity.isEasyMode)
					addHq(false);
			}
			
			mRenderHighQualityTask = null;
		}
	}
	
/*	public static void logHeap(String tag) 
	 {
	        Double allocated = new Double(Debug.getNativeHeapAllocatedSize())/new Double((1048576));
	        Double available = new Double(Debug.getNativeHeapSize())/1048576.0;
	        Double free = new Double(Debug.getNativeHeapFreeSize())/1048576.0;
	        DecimalFormat df = new DecimalFormat();
	        df.setMaximumFractionDigits(2);
	        df.setMinimumFractionDigits(2);

	        Log.v(tag, "debug. =================================");
	        Log.d(tag, "debug.heap native: allocated " + df.format(allocated) + "MB of " + df.format(available) + "MB (" + df.format(free) + "MB free)");
	        Log.d(tag, "debug.memory: allocated: " + df.format(new Double(Runtime.getRuntime().totalMemory()/1048576)) + "MB of " + df.format(new Double(Runtime.getRuntime().maxMemory()/1048576))+ "MB (" + df.format(new Double(Runtime.getRuntime().freeMemory()/1048576)) +"MB free)");
	 }*/
}
