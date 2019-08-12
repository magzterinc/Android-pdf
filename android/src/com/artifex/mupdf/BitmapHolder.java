package com.artifex.mupdf;

import android.graphics.Bitmap;

public class BitmapHolder {
	private Bitmap bm;

	
	public BitmapHolder() {
		bm = null;
	}

	public synchronized void setBm(Bitmap abm) {
/*		if(abm == null)
		{
			if(bm != null)
			{
				bm.recycle();
				bm = null;
			}
		}*/
		bm = abm;
	}

	public synchronized Bitmap getBm() {
		return bm;
	}
}
