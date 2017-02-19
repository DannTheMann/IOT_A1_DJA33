package kent.dja33.iot.a1;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kent.dja33.iot.a1.util.Out;
import kent.dja33.iot.a1.util.SerialReader;

/**
 * Main view for the SensorDisplay, creates all the buttons
 * and views for FX to handle and then handles an AnimationTimer
 * to update the LineChart on show. 
 * 
 * The View itself gives buttons for connecting/disconnecting, viewing
 * serialports, the linechart, an output log and finally buttons that
 * allow the user to increase or decrease the rate of readings. 
 * 
 * The user can also scroll on the graph to increase or decrease the
 * ranges of values currently shown. 
 * 
 * @author Dante
 *
 */
public class SensorDisplay extends Application {

	/* Const fields for display */
	private static final String TITLE = "MBED FRDM-K64F Temperature Monitor";
	private BorderPane root;
	private VBox buttonPanel;
	
	/* Commonly used buttons */
	private Button connect;
	private Button disconnect;
	private Button autodetect;
	private Button increase;
	private Button decrease;
	
	/* Selection box for available serial ports */
	private ComboBox<String> serialPortSelection;
	
	/* LineChart using the Date as a string on the X Axis
	 * and the temperature reading as a number on the Y Axis */
	private LineChart<String, Number> lineChart;
	private TemperatureHandler temperature;
	
	/* Static such that the Output reader can access it */
	private static TextArea logBox;
	
	/* If the GUI has been built and is ready */
	private static boolean ready = false;

	/* View resolutions */
	public static final int MAX_WINDOW_WIDTH = 1920;
	public static final int MAX_WINDOW_HEIGHT = 1080;
	public static final int MIN_WINDOW_WIDTH = 720;
	public static final int MIN_WINDOW_HEIGHT = 480;
	public static final String NO_SERIAL_PORT = "NO SERIAL PORT";

	@Override
	public void start(Stage stage) throws Exception {
		
		/*
		 * Start here, called from launch method and builds the GUI
		 * including the scene and any wrapper components.
		 */
		
		stage.setTitle(TITLE);

		root = new BorderPane();

		{

			/* Our list of buttons to go on the side of the graph */
			buttonPanel = new VBox(10);

			{

				/* All available serial ports */
				ObservableList<String> options = FXCollections.observableArrayList(new String[] { NO_SERIAL_PORT });
				options.addAll(SerialReader.in.getActiveSerialPorts());

				serialPortSelection = new ComboBox<>(options);

				serialPortSelection.getSelectionModel().select(0);

				serialPortSelection.valueProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue,
							String newValue) {
						connect.setDisable(newValue == NO_SERIAL_PORT || SerialReader.in.connected());
					}
				});

				/* Button to refresh the available serial ports */
				Button button = new Button("Refresh");
				button.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						Out.out.logln("Refreshing available serial ports.");
						serialPortSelection.getItems().clear();
						serialPortSelection.getItems().add(NO_SERIAL_PORT);
						serialPortSelection.getItems().addAll(SerialReader.in.getActiveSerialPorts());
						serialPortSelection.getSelectionModel().select(0);

					}

				});
				
				buttonPanel.getChildren().add(serialPortSelection);
				buttonPanel.getChildren().add(button);

				/* Connection button, by default disabled as we have nothing to connect to at first */
				connect = new Button("Connect");
				connect.setDisable(true);
				connect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						/* Attempt to connect on the specified port from the ComboBox */
						if (SerialReader.in.openPort(serialPortSelection.getSelectionModel().getSelectedItem())) {
							connectSuccessful();
						}else{
							Out.out.logln("Unable to connect to \"" + serialPortSelection.getSelectionModel().getSelectedItem() + "\"");
						}

					}

				});

				/* Disconnection button, by default disabled as we have nothing to disconnect from at first */
				disconnect = new Button("Disconnect");
				disconnect.setDisable(true);
				disconnect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						if (SerialReader.in.closePort()) {
							disconnectSuccessful();
						}

					}

				});

				buttonPanel.getChildren().add(connect);
				buttonPanel.getChildren().add(disconnect);
				
				/* Auto connect button, attempts to connect to any and all open serial channels */
				autodetect = new Button("Auto-detect");
				autodetect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						Out.out.logln("Attempting to automatically connect to Serial port.");
						autodetect.setDisable(true);
						
						/* For every serial channel, attempt to connect */
						for (String com : SerialReader.in.getActiveSerialPorts()) {
							if (SerialReader.in.openPort(com)) { // If successful
								connectSuccessful();
								autodetect.setText("Auto-detect");
								return;
							}
						}

						/* Unable to automatically connect */
						serialPortSelection.getSelectionModel().select(0);
						logBox.setScrollTop(Double.MAX_VALUE);
						Out.out.loglnErr("Could not automatically connect to any sensor devices.");

					}

				});

				/* Clear the text log at the bottom of the GUI */
				button = new Button("Clear log");
				button.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						logBox.setText("");
						Out.out.logln("Log cleared.");

					}

				});

				buttonPanel.getChildren().add(autodetect);
				buttonPanel.getChildren().add(button);
				
				// Set padding between each node in the button panel
				buttonPanel.setPadding(new Insets(5, 5, 5, 5));
				
				/* Increase the rate at which we receive samples */
				increase = new Button("Increase Rate");
				increase.setOnAction((event)->{
					Out.out.logln("Increasing the refresh rate.");
					SerialReader.in.sendPayload("#T0");
				});
				
				buttonPanel.getChildren().add(increase);
				
				/* Decrease the rate at which we receive samples */
				decrease = new Button("Decrease Rate");
				decrease.setOnAction((event)->{
					Out.out.logln("Decreasing the refresh rate.");
					SerialReader.in.sendPayload("#T1");
				});
				
				buttonPanel.getChildren().add(decrease);

				/* By default we are not connected, so both disabled */
				increase.setDisable(true);
				decrease.setDisable(true);
				
				/* For every node added, force the width and height */
				for (Node n : buttonPanel.getChildren()) {
					if (n instanceof Region) {
						Region node = (Region) n;
						node.setMaxWidth(120);
						node.setMinWidth(120);
					}
				}
				
			}

			buttonPanel.setAlignment(Pos.CENTER_LEFT);

			/* Create the log area and center it */
			StackPane sp = new StackPane();

			{

						logBox = new TextArea();
						logBox.setEditable(false);

						logBox.textProperty().addListener(new ChangeListener<Object>() {
							@Override
							public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
								/* Whenever something is added to the text area, automatically scroll to the bottom */
								logBox.setScrollTop(Double.MAX_VALUE); // this will
																	// scroll
																	// to the
																	// bottom
								// use Double.MIN_VALUE to scroll to the top
							}
						});

						logBox.setMaxHeight(MAX_WINDOW_HEIGHT / 10);
						logBox.setMinHeight(MIN_WINDOW_HEIGHT / 10);
						logBox.setMaxWidth(MAX_WINDOW_WIDTH / 3);
						logBox.setMinWidth(MIN_WINDOW_WIDTH / 3);

			}

			sp.getChildren().add(logBox);
			
			/* Pad it from the exterior */
			sp.setPadding(new Insets(5, 5, 20, 5));

			sp.setAlignment(Pos.CENTER);
			root.setBottom(sp);
		}

		root.setRight(buttonPanel);

		/* Now need to create the Chart used, in this case using a 
		 * LineChart with DateAsString for the X Axis and the temp
		 * as the Y Axis. */
		final CategoryAxis xAxis = new CategoryAxis();
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Time");
		yAxis.setLabel("Temperature");

		/* Advice taken from JewelSea on handling updating animated graphs, adapted for LineChart */
		lineChart = new LineChart<String, Number>(xAxis, yAxis) {
			// Override to remove symbols on each data point
			@Override
			protected void dataItemAdded(Series<String, Number> series, int itemIndex, Data<String, Number> item) {

			}
		};

		/* When the user scrolls on the LineChart, update it's scale */
		lineChart.setOnScroll((event) -> {

			temperature.resizeChart(event.getDeltaY());

		});

		/* Remove automatic ranging for Y as this is updated manually in scaling */
		yAxis.setAutoRanging(false);
		xAxis.setAutoRanging(true);

		/* Remove the legend as we only have one value being show */
		lineChart.setLegendVisible(false);
		
		/* Animation adds a layer of confusion onto the graph, so this is removed
		 * to allow the transitions between added elements to be simpler */
		lineChart.setAnimated(false);
		lineChart.setTitle("Temperature Samples");

		root.setCenter(lineChart);

		/* Create the scene, give it the borderpane which now contains all our elements */
		Scene scene = new Scene(root, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

		stage.setScene(scene);

		/* Constraints on interface */
		stage.setMinHeight(MIN_WINDOW_HEIGHT);
		stage.setMinWidth(MIN_WINDOW_WIDTH);
		stage.setMaxHeight(MAX_WINDOW_HEIGHT);
		stage.setMaxWidth(MAX_WINDOW_WIDTH);

		/* If we close the GUI prematurely then close connections and logger, exit smoothly */
		stage.setOnCloseRequest(e -> {
			SerialReader.in.closePort();
			Out.close();
			System.exit(0);
		});
		
		/* GUI is now ready */
		ready = true;

		/* Create our new TemperatureHandler and give it the LineChart and Axis */
		temperature = new TemperatureHandler(lineChart, xAxis, yAxis);

		/* Schedule Message Parser to run every 250ms */
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(temperature, 0, 250, TimeUnit.MILLISECONDS);

		/* Finally show everything */
		stage.show();
	}

	/**
	 * If the connection was successful then call
	 * this method, will update the GUI components
	 * and tell the temperature reader to start
	 * parsing messages
	 */
	protected void connectSuccessful() {
		Out.out.logln("Connected to \"" + SerialReader.in.getActivePort() + "\".");
		connect.setDisable(true);
		autodetect.setDisable(true);
		disconnect.setDisable(false);
		increase.setDisable(false);
		decrease.setDisable(false);
		temperature.start();
	}

	/**
	 * If we have disconnected then call this
	 * method, will update GUI components and 
	 * stop the temperature reader from parsing
	 * anymore messages.
	 */
	protected void disconnectSuccessful() {
		Out.out.logln("Disconnected from \"" + SerialReader.in.getActivePort() + "\".");
		disconnect.setDisable(true);
		connect.setDisable(false);
		autodetect.setDisable(false);
		increase.setDisable(true);
		decrease.setDisable(true);
		temperature.stop();
	}

	/**
	 * Whether the GUI has been built and
	 * is on display.
	 * @return true if built
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Launch the GUI
	 */
	public void launch() {
		super.launch();
	}

	/**
	 * Print a message to the log, prefixed with [WARNING] {object}
	 * and suffix with newline '\n'
	 * @param obj The object to display in the log
	 */
	public void printlnErr(Object obj) {
		logBox.appendText("[WARNING] " + obj + "\n");
	}

	/**
	 * Print a message to the log without newline '\n'
	 * @param obj The object to display in the log
	 */
	public void print(Object obj) {
		logBox.appendText(obj + "");
	}

	/**
	 * Print a newline to the log '\n'
	 */
	public void println() {
		logBox.appendText("\n");
	}

	/**
	 * Print a message to the log with suffix of newline '\n'
	 * @param obj The object to display in the log
	 */
	public void println(Object obj) {
		logBox.appendText(obj + "\n");
	}

}
