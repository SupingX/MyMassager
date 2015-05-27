package com.mycj.mymassager;

import java.util.*;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.AdapterView.*;

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
				// startActivity(mIntent);
				startActivityForResult(mIntent, 1);
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
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_FOUND)) {
				// 发现
				isconnetting = false;
				if (connectingDialog != null) {
					connectingDialog.dismiss();
				}
				startActivity(new Intent(ConnectActivity.this,
						MainActivity.class));
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BleApplication.addActivity(this);
		setContentView(R.layout.activity_connect);
		mBleService = ((BleApplication) getApplication()).getBleService();
		initViews();
		mBlueToothlist = new ArrayList<>();
		mBleDevicesAdapter = new BleDevicesAdapter();
		listViewBle.setAdapter(mBleDevicesAdapter);
		setListener();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (Activity.RESULT_OK == resultCode) {
				mHandle.postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						mBleService.rescanBleDevices();
					}
				}, 4000);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mBleService.setBackground(false);
		if (mBleService.getConnectState() != BluetoothGatt.STATE_CONNECTED) {
			mBleService.rescanBleDevices();
		}
		clearBleList();
		registerReceiver(mBroadcastReceiver, BleService.getIntentFilter());
		if (((BleApplication) getApplication()).isbleSupport()) {
			((BleApplication) getApplication()).getBleService()
					.rescanBleDevices();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mBleService.setBackground(true);
		Log.e(TAG, "set backgroudn in connect : " + mBleService.isBackground());
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

	/** 初始化基础视图 */
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
					final BluetoothDevice device = mBlueToothlist.get(position);
					if (device != null) {
						isconnetting = true;
						connectingDialog = ProgressDialog.show(
								ConnectActivity.this, "", "正在连接设备...", true);

						Runnable dialogRunnable = new Runnable() {
							@Override
							public void run() {

								if (isconnetting) {
									if (mBleService.getConnectState() != 1) {
										showDialog("连接设备失败");
									} else {
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
		/**
		 * imgRefresh.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { if (((BleApplication)
		 *           getApplication()).isbleSupport()) { // mBleService =
		 *           ((BleApplication) // getApplication()).getBleService(); //
		 *           System.out.println("ble service " + mBleService); //
		 *           mBleService.scanBleDevices(true); ((BleApplication)
		 *           getApplication()).getBleService() .scanBleDevices(true); }
		 *           } });
		 */

		// 跳过
		textViewSkip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// if (((BleApplication) getApplication()).isbleSupport()) {
				// ((BleApplication) getApplication()).getBleService()
				// .scanBleDevices(false);
				// }
				Intent intent = new Intent(ConnectActivity.this,
						MainActivity.class);
				startActivity(intent);
				// finish();

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
						clearBleList();
						// if (mBleService.isConnected()) {
						if (mBleService.getConnectState() == 1) {
							mBleService.disconnectBleDevice();
						}
						// mBleService.scanBleDevices(true);
						dialog.dismiss();
						if (connectingDialog != null) {
							connectingDialog.dismiss();
						}

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
//						finish();
						//退出
//						System.exit(0);
//						android.os.Process.killProcess(android.os.Process.myPid());
						BleApplication.finishActivity();
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

	private void clearBleList() {
		mBlueToothlist.clear();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBleDevicesAdapter.notifyDataSetChanged();
			}
		});
	}
}
