package data.rest.client;

import file.utilities.DataFileUtilities;
import file.utilities.WorkingDirectory;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EODHDRestClient extends RestClient {
    private static final Logger logger = Logger.getLogger("EODHDRestClient-logger");

    private String bearerToken;
    private String environment;
    private String uRLPrefix;
    private static String DIRECTORY =  new WorkingDirectory().getWorkingDirectory();

    String token = "64293ba5706115.55872934";
    String url = "https://eodhd.com/api";

    private static String BEARER_TOKEN = "64293ba5706115.55872934";

    public String getEnvironment() {
        return environment;
    }
    public String getURLPrefix() {
        return this.uRLPrefix;
    }
    public void setURLPrefix(String urlPrefix) {
        uRLPrefix = urlPrefix;
    }

    public EODHDRestClient() {
        super(BEARER_TOKEN,"Production");
    }

    public String getHistoricalPrices(String companyCode) throws Exception {
        String fileName = DIRECTORY + "Eod-" + companyCode + ".json";
        return new DataFileUtilities().readFile(fileName);
    }

    public boolean getCompanyDataDump(String companyCode) throws Exception {
        try {
            return this.writeHistoricalPrices(companyCode) && this.writeFinancialData(companyCode);
        } catch (Exception e) {
            throw e;
            //return false;
        }
    }

    private boolean writeHistoricalPrices(String companyCode) throws Exception {
        String fileName = DIRECTORY + "Eod-" + companyCode + ".json";
        URL historicalSharePrices = new URL(this.url + "/eod/" + companyCode + "?fmt=json&api_token=" + BEARER_TOKEN);
        HttpURLConnection historicalSharePriceCon = (HttpURLConnection) historicalSharePrices.openConnection();
        try {
            String response =  RestClient.getResponseString(historicalSharePriceCon).toString();
            new DataFileUtilities().writeFile(fileName, response);
            return true;
        } catch (Exception e) {
            throw e;
//            logger.log(Level.SEVERE,e.getMessage(),e);
//            return false;
        }
    }

    private boolean writeFinancialData(String companyCode) throws Exception {
        String fileName = DIRECTORY + "Fundamentals-" + companyCode + ".json";
        String response;
        URL url = new URL(this.url + "/fundamentals/" + companyCode+"?api_token=" + BEARER_TOKEN);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            response = EODHDRestClient.getResponseString(conn).toString();
        } catch (Exception e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
            return false;
        }
        new DataFileUtilities().writeFile(fileName, response);
        return true;

    }
    public JSONObject setFinancialData(String companyCode) throws Exception {
        String fileName = DIRECTORY + "Fundamentals-" + companyCode + ".json";
        String response;
        response = new DataFileUtilities().readFile(fileName);
        if (response != null) {
            return new JSONObject(response);
        }
        return new JSONObject();
    }

    public String getToken() {
        return this.bearerToken;
    }
    public void setToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public boolean isTokenExist() {
        if (this.bearerToken == null) {
            return false;
        }
        return true;
    }
    public void setEnvironment(String environment) {
        this.environment = environment;
    }


    public String doGetRequest(String urlString) {

        // Sending get request
        try {
            URL url = new URL(getURLPrefix() + urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (this.bearerToken!=null) {
                conn = setTokenAuthorization(conn, this.bearerToken);
            }
            conn.setRequestProperty("Content-Type","application/json");
            conn.setRequestMethod("GET");

            String response =  getResponseString(conn).toString();
            //System.out.println("The response is: " + response);
            conn.disconnect();
            return response;

        } catch (Exception e) {
            logger.log(Level.SEVERE,e.getMessage(),e + " " + this.getToken());
        }
        return null;
    }

    public String doPostRequest(String urlString, JSONObject dataObject) {
        try {
            URL url = new URL(getURLPrefix() + urlString);

            System.out.println("curl -X POST " + getURLPrefix() + urlString + " -H \"accept: */* \" -H \"Authorization: " + this.getToken() +"\" -H \"Content-Type: application/json\" -d \"" + dataObject.toString().replace("\"", "\\\"") + "\"");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn = setTokenAuthorization(conn, this.bearerToken);
            conn.setRequestMethod("POST");

            byte[] out = dataObject.toString().getBytes(StandardCharsets.UTF_8);

            OutputStream stream = conn.getOutputStream();
            stream.write(out);

            StringBuffer response = getResponseString(conn);
            conn.disconnect();
            return response.toString();
        } catch (Exception e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
            System.out.println(e.getMessage());
        }
        return null;
    }

    public String doPutRequest(String urlString, JSONObject dataObject) {
        return doPutRequest(urlString, dataObject.toString());

    }


    public String doPutRequest(String urlString, String dataString) {
        try {
            URL url = new URL(getURLPrefix() + urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            if (this.bearerToken!=null) {
                conn = setTokenAuthorization(conn, this.bearerToken);
            }
            conn.setRequestMethod("PUT");
            if (dataString != null) {
                byte[] out = dataString.getBytes(StandardCharsets.UTF_8);
                OutputStream stream = conn.getOutputStream();
                stream.write(out);
            }
            //System.out.println(urlString + " " + dataString);

            StringBuffer response = getResponseString(conn);

            conn.disconnect();
            return response.toString();
        } catch (Exception e) {
            System.out.println("Exception: " + urlString + " " + e.getMessage());
        }
        return null;
    }

    private static HttpURLConnection setTokenAuthorization(HttpURLConnection conn, String token) {

        if (token == null || token.length()==0) {
            return conn;
        }
        if (token.indexOf("Bearer ")==0 || token.indexOf("bearer ") ==0) {
            conn.setRequestProperty("Authorization", token);
            return conn;
        }

        conn.setRequestProperty("Authorization","Bearer "+ token);
        return conn;
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
