package data.rest.client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HotCopperScreenScrape {

    private String url ="https://hotcopper.com.au/asx/{asxCode}/";
    private static String SEARCH_STRING = " Last: $";

    private String page;
    private String asxCode;

    public HotCopperScreenScrape(String asxCode) {
        asxCode = asxCode.toUpperCase();
        if (asxCode.contains(".au")) {
            asxCode = asxCode.substring(0,asxCode.indexOf(".au")-1);
        }
        System.out.println("The ASX CODE IS " + asxCode);
        page = this.doGetRequest(url.replace("{asxCode}",asxCode));
       // System.out.println(asxCode + " current price is $" + this.getCurrentPrice());
    }
    public double getCurrentPrice() {

        int index = page.indexOf(SEARCH_STRING);
        String lastString = page.substring(index,page.length()-1);
        return Double.valueOf(lastString.substring(SEARCH_STRING.length(),lastString.indexOf(">")-1)).doubleValue();
    }

    public String doGetRequest(String urlString) {

        // Sending get request
        try {
            URL url = new URL( urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            String response =  getResponseString(conn).toString();
            conn.disconnect();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static StringBuffer getResponseString(HttpURLConnection conn) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;
        StringBuffer response = new StringBuffer();
        while ((output = in.readLine()) != null) {
            response.append(output);
        }
        in.close();
        return response;

    }

}
