
package kent.dja33.iot.a1.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import kent.dja33.iot.a1.Main;

/**
 * Logger class to output logs to a directory and log.txt
 * 
 * @author dja33
 */
public final class Out {

	/* Directories for use across the system */
	private static final String ROOT_DIRECTORY = System.getProperty("user.dir") + File.separator + "SensorMBED";
	public static final String RESOURCES_DIRECTORY = ROOT_DIRECTORY + File.separator + "Resources";

	/* Format to output logged data in */
	private static final DateFormat logTimeFormat = new SimpleDateFormat("HH:mm:ss");
	private boolean printDebugMessages = true;

	/* The absolute path for use in creating log files */
	private final String LOG_FILE_PATH;
	private final File log;
	private boolean canLog;

	/* Singleton reference */
	private BufferedWriter writer;
	public static Out out = new Out(ROOT_DIRECTORY + File.separator + "Logs");

	/**
	 * Create the logger, pass the directory expected to create log files in
	 * 
	 * @param logDir
	 */
	private Out(String logDir) {

		recordToLog("Creating logger...", true);

		File dir = new File(logDir);

		/* If the directory for creating logs in does not exist create it */
		if (!dir.exists()) {
			recordToLog("Directory '" + dir + "' did not exist. Creating it now...", true);
			dir.mkdirs();
		}

		/* Set the path for the log file */
		LOG_FILE_PATH = logDir + File.separator + "log" + new SimpleDateFormat("dd-MM-yy").format(new Date()) + ".txt";
		log = new File(LOG_FILE_PATH);

		/* If the file does not exist then populate the new file */
		if (!log.exists()) {
			try {
				recordToLog("Creating new log file: " + LOG_FILE_PATH, true);
				log.createNewFile();
			} catch (IOException ioe) {
				recordToLog("Logger could not create file to write out to: " + ioe.getMessage(), true);
				canLog = false;
				return;
			}
		} else {
			/* File already existed, append to the existing file */
			recordToLog("File exists: " + log.getAbsolutePath(), true);
		}
		try {
			writer = new BufferedWriter(new FileWriter(log, true));
			writer.write(" ___________________________________________________________" + System.lineSeparator());
			writer.write("|                                                           |" + System.lineSeparator());
			writer.write("| - - Start - - - - - - - - - - - - - - - - - - - - - - - - |" + System.lineSeparator());
			writer.write("|___________________________________________________________|" + System.lineSeparator());
			writer.newLine();
		} catch (IOException ioe) {
			recordToLog("Logger could not open file to write out to: " + ioe.getMessage(), true);
			canLog = false;
			return;
		}

		canLog = true;
	}

	/**
	 * Close the logger in use and all resources being used by it, including any
	 * files currently open by the logger
	 */
	public static void close() {
		out.logln("Closing logger.");
		if (out != null && out.writer != null) {
			out.logln("Closing writer for logger.");
			try {
				out.writer.close();
			} catch (IOException e) {
				out.loglnErr("Failed to close logger!.");
				e.printStackTrace();
			}
		}
		if (out != null) {
			out = null;
		}
	}

	/* Default message log prefix */
	private static final String PREFIX = "[LOG] ";

	/**
	 * Log out an input object, will utilise the GUI if possible, suffixed with
	 * '\n'
	 * 
	 * @param obj
	 *            what to output
	 */
	public void logln(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.println(obj);
		} else {
			System.out.println(PREFIX + obj);
		}

		recordToLog(obj + System.lineSeparator(), true);
	}

	/**
	 * Log out a '\n', utilises GUI if possible
	 * 
	 * @param obj
	 *            what to output
	 */
	public void logln() {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.println();
		} else {
			System.out.println();
		}

		recordToLog(System.lineSeparator(), false);
	}

	/**
	 * Log out an input object, will utilise the GUI if possible
	 * 
	 * @param obj
	 *            what to output
	 */
	public void log(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.print(obj);
		} else {
			System.out.print(obj.toString());
		}

		recordToLog(obj.toString(), false);

	}

	/**
	 * Log out an input object, will utilise the GUI if possible, includes a
	 * timestamp
	 * 
	 * @param obj
	 *            what to output
	 */
	public void logWithTime(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.print(obj);
		} else {
			System.out.print(obj.toString());
		}

		recordToLog(obj.toString(), true);

	}

	/* The PREFIX for error messages */
	private static final String ERROR_PREFIX = "[ERROR] ";

	/**
	 * Log out an input object, will utilise the GUI if possible, suffixed with
	 * '\n' includes a prefix of the ERROR_PREFIX
	 * 
	 * @param obj
	 *            what to output
	 */
	public void loglnErr(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.printlnErr(obj);
		} else {
			System.out.println(ERROR_PREFIX + obj.toString());
		}

		recordToLog(ERROR_PREFIX + obj.toString(), true);
	}

	/**
	 * Is the logger printing debug messages
	 * 
	 * @return
	 */
	public boolean isPrintingDebugMessages() {
		return printDebugMessages;
	}

	/**
	 * Set whether the log should print debug messages
	 * 
	 * @param print
	 *            true if it shouldF
	 */
	public void setPrintingDebugMessages(boolean print) {
		printDebugMessages = print;
	}

	/**
	 * Attempt to write out a passed message to the current log file, if chosen
	 * will also log the current time as well.
	 * 
	 * @param str
	 *            the message to log
	 * @param logTime
	 *            true to log time
	 */
	public void recordToLog(String str, boolean logTime) {

		if (canLog) {

			try {
				if (logTime) {
					writer.write(String.format("[%s] %s", logTimeFormat.format(new Date()), str));
				} else {
					writer.write(String.format("%s", str));
				}
			} catch (IOException ioe) {
				canLog = false;
				loglnErr("Logger could not write to file: " + ioe.getMessage());
			}

		} else {
			System.err.println(str);
		}

	}

	/**
	 * Get the directory of the log
	 * 
	 * @return directory
	 */
	public String getDirectory() {
		return log.getParent();
	}

	/**
	 * Get the absolutePath for the current log file
	 * 
	 * @return path
	 */
	private String getAbsolutePath() {
		return log.getAbsolutePath();
	}

}
