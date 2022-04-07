package com.example.exporttextmessages;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class PreviewActivity extends AppCompatActivity {

    // Intent constants.
    static final String INTENT_FILE_TO_PREVIEW = "INTENT_FILE_TO_PREVIEW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Add "back" button at top left.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Read the intent.
        Intent intent = getIntent();
        if (intent.hasExtra(INTENT_FILE_TO_PREVIEW)) {
            // Get the file path.
            String filePath = intent.getStringExtra(INTENT_FILE_TO_PREVIEW);

            // Load the file content.
            loadFile(filePath);
        }
    }

    /**
     * Load the content of the given file into the editor.
     * @param filePath The path to the file.
     */
    private void loadFile(String filePath) {
        try {
            // Open the file.
            File file = new File(filePath);

            // Read the content.
            BufferedReader buf = new BufferedReader(new FileReader(file));

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = buf.readLine()) != null) {
                builder.append(line).append("\n");
            }

            // Load the help content.
            EditText editText = findViewById(R.id.editTextPreviewContent);
            editText.setText(builder.toString());

        } catch (IOException e) {
            e.printStackTrace();
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