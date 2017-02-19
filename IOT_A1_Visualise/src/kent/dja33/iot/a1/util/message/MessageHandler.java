package kent.dja33.iot.a1.util.message;

import java.text.SimpleDateFormat;
import java.util.Date;

import kent.dja33.iot.a1.Main;
import kent.dja33.iot.a1.MeasurementType;

public class MessageHandler {

	/* Different formats to expect */
	public static final String DATA = "Data";
	public static final String SETTING = "Setting";
	public static final String ACK = "Acknowledgement";
	public static final String ERR = "Error";

	/*
	 * The maximum difference allowed in tolerating potentially damaged data
	 * messages
	 */
	private static final float MAX_DIFF = 10.0f;
	private static float lastPayload = Float.MAX_VALUE;

	/* Message ID, incremented on each assigning */
	private static long messageID = 1;

	/* Singleton */
	private static MessageHandler handler = new MessageHandler();

	public static final MessageHandler getHandler() {
		return handler;
	}

	/* Force singleton pattern */
	private MessageHandler() {}

	public Message createMessage(String name) {

		/* Immediately work out when the Message was RECEIVED */
		String timeStamp = getTime();

		/* Remove identifying start and stop flags of message */
		name = name.replaceAll("#", "");

		/* Set and increment the global message ID value */
		long id = messageID++;

		/* If the message is empty, we have an error */
		if (name.length() == 0 || name.equals("")) {
			name = ERR;
			return new ErrorMessage(ERR, timeStamp, null, id);
		}

		/* Extract the payload from the message */
		String payload = name.substring(1, name.length());
		/* Identify the type of Message */
		switch (name.charAt(0)) {

			case 'D':
				return new DataMessage(DATA, timeStamp, payload, id);
			case 'S':
				return new SettingMessage(SETTING, timeStamp, payload, id);
			case 'A':
				return new AcknowledgementMessage(ACK, timeStamp, payload, id);
			default:
				/* Anything else is unidentifiable and is an error */
				return new ErrorMessage(ERR, timeStamp, payload, id);

		}

	}

	/**
	 * Calculate the current Time in the format HH:mm:ss.S = 17:23:45.9
	 * 
	 * @return The time
	 */
	private String getTime() {
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.");
		SimpleDateFormat ms = new SimpleDateFormat("S");
		Date date = new Date();
		ms = new SimpleDateFormat("S");

		return time.format(date) + (ms.format(date).length() > 1 ? ms.format(date).substring(0, 2) : ms.format(date));
	}

	/** >> Series of sub classes to handle potential different Messages << **/

	private class ErrorMessage extends Message {

		public ErrorMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
		}

	}

	private class AcknowledgementMessage extends Message {

		public AcknowledgementMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
		}

	}

	private class SettingMessage extends Message {

		public SettingMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
		}

	}

	private class DataMessage extends Message {

		private float temperature;
		private float accelX;
		private float accelY;
		private float accelZ;

		public DataMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);

			String[] payloadSplit = payload.split(":");

			/* If we have data, then make sure it is not a potential error */
			if (name == DATA) {

				try {

					temperature = Float.parseFloat(payloadSplit[0]);
					accelX = Float.parseFloat(payloadSplit[1]);
					accelY = Float.parseFloat(payloadSplit[2]);
					accelZ = Float.parseFloat(payloadSplit[3]);

					if ((temperature > lastPayload + MAX_DIFF || temperature < lastPayload - MAX_DIFF)
							&& lastPayload != Float.MAX_VALUE) {
						name = ERR;
						return;
					}

					lastPayload = Float.parseFloat(payloadSplit[0]);
				} catch (Exception e) {
					System.err.println("Message useless, discarding...");
					/*
					 * This message is considered an error and not reliable as
					 * data
					 */
					name = ERR;
				}

			}
		}

		@Override
		public String getPayload() {

			/* TemperatureHandler has not been created yet */
			if (Main.display.getMeasurementType() == null) {
				return "NOT_READY";
			}

			/* Conversion if needed */
			if (Main.display.getMeasurementType().equals(MeasurementType.FAHRENHEIT)) {

				float val = temperature;
				val = MeasurementType.convert(MeasurementType.FAHRENHEIT, val);

				return val + ":" + accelX + ":" + accelY + ":" + accelZ;

			} else if (Main.display.getMeasurementType().equals(MeasurementType.CELSIUS)) {
				return temperature + ":" + accelX + ":" + accelY + ":" + accelZ;
			}

			return "INVALID CONVERSION";
		}

	}

}
