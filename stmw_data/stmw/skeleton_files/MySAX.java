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
    Integer sayi = 0;
    private StringBuilder currentValue = new StringBuilder();
    private String currentElement = "";
    private String itemID, name, category, currently, buyPrice, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, sellerRating, userID, description, bidderID, bidderRating, bidderLocation, bidderCountry, bidTime, bidAmount;
    private boolean bidderLoc = false;
    private List<String> categories = new ArrayList<>(); // Kategoriler için liste ekledim
    private boolean firstItem = false; // İlk dosya kontrolü için bayrak

    public static void main(String[] args) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        MySAX handler = new MySAX();

        for (String fileName : args) {
            parser.parse(new InputSource(new FileReader(fileName)), handler);
            System.out.println(fileName);
            handler.writeCSV("item.csv", handler.itemCsvData, true); // Başlık eklemek için true
            handler.writeCSV("seller.csv", handler.sellerCsvData, true); // Başlık eklemek için true
            handler.writeCSV("bidder.csv", handler.bidderCsvData, true); // Başlık eklemek için true
            handler.writeCSV("bidinfo.csv", handler.bidInfoCsvData, true); // Başlık eklemek için true
            handler.writeCSV("category.csv", handler.categoryCsvData, true); // Başlık eklemek için true
        }
    }

    public MySAX() {
        super();
    }

    private void writeCSV(String csvFilePath, Set<String> data, boolean isFirstFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath, true))) { // 'true' parametresi ekledik ki veriler eklenerek yazılsın
            if (isFirstFile) { // İlk dosya ise başlıkları yaz
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

    // XML'deki özel karakterleri çözümleme (örneğin, &amp; -> &)
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
        itemCsvData.clear();
        sellerCsvData.clear();
        bidderCsvData.clear();
        bidInfoCsvData.clear();
        categoryCsvData.clear();
        firstItem = true; // Her yeni dosya için başlık yazılacak
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
            categories.clear(); // Her item için kategorileri temizle
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
            // Kategorileri yazarken her kategori için ItemID ekle
            List<String> result = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                if (specialValues.contains(categories.get(i))) {
                    // Önceki ve sonraki elemanları birleştir
                    if (i > 0 && i < categories.size() - 1) {
                        String combined = categories.get(i - 1) + categories.get(i) + categories.get(i + 1);
                        result.set(result.size() - 1, combined); // Son eklenen elemanı güncelle
                        i++; // Bir sonraki eleman atlanır (zaten birleştirildi)
                    }
                } else {
                    // Normal elemanları ekle
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
            System.out.println("BUY PRICE");
            sayi +=1;
            System.out.println(sayi);
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
                    // Kategoriyi decode edip listeye ekle
                    categories.add(content);
                    break;
                case "Name":
                    name = content;
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
                        bidderLocation = content;
                    } else {
                        location = content;
                    }
                    break;
                case "Country":
                    if (bidderLoc) {
                        bidderCountry = content;
                    } else {
                        country = content;
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
