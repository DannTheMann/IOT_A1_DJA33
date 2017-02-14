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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
	private Stage stage;
	private Scene scene;
	private BorderPane root;
	private VBox buttonPanel;
	private Button connect;
	private Button disconnect;
	private Button autodetect;
	private LineChart<Number, Number> lineChart;
	private TemperatureThread temperature;
	private static TextArea log;
	private static boolean ready = false;

	public static final int MAX_WINDOW_WIDTH = 1920;
	public static final int MAX_WINDOW_HEIGHT = 1080;
	public static final int MIN_WINDOW_WIDTH = 720;
	public static final int MIN_WINDOW_HEIGHT = 480;

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		stage.setTitle(TITLE);

		root = new BorderPane();

		{

			buttonPanel = new VBox(10);

			{

				ObservableList<String> options = FXCollections.observableArrayList(new String[] { "NONE" });
				options.addAll(SerialReader.in.getActiveSerialPorts());

				ComboBox<String> serialPortSelection = new ComboBox<>(options);

				serialPortSelection.getSelectionModel().select(0);

				serialPortSelection.valueProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue,
							String newValue) {
						connect.setDisable(newValue == "NONE" || SerialReader.in.connected());
					}
				});

				Button button = new Button("Refresh");

				button.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						Out.out.logln("Refreshing available serial ports.");
						serialPortSelection.getItems().clear();
						serialPortSelection.getItems().add("NONE");
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
							Out.out.logln("Connected to \"" + serialPortSelection.getSelectionModel().getSelectedItem()
									+ "\".");
							connect.setDisable(true);
							autodetect.setDisable(true);
							disconnect.setDisable(false);
						}

					}

				});

				disconnect = new Button("Disconnect");
				disconnect.setDisable(true);

				disconnect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						if (SerialReader.in.closePort()) {
							Out.out.logln("Disconnected from \""
									+ serialPortSelection.getSelectionModel().getSelectedItem() + "\".");
							disconnect.setDisable(true);
							connect.setDisable(false);
							autodetect.setDisable(false);
						}

					}

				});

				buttonPanel.getChildren().add(connect);
				buttonPanel.getChildren().add(disconnect);

				autodetect = new Button("Auto-detect");

				autodetect.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						autodetect.setText(" Wait ... ");
						autodetect.setDisable(true);
						
						for (String com : SerialReader.in.getActiveSerialPorts()) {
							if (SerialReader.in.openPort(com)) {
								Out.out.logln("Automatically connected to \"" + com + "\".");
								serialPortSelection.getSelectionModel().select(getIndex(com));
								connect.setDisable(true);
								disconnect.setDisable(false);					
								autodetect.setText("Auto-detect");
								return;
							}
						}
						
						autodetect.setText("Auto-detect");
						autodetect.setDisable(false);

						serialPortSelection.getSelectionModel().select(0);
						Out.out.loglnErr("Could not automatically connect to any sensor devices.");

					}

					private int getIndex(String com) {
						for (int i = 1; i <= SerialReader.in.getActiveSerialPorts().length; i++) {
							if (com.equals(SerialReader.in.getActiveSerialPorts()[i - 1]))
								return i;
						}
						return 0;
					}

				});
				
				button = new Button("Clear log");

				button.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						log.setText("");
						Out.out.logln("Log cleared.");

					}

				});

				buttonPanel.getChildren().add(autodetect);
				buttonPanel.getChildren().add(button);
				buttonPanel.setPadding(new Insets(5, 5, 5, 5));

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
				log = new TextArea();
				log.setEditable(false);

				log.textProperty().addListener(new ChangeListener<Object>() {
					@Override
					public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
						log.setScrollTop(Double.MAX_VALUE); // this will scroll
															// to the bottom
						// use Double.MIN_VALUE to scroll to the top
					}
				});

				log.setMaxHeight(MAX_WINDOW_HEIGHT / 10);
				log.setMinHeight(MIN_WINDOW_HEIGHT / 10);
				log.setMaxWidth(MAX_WINDOW_WIDTH / 3);
				log.setMinWidth(MIN_WINDOW_WIDTH / 3);
			}

			sp.getChildren().add(log);
			sp.setPadding(new Insets(5, 5, 20, 5));

			sp.setAlignment(Pos.CENTER);
			root.setBottom(sp);
		}

		root.setRight(buttonPanel);

		// defining the axes
		final NumberAxis xAxis = new NumberAxis();
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Number of Month");
		// creating the chart
		lineChart = new LineChart<Number, Number>(xAxis, yAxis);

		lineChart.setTitle("Stock Monitoring, 2010");
		// defining a series
		XYChart.Series series = new XYChart.Series();
		series.setName("My portfolio");
		// populating the series with data
		series.getData().add(new XYChart.Data<Integer, Integer>(1, 23));
		series.getData().add(new XYChart.Data<Integer, Integer>(2, 14));
		series.getData().add(new XYChart.Data<Integer, Integer>(3, 15));
		series.getData().add(new XYChart.Data<Integer, Integer>(4, 24));
		series.getData().add(new XYChart.Data<Integer, Integer>(5, 34));
		series.getData().add(new XYChart.Data<Integer, Integer>(6, 36));
		series.getData().add(new XYChart.Data<Integer, Integer>(7, 22));
		series.getData().add(new XYChart.Data<Integer, Integer>(8, 45));
		series.getData().add(new XYChart.Data<Integer, Integer>(9, 43));
		series.getData().add(new XYChart.Data<Integer, Integer>(10, 17));
		series.getData().add(new XYChart.Data<Integer, Integer>(11, 29));
		series.getData().add(new XYChart.Data<Integer, Integer>(12, 25));

		root.setCenter(lineChart);

		Scene scene = new Scene(root, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

		lineChart.getData().add(series);

		stage.setScene(scene);

		/* Constraints on interface */
		stage.setMinHeight(MIN_WINDOW_HEIGHT);
		stage.setMinWidth(MIN_WINDOW_WIDTH);
		stage.setMaxHeight(MAX_WINDOW_HEIGHT);
		stage.setMaxWidth(MAX_WINDOW_WIDTH);

		ready = true;
		
		stage.setOnCloseRequest(e->{
			SerialReader.in.closePort();
			Out.close();
		});

		temperature = new TemperatureThread(lineChart);
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(temperature, 0, 1, TimeUnit.SECONDS);
		
		stage.show();
	}

	public boolean isReady() {
		return ready;
	}

	public void launch() {
		super.launch();
	}

	public void printlnErr(Object obj) {
		log.appendText("[WARNING] " + obj + "\n");
	}

	public void print(Object obj) {
		log.appendText(obj + "");
	}

	public void println() {
		log.appendText("\n");
	}

	public void println(Object obj) {
		log.appendText(obj + "\n");
	}

}
