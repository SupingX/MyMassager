package com.mycj.mymassager.entity;

public class MachineStatus {
	/**主机模式**/
	private String mainModel; 	//主机模式
	/**主机强度**/
	private String mainPower;	//主机强度
	/**主机频率**/
	private String mainFreq;	//主机频率
	/**从机模式**/
	private String slaveModel;  //从机模式
	/**从机强度**/
	private String slavePower;	//从机强度
	/**从机频率**/
	private String slaveFreq;	//从机频率
	/**开始停止**/
	private String startStatus; //开始停止
	/**电机开关**/
	private String motorStatus; //电机开关
	
	
	public MachineStatus() {
		super();
	}
	

	public MachineStatus(String mainModel, String mainPower, String mainFreq,
			String slaveModel, String slavePower, String slaveFreq,
			String startStatus, String motorStatus) {
		super();
		this.mainModel = mainModel;
		this.mainPower = mainPower;
		this.mainFreq = mainFreq;
		this.slaveModel = slaveModel;
		this.slavePower = slavePower;
		this.slaveFreq = slaveFreq;
		this.startStatus = startStatus;
		this.motorStatus = motorStatus;
	}


	@Override
	public String toString() {
		return mainModel + mainPower + mainFreq + slaveModel + slavePower + slaveFreq + startStatus + motorStatus ;  
	}
	
	public String getMainModel() {
		return mainModel;
	}
	public void setMainModel(String mainModel) {
		this.mainModel = mainModel;
	}
	public String getMainPower() {
		return mainPower;
	}
	public void setMainPower(String mainPower) {
		this.mainPower = mainPower;
	}
	public String getMainFreq() {
		return mainFreq;
	}
	public void setMainFreq(String mainFreq) {
		this.mainFreq = mainFreq;
	}
	public String getSlaveModel() {
		return slaveModel;
	}
	public void setSlaveModel(String slaveModel) {
		this.slaveModel = slaveModel;
	}
	public String getSlavePower() {
		return slavePower;
	}
	public void setSlavePower(String slavePower) {
		this.slavePower = slavePower;
	}
	public String getSlaveFreq() {
		return slaveFreq;
	}
	public void setSlaveFreq(String slaveFreq) {
		this.slaveFreq = slaveFreq;
	}
	public String getStartStatus() {
		return startStatus;
	}
	public void setStartStatus(String startStatus) {
		this.startStatus = startStatus;
	}
	public String getMotorStatus() {
		return motorStatus;
	}
	public void setMotorStatus(String motorStatus) {
		this.motorStatus = motorStatus;
	}
	
}
