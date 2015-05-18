package com.mycj.mymassager.util;

import java.text.DecimalFormat;

/**
 * 数据转换工具类
 * 
 * @author Administrator
 * 
 */
public class DataUtil {

	public static DecimalFormat df = new DecimalFormat("0.00");
	public static DecimalFormat df2 = new DecimalFormat("0.0");

	/**
	 * byte[]转变为16进制String字符, 每个字节2位, 不足补0
	 */
	public static String getStringByBytes(byte[] bytes) {
		String result = null;
		String hex = null;
		if (bytes != null && bytes.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(bytes.length);
			for (byte byteChar : bytes) {
				hex = Integer.toHexString(byteChar & 0xFF);
				if (hex.length() == 1) {
					hex = '0' + hex;
				}
				stringBuilder.append(hex.toUpperCase());
			}
			result = stringBuilder.toString();
		}
		return result;
	}

	/**
	 * 把16进制String字符转变为byte[]
	 */
	public static byte[] getBytesByString(String data) {
		byte[] bytes = null;
		if (data != null) {
			data = data.toUpperCase();
			int length = data.length() / 2;
			char[] dataChars = data.toCharArray();
			bytes = new byte[length];
			for (int i = 0; i < length; i++) {
				int pos = i * 2;
				bytes[i] = (byte) (charToByte(dataChars[pos]) << 4 | charToByte(dataChars[pos + 1]));
			}
		}
		return bytes;
	}

	/**
	 * 取得在16进制字符串中各char所代表的16进制数
	 */
	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	/**
	 * 根据传入的毫秒数，转换成MM:SS时间串形式返回
	 * 
	 * @param timeMillis
	 * @return
	 */
	public static String getMMSS(long timeMillis) {
		StringBuffer sb = new StringBuffer();
		long mm = (timeMillis % (1000 * 60 * 60)) / 1000 / 60;
		long ss = ((timeMillis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

		if (mm < 0) {
			sb.append("00:");
		} else if (mm < 10) {
			sb.append("0" + mm + ":");
		} else {
			sb.append(mm + ":");
		}
		if (ss < 0) {
			sb.append("00");
		} else if (ss < 10) {
			sb.append("0" + ss);
		} else {
			sb.append(ss);
		}
		return sb.toString();
	}

	/**
	 * 根据传入的耗秒数, 转换成为HH:MM:SS的字符串返回
	 */
	public static String getHHMMSS(long time) {
		String hhmmss = "00:00:00";
		StringBuffer bf = new StringBuffer();
		if (time == 0) {
			return hhmmss;
		}
		long hh = time / 1000 / 60 / 60;
		long mm = (time % (1000 * 60 * 60)) / 1000 / 60;
		long ss = ((time % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

		if (hh < 0) {
			bf.append("00:");
		} else if (hh < 10) {
			bf.append("0" + hh + ":");
		} else {
			bf.append(hh + ":");
		}
		if (mm < 0) {
			bf.append("00:");
		} else if (mm < 10) {
			bf.append("0" + mm + ":");
		} else {
			bf.append(mm + ":");
		}
		if (ss < 0) {
			bf.append("00");
		} else if (ss < 10) {
			bf.append("0" + ss);
		} else {
			bf.append(ss);
		}
		hhmmss = bf.toString();
		System.out.println(hhmmss);
		return hhmmss;
	}

	/**
	 * 根据传入的2个字节4位16进制字符比如FFFF, 计算返回int类型的绝对值
	 */
	public static int hexStringX2bytesToInt(String hexString) {
		return binaryString2int(hexString2binaryString(hexString));
	}

	/**
	 * 16进制转换为2进制
	 */
	public static String hexString2binaryString(String hexString) {
		if (hexString == null || hexString.length() % 2 != 0) {
			return null;
		}
		String bString = "", tmp;
		for (int i = 0; i < hexString.length(); i++) {
			tmp = "0000"
					+ Integer.toBinaryString(Integer.parseInt(
							hexString.substring(i, i + 1), 16));
			bString += tmp.substring(tmp.length() - 4);
		}
		return bString;
	}

	/**
	 * 二进制转为10进制 返回int
	 */
	public static int binaryString2int(String binarysString) {
		if (binarysString == null || binarysString.length() % 8 != 0) {
			return 0;
		}
		int result = Integer.valueOf(binarysString, 2);
		if ("1".equals(binarysString.substring(0, 1))) {
			System.out.println("这是个负数");
			char[] values = binarysString.toCharArray();
			for (int i = 0; i < values.length; i++) {
				if (values[i] == '1') {
					values[i] = '0';
				} else {
					values[i] = '1';
				}
			}
			binarysString = String.valueOf(values);
			result = Integer.valueOf(binarysString, 2) + 1;
		}

		return result;
	}

	/**
	 * 
	 * 二进制转为16进制
	 */
	public static String binaryString2hexString(String bString) {
		if (bString == null || bString.equals("") || bString.length() % 8 != 0) {
			return null;
		}
		StringBuffer tmp = new StringBuffer();
		int iTmp = 0;
		for (int i = 0; i < bString.length(); i += 4) {
			iTmp = 0;
			for (int j = 0; j < 4; j++) {
				iTmp += Integer.parseInt(bString.substring(i + j, i + j + 1)) << (4 - j - 1);
			}
			tmp.append(Integer.toHexString(iTmp));
		}
		return tmp.toString();
	}

	/**
	 * 手机-->主机 强度数据协议
	 * 
	 * @param progress
	 * @return
	 */
	public static final String getStrengthHexToMaster(int strength) {
		StringBuffer sb = new StringBuffer();
		sb.append("F2");
		String hex = Integer.toHexString(strength);
		if (hex.length() == 1) {
			hex = "0" + hex;
		}
		sb.append(hex);
		return sb.toString();
	}

	/**
	 * 手机-->主机 频率数据协议
	 * 
	 * @param progress
	 * @return
	 */
	public static final String getFrequencyHexToMaster(int frequency) {
		StringBuffer sb = new StringBuffer();
		sb.append("F3");
		String hex = Integer.toHexString(frequency);
		if (hex.length() == 1) {
			hex = "0" + hex;
		}
		sb.append(hex);
		return sb.toString();
	}

	/**
	 * 手机-->从机 强度数据协议
	 * 
	 * @param progress
	 * @return
	 */
	public static final String getStrengthHexToSlave(int strength) {
		StringBuffer sb = new StringBuffer();
		sb.append("F5");
		String hex = Integer.toHexString(strength);
		if (hex.length() == 1) {
			hex = "0" + hex;
		}
		sb.append(hex);
		return sb.toString();
	}

	/**
	 * 手机-->从机 频率数据协议
	 * 
	 * @param progress
	 * @return
	 */
	public static final String getFrequencyHexToSlave(int frequency) {
		StringBuffer sb = new StringBuffer();
		sb.append("F6");
		String hex = Integer.toHexString(frequency);
		if (hex.length() == 1) {
			hex = "0" + hex;
		}
		sb.append(hex);
		return sb.toString();
	}
}
