package org.example;

import australian.share.analysis.ASXCodes;
import australian.share.analysis.YearlyFinancials;
import australian.share.analysis.FinancialAnalysis;
import file.utilities.DataFileUtilities;
import file.utilities.WorkingDirectory;
import html.bootstrap4.utilities.BootstrapCard;
import html.bootstrap4.utilities.HTMLHeader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Buffetology {
    private static final Logger logger = Logger.getLogger("Buffetology-logger");
    JSONObject currentSharePriceObject;
    JSONArray historicalSharePriceObject;
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private double accuracy = 0d;
    private double previousAccuracy = 0d;

    private YearlyFinancials yearlyFinancials;

    public static void main(String[] args) throws Exception {
        boolean isReadFile = true;
        //	if (args[0].equals("true")) {
        //		isReadFile = true;
        //	}
        //https://eodhd.com/api/eod/MSFT.US?api_token=64293ba5706115.55872934
        String token = "64293ba5706115.55872934";
        String url = "https://eodhd.com/api";
        //String url = "https://eodhistoricaldata.com/api";
        List<String> stocks = ASXCodes.getASXCodes();
        Map<String, Integer> multipleFlag = new HashMap<String, Integer>();
        for (int i=7; i<10;i++) {
            for (String stock : stocks) {
                if (! stock.equals("MQG")){
                    continue;
                }
                StringBuilder html = new StringBuilder(HTMLHeader.getHTMLHeader() + "<body>");
                LocalDate date =  LocalDate.now().minusYears(0);
                html.append("<p>From: " +date.toString() + "</p>");
                stock = stock + ".AU";
                try {

                    Buffetology buffetology = new Buffetology(new YearlyFinancials(url, stock, token, isReadFile,0, i));
                    if (! buffetology.isEnoughHistory()) {
                        System.out.println(stock + ": not enough history");
                        continue;
                    }

                    if (! buffetology.isCheckSuccess()) {
                        System.out.println(stock + ": failed analsysis");
                        continue;
                    }
//                    date =  LocalDate.now().minusYears(10);

                    double averagePrediction = (buffetology.getYearlyFinancials().getFuturePerShareTradingPrice() + buffetology.getYearlyFinancials().getProjectedTradingPrice()) / 2d;
                    double currentHomeLoanInterestRate = 6.5d;

                    double predictedRateOfReturn = buffetology.getYearlyFinancials().getCompoundingInterestRate(buffetology.getYearlyFinancials().getCurrentSharePrice(),averagePrediction) * 100d;

                    String bg = buffetology.getBackgroundColor(predictedRateOfReturn, currentHomeLoanInterestRate);

                    html.append("<table class=\"table table-striped\"><tr><td bgcolor="+bg+"><font color=white>Rate of return: " + buffetology.formatNumber(predictedRateOfReturn) +"%</font></td></tr></table>");
                    html.append("<h2>Summary</h2>" +
                            "     <table class=\"table table-striped\">"  +
                            "<thead>"
                            +"<tr>"
                            + "<th>Current price</th>"
                            + "<th>Method 1</th>"
                            + "<th>Method 2</th>"
                            + "<th>Average</th>"
                            + "<th>Dividends</th>"
                            + "<th>Total Return %</th>"
                            + "</thead>"
                            +"</tr>"
                            + "<tbody>");
                    html.append("<tr><td>$" +  buffetology.getYearlyFinancials().getCurrentSharePrice()
                            + "</td><td>$" + buffetology.formatNumber(buffetology.getYearlyFinancials().getFuturePerShareTradingPrice()) + "</td>"
                            + "<td>$" 	+ buffetology.formatNumber(buffetology.getYearlyFinancials().getProjectedTradingPrice()) + "</td>"
                            + "<td><b>$" + buffetology.formatNumber(averagePrediction) + "</b></td>"
                            + "<td><b>$" + buffetology.getSumFutureDividendReturnOnOneDollar(10) + "</b></td>" +
                            "</tr>"
                            + "</tbody></table>");

                    String header = new BootstrapCard().wrapStringInCard(html.toString());
                    html = new StringBuilder(header);
                    html.append(buffetology.prepareReportPeriod(true));

                    html.append("</body></html>");
                   // logger.log(Level.SEVERE,html.toString());
                    //if (! bg.equals("green")) {
                    //    if (multipleFlag.containsKey(stock)) {
                     //       multipleFlag.remove(stock);
                     //   }
                     //   continue;
                   // }
                   // if (i == 7) {
                    //    multipleFlag.put(stock,1);
                     //   continue;
                    //}
                    //if (multipleFlag.containsKey(stock) && bg.equals("green") &&  i ==9) {
                        new DataFileUtilities().writeFile(new WorkingDirectory().getWorkingDirectory() + "results/" + stock +"--" + bg +"-" + i + ".html", html.toString());
                        logger.log(Level.INFO, "file: " + new WorkingDirectory().getWorkingDirectory() + "results/" + stock +"--" + bg +"-" + i + ".html");
                    //}
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }



    public YearlyFinancials getYearlyFinancials() {
        return this.yearlyFinancials;
    }

    public String getBackgroundColor(double predictedRateOfReturn, double currentHomeLoanInterestRate) {
        if (predictedRateOfReturn > currentHomeLoanInterestRate) {
            return "green";
        }
        if (predictedRateOfReturn > 0d) {
            return "orange";
        }
        return "red";

    }

    public Buffetology(YearlyFinancials yearlyFinancials) {
           this.yearlyFinancials = yearlyFinancials;
    }


    private StringBuilder prepareReportPeriod(boolean printHeader) {

        StringBuilder html = new StringBuilder();
        if (printHeader) {
            html.append(this.getCompanyHeader());
        }
        html.append("<br><br><br>");
        html.append(this.getBuffettologyStep1());
        html.append(this.getBuffettologyStep2());
        html.append(this.getBuffettologyStep3());
        html.append(this.getBuffettologyStep4());
        html.append(this.getBuffettologyStep6());
        html.append(this.getBuffettologyStep8());
        html.append(this.getBuffettologyStep9());
        html.append(this.getBuffettologyStep10());
        html.append(this.getBuffettologyStep11());
        html.append(this.getBuffettologyStep12());
        html.append(this.getBuffettologyStep13());

        return new StringBuilder(html.toString());
    }



    private boolean isEnoughHistory() {
        JSONArray historyArray = this.yearlyFinancials.getEPSHistory();
        if (historyArray == null || this.yearlyFinancials==null || this.yearlyFinancials.getNumberOfYears()<=0) {
            return false;
        }

        if (historyArray.length()<(this.yearlyFinancials.getNumberOfYears()-1)) {
            System.out.println("Array size is " + historyArray.length() +  "number of years is " + this.yearlyFinancials.getNumberOfYears());
            System.out.println(historyArray);
            return false;
        }
        return true;
    }

    private String getCompanyHeader() {
        StringBuilder builder = new StringBuilder();
        builder.append("<h1>Code: " + this.yearlyFinancials.getCompanyCode() + ":"
                + this.yearlyFinancials.getCompanyName()
                + "</h1>\n");
        builder.append("<p>" + yearlyFinancials.getCompanyDescription()
                + "</p>\n");
        return builder.toString();

    }

    private String getBuffettologyStep1() {
        return new BootstrapCard().wrapStringInCard("Does the company have an identifiable consumer monopoly?");
    }

    private String getBuffettologyStep2() {
        return new BootstrapCard().wrapStringInCard("Do you understand how it works?");
    }

    private String getBuffettologyStep3() {
        return new BootstrapCard().wrapStringInCard("What is the chance the product will become obsolete?");
    }

    private String getBuffettologyStep4() {
        return new BootstrapCard().wrapStringInCard("Is the company a conglomerate?");
    }
    private String getBuffettologyStep5() {
        StringBuilder builder = new StringBuilder();
        JSONArray epsHistoryArray = this.yearlyFinancials.getEPSHistory();
        builder.append("<h2>Earnings per share history</h2>" +
                "     <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Year</th>"
                + "<th>Earnings per share</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");
        for (int i=0;i<epsHistoryArray.length();i++) {
            JSONObject epsYear = epsHistoryArray.getJSONObject(i);
            builder.append("<tr>"
                    + "<td>"
                    + epsYear.getString("year")
                    + "</td>"
                    + "<td>"
                    + epsYear.getDouble("eps")
                    + "</td>" +
                    "</tr>");
        }
        builder.append("<tr><td>Average Earnings per share</td><td>" + this.formatNumber(yearlyFinancials.getAverageEPS())+"</td></tr>");
        builder.append("            </tbody>"  +
                "          </table>"
               );
        return new BootstrapCard().wrapStringInCard(builder.toString());
    }

    private String getBuffettologyStep6() {
        StringBuilder builder = new StringBuilder();
        builder.append("<h2>Is the company consistently earning a high return on shareholders equity? i.e. more than 15%</h2>" +
                "     <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Year</th>"
                + "<th>Net Income</th>"
                + "<th>Total shareholder equity</th>"
                + "<th>Return on equity</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");

        JSONArray equityArray = this.yearlyFinancials.getReturnOnShareholderEquity();
        for (int i=0;i<equityArray.length();i++) {

            JSONObject equity = equityArray.getJSONObject(i);
            if (! equity.has("netIncome") ||
                    ! equity.has("totalStockholderEquity") ||
                    ! equity.has("shareholderReturnOnEquity")) {
                continue;
            }
            builder.append("<tr>"
                    + "<td>"+equity.getString("year") +"</td>"
                    + "<td>" +df.format(new BigDecimal(equity.getDouble("netIncome")))+"</td>"
                    + "<td>" +df.format( new BigDecimal(equity.getDouble("totalStockholderEquity")))+"</td>"
                    + "<td>" +df.format(new BigDecimal(equity.getDouble("shareholderReturnOnEquity")))+"</td>"
                    +"</td></tr>");

        }
        builder.append("</tbody></table>");
        StringBuilder dataTable = new StringBuilder(new BootstrapCard().wrapStringInCard(builder.toString()));

        StringBuilder summary = new StringBuilder();
        summary.append("<p>Average return on shareholder equity  = " +
                df.format(yearlyFinancials.getAverageReturnOnShareholderEquity() *100) + "%");
        summary.append("<p>Is return greater than 15%? " +yearlyFinancials.isReturnOnShareholderEquityHigh() + "<p>");

        dataTable.append(new BootstrapCard().wrapStringInCard(summary.toString()));
        return dataTable.toString();
    }

    private String getBuffettologyStep8() {
        StringBuilder builder = new StringBuilder();

        if (! yearlyFinancials.isCompanyBuyingBackShares()) {
            builder.append(new BootstrapCard().wrapStringInCard("<table class=\" bg-danger \"><tr><th>The company is NOT buying back its shares</th></tr></table>"));
        } else {
            builder.append(new BootstrapCard().wrapStringInCard("<table class=\" bg-success \"><tr><th>The company is buying back its shares</th></tr></table>"));
        }

        /*
        builder.append("     <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Base year</th>"
                + "<th>Current year</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");

        builder.append("<tr>" +
                "<td>"+new BigDecimal(yearlyFinancials.getSharesOutstanding(yearlyFinancials.getNumberOfYears()-1))+"</td>"
                +"<td>"+new BigDecimal(yearlyFinancials.getSharesOutstanding(0))+"</td>" +
                "</tr>");
        builder.append("</tbody></table>");
        return new BootstrapCard().wrapStringInCard(builder.toString());
         */
        return builder.toString();
    }

    private String getBuffettologyStep9() {
        return "";
        //return new BootstrapCard().wrapStringInCard("Is the company free to raise prices with inflation?");
    }
    private String getBuffettologyStep10() {
        return "";
        //return new BootstrapCard().wrapStringInCard("Is the company's stock price suffering from market panic? -- use analysis predictions in api.");
    }
    private String getBuffettologyStep11() {
        StringBuilder builder = new StringBuilder();
        builder.append("<h2>What is the initial rate of investment and its expected annual rate of growth?</h2>");
        builder.append("<p>The current eps is " + df.format(yearlyFinancials.getCurrentEPS()) + "</p>");
        builder.append("<p>The current share price is " + df.format(yearlyFinancials.getCurrentSharePrice()) + "</p>");

        double rateOfReturn = yearlyFinancials.getCurrentRateOfReturn() * 100;
        builder.append("<p>The current rate of return is : " +df.format(rateOfReturn) + "%</p>");

        List<FinancialAnalysis> annualEPSList =  yearlyFinancials.getEarningsList();
        builder.append(" <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Base year EPS</th>"
                + "<th>Current year EPS</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");

        try {
            builder.append("<tr><td>$"+  df.format(annualEPSList.get(annualEPSList.size()-1).getEarningsPerShare())
                    +"</td><td>$" + df.format(yearlyFinancials.getCurrentEPS())  + "</td></tr>");
            builder.append("</tbody></table>");
        } catch (Exception e) {
            builder.append("Not enough data");
        }
        builder.append("<p>The expected growth rate is: " + df.format(yearlyFinancials.getEPSAnnualRateReturn()) + "%</p>");
        return new BootstrapCard().wrapStringInCard(builder.toString());
    }

    private double getSumFutureDividendReturnOnOneDollar(int numberOfYears) {
        JSONArray annualIncomeAndDividends = yearlyFinancials.getPercentageDividendsPaidArray();
        double compoundIncomeGrowth = yearlyFinancials.getCompoundingInterestRate(annualIncomeAndDividends.getJSONObject(0).getDouble("income"),annualIncomeAndDividends.getJSONObject(annualIncomeAndDividends.length()-1).getDouble("income"));
        double percentDividend = yearlyFinancials.getAveragePercentageDividendsPaid();
        double sumDividends = 0d;
        double principle = 1d;
        for (int i=0;i<numberOfYears;i++) {
            principle  = principle + (principle * compoundIncomeGrowth * (1 - percentDividend));
            sumDividends = sumDividends + principle * percentDividend;
        }
        return sumDividends;
    }

    private String getBuffettologyStep12() {

        StringBuilder builder = new StringBuilder();
        builder.append("<h1>Income and Dividend payouts</h1>\n");
        //double averageAnnualPerShareReturnOnShareholdersEquity = yearlyFinancials.getAverageReturnOnShareholderEquity();
        double percentDividend = yearlyFinancials.getAveragePercentageDividendsPaid();
        JSONArray getPecentagePaidArray = yearlyFinancials.getPercentageDividendsPaidArray();
        builder.append(" <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Year/th>"
                + "<th>Net Income</th>"
                + "<th>Dividend</th>"
                + "<th>% Paid out</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");
        for (int i=0;i<getPecentagePaidArray.length();i++) {
            double percentPaid = Double.valueOf(df.format(getPecentagePaidArray.getJSONObject(i).get("percentPaid"))) * 100d;
            builder.append("<tr><td>"+ getPecentagePaidArray.getJSONObject(i).getString("year") + "</td>"
                    + "<td>"+df.format(new BigDecimal(getPecentagePaidArray.getJSONObject(i).getDouble("income"))) + "</td>"
                    + "<td>"+df.format(new BigDecimal(getPecentagePaidArray.getJSONObject(i).getDouble("dividend"))) + "</td>"
                    + "<td>"+  percentPaid + "%</td>"
                    + "</tr>");
        }
        builder.append("</tbody></table>\n");
        builder.append(this.getBuffettologyStep5());
        builder.append("<p>Per Share Shareholders equity: " + df.format(new BigDecimal(yearlyFinancials.getPerShareShareholdersEquity())) + "</p>\n");
        //double futurePerShareEarnings = yearlyFinancials.getFuturePerShareEarnings();
        builder.append("<p>Future EPS " +df.format(new BigDecimal(yearlyFinancials.getFuturePerShareEarnings())) + "</p>\n");
        //double averagePERatio = yearlyFinancials.getAveragePERatio();
        builder.append("<p>Average PE Ratio " + this.formatNumber(yearlyFinancials.getAveragePERatio()) + "</p>\n");
        //double futurePerShareTradingPrice = yearlyFinancials.getFuturePerShareTradingPrice();
        //this.currentPerShareTradingPrice = yearlyFinancials.getCurrentSharePrice();
        //double compoundingRateOfReturn = yearlyFinancials.getCompoundingRateOfReturn() ;
        builder.append("<p>Average annual growth rate for shareholders equity for the past "
                + yearlyFinancials.getNumberOfYears() + " years: "
                +df.format(yearlyFinancials.getEquityGrowth()*100) +"%</p>\n");

        int reportYear = LocalDate.now().getYear()+yearlyFinancials.getNumberOfYears();
        builder.append("<p>Average percentage paid as dividend: " + df.format( percentDividend*100) + "%</p>\n");
        builder.append("<p>Future per share shareholder equity: " + yearlyFinancials.getFuturePerShareValueOfShareholderEquity()+ "</p>");
        builder.append("<p>Projected future trading price  =  (Sum(EPS) - Sum(Dividends)) * PE Ratio + Shareholder Equity = $" + df.format(yearlyFinancials.getFuturePerShareTradingPrice()) + " in " + reportYear);
        builder.append("<p>Projected rate of return on trading price " + df.format(yearlyFinancials.getCompoundingRateOfReturn()) + "%</p>");
        return new BootstrapCard().wrapStringInCard(builder.toString());
    }

    private String getBuffettologyStep13() {
        StringBuilder builder = new StringBuilder();
        builder.append("<h1>Projected annual compounding rate of return using historical annual per share earnings growth figure: </h1>");
        //double baseYearEPS = yearlyFinancials.getEarliestEPS();
        //double currentYearEPS =  this.getCurrentEPS(financials);
        builder.append(" <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Base Year EPS/th>"
                + "<th>Current EPS</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");
        builder.append("<tr><td>"+df.format(yearlyFinancials.getEarliestEPS())+"</td><td>" +df.format(yearlyFinancials.getCurrentEPS()) + "</td></tr>");
        builder.append("</tbody></table>\n");
        //double compoundingGrowthRate = yearlyFinancials.getCompoundingInterestRate(yearlyFinancials.getEarliestEPS(), yearlyFinancials.getCurrentEPS());
        //double projectedPerShareEarnings = yearlyFinancials.getFutureValue(yearlyFinancials.getCurrentEPS(), yearlyFinancials.getCompoundingGrowthRate());
        //double projectedTradingPrice = yearlyFinancials.getProjectedPerShareEarnings() * yearlyFinancials.getAveragePERatio();
        builder.append("<p>Historical compounding growth rate: "
                +  df.format(yearlyFinancials.getCompoundingGrowthRate()) + "</p>\n");
        builder.append("<p>Projected per share earnings:  "
                + df.format(yearlyFinancials.getProjectedPerShareEarnings()) + "</p>\n");
        builder.append("<p>Projected per trading price:</p>  <h3>$"
                + df.format(yearlyFinancials.getProjectedTradingPrice()) + "</h3>\n");
        if (yearlyFinancials.getStartYearIndex()==0) {
            this.accuracy = yearlyFinancials.getProjectionAccuracy();
        } else {
            this.previousAccuracy = yearlyFinancials.getProjectionAccuracy();
        }
        return new BootstrapCard().wrapStringInCard(builder.toString());
    }


    private boolean isCheckSuccess() {

        if ( ! yearlyFinancials.isValidLastSharePrice()) {
            System.out.println("No last share price found");
            return false;
        }

        if (yearlyFinancials.isReturnOnShareholderEquityLow()) {
            System.out.println("Low return on shareholder equity" + yearlyFinancials.isReturnOnShareholderEquityLow());
            return  false;
        }

        if ( yearlyFinancials.getCompoundingRateOfReturn()<=0) {
            System.out.println("Low return on rate of return" + yearlyFinancials.getCompoundingRateOfReturn());
            return false;
        }
        if (Double.valueOf( yearlyFinancials.getProjectedPerShareEarnings()).isNaN()) {
            return false;
        }

        if (Double.valueOf(yearlyFinancials.getAveragePERatio()).isInfinite()
                || Double.valueOf(yearlyFinancials.getAveragePERatio()).isNaN()
                || yearlyFinancials.getAveragePERatio() <=0) {
            System.out.println("Infinite averate pe ratio" + yearlyFinancials.getAveragePERatio());
            return false;
        }
        return true;
    }

    private String formatNumber(double d) {
        if (Double.valueOf(d).isInfinite()) {
            return "-- infinite --";
        }
        try {
            return	df.format(new BigDecimal(d));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "-error-";
    }

}
