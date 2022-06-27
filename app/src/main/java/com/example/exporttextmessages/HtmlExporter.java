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
            // E.g. "To: Android Phone 1".
            int type = message.getType();
            String line1 = (type == MessageDetails.TYPE_SENT) ? "To: " : "From: ";
            line1 += message.getAddress();

            // E.g. "Sent at 5 pm on 2 May 2022".
            String typeName = Utils.getMessageTypeName(type);
            String line2 = typeName + " at " + message.getDate() + ":";

            // Write HTML.
            Element headerParagraph = doc.body().appendElement("p");
            headerParagraph.appendText(line1);
            headerParagraph.appendElement("br");
            headerParagraph.appendText(line2);

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
