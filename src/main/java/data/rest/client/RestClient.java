package data.rest.client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RestClient {

    private String bearerToken;
    private String environment;
    private String uRLPrefix;

    public String getEnvironment() {
        return environment;
    }
    public String getURLPrefix() {
        return this.uRLPrefix;
    }
    public void setURLPrefix(String urlPrefix) {
        uRLPrefix = urlPrefix;
    }

    public RestClient(String bearerToken) {
        this(bearerToken,"Staging");
    }

    public RestClient(String bearerToken, String environment) {
        this.bearerToken = bearerToken;
        this.environment = environment;
    }
    public RestClient(String bearerToken, String environment, String urlPrefix) {

        this.bearerToken = bearerToken;
        this.environment = environment;
        this.uRLPrefix = urlPrefix;
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
            e.printStackTrace();
            System.out.println(e.getMessage() + " using token " + this.getToken());
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
