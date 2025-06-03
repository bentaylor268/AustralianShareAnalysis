package html.bootstrap4.utilities;

public class BootstrapCard {

    public String wrapStringInCard(String s) {
        return this.createCard(null,s);
    }
    public String createCard(String title, String s) {
        return this.createCard(title,s,null);
    }
    public String createCard(String title, String s, String classColor) {
        if (classColor == null) {
            classColor = "";
        }
        String header = "";
        if (title != null) {
            header = "<div class=\"card-header\">"+title+"</div>";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"row\">" +
                "    <div class=\"col\">" +
                "      <div class=\"card " + classColor + "\">" +
                header +
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
