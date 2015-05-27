package com.mycj.mymassager.bluetooth;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Process;
import android.widget.Toast;

public class BleApplication extends Application {
	private BleService mService;
	private boolean bleSupport;
	private static ArrayList<Activity> activities = new ArrayList<Activity>();

	public static void addActivity(Activity activity) {
		activities.add(activity);
	}

	public static void finishActivity() {
		for (Activity activity : activities) {
			if (activity != null) {
				activity.finish();
			}
		}
		Process.killProcess(Process.myPid());
		System.exit(0);
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			mService = ((BleService.LocalBinder) service).getBleService();
		}
	};

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub

		super.onCreate();

		Intent intent = new Intent(this, BleService.class);
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT)
					.show();
			bleSupport = false;
		} else {
			bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
			bleSupport = true;
		}

	}

	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		mService.disconnectBleDevice();
		unbindService(mServiceConnection);

	}

	public BleService getBleService() {
		return mService;
	}

	public boolean isbleSupport() {
		return bleSupport;
	}

}
