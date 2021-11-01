package com.asiainfo.datacenter.parse;

/**
 * Created by 董建斌 on 2018/9/26.
 */
import com.asiainfo.datacenter.utils.BytesUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class CbOggMessage {
	/**
	 * 数据操作 类型
	 */
	public static enum Operate{
		Update((byte)'U'), Insert((byte)'I'), Delete((byte)'D'), Key((byte)'K');

		private byte tag;

		private Operate(byte tag) {
			this.tag = tag;
		}

		static Operate load(byte tag) {
			for(Operate ins : Operate.values()) {
				if(ins.tag == tag) {
					return ins;
				}
			}
			return null;
		}
	}

	/**
	 * 0 开始、1中间、2结尾、3WHOLE、4UNKNOWN等
	 */
	public static enum OperateState {
		Begin(0), Middle(1), Tail(2), Whole(3),Unknown(4);
		private int tag;

		private OperateState(int tag) {
			this.tag = tag;
		}

		static OperateState load(int tag) {
			for(OperateState ins : OperateState.values()) {
				if(ins.tag == tag) {
					return ins;
				}
			}
			return null;
		}
	}

	public static class Column {
		/**
		 * 序号
		 */
		public int index;
		/**
		 * 列名
		 */
		public byte[] name;
		/**
		 * 当前值
		 */
		public byte[] currentValue;
		/**
		 * 旧值
		 */
		public byte[] oldValue;
		/**
		 * 当前值是否存在
		 */
		private boolean currentValueExist = true;
		/**
		 * 旧值是否存在
		 */
		private boolean oldValueExist = true;

		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public byte[] getName() {
			return name;
		}
		public void setName(byte[] name) {
			this.name = name;
		}
		public byte[] getCurrentValue() {
			return currentValue;
		}
		public void setCurrentValue(byte[] currentValue) {
			this.currentValue = currentValue;
		}
		public byte[] getOldValue() {
			return oldValue;
		}
		public void setOldValue(byte[] oldValue) {
			this.oldValue = oldValue;
		}
		public boolean isCurrentValueExist() {
			return currentValueExist;
		}
		public void setCurrentValueExist(boolean currentValueExist) {
			this.currentValueExist = currentValueExist;
		}
		public boolean isOldValueExist() {
			return oldValueExist;
		}
		public void setOldValueExist(boolean oldValueExist) {
			this.oldValueExist = oldValueExist;
		}


	}

	public static final SimpleDateFormat YMD_HMS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 一级分隔符，分割消息，得到一级元素
	 */
	public final static byte FIRST_SEPERATOR = 0x01;

	/**
	 * 二级分隔符，分割一级元素，获取子元素; 用于非列项
	 */
	public final static byte SECOND_SEPERATOR = 0x02;

	/**
	 * 值分隔符，也是值类型标识，仅支持正数
	 */
	public final static byte[] VALUE_SEPERATORS = new byte[] { 0x02, 0x03, 0x04};

	/**
	 * 一级元素最小个数
	 */
	public final static short MIN_FIRST_PARTS = 4;

	/**
	 * 唯一标识
	 */
	private byte[] uuid;

	/**
	 * scn
	 */
	private long scn;

	/**
	 * OGG 事务ID
	 */
	private byte[] oggTransactionId;

	/**
	 * 本地事务ID
	 */
	private byte[] localTransactionId;

	/**
	 * 该操作在事务中的状态/位置
	 */
	private OperateState opState;

	/**
	 * 重建事务ID
	 */
	private byte[] reBiuldTransactionId;

	/**
	 * 前操作在事务中的位置
	 */
	private byte[] posInTransaction;

	/**
	 *
	 */
	private byte[] catalogName;

	/**
	 * 模式名
	 */
	private byte[] schemeName;

	/**
	 * 表名
	 */
	private byte[] tableName;

	/**
	 * 操作
	 */
	private Operate operate;

	/**
	 *  操作发生时间：微秒标识，注意Java的为毫秒
	 */
	private long timestampInMicroSeconds;
	/**
	 *  操作发生时间：微秒标识，注意Java的为毫秒
	 */
	private String time;

	/**
	 * 列
	 */
	private List<Column> columns;

	/**
	 * 供优化速度
	 */
	private final static byte[] _VALUE_SEPERATORS = new byte[1 + Byte.MAX_VALUE];

	static {
		for(byte b : VALUE_SEPERATORS) {
			_VALUE_SEPERATORS[b] = 1;
		}
	}

	/**
	 * 解析一条Ogg消息，注意一个Kafka消息可能包含多个Ogg消息
	 *
	 *
	 */
	public static CbOggMessage parse(byte[] data) throws Exception{
		List<byte[]> parts = BytesUtil.splitList(data, FIRST_SEPERATOR, 0);
		if(parts.size() < MIN_FIRST_PARTS) {
			throw new Exception(String.format("message parts only has %s, less %s", parts.size(), MIN_FIRST_PARTS));
		}
		long scn = 0;
		List<byte[]> partA0 = splitAndCheck(parts, 0, SECOND_SEPERATOR, 7);
		List<byte[]> partA1 = BytesUtil.splitList(parts.get(1), (byte)'.', 0);

		if(parts.get(2) == null || parts.get(2).length == 0) {
			throw new Exception("get operate failed: empty");
		}

		if(partA1.size() < 2 || partA1.size() > 3) {
			throw new Exception("parser [CATALOG_NAME.]SCHEMA_NAME.TABLE_NAME failed: " + new String(parts.get(1)));
		}

		// scn
		byte[] temp = partA0.get(1);
		for(int i = 0, L = temp != null ? temp.length : 0; i < L; i++) {
			scn = scn * 10 + temp[i] - 48;
		}

		// 操作时序
		OperateState opState = OperateState.load(partA0.get(4)[0] - 48);
		if(opState == null) {
			throw new Exception("get status of operation in transaction failed, unkown: " + new String(partA0.get(4)));
		}

		// 操作
		Operate op = Operate.load(parts.get(2)[0]);
		if (op == null) {
			throw new Exception("get operate failed, unkown: " + new String(parts.get(2)));
		}

		// 操作时间
		temp = parts.get(3);
		if(temp == null || temp.length < 19) { // yyyy-MM-dd HH:mm:ss.SSSSSS
			throw new Exception("get operate time failed, unkown:" + new String(parts.get(3)));
		}
		long timestamp = parseDate(temp);
		String time = new String(parts.get(3));

		// 主键列 及 操作列
		List<Column> columns = new LinkedList<Column>();
		for(int i = 4; i < parts.size(); i++) {
			columns.add(parseColumn(parts, i));
		}

		// 拼装ogg消息对象
		CbOggMessage aOgg = new CbOggMessage();
		// 第一项
		aOgg.setUuid(partA0.get(0));
		aOgg.setScn(scn);
		aOgg.setOggTransactionId(partA0.get(2));
		aOgg.setLocalTransactionId(partA0.get(3));
		aOgg.setOpState(opState);
		aOgg.setReBiuldTransactionId(partA0.get(5));
		aOgg.setPosInTransaction(partA0.get(6));

		//第二项
		if(partA1.size() == 2) {
			aOgg.setSchemeName(partA1.get(0));
			aOgg.setTableName(partA1.get(1));
		} else {
			aOgg.setCatalogName(partA1.get(0));
			aOgg.setSchemeName(partA1.get(1));
			aOgg.setTableName(partA1.get(2));
		}

		// 第三项
		aOgg.setOperate(op);

		// 第四项
		aOgg.setTimestampInMicroSeconds(timestamp);
		aOgg.setTime(time);

		// 各列值（主键列 操作列）
		aOgg.setColumns(columns);
		return aOgg;

	}

	/**
	 *
	 * @param data
	 * @param index
	 * @param sep
	 * @param size
	 * @return
	 * @throws Exception
	 */
	private static List<byte[]> splitAndCheck(List<byte[]> data, int index, byte sep, int size) throws Exception {
		List<byte[]> parts = BytesUtil.splitList(data.get(index), sep, size > 1 ? size -1 : 0);
		if(size > 0 && parts.size() < size) {
			throw new Exception(String.format("part %s only has %s, less %s", index, parts.size(), size));
		}
		return parts;
	}

	/**
	 *
	 * @param temp
	 * @return
	 */
	public static long parseDate(byte[] temp) {
		// yyyy-MM-dd HH:mm:ss.SSSSSS
		int year, month, day, hour, minute, second, microsecond = 0;
		year = (temp[0] - 48) * 1000 + (temp[1] - 48) * 100 + (temp[2] - 48) * 10 + temp[3] - 48;
		month = (temp[5] - 48) * 10 + temp[6] - 48;
		day = (temp[8] - 48) * 10 + temp[9] - 48;
		hour = (temp[11] - 48) * 10 + temp[12] - 48;
		minute = (temp[14] - 48) * 10 + temp[15] - 48;
		second = (temp[17] - 48) * 10 + temp[18] - 48;

		for (int i = 20; i < temp.length; i++) {
			microsecond = microsecond * 10 + temp[i] - 48;
		}

		for (int i = 26 - temp.length; i > 0; i--) { // 不足6位 补齐
			microsecond = microsecond * 10;
		}

		long ret = 0;
		if(year > 2000) { // for speed
			int days = day;
			for (int i = 2000; i < year; i++) {
				days += (i % 400 == 0 || i % 4 == 0 && i % 100 != 0) ? 366 : 365;
			}
			int[] DAYS_BEFORE_MONTH = new int[] { 0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365 };
			days += DAYS_BEFORE_MONTH[month];
			if (month < 3 || year % 4 != 0 || year % 100 == 0 && year % 400 != 0) {
				days--;
			}
			ret = 946656000000L + (days * 86400 + hour * 3600 + minute * 60 + second) * 1000L;
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month - 1, day, hour, minute, second);
			calendar.set(Calendar.MILLISECOND, 0);
			ret = calendar.getTimeInMillis();
		}
		return ret * 1000 + microsecond;
	}

	/**
	 *
	 * @param parts
	 * @param i
	 * @return
	 * @throws Exception
	 */
	private static Column parseColumn(List<byte[]> parts, int i) throws Exception{
		byte[] data = parts.get(i);
		byte[][] sepData = null;

		List<byte[][]> columnParts = BytesUtil.splitMulSeps(data, _VALUE_SEPERATORS);

		if(columnParts.size() < 3) {
			throw new Exception(String.format("column %s only has %s, less %s", i, columnParts.size(), 3));
		}

		Column aColumn = new Column();
		byte[] bIndex = columnParts.get(0)[1];
		int v = 0;
		for(int j = 0; j < bIndex.length; j++) {
			v = v * 10 + bIndex[j] - 48;
		}
		aColumn.setIndex(v);

		sepData = columnParts.get(1);
		if(sepData[0][0] == 0x02) {
			aColumn.setCurrentValue(sepData[1]);
		} else if(sepData[0][0] == 0x03) {
			aColumn.setCurrentValue(null);
		} else if(sepData[0][0] == 0x04) {
			aColumn.setCurrentValueExist(false);
		}

		sepData = columnParts.get(2);
		if (sepData[0][0] == 0x02) {
			aColumn.setOldValue(sepData[1]);
		} else if (sepData[0][0] == 0x03) {
			aColumn.setOldValue(null);
		} else if (sepData[0][0] == 0x04) {
			aColumn.setOldValueExist(false);
		}

		return aColumn;
	}

	public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}

	public long getScn() {
		return scn;
	}

	public void setScn(long scn) {
		this.scn = scn;
	}

	public byte[] getOggTransactionId() {
		return oggTransactionId;
	}

	public void setOggTransactionId(byte[] oggTransactionId) {
		this.oggTransactionId = oggTransactionId;
	}

	public byte[] getLocalTransactionId() {
		return localTransactionId;
	}

	public void setLocalTransactionId(byte[] localTransactionId) {
		this.localTransactionId = localTransactionId;
	}

	public OperateState getOpState() {
		return opState;
	}

	public void setOpState(OperateState opState) {
		this.opState = opState;
	}

	public byte[] getReBiuldTransactionId() { return reBiuldTransactionId; }

	public void setReBiuldTransactionId(byte[] reBiuldTransactionId) {
		this.reBiuldTransactionId = reBiuldTransactionId;
	}

	public byte[] getPosInTransaction() { return posInTransaction; }

	public void setPosInTransaction(byte[] posInTransaction) {
		this.posInTransaction = posInTransaction;
	}

	public byte[] getCatalogName() {
		return catalogName;
	}

	public void setCatalogName(byte[] catalogName) {
		this.catalogName = catalogName;
	}

	public byte[] getSchemeName() {
		return schemeName;
	}

	public void setSchemeName(byte[] schemeName) {
		this.schemeName = schemeName;
	}

	public byte[] getTableName() {
		return tableName;
	}

	public void setTableName(byte[] tableName) {
		this.tableName = tableName;
	}

	public Operate getOperate() {
		return operate;
	}

	public void setOperate(Operate operate) {
		this.operate = operate;
	}

	public long getTimestampInMicroSeconds() {
		return timestampInMicroSeconds;
	}

	public String getTime() {
		return time;
	}

	public void setTimestampInMicroSeconds(long timestampInMicroSeconds) {
		this.timestampInMicroSeconds = timestampInMicroSeconds;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public static void main(String[] args) {
		//byte[] data = "uuid:12345:remoteID:localId:2,ogg.table1,U,2015-01-09 11:12:31.3256,0:123:456,2:abc:def".getBytes();

		//case 1)	delete操作
		/**
		 * 	ACT_CUE1:29605250:00000001910094667555:0:3为第一个元素，ACT_CUE1是进程标识，29605250是SCN标识号，00000001910094667555为事务Id，0为本地事务Id，3为事务标识
		 BIGDATA.TF_B_PAYLOG为第二个元素’ Catalog名.Schema名.table名’
		 D为第三个元素’操作类型’，表示是删除操作
		 2016-01-12 07:27:00.530097为第四个元素’时间戳’
		 从第五个元素0::1042开始的每个元素是列变更信息，因为是删除操作，所以’当前值’均为空，且当前值前面的二级分隔符为0x04，原始值前面的二级分隔符为0x02或0x03（正式消息中）
		 */
		byte[] data = ("ACT_CUE1:29605250:00000001910094667555:0:3,BIGDATA.TF_B_PAYLOG,"
				+ "D,2016-01-12 07:27:00.530097,0::1042,1::10,2::23,3::34,4::234,5::,6::53,"
				+ "7::2,8::345,9::4243,10::100004,11::34,12::42,13::43,14::,15::2015-10-21:17:28:56,"
				+ "16::56,17::56,18::534,19::567,20::54,21::,22::86,23::35,24::0,25::1,26::342,27::54,"
				+ "28::46,29::23,30::3,31::65,32::23,33::673,34::56,35::2015-10-21:17:28:56,36::58,37::84,"
				+ "38::83,39::45,40::11,41::23,42::235").getBytes();

		//case 2)	insert操作
		/**
		 *  ACT_CUE1:29605626:00000001910094668226:0:3为第一个元素，ACT_CUE1是进程标识, 29605626是scn标识号，00000001910094668226是事务Id，0是本地事务Id，3为事务标识；
		 BIGDATA.TF_B_PAYLOG是第二元素；
		 I为第三个元素’操作类型’，表示插入操作
		 2016-01-12 07:41:19.509712为第四个元素；
		 从第五个元素0:1044:开始的每个元素是列变更信息，因为是插入操作，所以原始值均为空，且原始值前面的二级分隔符为0x04，当前值前面的分隔符为0x02或0x03（正式消息中）
		 */
		/*byte[] data = ("ACT_CUE1:29605626:00000001910094668226:0:3,BIGDATA.TF_B_PAYLOG,"
				+ "I,2016-01-12 07:41:19.509712,0:1044:,1:10:,2:23:,3:34:,4:234:,5::,6:53:,"
				+ "7:2:,8:345:,9:4243:,10:100004:,11:34:,12:42:,13:43:,14::,15:2015-10-21:17:29:31:,16:56:,"
				+ "17:56:,18:534:,19:567:,20:54:,21::,22:86:,23:35:,24:0:,25:1:,26:342:,27:54:,"
				+ "28:46:,29:23:,30:3:,31:65:,32:23:,33:673:,34:56:,35:2015-10-21:17:29:31:,36:58:,37:84:,"
				+ "38:83:,39:45:,40:11:,41:23:,42:235:").getBytes();*/
		//case 3)	update(普通更新)
		/**
		 *  ACT_CUE1:29605827:00000001910094669096:0:2为第一个元素，ACT_CUE1为进程标识，29605827为scn标识号，00000001910094669096为事务Id，0为本地事务Id，2为事务标识；
		 BIGDATA.TF_B_PAYLOG为第二个元素；
		 U为第三个元素’操作类型’,表示更新的列均为普通列
		 2016-01-12 07:50:43.496418为第四个元素；
		 从第五个元素0:1044:1044开始的元素为列变更信息，由于此处更新的是第9和第30列（非主键列和唯一索引列），所以消息中只包含主键列（第一列和第二列）和变更列（第9列和第30列）的信息，且原始值和当前值前面的二级分隔符均为0x02（正式消息中）
		 */
		/*byte[] data = ("ACT_CUE1:29605827:00000001910094669096:0:2,BIGDATA.TF_B_PAYLOG,"
				+ "U,2016-01-12 07:50:43.496418,0:1044:1044,1:10:10,8:0:345,29:hello:23").getBytes();*/

		//case 4)	pkupdate(更新主键或唯一索引列)
		/**
		 *  ACT_CUE1:29606298:00000001910094670308:0:2为第一个元素，ACT_CUE1为进程标识，29606298为scn标识号，00000001910094670308为事务Id，0为本地事务Id，2为事务标识；
		 BIGDATA.TF_B_PAYLOG为第二个元素；
		 K为第三个元素’操作类型’，表示更新的列中包含主键或唯一索引列；
		 2016-01-12 08:03:29.476928,为第四个元素；
		 第五个元素0:1044:1044开始的是列变更信息，由于此处更新的是partition_id(主键列)和remark列（非主键列），所以消息中仅包含主键列的信息和发生变更列的信息。且原始值和当前值前面的二级分隔符均为0x02（正式消息中）
		 */
//		byte[] data = ("ACT_CUE1:29606298:00000001910094670308:0:2,BIGDATA.TF_B_PAYLOG,K,2016-01-12 08:03:29.476928,0:1044:1044,1:13:12,29:world:hell").getBytes();

		for(int i = 0; i < data.length; i++) {
			if(data[i] == ',') {
				data[i] = 0x01;
			} else if(data[i] == ':') {
				data[i] = 0x02;
			}
		}

		try {
			CbOggMessage msg = CbOggMessage.parse(data);
			System.out.println("uuid: " + new String(msg.getUuid()));
			System.out.println("scn: " + msg.getScn());
			System.out.println("oggTid: " + new String(msg.getOggTransactionId()));
			System.out.println("localTid: " + new String(msg.getLocalTransactionId()));
			System.out.println("scheme: " + new String(msg.getSchemeName()));
			System.out.println("tablename: " + new String(msg.getTableName()));
			System.out.println("Operate: " + msg.getOperate().name());
			System.out.println("Timestamp: " + msg.getTimestampInMicroSeconds());

			List<Column> msgColumns = msg.getColumns();
			System.out.println("Columns size: " + msgColumns.size());
			for (int i = msg.getColumns().size() - 1; i >= 0; i--) {
				Column c = msgColumns.get(i);
				if (c.isCurrentValueExist()) {
					if (c.getCurrentValue() != null) {
						System.out.println("Column " + i + ", current value: " + new String(c.getCurrentValue()));
					} else {
						System.out.println("Column " + i + ", current value [Database null]");
					}
				} else {
					System.out.println("Column " + i + ", current value not exists");
				}

				if (c.isOldValueExist()) {
					if (c.getOldValue() != null) {
						System.out.println("Column " + i + ", old value: " + new String(c.getOldValue()));
					} else {
						System.out.println("Column " + i + ", old value [Database null]");
					}
				} else {
					System.out.println("Column " + i + ", old value not exists");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
