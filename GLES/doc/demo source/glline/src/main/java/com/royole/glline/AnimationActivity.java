package com.royole.glline;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;


public class AnimationActivity extends Activity {
	  private static final String TAG = "AnimationActivity";

    private MyGLSurfaceView mSurface;
    private static Activity mContext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mSurface=new MyGLSurfaceView(this,getBitmaps(),new MyGLSurfaceView.AnimCallback() {
            @Override
            public void onFinish() {
                mContext.finish();
            }
        });
        setContentView(mSurface);
        mSurface.startAnimation();
    }
    
    private Bitmap[] getBitmaps(){
        Resources res = mContext.getResources();
        int texture_size = 5;
        Bitmap[] ret = new Bitmap[texture_size];

        Bitmap bitmap = BitmapFactory.decodeResource(res,R.mipmap.ic_launcher);
        ret[0] = bitmap;
        bitmap = BitmapFactory.decodeResource(res,R.mipmap.rem_icon);
        ret[1] = bitmap;
        bitmap = BitmapFactory.decodeResource(res,R.mipmap.rem_icon0);
        ret[2] = bitmap;
        bitmap = BitmapFactory.decodeResource(res,R.mipmap.rem_icon1);
        ret[3] = bitmap;
        bitmap = BitmapFactory.decodeResource(res,R.mipmap.rem_icon2);
        ret[4] = bitmap;

        return ret;
    }

}
