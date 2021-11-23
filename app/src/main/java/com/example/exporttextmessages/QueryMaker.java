package com.example.exporttextmessages;

import java.util.ArrayList;
import java.util.List;

public class QueryMaker {
    private List<String> mSelections = new ArrayList<>();
    private List<String> mArgs = new ArrayList<>();

    private void addSelection(String selection) {
        mSelections.add(selection);
    }

    private void addArg(String arg) {
        mArgs.add(arg);
    }

    public void addQuery(String selection, String arg) {
        addSelection(selection);
        addArg(arg);
    }

    public String getSelection() {
        if (mSelections.isEmpty()) {
            return null;
        }
        else {
            return join(mSelections, " and ");
        }
    }

    public String[] getSelectionArgs() {
        if (mArgs.isEmpty()) {
            return null;
        }
        else {
            // See https://stackoverflow.com/questions/4042434/converting-arrayliststring-to-string-in-java
            return mArgs.toArray(new String[0]);
        }
    }

    /**
     * Join a list of Strings using a delimiter.
     * From: https://stackoverflow.com/questions/1751844/java-convert-liststring-to-a-joind-string
     * @param list The list.
     * @param conjunction The delimiter.
     * @return The items in the list joined by the delimiter.
     */
    private String join(List<String> list, String conjunction)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list)
        {
            if (first)
                first = false;
            else
                sb.append(conjunction);
            sb.append(item);
        }
        return sb.toString();
    }
}
