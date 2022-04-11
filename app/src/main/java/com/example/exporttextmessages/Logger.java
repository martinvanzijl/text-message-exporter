package com.example.exporttextmessages;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    // Check if logging is enabled.
    private static boolean loggingEnabled(Context context) {
        // Check if enabled in settings.
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("enable_logging", false);
    }

    // Write a message to the log file.
    public static void appendLog(Context context, String text)
    {
        // Exit if not enabled.
        if (!loggingEnabled(context)) {
            return;
        }

        // Write message.
        try {
            // Get the file name.
            File dir = getLogFileDir();
            String fileName = dir + "/text-exporter-log.txt";
            File logFile = new File(fileName);

            // Create log file if it does not exist.
            if (!logFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logFile.createNewFile();
            }

            // Get the timestamp.
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String timestamp = format.format(date);

            // Format the message.
            String message = timestamp + ": " + text;

            // Write the message.
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(message);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            Log.w("Logging", e.getLocalizedMessage());
        }
    }

    // Return the directory for storing log files.
    public static File getLogFileDir() throws IOException {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File logDir = new File(documentsDir + File.separator + "log-files");

        if (!logDir.exists()) {
            Log.i("Log Files", "Creating logs directory");
            boolean result = logDir.mkdirs();

            if (!result) {
                Log.w("Log Files", "Could not create directory.");
                logDir = documentsDir;
            }
        }

        return logDir;
    }
}
