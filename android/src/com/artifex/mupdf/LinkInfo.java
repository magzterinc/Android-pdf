package com.artifex.mupdf;

import android.graphics.RectF;

public class LinkInfo extends RectF {
	public String url;

	public LinkInfo(float l, float t, float r, float b, String url) {
		super(l, t, r, b);
		 this.url = url;
	}
}
