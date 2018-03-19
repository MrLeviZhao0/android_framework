package com.royole.glline;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;

/**
 * Created by boli on 2017/6/12.
 */

public class MyGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "MyGLSurfaceView";
    Render render;
    Handler handler;

    private AnimCallback callback;

    public interface AnimCallback {
        void onFinish();
    }

    public MyGLSurfaceView(Context context, Bitmap[] bitmaps, AnimCallback callback) {
        super(context);
        this.callback = callback;
        handler = new Handler();
        this.setEGLContextClientVersion(2);
        render = new Render(context);
        render.setBitmaps(bitmaps);
        setRenderer(render);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private int i;

    public void startAnimation() {
    	  Log.d(TAG, "MyGLSurfaceView startAnimation: ");
        i = 1;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (i < 36000) {
                	Log.d(TAG, "MyGLSurfaceView startAnimation: i:"+i);
                    render.updateData();
                    requestRender();
                    handler.postDelayed(this, 5000);
                    i++;
                } else {
                		Log.d(TAG, "MyGLSurfaceView startAnimation: onfinish: i "+i);
                    callback.onFinish();
                }
            }
        });
    }

}
