package com.lsj.analogclock.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.icu.util.TimeZone;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;

import com.lsj.analogclock.R;


/**
 * Created by shiny_jia
 * on 2016/8/22 9:13.
 * 自定义时钟view
 */

public class AnalogClock extends View {

    /**
     * 记录表盘图片的宽和高
     */
    private int mDialHeight;
    private int mDialWidth;

    /**
     * 指针图片
     */
    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;

    /**
     * 记录当前时间
     */
    private Time mTime;

    /**
     * 时间值
     */
    private float mHours;
    private float mMinutes;
    private float mSeconds;

    /**
     * 跟踪View 的尺寸的变化
     */
    private boolean isChanged;

    /**
     * 记录View是否被加入到了Window中
     */
    private boolean isAttached;

    /**
     *刷新时间线程
     */
    private Thread refreshThread;

    /**
     * 秒针刷新的时间
     */
    private float refresh_time = 1000;

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        //        final Resources r = context.getResources();
        refreshThread = new Thread();
        if (mHourHand == null) {
            mHourHand = context.getDrawable(R.drawable.clock_hand_hour);
        }
        if (mMinuteHand == null) {
            mMinuteHand = context.getDrawable(R.drawable.clock_hand_minute);
        }
        if (mSecondHand == null) {
            mSecondHand = context.getDrawable(R.drawable.clock_hand_second);
        }
        if (mDial == null) {
            mDial = context.getDrawable(R.drawable.clock_dial);
        }

        mTime = new Time();

        mDialHeight = mDial.getIntrinsicHeight();
        mDialWidth = mDial.getIntrinsicWidth();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthSize < mDialWidth && MeasureSpec.UNSPECIFIED == widthMode) {
            hScale = (float) widthSize / (float) mDialWidth;
        }
        if (heightSize < mDialHeight && MeasureSpec.UNSPECIFIED == heightMode) {
            vScale = (float) heightSize / (float) mDialHeight;
        }
        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        isChanged = true;
    }


    /**
     * 接受时间变化的系统广播
     */
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
             * 时区变化判断
             */
            if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mTime = new Time(TimeZone.getTimeZone(tz).getID());
            }
            //更新时间
            onTimeChange();
            invalidate();

        }
    };

    /**
     * 当view attach 到window时注册广播
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTime = new Time();
        refreshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //设置更新界面的刷新时间
                    SystemClock.sleep((long) refresh_time);
//                    postInvalidate();
                    onTimeChange();
                }
            }
        });
        refreshThread.start();

        if (!isAttached) {
            isAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_TIME_TICK);
            getContext().registerReceiver(mIntentReceiver, filter);
        }

        onTimeChange();
    }

    /**
     * 当view detach时取消注册
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        refreshThread.interrupt();
        if (isAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            isAttached = false;
        }
    }

    /**
     * 监听时间变化
     */
    private void onTimeChange() {
        mTime.setToNow();
        int hour = mTime.hour;
        int minute = mTime.minute;
        int second = mTime.second;

        mSeconds = second / 60.0f;
        mMinutes = minute + second / 60.0f;
        mHours = hour + minute / 60.0f;
        isChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean changed = isChanged;
        if (changed) {
            isChanged = false;
        }
        int availableWidth = super.getRight() - super.getLeft();
        int availableHeight = super.getBottom() - super.getTop();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        //处理表盘
        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();
        boolean scaled = false;

        if ((availableHeight < h || availableWidth < w)) {
            scaled = true;
            float scale = Math.min(
                    (float) availableWidth / (float) w,
                    (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);
        canvas.save();

        //处理时针
        canvas.rotate(mHours / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;

        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        hourHand.draw(canvas);
        canvas.restore();
        canvas.save();

        //处理分针
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
        final Drawable minuteHand = mMinuteHand;

        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        minuteHand.draw(canvas);
        canvas.restore();
        canvas.save();

        //处理秒针
        canvas.rotate(mSeconds / 60.0f * 360.0f, x, y);
        final Drawable secondHand = mSecondHand;

        if (changed) {
            w = secondHand.getIntrinsicWidth();
            h = secondHand.getIntrinsicHeight();
            secondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        secondHand.draw(canvas);
        canvas.restore();
        canvas.save();

        //缩放坐标复原
        if (scaled) {
            canvas.restore();
        }
    }
}
