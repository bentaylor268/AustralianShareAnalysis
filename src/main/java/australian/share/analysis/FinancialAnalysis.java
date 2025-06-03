package australian.share.analysis;


import org.json.JSONArray;
import org.json.JSONObject;

public class FinancialAnalysis {
	
	private String year;
	private JSONObject financialObject;
	
	public FinancialAnalysis(String year, JSONObject financialObject) {
		this.year = year;
		this.financialObject = financialObject;
	}
	
	public String getYear() {
		return year;
	}
	
	public String getLiabilities() {
		return financialObject.getString("totalCurrentLiabilities");
	}
	public double getNetDebt() {
		if (financialObject.isNull("netDebt")) {
			return 0;
		}
		
		return Double.valueOf(financialObject.getString("netDebt")).doubleValue();
	}
	
	public double getNetIncome(JSONArray returnOnShareholderEquity) {
		if (! financialObject.isNull("netIncome")) {
			return Double.valueOf(financialObject.getString("netIncome")).doubleValue();
		}
			 
		 for (int i=0;i<returnOnShareholderEquity.length();i++) {

			 JSONObject equity = returnOnShareholderEquity.getJSONObject(i);
			 if (! equity.has("netIncome")) {
				 continue;
			 }
			 return equity.getDouble("netIncome");
		 }
		return 0;
	}
	
	public double getStockholderEquity() {
		if (financialObject.isNull("totalStockholderEquity")) {
			return 0;
		}
		//System.out.println(financialObject.get("totalStockholderEquity"));
		return Double.valueOf(financialObject.getString("totalStockholderEquity")).doubleValue();
	}
	public String getValue(String key) {
		return financialObject.getString(key);
	}
	public double getEarningsPerShare() {
		return financialObject.getDouble("epsActual");
	}
	
	public long getSharesOutstanding() {
		if (financialObject.isNull("commonStockSharesOutstanding")) {
			return -1;
		}
		return Double.valueOf(financialObject.getDouble("commonStockSharesOutstanding")).longValue();
	}
	public double getDividendPaid() {
		if (!financialObject.has("dividendsPaid") || financialObject.isNull("dividendsPaid")) {
			return 0d;
		}
		double rv = Double.valueOf(financialObject.getString("dividendsPaid")).doubleValue();
		if (rv < 0 ) {
			return rv *-1;
		}
		return rv;
	}
	
	public double getEarningsMinusDividend(JSONArray returnOnShareholderEquity) {
		return this.getNetIncome(returnOnShareholderEquity) - this.getDividendPaid();
	}
	
	public double getDividendPaymentPercentage(JSONArray returnOnShareholderEquity) {
		if (this.getDividendPaid() ==0) {
			return 0;
		}
		if (this.getNetIncome(returnOnShareholderEquity)==0) {
			return 0;
		}
		
		double d =  this.getDividendPaid()/this.getNetIncome(returnOnShareholderEquity);
		if (Double.valueOf(d).isInfinite()) {
			return 0;
		}
		return this.getDividendPaid()/this.getNetIncome(returnOnShareholderEquity);
	}
	
	public void setShareholderReturnOnEquity(double roe) {
		this.financialObject.put("roe", roe);
	}
	public double getShareholderReturnOnEquity() {
		return this.financialObject.getDouble("roe");
		
	}
	
	public double getPercentageDividendOfReturnOnEquity() {
		return this.getDividendPaid()/this.getShareholderReturnOnEquity();
	}
	
	public double getNumberOfShares() {
		if (!this.financialObject.has("commonStockSharesOutstanding") ) {
			System.out.println("about to return 0 shares for " + this.financialObject);
			return 0;
		}
		return Double.valueOf(this.financialObject.getString("commonStockSharesOutstanding")).doubleValue();
	
	}
	
	public double getShareholderEquityPerShare() {
		return this.getStockholderEquity()/this.getNumberOfShares();
	}
	
}
