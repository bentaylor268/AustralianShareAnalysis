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

public class YearlyFinancials {
    private static final Logger logger = Logger.getLogger("YearlyFinancials-logger");
    private static String DIRECTORY =  new WorkingDirectory().getWorkingDirectory();
    //private static String DIRECTORY = "/home/ben/Desktop/ASX/";
    private String url = "https://eodhistoricaldata.com/api";
    private String TOKEN = "demo";
    private String companyCode = null;
    private int startYearIndex;
    private int numberOfYears;
    private JSONObject financials;
    private JSONArray epsHistory;
    private JSONObject getLastSharePrice = new JSONObject();
    private JSONArray historicalSharePriceObject = new JSONArray();
    private JSONArray returnOnShareholderEquity;

    public YearlyFinancials(String url, String companyCode, String token, boolean isReadFile,
                            int startYearIndex, int numberOfYears) throws JSONException, Exception {

        logger.log(Level.SEVERE,"Start of yearly financials");
        this.url = url;
        this.startYearIndex = startYearIndex;
        this.numberOfYears = numberOfYears;
        this.companyCode = companyCode;
        this.TOKEN = token;

        this.setFinancialData(isReadFile);
        logger.log(Level.SEVERE,"Start of yearly financials");
        this.setFinalSharePrice(isReadFile);
        this.setHistoricalSharePrice(isReadFile);
        this.setEPSHistory();
        this.setReturnOnShareholderEquity();
    }

    public String getCompanyCode() {
        return financials.getJSONObject("General").getString("Code");
    }
    public String getCompanyName() {
        return financials.getJSONObject("General").getString("Name");
    }

    public String getCompanyDescription() {
        return financials.getJSONObject("General").getString("Description");
    }

    public int getStartYearIndex() {
        return startYearIndex;
    }

    public void setStartYearIndex(int startYearIndex) {
        this.startYearIndex = startYearIndex;
    }

    public int getNumberOfYears() {
        return numberOfYears;
    }

    public void setNumberOfYears(int numberOfYears) {
        this.numberOfYears = numberOfYears;
    }

    public void setFinancialData(boolean isFileRead) throws Exception {

        String fileName = DIRECTORY + "Fundamentals-" + this.companyCode + ".json";
        logger.log(Level.SEVERE,fileName + " is read file " + isFileRead);
        String response;
        if (! isFileRead) {
            URL url = new URL(this.url + "/fundamentals/" + this.companyCode+"?api_token=" + TOKEN);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                response = RestClient.getResponseString(conn).toString();
            } catch (Exception e) {
                return;
            }
            this.financials = new JSONObject(response);
            new DataFileUtilities().writeFile(fileName, response);
            return;
        }
        response = new DataFileUtilities().readFile(fileName);
        logger.log(Level.SEVERE,fileName + " read file " + response);
        if (response != null) {
            this.financials = new JSONObject(response);
        }
        return;
    }



    public JSONObject getFinalSharePrice() {
        return this.getLastSharePrice;
    }



    private void setFinalSharePrice(boolean isFileRead) throws JSONException, Exception {
        URL sharePriceUrl;
        if (! isFileRead && startYearIndex == 0 ) {
            sharePriceUrl = new URL(this.url + "/real-time/" + this.companyCode + "?fmt=json&api_token=" + TOKEN);
            System.out.println(sharePriceUrl.toString());
            HttpURLConnection con = (HttpURLConnection) sharePriceUrl.openConnection();
            String response = RestClient.getResponseString(con).toString();
            this.getLastSharePrice = new JSONObject(response);

            return;
        }

        LocalDate date =  LocalDate.now().minusYears(numberOfYears);
        String response = this.getHistoricalPrices(isFileRead);


		/*

		URL historicalSharePrices = new URL("https://eodhistoricaldata.com/api/eod/" + this.companyCode + "?fmt=json&api_token=" + TOKEN);
		HttpURLConnection historicalSharePriceCon = (HttpURLConnection) historicalSharePrices.openConnection();
		String response =  RestClient.getResponseString(historicalSharePriceCon).toString();
*/

        JSONArray historicalPrices;
        try {
            historicalPrices =  new JSONArray(response);
        } catch (Exception e) {
            return;
        }

        this.getLastSharePrice = getLatestPriceFromHistoricalPrices(new JSONArray(response));
        return;

    }

    private JSONObject getLatestPriceFromHistoricalPrices(JSONArray pricesArray) {
        JSONObject returnPrice = new JSONObject();
        for (int i=0;i<pricesArray.length();i++) {
            JSONObject price = pricesArray.getJSONObject(i);
            if (! returnPrice.has("date")) {
                returnPrice = new JSONObject(price.toString());
                continue;
            }
            if (price.getString("date").compareTo(returnPrice.getString("date"))>0) {
                returnPrice = new JSONObject(price.toString());
            }
        }
        return returnPrice;

    }

    private JSONObject getClosingPriceAtDate(JSONArray historicalPrices, LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i=0;i<historicalPrices.length();i++) {
            if (historicalPrices.getJSONObject(i).getString("date").equals(date.format(formatter))) {
                return historicalPrices.getJSONObject(i);
            }
        }
        return null;


    }

    public JSONArray getHistoricalSharePrice() {
        return this.historicalSharePriceObject;
    }



    private String getHistoricalPrices(boolean isFileRead) throws Exception {
        String fileName = DIRECTORY + "Eod-" + this.companyCode + ".json";
        if (! isFileRead) {
            URL historicalSharePrices = new URL(this.url + "/eod/" + this.companyCode + "?fmt=json&api_token=" + TOKEN);

            HttpURLConnection historicalSharePriceCon = (HttpURLConnection) historicalSharePrices.openConnection();

            try {
                String response =  RestClient.getResponseString(historicalSharePriceCon).toString();
                new DataFileUtilities().writeFile(fileName, response);
                return response;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
            }
        }

        return new DataFileUtilities().readFile(fileName);

    }
    private void setHistoricalSharePrice(boolean isFileRead) throws Exception {

        String prices = this.getHistoricalPrices(isFileRead);
        if (prices == null) {
            this.historicalSharePriceObject = null;
            return;
        }
        this.historicalSharePriceObject = new JSONArray(this.getHistoricalPrices(isFileRead));
    }


    public JSONArray getEPSHistory() {
        return this.epsHistory;
    }


    private int getReportingMonthValue(JSONObject epsHistory) {
        JSONObject firstObject = new JSONObject();
        for (String key : epsHistory.keySet()) {
            Object value = epsHistory.get(key);

            if (value instanceof JSONObject) {
                // Increment count for the current nested JSONObject
                firstObject = new JSONObject(value.toString());
            }
        }

        String firstDate = firstObject.getString("date");
        LocalDate localDate = LocalDate.parse(firstDate);
        return localDate.getMonthValue();


    }

    private int getReportingMonthValue1(List<FinancialAnalysis> annualEPSList) {


        List<Integer> monthList = new ArrayList<Integer>();
        for (FinancialAnalysis financialAnalysis : annualEPSList) {
            LocalDate localDate = LocalDate.parse(financialAnalysis.getYear());
            monthList.add(localDate.getMonthValue());
        }
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (Integer myMonth : monthList) {
            if (! map.containsKey(myMonth)) {
                map.put(myMonth, 0);
            }
            int newCount = map.get(myMonth) + 1;
            map.put(myMonth, newCount);
        }
        int maxCount = 0;
        int returnMonth = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            if (value.intValue()> maxCount) {
                returnMonth = key;
            }
        }
        return returnMonth;

    }

    private void setEPSHistory() {
        if (financials == null || ! financials.has("Earnings")) {
            this.epsHistory = null;
            return;
        }



        List<FinancialAnalysis> annualEPSList = this.castToList(financials.getJSONObject("Earnings")
                .getJSONObject("Annual"));

        System.out.println(financials.getJSONObject("Earnings")
                .getJSONObject("Annual"));

        JSONArray returnArray = new JSONArray();
        for (FinancialAnalysis financialAnalysis : annualEPSList) {
            LocalDate localDate = LocalDate.parse(financialAnalysis.getYear());


            if (localDate.getMonthValue()!=this.getReportingMonthValue(financials.getJSONObject("Earnings")
                    .getJSONObject("Annual"))) {
                continue;
            }

            JSONObject epsObject = new JSONObject();
            epsObject.put("year",financialAnalysis.getYear());
            epsObject.put("eps", financialAnalysis.getEarningsPerShare());
            returnArray.put(epsObject);
        }
        this.epsHistory = returnArray;
    }


    public List<FinancialAnalysis> castToList(JSONObject jsonObject) {
        List<FinancialAnalysis> returnList = new ArrayList<FinancialAnalysis>();
        Iterator<String> keys = jsonObject.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            if (jsonObject.get(key) instanceof JSONObject) {
                FinancialAnalysis year = new FinancialAnalysis(key,jsonObject.getJSONObject(key));
                returnList.add(year);
            }
        }

        // limit list to the last 'numberOfYears'.
        returnList = this.getHistoricalFinancialsList(returnList);

        return returnList;

    }


    private List<FinancialAnalysis> getHistoricalFinancialsList(List<FinancialAnalysis> financialAnalysis) {
        financialAnalysis = getSortedList(financialAnalysis);

        List<FinancialAnalysis> returnList = new ArrayList<FinancialAnalysis>();
        int targetIndex = this.startYearIndex + this.numberOfYears;
        if (financialAnalysis.size()>targetIndex) {
            return financialAnalysis.subList(startYearIndex, targetIndex);
        }

        for (int i=startYearIndex; i<targetIndex; i++) {
            if (i>financialAnalysis.size()-1) {
                continue;
            }
            returnList.add(financialAnalysis.get(i));
        }
        return returnList;
    }

    private List<FinancialAnalysis> getSortedList(List<FinancialAnalysis> yearlyFinancials) {
        Collections.sort(yearlyFinancials,(d1,d2)-> {
            return d2.getYear().compareTo(d1.getYear());
        });
        return yearlyFinancials;
    }


    public JSONArray getReturnOnShareholderEquity() {
        return this.returnOnShareholderEquity;
    }

    private void setReturnOnShareholderEquity() {

        if (this.financials == null || ! financials.has("Financials")) {
            this.returnOnShareholderEquity=null;
            return;
        }
        // array of return on shareholder equity values.


        // to calculate return on shareholders equity:
        // divde earnings (after tax) / shareholders equity * 100.


        // Financials->Income_Statement->yearly-> netIncome / Financials->Balance_Sheet->yearly->totalStockholderEquity * 100.

        // match years in different arrays and calculated to create array.
        JSONArray returnArray = new JSONArray();

        List<FinancialAnalysis> netIncomeList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Income_Statement").getJSONObject("yearly"));
        List<FinancialAnalysis> totalShareholderEquityList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));
        for (FinancialAnalysis financialAnalysis : netIncomeList) {
            JSONObject returnObject = new JSONObject();
            returnObject.put("year", financialAnalysis.getYear());
            for (FinancialAnalysis shareholderEquityFinancials : totalShareholderEquityList) {
                if (! financialAnalysis.getYear().equals(shareholderEquityFinancials.getYear())){
                    continue;
                }
                double returnOnShareholderEquity = financialAnalysis.getNetIncome(this.getReturnOnShareholderEquity())
                        / shareholderEquityFinancials.getStockholderEquity();

                if (Double.valueOf(returnOnShareholderEquity).isNaN()) {
                    returnObject.put("shareholderReturnOnEquity", 0d);
                    returnObject.put("netIncome", 0d);
                    returnObject.put("totalStockholderEquity", 0d);

                    break;
                }
                //	 System.out.println("roi: " + returnOnShareholderEquity);
                if (Double.valueOf(returnOnShareholderEquity).isNaN() || Double.valueOf(returnOnShareholderEquity).isInfinite()) {
                    returnOnShareholderEquity = 0d;
                }

                returnObject.put("shareholderReturnOnEquity", returnOnShareholderEquity);
                returnObject.put("netIncome", Double.valueOf(financialAnalysis.getNetIncome(this.getReturnOnShareholderEquity())));
                returnObject.put("totalStockholderEquity", Double.valueOf(shareholderEquityFinancials.getStockholderEquity()));

                break;
            }
            returnArray.put(returnObject);
        }
        this.returnOnShareholderEquity = returnArray;
    }



    public double getAverageReturnOnShareholderEquity() {

        return averageResult(returnOnShareholderEquity,"shareholderReturnOnEquity");
    }


    private double averageResult(JSONArray valueArray, String key) {
        double returnValue = 0d;

        double sum = 0d;
        for (int i=0;i<valueArray.length();i++) {
            if (! valueArray.getJSONObject(i).has(key)) {
                continue;
            }
            sum = sum + valueArray.getJSONObject(i).getDouble(key);
        }
        returnValue = sum/Double.valueOf(valueArray.length()).doubleValue();
        return returnValue;
    }



    public double getLatestDebt() {
        List<FinancialAnalysis> debtList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));
        if (debtList.size()==0) {
            return 0d;
        }
        return debtList.get(0).getNetDebt();
    }

    public List<FinancialAnalysis> getEarningsList() {
        JSONObject earnings = financials.getJSONObject("Earnings").getJSONObject("Annual");
        return this.excludeNoEarningsYearFromList(this.castToList(financials.getJSONObject("Earnings").getJSONObject("Annual")));
    }

    private List<FinancialAnalysis> excludeNoEarningsYearFromList(List<FinancialAnalysis> finList) {
        List<FinancialAnalysis> returnList = new ArrayList<>();
        for (FinancialAnalysis year : finList) {
            if (year.getEarningsPerShare()==0d) {
                continue;
            }
            returnList.add(year);
        }
        return returnList;

    }

    public double getLatestNetEarnings() {
        List<FinancialAnalysis> earningsList = this.getEarningsList();
        FinancialAnalysis earnings = earningsList.get(0);
        return earnings.getEarningsPerShare() * earnings.getNumberOfShares();
    }


    public double getEPSAnnualRateReturn() {
        try {
            List<FinancialAnalysis> annualEPSList = this.getEarningsList();
            logger.log(Level.SEVERE, "getEPSAnnualRateReturn first is " + annualEPSList.get(0).getEarningsPerShare() + " length is " + annualEPSList.size());
            int lastObjectIndex = numberOfYears-1;
            if (lastObjectIndex>annualEPSList.size()) {
                lastObjectIndex = annualEPSList.size() -1;
            }
            return getCompoundingInterestRate(annualEPSList.get(lastObjectIndex).getEarningsPerShare(),
                    annualEPSList.get(0).getEarningsPerShare());
        } catch (Exception e) {
            return 0;
        }
    }

    public double getCompoundingGrowthRate() {
        return this.getCompoundingInterestRate(this.getEarliestEPS(), this.getCurrentEPS());
    }

    public double getCompoundingInterestRate(double presentValue, double futureValue) {
        //i = ( FV / PV )1/n − 1

        double a = (futureValue / presentValue);

        double b = 1/Double.valueOf(numberOfYears).doubleValue();

        double i = Math.pow(a,b) - 1;
        return i;
    }

    public double getYearsToPayOffDebt() {
        // for this years figures
        // return long term debt / totla net earnings

        double latestDebt = getLatestDebt();
        double latestNetEarnings = getLatestNetEarnings();
        return latestDebt/latestNetEarnings;
    }


    public boolean isCompanyBuyingBackShares() {
        if (getSharesOutstanding(this.numberOfYears-1)>getSharesOutstanding(0)) {
            return true;
        }
        return false;
    }


    public double getSharesOutstanding(int yearIndex) {
        List<FinancialAnalysis> sharesOutstandingList =  this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));
        sharesOutstandingList = this.getSortedList(sharesOutstandingList);

        if (sharesOutstandingList.size()<yearIndex+1) {
            return -1;
        }

        return sharesOutstandingList.get(yearIndex).getSharesOutstanding();
    }


    public double getCurrentEPS() {
        //TODO	 need to update current values to handle starting on a previous Year.class
        if (! financials.has("Highlights") || financials.getJSONObject("Highlights").isNull("EarningsShare")) {
            return 0d;
        }
        return financials.getJSONObject("Highlights").getDouble("EarningsShare");
    }
    public double getProjectionAccuracy() {
        return (this.getCurrentSharePrice() - this.getProjectedTradingPrice())/this.getCurrentSharePrice()*100 ;
    }

    public boolean isValidLastSharePrice() {
        if (getLastSharePrice == null) {
            return false;
        }
        return true;
    }
    public double getCurrentSharePrice() {
        //TODO need to change to hisotirial api to handle running on previous periods.
        if ( getLastSharePrice.has("previousClose")
                && ! getLastSharePrice.get("previousClose").equals("NA")) {
            return getLastSharePrice.getDouble("previousClose");
        }

        if (getLastSharePrice.get("close").equals("NA")) {
            return 0;
        }
        return getLastSharePrice.getDouble("close");
    }

    public double getCurrentRateOfReturn() {
        return getCurrentEPS()/getCurrentSharePrice();
    }

    public double getCompoundingRateOfReturn() {

        return this.getCompoundingInterestRate(this.getCurrentSharePrice(),
                this.getFuturePerShareTradingPrice()*100);
    }

    public double getAveragePercentageDividendsPaid() {
        // get the percentage average of return on shareholder equity paid as a dividend
        List<FinancialAnalysis> dividendsList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Cash_Flow").getJSONObject("yearly"));
        JSONArray returnOnShareholderEquityArray = getReturnOnShareholderEquity();

        dividendsList = this.setAnnualReturnOnEquityToDividendObject(dividendsList, returnOnShareholderEquityArray);


        double sumDividendPercentage = 0;
        for (FinancialAnalysis yearDividend : dividendsList) {
            sumDividendPercentage = sumDividendPercentage
                    + yearDividend.getDividendPaymentPercentage(this.getReturnOnShareholderEquity());
        }

        return sumDividendPercentage/dividendsList.size();

    }


    private List<FinancialAnalysis> setAnnualReturnOnEquityToDividendObject(List<FinancialAnalysis> dividendsList, JSONArray returnOnShareholderEquityArray) {
        for (FinancialAnalysis yearDividend : dividendsList) {
            for (int i=0;i<returnOnShareholderEquityArray.length();i++) {
                JSONObject yearROE = returnOnShareholderEquityArray.getJSONObject(i);
                if (! yearROE.getString("year").equals(yearDividend.getYear())) {
                    continue;
                }
                if (!yearROE.has("shareholderReturnOnEquity")) {
                    continue;
                }
                yearDividend.setShareholderReturnOnEquity(yearROE.getDouble("shareholderReturnOnEquity"));

            }
        }
        return dividendsList;
    }



    public JSONArray getPercentageDividendsPaidArray() {

        List<FinancialAnalysis> dividendsList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Cash_Flow").getJSONObject("yearly"));
        JSONArray returnOnShareholderEquityArray = getReturnOnShareholderEquity();

        dividendsList = this.setAnnualReturnOnEquityToDividendObject(dividendsList, returnOnShareholderEquityArray);


        JSONArray returnArray = new JSONArray();
        for (FinancialAnalysis yearDividend : dividendsList) {
            JSONObject returnObject = new JSONObject();
            returnObject.put("year",yearDividend.getYear());
            returnObject.put("dividend", BigDecimal.valueOf(yearDividend.getDividendPaid()));
            returnObject.put("income", BigDecimal.valueOf(yearDividend.getNetIncome(this.getReturnOnShareholderEquity())));

            returnObject.put("percentPaid",BigDecimal.valueOf(yearDividend.getDividendPaymentPercentage(this.getReturnOnShareholderEquity())));
            returnArray.put(returnObject);
        }

        return returnArray;

    }

    public double getPerShareShareholdersEquity() {
        List<FinancialAnalysis> totalShareholderEquityList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));

        if (totalShareholderEquityList.size()==0) {
            return 0d;
        }
        return totalShareholderEquityList.get(0).getShareholderEquityPerShare();
    }

    public double getEquityGrowth() {
        double averageReturn =  this.getAverageReturnOnShareholderEquity();
        return averageReturn - (averageReturn * this.getAveragePercentageDividendsPaid());
    }



    public double getAverageEPS() {
        double sum = 0;
        int yearsCounted = 0;
        for (int i=0;i<this.epsHistory.length();i++) {
            if (this.epsHistory.getJSONObject(i).getDouble("eps") <=0d) {
                continue;
            }
            yearsCounted ++;
            sum = sum + this.epsHistory.getJSONObject(i).getDouble("eps");
        }

        return sum/Double.valueOf(yearsCounted).doubleValue();
    }

    public double getFuturePerShareEarnings() {
        return this.getFutureValue(this.getAverageEPS(), this.getEquityGrowth());
    }

    public double getAveragePERatio() {
        List<FinancialAnalysis> epsList = this.castToList(financials.getJSONObject("Earnings").getJSONObject("Annual"));
        logger.log(Level.SEVERE, "epsList using " + financials.getJSONObject("Earnings").getJSONObject("Annual").toString());
        List<Double> peList = new ArrayList<Double>();
        double sumTotal = 0d;
        for (int i=0;i<historicalSharePriceObject.length();i++) {
            JSONObject dayPrice = historicalSharePriceObject.getJSONObject(i);

            for (FinancialAnalysis yearEps : epsList) {
                if (! dayPrice.getString("date").equals(yearEps.getYear())) {
                    continue;
                }
                if (yearEps.getEarningsPerShare()==0d) {
                    continue;
                }
                logger.log(Level.SEVERE, "day peRatio close is  " + dayPrice.getDouble("close") + " and " + yearEps.getEarningsPerShare() + " " + yearEps.getYear());
                double peRatio = dayPrice.getDouble("close")/yearEps.getEarningsPerShare();
                logger.log(Level.SEVERE, "day peRatio is  " + peRatio);

                peList.add(peRatio);
                sumTotal = sumTotal + peRatio;

            }
        }
        logger.log(Level.SEVERE, "epsList value using " + sumTotal + "  and " + peList.size());
        return sumTotal/peList.size();

    }


    public boolean isReturnOnShareholderEquityHigh() {
        /// loookinf for average above 15%
        double target = 0.15d;
        double averageReturn = this.getAverageReturnOnShareholderEquity();
        if (averageReturn >= target) {
            return true;
        }
        return false;
    }

    public boolean isReturnOnShareholderEquityLow() {
        /// loookinf for average above 15%
        double target = 0.08d;
        double averageReturn = this.getAverageReturnOnShareholderEquity();
        if (averageReturn >= target) {
            return false;
        }
        return true;
    }

    public double getEarliestEPS() {
        if (this.getEPSHistory().length()<numberOfYears) {
            return this.getEPSHistory().getJSONObject(this.getEPSHistory().length()-1).getDouble("eps");
            //	return -1;
        }

        return this.getEPSHistory().getJSONObject(numberOfYears-1).getDouble("eps");
    }

    public double getProjectedTradingPrice() {

        return this.getProjectedPerShareEarnings() * this.getAveragePERatio();
    }

    public double getProjectedPerShareEarnings() {
        return this.getFutureValue(this.getAverageEPS(), this.getCompoundingGrowthRate());
    }

    public double getFuturePerShareTradingPrice() {
        double dividendAmount = this.getFuturePerShareEarnings() * this.getAveragePercentageDividendsPaid();
        double earningsMinusDividends = this.getFuturePerShareEarnings() - dividendAmount;
        return  earningsMinusDividends * (this.getAveragePERatio()) + this.getFuturePerShareValueOfShareholderEquity();
    }

    private double getFutureValue(double presentValue, double interest) {
        // FV = PV (1+r)n
        return presentValue * Math.pow((1+interest),numberOfYears);
    }

    public double getFuturePerShareValueOfShareholderEquity() {
        double equityGrowth = this.getAverageReturnOnShareholderEquity() - (this.getAverageReturnOnShareholderEquity() * this.getAveragePercentageDividendsPaid());
        return this.getFutureValue(this.getPerShareShareholdersEquity(), equityGrowth);
    }


}
