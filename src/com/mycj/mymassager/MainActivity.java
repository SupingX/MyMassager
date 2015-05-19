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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mycj.mymassager.bluetooth.BleApplication;
import com.mycj.mymassager.bluetooth.BleService;
import com.mycj.mymassager.entity.MachineStatus;
import com.mycj.mymassager.util.DataUtil;
import com.mycj.mymassager.view.MyCountDownTimer;
import com.mycj.mymassager.view.VerticalSeekBar;

/**
 * 机器运行状态
 * 
 * @author Administrator
 * 
 */
public class MainActivity extends Activity implements OnClickListener {
	private String TAG = this.getClass().getSimpleName();
	private TextView textViewConnectState; // 连接蓝牙状态
	private TextView textViewConnect; // 连接蓝牙
	private TextView textViewMain; // 主机
	private TextView textViewSlave; // 从机

	private CheckBox cbModelMain; // 主机模式
	private CheckBox cbModelSlaver; // 从机模式
	private CheckBox cbFuzaiMain; //  主机负载状态
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
	
	private TextView textViewReconnect; // 返回重连
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
				// // 计时结束
				// // System.out.println("计时结束");
				// mHandler.removeCallbacks(this);
				// restore();
				// myApp.requestWriteCharacteristic("FD00");
				// // iv_start.setImageResource(R.drawable.select_button_start);
				// startActivity(intentShutDown);
				return;
			}
			mHandler.postDelayed(this, 1000);
		}
	};
	
	private final static int REQUEST_CONNECT = 0X01; // 连接蓝牙请求
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
				validate();
			} else if (action.equals(BleService.BLE_GATT_DISCONNECTED)) {
				Log.d("OB", "断开");
				// 断开连接
				isConnected = false;
				validate();
			} else if (action.equals(BleService.BLE_SERVICE_DISCOVERED)) {
				// 发现
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_READ)) {
				Log.d(TAG, "read数据");
				
			
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_NOTIFICATION)) {
				// 通知
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_WRITE)) {
				// 写入数据
			} else if (action.equals(BleService.BLE_CHARACTERISTIC_CHANGED)) {
				Log.d(TAG, "数据变化");
				// 读取数据
				Bundle b = intent.getExtras();
				byte[] value =  b.getByteArray(BleService.EXTRA_VALUE);
				Log.d(TAG, "value:" + value);
				if(value!=null){
					String data = DataUtil.getStringByBytes(value);
					Log.d(TAG, "data:" + data);
				} else {
					Log.d(TAG, "没有数据收到");
				}
				// 数据变化
			}  else if (action.equals(BleService.BLE_RSSI_READ)) {
				Log.d(TAG, "read RSSI");
			// RSSI读
			} 
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mMachineStatus = new MachineStatus("00", "01", "01", "00", "01", "01",
				"00", "01");
		mBleService = ((BleApplication) getApplication()).getBleService();
		// Log.d("OB", "---"+DataUtil.BinaryToHex("10"));
		// Log.d("OB", "----" + DataUtil.HexToBinary("FF"));
		// Log.d("OB", "----" + Integer.valueOf("FF",16));
		initViews();
		setListener();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mBroadcastReceiver, mBleService.getIntentFilter());
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mBroadcastReceiver);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBleService.disconnectBleDevice();
	}
	
	private void initViews() {
		// vierPager
		initViewPager();
		// 底部
		tvMaster = (TextView) findViewById(R.id.tv_tab_massager);
		tvSetting = (TextView) findViewById(R.id.tv_tab_setting);
		imgMaster = (ImageView) findViewById(R.id.img_massager);
		imgSetting = (ImageView) findViewById(R.id.img_setting);
		//main
		line = massagerView.findViewById(R.id.line);
		// 1初始蓝牙状态
		initBluetoothState();
		// 2负载
		initFuzai();
		// 3主机/从机进度条
		initSeekBar();
		// 4按摩模式
		initModel();
		// 5加减
		initAddMinus();
		// 6计时器
		initTimer();
		//slave
		// 1电机开关
		initMotor();
		// 2返回重连
		textViewReconnect = (TextView) settingView.findViewById(R.id.tv_reconnect);
		textViewReconnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBleService.disconnectBleDevice();
				Intent connectIntent = new Intent(MainActivity.this,
						ConnectActivity.class);
				startActivityForResult(connectIntent, REQUEST_CONNECT);
			}
		});
		
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
				Log.d(TAG, "arg0 : " + arg0 + ",arg1 : " + arg1 + ",arg2 : "
						+ arg2);
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});
	}

	/**
	 * 初始蓝牙状态
	 */
	private void initBluetoothState() {

		// 初始蓝牙状态
		textViewConnectState = (TextView) massagerView
				.findViewById(R.id.tv_connect_state);
		textViewConnect = (TextView) massagerView.findViewById(R.id.tv_connect);
		// 主机/从机
		textViewMain = (TextView) massagerView.findViewById(R.id.tv_main);
		textViewSlave = (TextView) massagerView.findViewById(R.id.tv_slave);
		validate();

	}

	/**
	 * 主机/从机 负载模式
	 */
	public void initFuzai() {
		// 负载模式
		// 1.主机负载模式
		cbModelMain = (CheckBox) massagerView.findViewById(R.id.cb_model_main);
		// 初始化主机负载模式
		cbModelMain.setChecked(true);
		cbModelMain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					buttonView
							.setButtonDrawable(R.drawable.selector_model_power);
					seekBarMain.setMax(20);
					seekBarMain.setProgress(1);
				} else {
					buttonView
							.setButtonDrawable(R.drawable.selector_model_frequency);
					seekBarMain.setMax(10);
					seekBarMain.setProgress(1);
				}
			}
		});
		// 2.从机负载模式
		cbModelSlaver = (CheckBox) massagerView
				.findViewById(R.id.cb_model_affiliate);
		cbModelMain.setChecked(true);
		cbModelSlaver.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					buttonView
							.setButtonDrawable(R.drawable.selector_model_power);
					seekBarSlaver.setMax(20);
					seekBarSlaver.setProgress(1);
				} else {
					buttonView
							.setButtonDrawable(R.drawable.selector_model_frequency);
					seekBarSlaver.setMax(10);
					seekBarSlaver.setProgress(1);
				}
			}
		});

	}
	
	/**
	 * 主机/从机 负载模式
	 */
	public void initFuzaiState() {
		cbFuzaiMain = (CheckBox) massagerView.findViewById(R.id.cb_main_fuzai);
		cbFuzaiSlave = (CheckBox) massagerView.findViewById(R.id.cb_slave_fuzai);
	}
	/**
	 * 进度条Seekbar
	 */
	private void initSeekBar() {
		// seekbar
		seekBarMain = (VerticalSeekBar) massagerView
				.findViewById(R.id.seekbar_master);
		seekBarMain.setMax(20);
		seekBarMain.setProgress(1);
		seekBarMain.setProgress(Integer.valueOf(mMachineStatus.getMainPower()));
		seekBarSlaver = (VerticalSeekBar) massagerView
				.findViewById(R.id.seekbar_slaver);
		seekBarSlaver.setMax(20);
		seekBarSlaver.setProgress(1);
	}

	/**
	 * 主机/从机按摩模式
	 */
	public void initModel() {
		// 按摩模式
		// 1.主机按摩模式
		radioGroupMain = (RadioGroup) massagerView
				.findViewById(R.id.radio_main);
		radioBtnMainMaster = (RadioButton) massagerView
				.findViewById(R.id.rb_main_master);
		radioBtnMainAcupuncture = (RadioButton) massagerView
				.findViewById(R.id.rb_main_acup);
		radioBtnMainHammer = (RadioButton) massagerView
				.findViewById(R.id.rb_main_hammer);
		radioGroupMain
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						// 按摩master ：0x00
						// 针灸acupuncture：0x01
						// 锤击hammer ：0x02
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
					}
				});
		radioGroupMain.check(radioBtnMainMaster.getId());

		// 2.从机按摩模式
		radioGroupSlaver = (RadioGroup) massagerView
				.findViewById(R.id.rg_slave);
		radioBtnSlaverMaster = (RadioButton) massagerView
				.findViewById(R.id.rb_slave_master);
		radioBtnSlaverAcupuncture = (RadioButton) massagerView
				.findViewById(R.id.rb_slave_acup);
		radioBtnSlaverHammer = (RadioButton) massagerView
				.findViewById(R.id.rb_slave_hammer);
		radioGroupSlaver
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					// 按摩master ：0x00
					// 针灸acupuncture：0x01
					// 锤击hammer ：0x02
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
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
					}
				});
		radioGroupSlaver.check(radioBtnSlaverMaster.getId());
	}

	/**
	 * 加减
	 */
	private void initAddMinus() {
		// 加减
		// 频率 0x01 ~ 0x0a
		// 强度 0x01 ~ 0x14
		imgMainAdd = (ImageView) massagerView.findViewById(R.id.img_main_add);
		imgMainAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (isConnected) {
					int mainCurrentProgress = seekBarMain.getProgress() + 1;
					if (mainCurrentProgress >= 20) {
						mainCurrentProgress = 20;
					}
					seekBarMain.setProgress(mainCurrentProgress);
					if (cbModelMain.isChecked()) {// 当负载为power时
						mMachineStatus.setMainPower(Integer
								.toHexString(mainCurrentProgress));
					} else { // 负载为freq频率时
						mMachineStatus.setMainFreq(Integer
								.toHexString(mainCurrentProgress));
					}
				} else {
					Toast.makeText(getApplicationContext(), "请连接蓝牙", 0).show();
				}
			}
		});
		imgMainMinus = (ImageView) massagerView
				.findViewById(R.id.img_main_minus);
		imgMainMinus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekBarMain.setProgress(seekBarMain.getProgress() - 1);
			}
		});
		imgSlaverAdd = (ImageView) massagerView
				.findViewById(R.id.img_slave_add);
		imgSlaverAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekBarSlaver.setProgress(seekBarSlaver.getProgress() + 1);
			}
		});
		imgSlaverMinus = (ImageView) massagerView
				.findViewById(R.id.img_slave_minus);
		imgSlaverMinus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (isConnected) {
					int slaveCurrentProgress = seekBarSlaver.getProgress() - 1;
					if (slaveCurrentProgress <= 0) {
						slaveCurrentProgress = 0;
					}
					seekBarSlaver.setProgress(slaveCurrentProgress);
					if (cbModelSlaver.isChecked()) {// 当负载为power时
						mMachineStatus.setSlavePower(Integer
								.toHexString(slaveCurrentProgress));
					} else { // 负载为freq频率时
						mMachineStatus.setSlaveFreq(Integer
								.toHexString(slaveCurrentProgress));
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
	private void initTimer() {
		// 计时器
		textViewTimer = (TextView) massagerView.findViewById(R.id.tv_timer);
		timer = MyCountDownTimer.getInstance(15 * 60 * 1000, 1000);
		imgStart = (ImageView) massagerView.findViewById(R.id.img_start);
		imgStop = (ImageView) massagerView.findViewById(R.id.img_stop);
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
				timer.stop();
				mHandler.removeCallbacks(counterRunnable);
				textViewTimer.setText("15:00");
				imgStart.setImageDrawable(getResources().getDrawable(
						R.drawable.selector_start));
				imgStart.setClickable(true);
				//
				// mMachineStatus = null ;
			}
		});
	}
	
	/**
	 * 电机开关
	 */
	private void initMotor(){
		checkBoxMotor = (CheckBox) settingView.findViewById(R.id.cb_motor);
		checkBoxMotor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					Toast.makeText(getApplicationContext(), "开！", 0).show();
				} else {
					Toast.makeText(getApplicationContext(), "关！", 0).show();
				}
			}
		});
	}

	private void setListener() {
		imgSetting.setOnClickListener(this);
		imgMaster.setOnClickListener(this);
		textViewConnect.setOnClickListener(this);
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


	/**
	 * 当前连接则不显示返回重连
	 */
	private void validate() {
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
	
	
}
