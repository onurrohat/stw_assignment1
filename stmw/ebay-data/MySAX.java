import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class MySAX extends DefaultHandler {
    private List<String[]> itemCsvData = new ArrayList<>();
    private List<String[]> sellerCsvData = new ArrayList<>();
    private List<String[]> bidderCsvData = new ArrayList<>();
    private List<String[]> bidInfoCsvData = new ArrayList<>();
    private List<String[]> categoryCsvData = new ArrayList<>();
    private StringBuilder currentValue = new StringBuilder();
    private String currentElement = "";
    private String itemID, name, category, currently, buyPrice, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, sellerRating, userID, description, bidderID, bidderRating, bidderLocation, bidderCountry, bidTime, bidAmount;
    private boolean bidderLoc = false;
    public static void main(String args[]) throws Exception {
        // Create SAX parser instance
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        MySAX handler = new MySAX();

        // Parse each file provided on the command line
        for (String fileName : args) {
            System.out.println("Processing file: " + fileName);
            parser.parse(new InputSource(new FileReader(fileName)), handler);
            handler.writeCSV("item.csv", handler.itemCsvData);
            handler.writeCSV("seller.csv", handler.sellerCsvData);
            handler.writeCSV("bidder.csv", handler.bidderCsvData);
            handler.writeCSV("bidinfo.csv", handler.bidInfoCsvData);
            handler.writeCSV("category.csv", handler.categoryCsvData);
        }
    }

    public MySAX() {
        super();
    }

    // CSV'ye veri yazma
    private void writeCSV(String csvFilePath, List<String[]> data) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
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

            // Verileri CSV formatında yazma
            for (String[] row : data) {
                String[] escapedRow = Arrays.stream(row)
                    .map(this::escapeCsvValue)  // Escape işlemi
                    .toArray(String[]::new);
                writer.write(String.join(",", escapedRow));
                writer.write("\n");
            }
        }
    }

    // Para formatını düzeltme (örneğin, "$3,453.23" -> "3453.23")
    static String strip(String money) {
        if (money.equals(""))
            return money;
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

    // CSV'ye yazma işlemi sırasında, değerlerin düzgün işlenmesi için escape işlemi
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

    ////////////////////////////////////////////////////////////////////
    // Event handlers.
    ////////////////////////////////////////////////////////////////////

    @Override
    public void startDocument() {
        // Her yeni dosya için CSV verilerini sıfırlıyoruz
        itemCsvData.clear();
        sellerCsvData.clear();
        bidderCsvData.clear();
        bidInfoCsvData.clear();
        categoryCsvData.clear();
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
        // Etiketler için başlangıç işlemleri
        if (qName.equals("Item")) {
            itemID = atts.getValue("ItemID");
            this.name = category = currently = buyPrice = firstBid = numberOfBids = location = latitude = longitude = country = started = ends = sellerRating = userID = description = "";
        }

        if (qName.equals("Seller")) {
            sellerRating = atts.getValue("Rating");
            userID = atts.getValue("UserID");
        }

        if (qName.equals("Bidder")) {
            bidderLoc = true;
            bidderLocation = ""; // Yeni bidder için sıfırla
            bidderCountry = "";
            bidderID = atts.getValue("UserID");
            bidderRating = atts.getValue("Rating");
        }

        // Etiketin altındaki özellikleri yazdırma
        for (int i = 0; i < atts.getLength(); i++) {
            System.out.println("Attribute: " + atts.getLocalName(i) + "=" + atts.getValue(i));
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        // Etiket bittiğinde veri ekleme
        System.out.println(this.name);
        if (qName.equals("Item")) {
            // Item bilgilerini CSV'ye ekliyoruz, latitude ve longitude bilgilerini de ekliyoruz
            itemCsvData.add(new String[] {
                itemID, this.name, userID, currently, buyPrice, firstBid, location, latitude, longitude, country, started, ends, description
            });
            System.out.println("Item Added: " + Arrays.toString(new String[]{
             location, latitude, longitude, country
                }));;
        }

        if (qName.equals("Seller")) {
            // Seller bilgilerini CSV'ye ekliyoruz
            sellerCsvData.add(new String[]{userID, sellerRating});
        }

        if (qName.equals("Bidder")) {
            // Bidder bilgilerini CSV'ye ekliyoruz
            bidderLoc = false;
            bidderCsvData.add(new String[]{bidderID, bidderRating, bidderLocation, bidderCountry});
        }

        if (qName.equals("Category")) {
            // Category bilgilerini CSV'ye ekliyoruz
            String[] categories = category.split(";");
            for (String cat : categories) {
                categoryCsvData.add(new String[]{cat, itemID});
            }
        }
        if (qName.equals("Bid")) {
            // BidInfo bilgilerini CSV'ye ekliyoruz
            bidInfoCsvData.add(new String[]{itemID, bidderID, bidTime, bidAmount});
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        // Etiket içeriğini yakalayıp gerekli değişkenlere atama
        String content = new String(ch, start, length).trim();
        if (!content.isEmpty()) {
            switch (currentElement) {
                case "Name":
                    name = content; // İsim değeri alındığında, name değişkenine atanacak
                    System.out.println("NAME:"+ name);
                    break;
                case "Category":
                    category = category == null ? content : category + ";" + content; // Birden fazla kategori varsa, birleştir
                    break;
                case "Currently":
                    currently = strip(content);
                    break;
                case "First_Bid":
                    firstBid = strip(content);
                    break;
                case "Buy_Price":
                    buyPrice = strip(content);
                    break;
                case "Number_of_Bids":
                    numberOfBids = content;
                    break;
                case "Location":
                    if (bidderLoc) {
                        bidderLocation = content;  // Bidder'ın içindeki Location
                    } else {
                        location = content;  // Item için Location
                    }
                    break;
                case "Latitude":
                    latitude = content;
                    break;
                case "Longitude":
                    longitude = content;
                    break;
                case "Country":
                    if (bidderLoc) {
                        bidderCountry = content;  // Bidder'ın içindeki Country
                    } else {
                        country = content;  // Item için Country
                    }
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
                case "Time":
                    bidTime = content;
                    break;
                case "Amount":
                    bidAmount = strip(content);
                    break;
            }
        }
    }
}
