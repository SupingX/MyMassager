package com.mycj.mymassager.view;

import com.mycj.mymassager.R;
//import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * 竖直的SeekBar
 * 
 * @author Administrator
 * 
 */
public class VerticalSeekBar extends SeekBar {

	private String text = "1";

	private Paint paint;

	public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		TypedArray typedArray = context.obtainStyledAttributes(attrs,
				R.styleable.VerticalSeekBar);
		float thumbTextsize = typedArray.getDimension(
				R.styleable.VerticalSeekBar_thumb_textSize, 22);
		paint.setTextSize(thumbTextsize);
		paint.setColor(Color.WHITE);
		paint.setTextAlign(Paint.Align.CENTER);

		typedArray.recycle();
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec,
			int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.rotate(-90);
		canvas.translate(-getHeight(), 0);
		super.onDraw(canvas);

		// 还原canvas设置
		// canvas.restore();
		// 只还原旋转角度即可，不要调用restore全部还原
		canvas.rotate(90);
		canvas.translate(0, -getHeight());
		Rect rectThumb = getThumb().getBounds();
		Rect rectText = new Rect();
		paint.getTextBounds(text, 0, 1, rectText);
		// 在滑块上画上当前进度
		canvas.drawText(text, rectThumb.width() / 2 - 3, getHeight()
				- rectThumb.left - rectText.height() / 2 + 1, paint);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		// 使之不可拖动,每种情况都直接break掉，拦截了但是不做任何处理
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
			// int i = 0;
			// i = getMax() - (int) (getMax() * event.getY() / getHeight());
			// setProgress(i);
			// // Log.i("Progress", getProgress() + "");
			// onSizeChanged(getWidth(), getHeight(), 0, 0);
			break;

		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return true;
	}

	/**
	 * 重写setProgress，使thumb随着progress而动
	 */
	@Override
	public synchronized void setProgress(int progress) {
		// TODO Auto-generated method stub
		super.setProgress(progress);
		if (progress >= 0 && progress <= getMax()) {
			text = progress + "";
		}
		// 调用onSizeChanged方法，使滑块移动
		onSizeChanged(getWidth(), getHeight(), 0, 0);

	}

}
