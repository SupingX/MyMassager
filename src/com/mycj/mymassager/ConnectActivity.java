package com.mycj.mymassager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mycj.mymassager.bluetooth.BleApplication;
import com.mycj.mymassager.bluetooth.BleService;

public class ConnectActivity extends Activity {
	String TAG = this.getClass().getSimpleName();
	private List<BluetoothDevice> mBlueToothlist;
	// private TextView textViewFresh;
	private BleService mBleService;
	private ImageView imgRefresh;
	private TextView textViewSkip;
	private ListView listViewBle;
	private ProgressBar mProgressBar;
	private BleDevicesAdapter mBleDevicesAdapter;
	// private ProgressBar mProgressConnectting;
	private ProgressDialog connectingDialog;
	private boolean isconnetting = false;
	private Handler mHandle = new Handler();
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(BleService.BLE_STATUS_ABNORMAL)) {
				// 打开蓝牙
				Intent mIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(mIntent);
			} else if (action.equals(BleService.BLE_DEVICE_SCANING)) {
				mProgressBar.setVisibility(View.VISIBLE);
				// textViewFresh.setVisibility(View.GONE);
				imgRefresh.setClickable(false);
				// 搜索中
			} else if (action.equals(BleService.BLE_DEVICE_STOP_SCAN)) {
				mProgressBar.setVisibility(View.GONE);
				// textViewFresh.setVisibility(View.VISIBLE);
				imgRefresh.setClickable(true);
				// 停止搜索
			} else if (action.equals(BleService.BLE_DEVICE_FOUND)) {
				// 找到设备
				Bundle b = intent.getExtras();
				BluetoothDevice device = b
						.getParcelable(BleService.EXTRA_DEVICE);
				if (!mBlueToothlist.contains(device)) {
					mBlueToothlist.add(device);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mBleDevicesAdapter.notifyDataSetChanged();
						}
					});
				}
			} else if (action.equals(BleService.BLE_GATT_CONNECTED)) {
			} else if (action.equals(BleService.BLE_GATT_DISCONNECTED)) {
			} else if (action.equals(BleService.BLE_NOT_SUPPORTED)) {
				// Log.d("OB","not support");
			} else if (action.equals(BleService.BLE_STATUS_ABNORMAL)) {
				//
			} else if (action.equals(BleService.BLE_SERVICE_DISCOVERED)) {
				// 发现
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_FOUND)) {
				// 发现
				isconnetting = false;
				connectingDialog.dismiss();
				startActivity(new Intent(ConnectActivity.this,MainActivity.class));
				finish();
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect);
		mBleService = ((BleApplication) getApplication()).getBleService();
		initViews();
		mBlueToothlist = new ArrayList<>();
		mBleDevicesAdapter = new BleDevicesAdapter();
		listViewBle.setAdapter(mBleDevicesAdapter);
		setListener();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mBroadcastReceiver, BleService.getIntentFilter());
		if (((BleApplication) getApplication()).isbleSupport()) {
			((BleApplication) getApplication()).getBleService().scanBleDevices(
					true);
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mBroadcastReceiver);
	}

	@Override
	protected void onDestroy() {
		if (backDialog != null) {
			backDialog.dismiss();
		}
		if (connectingDialog != null) {
			connectingDialog.dismiss();
		}
		super.onDestroy();
	}

	public void initViews() {
		// textViewFresh = (TextView) findViewById(R.id.tv_fresh);
		imgRefresh = (ImageView) findViewById(R.id.img_conn);
		textViewSkip = (TextView) findViewById(R.id.tv_skip);
		listViewBle = (ListView) findViewById(R.id.lv_ble);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
	}

	public void setListener() {
		// 连接蓝牙
		listViewBle.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (((BleApplication) getApplication()).isbleSupport()) {
					((BleApplication) getApplication()).getBleService()
							.scanBleDevices(false);
					final BluetoothDevice device = mBlueToothlist.get(position);
					if (device != null) {
						// AlertDialog.Builder mBuilder = new
						// AlertDialog.Builder(
						// ConnectActivity.this);
						// View dialog = getLayoutInflater().inflate(
						// R.layout.dialog, null);
						// mProgressConnectting = (ProgressBar) dialog
						// .findViewById(R.id.numberbar1);
						// mBuilder.setView(dialog);
						// final AlertDialog connectingDialog =
						// mBuilder.create();
						// connectingDialog.show();
						isconnetting = true;
						connectingDialog = ProgressDialog.show(
								ConnectActivity.this, "", "正在连接设备...", true);

						Runnable dialogRunnable = new Runnable() {
							@Override
							public void run() {

								if (isconnetting) {
									if(mBleService.getConnectState()!=1){
										showDialog("连接设备失败");
									}else {
										showDialog("请连接按摩器");
									}
								}
								
							}
						};

						// 连接蓝牙
						mBleService.connectBleDevice(device);
						mHandle.postDelayed(dialogRunnable, 8000);

					} else {

					}
				}
			}
		});

		// 刷新
		// textViewFresh.setOnClickListener(new OnClickListener() {
		imgRefresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((BleApplication) getApplication()).isbleSupport()) {
					// mBleService = ((BleApplication)
					// getApplication()).getBleService();
					// System.out.println("ble service " + mBleService);
					// mBleService.scanBleDevices(true);
					((BleApplication) getApplication()).getBleService()
							.scanBleDevices(true);
				}
			}
		});

		// 跳过
		textViewSkip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((BleApplication) getApplication()).isbleSupport()) {
					((BleApplication) getApplication()).getBleService()
							.scanBleDevices(false);
				}
				Intent intent = new Intent(ConnectActivity.this,
						MainActivity.class);
				startActivity(intent);
				finish();

			}
		});
	}

	private class BleDevicesAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mBlueToothlist.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (mBlueToothlist.size() > 0) {
				ViewHolder holder = new ViewHolder();
				convertView = LayoutInflater.from(getApplicationContext())
						.inflate(R.layout.list_ble, parent, false);
				holder.deviceName = (TextView) convertView
						.findViewById(R.id.tv_ble_name);
				holder.deviceAddress = (TextView) convertView
						.findViewById(R.id.tv_ble_address);
				BluetoothDevice device = (BluetoothDevice) mBlueToothlist
						.get(position);
				holder.deviceName.setText(device.getName());
				;
				holder.deviceAddress.setText(device.getAddress());
				;
				return convertView;
			} else {
				return null;
			}
		}

		private class ViewHolder {
			private TextView deviceName;
			private TextView deviceAddress;
		}
	}

	private void showDialog(String msg) {
		new AlertDialog.Builder(ConnectActivity.this).setTitle(msg)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBlueToothlist.clear();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mBleDevicesAdapter.notifyDataSetChanged();
							}
						});
						// if (mBleService.isConnected()) {
						if (mBleService.getConnectState() == 1) {
							mBleService.disconnectBleDevice();
						}
						mBleService.scanBleDevices(true);
						dialog.dismiss();
						connectingDialog.dismiss();
					}
				}).create().show();
	}

	private AlertDialog backDialog;

	@Override
	public void onBackPressed() {
		// View dialog = getLayoutInflater().inflate(R.layout.edit_dialog,
		// null);
		backDialog = new AlertDialog.Builder(ConnectActivity.this)
				.setTitle("是否确定退出？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				// .setView(dialog)
				.create();
		// backDialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_background_color));
		backDialog.show();

	}
}
