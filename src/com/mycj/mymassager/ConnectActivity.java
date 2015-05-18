package com.mycj.mymassager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mycj.mymassager.bluetooth.BleApplication;
import com.mycj.mymassager.bluetooth.BleService;

public class ConnectActivity extends Activity {
	String TAG = this.getClass().getSimpleName();
	private List<BluetoothDevice> mBlueToothList;
	private TextView textViewFresh;
	private TextView textViewSkip;
	private ListView listViewBle;
	private ProgressBar mProgressBar;
	private BleService mBleService;
	private BleDevicesAdapter mBleDevicesAdapter;
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
				textViewFresh.setVisibility(View.GONE);
				// 搜索中
			} else if (action.equals(BleService.BLE_DEVICE_STOP_SCAN)) {
				mProgressBar.setVisibility(View.GONE);
				textViewFresh.setVisibility(View.VISIBLE);
				// 停止搜索
			} else if (action.equals(BleService.BLE_DEVICE_FOUND)) {
				// 找到设备
				Bundle b = intent.getExtras();
				BluetoothDevice device = b
						.getParcelable(BleService.EXTRA_DEVICE);
				if (!mBlueToothList.contains(device)) {
					mBlueToothList.add(device);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mBleDevicesAdapter.notifyDataSetChanged();
						}
					});
				}
			} else if (action.equals(BleService.BLE_GATT_CONNECTED)) {
				// 已连接设备
				// BluetoothDevice device =
				// mBleService.getBluetoothGatt().getDevice();
				// String name;
				// if(device!=null){
				// name = device.getName();
				// }else{
				// name = "disconnected";
				// }
			} else if (action.equals(BleService.BLE_GATT_DISCONNECTED)) {
				// 断开连接
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect);
		initViews();
	
		mBleService = ((BleApplication) getApplication()).getBleService();
		Log.d(TAG, mBleService + "");
		mBlueToothList = new ArrayList<>();
		mBleDevicesAdapter = new BleDevicesAdapter();
		listViewBle.setAdapter(mBleDevicesAdapter);
		
		setListener();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mBroadcastReceiver, BleService.getIntentFilter());
		mBleService.scanBleDevices(true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mBroadcastReceiver);
	}

	public void initViews() {
		textViewFresh = (TextView) findViewById(R.id.tv_fresh);
		textViewSkip = (TextView) findViewById(R.id.tv_slip);
		listViewBle = (ListView) findViewById(R.id.lv_ble);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
	}

	public void setListener() {
		//连接蓝牙
		listViewBle.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				final BluetoothDevice device = mBlueToothList.get(position);
				if (device != null) {
					Toast.makeText(getApplicationContext(),
							"正在连接  " + device.getName(), 0).show();
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							mBleService.connectBleDevice(device);
							Intent intent = new Intent(ConnectActivity.this,MainActivity.class);
							Bundle b = new Bundle();
							b.putString("device", device.getName());
							intent.putExtras(b);
							setResult(RESULT_OK, intent);
							finish();
						}
					}, 1000);
				}
			}
		});
		
		//刷新
		textViewFresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBleService.scanBleDevices(true);
			}
		});
		
		//跳过
		textViewSkip.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBleService.scanBleDevices(false);
				Intent intent = new Intent(ConnectActivity.this,MainActivity.class);
				startActivity(intent);
				finish();
				
			}
		});
	}

	private class BleDevicesAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mBlueToothList.size();
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
			if (mBlueToothList.size() > 0) {
				ViewHolder holder = new ViewHolder();
				convertView = LayoutInflater.from(getApplicationContext())
						.inflate(R.layout.list_ble, parent, false);
				holder.deviceName = (TextView) convertView
						.findViewById(R.id.tv_ble_name);
				holder.deviceAddress = (TextView) convertView
						.findViewById(R.id.tv_ble_address);
				BluetoothDevice device = (BluetoothDevice) mBlueToothList
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

}
