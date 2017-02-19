package kent.dja33.iot.a1.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Wrapper class to handle incoming serial data
 * 
 * Formats the data coming in and separates it based on fields 
 * of relevance, such as:
 * 
 *  -> D = Data (Temperature samples) 
 *  -> S = Setting (Change a setting)
 *  -> A = Acknowledgement (Acknowledge each other)
 *  -> * = Error (None of the above)
 *  
 * These messages are then slightly tweaked based on their role.
 * Data messages are checked to see whether their values are 
 * potential errors (Going from 30 to 0 is a good sign of this)
 * and any unrecognised messages are immediately labelled as errors.
 * 
 * @author Dante
 *
 */
public class Message {

	
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
	
	/* General information about the message */
	private String name;
	private String payload;
	private long id;
	private final String timeStamp;

	/**
	 * Create a new message from a raw stream of bytes
	 * interpreted as a String 
	 * @param name The String form of the bytes received from Serial
	 */
	public Message(String name) {

		/* Immediately work out when the Message was RECEIVED */
		this.timeStamp = getTime();
		
		/* Remove identifying start and stop flags of message */
		name = name.replaceAll("#", "");
		
		/* If the message is empty, we have an error */
		if (name.length() == 0 || name.equals("")) {
			this.name = ERR;
			return;
		}

		/* Identify the type of Message */
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
			/* Anything else is unidentifiable and is an error */
			this.name = ERR;
			return;

		}
		/* Extract the payload from the message */
		this.payload = name.substring(1, name.length());
		
		/* Set and increment the global message ID value */
		this.id = messageID++;

		/* If we have data, then make sure it is not a potential error */
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
				/* This message is considered an error and not reliable as data */
				this.name = ERR;
			}

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

	/**
	 * Get the name of this message type 
	 * @return message type
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the payload of this message
	 * @return payload
	 */
	public String getPayload() {
		return payload;
	}

	/**
	 * Get the timestamp associated with the time
	 * the message was received
	 * @return timestamp
	 */
	public String getTimeReceived() {
		return timeStamp;
	}

	/**
	 * Get the unique ID for this message
	 * @return id
	 */
	public long getID() {
		return id;
	}
}
