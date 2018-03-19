package com.royole.glline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by lezh on 5/26/2017.
 */

public class WaterView extends View{

    private static final int START_DRAW = 0;
    private static final int DRAWING = 1;
    private Bitmap mBitmap ;

    private boolean isDrawing = false;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case START_DRAW:{
                    mHandler.sendEmptyMessageDelayed(DRAWING,100);
                    isDrawing = true;
                    break;
                }case DRAWING:{
                    mHandler.sendEmptyMessageDelayed(DRAWING,100);
                    invalidate();
                    break;
                }
            }
        }
    };

    public WaterView(Context context) {
        super(context);
    }

    public WaterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void startAnimation(){
        mHandler.sendEmptyMessage(START_DRAW);
    }

    public void setBitMap(Bitmap input){
        mBitmap = input;
    }

    int indexOfDraw=0;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        if (null!=mBitmap){
        		canvas.drawRect(0,0,mBitmap.getWidth(),mBitmap.getHeight(),mPaint);
            canvas.drawBitmap(mBitmap,0,0,mPaint);
            
			}
    }
}
