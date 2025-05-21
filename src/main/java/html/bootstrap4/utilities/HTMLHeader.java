package html.bootstrap4.utilities;

public class HTMLHeader {

    public static String getHTMLHeader() {
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
