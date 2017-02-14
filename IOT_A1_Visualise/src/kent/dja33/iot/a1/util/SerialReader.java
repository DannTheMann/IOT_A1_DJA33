package kent.dja33.iot.a1.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class SerialReader {

	public static final SerialReader in = new SerialReader();
	private static final int RETRY_CONNECTION_ATTEMPTS = 3;
	private String portName;
	private SerialPort port;
	private SerialReaderEventHandler portReader;

	private SerialReader() {
		portName = "n/a";
	}

	public String[] getActiveSerialPorts() {
		return SerialPortList.getPortNames();
	}

	public boolean openPort(String portName) {

		if (port != null && port.isOpened()) {
			Out.out.loglnErr(
					"Cannot open SerialPort without first closing current connection. Close connection first.");
			return false;
		}

		if (Arrays.stream(getActiveSerialPorts()).filter(port -> port.equals(portName)).count() > 0) {

			try {

				if (portName.equals("NONE")) {
					Out.out.loglnErr("Can't connect to nothing!");
					return false;
				}

				port = new SerialPort(portName);
				port.openPort();
				port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				this.portName = portName;

				portReader = new SerialReaderEventHandler(port);
				port.addEventListener(portReader);
				int retries = RETRY_CONNECTION_ATTEMPTS;

				try {

					while (retries > 0) {

						// Send Acknowledgement
						Out.out.logln("Sending ACK. ");

						if (!sendPayload("#ACK")) {
							Out.out.logln("Unable to transmit, retrying... " + retries-- + " more times...");
							continue;
						}

						// Wait for response
						Thread.sleep(500);

						if (portReader.find("ACKR") != null) {
							if (sendPayload("#ACKC")) {
								Out.out.logln("Established connection to sensor on \"" + portName + "\".");
								portReader.setReadingData(true);
								return true;
							} else {
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

	public boolean connected() {
		return port != null && port.isOpened();
	}

	public List<String> getAllMessages() {
		return portReader.messages();
	}

	public String popMessage() {
		return portReader != null ? portReader.popMessage() : null;
	}

	public String pollActiveSerialPort() {
		return null;
	}

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

	private static class SerialReaderEventHandler implements SerialPortEventListener {

		private final SerialPort openPort;
		private final List<String> queuedInput = new ArrayList<>();
		private char[] readBuffer;
		private byte pointer;
		private boolean readingData;

		public SerialReaderEventHandler(SerialPort port) {
			this.openPort = port;
			readBuffer = new char[256];
			if (port == null) {
				throw new NullPointerException("Cannot create SerialThread with null SerialPort.");
			}
		}

		public void clearBuffer() {
			queuedInput.clear();
			readBuffer = null;
			pointer = 0;
		}

		private boolean readInput(int bytesToRead) {

			try {
				
				if(bytesToRead > 127)
					bytesToRead = 127;
				
				byte[] buffer = openPort.readBytes(bytesToRead);
				
				for(Byte b : buffer){
					readBuffer[pointer++] = (char) (b & 0xFF);
				}

				String potential = "";
				
				for(char b : readBuffer){				
					if(potential.equals("") && b == '#'){
						potential += b;
					}else if(!potential.equals("") && b == '#'){
						Out.out.recordToLog(" {MSG} -> {" + potential.replaceAll("#","") + "} \n", true);
						queuedInput.add(potential.replaceAll("#",""));
						readBuffer = new char[256];
						pointer = 0;
						break;
					}else{
						potential += b;
					}
				}

				openPort.purgePort(SerialPort.PURGE_RXCLEAR);

				return true;

			} catch (SerialPortException exe) {
				exe.printStackTrace();
				Out.out.loglnErr("Failed t222o read from SerialPort \"" + openPort.getPortName() + "\".");

			}

			return false;

		}

		@Override
		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR()) {// If data is available
					boolean result = readInput(event.getEventValue());
					if (!result) {
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

		public String popMessage() {
			if (queuedInput.size() > 0) {
				String msg = queuedInput.get(0);
				queuedInput.remove(0);
				return msg;
			} else {
				return null;
			}
		}

		public String find(String str) {
			String msg = popMessage();
			while (msg != null && !msg.equals(str)) {
				msg = popMessage();
			}
			return msg;
		}

		public List<String> messages() {
			return new ArrayList<>(queuedInput);
		}

		public boolean isReadingData() {
			return readingData;
		}

		public void setReadingData(boolean readingData) {
			this.readingData = readingData;
		}

	}

}
