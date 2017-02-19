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

public class SensorDisplay extends Application {

	private static final String TITLE = "MBED FRDM-K64F Temperature Monitor";
	private BorderPane root;
	private VBox buttonPanel;
	private Button connect;
	private Button disconnect;
	private Button autodetect;
	private LineChart<String, Number> lineChart;
	private TemperatureHandler temperature;
	private static TextArea logBox;
	private static boolean ready = false;

	public static final int MAX_WINDOW_WIDTH = 1920;
	public static final int MAX_WINDOW_HEIGHT = 1080;
	public static final int MIN_WINDOW_WIDTH = 720;
	public static final int MIN_WINDOW_HEIGHT = 480;
	public static final String NO_SERIAL_PORT = "NO SERIAL PORT";
	
	private ComboBox<String> serialPortSelection;
	private Button increase;
	private Button decrease;

	@Override
	public void start(Stage stage) throws Exception {
		stage.setTitle(TITLE);

		root = new BorderPane();

		{

			buttonPanel = new VBox(10);

			{

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

				connect = new Button("Connect");
				connect.setDisable(true);

				connect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						if (SerialReader.in.openPort(serialPortSelection.getSelectionModel().getSelectedItem())) {
							connectSuccessful();
						}else{
							Out.out.logln("Unable to connect to \"" + serialPortSelection.getSelectionModel().getSelectedItem() + "\"");
						}

					}

				});

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

				autodetect = new Button("Auto-detect");

				autodetect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						Out.out.logln("Attempting to automatically connect to Serial port.");
						autodetect.setDisable(true);
						
						for (String com : SerialReader.in.getActiveSerialPorts()) {
							if (SerialReader.in.openPort(com)) {
								connectSuccessful();
								autodetect.setText("Auto-detect");
								return;
							}
						}

						autodetect.setDisable(false);

						serialPortSelection.getSelectionModel().select(0);
						logBox.setScrollTop(Double.MAX_VALUE);
						Out.out.loglnErr("Could not automatically connect to any sensor devices.");

					}

				});

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
				buttonPanel.setPadding(new Insets(5, 5, 5, 5));
				
				increase = new Button("Increase Rate");
				increase.setOnAction((event)->{
					Out.out.logln("Increasing the refresh rate.");
					SerialReader.in.sendPayload("#T0");
				});
				
				buttonPanel.getChildren().add(increase);
				
				decrease = new Button("Decrease Rate");
				decrease.setOnAction((event)->{
					Out.out.logln("Decreasing the refresh rate.");
					SerialReader.in.sendPayload("#T1");
				});
				
				buttonPanel.getChildren().add(decrease);

				increase.setDisable(true);
				decrease.setDisable(true);
				
				for (Node n : buttonPanel.getChildren()) {
					if (n instanceof Region) {
						Region node = (Region) n;
						node.setMaxWidth(120);
						node.setMinWidth(120);
					}
				}
				
			}

			buttonPanel.setAlignment(Pos.CENTER_LEFT);

			StackPane sp = new StackPane();

			{

						logBox = new TextArea();
						logBox.setEditable(false);

						logBox.textProperty().addListener(new ChangeListener<Object>() {
							@Override
							public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
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
			sp.setPadding(new Insets(5, 5, 20, 5));

			sp.setAlignment(Pos.CENTER);
			root.setBottom(sp);
		}

		root.setRight(buttonPanel);

		// defining the axes
		final CategoryAxis xAxis = new CategoryAxis();
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Time");
		yAxis.setLabel("Temperature");
		// creating the chart
		lineChart = new LineChart<String, Number>(xAxis, yAxis) {
			// Override to remove symbols on each data point
			@Override
			protected void dataItemAdded(Series<String, Number> series, int itemIndex, Data<String, Number> item) {

			}
		};

		lineChart.setOnScroll((event) -> {

			temperature.resizeChart(event.getDeltaY());

		});

		yAxis.setAutoRanging(false);
		xAxis.setAutoRanging(true);

		lineChart.setLegendVisible(false);
		lineChart.setAnimated(false);
		lineChart.setTitle("Temperature Samples");
		// defining a series

		root.setCenter(lineChart);

		Scene scene = new Scene(root, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

		stage.setScene(scene);

		/* Constraints on interface */
		stage.setMinHeight(MIN_WINDOW_HEIGHT);
		stage.setMinWidth(MIN_WINDOW_WIDTH);
		stage.setMaxHeight(MAX_WINDOW_HEIGHT);
		stage.setMaxWidth(MAX_WINDOW_WIDTH);

		ready = true;

		stage.setOnCloseRequest(e -> {
			SerialReader.in.closePort();
			Out.close();
			System.exit(0);
		});

		temperature = new TemperatureHandler(lineChart, xAxis, yAxis);

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(temperature, 0, 250, TimeUnit.MILLISECONDS);

		stage.show();
	}

	protected void connectSuccessful() {
		Out.out.logln("Connected to \"" + SerialReader.in.getActivePort() + "\".");
		connect.setDisable(true);
		autodetect.setDisable(true);
		disconnect.setDisable(false);
		increase.setDisable(false);
		decrease.setDisable(false);
		temperature.start();
	}

	protected void disconnectSuccessful() {
		Out.out.logln("Disconnected from \"" + SerialReader.in.getActivePort() + "\".");
		disconnect.setDisable(true);
		connect.setDisable(false);
		autodetect.setDisable(false);
		increase.setDisable(true);
		decrease.setDisable(true);
		temperature.stop();
	}

	public boolean isReady() {
		return ready;
	}

	public void launch() {
		super.launch();
	}

	public void printlnErr(Object obj) {
		logBox.appendText("[WARNING] " + obj + "\n");
	}

	public void print(Object obj) {
		logBox.appendText(obj + "");
	}

	public void println() {
		logBox.appendText("\n");
	}

	public void println(Object obj) {
		logBox.appendText(obj + "\n");
	}

}
