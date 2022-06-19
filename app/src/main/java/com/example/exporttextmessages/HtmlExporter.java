package com.example.exporttextmessages;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

public class HtmlExporter {

    /**
     * Get the HTML.
     * @return The HTML.
     */
    public String getHtml(List<MessageDetails> list) {

        // Create document.
        Document doc = Jsoup.parse("<html></html>");
        doc.title("Exported Text Messages");
        doc.body().appendElement("h2").text("Exported Text Messages");

        // Export messages.
        for (MessageDetails message: list) {

            // Create header.
            StringBuilder builder = new StringBuilder();
            if (message.getType() == MessageDetails.TYPE_SENT) {
                builder.append("Sent to ");
            }
            else {
                builder.append("Received from ");
            }
            builder.append(message.getAddress());
            builder.append(" at ");
            builder.append(message.getDate().toString());
            builder.append(":");

            // Write HTML.
            doc.body().appendElement("p").appendText(builder.toString());
            Element bodyParagraph = doc.body().appendElement("p");

            boolean firstLine = true;
            for (String line: message.getBody().split("\n")) {
                if (!firstLine) {
                    bodyParagraph.appendElement("br");
                }
                bodyParagraph.appendText(line);
                firstLine = false;
            }

            // Write separator.
            doc.body().appendElement("hr");
        }

        // Return HTML.
        return doc.toString();
    }
}
