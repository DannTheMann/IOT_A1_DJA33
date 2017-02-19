package kent.dja33.iot.a1.util.message;

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
public abstract class Message {
	
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
	public Message(String name, String timeStamp, String payload, long id) {
		this.name = name;
		this.timeStamp = timeStamp;
		if(this.payload != null){
			this.payload = payload;
		}else{
			this.payload = "INVALID_PAYLOAD";
		}
		this.id = id;
	}
	
	/* Designed to overriden if messages need updating */
	public void update(){}

	/**
	 * Get the name of this message type 
	 * @return message type
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Get the payload of this message
	 * @return payload
	 */
	public final String getPayload() {
		return payload;
	}

	/**
	 * Get the timestamp associated with the time
	 * the message was received
	 * @return timestamp
	 */
	public final String getTimeReceived() {
		return timeStamp;
	}

	/**
	 * Get the unique ID for this message
	 * @return id
	 */
	public final long getID() {
		return id;
	}

}
