package com.mycj.mymassager;

//https://github.com/SupingX/MyMassager.git

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mycj.mymassager.bluetooth.BleApplication;
import com.mycj.mymassager.bluetooth.BleService;
import com.mycj.mymassager.entity.MachineStatus;
import com.mycj.mymassager.util.DataUtil;
import com.mycj.mymassager.util.SharedPreferenceUtil;
import com.mycj.mymassager.view.MyCountDownTimer;
import com.mycj.mymassager.view.VerticalSeekBar;

/**
 * 机器运行状态
 * 
 * @author Administrator
 * 
 */
public class MainActivity extends Activity implements OnClickListener {
	// private String TAG = this.getClass().getSimpleName();
	private String TAG = "OB";
	private long exitTime = 0;
	private TextView textViewConnectState; // 连接蓝牙状态
	private TextView textViewConnect; // 连接蓝牙
	private TextView textViewMain; // 主机
	private TextView textViewSlave; // 从机

	private CheckBox cbModelMain; // 主机模式
	private CheckBox cbModelSlaver; // 从机模式
	private CheckBox cbFuzaiMain; // 主机负载状态
	private CheckBox cbFuzaiSlave; // 从机负载状态

	private VerticalSeekBar seekBarMain, seekBarSlaver; // 主机/从机进度条
	private ImageView imgMainAdd, imgMainMinus, imgSlaverAdd, imgSlaverMinus; // 主机/从机进度条加减
	private RadioGroup radioGroupMain, radioGroupSlaver; // 主机/从机按摩模式
	private RadioButton radioBtnMainMaster, radioBtnMainAcupuncture,
			radioBtnMainHammer; // 主机三种按摩模式
	private RadioButton radioBtnSlaverMaster, radioBtnSlaverAcupuncture,
			radioBtnSlaverHammer; // 从机三种按摩模式

	private MyCountDownTimer timer; // 计时timer
	private TextView textViewTimer; // 计时
	private ImageView imgStart, imgStop; // 开始/停止

	private ImageView imgMaster; // 按摩仪
	private ImageView imgSetting; // 设置
	private TextView tvMaster; // 按摩仪
	private TextView tvSetting; // 设置

	private ViewPager mViewPager; // viewpager
	private View massagerView;
	private View settingView;
	private List<View> mlist; // 包含按摩仪+设置
	private PagerAdapter mPagerAdapter; // PagerAdapter

	private View line;

	private RelativeLayout rlReconnect; // 返回重连
	private CheckBox checkBoxMotor; // 电机
	/** handler **/
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {

		};
	};

	/** 计时Runnable **/
	private Runnable counterRunnable = new Runnable() {

		public void run() {
			String timeStr = DataUtil.getMMSS(timer.getCurrentTime());
			textViewTimer.setText(timeStr);
			if (timeStr.equals("00:00")) {
				// 计时结束
				Log.d(TAG, "计时结束");

				timer.stop();
				timer = MyCountDownTimer.getInstance(1 * 60 * 1000, 1000);
				mHandler.removeCallbacks(counterRunnable);
				textViewTimer.setText("1:00");
				imgStart.setImageDrawable(getResources().getDrawable(
						R.drawable.selector_start));
				imgStart.setClickable(true);
				instanceMachineStatus();
				Log.d(TAG, mMachineStatus.toString());
				mBleService.writeCharacteristic(mMachineStatus);

				validateSeekBar();
				validateModel();
				return;
			}
			mHandler.postDelayed(this, 1000);
		}
	};

	private MachineStatus mMachineStatus; // 初始状态
	private boolean isConnected;
	private BleService mBleService;
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
			} else if (action.equals(BleService.BLE_DEVICE_STOP_SCAN)) {
			} else if (action.equals(BleService.BLE_DEVICE_FOUND)) {
			} else if (action.equals(BleService.BLE_GATT_CONNECTED)) {
				Log.d("OB", "连接");
				isConnected = true;
				validateBluetoothState();
			} else if (action.equals(BleService.BLE_GATT_DISCONNECTED)) {
				Log.d("OB", "断开");
				// 断开连接
				isConnected = false;
				cbFuzaiMain.setChecked(false);
				validateBluetoothState();
				instanceMachineStatus();
				mBleService.writeCharacteristic(mMachineStatus);
			} else if (action.equals(BleService.BLE_SERVICE_DISCOVERED)) {
				// 发现
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_READ)) {
				Log.d(TAG, "read数据");
			} else if (action
					.equals(BleService.BLE_CHARACTERISTIC_NOTIFICATION)) {
				// 通知
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_WRITE)) {
				// 写入数据
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_CHANGED)) {

				// 读取数据
				Bundle b = intent.getExtras();
				byte[] value = b.getByteArray(BleService.EXTRA_VALUE);
				Log.d(TAG, "CHARACTERISTIC数据变化：" + value);
				if (isConnected) {
					if (value != null) {
						String data = DataUtil.getStringByBytes(value);
						Log.d(TAG, "data:" + data);
						String mainFuzai = data.substring(16, 18);
						if (mainFuzai.equals("01")) {
							// 有负载01
							cbFuzaiMain.setChecked(true); // 主机负载状态
						} else if (mainFuzai.equals("00")) {
							// 无负载00
							cbFuzaiMain.setChecked(false);
						}
						String slaveFuzai = data.substring(19, 20);
						if (slaveFuzai.equals("01")) {
							// 有负载01
							cbFuzaiSlave.setChecked(true);// 从机负载状态
						} else if (slaveFuzai.equals("00")) {
							// 无负载00
							cbFuzaiSlave.setChecked(false);
						}
					} else {
						Log.d(TAG, "没有数据收到");
					}
				}
			} else if (action.equals(BleService.BLE_RSSI_READ)) {
				Log.d(TAG, "read RSSI");
				// RSSI读
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		instanceMachineStatus();
		mBleService = ((BleApplication) getApplication()).getBleService();
		// Log.d("OB", "---"+DataUtil.BinaryToHex("10"));
		// Log.d("OB", "----" + DataUtil.HexToBinary("FF"));
		// Log.d("OB", "----" + Integer.valueOf("FF",16));
		initViews();
		setListener();
		validateBluetoothState();
		validateFuzaiModel();
		validateSeekBar();
		validateModel();

	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mBroadcastReceiver, mBleService.getIntentFilter());
		int i = 0;
		Log.d(TAG, "初始化电机中...");
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (isConnected) {
					Log.d(TAG, "连接成功后，5秒后初始化电机：" + mMachineStatus.toString());
					mBleService.writeCharacteristic(mMachineStatus);
				}
				{
					Log.d(TAG, "连接断开，无法初始化电机");
				}
			}
		}, 5000);

	}

	@Override
	protected void onStop() {
		//退出前关闭电机/开关
//		if (mBleService != null) {
//			Log.d(TAG, "onStop() : " + "关闭操作开始...");
//			mMachineStatus.setMotorStatus("00");
//			mMachineStatus.setStartStatus("00");
//			mBleService.writeCharacteristic(mMachineStatus);
//			Log.d(TAG, " mMachineStatus : " + mMachineStatus.toString());
//			Log.d(TAG, "onStop() : " + "关闭操作结束...");
//			mBleService.disconnectBleDevice();
//		}
		unregisterReceiver(mBroadcastReceiver);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//	    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
//	        if((System.currentTimeMillis()-exitTime) > 2000){  
//	            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
//	            exitTime = System.currentTimeMillis();   
//	        } else {
//	        	
//	            finish();
//	            System.exit(0);
//	        }
//	        return true;   
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}
//	
	public void instanceMachineStatus() {
		String motor  = (String) SharedPreferenceUtil.get(getApplicationContext(), "motor", "00");
		mMachineStatus = new MachineStatus("00", "01",
				"01", "00", "01", "01", "00",motor);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		
	}

	private void initViews() {
		// vierPager
		initViewPager();
		// 底部
		tvMaster = (TextView) findViewById(R.id.tv_tab_massager);
		tvSetting = (TextView) findViewById(R.id.tv_tab_setting);
		imgMaster = (ImageView) findViewById(R.id.img_massager);
		imgSetting = (ImageView) findViewById(R.id.img_setting);
		// main
		line = massagerView.findViewById(R.id.line);
		// 1初始蓝牙状态
		textViewConnectState = (TextView) massagerView
				.findViewById(R.id.tv_connect_state);
		textViewConnect = (TextView) massagerView.findViewById(R.id.tv_connect);
		textViewMain = (TextView) massagerView.findViewById(R.id.tv_main);
		textViewSlave = (TextView) massagerView.findViewById(R.id.tv_slave);
		// 2 负载状态
		// 2.1 主机负载状态
		cbFuzaiMain = (CheckBox) massagerView.findViewById(R.id.cb_main_fuzai);
		// 2.2 从机负载状态
		cbFuzaiSlave = (CheckBox) massagerView
				.findViewById(R.id.cb_slave_fuzai);
		// 3 负载模式
		// 3.1 主机负载模式
		cbModelMain = (CheckBox) massagerView.findViewById(R.id.cb_model_main);
		cbModelMain.setChecked(true);
		// 3.2 从机负载模式
		cbModelSlaver = (CheckBox) massagerView
				.findViewById(R.id.cb_model_affiliate);
		cbModelSlaver.setChecked(true);
		// 4 进度条
		// 4.1 主机进度条
		seekBarMain = (VerticalSeekBar) massagerView
				.findViewById(R.id.seekbar_master);
		// 4.2 从机进度条
		seekBarSlaver = (VerticalSeekBar) massagerView
				.findViewById(R.id.seekbar_slaver);
		// 5 按摩模式
		// 5.1 主机按摩模式
		radioGroupMain = (RadioGroup) massagerView
				.findViewById(R.id.radio_main);
		radioBtnMainMaster = (RadioButton) massagerView
				.findViewById(R.id.rb_main_master);
		radioBtnMainAcupuncture = (RadioButton) massagerView
				.findViewById(R.id.rb_main_acup);
		radioBtnMainHammer = (RadioButton) massagerView
				.findViewById(R.id.rb_main_hammer);
		// 5.2 从机按摩模式
		radioGroupSlaver = (RadioGroup) massagerView
				.findViewById(R.id.rg_slave);
		radioBtnSlaverMaster = (RadioButton) massagerView
				.findViewById(R.id.rb_slave_master);
		radioBtnSlaverAcupuncture = (RadioButton) massagerView
				.findViewById(R.id.rb_slave_acup);
		radioBtnSlaverHammer = (RadioButton) massagerView
				.findViewById(R.id.rb_slave_hammer);
		// 6 加减
		// 6.1 主机加减
		imgMainAdd = (ImageView) massagerView.findViewById(R.id.img_main_add);
		imgMainMinus = (ImageView) massagerView
				.findViewById(R.id.img_main_minus);
		// 6.2 副机加减
		imgSlaverAdd = (ImageView) massagerView
				.findViewById(R.id.img_slave_add);
		imgSlaverMinus = (ImageView) massagerView
				.findViewById(R.id.img_slave_minus);
		// 7 计时器
		textViewTimer = (TextView) massagerView.findViewById(R.id.tv_timer);
		imgStart = (ImageView) massagerView.findViewById(R.id.img_start);
		imgStop = (ImageView) massagerView.findViewById(R.id.img_stop);
		// 8 电机开关
		checkBoxMotor = (CheckBox) settingView.findViewById(R.id.cb_motor);
		checkBoxMotor
				.setChecked(mMachineStatus.getMotorStatus().equals("01") ? true
						: false);
		// 9 返回重连
		rlReconnect = (RelativeLayout) settingView
				.findViewById(R.id.rl_reconnect);

	}

	/**
	 * 加载vierPager
	 */
	private void initViewPager() {
		// viewPage
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		massagerView = getLayoutInflater().inflate(R.layout.tab_massager, null);
		settingView = getLayoutInflater().inflate(R.layout.tab_setting, null);
		mlist = new ArrayList<>();
		mlist.add(massagerView);
		mlist.add(settingView);
		mPagerAdapter = new PagerAdapter() {
			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				// TODO Auto-generated method stub
				return arg0 == arg1;
			}

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return mlist.size();
			}

			@Override
			public void destroyItem(View container, int position, Object object) {
				// TODO Auto-generated method stub
				((ViewPager) container).removeView(mlist.get(position));
			}

			@Override
			public Object instantiateItem(View container, int position) {
				// TODO Auto-generated method stub
				((ViewPager) container).addView(mlist.get(position));
				return mlist.get(position);
			}
		};
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				validateBottom(arg0);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				//
				// Log.d(TAG, "arg0 : " + arg0 + ",arg1 : " + arg1 + ",arg2 : "
				// + arg2);
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});
	}

	/**
	 * 初始蓝牙状态
	 */
	private void validateBluetoothState() {
		if (!isConnected) {
			textViewConnectState.setVisibility(View.VISIBLE);
			textViewConnect.setVisibility(View.VISIBLE);
			textViewMain.setVisibility(View.GONE);
			textViewSlave.setVisibility(View.GONE);
			line.setBackgroundColor(getResources().getColor(R.color.grey));
		} else {
			textViewConnectState.setVisibility(View.GONE);
			textViewConnect.setVisibility(View.GONE);
			textViewMain.setVisibility(View.VISIBLE);
			textViewSlave.setVisibility(View.VISIBLE);
			line.setBackgroundColor(getResources().getColor(R.color.chenghong));
		}
	}

	/**
	 * 主机/从机 负载模式
	 */
	public void validateFuzaiModel() {

		cbModelMain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// 力度
					buttonView
							.setButtonDrawable(R.drawable.selector_model_power);
					seekBarMain.setMax(20);
					seekBarMain.setProgress(Integer.valueOf(
							mMachineStatus.getMainPower(), 16));
				} else {
					// 频率
					buttonView
							.setButtonDrawable(R.drawable.selector_model_frequency);
					seekBarMain.setMax(10);
					Log.d(TAG, mMachineStatus.getMainFreq());
					Log.d(TAG,
							"-->"
									+ Integer.valueOf(
											mMachineStatus.getMainFreq(), 16));
					seekBarMain.setProgress(Integer.valueOf(
							mMachineStatus.getMainFreq(), 16));
				}
			}
		});

		cbModelSlaver.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// 力度
					buttonView
							.setButtonDrawable(R.drawable.selector_model_power);
					seekBarSlaver.setMax(20);
					seekBarSlaver.setProgress(Integer.valueOf(
							mMachineStatus.getSlavePower(), 16));
				} else {
					// 频率
					buttonView
							.setButtonDrawable(R.drawable.selector_model_frequency);
					seekBarSlaver.setMax(10);
					seekBarSlaver.setProgress(Integer.valueOf(
							mMachineStatus.getSlaveFreq(), 16));
				}
			}
		});

	}

	/**
	 * 主机/从机 负载状态
	 */
	public void fuzaiListener() {

		cbFuzaiMain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// 有负载
					cbFuzaiMain.setButtonDrawable(getResources().getDrawable(
							R.drawable.ic_electload_ok));
				} else {
					// 无负载
					cbFuzaiMain.setButtonDrawable(getResources().getDrawable(
							R.drawable.ic_electload));
				}
			}
		});
		// cbFuzaiMain.setChecked(false);
		cbFuzaiSlave.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// 有负载
					cbFuzaiSlave.setButtonDrawable(getResources().getDrawable(
							R.drawable.ic_electload_ok));
				} else {
					// 无负载
					cbFuzaiSlave.setButtonDrawable(getResources().getDrawable(
							R.drawable.ic_electload));
				}
			}
		});
		// cbFuzaiSlave.setChecked(false);
	}

	/**
	 * 进度条Seekbar
	 */
	private void validateSeekBar() {

		if (cbModelMain.isChecked()) {
			// 主机力度
			Log.d(TAG,
					"mMachineStatus.getMainPower():"
							+ mMachineStatus.getMainPower());
			seekBarMain.setProgress(Integer.valueOf(
					mMachineStatus.getMainPower(), 16));
		} else {
			// 主机频率
			Log.d(TAG,
					"mMachineStatus.getMainFreq():"
							+ mMachineStatus.getMainFreq());
			seekBarMain.setProgress(Integer.valueOf(
					mMachineStatus.getMainFreq(), 16));
		}

		if (cbModelSlaver.isChecked()) {
			// 从机力度
			Log.d(TAG,
					"mMachineStatus.getSlavePower():"
							+ mMachineStatus.getSlavePower());
			seekBarSlaver.setProgress(Integer.valueOf(
					mMachineStatus.getSlavePower(), 16));
		} else {
			// 从机频率
			Log.d(TAG,
					"mMachineStatus.getSlaveFreq():"
							+ mMachineStatus.getSlaveFreq());
			seekBarSlaver.setProgress(Integer.valueOf(
					mMachineStatus.getSlaveFreq(), 16));
		}

	}

	public void modelListener() {

		// 主机按摩模式 按摩/针灸/锤击
		radioGroupMain
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						// 按摩master ：0x00
						// 针灸acupuncture：0x01
						// 锤击hammer ：0x02
						if (cbModelMain.isChecked()) {
							// 力度 -->seekbar为1
							seekBarMain.setProgress(1);
							mMachineStatus.setMainPower("01");
						}

						switch (checkedId) {
						case R.id.rb_main_master:
							radioBtnMainMaster
									.setButtonDrawable(R.drawable.ic_massage_pressed);
							radioBtnMainAcupuncture
									.setButtonDrawable(R.drawable.selector_acup);
							radioBtnMainHammer
									.setButtonDrawable(R.drawable.selector_hammer);
							mMachineStatus.setMainModel("00");
							break;
						case R.id.rb_main_acup:
							radioBtnMainMaster
									.setButtonDrawable(R.drawable.selector_massager);
							radioBtnMainAcupuncture
									.setButtonDrawable(R.drawable.ic_acupuncture_pressed);
							radioBtnMainHammer
									.setButtonDrawable(R.drawable.selector_hammer);
							mMachineStatus.setMainModel("01");
							break;
						case R.id.rb_main_hammer:
							radioBtnMainMaster
									.setButtonDrawable(R.drawable.selector_massager);
							radioBtnMainAcupuncture
									.setButtonDrawable(R.drawable.selector_acup);
							radioBtnMainHammer
									.setButtonDrawable(R.drawable.ic_hammer_pressed);
							mMachineStatus.setMainModel("02");
							break;

						default:
							break;
						}
						if (mBleService != null) {
							mBleService.writeCharacteristic(mMachineStatus);
						}
					}
				});
		// 从机按摩模式：按摩/针灸/锤击
		radioGroupSlaver
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					// 按摩master ：0x00
					// 针灸acupuncture：0x01
					// 锤击hammer ：0x02
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (cbModelSlaver.isChecked()) {
							// 力度 -->seekbar为1
							seekBarSlaver.setProgress(1);
							mMachineStatus.setSlavePower("01");
						}
						switch (checkedId) {
						case R.id.rb_slave_master:
							radioBtnSlaverMaster
									.setButtonDrawable(R.drawable.ic_massage_pressed);
							radioBtnSlaverAcupuncture
									.setButtonDrawable(R.drawable.selector_acup);
							radioBtnSlaverHammer
									.setButtonDrawable(R.drawable.selector_hammer);
							mMachineStatus.setSlaveModel("00");
							break;
						case R.id.rb_slave_acup:
							radioBtnSlaverMaster
									.setButtonDrawable(R.drawable.selector_massager);
							radioBtnSlaverAcupuncture
									.setButtonDrawable(R.drawable.ic_acupuncture_pressed);
							radioBtnSlaverHammer
									.setButtonDrawable(R.drawable.selector_hammer);
							mMachineStatus.setSlaveModel("01");
							break;
						case R.id.rb_slave_hammer:
							radioBtnSlaverMaster
									.setButtonDrawable(R.drawable.selector_massager);
							radioBtnSlaverAcupuncture
									.setButtonDrawable(R.drawable.selector_acup);
							radioBtnSlaverHammer
									.setButtonDrawable(R.drawable.ic_hammer_pressed);
							mMachineStatus.setSlaveModel("02");
							break;

						default:
							break;
						}
						if (mBleService != null) {
							mBleService.writeCharacteristic(mMachineStatus);
						}
					}
				});
	}

	/**
	 * 主机/从机按摩模式
	 */
	public void validateModel() {
		Log.d(TAG, "主机模式:" + Integer.valueOf(mMachineStatus.getMainModel(), 16));
		// 初始化主机按摩模式
		switch (Integer.valueOf(mMachineStatus.getMainModel(), 16)) {
		case 0:
			radioGroupMain.check(R.id.rb_main_master);
			break;
		case 1:
			radioGroupMain.check(R.id.rb_main_acup);
			break;
		case 2:
			radioGroupMain.check(R.id.rb_main_hammer);
			break;
		default:
			break;
		}

		// 初始化从机按摩模式
		switch (Integer.valueOf(mMachineStatus.getSlaveModel(), 16)) {
		case 0:
			radioGroupSlaver.check(R.id.rb_slave_master);
			break;
		case 1:
			radioGroupSlaver.check(R.id.rb_slave_acup);
			break;
		case 2:
			radioGroupSlaver.check(R.id.rb_slave_hammer);
			break;
		default:
			break;
		}
	}

	/**
	 * 加减
	 */
	private void validateAddListener() {

		imgMainAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isConnected) {
					String start = mMachineStatus.getStartStatus(); // 只有开始才能加减
					if (start.equals("01")) {
						int mainCurrentProgress = seekBarMain.getProgress() + 1;
						if (cbModelMain.isChecked()) {
							// 当负载为power时
							if (mainCurrentProgress >= 20) {
								mainCurrentProgress = 20;
							}
							seekBarMain.setProgress(mainCurrentProgress);
							mMachineStatus.setMainPower(DataUtil
									.toHexString(mainCurrentProgress));
							Log.d(TAG, mMachineStatus.toString());
							mBleService.writeCharacteristic(mMachineStatus);
						} else {
							if (mainCurrentProgress >= 10) {
								mainCurrentProgress = 10;
							}
							seekBarMain.setProgress(mainCurrentProgress);
							// 负载为freq频率时
							mMachineStatus.setMainFreq(DataUtil
									.toHexString(mainCurrentProgress));
							Log.d(TAG, mMachineStatus.toString());
							mBleService.writeCharacteristic(mMachineStatus);
						}
					} else {
						Log.d(TAG, "请按开始增减强度/频率");
					}
				} else {
					Toast.makeText(getApplicationContext(), "请连接蓝牙", 0).show();
				}
			}
		});

		imgMainMinus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isConnected) {
					String start = mMachineStatus.getStartStatus();
					if (start.equals("01")) {
						int mainCurrentProgress = seekBarMain.getProgress() - 1;
						if (mainCurrentProgress <= 0) {
							mainCurrentProgress = 0;
						}
						seekBarMain.setProgress(mainCurrentProgress);
						if (cbModelMain.isChecked()) {// 当负载为power时
							mMachineStatus.setMainPower(DataUtil
									.toHexString(mainCurrentProgress));
							Log.d(TAG, mMachineStatus.toString());
							mBleService.writeCharacteristic(mMachineStatus);
						} else { // 负载为freq频率时
							mMachineStatus.setMainFreq(DataUtil
									.toHexString(mainCurrentProgress));
							Log.d(TAG, mMachineStatus.toString());
							mBleService.writeCharacteristic(mMachineStatus);
						}
					} else {
						Log.d(TAG, "请按开始增减强度/频率");
					}
				} else {
					Toast.makeText(getApplicationContext(), "请连接蓝牙", 0).show();
				}
			}
		});

		imgSlaverAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isConnected) {
					String start = mMachineStatus.getStartStatus(); // 只有开始才能加减
					if (start.equals("01")) {
						int slaveCurrentProgress = seekBarSlaver.getProgress() + 1;
						if (cbModelSlaver.isChecked()) {
							// 当负载为power时
							if (slaveCurrentProgress >= 20) {
								slaveCurrentProgress = 20;
							}
							seekBarSlaver.setProgress(slaveCurrentProgress);
							mMachineStatus.setSlavePower(DataUtil
									.toHexString(slaveCurrentProgress));
							Log.d(TAG, mMachineStatus.toString());
							mBleService.writeCharacteristic(mMachineStatus);
						} else {
							if (slaveCurrentProgress >= 10) {
								slaveCurrentProgress = 10;
							}
							seekBarSlaver.setProgress(slaveCurrentProgress);
							// 负载为freq频率时
							mMachineStatus.setSlaveFreq(DataUtil
									.toHexString(slaveCurrentProgress));
							Log.d(TAG, mMachineStatus.toString());
							mBleService.writeCharacteristic(mMachineStatus);
						}
					} else {
						Log.d(TAG, "请按开始增减强度/频率");
					}
				} else {
					Toast.makeText(getApplicationContext(), "请连接蓝牙", 0).show();
				}
			}
		});

		imgSlaverMinus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isConnected) {
					String start = mMachineStatus.getStartStatus(); // 只有开始才能加减
					if (start.equals("01")) {
						int slaveCurrentProgress = seekBarSlaver.getProgress() - 1;
						if (slaveCurrentProgress <= 0) {
							slaveCurrentProgress = 0;
						}
						seekBarSlaver.setProgress(slaveCurrentProgress);
						if (cbModelSlaver.isChecked()) {// 当负载为power时
							mMachineStatus.setSlavePower(DataUtil
									.toHexString(slaveCurrentProgress));
							mBleService.writeCharacteristic(mMachineStatus);
						} else { // 负载为freq频率时
							mMachineStatus.setSlaveFreq(DataUtil
									.toHexString(slaveCurrentProgress));
							mBleService.writeCharacteristic(mMachineStatus);
						}
					} else {
						Log.d(TAG, "请按开始增减强度/频率");
					}
				} else {
					Toast.makeText(getApplicationContext(), "请连接蓝牙", 0).show();
				}

			}
		});
	}

	/**
	 * 计时
	 */
	private void timerListener() {
		timer = MyCountDownTimer.getInstance(1 * 60 * 1000, 1000);
		String timeStr = DataUtil.getMMSS(timer.getCurrentTime());
		textViewTimer.setText(timeStr);
		// 开始计时
		// 开始 0x01
		// 结束 0x00
		imgStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isConnected) {
					imgStart.setImageDrawable(getResources().getDrawable(
							R.drawable.ic_start_pressed));
					mMachineStatus.setStartStatus("01");
					// 发送
					Log.d(TAG, mMachineStatus.toString());
					mBleService.writeCharacteristic(mMachineStatus);
					timer.start();
					mHandler.postDelayed(counterRunnable, 1000);
				} else {
					Toast.makeText(getApplicationContext(), "请连接蓝牙", 0).show();
				}
			}
		});
		// 结束计时
		imgStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isConnected) {
					timer.stop();
					timer = MyCountDownTimer.getInstance(1 * 60 * 1000, 1000);
					mHandler.removeCallbacks(counterRunnable);
					textViewTimer.setText("1:00");
					imgStart.setImageDrawable(getResources().getDrawable(
							R.drawable.selector_start));
					imgStart.setClickable(true);
					mMachineStatus = new MachineStatus(getApplicationContext(),
							"00", "01", "01", "00", "01", "01", "00");

					Log.d(TAG, mMachineStatus.toString());
					mBleService.writeCharacteristic(mMachineStatus);
					validateSeekBar();
					validateModel();
				} else {
					Log.d(TAG, "已断开连接");
				}
			}
		});
	}

	/**
	 * 电机开关
	 */
	private void motorListener() {
		checkBoxMotor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					checkBoxMotor.setButtonDrawable(R.drawable.ic_switch_on);
					mMachineStatus.setMotorStatus("01");
					SharedPreferenceUtil.put(getApplicationContext(), "motor", "01");
					Log.d(TAG, "开关电机：开！ " + mMachineStatus.toString());
					if (isConnected) {
						mBleService.writeCharacteristic(mMachineStatus);
					} else {
						Log.d(TAG, "开关电机：网络已断开！");
					}
				} else {
					checkBoxMotor.setButtonDrawable(R.drawable.ic_switch_off);
					mMachineStatus.setMotorStatus("00");
					SharedPreferenceUtil.put(getApplicationContext(), "motor", "00");
					Log.d(TAG, "开关电机：关！ " + mMachineStatus.toString());
					if (isConnected) {
						mBleService.writeCharacteristic(mMachineStatus);
					} else {
						Log.d(TAG, "开关电机：网络已断开！");
					}
				}

			}
		});
	}

	private void setListener() {
		// 设置
		imgSetting.setOnClickListener(this);
		// 按摩仪
		imgMaster.setOnClickListener(this);
		// 连接蓝牙
		textViewConnect.setOnClickListener(this);
		// 重连蓝牙
		rlReconnect.setOnClickListener(this);
		// 负载模式选择：力度power 频率freq
		fuzaiListener();
		// 主机/从机按摩模式：按摩/针灸/锤击
		modelListener();
		// 加减
		validateAddListener();
		// 开始结束
		timerListener();
		// 电机
		motorListener();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.img_massager:
			mViewPager.setCurrentItem(0);
			validateBottom(0);
			break;
		case R.id.img_setting:
			mViewPager.setCurrentItem(1);
			validateBottom(1);
			break;
		case R.id.tv_connect:
			Intent connectIntent = new Intent(MainActivity.this,
					ConnectActivity.class);
			startActivity(connectIntent);
			finish();
			break;
		case R.id.rl_reconnect:
			if (mBleService != null) {
				mBleService.disconnectBleDevice();
			}
			Intent reconnectIntent = new Intent(MainActivity.this,
					ConnectActivity.class);
			startActivity(reconnectIntent);
			finish();
			break;

		default:
			break;
		}
	}

	public void validateBottom(int flag) {
		switch (flag) {
		case 0:
			tvMaster.setTextColor(getResources().getColor(R.color.green_1));
			tvSetting.setTextColor(getResources().getColor(R.color.grey));
			imgMaster.setImageResource(R.drawable.ic_tab_massager_selected);
			imgSetting.setImageResource(R.drawable.ic_tab_setting);
			break;
		case 1:
			tvMaster.setTextColor(getResources().getColor(R.color.grey));
			tvSetting.setTextColor(getResources().getColor(R.color.green_1));
			imgMaster.setImageResource(R.drawable.ic_tab_massager);
			imgSetting.setImageResource(R.drawable.ic_tab_setting_selected);
			break;

		default:
			break;
		}
	}

	


}
