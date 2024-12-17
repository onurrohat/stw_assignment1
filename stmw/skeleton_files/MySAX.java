import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class MySAX extends DefaultHandler {
    private Set<String> itemCsvData = new HashSet<>();
    private Set<String> sellerCsvData = new HashSet<>();
    private Set<String> bidderCsvData = new HashSet<>();
    private Set<String> bidInfoCsvData = new HashSet<>();
    private Set<String> categoryCsvData = new HashSet<>();
    private StringBuilder currentValue = new StringBuilder();
    private String currentElement = "";
    private String itemID, name, category, currently, buyPrice, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, sellerRating, userID, description, bidderID, bidderRating, bidderLocation, bidderCountry, bidTime, bidAmount;
    private boolean bidderLoc = false;
    private List<String> categories = new ArrayList<>(); 

    public static void main(String[] args) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        MySAX handler = new MySAX();

        deleteFileIfExists("seller.csv");
        deleteFileIfExists("item.csv");
        deleteFileIfExists("category.csv");
        deleteFileIfExists("bidder.csv");
        deleteFileIfExists("bidinfo.csv");

        // Process XML Files
        for (String fileName : args) {
            parser.parse(new InputSource(new FileReader(fileName)), handler);
            System.out.println("Processing: " + fileName);
        }

        // Write files at the end
        handler.writeCSV("item.csv", handler.itemCsvData);
        handler.writeCSV("seller.csv", handler.sellerCsvData);
        handler.writeCSV("bidder.csv", handler.bidderCsvData);
        handler.writeCSV("bidinfo.csv", handler.bidInfoCsvData);
        handler.writeCSV("category.csv", handler.categoryCsvData);
    }

    private static void deleteFileIfExists(String fileName) {
        File file = new File(fileName);
        if (file.exists()) { // Check csv files already exists
            if (file.delete()) {
                System.out.println(fileName + " deleted.");
            } else {
                System.err.println(fileName + " can't deleted!");
            }
        }
    }

    public MySAX() {
        super();
    }

    public void writeCSV(String csvFilePath, Set<String> data) throws IOException {
        File file = new File(csvFilePath);
        boolean fileExists = file.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath, true))) {
            if (!fileExists) { // Write column titles just at the beginning
                if (csvFilePath.equals("item.csv")) {
                    writer.write("ItemID,Name,SellerID,Currently,Buy_Price,First_bid,Location,Latitude,Longitude,Country,Started,Ends,Description\n");
                } else if (csvFilePath.equals("seller.csv")) {
                    writer.write("UserID,Rating\n");
                } else if (csvFilePath.equals("bidder.csv")) {
                    writer.write("BidderID,Rating,Location,Country\n");
                } else if (csvFilePath.equals("bidinfo.csv")) {
                    writer.write("ItemID,BidderID,Time,Amount\n");
                } else if (csvFilePath.equals("category.csv")) {
                    writer.write("Category,ItemID\n");
                }
            }

            // Write Data
            for (String row : data) {
                writer.write(row);
                writer.write("\n");
            }
        }
    }


    static String strip(String money) {
        if (money.equals("")) return money;
        else {
            double amount = 0.0;
            try {
                amount = Double.parseDouble(money.replaceAll("[^\\d.]", ""));
            } catch (NumberFormatException e) {
                System.err.println("Invalid money format: " + money);
            }
            return String.format("%.2f", amount);
        }
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        value = value.replace("\"", "\"\"");
        if (value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"")) {
            value = "\"" + value + "\"";
        }
        return value;
    }

    // Since SAXParser may accept special characters not correct, fix it
    private String decodeXmlSpecialCharacters(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'");
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {
    }

    @Override
    public void startElement(String uri, String name, String qName, Attributes atts) {
        currentElement = qName;

        if (qName.equals("Location")) {
            latitude = atts.getValue("Latitude");
            longitude = atts.getValue("Longitude");
        }
        if (qName.equals("Item")) {
            itemID = atts.getValue("ItemID");
            this.name = category = currently = buyPrice = firstBid = numberOfBids = location = latitude = longitude = country = started = ends = sellerRating = userID = description = "";
            categories.clear();
        }

        if (qName.equals("Seller")) {
            sellerRating = atts.getValue("Rating");
            userID = atts.getValue("UserID");
        }

        if (qName.equals("Bidder")) {
            bidderLoc = true;
            bidderLocation = ""; 
            bidderCountry = "";
            bidderID = atts.getValue("UserID");
            bidderRating = atts.getValue("Rating");
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        List<String> tempList = Arrays.asList("&", "'", "<", ">", "\"");
        Set<String> specialValues = new HashSet<>(tempList);

        if (qName.equals("Item")) {
            List<String> result = new ArrayList<>();
            // TODO: This is not a pro solution, maybe I should fix it later 
            for (int i = 0; i < categories.size(); i++) {
                if (specialValues.contains(categories.get(i))) {
                    // Merge categories before and after special chars
                    if (i > 0 && i < categories.size() - 1) {
                        String combined = categories.get(i - 1) + categories.get(i) + categories.get(i + 1);
                        result.set(result.size() - 1, combined); 
                        i++; 
                    }
                } else {
                    result.add(categories.get(i));
                }
            }
            

            for (String category : result) {
                String row = String.join(",", escapeCsvValue(category), escapeCsvValue(itemID));
                categoryCsvData.add(row);
            }

            String row = String.join(",", escapeCsvValue(itemID), escapeCsvValue(this.name), escapeCsvValue(userID), escapeCsvValue(currently),
                    escapeCsvValue(buyPrice), escapeCsvValue(firstBid), escapeCsvValue(location), escapeCsvValue(latitude), escapeCsvValue(longitude),
                    escapeCsvValue(country), escapeCsvValue(started), escapeCsvValue(ends), escapeCsvValue(description));
            itemCsvData.add(row);
        }

        if (qName.equals("Seller")) {
            int num = Integer.parseInt(sellerRating);
            String row = String.join(",", escapeCsvValue(userID), escapeCsvValue(sellerRating));
            sellerCsvData.add(row);
        }
        
        if (qName.equals("Bidder")) {
            bidderLoc = false;
            String row = String.join(",", escapeCsvValue(bidderID), escapeCsvValue(bidderRating), escapeCsvValue(bidderLocation), escapeCsvValue(bidderCountry));
            bidderCsvData.add(row);
        }
        
        if (qName.equals("Bid")) {
            String row = String.join(",", escapeCsvValue(itemID), escapeCsvValue(bidderID), escapeCsvValue(bidTime), escapeCsvValue(bidAmount));
            bidInfoCsvData.add(row);
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        String content = new String(ch, start, length).trim();
        if (!content.isEmpty()) {
            switch (currentElement) {
                case "Category":
                    categories.add(content.trim());
                    break;
                case "Name":
                    name = content.trim();
                    break;
                case "Currently":
                    currently = strip(content.trim());
                    break;
                case "First_Bid":
                    firstBid = strip(content.trim());
                    break;
                case "Buy_Price":
                    buyPrice = strip(content.trim());
                    break;
                case "Number_of_Bids":
                    numberOfBids = content.trim();
                    break;
                case "Location":
                    if (bidderLoc) {
                        bidderLocation = content.trim();
                    } else {
                        location = content.trim();
                    }
                    break;
                case "Country":
                    if (bidderLoc) {
                        bidderCountry = content.trim();
                    } else {
                        country = content.trim();
                    }
                    break;
                case "Started":
                    started = content.trim();
                    break;
                case "Ends":
                    ends = content.trim();
                    break;
                case "Description":
                    description = content.length() > 4000 ? content.substring(0, 4000) : content.trim();;
                    break;
                case "Time":
                    bidTime = content.trim();
                    break;
                case "Amount":
                    bidAmount = strip(content.trim());
                    break;
            }
        }
    }
}
