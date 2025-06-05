package html.bootstrap4.utilities;

import org.json.JSONObject;

import java.time.LocalDate;

public class ReportIndexPage {

    private StringBuilder html;
    private StringBuilder recommended;
    private StringBuilder notRecommended;
    private StringBuilder suggested;
    private StringBuilder error;

    public ReportIndexPage() {
        html = new StringBuilder(HTMLHeader.getHTMLHeader());
        html.append("<body>");
        html.append("<h2>Buffetology - Australian Share Analysis</h2>");
        html.append("<h4>" + LocalDate.now().toString() + "</h4>");
        recommended = new StringBuilder(this.getTableHeader());
        notRecommended = new StringBuilder(this.getTableHeader());
        suggested = new StringBuilder(this.getTableHeader());
        error = new StringBuilder(this.getTableHeader());
    }

    public String getIndexPage() {
        recommended.append("</table>");
        notRecommended.append("</table>");
        suggested.append("</table>");
        error.append("</table>");
        html.append(new BootstrapCard().createCard("Recommended",recommended.toString(),"bg-success  text-white"));
        html.append(new BootstrapCard().createCard("Suggested",suggested.toString(),"bg-warning  text-white"));
        html.append(new BootstrapCard().createCard("Not recommended",notRecommended.toString(),"bg-danger  text-white"));
        html.append(new BootstrapCard().createCard("Error",error.toString(),"bg-danger  text-white"));

        html.append("</body></html>");

        return html.toString();
    }
    private String getTableHeader() {
        return "<table class=\"table table-striped\">" +
                "<tr class=' table-primary'>" +
                "<th>Code</th>" +
                "<th>Company Name</th>" +
                "<th>Current Price</th>" +
                "<th>Target Price</th>" +
                "</tr>";
    }
    public void addReport(JSONObject reportObject) {
        // asxCode,reportObject.getString("status"), reportObject.getString("fileName")
          String companyRow =  "<tr>" +
                  "<td><a href='"+reportObject.getString("fileName")+"'>"+reportObject.getString("asxCode") +"</a></td>" +
                  "<td>"+ reportObject.getString("companyName")+"</a></td>" +
                  "<td>$"+ reportObject.getDouble("currentPrice") + "</td>" +
                  "<td>$"+ reportObject.getDouble("targetBuyPrice") + "</td>" +
                  "</tr>";
          if (reportObject.getString("status").equalsIgnoreCase("recommended")) {
              recommended.append(companyRow);
              return;
          }
          if (reportObject.getString("status").equalsIgnoreCase("not-recommended")) {
              notRecommended.append(companyRow);
              return;
           }
         if (reportObject.getString("status").equalsIgnoreCase("suggested")) {
             suggested.append(companyRow);
             return;
        }
        error.append(companyRow);
    }
    public String getHTMLHeader() {
        return "<!DOCTYPE html>\n" +
                //"<%@ page language=\"java\" contentType=\"text/html; charset=ISO-8859-1\"\n" +
                //"    pageEncoding=\"ISO-8859-1\"%>\n" +
                "<sh pihtml>\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\" />\n" +
                "<title>Buffetology</title>\n" +
                "  <!-- Bootstrap CSS CDN -->\n" +
                " <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n" +
                " \n" +
                "  <script src=\"https://cdn.jsdelivr.net/npm/jquery@3.7.1/dist/jquery.slim.min.js\"></script>\n" +
                "  <script src=\"https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js\"></script>\n" +
                "  <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/js/bootstrap.bundle.min.js\"></script>\n" +
                "    \n" +
                "  <!-- Chart.js CDN -->\n" +
                "  <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n" +
                "  \n" +
                "  <script src=\"js/jquery-1.8.3.js\"></script>\n" +
                "<script src=\"js/jquery-ui-1.9.2.custom.js\"></script>\n" +
                "\n" +
                "  <link href=\"css/spinner.css\" rel=\"stylesheet\" type=\"text/css\" />\n" +
                "\n" +
                "</head>\n";

    }
}
