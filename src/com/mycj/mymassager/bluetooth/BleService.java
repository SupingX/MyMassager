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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BleService extends Service {
	private static final String TAG = BleService.class.getSimpleName();
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
	public static String MYMCU_BLE = 	"6e400001-b5a3-f393-e0a9-e50e24dcca9e";
	public static String MYMCU_BLE_READ =  "6e400003-b5a3-f393-e0a9-e50e24dcca9e";;
	public static String MYMCU_BLE_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";;

	private final IBinder mBinder = new LocalBinder();
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothDevice mBluetoothDevice;
	private boolean isScanning;
	private Handler mHandler;
	private boolean isConnected = false;
	private BluetoothGattCharacteristic mWriteChar; // 发
	private BluetoothGattCharacteristic mNotiChar; // 收
	
	/**
	 * LeScanCallback
	 *  	当搜索到设备，在其 onLeScan()传入当前搜索到的设备参数
	 *  		device
	 *  		rssi
	 *  		scanRecord
	 */
	private final LeScanCallback mLeScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			bleDeviceFound(device, rssi, scanRecord);
		}
	};
	
	/**
	 * Gatt连接状态回调
	 * BluetoothGattCallback
	 * 
	 * BlueToothGatt的连接状态，连接信息
	 */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		//连接状态
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			switch (newState) {
			//已连接
			case BluetoothGatt.STATE_CONNECTED:
				mBluetoothDevice = gatt.getDevice();
				isConnected = true;
				if (!gatt.discoverServices()) {
					Log.d(TAG, "discover services failure...");
				} else {
					bleGattConnected(mBluetoothDevice);
				}
				
				break;
			//连接中
			case BluetoothGatt.STATE_CONNECTING:
				//
			break;
			//已断开
			case BluetoothGatt.STATE_DISCONNECTED:
				bleGattDisconnected();
				isConnected = false;
				mBluetoothDevice = null;
				mWriteChar = null;
				mNotiChar = null;
				mBluetoothGatt = null;
				break;
			//断开中
			case BluetoothGatt.STATE_DISCONNECTING:
				bleGattDisconnectting();
				isConnected = false;
				mBluetoothDevice = null;
				mWriteChar = null;
				mNotiChar = null;
				mBluetoothGatt = null;
				break;

			default:
				break;
			}
		}
		
		//连接信息
		//service -- characteristic -- descriptor
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			bleServiceDiscovered(gatt.getDevice());
			List<BluetoothGattService> services = gatt.getServices();
			for	(BluetoothGattService service : services) {
				String serviceUuid = service.getUuid().toString();
				if (serviceUuid.equals(MYMCU_BLE)) {
					Log.d(TAG, "onServicesDiscovered() -->service被发现！" + service.getUuid().toString());
					mWriteChar = service.getCharacteristic(UUID.fromString(MYMCU_BLE_WRITE));
					mNotiChar = service.getCharacteristic(UUID.fromString(MYMCU_BLE_READ));
					if (mNotiChar != null) {
						Log.d(TAG, "onServicesDiscovered() -->Characteristic被发现！" + mNotiChar.toString());
						updateNotificaiton(mNotiChar, true); //发现有notification特性就发送通知
					} else {
						Log.d(TAG, "onServicesDiscovered() -->Characteristic没有发现！");
					}
				} else {
					Log.d(TAG, "onServicesDiscovered() -->service没有发现！");
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
			;
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			// TODO Auto-generated method stub
			super.onCharacteristicWrite(gatt, characteristic, status);
			bleCharacteristicWrite(gatt.getDevice(), characteristic.getUuid()
					.toString(), status);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// TODO Auto-generated method stub
			super.onCharacteristicChanged(gatt, characteristic);
			bleCharacteristicChange(gatt.getDevice(), characteristic.getUuid()
					.toString(), characteristic.getValue());
			;
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "on create");
		mHandler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
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
		return filter;
	}

	public BluetoothGatt getBluetoothGatt() {
		return mBluetoothGatt;
	}
	
	/**
	 * 搜索/停止搜索
	 * @param isScan
	 */
	public void scanBleDevices(boolean isScan) {
		if (isBleEnabled()) {
			if (isScan) {
				Log.d(TAG, "开始搜索...10秒后停止");
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
				Log.d(TAG, "停止搜索");
				isScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				bleDeviceStopScan();
			}
		} else {
			//not support
		}
	}
	
	/**
	 * 连接蓝牙
	 * @param device
	 */
	public void connectBleDevice(BluetoothDevice device) {
		this.connectBleDevice(device.getAddress());
	}
	/**
	 * 连接GATT
	 * @param address
	 */
	public void connectBleDevice(String address) {
		Log.d(TAG, "连接蓝牙...");
		if (isBleEnabled()) {
			if (mBluetoothGatt != null&& mBluetoothDevice.getAddress().equals(address)) {
				if (mBluetoothGatt.connect()) {
					Log.d(TAG, "connectBleDevice():" + "重新连接？");
					return;
				}
			}
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			mBluetoothDevice = device;
			mBluetoothGatt = device.connectGatt(getApplicationContext(), true,
					mGattCallback);
		} else {
			Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
		}
	}
	
	/**
	 * 断开蓝牙
	 */
	public void disconnectBleDevice() {
		Log.d(TAG, "断开蓝牙...");
		if (isBleEnabled()) {
			if (mBluetoothGatt != null) {
				mBluetoothGatt.disconnect();
				mBluetoothGatt = null;
			}
			bleGattDisconnected();
		} else {
			Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
		}
	}
	
	/**
	 * 通知。
	 * @param characteristic
	 * @param enable
	 */
	public void updateNotificaiton(BluetoothGattCharacteristic characteristic,boolean enable) {
		if (isBleEnabled()) {
			Log.d(TAG, "updateNotificaiton():注册setCharacteristicNotification()");
			mBluetoothGatt.setCharacteristicNotification(characteristic, enable);//设置characteristic通知,当有变化时会返回
			if (enable) {
				BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
								.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);//
				mBluetoothGatt.writeDescriptor(descriptor);
			}
		}else {
			Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
		}
	}
	
	/**
	 * 蓝牙是否可用
	 * @return
	 */
	protected boolean isBleEnabled() {
		Log.d("SF","检测蓝牙是否可用");
		boolean isEnabled = false;
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		//是否打开
		if (!mBluetoothAdapter.isEnabled()) {
			Log.d("SF","检测蓝牙是否打开");
			bleStatusAbnormal();
		} else {
			//是否支持BLE
			if (getPackageManager().hasSystemFeature(
					PackageManager.FEATURE_BLUETOOTH_LE)) {
				Log.d("SF","支持ble");
//				if (mBluetoothAdapter != null) {
					isEnabled = true;
//				}
			} else {
				Log.d("SF","not support------------------");
				Toast.makeText(getApplicationContext(), "ble not support",
						Toast.LENGTH_SHORT).show();
				bleNotSupported();
				Log.d("SF","not support===================");
			}
		}
		Log.d(TAG,"isBleEnabled() 蓝牙是否可用: " + isEnabled);
		return isEnabled;
	}
	
	
	/**               actionIntents                    **/
	
	protected void bleNotSupported() {
		Log.d(TAG, "ble not supported.");
		Intent intent = new Intent(BLE_NOT_SUPPORTED);
		sendBroadcast(intent);
	}

	protected void bleNotBTAdapter() {
		Log.d(TAG, "ble not bt adapter.");
		Intent intent = new Intent(BLE_NOT_BT_ADAPTER);
		sendBroadcast(intent);
	}

	protected void bleStatusAbnormal() {
		Log.d(TAG, "ble status abnormal.");
		Intent intent = new Intent(BLE_STATUS_ABNORMAL);
		sendBroadcast(intent);
	}
	
	/**
	 * 找到设备
	 * @param device
	 * @param rssi
	 * @param scanRecord
	 */
	protected void bleDeviceFound(BluetoothDevice device, int rssi,
			byte[] scanRecord) {
		Log.d(TAG, "device found " + device.getAddress());
		Intent intent = new Intent(BleService.BLE_DEVICE_FOUND);
		intent.putExtra(BleService.EXTRA_DEVICE, device);
		intent.putExtra(BleService.EXTRA_RSSI, rssi);
		intent.putExtra(BleService.EXTRA_SCAN_RECORD, scanRecord);
		sendBroadcast(intent);
	}

	protected void bleDeviceScanning() {
		Log.d(TAG, "ble device scanning.");
		Intent intent = new Intent(BleService.BLE_DEVICE_SCANING);
		sendBroadcast(intent);
	}

	protected void bleDeviceStopScan() {
		Log.d(TAG, "ble device stop scan.");
		Intent intent = new Intent(BleService.BLE_DEVICE_STOP_SCAN);
		sendBroadcast(intent);
	}

	protected void bleGattConnected(BluetoothDevice device) {
		Log.d(TAG, "ble gatt connected.");
		Intent intent = new Intent(BLE_GATT_CONNECTED);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		sendBroadcast(intent);
	}

	protected void bleGattDisconnected() {
		Log.d(TAG, "ble gatt disconnted.");
		Intent intent = new Intent(BLE_GATT_DISCONNECTED);
		sendBroadcast(intent);
	}
	
	protected void bleGattDisconnectting() {
		Log.d(TAG, "ble gatt disconnted.");
		Intent intent = new Intent(BLE_GATT_DISCONNECTTING);
		sendBroadcast(intent);
	}

	protected void bleServiceDiscovered(BluetoothDevice device) {
		Log.d(TAG, "ble service discovered.");
		Intent intent = new Intent(BLE_SERVICE_DISCOVERED);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		sendBroadcast(intent);
	}

	protected void bleCharacteristicRead(BluetoothDevice device, String uuid,
			int status, byte[] value) {
		Log.d(TAG, "ble characteristic : " + uuid + "; value : " + value);
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
		Log.d(TAG, "ble characteristic : " + uuid + " notification status : "
				+ status);
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
		Log.d(TAG, "ble characteristic : " + uuid + " status : " + status);
		Intent intent = new Intent(BLE_CHARACTERISTIC_WRITE);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		intent.putExtra(EXTRA_UUID, uuid);
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	protected void bleCharacteristicChange(BluetoothDevice device, String uuid,
			byte[] value) {
		Log.d(TAG, "ble characteristic : " + uuid + " value : " + value);
		Intent intent = new Intent(BLE_CHARACTERISTIC_CHANGED);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_ADDR, device.getAddress());
		intent.putExtra(EXTRA_UUID, uuid);
		intent.putExtra(EXTRA_VALUE, value);
		sendBroadcast(intent);
	}

	protected void bleReadRemoteRssi(BluetoothDevice device, int rssi,
			int status) {
		Log.d(TAG, "ble read remote rssi : " + rssi);
		Intent intent = new Intent(BLE_RSSI_READ);
		intent.putExtra(EXTRA_DEVICE, device);
		intent.putExtra(EXTRA_RSSI, rssi);
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}
	
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * 发送数据
	 * @param dataString
	 */
	public void writeCharacteristic(String dataString) {
		if (isBleEnabled()) {
			if (mBluetoothGatt != null) {
				if (dataString != null) {
					byte[] data = DataUtil.getBytesByString(dataString);
					if (mWriteChar != null) {
						mWriteChar.setValue(data);
						Log.d(TAG, "data :" + data);
						//						characteristic.setValue(DataUtil.getBytesByString("0002020002020101"));
						mBluetoothGatt.writeCharacteristic(mWriteChar);
					} else {
						Log.d(TAG,
								"characteristic is not found! -->characteristic没有找到！");
					}
				}
			} else {
				Log.d(TAG, "mBluetoothGatt is null! -->mBluetoothGatt没有连接！");
				return;
			}
		}else {
			Log.d(TAG, "writeCharacteristic()--手机不支持ble蓝牙！");
		}
	}
	
	/**
	 * 发送数据
	 * @param dataString
	 */
	public void writeCharacteristic(MachineStatus status) {
		 this.writeCharacteristic(status.toString());
	}
	
	
}
