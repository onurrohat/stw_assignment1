import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class XmlToCsvParser extends DefaultHandler {
    private List<String[]> itemCsvData = new ArrayList<>();  // Item verileri
    private List<String[]> sellerCsvData = new ArrayList<>(); // Seller verileri
    private List<String[]> bidderCsvData = new ArrayList<>(); // Bidder verileri
    private List<String[]> bidInfoCsvData = new ArrayList<>(); // BidInfo verileri
    private List<String[]> categoryCsvData = new ArrayList<>(); // Category verileri

    private String currentElement = "";
    private String itemID, name, category, currently, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, sellerRating, userID, description;
    private String bidderID, bidderRating, bidderLocation, bidderCountry, bidAmount, bidTime;

    public static void main(String args[]) throws Exception {
        // Create SAX parser instance
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XmlToCsvParser handler = new XmlToCsvParser();

        // Parse all the XML files provided on the command line
        for (String fileName : args) {
            System.out.println("Processing file: " + fileName);
            parser.parse(new InputSource(new FileReader(fileName)), handler);
        }

        // Write the collected data to separate CSV files
        handler.writeCSV("item.csv", handler.itemCsvData);
        handler.writeCSV("seller.csv", handler.sellerCsvData);
        handler.writeCSV("bidder.csv", handler.bidderCsvData);
        handler.writeCSV("bidinfo.csv", handler.bidInfoCsvData);
        handler.writeCSV("category.csv", handler.categoryCsvData);
    }

    // Constructor
    public XmlToCsvParser() {
        super();
    }

    // Write data to CSV
    private void writeCSV(String csvFilePath, List<String[]> data) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
            if (csvFilePath.equals("item.csv")) {
                writer.write("ItemID,Name,SellerID,Currently,Buy_price,First_bid,Location,Latitude,Longitude,Country,Started,Ends,Description\n");
            } else if (csvFilePath.equals("seller.csv")) {
                writer.write("UserID,Rating\n");
            } else if (csvFilePath.equals("bidder.csv")) {
                writer.write("BidderID,Rating,Location,Country\n");
            } else if (csvFilePath.equals("bidinfo.csv")) {
                writer.write("ItemID,BidderID,Time,Amount\n");
            } else if (csvFilePath.equals("category.csv")) {
                writer.write("Category,ItemID\n");
            }

            // Write all rows of data
            for (String[] row : data) {
                String[] escapedRow = Arrays.stream(row)
                    .map(this::escapeCsvValue)  // Escape CSV value
                    .toArray(String[]::new);
                writer.write(String.join(",", escapedRow));
                writer.write("\n");
            }
        }
    }

    // Escape CSV values
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        value = value.replace("\"", "\"\""); // Escape double quotes
        if (value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"")) {
            value = "\"" + value + "\"";
        }
        return value;
    }

    // Handle start of XML document
    @Override
    public void startDocument() {
        // Reset the data lists when starting a new document
        itemCsvData.clear();
        sellerCsvData.clear();
        bidderCsvData.clear();
        bidInfoCsvData.clear();
        categoryCsvData.clear();
    }

    // Handle end of XML document
    @Override
    public void endDocument() {}

    // Handle start of XML elements
    @Override
    public void startElement(String uri, String name, String qName, Attributes atts) {
        currentElement = qName;

        // Clear variables when encountering a new "Item" element
        if (qName.equals("Item")) {
            itemID = atts.getValue("ItemID");
            name = category = currently = firstBid = numberOfBids = location = latitude = longitude = country = started = ends = sellerRating = userID = description = "";
        }

        // Collect "Seller" data
        if (qName.equals("Seller")) {
            sellerRating = atts.getValue("Rating");
            userID = atts.getValue("UserID");
        }

        // Collect "Location" data
        if (qName.equals("Location")) {
            latitude = atts.getValue("Latitude");
            longitude = atts.getValue("Longitude");
        }

        // Handle Bidder data
        if (qName.equals("Bidder")) {
            bidderID = atts.getValue("UserID");
        }
    }

    // Handle end of XML elements
    @Override
    public void endElement(String uri, String name, String qName) {
        // When an "Item" element ends, store the data
        if (qName.equals("Item")) {
            itemCsvData.add(new String[]{
                itemID, name, userID, currently, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, description
            });

            // Handle Category data
            if (category != null && !category.isEmpty()) {
                String[] categories = category.split(";");
                for (String cat : categories) {
                    categoryCsvData.add(new String[]{cat, itemID});
                }
            }
        }

        // Add Seller data to the list
        if (qName.equals("Seller")) {
            sellerCsvData.add(new String[]{userID, sellerRating});
        }

        // Add Bidder data to the list
        if (qName.equals("Bidder")) {
            bidderCsvData.add(new String[]{bidderID, bidderRating, bidderLocation, bidderCountry});
        }

        // Add BidInfo data
        if (qName.equals("Bid")) {
            bidInfoCsvData.add(new String[]{itemID, bidderID, bidTime, bidAmount});
        }
    }

    // Handle characters (data) inside XML elements
    @Override
    public void characters(char ch[], int start, int length) {
        String content = new String(ch, start, length).trim();
        if (!content.isEmpty()) {
            switch (currentElement) {
                case "Name":
                    name = content;
                    break;
                case "Category":
                    category = category == null ? content : category + ";" + content;
                    break;
                case "Currently":
                    currently = strip(content);
                    break;
                case "First_Bid":
                    firstBid = strip(content);
                    break;
                case "Number_of_Bids":
                    numberOfBids = content;
                    break;
                case "Location":
                    location = content;
                    break;
                case "Country":
                    country = content;
                    break;
                case "Started":
                    started = content;
                    break;
                case "Ends":
                    ends = content;
                    break;
                case "Description":
                    description = content.length() > 4000 ? content.substring(0, 4000) : content;
                    break;
                case "Bidder":
                    bidderRating = content;
                    break;
                case "Bid":
                    bidAmount = content;
                    break;
                case "Time":
                    bidTime = content;
                    break;
            }
        }
    }

    // Helper method to strip currency symbols
    private static String strip(String money) {
        if (money.equals("")) return money;
        double amount = 0.0;
        try {
            amount = Double.parseDouble(money.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            System.err.println("Invalid money format: " + money);
        }
        return String.format("%.2f", amount);
    }
}
