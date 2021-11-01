package com.asiainfo.datacenter.utils;

import java.util.LinkedList;
import java.util.List;
/**
 * Created by 董建斌 on 2018/9/26.
 */
public class BytesUtil {
	public final static byte LF = 10; // \n
	public final static byte DOUBLE_QUOTE = 34; // "
	public final static byte COMMA = 44; // ,
	public final static byte DASH = 45; // -
	public final static byte COLON = 58; // :
	public final static byte LEFT_BRACKET = 91; // [
	public final static byte RIGHT_BRACKET = 93; // ]
	public final static byte UNDERSCORE = 95; // _
	public final static byte LEFT_BRACE = 123; // {
	public final static byte RIGHT_BRACE = 125; // }
	public final static char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * 切分数组, 注意如果分割符出现的次数少于切分的次数, 返回的数据个数为实际可以分割的个数
	 * @param data 待分割数数组
	 * @param seperator 分割字符
	 * @param splitTimes 切分的次数 0表示不做次数限制
	 * @return
	 */
	public static List<byte[]> splitList(byte[] data, byte seperator, int splitTimes) {
		List<byte[]> container = new LinkedList<byte[]>();
		if (data == null) {
			return container;
		}
		int count = 0, start = 0, index = 0;
		byte[] empty = new byte[0];
		for (byte i : data) {
			if (i == seperator) {
				if (index == start) {
					container.add(empty);
				} else {
					byte[] one = new byte[index - start];
					System.arraycopy(data, start, one, 0, one.length);
					container.add(one);
				}
				start = index + 1;
				count++;
			}
			if (splitTimes > 0 && count >= splitTimes) {
				break;
			}
			index++;
		}
		if (start < data.length) {
			byte[] one = new byte[data.length - start];
			System.arraycopy(data, start, one, 0, one.length);
			container.add(one);
		} else if (splitTimes <= 0 || count <= splitTimes) {
			container.add(empty);
		}
		return container;
	}

	/**
	 * 根据分隔符分割字节串, 分隔符有多个，用索引标示
	 * @param data
	 * @param indexSeps 用索引标示的分隔符, 值为1则该索引是分隔符
	 * @return
	 */
	public static List<byte[][]> splitMulSeps(byte[] data, byte[] indexSeps) {
		LinkedList<byte[][]> ret = new LinkedList<byte[][]>();
		if (data == null || data.length == 0) {
			return ret;
		}

		int index = 0, start = 0;
		byte tag = 0;

		for(byte cur : data) {
			if(cur >= 0 && indexSeps[cur] == 1) {
				byte[] content = new byte[index - start];
				System.arraycopy(data, start, content, 0, content.length);

				byte[][] item = new byte[2][];
				item[0] = new byte[]{tag};
				item[1] = content;

				ret.add(item);
				start = index + 1;
				tag = cur;
			}
			index++;
		}

		byte[] content = new byte[index - start];
		System.arraycopy(data, start, content, 0, content.length);

		byte[][] item = new byte[2][];
		item[0] = new byte[]{tag};
		item[1] = content;

		ret.add(item);
		return ret;
	}

}
