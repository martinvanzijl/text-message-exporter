package com.example.exporttextmessages;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EXPORT_MESSAGES = 1000;
    private static final int REQUEST_CODE_CHOOSE_CONTACT = 2000;
    private String m_filterContact = "";
    private String m_filterContactDisplayName = "";
    private Date m_filterStartDate = null;
    private Date m_filterEndDate = null;
    // A cache for contact names. This does make a difference in speed.
    private Map<String, String> mContactNames = new HashMap<>();

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

            // Apply date filter.
            QueryMaker queryMaker = new QueryMaker();
//            String selection = null;
//            String[] selectionArgs = null;
            if (m_filterStartDate != null) {
//                selection = "date >= ?";
//                selectionArgs = new String[] {String.valueOf(m_filterStartDate.getTime())};
                queryMaker.addQuery("date >= ?", String.valueOf(m_filterStartDate.getTime()));
            }
            if (m_filterEndDate != null) {
//                selection = "date <= ?";
//                selectionArgs = new String[] {String.valueOf(m_filterEndDate.getTime())};
                queryMaker.addQuery("date <= ?", String.valueOf(m_filterEndDate.getTime()));
            }

            String selection = queryMaker.getSelection();
            String[] selectionArgs = queryMaker.getSelectionArgs();

            // Get messages from the database.
            Cursor c = cr.query(Telephony.Sms.CONTENT_URI,
                    fields,
                    selection, selectionArgs, Telephony.Sms.DEFAULT_SORT_ORDER);
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

                    // Apply contact filter.
                    boolean exportMessage = true;
                    if (!m_filterContact.isEmpty()) {
                        if (!PhoneNumberUtils.compare(address, m_filterContact)) {
                            exportMessage = false;
                        }
                    }

                    // Add to list.
                    if (exportMessage) {
                        String text = formatMessage(body, type, address, date);
                        lstSms.add(text);
                    }

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

    private String formatMessage(String body, int type, String address, Date date) {
        String typeName = getMessageTypeName(type);
        String line = typeName + " at " + date + ":\n";
        line += body;
        String contact = m_filterContact.isEmpty() ? getContactDisplayName(address) : m_filterContactDisplayName;
        line += "\nContact: " + contact;
        line += "\n---";
        return line;
    }

    // Return contact display name for the given phone number.
    private String getContactDisplayName(String phoneNumber) {
        // Check if name is cached.
        if (mContactNames.containsKey(phoneNumber)) {
            return mContactNames.get(phoneNumber);
        }

        // Default to the phoneNumber.
        String contactName = phoneNumber;

        // Check for permissions first.
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            mContactNames.put(phoneNumber, contactName);
            return contactName;
        }

        // Read the contact name from the database.
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        String[] projection = new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME };

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName = cursor.getString(0);
            }
            cursor.close();
        }

        mContactNames.put(phoneNumber, contactName);
        return contactName;
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
        writeExportFile(textMessages);
    }

    // Callback for after the user selects whether to give a required permission.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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

        // Do the action.
        if (requestCode == REQUEST_CODE_EXPORT_MESSAGES) {
            if (okToStart) {
                exportTextMessages();
            }
            else {
                String message = "Could not export messages.";
                message += "\nMissing: " + missingPermission;
                Log.w("Export", message);
            }
        }
        else if (requestCode == REQUEST_CODE_CHOOSE_CONTACT) {
            if (okToStart) {
                chooseFilterContact();
            }
            else {
                String message = "Could not choose contact.";
                message += "\nMissing: " + missingPermission;
                Log.w("Choose Contact", message);
            }
        }
    }

    public void onChooseContactClick(View view) {
        // Declare required permissions.
        String[] requiredPermissions = new String[] {
                Manifest.permission.READ_CONTACTS,
        };

        // Choose contact if permission is present.
        if (checkForPermissions(requiredPermissions, REQUEST_CODE_CHOOSE_CONTACT)) {
            chooseFilterContact();
        }
    }

    private void chooseFilterContact() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, REQUEST_CODE_CHOOSE_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_CHOOSE_CONTACT:
                    filterByContact(data);
                    break;
                default:
                    Log.w("Activity Result", "Unexpected activity request code: " + requestCode);
            }
        } else {
            // Gracefully handle failure.
            Log.i("Activity Result", "Activity result not OK.");
        }
    }

    private void filterByContact(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            Cursor cursor = null;
            try {
                Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                String id = uri.getLastPathSegment();
                String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";
                String[] selectionArgs = new String[] {id};
                cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null);

                if (cursor != null && cursor.moveToFirst()) {
                    // Store number for filtering.
                    String columnName = ContactsContract.CommonDataKinds.Phone.NUMBER;
                    int columnIndex = cursor.getColumnIndex(columnName);
                    String number = cursor.getString(columnIndex);
                    m_filterContact = number;

                    // Update label with name.
                    columnName = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
                    columnIndex = cursor.getColumnIndex(columnName);
                    String name = cursor.getString(columnIndex);
                    TextView label = findViewById(R.id.textViewContact);
                    label.setText("Contact: " + name);

                    // Store for later use.
                    m_filterContactDisplayName = name;
                }
            }
            catch (SQLiteException | SecurityException | IllegalArgumentException e) {
                Log.w("Exception", e.getLocalizedMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void onAllContactsClick(View view) {
        TextView label = findViewById(R.id.textViewContact);
        label.setText("Contact: All");
        m_filterContact = "";
        m_filterContactDisplayName = "";
    }

    // Write the exported file.
    private void writeExportFile(List<String> textMessages) {
        try {
            // Update the label.
            TextView label = findViewById(R.id.textViewHint);
            label.setText(R.string.label_status_busy_exporting);

            // Get the directory.
            File dir = getExportFileDir();

            // Get the date.
//            Date date = new Date();
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

            // Get the file name.
//            String fileName = "log_" + format.format(date) + ".txt";
            String fileName = "export.txt";
            String filePath = dir + File.separator + fileName;
            Log.i("File name", filePath);

            // Create the file.
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            // Write to the file.
            FileWriter writer = new FileWriter(file);
            for (String message: textMessages) {
                writer.write(message);
                writer.write("\n");
            }
            writer.close();

            // Update the label.
            label.setText(R.string.label_status_text_file_written);
        } catch (IOException e) {
            Log.w("Export", e.getLocalizedMessage());
        }
    }

    private File getExportFileDir() throws IOException {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportsDir = new File(documentsDir + File.separator + "sms-exports");

        if (!exportsDir.exists()) {
            Log.i("Export", "Creating exports directory");
            boolean result = exportsDir.mkdirs();

            if (result == false) {
                Log.w("Export", "Could not create directory.");
                exportsDir = documentsDir;
            }
        }

        return exportsDir;
    }

    // Class for the date picker.
    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        public enum Purpose {
            CHOOSE_START_DATE,
            CHOOSE_END_DATE
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            if (mMainActivity != null) {
                if (mPurpose == Purpose.CHOOSE_START_DATE) {
                    mMainActivity.onStartDateSet(view, year, month, day);
                }
                else {
                    mMainActivity.onEndDateSet(view, year, month, day);
                }
            }
        }

        MainActivity mMainActivity = null;

        private void setMainActivity(MainActivity activity) {
            mMainActivity = activity;
        }

        private Purpose mPurpose = Purpose.CHOOSE_START_DATE;

        public void setPurpose(Purpose purpose) {
            mPurpose = purpose;
        }
    }

    // Callback for when the end date is set.
    private void onEndDateSet(DatePicker view, int year, int month, int day) {
        try {
            TextView label = findViewById(R.id.textViewEndDate);

            // Format date.
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, 23, 59, 59);
            Date date = calendar.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE dd MMMM yyyy", Locale.US);
            String dateStr = formatter.format(date);

            // Set label.
            label.setText("End date: " + dateStr);

            // Set filter field.
            m_filterEndDate = date;
        }
        catch (Exception e) {
            Log.w("Setting date", e.getLocalizedMessage());
            appendLog(e.getLocalizedMessage());
        }
    }

    // Show the start date picker.
    public void showDatePickerDialog(View view) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setMainActivity(this);
        if (view.getId() == R.id.buttonStartDate) {
            newFragment.setPurpose(DatePickerFragment.Purpose.CHOOSE_START_DATE);
        }
        else if (view.getId() == R.id.buttonEndDate) {
            newFragment.setPurpose(DatePickerFragment.Purpose.CHOOSE_END_DATE);
        }
        else {
            Log.w("Choose Date", "Unknown button.");
        }
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    // Callback for when the start date is set.
    public void onStartDateSet(DatePicker view, int year, int month, int day) {
        try {
            TextView label = findViewById(R.id.textViewStartDate);

            // Format date.
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, 0, 0, 0);
            Date date = calendar.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE dd MMMM yyyy", Locale.US);
            String dateStr = formatter.format(date);

            // Set label.
            label.setText("Start date: " + dateStr);

            // Set filter field.
            m_filterStartDate = date;
        }
        catch (Exception e) {
            Log.w("Setting date", e.getLocalizedMessage());
            appendLog(e.getLocalizedMessage());
        }
    }

    void appendLog(String message) {
        Logger.appendLog(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}