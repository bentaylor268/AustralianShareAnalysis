package australian.share.analysis;

import data.rest.client.EODHDRestClient;
import data.rest.client.HotCopperScreenScrape;
import file.utilities.WorkingDirectory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompanyFinancials {
    private static final Logger logger = Logger.getLogger("YearlyFinancials-logger");
    private static String DIRECTORY =  new WorkingDirectory().getWorkingDirectory();
    public static String ASX_CODE_REGION = ".AU";
    //private static String DIRECTORY = "/home/ben/Desktop/ASX/";
   // private String url = "https://eodhistoricaldata.com/api";
    private String TOKEN = "demo";
    private String companyCode;
    private int startYearIndex;
    private int numberOfYears;
    private JSONObject financials;
    private JSONArray epsHistory;
    private JSONObject getLastSharePrice = new JSONObject();
    private JSONArray historicalSharePriceObject = new JSONArray();
    private JSONArray returnOnShareholderEquity;
    private double currentPrice;

    public CompanyFinancials(String companyCode,int startYearIndex, int numberOfYears) throws JSONException, Exception {


      //  this.url = url;
        this.startYearIndex = startYearIndex;
        this.numberOfYears = numberOfYears;
        this.companyCode = companyCode;

        this.setFinancialData();
        this.currentPrice = this.setCurrentSharePrice();
       // this.setFinalSharePrice(isReadFile);
        this.setHistoricalSharePrice();
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
            returnObject.put("numberOfShares",this.getNumberOfShares(Integer.valueOf(yearDividend.getYear().substring(0,4)).intValue()));
            returnObject.put("dividendPerShare",BigDecimal.valueOf(yearDividend.getDividendPaid()/ this.getNumberOfShares(Integer.valueOf(yearDividend.getYear().substring(0,4)).intValue())));
            returnArray.put(returnObject);
        }

        return returnArray;

    }

    public double getCurrentNetIncome() {
        List<FinancialAnalysis> dividendsList = this.castToList(financials.getJSONObject("Financials").getJSONObject("Cash_Flow").getJSONObject("yearly"));
        JSONArray returnOnShareholderEquityArray = getReturnOnShareholderEquity();
        dividendsList = this.setAnnualReturnOnEquityToDividendObject(dividendsList, returnOnShareholderEquityArray);
        String latestYear = "";
        JSONArray returnArray = new JSONArray();
        double latestNetIncome = 0d;
        for (FinancialAnalysis yearDividend : dividendsList) {
            if (latestYear.compareTo(yearDividend.getYear())>0) {
                continue;
            }
            latestYear = yearDividend.getYear();
            logger.log(Level.SEVERE,"Latest Year is " + latestYear + " vs " + yearDividend.getYear());
            latestNetIncome = yearDividend.getNetIncome(this.getReturnOnShareholderEquity());
        }
        return latestNetIncome;
    }


    public void setFinancialData() throws Exception {
        this.financials = new EODHDRestClient().setFinancialData(this.companyCode+ASX_CODE_REGION);
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

    private void setHistoricalSharePrice() throws Exception {
        String prices = new EODHDRestClient().getHistoricalPrices(this.companyCode+ASX_CODE_REGION);
        if (prices == null) {
            this.historicalSharePriceObject = null;
            return;
        }
        this.historicalSharePriceObject = new JSONArray(prices);
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
        return this.getHistoricalFinancialsList(returnList);
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
            int lastObjectIndex = numberOfYears-1;
            if (lastObjectIndex>annualEPSList.size()) {
                lastObjectIndex = annualEPSList.size() -1;
            }
            return new CalculationUtilities().getCompoundingInterestRate(annualEPSList.get(lastObjectIndex).getEarningsPerShare(),
                    annualEPSList.get(0).getEarningsPerShare(),numberOfYears);
        } catch (Exception e) {
            return 0;
        }
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


    public long getSharesOutstanding(int yearIndex) {
        List<FinancialAnalysis> sharesOutstandingList =  this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));
        sharesOutstandingList = this.getSortedList(sharesOutstandingList);

        if (sharesOutstandingList.size()<yearIndex+1) {
            return -1;
        }
        return sharesOutstandingList.get(yearIndex).getSharesOutstanding();
    }

    public long getNumberOfShares() {
        List<FinancialAnalysis> sharesOutstandingList =  this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));
        sharesOutstandingList = this.getSortedList(sharesOutstandingList);
        if (sharesOutstandingList.size()==0) {
            return -1;
        }
        return sharesOutstandingList.get(0).getSharesOutstanding();
    }
    public long getNumberOfShares(int year) {
        List<FinancialAnalysis> yearlyData =  this.castToList(financials.getJSONObject("Financials").getJSONObject("Balance_Sheet").getJSONObject("yearly"));
        yearlyData = this.getSortedList(yearlyData);

        if (yearlyData.size()==0) {
            return -1;
        }
        for (FinancialAnalysis financialAnalysis : yearlyData) {
            if (Integer.valueOf(financialAnalysis.getYear().substring(0,4)).intValue()!=year) {
                continue;
            }
            return financialAnalysis.getSharesOutstanding();
        }
        return -1;
    }

    public double getBookValue() {
        if (! financials.has("Highlights") || financials.getJSONObject("Highlights").isNull("BookValue")) {
            return 0d;
        }
        logger.log(Level.SEVERE,"CURRENT EPS IS " + financials.getJSONObject("Highlights"));
        return financials.getJSONObject("Highlights").getDouble("BookValue");
    }

    public double getCurrentEPS() {
        //TODO	 need to update current values to handle starting on a previous Year.class
        if (! financials.has("Highlights") || financials.getJSONObject("Highlights").isNull("EarningsShare")) {
            return 0d;
        }
        logger.log(Level.SEVERE,"CURRENT EPS IS " + financials.getJSONObject("Highlights"));
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
        return this.currentPrice;
    }
    public double setCurrentSharePrice() {
        return new HotCopperScreenScrape(this.companyCode).getCurrentPrice();
    }
    public double getCurrentRateOfReturn() {

        return getCurrentEPS()/getCurrentSharePrice();
    }

    public double getCompoundingRateOfReturn() {

        return new CalculationUtilities().getCompoundingInterestRate(this.getCurrentSharePrice(),
                this.getFuturePerShareTradingPrice()*100,numberOfYears);
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
                double peRatio = dayPrice.getDouble("close")/yearEps.getEarningsPerShare();
                peList.add(peRatio);
                sumTotal = sumTotal + peRatio;

            }
        }
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
        int index = 0;
        if (this.getEPSHistory().length()<numberOfYears) {
             return this.getEarliestEPSGreaterThanZero(this.getEPSHistory().length());
        }
        return this.getEarliestEPSGreaterThanZero(numberOfYears);
    }

    private double getEarliestEPSGreaterThanZero(int index) {
        for (int i=index-1; i>=0; i--) {
            if (this.getEPSHistory().getJSONObject(i-1).getDouble("eps")<=0) {
                continue;
            }
            logger.log(Level.SEVERE,"EPS " + this.getEPSHistory().getJSONObject(i-1));
            return this.getEPSHistory().getJSONObject(i-1).getDouble("eps");
        }
        return 0d;

    }

    public double getProjectedTradingPrice() {
        logger.log(Level.INFO,"PROJECED TRADING PRICE earliestEPS: " + this.getProjectedPerShareEarnings() + " average pe ratio: " + this.getAveragePERatio());
        return this.getProjectedPerShareEarnings() * this.getAveragePERatio();
    }
    public double getCompoundingGrowthRate() {

        logger.log(Level.INFO,"getCompoundingGrowthRate earliestEPS: " + this.getEarliestEPS() + " current eps " + this.getCurrentEPS());
        return new CalculationUtilities().getCompoundingInterestRate(this.getEarliestEPS(), this.getCurrentEPS(),numberOfYears);
    }
    public double getProjectedPerShareEarnings() {
        logger.log(Level.INFO,"getProjectedPerShareEarnings averageEPS: " + this.getAverageEPS() + " growth rate is " + this.getCompoundingGrowthRate() );

        return this.getFutureValue(this.getAverageEPS(), this.getCompoundingGrowthRate());
    }

    public double getFuturePerShareTradingPrice() {

        double earningsMinusDividends = this.getFuturePerShareEarnings() - this.getFutureDividendsToBePaid();
        return earningsMinusDividends * (this.getAveragePERatio()) + this.getFuturePerShareValueOfShareholderEquity();
    }

    public double getFutureDividendsToBePaid() {
        return this.getFuturePerShareEarnings() * this.getAveragePercentageDividendsPaid();
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
