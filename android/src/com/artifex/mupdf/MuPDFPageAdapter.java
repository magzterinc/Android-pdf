package com.artifex.mupdf;

import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.io.File;

public class MuPDFPageAdapter extends BaseAdapter
{
	private final Context mContext;
	private final MuPDFCore mCore;
	private int addPagesCount;
	private int pagePositionOriginal;
	private int addPagesList[];
	private String[] htmlUrls;
	private boolean isAddExists = false;
	private String path;
	private int interactivePagePosition;
	public boolean hideCustomView = false;
	private MuPDFActivity mActivity = null;

	public MuPDFPageAdapter(Context c, MuPDFCore core, int[] addPagesList, String path, String[] htmlUrls)
	{
		mContext 		  = c;
		mCore 			  = core;
		mActivity 		  = (MuPDFActivity)c;
		addPagesCount 	  = addPagesList.length;
		this.addPagesList = addPagesList;
		this.htmlUrls	  = htmlUrls;
		this.path 		  = path;
	}

	public int getCount()
	{
		if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2 && mActivity.isEasyMode))
			return mActivity.noOfPages+addPagesCount;
		else
			return ((mActivity.noOfPages/2)+addPagesCount+1);
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	private String url;
	int decrementor = 0;
	private int checkPosition, mAdder;
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		final int pagePosition;
		decrementor = 0;
		if(mActivity.isPreview)
		{
			int tempPosition = 0;
			try
			{
				tempPosition = (Integer.parseInt(mActivity.previewPageNumbers[position]) - 1);
			}
			catch(ArrayIndexOutOfBoundsException e)
			{
				tempPosition = (Integer.parseInt(mActivity.previewPageNumbers[position - 1]) - 1);
			}
			catch(Exception e)
			{
				
			}
			pagePosition = tempPosition;
		}
		else 
			pagePosition = position;
		
		pagePositionOriginal = position;

		if(!mActivity.isPreview && addPagesCount != 0 )
		{
			for (int i = 0; i < addPagesList.length; i++)
			{
				if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2 && mActivity.isEasyMode))
				{
					 checkPosition = position;
					 mAdder = i;
					 interactivePagePosition = (addPagesList[i]);
				}
				else
				{
					checkPosition = ((position-i)*2)-1;
					mAdder = 0;
					interactivePagePosition  = ((position-i)*2)-1;
				}
				
				if(checkPosition == (addPagesList[i]+mAdder))
				{
					isAddExists = true;
					url = htmlUrls[i];
					break;
				}
				else
					isAddExists = false;
			}
			
			for (int i = 0; i < addPagesList.length; i++)
			{
				if(mActivity.screenOrientation == 1 || (mActivity.screenOrientation == 2 && mActivity.isEasyMode))
				{
					 checkPosition = position;
					 mAdder = i;
				}
				else
				{
					checkPosition = ((position-i)*2)-1;
					mAdder = 0;
				}
				if(checkPosition > ((addPagesList[i]+mAdder)))
					decrementor++;
			}
		}
		else 
			decrementor = 0;
		
		if(isAddExists && !mActivity.isPreview)
		{
			WebPageView web;
			if(convertView == null || !(convertView instanceof WebPageView))
				web = new WebPageView(mContext, new Point(parent.getWidth(), parent.getHeight()), parent);
			else 
				web = (WebPageView) convertView;

			web.blank();
			web.setPage("0");
			
			try
			{
				if(url.startsWith("http") || url.startsWith("www"))
					web.setPage(url);
				else
				{
					File f = new File(url+"/"+interactivePagePosition);
					if(f.exists())
					{
						if (new File(url + "/" + interactivePagePosition + "/" + "/index.html").exists())
						{
							if(hideCustomView)
							{
								hideCustomView = false;
								web.hideCustomView();
							}
							else
								web.setPage("file://" + url + "/" + interactivePagePosition+ "/index.html");
						} 
						else
						{
							for (int i = 0; i < f.list().length; i++)
							{
								if (new File(url + "/" + interactivePagePosition + "/"+ f.list()[i] + "/index.html").exists())
								{
									if(hideCustomView)
									{
										hideCustomView = false;
										web.hideCustomView();
									}
									else
										web.setPage("file://" + url + "/"  + interactivePagePosition+ "/" + f.list()[i] + "/index.html");
								}
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			/*if(pagePositionOriginal < 0)
				pagePositionOriginal = 0;
			Constants.currentPage = pagePositionOriginal;*/
			return web;
		}
		
		else
		{
			final MuPDFPageView pageView;
			
			if (convertView == null || !(convertView instanceof MuPDFPageView) ) 
				pageView = new MuPDFPageView(mContext, mCore, new Point(parent.getWidth(), parent.getHeight()));
			 else 
				pageView = (MuPDFPageView) convertView;
			
			pageView.blankPage(pagePosition-decrementor, position);
			
			if(pagePositionOriginal == ReaderView.mCurrent || pagePositionOriginal == ReaderView.mCurrent+1 || pagePositionOriginal == ReaderView.mCurrent-1)
				pageView.setPageTempImage(path, (pagePosition - decrementor), position);

			/*if(pagePosition < 0)
				Constants.currentPage = 0;
			else
				Constants.currentPage = pagePosition;*/
			return pageView;
		}	
	}
}

 
