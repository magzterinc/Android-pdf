 package com.artifex.mupdf;

 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.Bitmap.Config;
 import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.PointF;
 import android.os.Build;
 import android.util.SparseArray;

 import java.io.File;

 public class MuPDFCore
 {
     /* load our native library */
     static
     {
         System.loadLibrary("magzter");
     }

     /* Readable members */
     private float pageWidthForImage;
     private float pageHeightForImage;
     private long globals;
     private MuPDFActivity mActivity = null;

     private String pdfName 				= null;
     private int mPage = 0, mPage1 = 0, mTempPage = 0;

     SparseArray<PointF> mPageSizes = new SparseArray<PointF>();
     SparseArray<LinkInfo[]> mPageAnnots = new SparseArray<LinkInfo[]>();

     /* The native functions */
     private native long openFile(String filename);
     private native void gotoPageInternal(int localActionPageNum, int forImage);
     private native float getPageWidth();
     private native float getPageHeight();
     native void drawPageNative(Bitmap bitmap, int pageW, int pageH, int patchX, int patchY, int patchW, int patchH, int isImage);
     private native LinkInfo[] getPageAnnots(int page);
     private native void destroying();
     private native String getMediaStream(String writepath, String fileName);

     native void drawPageForImage(Bitmap bitmap, int pageW, int pageH, int patchX, int patchY, int patchW, int patchH);
     private native String getPageLink(int page, float x, float y);

     public MuPDFCore(String filename, Context context) throws Exception
     {
         globals   = openFile(filename);
         mActivity = ((MuPDFActivity)context);
         if (globals == 0)
         {
             throw new Exception("Failed to open "+filename);
         }
     }

     /* Shim function */
     private synchronized void gotoPage(int page, int forImage)
     {
         if(page < 0)
             page = 0;

         gotoPageInternal(page, forImage);

         if(forImage == 1)
         {
             this.pageWidthForImage  = getPageWidth();
             this.pageHeightForImage = getPageHeight();
         }
     }

     public synchronized PointF getPageSize(int page, int forImage)
     {
         if(page < 0)
             page = 0;

         gotoPage(page, forImage);
         mPageSizes.put(page, (new PointF(this.pageWidthForImage, this.pageHeightForImage)));
         return new PointF(this.pageWidthForImage, this.pageHeightForImage);
     }

     public synchronized void onDestroy()
     {
         destroying();
         globals = 0;
     }

     public void drawSinglePage(BitmapHolder h, int page, int pageW, int pageH, int patchX, int patchY, int patchW, int patchH, String path)
     {
         BitmapFactory.Options mOptions = new BitmapFactory.Options();

         if(/*Constants.device_inch <= 6 ||*/ Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
         {
             mOptions.inJustDecodeBounds = true;
             mOptions.inPreferredConfig = Config.RGB_565;
             BitmapFactory.decodeFile(path + "/"+page, mOptions);
             int bWidth  = mOptions.outWidth;
             int bHeight = mOptions.outHeight;

             if(bWidth > pageW || bHeight > pageH)
             {
                 int heightRatio = Math.round((float)bHeight / (float)pageH);
                 int widthRatio  = Math.round((float)bWidth / (float)pageW);

                 int sampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
                 sampleSize = sampleSize - 1;

                 if(sampleSize < 0)
                     sampleSize = 0;

                 mOptions.inSampleSize = sampleSize;
             }
         }

         mOptions.inJustDecodeBounds = false;

         try
         {
             if((new File(path+"/"+page).exists()))
             {
                 Bitmap bmp = null;
                 try
                 {
                     bmp = BitmapFactory.decodeFile(path + "/"+page, mOptions);
                 }
                 catch(OutOfMemoryError e)
                 {
                     h.setBm(null);
                     System.gc();
                     mOptions = null;
                     return;
                 }

                 if( bmp == null )
                 {
                     try
                     {
                         bmp = Bitmap.createBitmap(patchW, patchH, Config.RGB_565);
                     }
                     catch(OutOfMemoryError e)
                     {
                         h.setBm(null);
                         System.gc();
                         mOptions = null;
                         return;
                     }
                 }
                 else if( bmp.getWidth() > 2048 || bmp.getHeight() > 2048 )
                 {
                     bmp.recycle();
                     bmp = null;
                     try
                     {
                         bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(path + "/" + page, mOptions), patchW, patchH, true);
                     }
                     catch(OutOfMemoryError e)
                     {
                         h.setBm(null);
                         System.gc();
                         mOptions = null;
                         return;
                     }
                 }
                 h.setBm(bmp);
             }
             else
                 h.setBm(null);
         }
         catch(Exception e)
         {
             h.setBm(null);
         }
         mOptions = null;
     }

     public synchronized void drawPage(BitmapHolder h, int page,	int pageW, int pageH, int patchX, int patchY, int patchW, int patchH, int forImage)
     {
         Bitmap bm = null;
         try
         {
             if(forImage == 0)
             {
                 gotoPage(page, 0);
                 try
                 {
                     bm = Bitmap.createBitmap(patchW, patchH, Config.ARGB_8888);
                 }
                 catch(OutOfMemoryError e)
                 {
                     h.setBm(null);
                     System.gc();
                     return;
                 }
                 drawPageNative(bm, pageW, pageH, patchX, patchY, patchW, patchH, forImage);
                 h.setBm(bm);
             }
             else
             {
                 try
                 {
                     bm = Bitmap.createBitmap(patchW, patchH, Config.ARGB_8888);
                 }
                 catch(OutOfMemoryError e)
                 {
                     h.setBm(null);
                     System.gc();
                     return;
                 }
                 drawPageNative(bm, pageW, pageH, patchX, patchY, patchW, patchH, forImage);
                 h.setBm(bm);
             }
         }
         catch(Exception e)
         {
             h.setBm(null);
         }
     }

     public void drawPageForLandscape(BitmapHolder h, int page,	int pageW, int pageH, int patchX, int patchY, int patchW, int patchH, int position, String path)
     {
         try
         {
             Bitmap bm1 = null;
             Bitmap tempBitmap1 = null;
             Bitmap tempBitmap2 = null;
             Bitmap tempLeftBitmap = null;
             Bitmap tempRightBitmap = null;

             if(mActivity.isPreview)
             {
                 try
                 {
                     mPage  = Integer.parseInt(mActivity.previewPageNumbers[(position*2)-1]) - 1;
                     mPage1 = Integer.parseInt(mActivity.previewPageNumbers[(position*2)]) - 1;
                 }
                 catch (Exception e)
                 {

                 }
             }
             else
             {
                 mPage  = (page * 2) - 1;
                 mPage1 = page * 2;
             }

             BitmapFactory.Options mOptionsLeft  = new BitmapFactory.Options();
             BitmapFactory.Options mOptionsRight = new BitmapFactory.Options();

             if(page == 0)
             {
                 if((new File(path + "/" + page).exists()))
                 {
                     mOptionsLeft.inJustDecodeBounds = true;
                     BitmapFactory.decodeFile(path + "/" + page, mOptionsLeft);
                     int bWidth  = mOptionsLeft.outWidth;
                     int bHeight = mOptionsLeft.outHeight;

                     if(bWidth > pageW / 2 || bHeight > pageH)
                     {
                             int heightRatio = Math.round((float)bHeight / (float)pageH);
                             int widthRatio  = Math.round((float)bWidth / (float)(pageW / 2));

                             mOptionsLeft.inPreferredConfig = Config.RGB_565;
                             mOptionsLeft.inDither = true;
                             mOptionsLeft.inSampleSize = (heightRatio < widthRatio ? heightRatio : widthRatio);
                     }

                     mOptionsLeft.inJustDecodeBounds = false;

                     try
                     {
                         tempBitmap1 = Bitmap.createBitmap(patchW/2, patchH, Config.RGB_565);
                         tempRightBitmap = BitmapFactory.decodeFile(path + "/" + page, mOptionsLeft);
                     }
                     catch(OutOfMemoryError e)
                     {
                         System.gc();
                         h.setBm(null);
                         return;
                     }

                     if(tempRightBitmap == null)
                     {
                         try
                         {
                             tempBitmap2 = Bitmap.createBitmap(patchW/2, patchH, Config.RGB_565);
                         }
                         catch(OutOfMemoryError e)
                         {
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                     else
                     {
                         try
                         {
                             tempBitmap2 = Bitmap.createScaledBitmap(tempRightBitmap, patchW/2, patchH, true);
                             tempRightBitmap.recycle();
                             tempRightBitmap = null;
                         }
                         catch(OutOfMemoryError e)
                         {
                             tempRightBitmap.recycle();
                             tempRightBitmap = null;
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                 }
             }
             else if(position == ((mActivity.noOfPages / 2) + MuPDFActivity.addPagesCount))
             {
                 if((new File(path+"/"+mPage).exists()))
                 {
                     mOptionsLeft.inJustDecodeBounds = true;
                     BitmapFactory.decodeFile(path + "/" + mPage, mOptionsLeft);
                     int bWidth  = mOptionsLeft.outWidth;
                     int bHeight = mOptionsLeft.outHeight;

                     if(bWidth > pageW / 2 || bHeight > pageH)
                     {
                             int heightRatio = Math.round((float)bHeight / (float)pageH);
                             int widthRatio  = Math.round((float)bWidth / (float)(pageW / 2));

                             mOptionsLeft.inPreferredConfig = Config.RGB_565;
                             mOptionsLeft.inDither = true;
                             mOptionsLeft.inSampleSize = (heightRatio < widthRatio ? heightRatio : widthRatio);
                     }

                     mOptionsLeft.inJustDecodeBounds = false;

                     try
                     {
                         tempBitmap2 = Bitmap.createBitmap(patchW/2, patchH, Config.RGB_565);
                         tempLeftBitmap = BitmapFactory.decodeFile(path + "/" + mPage, mOptionsLeft);
                     }
                     catch(OutOfMemoryError e)
                     {
                         System.gc();
                         h.setBm(null);
                         return;
                     }

                     if(tempLeftBitmap == null)
                     {
                         try
                         {
                             tempBitmap1 = Bitmap.createBitmap(patchW/2, patchH, Config.RGB_565);
                         }
                         catch(OutOfMemoryError e)
                         {
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                     else
                     {
                         try
                         {
                             tempBitmap1 = Bitmap.createScaledBitmap(tempLeftBitmap, patchW/2, patchH, true);
                             tempLeftBitmap.recycle();
                             tempLeftBitmap = null;
                         }
                         catch(OutOfMemoryError e)
                         {
                             tempLeftBitmap.recycle();
                             tempLeftBitmap = null;
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                 }
             }
             else
             {
                 if((new File(path + "/" + mPage).exists()) && (new File(path + "/" + mPage1).exists()))
                 {
                     int bWidth;
                     int bHeight;
                     int sampleSizeLeft;
                     int sampleSizeRight;

                     mOptionsLeft.inJustDecodeBounds = true;
                     BitmapFactory.decodeFile(path + "/" + mPage, mOptionsLeft);
                     bWidth  = mOptionsLeft.outWidth;
                     bHeight = mOptionsLeft.outHeight;

                     int heightRatio = Math.round((float)bHeight / (float)pageH);
                     int widthRatio  = Math.round((float)bWidth / (float)(pageW / 2));

                         mOptionsLeft.inPreferredConfig = Config.RGB_565;
                         mOptionsLeft.inDither = true;

                         sampleSizeLeft = (heightRatio < widthRatio ? heightRatio : widthRatio);
                         mOptionsLeft.inSampleSize = sampleSizeLeft;

                     mOptionsLeft.inJustDecodeBounds = false;

                     try
                     {
                         tempLeftBitmap = BitmapFactory.decodeFile(path + "/" + mPage, mOptionsLeft);
                     }
                     catch(OutOfMemoryError e)
                     {
                         System.gc();
                         h.setBm(null);
                         return;
                     }

                     mOptionsRight.inJustDecodeBounds = true;
                     BitmapFactory.decodeFile(path + "/" + mPage1, mOptionsRight);
                     bWidth  = mOptionsRight.outWidth;
                     bHeight = mOptionsRight.outHeight;

                     int heightRatio1 = Math.round((float)bHeight / (float)pageH);
                     int widthRatio1  = Math.round((float)bWidth / (float)(pageW / 2));

                         mOptionsRight.inPreferredConfig = Config.RGB_565;
                         mOptionsRight.inDither = true;

                         sampleSizeRight = (heightRatio1 < widthRatio1 ? heightRatio1 : widthRatio1);
                         mOptionsRight.inSampleSize = sampleSizeRight;

                     mOptionsRight.inJustDecodeBounds = false;

                     try
                     {
                         tempRightBitmap= BitmapFactory.decodeFile(path + "/" + mPage1, mOptionsRight);
                     }
                     catch(OutOfMemoryError e)
                     {
                         System.gc();
                         h.setBm(null);
                         return;
                     }

                     if(tempLeftBitmap == null)
                     {
                         try
                         {
                             tempBitmap1 = Bitmap.createBitmap(patchW / 2, patchH, Config.RGB_565);
                         }
                         catch(OutOfMemoryError e)
                         {
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                     else
                     {
                         try
                         {
                             tempBitmap1 = Bitmap.createScaledBitmap(tempLeftBitmap, patchW/2, patchH, true);
                             tempLeftBitmap.recycle();
                             tempLeftBitmap = null;
                         }
                         catch(OutOfMemoryError e)
                         {
                             tempLeftBitmap.recycle();
                             tempLeftBitmap = null;
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }

                     if(tempRightBitmap == null)
                     {
                         try
                         {
                             tempBitmap2 = Bitmap.createBitmap(patchW / 2, patchH, Config.RGB_565);
                         }
                         catch(OutOfMemoryError e)
                         {
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                     else
                     {
                         try
                         {
                             tempBitmap2 = Bitmap.createScaledBitmap(tempRightBitmap, patchW / 2, patchH, true);
                             tempRightBitmap.recycle();
                             tempRightBitmap = null;
                         }
                         catch(OutOfMemoryError e)
                         {
                             tempRightBitmap.recycle();
                             tempRightBitmap = null;
                             System.gc();
                             h.setBm(null);
                             return;
                         }
                     }
                 }
             }

             mOptionsLeft  = null;
             mOptionsRight = null;

             if(tempBitmap1 == null || tempBitmap2 == null)
             {
                 h.setBm(null);
                 return;
             }

             bm1 = mergeBitmaps(tempBitmap1, tempBitmap2, pageW);

             tempBitmap1.recycle();
             tempBitmap1 = null;

             tempBitmap2.recycle();
             tempBitmap2 = null;

             h.setBm(bm1);
         }
         catch (Exception e)
         {

         }
     }

     public synchronized void drawPageForLandscapeZoom(BitmapHolder h, int page,	int pageW, int pageH, int patchX, int patchY, int patchW, int patchH, int position)
     {
         try
         {
             Bitmap bm1 = null;
             int bitmapWidth1 = (int)((pageW/2)-(patchX));

             if(mActivity.isPreview)
             {
                 try
                 {
                     mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(position*2)]) - 1;
                 }
                 catch (Exception e)
                 {
                     mTempPage = Integer.parseInt(mActivity.previewPageNumbers[(position*2)-1]);
                 }
                 mPage  = mTempPage - 1;
                 mPage1 = mTempPage;
             }
             else
             {
                 mPage = (page*2)-1;
                 mPage1 = page*2;
             }

             try
             {
                 if(bitmapWidth1 > mActivity.screenWidth)
                 {
                     if(position != 0)
                     {
                         gotoPage(mPage, 0);
                         bm1 = Bitmap.createBitmap(patchW, patchH, Config.ARGB_8888);
                         drawPageNative(bm1, pageW/2, pageH, patchX, patchY, patchW, patchH, 0);
                     }
                 }
                 else if(bitmapWidth1 <= 0)
                 {
                     if(position !=((mActivity.noOfPages/2)+ MuPDFActivity.addPagesCount))
                     {
                         gotoPage(mPage1, 0);
                         bm1 = Bitmap.createBitmap(patchW, patchH, Config.ARGB_8888);
                         drawPageNative(bm1, pageW/2, pageH, -(bitmapWidth1), patchY, patchW, patchH, 0);
                     }
                 }
                 else
                 {
                     Bitmap tempBitmap1 = Bitmap.createBitmap(bitmapWidth1, patchH, Config.ARGB_8888);
                     Bitmap tempBitmap2 = Bitmap.createBitmap((mActivity.screenWidth - bitmapWidth1), patchH, Config.ARGB_8888);
                     if(page == 0)
                     {
                         gotoPage(page, 0);
                         drawPageNative(tempBitmap2, pageW/2, pageH, 0, patchY, tempBitmap2.getWidth(), patchH, 0);
                     }
                     else if(position == ((mActivity.noOfPages/2)+ MuPDFActivity.addPagesCount))
                     {
                         gotoPage(mPage, 0);
                         drawPageNative(tempBitmap1, pageW/2, pageH, patchX, patchY, tempBitmap1.getWidth(), patchH, 0);
                     }
                     else
                     {
                         gotoPage(mPage, 0);
                         drawPageNative(tempBitmap1, pageW/2, pageH,  patchX, patchY, tempBitmap1.getWidth(), patchH, 0);

                         gotoPage(mPage1, 0);
                         drawPageNative(tempBitmap2, pageW/2, pageH, 0, patchY, tempBitmap2.getWidth(), patchH, 0);
                     }

                     bm1 = mergeBitmaps(tempBitmap1, tempBitmap2, patchW);
                     tempBitmap1.recycle();
                     tempBitmap1 = null;
                     tempBitmap2.recycle();
                     tempBitmap2 = null;
                 }

                 h.setBm(bm1);
             }
             catch(Exception e)
             {
                 h.setBm(null);
             }
         }
         catch (Exception e)
         {
             h.setBm(null);
         }
     }

     public synchronized String writeMediaStream(String filePath, String fileName, int pageNumber)
     {
         gotoPage(pageNumber, 0);
         return getMediaStream(filePath, fileName);
     }

     public synchronized void getPageLinks(int page)
     {
         if(mPageAnnots != null && mPageAnnots.get(page) == null)
         {
             gotoPage(page, 2);
             try
             {
                 mPageAnnots.put(page, getPageAnnots(page));
             }
             catch(Exception e)
             {

             }
         }
     }

     public Bitmap mergeBitmaps(Bitmap value1, Bitmap value2, int width)
     {
         Bitmap mergedBitmap = null;
         try
         {
             mergedBitmap = Bitmap.createBitmap(width, value1.getHeight(), Bitmap.Config.ARGB_8888);
         }
         catch(OutOfMemoryError e)
         {
             System.gc();
             return null;
         }

         Canvas temp = new Canvas(mergedBitmap);
         temp.drawBitmap(value1, 0f, 0f, null);
         temp.drawBitmap(value2, value1.getWidth(), 0f, null);

         return mergedBitmap;
     }
 }