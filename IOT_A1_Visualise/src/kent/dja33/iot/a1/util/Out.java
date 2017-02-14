/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

	private static final String ROOT_DIRECTORY = System.getProperty("user.dir") + File.separator + "SensorMBED";
	public static final String RESOURCES_DIRECTORY = ROOT_DIRECTORY + File.separator + "Resources";

	private static final DateFormat logTimeFormat = new SimpleDateFormat("HH:mm:ss");
	private boolean printDebugMessages = true;
	private final String LOG_DIRECTORY;
	private final File log;
	private boolean canLog;
	private BufferedWriter writer;
	public static Out out = new Out(ROOT_DIRECTORY + File.separator + "Logs");

	private Out(String logDir) {
		
		recordToLog("Creating logger...", true);

		File dir = new File(logDir);

		if (!dir.exists()) {
			recordToLog("Directory '" + dir + "' did not exist. Creating it now...", true);
			dir.mkdirs();
		}
		LOG_DIRECTORY = logDir + File.separator + "log" + new SimpleDateFormat("dd-MM-yy").format(new Date()) + ".txt";
		log = new File(LOG_DIRECTORY);
		if (!log.exists()) {
			try {
				recordToLog("Creating new log file: " + LOG_DIRECTORY, true);
				log.createNewFile();
			} catch (IOException ioe) {
				recordToLog("Logger could not create file to write out to: " + ioe.getMessage(), true);
				canLog = false;
				return;
			}
		} else {
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

	public static boolean createLogger(String dir) {
		if (out != null) {
			Main.display.printlnErr("Can't create new logger, one already exists. Must be closed first.");
			return false;
		} else {
			out = new Out(dir);
			out.logln("Created new logger. '" + out.getAbsolutePath() + "'");
			return true;
		}
	}

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
		// ...
	}

	private static final String PREFIX = "[LOG] ";

	public void logln(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.println(obj);
		} else {
			System.out.println(PREFIX + obj);
		}

		recordToLog(obj + System.lineSeparator(), true);
	}

	public void logln() {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.println();
		} else {
			System.out.println();
		}

		recordToLog(System.lineSeparator(), false);
	}

	public void log(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.print(obj);
		} else {
			System.out.print(obj.toString());
		}

		recordToLog(obj.toString(), false);

	}

	public void logWithTime(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.print(obj);
		} else {
			System.out.print(obj.toString());
		}

		recordToLog(obj.toString(), true);

	}

	public void loglnErr(Object obj) {
		if (Main.display != null && Main.display.isReady()) {
			Main.display.printlnErr(obj);
		} else {
			System.out.println(ERROR_PREFIX + obj.toString() + " | " + Main.display.isReady());
		}

		recordToLog(ERROR_PREFIX + obj.toString(), true);
	}

	public boolean isPrintingDebugMessages() {
		return printDebugMessages;
	}

	public void setPrintingDebugMessages(boolean print) {
		printDebugMessages = print;
	}

	private static final String ERROR_PREFIX = "[ERROR] ";

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

		}

	}

	public String getDirectory() {
		return log.getParent();
	}

	private String getAbsolutePath() {
		return log.getAbsolutePath();
	}

}
