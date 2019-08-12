package com.artifex.mupdf;
import android.content.Context;
import android.graphics.Point;
import android.widget.TextView;

public class MuPDFPageView extends PageView
{
	private final MuPDFCore mCore;
	private Context mContext;
	
	private TextView mLeftTextView;
	private TextView mRightTextView;
	
	int mTempPageNumber = 0;
	
	public MuPDFPageView(Context c, MuPDFCore core, Point parentSize) 
	{
		super(c, core, parentSize);
		mCore = core;
		mContext = c;
		
		mLeftTextView  = gettxtProgressLft();
		mRightTextView = gettxtProgressRht();
	}

	public String[] hitLinkPage(float x, float y)
	{
		
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		float docRelX = (x - getLeft())/scale;
		float docRelY = (y - getTop())/scale;
		
		if(((MuPDFActivity)mContext).screenOrientation == 1 || ((MuPDFActivity)mContext).screenOrientation == 2  && ((MuPDFActivity)mContext).isEasyMode)
		{
			if(mCore.mPageAnnots.get(mPageNumber) != null && mCore.mPageAnnots.get(mPageNumber).length > 0)
			{
				LinkInfo[] mTempLink = mCore.mPageAnnots.get(mPageNumber);
				for (LinkInfo l: mTempLink)
				{
					if (l.contains(docRelX, docRelY))
						return new String[]{l.url.toString(), ""+mPageNumber};
				}
			}
		}
		else
		{
			if(((MuPDFActivity)mContext).isPreview)
			{
				try
				{
					mTempPageNumber = (Integer.parseInt(((MuPDFActivity)mContext).previewPageNumbers[mPosition * 2]) - 1);
				}
				catch (Exception e)
				{
					mTempPageNumber = (Integer.parseInt(((MuPDFActivity)mContext).previewPageNumbers[(mPosition * 2) - 1]));
				}
			}
			else
				mTempPageNumber = mPageNumber * 2;
			
			if(docRelX > ((float)getWidth() / scale) / 2)
			{
				docRelX = docRelX - ((float)getWidth() / scale) / 2;
				
				if(mCore.mPageAnnots.get(mTempPageNumber) != null && mCore.mPageAnnots.get(mTempPageNumber).length > 0)
				{
					LinkInfo[] mTempLink = mCore.mPageAnnots.get(mTempPageNumber);
					for (LinkInfo l: mTempLink)
						if (l.contains(docRelX, docRelY))
							return new String[]{l.url.toString(), ""+mTempPageNumber};
				}
			}
			else
			{
				mTempPageNumber = mTempPageNumber - 1;
				if(mCore.mPageAnnots.get(mTempPageNumber) != null && mCore.mPageAnnots.get(mTempPageNumber).length > 0)
				{
					LinkInfo[] mTempLink = mCore.mPageAnnots.get(mTempPageNumber);
					for (LinkInfo l: mTempLink)
						if (l.contains(docRelX, docRelY))
							return new String[]{l.url.toString(), ""+mTempPageNumber};
					}
			}
		}
		return null;
	}

	@Override
	protected void updateProgress(int status, int visible, boolean isLft, int page) 
	{
		try 
		{
			if(((MuPDFActivity)mContext).screenOrientation == 2 && !((MuPDFActivity)mContext).isEasyMode)
			{
				if(isLft && visible == 1)
				{
					mLeftTextView.setText("           "+status+"%           ");
					mRightTextView.setText(" Waiting to Download ");
				}
				else if(!(isLft) && visible == 1)
				{
					mLeftTextView.setText("        100%       ");
					mRightTextView.setText("           "+status+"%           ");
				}
			}
			else
			{
				if(visible == 1)
					mLeftTextView.setText("          "+status+"%          ");
			}
		}
		catch (Exception e)
		{
			
		}
	}
	
	@Override
	protected void drawSinglePage(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight, String path)
	{
		mCore.drawSinglePage(h, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, path);
	}

	@Override
	protected void drawPage(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight)
	{
		mCore.drawPage(h, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, 0);
	}
	
	@Override
	protected void drawPageForLandscape(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight, int position, String path) 
	{
		mCore.drawPageForLandscape(h, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, position, path);
	}
	
	@Override
	protected void drawPageForLandscapeZoom(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight, int position) 
	{
		mCore.drawPageForLandscapeZoom(h, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, position);
	}

	@Override
	protected void getLinkInfo(int value)
	{
		mCore.getPageLinks(value);
	}
}
