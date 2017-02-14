package kent.dja33.iot.a1;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kent.dja33.iot.a1.util.Out;

public class Main {
	
	public static final SensorDisplay display = new SensorDisplay();
	
	public static void main(String[] args){
		
		Out.out.logln("Starting application...");	
		
		display.launch();
		
	}

}
