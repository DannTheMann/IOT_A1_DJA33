package kent.dja33.iot.a1;

import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.animation.AnimationTimer;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.ProgressBar;
import kent.dja33.iot.a1.util.SerialReader;
import kent.dja33.iot.a1.util.message.Message;
import kent.dja33.iot.a1.util.message.MessageHandler;

/**
 * This class is designed to handle parsing input from the serialReader by
 * popping off messages from its queue. These messages are then added to its own
 * queue which is used to update the Graphs with Sample objects.
 * 
 * Although the class has concurrent components it is also called within the FX
 * thread to assure that updating the graph does not interfere between threaded
 * objects.
 * 
 * 
 * @author Dante
 *
 */
public class SensorHandler implements Runnable {

	/* Title of the graph */
	private static final String TITLE = "Temperature Samples";

	/* Minimum and maximum scales allowed for Y boundary */
	private static final int YAXIS_MIN_BOUNDARY_SHIFT = 1;
	private static final int YAXIS_MAX_BOUNDARY_SHIFT = 10;

	/* Minimum and maximum display ranges for samples */
	private static final int MAX_DISPLAY = 100;
	private static final int MIN_DISPLAY = 10;

	/*
	 * Default starting points for display size and the value to increment by
	 */
	private static final int DISPLAY_SIZE_INCREMENTAL_VALUE = 10;
	private int displaySize = MAX_DISPLAY / 2;
	private int yAxisBoundaryShift = YAXIS_MAX_BOUNDARY_SHIFT / 2;

	/* Charts and axis that are relevant for updating */
	private final LineChart<String, Number> temperatureChart;
	private final ScatterChart<Number, Number> accelerometerChart;
	private final XYChart.Series<String, Number> temperatureChartSeries;
	private final XYChart.Series<Number, Number> accelerometerChartSeries;
	private final NumberAxis yAxis;
	private final ProgressBar accelerometerZForce;

	/* Concurrent queue for samples which are used for updating the chart */
	private ConcurrentLinkedQueue<Sample> dataQ = new ConcurrentLinkedQueue<Sample>();
	/* Are we measuring in Celsius or Fahrenheit, default is Celsius */
	private MeasurementType measurementType;

	/**
	 * Create the TemperatureHandler, pass it the chart to update and the axis
	 * to work with.
	 * 
	 * @param lineChart
	 *            The chart we wish to update
	 * @param xAxis
	 *            The xAxis of that chart
	 * @param yAxis
	 *            The yAxis of that chart
	 */
	public SensorHandler(LineChart<String, Number> lineChart, ScatterChart<Number, Number> scatterChart,
			NumberAxis yAxis, ProgressBar accelerometerZ) {
		this.temperatureChart = lineChart;
		/*
		 * Create a series of data points that we can there add and remove
		 * values from
		 */
		this.temperatureChartSeries = new XYChart.Series<String, Number>();
		this.temperatureChart.getData().add(temperatureChartSeries);
		this.yAxis = yAxis;
		this.measurementType = MeasurementType.CELSIUS;
		this.yAxis.setLabel("Temperature " + this.measurementType.getMeasurementSymbol());

		this.accelerometerChart = scatterChart;
		this.accelerometerChartSeries = new XYChart.Series<Number, Number>();
		this.accelerometerChartSeries.getData().add(new Data<Number, Number>(0, 0));
		this.accelerometerChart.getData().add(this.accelerometerChartSeries);

		this.accelerometerZForce = accelerometerZ;

	}

	/**
	 * Thread to handle adding messages to our queue, will try to read in all
	 * available messages it can find that are flagged as DATA and add them to
	 * the queue for processing later.
	 */
	@Override
	public void run() {

		try {

			Message msg = SerialReader.in.popMessage(MessageHandler.DATA);

			while (msg != null) {

				// If the message is not corrupted at all and is 100% good to go
				if (msg.getPayload() == "" || msg.getPayload().length() == 0 || msg.getName() == MessageHandler.ERR) {
					msg = SerialReader.in.popMessage(MessageHandler.DATA);
					continue;
				}

				dataQ.add(new Sample(msg));

				msg = SerialReader.in.popMessage(MessageHandler.DATA);

			}

		} catch (Exception e) {
			e.printStackTrace();
			// ... Stop thread
		}

	}

	public MeasurementType getMeasurementType() {
		return measurementType;
	}

	/**
	 * 
	 * @param string
	 */
	public void switchMeasurementType(MeasurementType type) {

		pause = true;

		measurementType = type;

		yAxis.setLabel("Temperature " + measurementType.getMeasurementSymbol());

		temperatureChartSeries.getData().forEach((val) -> {
			val.setYValue(MeasurementType.convert(type, val.getYValue().floatValue()));
		});

		dataQ.forEach((val) -> {
			val.setTemperatureSample(MeasurementType.convert(type, val.getTemperatureSample()));
		});

		pause = false;

	}

	/**
	 * Resize the chart based on the user scrolling in negative for scrolling
	 * in, positive for scrolling out
	 * 
	 * @param deltaX
	 *            The indent of the scroll factor,
	 */
	public void resizeChart(double deltaX) {

		// Scroll in
		if (deltaX < 0) {
			if (displaySize < MAX_DISPLAY) {
				displaySize += DISPLAY_SIZE_INCREMENTAL_VALUE;
				if (displaySize > MAX_DISPLAY) {
					displaySize = MAX_DISPLAY;
				}
			}
			if (yAxisBoundaryShift < YAXIS_MAX_BOUNDARY_SHIFT) {
				yAxisBoundaryShift += 1;
			}
			// Scroll out
		} else {
			if (displaySize > MIN_DISPLAY) {
				displaySize -= DISPLAY_SIZE_INCREMENTAL_VALUE;
				if (displaySize < MIN_DISPLAY) {
					displaySize = MIN_DISPLAY;
				}
			}

			if (yAxisBoundaryShift > YAXIS_MIN_BOUNDARY_SHIFT) {
				yAxisBoundaryShift -= 1;
			}
		}

		// Update our changes to the graph
		updateGraphHandler();

	}

	/*
	 * Average value stored here, used to work out where the limits on the Y
	 * range should be
	 */
	private int average;

	/*
	 * Whether the animation of the chart updating needs to be stopped
	 */
	private boolean stop;

	/*
	 * Pause updating the graph handler, useful for when needing to convert
	 * existing values to new temperature format
	 */
	private boolean pause;

	/**
	 * Update the chart being displayed, will try to update the current refresh
	 * rate reported by the MBED and display that as well. Will remove all
	 * elements that exceed the size of the displaySize that are 'old' and add
	 * all new elements it can. Finally updates the limits on the Y axis so that
	 * the chart does not shift too far or become to hard to read over varying
	 * values
	 * 
	 * @return true if the animation needs to stop
	 */
	public boolean updateGraphHandler() {

		// Setting message
		Message msg = SerialReader.in.popLatestMessage(MessageHandler.SETTING);

		if (msg != null) {

			String[] payloadSplit = msg.getPayload().split(":");

			if (payloadSplit.length > 1) {
				temperatureChart.setTitle(TITLE + " { Refresh rate: " + payloadSplit[1] + "ms }");
			}

		}
		/* Updating Temperature Chart */
		{

			if (dataQ.isEmpty() || pause) {
				return false;
			}

			for (int i = 0; i < displaySize; i++) {
				if (dataQ.isEmpty()) {
					break;
				}
				Sample s = dataQ.remove();
				temperatureChartSeries.getData()
						.add(new Data<String, Number>(s.getTimeStamp(), s.getTemperatureSample()));

				/* Update Accelerometer scatter graph */
				accelerometerChartSeries.getData().get(0).setXValue(s.getX());
				accelerometerChartSeries.getData().get(0).setYValue(s.getY());
				float z = (float) (1 - s.getZ() < 0 ? 0.1 : 1 - s.getZ());
				accelerometerZForce.setProgress(z);

			}

			if (temperatureChartSeries.getData().size() > displaySize) {
				temperatureChartSeries.getData().remove(0, temperatureChartSeries.getData().size() - displaySize);
			}

			average = 0;
			temperatureChartSeries.getData().forEach(e -> average += e.getYValue().intValue());
			average /= temperatureChartSeries.getData().size();

			yAxis.setLowerBound(average - yAxisBoundaryShift);
			yAxis.setUpperBound(average + yAxisBoundaryShift);

		}

		return stop;
	}

	/**
	 * Start the animationTimer, which in turns starts calling
	 * updateGraphHandler until its told to stop
	 */
	public void start() {
		stop = false;
		new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (updateGraphHandler()) {
					System.out.println("Stopping");
					stop();
				}
			}
		}.start();
	}

	/**
	 * Flag that the animation for the chart needs to stop
	 */
	public void stop() {
		stop = true;
	}

	/**
	 * Inner class used to store samples, effectively a wrapper component for
	 * the Message itself that stores a conversion of the payload to floats and
	 * then the timestamp it was given.
	 * 
	 * @author Dante
	 *
	 */
	private class Sample {

		private float tempSample;
		private float accelX;
		private float accelY;
		private float accelZ;
		private final String timeStamp;

		public Sample(Message msg) {

			String[] split = msg.getPayload().split(":");

			tempSample = Float.parseFloat(split[0]);
			accelX = Float.parseFloat(split[1]);
			accelY = Float.parseFloat(split[2]);
			accelZ = Float.parseFloat(split[3]);
			timeStamp = msg.getTimeReceived();
		}

		public float getX() {
			return accelX;
		}

		public float getY() {
			return accelY;
		}

		public float getZ() {
			return accelZ;
		}

		public float getTemperatureSample() {
			return tempSample;
		}

		/*
		 * Needed for handling conversion of fahrenheit to celsius and vice
		 * versa
		 */
		public void setTemperatureSample(float sample) {
			this.tempSample = sample;
		}

		public String getTimeStamp() {
			return timeStamp;
		}

	}
}
