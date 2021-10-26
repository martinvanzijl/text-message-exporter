package com.example.exporttextmessages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.DateFormat;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EXPORT_MESSAGES = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

//    @SuppressLint("NewApi")
    public List<String> getAllSms() {
        List<String> lstSms = new ArrayList<>();

        try {
            ContentResolver cr = getContentResolver();

            // Conversations.
//            Cursor c = cr.query(Telephony.Sms.Conversations.CONTENT_URI,
//                    new String[]{Telephony.Sms.Conversations.THREAD_ID, Telephony.Sms.Conversations.MESSAGE_COUNT},
//                    null, null, Telephony.Sms.Conversations.DEFAULT_SORT_ORDER);

            String[] fields = new String[] {
                    Telephony.Sms.BODY,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.DATE,
                    Telephony.Sms.ADDRESS
            };

            // Get messages from the database.
            Cursor c = cr.query(Telephony.Sms.CONTENT_URI,
                    fields,
                    null, null, Telephony.Sms.DEFAULT_SORT_ORDER);
            int totalSMS = c.getCount();

            if (c.moveToFirst()) {
                // Loop through messages.
                for (int i = 0; i < totalSMS; i++) {
                    // Read fields.
                    String body = c.getString(0);
                    int type = c.getInt(1);
                    long dateLong = c.getLong(2);
                    String address = c.getString(3);

                    // Format date.
                    Date date = new Date(dateLong);

                    // Print message.
                    String typeName = getMessageTypeName(type);
                    String line = typeName + " at " + date + ":\n";
                    line += body;
                    line += "\nAddress: " + address;
                    line += "\n---";
                    Log.i("Message", line);

                    // Add to list.
                    lstSms.add(body);

                    // Go to next record.
                    c.moveToNext();
                }
            } else {
                Log.i("No Messages", "There are no messages.");
            }
            c.close();
        }
        catch (IllegalArgumentException e) {
            Log.e("Database", e.getLocalizedMessage());
        }

        return lstSms;
    }

    // Get name for the message type.
    private String getMessageTypeName(int type) {
        String typeName = "Unknown";
        if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            typeName = "Received";
        }
        else if (type == Telephony.Sms.MESSAGE_TYPE_SENT) {
            typeName = "Sent";
        }
        return typeName;
    }

    public void onButtonExportClick(View view) {
        // Declare required permissions.
        String[] requiredPermissions = new String[] {
                Manifest.permission.READ_SMS,
        };

        // Export text messages if permissions have been granted.
        if (checkForPermissions(requiredPermissions, REQUEST_CODE_EXPORT_MESSAGES)) {
            exportTextMessages();
        }
    }

    // Check that the required permissions are granted.
    // If not, ask for permission with the given request code.
    private boolean checkForPermissions(String[] permissions, int requestCode) {

        boolean mustAsk = false;
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                mustAsk = true;
                break;
            }
        }

        if (!mustAsk) {
            return true;
        }

        if (shouldShowRequestPermissionRationale()) {
            Log.w("Export", "The app requires these permissions to export text messages.");
        } else {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        }

        return false;
    }

    // Return true if the app should explain why it needs a set of permissions.
    private boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    // Return true if the app has the given permission.
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(
                getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    // Export SMS messages.
    private void exportTextMessages() {
        List<String> textMessages = getAllSms();
        Log.i("Export", "There are " + textMessages.size() + " messages.");
    }

    // Callback for after the user selects whether to give a required permission.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_EXPORT_MESSAGES) {
            // Check if all results were granted.
            boolean okToStart = true;
            String missingPermission = "";

            if (grantResults.length == 0) {
                okToStart = false;
            }
            else {
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        missingPermission = permissions[i];
                        okToStart = false;
                        break;
                    }
                }
            }

            // Export messages.
            if (okToStart) {
                exportTextMessages();
            }
            else {
                String message = "Could not start service, since not all permissions were granted.";
                message += "\nMissing: " + missingPermission;
                Log.w("Export", message);
            }
        }
    }
}