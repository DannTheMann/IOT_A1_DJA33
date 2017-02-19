package kent.dja33.iot.a1.util.message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageHandler {
	
	/* Different formats to expect */
	public static final String DATA = "Data";
	public static final String SETTING = "Setting";
	public static final String ACK = "Acknowledgement";
	public static final String ERR = "Error";

	/* The maximum difference allowed in tolerating 
	 * potentially damaged data messages */
	private static final float MAX_DIFF = 10.0f;
	private static float lastPayload = Float.MAX_VALUE;
	
	/* Message ID, incremented on each assigning */
	private static long messageID = 1;

	private static MessageHandler handler;
	
	public static final MessageHandler getHandler(){
		return handler;
	}
	
	private MessageHandler(){}
	
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
	 * Calculate the current Time in the format
	 * HH:mm:ss.S = 17:23:45.9
	 * @return The time 
	 */
	private String getTime() {
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.");
		SimpleDateFormat ms = new SimpleDateFormat("S");
		Date date = new Date();
		ms = new SimpleDateFormat("S");

		return time.format(date)
				+ (ms.format(date).length() > 1 ? ms.format(date).substring(0, 2) : ms.format(date));
	}	
	
	private class ErrorMessage extends Message{
		
		public ErrorMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
		}
		
	}
	
	private class AcknowledgementMessage extends Message{
		
		public AcknowledgementMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
		}
		
	}
	
	private class SettingMessage extends Message{
		
		public SettingMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
		}

		@Override
		/**
		 * Update from Celsius to Fahrenheit or back
		 */
		public void update() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class DataMessage extends Message{
		
		public DataMessage(String name, String timeStamp, String payload, long id) {
			super(name, timeStamp, payload, id);
			
			/* If we have data, then make sure it is not a potential error */
			if (name == DATA) {
	
				try {
	
					float curVal = Float.parseFloat(payload);
	
					if ((curVal > lastPayload + MAX_DIFF || curVal < lastPayload - MAX_DIFF)
							&& lastPayload != Float.MAX_VALUE) {
						name = ERR;
						return;
					}
	
					lastPayload = Float.parseFloat(payload);
				} catch (Exception e) {
					/* This message is considered an error and not reliable as data */
					name = ERR;
				}
	
			}
		}

		@Override
		public void update() {
			// TODO Auto-generated method stub
			
		}
		
	}

}
