package com.mycj.mymassager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * 启动activity
 * @author Administrator
 *
 */
public class FrontActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_front);
		Intent intentScan  = new Intent(FrontActivity.this,ConnectActivity.class);
		startActivity(intentScan);
		this.finish();
		
	}
}
