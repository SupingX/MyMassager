package com.mycj.mymassager.view;

import android.os.Handler;
import android.util.Log;

/**
 * 自定义倒计时类
 * 
 * @author Administrator
 * 
 */
public class MyCountDownTimer {
	
	private long millisInFuture;

	private long countDownInterval;

	private boolean status;

	private static MyCountDownTimer myCounter;

	/**
	 * 
	 * @param pMillisInFuture
	 *            总时间
	 * @param pCountDownInterval
	 *            计时间隔 （1000ms）
	 */
	public MyCountDownTimer(long pMillisInFuture, long pCountDownInterval) {

		this.millisInFuture = pMillisInFuture;

		this.countDownInterval = pCountDownInterval;

		status = false;

		initialize();

	}

	/**
	 * 单例模式，获取唯一对象
	 * 
	 * @param pMillisInFuture
	 *            总时间（毫秒）
	 * @param pCountDownInterval
	 *            计时间隔（毫秒）
	 * @return
	 */
	public static MyCountDownTimer getInstance(long pMillisInFuture,
			long pCountDownInterval) {
		if (myCounter == null) {
			return new MyCountDownTimer(pMillisInFuture, pCountDownInterval);
		}
		return myCounter;
	}

	public void stop() {
		status = false;
	}

	public long getCurrentTime() {
		return millisInFuture;
	}

	public void start() {
		status = true;
	}

	public void initialize() {

		final Handler handler = new Handler();
		Log.v("status", "starting");
		final Runnable counter = new Runnable() {

			public void run() {

				// long sec = millisInFuture / 1000;
				if (status) {
					if (millisInFuture <= 0) {
						// Log.v("status", "done");
					} else {
						// Log.v("status", Long.toString(sec) +
						// " seconds remain");
						millisInFuture -= countDownInterval;
						handler.postDelayed(this, countDownInterval);
					}
				} else {
					// Log.v("status", Long.toString(sec)
					// + " seconds remain and timer has stopped!");
					handler.postDelayed(this, countDownInterval);
				}
			}
		};
		handler.postDelayed(counter, countDownInterval);

	}
}
