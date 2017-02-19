package kent.dja33.iot.a1.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import kent.dja33.iot.a1.SensorDisplay;
import kent.dja33.iot.a1.util.message.Message;
import kent.dja33.iot.a1.util.message.MessageHandler;

/**
 * 
 * Class to handle reading of Serial from the MBED, originally utilised threads
 * to continuously read in all data plausible but not instead uses the
 * SerialReceiveEvent.
 * 
 * Relies on Singleton pattern and will queue and create messages as it reads in
 * data packets, tries to allow for error handling of invalid messages.
 * 
 * @author Dante
 *
 */
public class SerialReader {

	/* Singleton reference */
	public static final SerialReader in = new SerialReader();
	private static final int RETRY_CONNECTION_ATTEMPTS = 3;
	private String portName;
	private SerialPort port;

	/* Reference to private static class for handling events */
	private SerialReaderEventHandler portReader;

	/**
	 * Default creation of SerialReader has no active serial port
	 */
	private SerialReader() {
		portName = SensorDisplay.NO_SERIAL_PORT;
	}

	/**
	 * Return all active serial ports
	 * 
	 * @return String[] of ports
	 */
	public String[] getActiveSerialPorts() {
		return SerialPortList.getPortNames();
	}

	/**
	 * Attempts to open a port to the port name given, will use acknowledgements
	 * to determine whether the device on the other end is indeed an MBED
	 * device. Otherwise if no response is heard will assume otherwise and
	 * return false. If a connection is already open will refuse to attempt to
	 * open another and return false.
	 * 
	 * Will use a maximum of RETRY_CONNECTION_ATTEMPTS to attempt connection
	 * with the device on the other end and expects a response from the device
	 * when it requests one otherwise assumes it is not the correct port.
	 * 
	 * @param portName
	 *            The port to connect to
	 * @return Whether we connected or not
	 */
	public boolean openPort(String portName) {

		if (port != null && port.isOpened()) {
			Out.out.loglnErr(
					"Cannot open SerialPort without first closing current connection. Close connection first.");
			return false;
		}

		/* If the portname is on the list of active ports */
		if (Arrays.stream(getActiveSerialPorts()).filter(port -> port.equals(portName)).count() > 0) {

			try {

				/* If the portname passed matches NO_SERIAL_PORT */
				if (portName.equals(SensorDisplay.NO_SERIAL_PORT)) {
					Out.out.loglnErr("Can't connect to nothing!");
					return false;
				}

				/*
				 * Set basic parameters, if connection fails here most likely
				 * port is in use
				 */
				this.port = new SerialPort(portName);
				this.port.openPort();
				this.port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				this.portName = portName;

				/*
				 * At this point we've successfully connected to the port, now
				 * to acknowledge and discover whether this is the MBED device
				 * or not
				 */
				this.portReader = new SerialReaderEventHandler(port);
				this.port.addEventListener(portReader);
				int retries = RETRY_CONNECTION_ATTEMPTS;

				try {

					/* While we haven't connected and we still have to retry */
					while (retries > 0) {

						/* Send Acknowledgement */
						Out.out.log("Sending ACK. ");
						if (!sendPayload("#ACK")) {
							Out.out.logln("Unable to transmit, retrying... " + retries-- + " more times...");
							continue;
						}

						/* Wait for response */
						Thread.sleep(250);

						/*
						 * If we can find a message received that has matched
						 * our acknowledgement
						 */
						if (portReader.find("ACKR") != null) {
							/*
							 * Acknowledge this message to say we want
							 * temperature samples
							 */
							if (sendPayload("#ACKC")) {
								Out.out.logln("Established connection to sensor on \"" + portName + "\".");
								return true;
							} else {
								/*
								 * We couldn't respond to their acknowledgement
								 */
								Out.out.logln(
										"Received but unable to confirm, retrying... " + retries-- + " more times...");
							}
						} else {
							Out.out.logln("No response, retrying " + retries-- + " more times...");
						}

					}

				} catch (InterruptedException e) {
					Out.out.loglnErr("Error while waiting for response from SerialPort.");
					e.printStackTrace();
				}

				/*
				 * If all else fails, close any open ports and make the
				 * portReader null
				 */
				portReader = null;
				port.closePort();

				return false;

			} catch (SerialPortException exe) {
				Out.out.loglnErr("An error occurred while trying to open the connection \"" + portName + "\".");
				// exe.printStackTrace();
				return false;
			}

		} else {
			Out.out.loglnErr("Invalid Serial Port specified \"" + portName + "\".");
			return false;
		}

	}

	/**
	 * Close current connection to port, will send a disconnect acknowledgement
	 * to the MBED and tell it stop transmitting as well.
	 * 
	 * @return true if disconnected, false otherwise
	 */
	public boolean closePort() {
		if (port != null && port.isOpened()) {
			try {
				portReader.clearBuffer();
				if (!sendPayload("#DIS")) {
					Out.out.loglnErr(
							"Failed to acknowledge sensor disconnect, disconnecting regardless but sensor is unaware.");
				}
				return port.closePort();
			} catch (SerialPortException e) {
				Out.out.loglnErr("An error occurred while trying to close the connection \"" + portName + "\".");
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Whether we are currently connected to anything
	 * 
	 * @return
	 */
	public boolean connected() {
		return port != null && port.isOpened();
	}

	/**
	 * A deep clone of all messages currently queued within the
	 * SerialReaderEventHandler
	 * 
	 * @return
	 */
	public List<Message> getAllMessages() {
		return portReader.messages();
	}

	/**
	 * Pop a message from the queue, will return null if no message is ready or
	 * the SerialPort is null
	 * 
	 * @return the popped message
	 */
	public Message popMessage() {
		return portReader != null ? portReader.popMessage() : null;
	}

	/**
	 * Recursively pop messages until we find the one we're looking for, will
	 * return null if no message is found or the SerialPort is null
	 * 
	 * @return the popped message
	 */
	public Message popMessage(String data) {
		return portReader != null ? portReader.popMessage(data) : null;
	}

	/**
	 * Pop the latest message of the message type found, if none are present
	 * returns null or if the portReader is null
	 * 
	 * @return the popped message
	 */
	public Message popLatestMessage(String data) {
		return portReader != null ? portReader.popLatestMessage(data) : null;
	}

	/**
	 * Send a message to the MBED device, used for sending Acknowledgements and
	 * responses to settings (i.e changing the refresh rate).
	 * 
	 * @param payload
	 *            The payload to transmit
	 * @return Whether the message was delivered or not
	 */
	public boolean sendPayload(String payload) {
		if (port != null && port.isOpened()) {
			try {
				return port.writeBytes(payload.getBytes());
			} catch (SerialPortException e) {
				Out.out.loglnErr("An error occurred while trying to write to the connection \"" + portName + "\".");
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * The currently active serial ports name
	 * @return name of port
	 */
	public String getActivePort() {
		return portName;
	}
	
	/**
	 * Wrapper singleton for handling inner workings of the SerialReaderEvents
	 * 
	 * Designed to listen and handle all incoming data on the open serial port,
	 * will try to form messages from incoming data and store that in a queue
	 * while providing methods to access the relevant data from the queue.
	 * 
	 * @author Dante
	 *
	 */
	private static class SerialReaderEventHandler implements SerialPortEventListener {

		private final SerialPort openPort;
		private static final List<Message> queuedInput = new ArrayList<>();
		/* Buffer used to read in bytes at a time */
		private char[] readBuffer;
		private byte pointer;

		public SerialReaderEventHandler(SerialPort port) {
			this.openPort = port;
			readBuffer = new char[256];
			if (port == null) {
				throw new NullPointerException("Cannot create SerialThread with null SerialPort.");
			}
		}

		/**
		 * Empty the buffer
		 */
		public void clearBuffer() {
			queuedInput.clear();
			readBuffer = null;
			pointer = 0;
		}

		/**
		 * Read input, reads all bytes it can at once and tries to form messages
		 * from the input by placing it into a buffer. The MTU for messages
		 * being sent this manner is 127 as anything exceeding this will cause a
		 * overflow of the buffer. Any messages successfully read in will placed
		 * into a Message object and added to the queue.
		 * 
		 * @param bytesToRead
		 *            number of bytes to read in from serial
		 * @return true if no errors were encountered while reading, does not
		 *         account for errors in messages
		 */
		private boolean readInput(int bytesToRead) {

			try {

				if (bytesToRead > 127)
					bytesToRead = 127;

				byte[] buffer = openPort.readBytes(bytesToRead);

				for (Byte b : buffer) {
					readBuffer[pointer++] = (char) (b & 0xFF);
				}

				String potential = "";

				/* Add to, wrap around and feed buffer */
				for (char b : readBuffer) {
					if (potential.equals("") && b == '#') { // Found the
															// beginning of a
															// new message
						potential += b;
					} else if (!potential.equals("") && b == '#') { // Found a
																	// new
																	// message
						Out.out.recordToLog(" {MSG} -> {" + potential.replaceAll("#", "") + "} \n", true);
						addNewMessage(potential);
						readBuffer = new char[256];
						pointer = 0;
						break;
					} else { // Concatenating onto a premature message
						potential += b;
					}
				}

				// openPort.purgePort(SerialPort.PURGE_RXCLEAR);

				return true;

			} catch (SerialPortException exe) {
				exe.printStackTrace();
				Out.out.loglnErr("Failed to read from SerialPort \"" + openPort.getPortName() + "\".");

			}

			return false;

		}

		/**
		 * Create a new message and add it to the queue
		 * 
		 * @param potential
		 *            The potential for a new message
		 */
		private void addNewMessage(String potential) {

			Message msg = MessageHandler.getHandler().createMessage(potential);
			
			/* Discard failed message */
			if(msg.getName().equals(MessageHandler.ERR)){
				return;
			}
			
			queuedInput.add(msg);

		}

		/**
		 * The serialportevent listener, taken example from the JSSC wiki
		 * examples. Only utilising the RX listener.
		 */
		@Override
		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR()) {// If data is available
				/*
				 * Once a single byte is available, read the amount that can be
				 * read into the readInput function
				 */
				if (!readInput(event.getEventValue())) {
					Out.out.loglnErr("Failed to read from SerialPort \"" + openPort.getPortName() + "\".");
				}

			} else if (event.isCTS()) {// If CTS line has changed state
				if (event.getEventValue() == 1) {// If line is ON
					System.out.println("CTS - ON");
				} else {
					System.out.println("CTS - OFF");
				}
			} else if (event.isDSR()) {/// If DSR line has changed state
				if (event.getEventValue() == 1) {// If line is ON
					System.out.println("DSR - ON");
				} else {
					System.out.println("DSR - OFF");
				}
			}
		}

		/**
		 * Pop the earliest message from the queue regardless of message type
		 * 
		 * @return Message if present, if empty then null
		 */
		public Message popMessage() {
			if (queuedInput.size() > 0) {
				Message msg = queuedInput.get(0);
				queuedInput.remove(0);
				return msg;
			} else {
				return null;
			}
		}

		/**
		 * Pop the earliest message of a specific type from the queue, utilises
		 * tail recursion
		 * 
		 * @param type
		 *            The message type
		 * @return Message if present, if empty then null
		 */
		public Message popMessage(String type) {
			return popMessage(type, 0);
		}

		/**
		 * Pop the latest message of a specific type from the queue, utilises
		 * tail recursion
		 * 
		 * @param type
		 *            The message type
		 * @return Message if present, if empty then null
		 */
		public Message popLatestMessage(String type) {
			Message msg = popMessage(type, 0);

			if (msg == null)
				return null;

			while (!msg.getName().equals(type)) {
				msg = popMessage(type, 0);
			}
			return msg;
		}

		/**
		 * Tail recursive private function designed to find a the earliest
		 * message of a specific type, will NOT discard any messages encountered
		 * until it finds one of the type it expects
		 * 
		 * @param type
		 *            The type of message looked for
		 * @param n
		 *            The position in queue
		 * @return The message if present, or null if not found
		 */
		private Message popMessage(String type, int n) {

			if (queuedInput.size() > n) {
				Message msg = queuedInput.get(n);
				if (msg == null)
					return null;
				if (msg.getName().equals(type)) {
					queuedInput.remove(n);
					return msg;
				} else {
					return popMessage(type, ++n);
				}
			} else {
				return null;
			}

		}

		/**
		 * Will find the first message payload that matches the sequence
		 * expected
		 * 
		 * @param str
		 *            The sequence to match against
		 * @return Message if found else null
		 */
		public Message find(String str) {
			Message msg = popMessage();

			if (msg != null && msg.getPayload() == null)
				return find(str);

			while (msg != null && msg.getPayload() != null && str != null && !msg.getPayload().equals(str)) {
				msg = popMessage();
			}
			return msg;
		}

		/**
		 * A deep clone of all messages currently queued 
		 * 
		 * @return copy of queuendInput
		 */
		public List<Message> messages() {
			return new ArrayList<>(queuedInput);
		}

	}


}
