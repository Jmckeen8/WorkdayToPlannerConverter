package jsonToXml;

public class Converter {

	public static void main(String[] args) {
		Schedb schedb = new Schedb();
		
		jsonIN jsonIn = new jsonIN();
		jsonIn.readJSON(schedb);
		System.out.println("Done JSON in");
		
		xmlOUT xmlOut = new xmlOUT();
		xmlOut.exportXML(schedb);
		
		System.out.println("Done XML out");
		
		
	}

}
