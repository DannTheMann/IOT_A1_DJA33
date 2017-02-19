package kent.dja33.iot.a1;

import kent.dja33.iot.a1.util.Out;

public class Main {
	
	public static final SensorDisplay display = new SensorDisplay();
	
	public static void main(String[] args){
		
//		String test = "hello";
//		String te = test.substring(1, test.length());
//		Out.out.logln(te);
//		
//		Message msg = new Message("#D18.72");
//		
//		Out.out.logln("Name: " + msg.getName() + " | Payload: " + msg.getPayload() + " | time: " + msg.getTimeReceived());
		
		Out.out.logln("Starting application...");	
		
		display.launch();
		
	}

}
