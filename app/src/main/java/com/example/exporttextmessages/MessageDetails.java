package com.example.exporttextmessages;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Date;

/**
 * Represents details of a text message.
 */
public class MessageDetails {
    private String mBody;
    private int mType;
    private String mAddress;
    private Date mDate;

    public MessageDetails(String body, int type, String address, Date date) {
        mBody = body;
        mType = type;
        mAddress = address;
        mDate = date;
    }

    public String getBody() {
        return mBody;
    }

    public int getType() {
        return mType;
    }

    public String getAddress() {
        return mAddress;
    }

    public Date getDate() {
        return mDate;
    }

    /**
     * Add self to XML document.
     * @param doc The document.
     * @param parentElement The parent element.
     */
    public void addToXml(Document doc, Element parentElement) {
        Element messageElement = doc.createElement("message");
        messageElement.setAttribute("type", String.valueOf(mType));
        messageElement.setAttribute("contact", mAddress);
        messageElement.setAttribute("date", mDate.toString());
        Element bodyElement = doc.createElement("body");
        bodyElement.appendChild(doc.createTextNode(mBody));
        messageElement.appendChild(bodyElement);
        parentElement.appendChild(messageElement);
    }
}
