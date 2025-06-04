package org.example;

import australian.share.analysis.ASXCodes;
import australian.share.analysis.CalculationUtilities;
import australian.share.analysis.CompanyFinancials;
import australian.share.analysis.FinancialAnalysis;
import data.rest.client.EODHDRestClient;
import file.utilities.DataFileUtilities;
import file.utilities.WorkingDirectory;
import html.bootstrap4.utilities.BootstrapCard;
import html.bootstrap4.utilities.HTMLHeader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Buffetology {
    private static final Logger logger = Logger.getLogger("Buffetology-logger");
    private double TARGET_COMPOUND_PERCENT_RETURN = 8d;
    private int NUMBER_OF_YEARS = 10;
    private String asxCode;
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private double accuracy = 0d;
    private double previousAccuracy = 0d;

    private static int MINUMUM_YEARS = 9;

    private CompanyFinancials companyFinancials;
    private double projectedDividends;

    public static void main(String[] args)  {
         boolean isReadFile = true;
         List<String> validCodes = Arrays.asList("ANZ","ARB","ASX","BKW","MAQ","MFG","MQG","NDQ","PME","RMD");
       // List<String> validCodes = Arrays.asList("BKW");

        List<String> asxCodes = ASXCodes.getASXCodes();
        for (String asxCode : asxCodes) {
                if (! validCodes.contains(asxCode)) {
                    continue;
                }
                if (isReadFile) {
                    new Buffetology(asxCode).generateCompanyReport();
                    continue;
                }

                EODHDRestClient eodhdRestClient = new EODHDRestClient();
                try {
                     eodhdRestClient.getCompanyDataDump(asxCode);
                } catch (Exception e) {
                     logger.log(Level.SEVERE,"Data dump for " + asxCode + " failed " + e.getMessage());
                     continue;
                }
                logger.log(Level.INFO,"Data dump for " + asxCode + " has been completed");
            }
    }

    public Buffetology(String asxCode) {
        try {
            this.asxCode = asxCode;
            this.companyFinancials = new CompanyFinancials(this.asxCode, 0, MINUMUM_YEARS);
            this.projectedDividends = this.getProjectedDividendPayout(this.getProjectedIncome(companyFinancials.getCurrentNetIncome(), companyFinancials.getEquityGrowth(), companyFinancials.getAveragePercentageDividendsPaid(), companyFinancials.getNumberOfShares(), this.MINUMUM_YEARS));
        } catch (Exception e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
        }

    }

    public boolean generateCompanyReport() {
        StringBuilder html = new StringBuilder(HTMLHeader.getHTMLHeader() + "<body>");
        LocalDate date =  LocalDate.now().minusYears(0);
        html.append("<p>From: ").append(date).append("</p>");
        try {
            html.append(this.getCompanyHeader());
            if (! this.isEnoughHistory()) {
                html.append("Not enough history");
                new DataFileUtilities().writeFile(new WorkingDirectory().getWorkingDirectory() + "results/" +this.getErrorResultDirectory() + "/" + this.asxCode +"-" + MINUMUM_YEARS + ".html", html.toString());
                return true;
            }
            if (this.isCheckSuccess().has("error")) {
                html.append(this.isCheckSuccess().getString("error"));
                new DataFileUtilities().writeFile(new WorkingDirectory().getWorkingDirectory() + "results/" + this.getErrorResultDirectory() + "/" +  this.asxCode + MINUMUM_YEARS + ".html", html.toString());
                return true;
            }

            double averagePrice = (this.getCompanyFinancials().getProjectedTradingPrice() + this.getCompanyFinancials().getFuturePerShareTradingPrice()) / 2d;
            double totalReturn = averagePrice + this.projectedDividends;
            double compoundingRateOfReturn = new CalculationUtilities().getCompoundingInterestRate(this.getCompanyFinancials().getCurrentSharePrice(), totalReturn,this.NUMBER_OF_YEARS)*100d;
            double targetBuyPrice = new CalculationUtilities().calculatePresentValue(averagePrice,this.TARGET_COMPOUND_PERCENT_RETURN, this.NUMBER_OF_YEARS, 1);

            StringBuilder summary = new StringBuilder();
            summary.append("<h2>Summary</h2>" + "     <table class=\"table table-striped\">" + "<thead>" + "<tr>" + "<th>Book value</th>" + "<th>Method 1</th>" + "<th>Method 2</th>" + "<th>Average</th>" + "<th>Dividends</th>" + "<th>Total Return %</th>" + "<th>Compounding Rate of Return</th>" + "<th>Current price</th>" + "<th>Target Buy Price for ").append(this.TARGET_COMPOUND_PERCENT_RETURN).append("%</th>").append("</thead>").append("</tr>").append("<tbody>");

            summary.append("<tr>" + "<td>$")
                    .append(this.getCompanyFinancials().getBookValue())
                    .append("</td>")
                    .append("<td>$")
                    .append(this.formatNumber(this.getCompanyFinancials().getFuturePerShareTradingPrice()))
                    .append("</td>"
                    // method 2
            ).append("<td>$")
                    .append(this.formatNumber(this.getCompanyFinancials().getProjectedTradingPrice()))
                    .append("</td>")
                    .append("<td>$").append(this.formatNumber(averagePrice)).append("</td>")
                    .append("<td><b>$")               .append(this.formatNumber(this.projectedDividends))                   .append("</b></td>")
                    .append("<td><b>").append(this.formatNumber((averagePrice + this.projectedDividends - this.getCompanyFinancials().getCurrentSharePrice()) / this.getCompanyFinancials().getCurrentSharePrice() * 100d)) .append("%</b></td>")
                    .append("<td><b>") .append(this.formatNumber(compoundingRateOfReturn)) .append("%</b></td>")
                    .append("<td>$")  .append(this.getCompanyFinancials().getCurrentSharePrice())     .append("</td>")
                    .append("<td><b>$")   .append(this.formatNumber(targetBuyPrice))  .append("</b></td>")
                    .append("</tr>")
                    .append("</tbody></table>");
            html.append(new BootstrapCard().createCard("Summary", summary.toString(),this.getReportColorFlag(this.getCompanyFinancials().getCurrentSharePrice(),targetBuyPrice)));

            String header = new BootstrapCard().wrapStringInCard(html.toString());
            html = new StringBuilder(header);
            html.append(this.prepareReportPeriod());
            html.append("</body></html>");
            new DataFileUtilities().writeFile(new WorkingDirectory().getWorkingDirectory() + "results/" +this.getResultDirectory(this.getCompanyFinancials().getCurrentSharePrice(), targetBuyPrice) + "/" +  this.asxCode +"-" + MINUMUM_YEARS + ".html", html.toString());
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Error with " +  this.asxCode);
            return false;
        }
    }

    private String getResultDirectory(double currentPrice, double targetPrice) {
        if (targetPrice >= currentPrice) {
            return "recommended";
        }
        if (targetPrice >= currentPrice - (currentPrice * .2d)) {
            return "suggested";
        }
        return "not-recommended";
    }
    private String getErrorResultDirectory() {
        return "error";
    }
    private String getReportColorFlag(double currentPrice, double targetPrice) {
        if (targetPrice >= currentPrice) {
            return " bg-success ";
        }
        if (targetPrice >= currentPrice - (currentPrice * .2d)) {
            return " bg-warning ";
        }
        return "bg-danger";

    }


    public CompanyFinancials getCompanyFinancials() {
        return this.companyFinancials;
    }


    private StringBuilder prepareReportPeriod() {

        String html = this.getBuffettologyStep12() +
                this.getBuffettologyStep13() +
                this.getBuffettologyStep6() +
                this.getBuffettologyStep8() +
                this.getBuffettologyStep11();
        return new StringBuilder(html);
    }

    private boolean isEnoughHistory() {
        JSONArray historyArray = this.companyFinancials.getEPSHistory();
        if (historyArray == null || this.companyFinancials==null || this.companyFinancials.getNumberOfYears()<=0) {
            return false;
        }

        if (historyArray.length()<(this.companyFinancials.getNumberOfYears()-1)) {
            System.out.println("Array size is " + historyArray.length() +  "number of years is " + this.companyFinancials.getNumberOfYears());
            System.out.println(historyArray);
            return false;
        }
        return true;
    }

    private String getCompanyHeader() {
        String builder = "<h1>" + this.companyFinancials.getCompanyCode() + ":"
                + this.companyFinancials.getCompanyName()
                + "</h1>\n" +
                "<p>" + companyFinancials.getCompanyDescription()
                + "</p>\n";
        return builder;

    }

    private int getDisplayYear(String dateString) {
       return Integer.parseInt(dateString.substring(0, 4)) -1;
    }
    private String getBuffettologyStep5() {
        StringBuilder builder = new StringBuilder();
        JSONArray epsHistoryArray = this.companyFinancials.getEPSHistory();
        logger.log(Level.SEVERE,"THE EPS HISTORY IS " + this.companyFinancials.getEPSHistory());
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
            builder.append("<tr>" + "<td>").append(this.getDisplayYear(epsYear.getString("year"))).append("</td>").append("<td>").append(epsYear.getDouble("eps")).append("</td>").append("</tr>");
        }
        builder.append("<tr>" + "<td>Average Earnings per share</td>" + "<td>").append(this.formatNumber(companyFinancials.getAverageEPS())).append("</td>").append("</tr>");
        builder.append("</tbody>"  +
                "          </table>"
               );
        return new BootstrapCard().wrapStringInCard(builder.toString());
    }

    private String getBuffettologyStep6() {
        StringBuilder builder = new StringBuilder();
        builder.append("<h2>Is the company consistently earning more than 15% on shareholders equity?</h2>" +
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

        JSONArray equityArray = this.companyFinancials.getReturnOnShareholderEquity();
        for (int i=0;i<equityArray.length();i++) {

            JSONObject equity = equityArray.getJSONObject(i);
            if (! equity.has("netIncome") ||
                    ! equity.has("totalStockholderEquity") ||
                    ! equity.has("shareholderReturnOnEquity")) {
                continue;
            }
            builder.append("<tr>" + "<td>").append(this.getDisplayYear(equity.getString("year"))).append("</td>").append("<td>$").append(df.format(new BigDecimal(equity.getDouble("netIncome")))).append("</td>").append("<td>$").append(df.format(new BigDecimal(equity.getDouble("totalStockholderEquity")))).append("</td>").append("<td>").append(df.format(new BigDecimal(equity.getDouble("shareholderReturnOnEquity") * 100d))).append("%</td>").append("</td></tr>");

        }
        builder.append("</tbody></table>");
       // StringBuilder dataTable = new StringBuilder(new BootstrapCard().wrapStringInCard(builder.toString()));
        StringBuilder dataTable = new StringBuilder();

        String summary = "<table class=\"table table-striped\">" +
                "<thead>" +
                "<tr>" +
                "<th>Average return on shareholder equity</th>" +
                "<th>" + df.format(companyFinancials.getAverageReturnOnShareholderEquity() * 100) + "%</th>" +
                "</tr>" +
                "</table>";
        String bgColor = "bg-danger text-white";
        if (companyFinancials.getAverageReturnOnShareholderEquity() *100>10d) {
            bgColor = "bg-warning text-white";
        }
        if (companyFinancials.isReturnOnShareholderEquityHigh()) {
            bgColor = "bg-success text-white";
        }
        dataTable.append(new BootstrapCard().createCard("<h2>Return on shareholder equity</h2>", summary, bgColor));
        dataTable.append(new BootstrapCard().wrapStringInCard(builder.toString()));
        return dataTable.toString();
    }

    private String getBuffettologyStep8() {
        StringBuilder builder = new StringBuilder();

        if (! companyFinancials.isCompanyBuyingBackShares()) {
            builder.append(new BootstrapCard().createCard("<h2>Share buybacks</h2>", "The company is NOT buying back its shares","bg-danger text-white"));
        } else {
            builder.append(new BootstrapCard().createCard("<h2>Share buybacks</h2>", "The company is buying back its shares","bg-success text-white"));
        }

        return builder.toString();
    }
    private String getBuffettologyStep11() {
        double rateOfReturn = companyFinancials.getCurrentRateOfReturn() * 100;

        String builder = "<table class=\"table table-striped\">" +
                "<thead>" +
                "<tr>" +
                "<th>Current share price</th><th>Rate of return</th>" +
                "</tr>" +
                "<tr>" +
                "<td>$" + df.format(companyFinancials.getCurrentSharePrice()) + "</td><td>" + df.format(rateOfReturn) + "%</td>" +
                "</tr>" +
                "</table>" +
                this.getEPSTable() +
                "<h2>The expected growth rate is: " + df.format(companyFinancials.getEPSAnnualRateReturn() * 100) + "%</h2>";
        return new BootstrapCard().createCard("<h2>Expected annual rate of growth</h2>", builder);
    }

    private String getEPSTable() {
        List<FinancialAnalysis> annualEPSList =  companyFinancials.getEarningsList();
        StringBuilder builder = new StringBuilder();
        builder.append(" <table class=\"table table-striped\">"  +
                "<thead>"
                +"<tr>"
                + "<th>Base year EPS</th>"
                + "<th>Current year EPS</th>"
                + "</thead>"
                +"</tr>"
                + "<tbody>");
        try {
            builder.append("<tr>" + " <td>$").append(df.format(annualEPSList.get(annualEPSList.size() - 1).getEarningsPerShare())).append("</td>").append("<td>$").append(df.format(companyFinancials.getCurrentEPS())).append("</td>").append("</tr>");
            builder.append("</tbody></table>");
        } catch (Exception e) {
            builder.append("Not enough data");
        }



        return builder.toString();
    }

    private double getSumFutureDividendReturnOnOneDollar(int numberOfYears) {
        JSONArray annualIncomeAndDividends = companyFinancials.getPercentageDividendsPaidArray();
        double compoundIncomeGrowth = new CalculationUtilities().getCompoundingInterestRate(annualIncomeAndDividends.getJSONObject(0).getDouble("income"),annualIncomeAndDividends.getJSONObject(annualIncomeAndDividends.length()-1).getDouble("income"),numberOfYears);
        double percentDividend = companyFinancials.getAveragePercentageDividendsPaid();
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

        //double averageAnnualPerShareReturnOnShareholdersEquity = companyFinancials.getAverageReturnOnShareholderEquity();

        int reportYear = LocalDate.now().getYear()+companyFinancials.getNumberOfYears();

        builder.append("<h2>").append(reportYear).append(" estimates</h2>");
        builder.append(" <table class=\"table table-striped\">");
        builder.append("<tr>");
        builder.append("<th>Future per share shareholder equity</th>");
        builder.append("<th>Projected rate of return on trading price</th>");
        builder.append("<th>Projected future trading price  =  (Sum(EPS) - Sum(Dividends)) * PE Ratio + Shareholder Equity</th>");
        builder.append("<th>Total expected dividends</th>");
        builder.append("</tr>");
        builder.append("<tr>");
        builder.append("<td>$").append(df.format(companyFinancials.getFuturePerShareValueOfShareholderEquity())).append("</td>");
        builder.append("<td>").append(df.format(companyFinancials.getCompoundingRateOfReturn() * 100d)).append("%</td>");
        builder.append("<td>$").append(df.format(companyFinancials.getFuturePerShareTradingPrice())).append("</td>");
        builder.append("<td>$").append(df.format(this.projectedDividends)).append("</td>");
        builder.append("</tr>");
        builder.append("</table>");

        return new BootstrapCard().wrapStringInCard(builder.toString());
    }

    private static double getProjectedDividend(double netIncome, double avgPaid, double numberOfShares) {
        return netIncome/numberOfShares * avgPaid;
    }

    private static double getProjectedNetIncome(double netIncome, double growthRate) {
        return netIncome + (netIncome * growthRate);
    }


    public JSONArray getProjectedIncome(double currentIncome, double growthRate, double averageDividendPercent, double numberOfShares, int numberOfYears) {
        JSONArray response = new JSONArray();
        double income = currentIncome;
        for (int i=0;i<numberOfYears;i++) {
            JSONObject values = new JSONObject();
            income = getProjectedNetIncome(income, growthRate);
            double dividend = getProjectedDividend(income, averageDividendPercent, numberOfShares);

            values.put("income",income);
            values.put("dividend",0d);
            values.put("yearOffset", i+1);
            values.put("numberOfShares", numberOfShares);
            values.put("percentPaid", averageDividendPercent);
            values.put("dividendPerShare", dividend);
            response.put(values);
        }
        return response;
    }

    private double getProjectedDividendPayout(JSONArray projectedIncomes) {
        double sumDividend = 0d;
        for (int i=0;i<projectedIncomes.length();i++) {
            sumDividend = sumDividend + projectedIncomes.getJSONObject(i).getDouble("dividendPerShare");
        }
        return sumDividend;
    }

    private String getProjectedDividendRows(JSONArray projectedIncomes, int year) {
        StringBuilder builder = new StringBuilder();
        for (int i=projectedIncomes.length()-1;i>=0;i--) {
            JSONObject income = projectedIncomes.getJSONObject(i);
            int displayYear = year + income.getInt("yearOffset");
            double percentPaid = Double.parseDouble(df.format(income.get("percentPaid"))) * 100d;
            builder.append("<tr> <td>").append(displayYear).append("</td>").append("<td>$").append(df.format(new BigDecimal(income.getDouble("income")))).append("</td>").append("<td>$").append(df.format(new BigDecimal(income.getDouble("dividend")))).append("</td>").append("<td>").append(df.format(income.getDouble("numberOfShares"))).append("</td>").append("<td>").append(df.format(percentPaid)).append("%</td>").append("<td>$").append(df.format(income.getDouble("dividendPerShare"))).append("</td>").append("</tr>");
        }
         return builder.toString();

    }

    private String getIncomeAndDividendsTable() {

        String builder = "<h1>Income and Dividend payouts</h1>" +
                " <table class=\"table table-striped\">" +
                "<thead>"
                + "<tr>"
                + "<th>Year</th>"
                + "<th>Net Income</th>"
                + "<th>Dividend</th>"
                + "<th>Shares on issue</th>"
                + "<th>% Paid out</th>"
                + "<th>Per share</th>"
                + "</thead>"
                + "</tr>"
                + "<tbody>" +

                // the number of shares is not correct. need to get latest number of shares here
                this.getProjectedDividendRows(this.getProjectedIncome(companyFinancials.getCurrentNetIncome(), companyFinancials.getEquityGrowth(), companyFinancials.getAveragePercentageDividendsPaid(), companyFinancials.getNumberOfShares(), 10), 2024) +
                this.getActualDividendsAndIncomeRows() +
                "</tbody>" +
                "</table>\n";
        return builder;
    }

    private String getActualDividendsAndIncomeRows() {
        JSONArray getPecentagePaidArray = companyFinancials.getPercentageDividendsPaidArray();
        StringBuilder builder = new StringBuilder();
        for (int i=0;i<getPecentagePaidArray.length();i++) {
            double percentPaid = Double.parseDouble(df.format(getPecentagePaidArray.getJSONObject(i).get("percentPaid"))) * 100d;
            builder.append("<tr>" + " <td>").append(this.getDisplayYear(getPecentagePaidArray.getJSONObject(i).getString("year"))).append("</td>").append("<td>$").append(df.format(new BigDecimal(getPecentagePaidArray.getJSONObject(i).getDouble("income")))).append("</td>").append("<td>$").append(df.format(new BigDecimal(getPecentagePaidArray.getJSONObject(i).getDouble("dividend")))).append("</td>").append("<td>").append(df.format(getPecentagePaidArray.getJSONObject(i).getDouble("numberOfShares"))).append("</td>").append("<td>").append(df.format(percentPaid)).append("%</td>").append("<td>$").append(df.format(getPecentagePaidArray.getJSONObject(i).getDouble("dividendPerShare"))).append("</td>").append("</tr>");
        }
        return builder.toString();
    }

    private String getBuffettologyStep13() {
        StringBuilder builder = new StringBuilder();
        //builder.append("<h1>Projected annual compounding rate of return using historical annual per share earnings growth figure: </h1>");
        builder.append(this.getEPSTable());
        builder.append(" <table class=\"table table-striped\">");
        builder.append("<tr>");
        builder.append("<th>Historical compounding growth rate</th>");
        builder.append("<th>Projected per share earnings</th>");
        builder.append("<th>Projected per trading price</th>");
        builder.append("</tr>");
        builder.append("<tr>");
        builder.append("<td>").append(df.format(companyFinancials.getCompoundingGrowthRate() * 100d)).append("%</td>");
        builder.append("<td>$").append(df.format(companyFinancials.getProjectedPerShareEarnings())).append("</td>");
        builder.append("<td>$").append(df.format(companyFinancials.getProjectedTradingPrice())).append("</td>");
        builder.append("</tr>");
        builder.append("</table>");
        builder.append(this.getIncomeAndDividendsTable());
        double percentDividend = companyFinancials.getAveragePercentageDividendsPaid();

        builder.append(this.getBuffettologyStep5());
        builder.append(" <table class=\"table table-striped\">");
        builder.append("<tr>");
        builder.append("<th>Per share equity</th>" +
                "<th>Future EPS</th>" +
                "<th>Average PE Ratio</th>" +
                "<th>Average past growth rate</th>" +
                "<th>Average % dividend</th>"
        );
        builder.append("</tr>");
        builder.append("<tr>");
        builder.append("<td>$").append(df.format(new BigDecimal(companyFinancials.getPerShareShareholdersEquity()))).append("</td>");
        builder.append("<td>$").append(df.format(new BigDecimal(companyFinancials.getFuturePerShareEarnings()))).append("</td>");
        builder.append("<td>").append(this.formatNumber(companyFinancials.getAveragePERatio())).append("</td>");
        builder.append("<td>").append(companyFinancials.getNumberOfYears()).append(" years: ").append(df.format(companyFinancials.getEquityGrowth() * 100)).append("%</td>");
        builder.append("<td>").append(df.format(percentDividend * 100)).append("%</td>");
        builder.append("</tr>");
        builder.append("</table>");

        return new BootstrapCard().wrapStringInCard(builder.toString());
    }


    private JSONObject isCheckSuccess() {
        JSONObject response = new JSONObject();
        if ( ! companyFinancials.isValidLastSharePrice()) {
            response.put("error", "No last share price found");
            return response;
        }

        if ( companyFinancials.getCompoundingRateOfReturn()<=0) {
            response.put("error", "Compounding rate of return <-0");
            return response;
        }
        if (Double.valueOf( companyFinancials.getProjectedPerShareEarnings()).isNaN()) {
            response.put("error", "getProjectedPerShareEarnings - last earnings per share is negative");
            return response;

        }

        if (Double.valueOf(companyFinancials.getAveragePERatio()).isInfinite()
                || Double.valueOf(companyFinancials.getAveragePERatio()).isNaN()
                || companyFinancials.getAveragePERatio() <=0) {
            response.put("error", "Infinite averate pe ratio" + companyFinancials.getAveragePERatio());
            return response;
        }
        return response;
    }

    private String formatNumber(double d) {
        if (Double.valueOf(d).isInfinite()) {
            return "-- infinite --";
        }
        try {
            return	df.format(new BigDecimal(d));
        } catch (Exception e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
        }
        return "-error-";
    }

}
