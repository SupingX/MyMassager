package com.mycj.mymassager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * 启动activity
 * @author Administrator
 *
 */
public class FrontActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_front);
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				Intent intentScan  = new Intent(FrontActivity.this,ConnectActivity.class);
				startActivity(intentScan);
				finish();
			}
		}, 2000);
	
		
	}
}
