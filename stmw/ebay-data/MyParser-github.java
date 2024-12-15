import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

class MyParser {
    
    static final String columnSeparator = ",";
    static DocumentBuilder builder;
    
    private static BufferedWriter itemFileWriter;
    private static BufferedWriter userFileWriter;
    private static BufferedWriter categoryFileWriter;
    private static BufferedWriter bidFileWriter;
    private static int bidID = 0;
    
    static class MyErrorHandler implements ErrorHandler {
        
        public void warning(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }
        
        public void error(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }
        
        public void fatalError(SAXParseException exception)
        throws SAXException {
            exception.printStackTrace();
            System.out.println("There should be no errors in the supplied XML files.");
            System.exit(3);
        }
        
    }
    
    static Element[] getElementsByTagNameNR(Element e, String tagName) {
        Vector<Element> elements = new Vector<>();
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName)) {
                elements.add((Element) child);
            }
            child = child.getNextSibling();
        }
        Element[] result = new Element[elements.size()];
        elements.copyInto(result);
        return result;
    }
    
    static Element getElementByTagNameNR(Element e, String tagName) {
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
                return (Element) child;
            child = child.getNextSibling();
        }
        return null;
    }
    
    static String getElementText(Element e) {
        if (e.getChildNodes().getLength() == 1) {
            Text elementText = (Text) e.getFirstChild();
            return elementText.getNodeValue();
        } else {
            return "";
        }
    }
    
    static String getElementTextByTagNameNR(Element e, String tagName) {
        Element elem = getElementByTagNameNR(e, tagName);
        if (elem != null)
            return getElementText(elem);
        else
            return "";
    }
    
    static String strip(String money) {
        if (money.equals(""))
            return money;
        else {
            double am = 0.0;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            try { am = nf.parse(money).doubleValue(); }
            catch (ParseException e) {
                System.out.println("This method should work for all money values you find in our data.");
                System.exit(20);
            }
            nf.setGroupingUsed(false);
            return nf.format(am).substring(1);
        }
    }
    
    static void processFile(File xmlFile) {
        Document doc = null;
        try {
            doc = builder.parse(xmlFile);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            System.exit(3);
        }
        
        System.out.println("Successfully parsed - " + xmlFile);
        
        Element[] items = getElementsByTagNameNR(doc.getDocumentElement(), "Item");
        
        try {
            for (int i = 0; i < items.length; i++) {
                parseItem(items[i]);
                parseUser(items[i]);
                parseCategories(items[i]);
                parseBid(items[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void parseItem(Element item) throws IOException {
        String itemID = item.getAttribute("ItemID");
        Element seller = getElementByTagNameNR(item, "Seller");
        String sellerID = seller.getAttribute("UserID");
        String name = getElementTextByTagNameNR(item, "Name");
        String buyPrice = strip(getElementTextByTagNameNR(item, "Buy_Price"));
        String firstBid = strip(getElementTextByTagNameNR(item, "First_Bid"));
        String curr = strip(getElementTextByTagNameNR(item, "Currently"));
        String itemStarted = getElementTextByTagNameNR(item, "Started");
        String itemEnds = getElementTextByTagNameNR(item, "Ends");
        String started = "" + timestamp(itemStarted);
        String ends = "" + timestamp(itemEnds);
        String desc = getElementTextByTagNameNR(item, "Description");
        if (desc.length() > 4000) desc = desc.substring(0, 4000);
        
        load(itemFileWriter, itemID, sellerID, name, buyPrice, firstBid, curr, started, ends, desc);
    }
    
    public static void parseUser(Element item) throws IOException {
        Element user = getElementByTagNameNR(item, "Seller");
        String userID = user.getAttribute("UserID");
        String rating = user.getAttribute("Rating");
        String location = getElementText(getElementByTagNameNR(item, "Location"));
        String country = getElementText(getElementByTagNameNR(item, "Country"));
        
        if (location == null) location = "";
        if (country == null) country = "";
        
        load(userFileWriter, userID, rating, location, country);
        
        Element[] bids = getElementsByTagNameNR(getElementByTagNameNR(item, "Bids"), "Bid");
        
        for (int i = 0; i < bids.length; i++) {
            Element bidder = getElementByTagNameNR(bids[i], "Bidder");
            String bidderID = bidder.getAttribute("UserID");
            String bidderRating = bidder.getAttribute("Rating");
            String bidderLocation = getElementTextByTagNameNR(bidder, "Location");
            String bidderCountry = getElementTextByTagNameNR(bidder, "Country");
            if (bidderLocation == null) bidderLocation = "";
            if (bidderCountry == null) bidderCountry = "";
            load(userFileWriter, bidderID, bidderRating, bidderLocation, bidderCountry);
        }
    }
    
    public static void parseCategories(Element item) throws IOException {
        String itemID = item.getAttribute("ItemID");
        Element[] categories = getElementsByTagNameNR(item, "Category");
        
        for (int i = 0; i < categories.length; i++) {
            String category = getElementText(categories[i]);
            load(categoryFileWriter, itemID, category);
        }
    }
    
    public static void parseBid(Element item) throws IOException {
        Element[] bids = getElementsByTagNameNR(getElementByTagNameNR(item, "Bids"), "Bid");
        String itemID = item.getAttribute("ItemID");
        
        for (int i = 0; i < bids.length; i++) {
            Element bidder = getElementByTagNameNR(bids[i], "Bidder");
            String userID = bidder.getAttribute("UserID");
            String bid_time = getElementTextByTagNameNR(bids[i], "Time");
            String time = "" + timestamp(bid_time);
            String amount = strip(getElementTextByTagNameNR(bids[i], "Amount"));
            load(bidFileWriter, "" + bidID++, userID, itemID, time, amount);
        }
    }
    
    private static String timestamp(String date) {
        SimpleDateFormat format_in = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
        SimpleDateFormat format_out = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuffer buffer = new StringBuffer();
        try {
            Date parsedDate = format_in.parse(date);
            return format_out.format(parsedDate);
        } catch (ParseException pe) {
            System.err.println("Parse error");
            return "Parse error";
        }
    }
    
    private static String formatRow(String[] input) {
        StringBuilder formatted_input = new StringBuilder();
        for (int i = 0; i < input.length - 1; i++) {
            formatted_input.append(input[i]).append(columnSeparator);
        }
        formatted_input.append(input[input.length - 1]);
        System.out.println(formatted_input.toString());
        return formatted_input.toString();
    }
    
    private static void load(BufferedWriter output, String... args) throws IOException {
        output.write(formatRow(args));
        output.newLine();
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java MyParser [file] [file] ...");
            System.exit(1);
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
        } catch (FactoryConfigurationError e) {
            System.out.println("unable to get a document builder factory");
            System.exit(2);
        } catch (ParserConfigurationException e) {
            System.out.println("parser was unable to be configured");
            System.exit(2);
        }
        
        try {
            itemFileWriter = new BufferedWriter(new FileWriter("item.csv", true));
            userFileWriter = new BufferedWriter(new FileWriter("user.csv", true));
            categoryFileWriter = new BufferedWriter(new FileWriter("category.csv", true));
            bidFileWriter = new BufferedWriter(new FileWriter("bid.csv", true));
            
            for (int i = 0; i < args.length; i++) {
                File currentFile = new File(args[i]);
                processFile(currentFile);
            }
            
            itemFileWriter.close();
            userFileWriter.close();
            categoryFileWriter.close();
            bidFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
