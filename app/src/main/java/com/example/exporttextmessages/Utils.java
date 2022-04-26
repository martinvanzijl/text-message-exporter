package com.example.exporttextmessages;

import android.provider.Telephony;

/**
 * Utility functions for the program.
 */
public class Utils {
    // Get name for the message type.
    static String getMessageTypeName(int type) {
        String typeName = "Unknown";
        if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            typeName = "Received";
        }
        else if (type == Telephony.Sms.MESSAGE_TYPE_SENT) {
            typeName = "Sent";
        }
        return typeName;
    }
}
