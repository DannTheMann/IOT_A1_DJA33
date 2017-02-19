package kent.dja33.iot.a1;

public enum MeasurementType {

	CELSIUS("°C"), FAHRENHEIT("°F");
	
	private String symbol;
	
	private MeasurementType(String sym) {
		this.symbol = sym;
	}
	
	String getMeasurementSymbol(){
		return this.symbol;
	}

	public static float convert(MeasurementType mt, float val) {
		float newVal = 0f;
		if (mt == MeasurementType.FAHRENHEIT) {
			newVal = (float) ((val * 1.8) + 32);
		} else if (mt == MeasurementType.CELSIUS) { // Assume Celsius
			newVal = (float) ((val - 32) / 1.8);
		}
		return newVal;

	}
}
