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
	 * 根据传入的2个字节2位16进制字符比如FFFF, 计算返回int类型的绝对值
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
	
 //二进制转十六进制
	public static String BinaryToHex(String s){
		return Long.toHexString(Long.parseLong(s,2));
		}
		//十六进制转二进制
	public static String HexToBinary(String s){
		return Long.toBinaryString(Long.parseLong(s, 16));
		}
	
	/**
	 * int n1 = 14;
//十进制转成十六进制：
Integer.toHexString(n1);
//十进制转成八进制
Integer.toOctalString(n1);
//十进制转成二进制
Integer.toBinaryString(12);

//十六进制转成十进制
Integer.valueOf("FFFF",16).toString();
//十六进制转成二进制
Integer.toBinaryString(Integer.valueOf("FFFF",16));
//十六进制转成八进制
Integer.toOctalString(Integer.valueOf("FFFF",16));

//八进制转成十进制
Integer.valueOf("576",8).toString();
//八进制转成二进制
Integer.toBinaryString(Integer.valueOf("23",8));
//八进制转成十六进制
Integer.toHexString(Integer.valueOf("23",8));


//二进制转十进制
Integer.valueOf("0101",2).toString();
//二进制转八进制
Integer.toOctalString(Integer.parseInt("0101", 2));
//二进制转十六进制
Integer.toHexString(Integer.parseInt("0101", 2));
	 */
}
