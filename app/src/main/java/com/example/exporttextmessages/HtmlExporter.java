package com.example.exporttextmessages;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
            doc.body().appendElement("p").appendText(message.getBody());

            // Write separator.
            doc.body().appendElement("hr");
        }

        // Return HTML.
        return doc.toString();
    }
}
