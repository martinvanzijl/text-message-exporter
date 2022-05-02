package com.example.exporttextmessages;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = null;
    private static final String EXPORTED_FILE_TYPE_KEY = "exported_file_type";
    private boolean firstStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Set label when preference changed.
        prefListener = (sharedPreferences, key) -> {
            if (key.equals(EXPORTED_FILE_TYPE_KEY)) {
                updateExportFileTypeLabel();
            }
        };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Update the label at the start.
        if (firstStart) {
            updateExportFileTypeLabel();
            firstStart = false;
        }
    }

    /**
     * Update the label for the "Exported file type" preference.
     */
    private void updateExportFileTypeLabel() {
        // Get the UI fragment containing the label.
        SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings);

        // Sanity check.
        if (fragment == null) {
            Log.w(getLogName(), "Fragment was null.");
            return;
        }

        // Get the preference widget within the fragment.
        Preference pref = fragment.findPreference((CharSequence) EXPORTED_FILE_TYPE_KEY);

        // Get the setting value.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String value = prefs.getString(EXPORTED_FILE_TYPE_KEY, getString(R.string.text));

        // Set the widget label.
        assert pref != null;
        //pref.setTitle(getString(R.string.choose_file_type_label));
        pref.setSummary(getString(R.string.file_type_chosen_value, value));
    }

    /**
     * Get the name for logging.
     * @return The name for logging.
     */
    private String getLogName() {
        return "Settings";
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}