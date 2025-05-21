package html.bootstrap4.utilities;

public class BootstrapCard {

    public String wrapStringInCard(String s) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"row\">" +
                "    <div class=\"col\">" +
                "      <div class=\"card\">" +
                "        <div class=\"card-body\">" +
                "          <table class=\"table\">" +
                "            <tbody>"
                + "<tr>"
                + "<td>" + s + "</td>"
                + "</tr>"
                + "            </tbody>" +
                "          </table>" +
                "        </div>" +
                "      </div>" +
                "    </div>");
        return builder.toString();
    }
}
