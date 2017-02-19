package kent.dja33.iot.a1;

import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.animation.AnimationTimer;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import kent.dja33.iot.a1.util.Message;
import kent.dja33.iot.a1.util.SerialReader;

public class TemperatureHandler implements Runnable {

	private static final String TITLE = "Temperature Samples";
	private static final int YAXIS_MIN_BOUNDARY_SHIFT = 1;
	private static final int YAXIS_MAX_BOUNDARY_SHIFT = 25;
	private static final int MAX_DISPLAY = 250;
	private static final int MIN_DISPLAY = 10;
	private static final int DISPLAY_SIZE_INCREMENTAL_VALUE = 10;
	private int displaySize = MAX_DISPLAY / 2;
	private int yAxisBoundaryShift = YAXIS_MAX_BOUNDARY_SHIFT / 2;

	private final LineChart<String, Number> chart;
	private final XYChart.Series<String, Number> series;
	private final NumberAxis yAxis;

	private ConcurrentLinkedQueue<Sample> dataQ = new ConcurrentLinkedQueue<Sample>();

	public TemperatureHandler(LineChart<String, Number> lineChart, CategoryAxis xAxis, NumberAxis yAxis) {
		this.chart = lineChart;
		this.series = new XYChart.Series<String, Number>();
		this.chart.getData().add(series);
		this.yAxis = yAxis;
	}

	@Override
	public void run() {

		try {

			Message msg = SerialReader.in.popMessage(Message.DATA);

			while (msg != null) {

				if (msg.getPayload() == "" || msg.getPayload().length() == 0 || msg.getName() == Message.ERR) {
					msg = SerialReader.in.popMessage(Message.DATA);
					continue;
				}

				dataQ.add(new Sample(msg));

				msg = SerialReader.in.popMessage(Message.DATA);

			}

		} catch (Exception e) {
			e.printStackTrace();
			// ... Stop thread
		}

	}

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

		updateGraphHandler();

	}

	private int average;
	private boolean stop;

	public boolean updateGraphHandler() {

		// Setting message
		Message msg = SerialReader.in.popLatestMessage(Message.SETTING);

		if (msg != null) {

			String[] payloadSplit = msg.getPayload().split(":");

			if (payloadSplit.length > 1) {
				chart.setTitle(TITLE + " { Refresh rate: " + payloadSplit[1] + "ms }");
			}

		}
		
		if (dataQ.isEmpty()) {
			return false;
		}

		for (int i = 0; i < displaySize; i++) {
			if (dataQ.isEmpty()) {
				break;
			}
			Sample s = dataQ.remove();
			series.getData().add(new Data<String, Number>(s.getTimeStamp(), s.getSample()));
		}

		if (series.getData().size() > displaySize) {
			series.getData().remove(0, series.getData().size() - displaySize);
		}

		average = 0;
		series.getData().forEach(e -> average += e.getYValue().intValue());
		average /= series.getData().size();

		yAxis.setLowerBound(average - yAxisBoundaryShift);
		yAxis.setUpperBound(average + yAxisBoundaryShift);
		
		return stop;
	}

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

	public void stop() {
		stop = true;
	}

	public int getSampleSize() {
		return dataQ.size();
	}

	public boolean isEmpty() {
		return getSampleSize() == 0;
	}

	private class Sample {

		private double sample;
		private String timeStamp;

		public Sample(Message msg) {
			sample = Double.parseDouble(msg.getPayload());
			timeStamp = msg.getTimeReceived();
		}

		public double getSample() {
			return sample;
		}

		public String getTimeStamp() {
			return timeStamp;
		}

	}

}
