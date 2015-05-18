package com.mycj.mymassager;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mycj.mymassager.util.DataUtil;
import com.mycj.mymassager.view.MyCountDownTimer;
import com.mycj.mymassager.view.VerticalSeekBar;

public class MainActivity extends Activity implements OnClickListener {
	private ImageView imgMaster;
	private ImageView imgSetting;
	private CheckBox cbModelMain;
	private CheckBox cbModelSlaver;
	
	private RadioGroup radioGroupMain,radioGroupSlaver;
	private RadioButton radioBtnMainMaster,radioBtnMainAcupuncture,radioBtnMainHammer;
	private RadioButton radioBtnSlaverMaster,radioBtnSlaverAcupuncture,radioBtnSlaverHammer;
	private TextView tvMaster;
	private TextView tvSetting;
	private TextView textViewConnectState;
	private TextView textViewConnect;
	private VerticalSeekBar seekBarMain;
	private VerticalSeekBar seekBarSlaver;
	private ImageView imgMainAdd,imgMainMinus,imgSlaverAdd,imgSlaverMinus;
	
	private ViewPager mViewPager;
	private List<View> mlist;
	private PagerAdapter mPagerAdapter;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			
		};
	};
	private MyCountDownTimer timer;
	private TextView textViewTimer;
	private ImageView imgStart,imgStop;
	private Runnable counterRunnable = new Runnable() {

		public void run() {
			String timeStr = DataUtil.getMMSS(timer.getCurrentTime());
			textViewTimer.setText(timeStr);
			if (timeStr.equals("00:00")) {
//				// 计时结束
//				// System.out.println("计时结束");
//				mHandler.removeCallbacks(this);
//				restore();
//				myApp.requestWriteCharacteristic("FD00");
//				// iv_start.setImageResource(R.drawable.select_button_start);
//				startActivity(intentShutDown);
				return;
			}
			mHandler.postDelayed(this, 1000);
		}
	};
	
	private final static int REQUEST_CONNECT = 0X01;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();
		setListener();
	}
	
	private void initViews(){
	
		//底部
		tvMaster = (TextView) findViewById(R.id.tv_tab_massager);
		tvSetting = (TextView) findViewById(R.id.tv_tab_setting);
		imgMaster = (ImageView) findViewById(R.id.img_massager);
		imgSetting = (ImageView) findViewById(R.id.img_setting);
		
		//viewPage
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		View massagerView = getLayoutInflater().inflate(R.layout.tab_massager, null);
		View settingView = getLayoutInflater().inflate(R.layout.tab_setting, null);
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
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		//初始蓝牙状态
		textViewConnectState = (TextView) massagerView.findViewById(R.id.tv_connect_state);
		textViewConnect = (TextView) massagerView.findViewById(R.id.tv_connect);
		
		// seekbar 
		seekBarMain = (VerticalSeekBar) massagerView.findViewById(R.id.seekbar_master);
		seekBarMain.setMax(20);
		seekBarMain.setProgress(1);
		seekBarSlaver = (VerticalSeekBar) massagerView.findViewById(R.id.seekbar_slaver);
		seekBarSlaver.setMax(20);
		seekBarSlaver.setProgress(1);
		
		//负载模式
			//1.主机负载模式
		cbModelMain = (CheckBox) massagerView.findViewById(R.id.cb_model_main);
		cbModelMain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					buttonView.setButtonDrawable(R.drawable.selector_model_power);
					seekBarMain.setMax(20);
					seekBarMain.setProgress(1);
				}else{
					buttonView.setButtonDrawable(R.drawable.selector_model_frequency);
					seekBarMain.setMax(10);
					seekBarMain.setProgress(1);
				}
			}
		});
			//2.从机负载模式	
		cbModelSlaver = (CheckBox) massagerView.findViewById(R.id.cb_model_affiliate);
		cbModelSlaver.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					buttonView.setButtonDrawable(R.drawable.selector_model_power);
					seekBarSlaver.setMax(20);
					seekBarSlaver.setProgress(1);
				}else{
					buttonView.setButtonDrawable(R.drawable.selector_model_frequency);
					seekBarSlaver.setMax(10);
					seekBarSlaver.setProgress(1);
				}
			}
		});
		
		//按摩模式
			//1.主机按摩模式
		radioGroupMain = (RadioGroup) massagerView.findViewById(R.id.radio_main);
		radioBtnMainMaster = (RadioButton) massagerView.findViewById(R.id.rb_main_master);
		radioBtnMainAcupuncture = (RadioButton) massagerView.findViewById(R.id.rb_main_acup);
		radioBtnMainHammer = (RadioButton) massagerView.findViewById(R.id.rb_main_hammer);
		radioGroupMain.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_main_master:
					radioBtnMainMaster.setButtonDrawable(R.drawable.ic_massage_pressed);
					radioBtnMainAcupuncture.setButtonDrawable(R.drawable.selector_acup);
					radioBtnMainHammer.setButtonDrawable(R.drawable.selector_hammer);
					break;
				case R.id.rb_main_acup:
					radioBtnMainMaster.setButtonDrawable(R.drawable.selector_massager);
					radioBtnMainAcupuncture.setButtonDrawable(R.drawable.ic_acupuncture_pressed);
					radioBtnMainHammer.setButtonDrawable(R.drawable.selector_hammer);
					break;
				case R.id.rb_main_hammer:
					radioBtnMainMaster.setButtonDrawable(R.drawable.selector_massager);
					radioBtnMainAcupuncture.setButtonDrawable(R.drawable.selector_acup);
					radioBtnMainHammer.setButtonDrawable(R.drawable.ic_hammer_pressed);
					break;

				default:
					break;
				}
			}
		});
		radioGroupMain.check(radioBtnMainMaster.getId());
		
			//2.从机按摩模式
		radioGroupSlaver = (RadioGroup) massagerView.findViewById(R.id.rg_slave);
		radioBtnSlaverMaster = (RadioButton) massagerView.findViewById(R.id.rb_slave_master);
		radioBtnSlaverAcupuncture = (RadioButton) massagerView.findViewById(R.id.rb_slave_acup);
		radioBtnSlaverHammer = (RadioButton) massagerView.findViewById(R.id.rb_slave_hammer);
		radioGroupSlaver.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_slave_master:
					radioBtnSlaverMaster.setButtonDrawable(R.drawable.ic_massage_pressed);
					radioBtnSlaverAcupuncture.setButtonDrawable(R.drawable.selector_acup);
					radioBtnSlaverHammer.setButtonDrawable(R.drawable.selector_hammer);
					break;
				case R.id.rb_slave_acup:
					radioBtnSlaverMaster.setButtonDrawable(R.drawable.selector_massager);
					radioBtnSlaverAcupuncture.setButtonDrawable(R.drawable.ic_acupuncture_pressed);
					radioBtnSlaverHammer.setButtonDrawable(R.drawable.selector_hammer);
					break;
				case R.id.rb_slave_hammer:
					radioBtnSlaverMaster.setButtonDrawable(R.drawable.selector_massager);
					radioBtnSlaverAcupuncture.setButtonDrawable(R.drawable.selector_acup);
					radioBtnSlaverHammer.setButtonDrawable(R.drawable.ic_hammer_pressed);
					break;
					
				default:
					break;
				}
			}
		});
		radioGroupSlaver.check(radioBtnSlaverMaster.getId());
	
		//加减
		imgMainAdd = (ImageView) massagerView.findViewById(R.id.img_main_add);
		imgMainAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekBarMain.setProgress(seekBarMain.getProgress()+1);
			}
		});
		imgMainMinus = (ImageView) massagerView.findViewById(R.id.img_main_minus);
		imgMainMinus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekBarMain.setProgress(seekBarMain.getProgress()-1);
			}
		});
		imgSlaverAdd = (ImageView) massagerView.findViewById(R.id.img_slave_add);
		imgSlaverAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekBarSlaver.setProgress(seekBarSlaver.getProgress()+1);
			}
		});
		imgSlaverMinus = (ImageView) massagerView.findViewById(R.id.img_slave_minus);
		imgSlaverMinus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seekBarSlaver.setProgress(seekBarSlaver.getProgress()-1);
			}
		});
		
		//计时器
		textViewTimer = (TextView) massagerView.findViewById(R.id.tv_timer); 
		timer = MyCountDownTimer.getInstance(15*60*1000, 1000);
		imgStart = (ImageView) massagerView.findViewById(R.id.img_start);
		imgStop = (ImageView) massagerView.findViewById(R.id.img_stop);
		String timeStr = DataUtil.getMMSS(timer.getCurrentTime());
		textViewTimer.setText(timeStr);
			//开始计时
		imgStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				imgStart.setImageDrawable(getResources().getDrawable(R.drawable.ic_start_pressed));
				timer.start();
				mHandler.postDelayed(counterRunnable, 1000);
				
			}
		});
			//结束计时
		imgStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				timer.stop();
				mHandler.removeCallbacks(counterRunnable);
				textViewTimer.setText("15:00");
				imgStart.setImageDrawable(getResources().getDrawable(R.drawable.selector_start));
				imgStart.setClickable(true);
			}
		});
	
		//test
		SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar);
	}
	
	private void setListener(){
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
			Intent connectIntent = new Intent(MainActivity.this,ConnectActivity.class);
			startActivityForResult(connectIntent, REQUEST_CONNECT);
			break;
			
		default:
			break;
		}
	} 
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CONNECT:
			switch (resultCode) {
			case RESULT_OK:
				String name = data.getExtras().getString("device");
				if(name == null) {
					textViewConnectState.setVisibility(View.VISIBLE);
					textViewConnect.setVisibility(View.VISIBLE);
				} else {
					textViewConnectState.setVisibility(View.GONE);
					textViewConnect.setVisibility(View.GONE);
				}
				break;

			default:
				break;
			}
			break;

		default:
			break;
		}
	}
	public void validateBottom(int flag){
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
