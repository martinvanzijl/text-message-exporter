package com.example.exporttextmessages;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EXPORT_MESSAGES = 1000;
    private static final int REQUEST_CODE_CHOOSE_CONTACT = 2000;
    private String m_filterContact = "";
    private String m_filterContactDisplayName = "";
    private Date m_filterStartDate = null;
    private Date m_filterEndDate = null;
    // A cache for contact names. This does make a difference in speed.
    @SuppressWarnings("CanBeFinal")
    private Map<String, String> mContactNames = new HashMap<>();
    private ActivityResultLauncher<Intent> chooseContactActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Allow opening export folder.
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // Set click handlers.
        findViewById(R.id.textViewHint).setOnClickListener(this::onTextViewHintClick);
        findViewById(R.id.buttonExport).setOnClickListener(this::onButtonExportClick);
        findViewById(R.id.buttonEmail).setOnClickListener(this::onButtonEmailClick);
        findViewById(R.id.buttonChooseContact).setOnClickListener(this::onChooseContactClick);
        findViewById(R.id.buttonAllContacts).setOnClickListener(this::onAllContactsClick);
        findViewById(R.id.buttonStartDate).setOnClickListener(this::showDatePickerDialog);
        findViewById(R.id.buttonResetStartDate).setOnClickListener(this::onResetStartDateClick);
        findViewById(R.id.buttonEndDate).setOnClickListener(this::showDatePickerDialog);
        findViewById(R.id.buttonResetEndDate).setOnClickListener(this::onResetEndDateClick);

        // Create result handler.
        chooseContactActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        filterByContact(data);
                    }
                }
        );

        // Hide "reset" buttons.
        findViewById(R.id.buttonAllContacts).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonResetStartDate).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonResetEndDate).setVisibility(View.INVISIBLE);
    }

//    @SuppressLint("NewApi")
    public List<MessageDetails> getAllSms() {
        List<MessageDetails> lstSms = new ArrayList<>();

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
//                        String text = formatMessage(body, type, address, date);
//                        lstSms.add(text);
                        MessageDetails details = new MessageDetails(body, type, address, date);
                        lstSms.add(details);
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

    /**
     * Format the message as a string.
     *
     * @param m The message.
     * @return The string representation.
     */
    private String formatMessage(MessageDetails m) {
        return formatMessage(m.getBody(), m.getType(), m.getAddress(), m.getDate());
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

    public void onButtonExportClick(@SuppressWarnings("unused") View view) {
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(
                getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    // Export SMS messages.
    private void exportTextMessages() {
        List<MessageDetails> textMessages = getAllSms();
        Log.i("Export", "There are " + textMessages.size() + " messages.");
        writeExportFile(textMessages);

        if (exportXmlFileEnabled()) {
            writeExportFileXml(textMessages);
        }
    }

    /**
     * Check if export to XML is enabled.
     * @return Whether exporting to XML is enabled.
     */
    private boolean exportXmlFileEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("export_xml", false);
    }

    /**
     * Export messages to an XML file.
     * @param textMessages The messages.
     */
    private void writeExportFileXml(List<MessageDetails> textMessages) {
        try {
            // Update the label.
            TextView label = findViewById(R.id.textViewHint);
            label.setText(R.string.label_status_busy_exporting);

            // Get the directory.
            File dir = getExportFileDir();

            // Get the file name.
            String fileName = "export.xml";
            String filePath = dir + File.separator + fileName;

            // Create factory.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Create builder.
            DocumentBuilder db = factory.newDocumentBuilder();

            // Create document.
            Document doc = db.newDocument();

            // Create the root element.
            Element rootElement = doc.createElement("text_messages");

            // Create message element.
            for (MessageDetails message: textMessages) {
                message.addToXml(doc, rootElement);
            }

            // Add the root element.
            doc.appendChild(rootElement);

            try {
                // Set XML format.
                Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                // Send output to file.
                tr.transform(new DOMSource(doc),
                        new StreamResult(new FileOutputStream(filePath)));

            } catch (TransformerException | IOException e) {
                Log.w("XML", e.getLocalizedMessage());
            }

            // Update the label.
            label.setText(R.string.label_status_text_file_written);
        } catch (Exception e) {
            Log.w("Export", e.getLocalizedMessage());
        }
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

    public void onChooseContactClick(@SuppressWarnings("unused") View view) {
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
//        startActivityForResult(contactPickerIntent, REQUEST_CODE_CHOOSE_CONTACT);

        // Start activity.
        chooseContactActivity.launch(contactPickerIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_CONTACT) {
                filterByContact(data);
            } else {
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
                    m_filterContact = cursor.getString(columnIndex);

                    // Update label with name.
                    columnName = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
                    columnIndex = cursor.getColumnIndex(columnName);
                    String name = cursor.getString(columnIndex);
                    TextView label = findViewById(R.id.textViewContact);
                    label.setText(getString(R.string.filter_contact_set, name));

                    // Store for later use.
                    m_filterContactDisplayName = name;

                    // Show "reset" button.
                    findViewById(R.id.buttonAllContacts).setVisibility(View.VISIBLE);
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

    public void onAllContactsClick(@SuppressWarnings("unused") View view) {
        TextView label = findViewById(R.id.textViewContact);
        label.setText(R.string.contact_label);
        m_filterContact = "";
        m_filterContactDisplayName = "";

        // Hide "reset" button.
        findViewById(R.id.buttonAllContacts).setVisibility(View.INVISIBLE);
    }

    // Write the exported file.
    private void writeExportFile(List<MessageDetails> textMessages) {
        try {
            // Update the label.
            TextView label = findViewById(R.id.textViewHint);
            label.setText(R.string.label_status_busy_exporting);

            // Get the path.
            String filePath = getExportedTextFilePath();

            // Create the file.
            File file = new File(filePath);
            if (!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            }

            // Write to the file.
            FileWriter writer = new FileWriter(file);
            for (MessageDetails message: textMessages) {
                String line = formatMessage(message);
                writer.write(line);
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

            if (!result) {
                Log.w("Export", "Could not create directory.");
                exportsDir = documentsDir;
            }
        }

        return exportsDir;
    }

    public void onResetStartDateClick(View view) {
        // Reset label.
        TextView label = findViewById(R.id.textViewStartDate);
        label.setText(R.string.label_start_date);

        // Reset filter field.
        m_filterStartDate = null;

        // Hide "reset" button.
        findViewById(R.id.buttonResetStartDate).setVisibility(View.INVISIBLE);
    }

    public void onResetEndDateClick(View view) {
        // Reset label.
        TextView label = findViewById(R.id.textViewEndDate);
        label.setText(R.string.label_end_date);

        // Reset filter field.
        m_filterEndDate = null;

        // Hide "reset" button.
        findViewById(R.id.buttonResetEndDate).setVisibility(View.INVISIBLE);
    }

    // Class for the date picker.
    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        public enum Purpose {
            CHOOSE_START_DATE,
            CHOOSE_END_DATE
        }

        @NonNull
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
    private void onEndDateSet(@SuppressWarnings("unused") DatePicker view, int year, int month, int day) {
        try {
            TextView label = findViewById(R.id.textViewEndDate);

            // Format date.
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, 23, 59, 59);
            Date date = calendar.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE dd MMMM yyyy", Locale.US);
            String dateStr = formatter.format(date);

            // Set label.
            label.setText(getString(R.string.end_date_set, dateStr));

            // Set filter field.
            m_filterEndDate = date;

            // Show "reset" button.
            findViewById(R.id.buttonResetEndDate).setVisibility(View.VISIBLE);
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
    public void onStartDateSet(@SuppressWarnings("unused") DatePicker view, int year, int month, int day) {
        try {
            TextView label = findViewById(R.id.textViewStartDate);

            // Format date.
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, 0, 0, 0);
            Date date = calendar.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE dd MMMM yyyy", Locale.US);
            String dateStr = formatter.format(date);

            // Set label.
            label.setText(getString(R.string.start_date_set, dateStr));

            // Set filter field.
            m_filterStartDate = date;

            // Show "reset" button.
            findViewById(R.id.buttonResetStartDate).setVisibility(View.VISIBLE);
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
        else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onTextViewHintClick(@SuppressWarnings("unused") View view) {
//        openExportFolder();
        viewExportedFile();
    }

    /**
     * View the latest exported file.
     */
    private void viewExportedFile() {
        try {
            // Get file path.
            String path = getExportedTextFilePath();

            // Check that file exists.
            File file = new File(path);
            if (!file.exists()) {
                showToastMessage("Export file does not exist.");
                return;
            }

            // Use file provider URI.
            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

            // Launch activity to view file.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/plain");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.w("Open Folder", e.getLocalizedMessage());
        }
    }

    /**
     * Show a "toast" message.
     * @param message The message.
     */
    protected void showToastMessage(String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**
     * Open the folder containing the export files.
     */
    private void openExportFolder() {
        try {
            openFolder(getExportFileDir());
        } catch (IOException e) {
            Log.w("Open Export Folder", e.getLocalizedMessage());
        }
    }

    /**
     * Open a specific folder in File Explorer.
     * @param location The folder to open.
     */
    private void openFolder(File location) {
        // From: https://stackoverflow.com/questions/41611757/how-to-simply-open-directory-folder
        try {
            // Get file path.
            String path = location.getAbsolutePath(); // Does not open correct folder.
//            String path = location.getPath(); // Does not work.

            // Create intent.
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Set location.
            Uri dir = Uri.parse("file://" + path); // Does not open correct folder.
//            Uri dir = Uri.parse(path); // Does not work.
//            Uri dir = Uri.parse(path + File.separator); // Does not work.
//            Uri dir = Uri.parse("content://" + path); // Does not work. Expects file contents.

            // Use a file provider. Does not work.
//            Uri dir = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", location);

            // Set URI and target file type.
            intent.setDataAndType(dir, "*/*");
//            intent.setDataAndType(dir, "vnd.android.document/directory"); // Does not work.

            // Allow intent to access the folder.
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // From reading source code of Files app. Does not work.
//            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dir);

            // Show the file browser.
            startActivity(intent);

            // Does not work. Still enters wrong folder.
//            startActivity(Intent.createChooser(intent, "Navigate"));
        }
        catch (Exception e) {
            Log.w("Open Folder", e.getLocalizedMessage());
        }
    }

    /**
     * Get the path the text file must be exported to.
     * @return The file the text file is exported to.
     */
    private String getExportedTextFilePath() throws IOException {

        File dir = getExportFileDir();
        String fileName = "export.txt";

        return dir + File.separator + fileName;
    }

    public void onButtonEmailClick(View view) {
        // Print message.
        Log.i("Email", "Email button clicked.");

        try {
            // Get the file to attach.
            String path = getExportedTextFilePath();

            // Check that file exists.
            File file = new File(path);
            if (!file.exists()) {
                showToastMessage("Export file does not exist.");
                return;
            }

            // Use file provider URI.
            Uri attachmentURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

            // Get the address.
//            String recipient = "martin.vanzijl@gmail.com";
//            String[] recipients = new String[] { recipient };
            String[] recipients = null;

            // Create the intent.
            Intent emailIntent = new Intent(Intent.ACTION_SEND);

            // The intent does not have a URI, so declare the "text/plain" MIME type
//        emailIntent.setType(HTTP.PLAIN_TEXT_TYPE);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients); // recipients
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Text Message Export");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Here are text messages exported from my phone.");
            emailIntent.putExtra(Intent.EXTRA_STREAM, attachmentURI);
            // You can also attach multiple items by passing an ArrayList of Uris

            // Start the email activity.
//            startActivity(Intent.createChooser(emailIntent, "Send email..."));
            startActivity(emailIntent);
        }
        catch (IOException e) {
            Log.w("Email", e.getLocalizedMessage());
            showToastMessage("Problem sending email.");
        }
        catch (ActivityNotFoundException e) {
            Log.w("Email", e.getLocalizedMessage());
            showToastMessage("Could not find app to send email.");
        }
    }
}