package com.mycj.mymassager.bluetooth;

import java.util.List;
import java.util.UUID;

import com.mycj.mymassager.entity.MachineStatus;
import com.mycj.mymassager.util.DataUtil;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

@SuppressLint("NewApi")
public class BleService extends Service {
	// private static final String TAG = BleService.class.getSimpleName();
	private static final String TAG = "OB";
	private static final int SCAN_PERIOD = 10000;

	/** Intent for broadcast */
	public static final String BLE_NOT_SUPPORTED = "com.maginawin.bleguide.BLE_NOT_SUPPORTED";
	public static final String BLE_NOT_BT_ADAPTER = "com.maginawin.bleguide.BLE_NOT_BT_ADAPTER";
	public static final String BLE_STATUS_ABNORMAL = "com.maginawin.bleguide.BLE_STATUS_ABNORMAL";
	public static final String BLE_DEVICE_FOUND = "com.maginawin.bleguide.BLE_DEVICE_FOUND";
	public static final String BLE_DEVICE_SCANING = "com.maginawin.bleguide.BLE_DEVICE_SCAN";
	public static final String BLE_DEVICE_STOP_SCAN = "com.maginawin.bleguide.BLE_DEVICE_STOP_SCAN";
	public static final String BLE_GATT_CONNECTED = "com.maginawin.bleguide.BEL_GATT_CONNECTED";
	public static final String BLE_GATT_DISCONNECTTING = "com.maginawin.bleguide.BEL_GATT_CONNECTTING";
	public static final String BLE_GATT_DISCONNECTED = "com.maginawin.bleguide.BLE_GATT_DISCONNECTED";
	public static final String BLE_SERVICE_DISCOVERED = "com.maginawin.bleguide.BLE_SERVICE_DISCOVERED";
	public static final String BLE_CHARACTERISTIC_READ = "com.maginawin.bleguide.BLE_CHARACTERISTIC_READ";
	public static final String BLE_CHARACTERISTIC_NOTIFICATION = "com.maginawin.bleguide.BLE_CHARACTERISTIC_NOTIFICATION";
	public static final String BLE_CHARACTERISTIC_WRITE = "com.maginawin.bleguide.BLE_CHARACTERISTIC_WRITE";
	public static final String BLE_CHARACTERISTIC_CHANGED = "com.maginawin.bleguide.BLE_CHARACTERISTIC_CHANGED";
	public static final String BLE_RSSI_READ = "com.maginawin.bleguide.BLE_RSSI_READ";
	public static final String BLE_CHARACTERISTIC_FOUND = "com.maginawin.bleguide.BLE_CHARACTERISTIC_FOUND";

	/** Intent extras */
	public static final String EXTRA_DEVICE = "DEVICE";
	public static final String EXTRA_RSSI = "RSSI";
	public static final String EXTRA_SCAN_RECORD = "SCAN_RECORD";
	public static final String EXTRA_SOURCE = "SOURCE";
	public static final String EXTRA_ADDR = "ADDRESS";
	public static final String EXTRA_CONNECTED = "CONNECTED";
	public static final String EXTRA_STATUS = "STATUS";
	public static final String EXTRA_UUID = "UUID";
	public static final String EXTRA_VALUE = "VALUE";
	public static final String EXTRA_REQUEST = "REQUEST";
	public static final String EXTRA_REASON = "REASON";
	public static final String EXTRA_DEVICE_CONNECTED = "DEVICE_CONNECTED";
	public static String MYMCU_BLE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
	public static String MYMCU_BLE_READ = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";;
	public static String MYMCU_BLE_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";;

	private final IBinder mBinder = new LocalBinder();
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	/** 上一个蓝牙地址  **/
	private String lastDeviceAdress = "";
	private BluetoothDevice mBluetoothDevice;
	private boolean isScanning;
	/** 是否在后台 */
	private boolean isBackground = false;
	private Handler mHandler;
	// private boolean isConnected = false;
	private int connectState;

	private BluetoothGattCharacteristic mWriteChar; // 发
	private BluetoothGattCharacteristic mNotiChar; // 收

	/**
	 * LeScanCallback 当搜索到设备，在其 onLeScan()传入当前搜索到的设备参数 device rssi scanRecord
	 */
	private final LeScanCallback mLeScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			bleDeviceFound(device, rssi, scanRecord);
			//当搜索到的蓝牙地址与上一个蓝牙地址相同时，连接蓝牙
			if (device.getAddress().equals(lastDeviceAdress)) {
				connectBleDevice(lastDeviceAdress);
			}
		}
	};

	/**
	 * Gatt连接状态回调 BluetoothGattCallback
	 * 
	 * BlueToothGatt的连接状态，连接信息
	 */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		// 连接状态
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			switch (newState) {
			// 已连接
			case BluetoothGatt.STATE_CONNECTED:
				mBluetoothDevice = gatt.getDevice();
				//存贮上一次的蓝牙地址
				lastDeviceAdress = mBluetoothDevice.getAddress();
				connectState = BluetoothGatt.STATE_CONNECTED;
				if (gatt.discoverServices()) {
					bleGattConnected(mBluetoothDevice);
				} else {
					Log.d(TAG, "找不到service...");
				}
				Log.d(TAG, "state connected..");
				break;
			// 连接中
			case BluetoothGatt.STATE_CONNECTING:
				connectState = BluetoothGatt.STATE_CONNECTING;
				break;
			// 已断开
			case BluetoothGatt.STATE_DISCONNECTED:
				connectState = BluetoothGatt.STATE_DISCONNECTED;
				mWriteChar = null;
				mNotiChar = null;
				bleGattDisconnected();
				Log.d(TAG, "state disconnected..");
				break;
			// 断开中
			case BluetoothGatt.STATE_DISCONNECTING:
				connectState = BluetoothGatt.STATE_DISCONNECTING;
				break;

			default:
				break;
			}
		}

		// 连接信息
		// service -- characteristic -- descriptor
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			bleServiceDiscovered(gatt.getDevice());
			List<BluetoothGattService> services = gatt.getServices();
			for (BluetoothGattService service : services) {
				//当service发现时，检测是否是匹配的Service以及Characteristic属性是否存在
				String serviceUuid = service.getUuid().toString();
				if (serviceUuid.equals(MYMCU_BLE)) {
					mWriteChar = service.getCharacteristic(UUID
							.fromString(MYMCU_BLE_WRITE));
					mNotiChar = service.getCharacteristic(UUID
							.fromString(MYMCU_BLE_READ));
					updateNotificaiton(mNotiChar, true); // 发现有notification特性就发送通知
					if (isRightDevice()) {
						bleCharacteristicFound();
					}
				} else {
					 Log.d(TAG, "Services is not discovered...！");
				}
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			// TODO Auto-generated method stub
			super.onCharacteristicRead(gatt, characteristic, status);
			bleCharacteristicRead(gatt.getDevice(), characteristic.getUuid()
					.toString(), status, characteristic.getValue());
			// String characteristicStr =
			// DataUtil.getStringByBytes(characteristic
			// .getValue());
			// Log.d(TAG, "【characteristic is read】 " + characteristicStr);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			bleCharacteristicWrite(gatt.getDevice(), characteristic.getUuid()
					.toString(), status);
			String characteristicStr = DataUtil.getStringByBytes(characteristic
					.getValue());
			Log.d(TAG, "【characteristic is write】 " + characteristicStr);

		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// TODO Auto-generated method stub
			super.onCharacteristicChanged(gatt, characteristic);
			bleCharacteristicChange(gatt.getDevice(), characteristic.getUuid()
					.toString(), characteristic.getValue());
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
			bleReadRemoteRssi(gatt.getDevice(), rssi, status);
		}
	};

	public class LocalBinder extends Binder {
		public BleService getBleService() {
			return BleService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		mHandler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		disconnectBleDevice();
		return super.onUnbind(intent);
	}

	public static IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(BLE_NOT_SUPPORTED);
		filter.addAction(BLE_NOT_BT_ADAPTER);
		filter.addAction(BLE_STATUS_ABNORMAL);
		filter.addAction(BLE_DEVICE_FOUND);
		filter.addAction(BLE_DEVICE_SCANING);
		filter.addAction(BLE_DEVICE_STOP_SCAN);
		filter.addAction(BLE_GATT_CONNECTED);
		filter.addAction(BLE_GATT_DISCONNECTED);
		filter.addAction(BLE_SERVICE_DISCOVERED);
		filter.addAction(BLE_CHARACTERISTIC_READ);
		filter.addAction(BLE_CHARACTERISTIC_NOTIFICATION);
		filter.addAction(BLE_CHARACTERISTIC_WRITE);
		filter.addAction(BLE_CHARACTERISTIC_CHANGED);
		filter.addAction(BLE_GATT_DISCONNECTTING);
		filter.addAction(BLE_RSSI_READ);
		filter.addAction(BLE_CHARACTERISTIC_FOUND);
		return filter;
	}

	public BluetoothGatt getBluetoothGatt() {
		return mBluetoothGatt;
	}

	/**
	 * 搜索/停止搜索
	 * 
	 * @param isScan
	 */
	public void scanBleDevices(boolean isScan) {
		if (isBleEnabled()) {

			if (isScan) {
				// Log.d(TAG, "开始搜索...10秒后停止");
				if (!isScanning) {
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							isScanning = false;
							scanBleDevices(false);
						}
					}, SCAN_PERIOD);
					isScanning = true;
					mBluetoothAdapter.startLeScan(mLeScanCallback);
					bleDeviceScanning();
				}
			} else {
				// Log.d(TAG, "停止搜索");
				isScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				bleDeviceStopScan();
			}
		} else {
			// not support
		}
	}

	public void rescanBleDevices() {
		if (isBleEnabled()) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (getConnectState() != BluetoothGatt.STATE_CONNECTED
							&& !isBackground) {
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						mBluetoothAdapter.startLeScan(mLeScanCallback);
						rescanBleDevices();
					} else {
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
					}
				}
			}, 2000);
		}
	}

	/**
	 * 连接蓝牙
	 * 
	 * @param device
	 */
	public void connectBleDevice(BluetoothDevice device) {
		this.connectBleDevice(device.getAddress());
	}

	/**
	 * 连接GATT
	 * 
	 * @param address
	 */
	public void connectBleDevice(final String address) {
		// Log.d(TAG, "连接蓝牙...");
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (isBleEnabled()) {
					if (mBluetoothGatt != null) {
						// && mBluetoothDevice.getAddress().equals(address)) {
						if (mBluetoothGatt.connect()) {
							// Log.d(TAG, "connectBleDevice():" + "重新连接？");
							// return;
							mBluetoothGatt.disconnect();
						}
					}
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
					mBluetoothGatt = device.connectGatt(getApplicationContext(), false, // true时，自动连接蓝牙
							mGattCallback);
				} else {
					// Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
				}
			}
		}, 100);
	}

	/**
	 * 断开蓝牙
	 */
	public void disconnectBleDevice() {
		// Log.d(TAG, "断开蓝牙...");
		if (isBleEnabled()) {
			if (mBluetoothGatt != null) {
				mBluetoothGatt.disconnect();
				// mBluetoothGatt.close();//释放会导致发不出broadcast
			}
			bleGattDisconnected();
		} else {
			// Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
		}
		lastDeviceAdress = "";
	}

	/**
	 * 通知。
	 * 
	 * @param characteristic
	 * @param enable
	 */
	public void updateNotificaiton(BluetoothGattCharacteristic characteristic,
			boolean enable) {
		if (isBleEnabled()) {
			// Log.d(TAG,
			// "updateNotificaiton():注册setCharacteristicNotification()");
			mBluetoothGatt
					.setCharacteristicNotification(characteristic, enable);// 设置characteristic通知,当有变化时会返回
			if (enable) {
				BluetoothGattDescriptor descriptor = characteristic
						.getDescriptor(UUID
								.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor
						.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);//
				mBluetoothGatt.writeDescriptor(descriptor);
			}
		} else {
			// Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
		}
	}

	/**
	 * 蓝牙是否可用
	 * 
	 * @return
	 */
	protected boolean isBleEnabled() {
		// Log.d(TAG, "检测蓝牙是否可用");
		boolean isEnabled = false;
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		// 是否打开
		if (!mBluetoothAdapter.isEnabled()) {
			// Log.d(TAG, "检测蓝牙是否打开");
			bleStatusAbnormal();
		} else {
			// 是否支持BLE
			if (getPackageManager().hasSystemFeature(
					PackageManager.FEATURE_BLUETOOTH_LE)) {
				// Log.d(TAG, "支持ble");
				// if (mBluetoothAdapter != null) {
				isEnabled = true;
				// }
			} else {
				bleNotSupported();
				// Log.d(TAG, "not support===================");
			}
		}
		// Log.d(TAG, "isBleEnabled() 蓝牙是否可用: " + isEnabled);
		return isEnabled;
	}

	/** actionIntents **/

	protected void bleNotSupported() {
		// Log.d(TAG, "ble not supported.");
		Intent intent = new Intent(BLE_NOT_SUPPORTED);
		sendBroadcast(intent);
	}

	protected void bleNotBTAdapter() {
		// Log.d(TAG, "ble not bt adapter.");
		Intent intent = new Intent(BLE_NOT_BT_ADAPTER);
		sendBroadcast(intent);
	}

	protected void bleStatusAbnormal() {
		// Log.d(TAG, "ble status abnormal.");
		Intent intent = new Intent(BLE_STATUS_ABNORMAL);
		sendBroadcast(intent);
	}

	/**
	 * 找到设备
	 * 
	 * @param device
	 * @param rssi
	 * @param scanRecord
	 */
	protected void bleDeviceFound(BluetoothDevice device, int rssi,
			byte[] scanRecord) {
		// Log.d(TAG, "device found " + device.getAddress());
		Intent intent = new Intent(BleService.BLE_DEVICE_FOUND);
		intent.putExtra(BleService.EXTRA_DEVICE, device);
		intent.putExtra(BleService.EXTRA_RSSI, rssi);
		intent.putExtra(BleService.EXTRA_SCAN_RECORD, scanRecord);
		sendBroadcast(intent);
	}

	protected void bleDeviceScanning() {
		// Log.d(TAG, "ble device scanning.");
		Intent intent = new Intent(BleService.BLE_DEVICE_SCANING);
		sendBroadcast(intent);
	}

	protected void bleDeviceStopScan() {
		// Log.d(TAG, "ble device stop scan.");
		Intent intent = new Intent(BleService.BLE_DEVICE_STOP_SCAN);
		sendBroadcast(intent);
	}

	protected void bleGattConnected(BluetoothDevice device) {
		// Log.d(TAG, "ble gatt connected.");
		Intent intent = new Intent(BLE_GATT_CONNECTED);
		intent.putExtra(EXTRA_DEVICE_CONNECTED, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		sendBroadcast(intent);
	}

	protected void bleGattDisconnected() {
		Log.e(TAG, "ble gatt disconnted.");
		Intent intent = new Intent(BLE_GATT_DISCONNECTED);
		intent.putExtra("devicePre", mBluetoothDevice);
		sendBroadcast(intent);
	}

	protected void bleGattDisconnectting() {
		// Log.d(TAG, "ble gatt disconnted.");
		Intent intent = new Intent(BLE_GATT_DISCONNECTTING);
		sendBroadcast(intent);
	}

	protected void bleServiceDiscovered(BluetoothDevice device) {
		// Log.d(TAG, "ble service discovered.");
		Intent intent = new Intent(BLE_SERVICE_DISCOVERED);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		sendBroadcast(intent);
	}

	protected void bleCharacteristicRead(BluetoothDevice device, String uuid,
			int status, byte[] value) {
		// // Log.d(TAG, "ble characteristic : " + uuid + "; value : " + value);
		Intent intent = new Intent(BLE_CHARACTERISTIC_READ);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		intent.putExtra(EXTRA_UUID, uuid);
		intent.putExtra(EXTRA_STATUS, status);
		intent.putExtra(EXTRA_VALUE, value);
		sendBroadcast(intent);
	}

	protected void bleCharacteristicNotification(BluetoothDevice device,
			String uuid, boolean isEnabled, int status) {
		// // Log.d(TAG, "ble characteristic : " + uuid +
		// " notification status : "
		// + status);
		Intent intent = new Intent(BLE_CHARACTERISTIC_NOTIFICATION);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		intent.putExtra(EXTRA_UUID, uuid);
		intent.putExtra(EXTRA_STATUS, status);
		intent.putExtra(EXTRA_VALUE, isEnabled);
		sendBroadcast(intent);
	}

	protected void bleCharacteristicWrite(BluetoothDevice device, String uuid,
			int status) {
		// // Log.d(TAG, "ble characteristic : " + uuid + " status : " +
		// status);
		Intent intent = new Intent(BLE_CHARACTERISTIC_WRITE);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		intent.putExtra(EXTRA_UUID, uuid);
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	protected void bleCharacteristicChange(BluetoothDevice device, String uuid,
			byte[] value) {
		// // Log.d(TAG, "ble characteristic : " + uuid + " value : " + value);
		Intent intent = new Intent(BLE_CHARACTERISTIC_CHANGED);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		intent.putExtra(EXTRA_UUID, uuid);
		intent.putExtra(EXTRA_VALUE, value);
		sendBroadcast(intent);
	}

	protected void bleCharacteristicFound() {
		// // Log.d(TAG, "【ble characteristic is found : " + characteristic);
		Intent intent = new Intent(BLE_CHARACTERISTIC_FOUND);
		sendBroadcast(intent);
	}

	protected void bleReadRemoteRssi(BluetoothDevice device, int rssi,
			int status) {
		// // Log.d(TAG, "ble read remote rssi : " + rssi);
		Intent intent = new Intent(BLE_RSSI_READ);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_RSSI, rssi);
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	// public boolean isConnected() {
	// return isConnected;
	// }
	public int getConnectState() {
		return connectState;
	}

	/**
	 * 发送数据
	 * 
	 * @param dataString
	 */
	private void writeCharacteristic(final String dataString) {
		// mHandler.post(new Runnable() {
		// @Override
		// public void run() {
		if (isRightDevice()) {
			if (dataString != null) {
				byte[] data = DataUtil.getBytesByString(dataString);
				mWriteChar.setValue(data);
				// Log.d(TAG, "data :" + data);
				// characteristic.setValue(DataUtil.getBytesByString("0002020002020101"));
				mBluetoothGatt.writeCharacteristic(mWriteChar);
			}
		} else {
			// Log.d(TAG, "连接的设备不匹配！");
		}
		// }
		// });

	}

	/**
	 * 发送数据
	 * 
	 * @param dataString
	 */
	public void writeCharacteristic(MachineStatus status) {
		writeCharacteristic(status.toString());
	}

	public boolean isRightDevice() {
		return mWriteChar != null && mNotiChar != null;
	}

	// public boolean writeCharacteristic(MachineStatus status) {
	// return true;
	// }

	/**
	 * 关闭
	 */
	public void close() {
		if (isBleEnabled()) {
			if (mBluetoothGatt == null) {
				return;
			}
			mBluetoothGatt.close();// 释放
			mBluetoothGatt = null;
		}
	}

	public void setLastDeviceAdress(String lastDeviceAdress) {
		this.lastDeviceAdress = lastDeviceAdress;
	}

	public boolean isBackground() {
		return isBackground;
	}

	public void setBackground(boolean isBackground) {
		this.isBackground = isBackground;
	}

}
