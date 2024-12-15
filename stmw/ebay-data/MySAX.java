import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class MySAX extends DefaultHandler {
    private List<String[]> csvData = new ArrayList<>();
    private StringBuilder currentValue = new StringBuilder();
    private String currentElement = "";
    private String itemID, name, category, currently, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, sellerRating, userID, description;

    public static void main(String args[]) throws Exception {
        // Create SAX parser instance
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        MySAX handler = new MySAX();

        // Parse each file provided on the command line
        for (String fileName : args) {
            System.out.println("Processing file: " + fileName);
            parser.parse(new InputSource(new FileReader(fileName)), handler);
            handler.writeCSV(fileName.replace(".xml", ".csv"));
        }
    }

    public MySAX() {
        super();
    }

    // CSV'ye veri yazma
    private void writeCSV(String csvFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
            // CSV başlıkları
            writer.write("ItemID,Name,Category,Currently,First_Bid,Number_of_Bids,Location, Latitude, Longitude, Country,Started,Ends,Seller_Rating,UserID,Description\n");

            // CSV verileri
            for (String[] row : csvData) {
                // Her hücreyi escape et
                String[] escapedRow = Arrays.stream(row)
                    .map(this::escapeCsvValue)  // escape işlemi
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

    // CSV'ye yazma işlemi sırasında, değerlerin düzgün işlenmesi için escape işlemi
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // Her çift tırnağı iki çift tırnakla kaçırıyoruz
        value = value.replace("\"", "\"\"");

        // Eğer değer virgül, yeni satır, satır başı, veya çift tırnak içeriyorsa, değeri çift tırnakla sarıyoruz
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
        csvData.clear(); // Her yeni dosya için CSV verilerini sıfırlıyoruz
    }

    @Override
    public void endDocument() {
    }

    @Override
    public void startElement(String uri, String name, String qName, Attributes atts) {
        currentElement = qName;

        if ("".equals(uri)) {
            System.out.println("Start element: " + qName);
        } else {
            System.out.println("Start element: {" + uri + "}" + name);
        }

        // Etiketler için başlangıç işlemleri
        if (qName.equals("Item")) {
            // ItemID gibi özelliklere bu şekilde erişilebilir
            itemID = atts.getValue("ItemID");  // ItemID özelliğini alıyoruz
            this.name = category = currently = firstBid = numberOfBids = location = latitude = longitude = country = started = ends = sellerRating = userID = description = "";
        }

        if (qName.equals("Seller")) {
            // Seller elemanındaki Rating ve UserID özelliklerini alıyoruz
            sellerRating = atts.getValue("Rating");  // Rating özelliğini alıyoruz
            userID = atts.getValue("UserID");  // UserID özelliğini alıyoruz
        }

        if (qName.equals("Location")) {
            // Seller elemanındaki Rating ve UserID özelliklerini alıyoruz
            latitude = atts.getValue("Latitude");  // Rating özelliğini alıyoruz
            longitude = atts.getValue("Longitude");  // UserID özelliğini alıyoruz
        }
        // Etiketin altındaki özellikleri yazdırma
        for (int i = 0; i < atts.getLength(); i++) {
            System.out.println("Attribute: " + atts.getLocalName(i) + "=" + atts.getValue(i));
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if ("".equals(uri)) {
            System.out.println("End element: " + qName);
        } else {
            System.out.println("End element:   {" + uri + "}" + name);
        }

        // Etiket bittiğinde veri ekleme
        if (qName.equals("Item")) {
            System.out.println("Adding item to CSV data: " + itemID + ", " + this.name + ", " + category); // Debug log
            csvData.add(new String[]{
                itemID, this.name, category, currently, firstBid, numberOfBids, location, latitude, longitude, country, started, ends, sellerRating, userID, description
            });
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        // Etiket içeriğini yakalayıp gerekli değişkenlere atama
        String content = new String(ch, start, length).trim();
        if (!content.isEmpty()) {
            switch (currentElement) {
                case "Name":
                    name = content;
                    break;
                case "Category":
                    category = category == null ? content : category + ";" + content; // Birden fazla kategori varsa, birleştir
                    System.out.println(category);
                    break;
                case "Currently":
                    currently = strip(content);
                    break;
                case "Buy_Price":
                    buyPrice = strip(content);
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
                    // Eğer description 4000 karakterden fazla ise, ilk 4000 karakteri al
                    if (content.length() > 4000) {
                        description = content.substring(0, 4000);
                    } else {
                        description = content;
                    }
                    break;
            }
        }
    }
}
