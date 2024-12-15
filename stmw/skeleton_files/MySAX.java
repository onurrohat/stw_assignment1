/* Parser skeleton for processing item-???.xml files. Must be compiled in
 * JDK 1.5 or above.
 *
 * Instructions:
 *
 * This program processes all files passed on the command line (to parse
 * an entire diectory, type "java MyParser myFiles/*.xml" at the shell).
 *
 */

 import java.io.FileReader;
 import java.text.*;
 import java.util.*;
 import javax.xml.parsers.SAXParser;
 import javax.xml.parsers.SAXParserFactory;
 import org.xml.sax.*;
 import org.xml.sax.helpers.DefaultHandler;


public class MySAX extends DefaultHandler {
	public static void main(String args[]) throws Exception {
		// Create SAX parser instance
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		MySAX handler = new MySAX();

		// Parse each file provided on the command line
		for (String fileName : args) {
			System.out.println("Processing file: " + fileName);
			parser.parse(new InputSource(new FileReader(fileName)), handler);
		}
	}

	public MySAX() {
		super();
	}

	/*
	 * Returns the amount (in XXXXX.xx format) denoted by a money-string
	 * like $3,453.23. Returns the input if the input is an empty string.
	 */
    static String strip(String money) {
        if (money.equals(""))
            return money;
        else {
            double amount = 0.0;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            try {
                amount = nf.parse(money).doubleValue();
            } catch (ParseException e) {
                System.err.println("Invalid money format: " + money);
                System.exit(20);
            }
            nf.setGroupingUsed(false);
            return nf.format(amount).substring(1);
        }
    }

	////////////////////////////////////////////////////////////////////
	// Event handlers.
	////////////////////////////////////////////////////////////////////

	@Override
	public void startDocument() {
		System.out.println("Start document");
		// TODO
	}

	@Override
	public void endDocument() {
		System.out.println("End document");
		// TODO
	}

	@Override
	public void startElement(String uri, String name, String qName, Attributes atts) {
		if ("".equals(uri))
			System.out.println("Start element: " + qName);
		else
			System.out.println("Start element: {" + uri + "}" + name);
		for (int i = 0; i < atts.getLength(); i++) {
			System.out.println("Attribute: " + atts.getLocalName(i) + "=" + atts.getValue(i));
		}
		// TODO
	}

	@Override
	public void endElement(String uri, String name, String qName) {
		if ("".equals(uri))
			System.out.println("End element: " + qName);
		else
			System.out.println("End element:   {" + uri + "}" + name);

		// TODO
	}

	@Override
	public void characters(char ch[], int start, int length) {
		System.out.print("Characters:    \"");
		for (int i = start; i < start + length; i++) {
			switch (ch[i]) {
				case '\\':
					System.out.print("\\\\");
					break;
				case '"':
					System.out.print("\\\"");
					break;
				case '\n':
					System.out.print("\\n");
					break;
				case '\r':
					System.out.print("\\r");
					break;
				case '\t':
					System.out.print("\\t");
					break;
				default:
					System.out.print(ch[i]);
					break;
			}
		}
		System.out.print("\"\n");
	}

	// TODO

}
