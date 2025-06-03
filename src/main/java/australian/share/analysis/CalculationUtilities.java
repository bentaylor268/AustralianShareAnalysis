package australian.share.analysis;

import data.rest.client.RestClient;
import file.utilities.DataFileUtilities;
import file.utilities.WorkingDirectory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalculationUtilities {


    public double getCompoundingInterestRate(double presentValue, double futureValue, int numberOfYears) {
        //i = ( FV / PV )1/n − 1
        double a = (futureValue / presentValue);
        double b = 1/Double.valueOf(numberOfYears).doubleValue();
        double i = Math.pow(a,b) - 1;
        return i;
    }


    public static double calculatePresentValue(double futureValue, double annualRate, int years, int compoundsPerYear) {
        double rateDecimal = annualRate / 100.0;
        double base = 1 + rateDecimal / compoundsPerYear;
        double exponent = compoundsPerYear * years;
        return futureValue / Math.pow(base, exponent);
    }
}
