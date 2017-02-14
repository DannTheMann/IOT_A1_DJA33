package kent.dja33.iot.a1;

import javafx.scene.chart.LineChart;
import kent.dja33.iot.a1.util.Out;
import kent.dja33.iot.a1.util.SerialReader;

public class TemperatureThread implements Runnable {

	private LineChart chart;
	private int refreshRate;

	public TemperatureThread(LineChart lc) {
		chart = lc;
	}

	@Override
	public void run() {

		try {

			String msg = SerialReader.in.popMessage();

			while (msg != null) {
				Out.out.logln("Read: " + msg);
				msg = SerialReader.in.popMessage();
			}

		} catch (Exception e) {

		}

	}

}
