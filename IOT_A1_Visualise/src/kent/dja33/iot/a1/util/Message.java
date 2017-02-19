package kent.dja33.iot.a1.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {

	public static final String DATA = "Data";
	public static final String SETTING = "Setting";
	public static final String ACK = "Acknowledgement";
	public static final String ERR = "Error";

	private static final float MAX_DIFF = 10.0f;
	private static float lastPayload = Float.MAX_VALUE;
	private static long messageID;
	private String name;
	private String payload;
	private long id;
	private String timeStamp;

	public Message(String name) {

		name = name.replaceAll("#", "");

		if (name.length() == 0 || name.equals("")) {
			this.name = ERR;
			return;
		}

		switch (name.charAt(0)) {

		case 'D':
			this.name = DATA;
			break;
		case 'S':
			this.name = SETTING;
			break;
		case 'A':
			this.name = ACK;
			break;
		default:
			this.name = ERR;
			return;

		}
		this.payload = name.substring(1, name.length());

		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.");
		SimpleDateFormat ms = new SimpleDateFormat("S");
		Date date = new Date();
		ms = new SimpleDateFormat("S");

		this.timeStamp = time.format(date)
				+ (ms.format(date).length() > 1 ? ms.format(date).substring(0, 2) : ms.format(date));
		this.id = messageID++;

		if (this.name == DATA) {

			try {

				float curVal = Float.parseFloat(this.payload);

				if ((curVal > lastPayload + MAX_DIFF || curVal < lastPayload - MAX_DIFF)
						&& lastPayload != Float.MAX_VALUE) {
					this.name = ERR;
					return;
				}

				lastPayload = Float.parseFloat(this.payload);
			} catch (Exception e) {
				this.name = ERR;
			}

		}

	}

	public String getName() {
		return name;
	}

	public String getPayload() {
		return payload;
	}

	public String getTimeReceived() {
		return timeStamp;
	}

	public long getID() {
		return id;
	}
}
