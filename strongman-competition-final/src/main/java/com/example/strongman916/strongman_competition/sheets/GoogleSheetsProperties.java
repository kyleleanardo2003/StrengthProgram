package com.example.strongman916.strongman_competition.sheets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google.sheets")
public class GoogleSheetsProperties {

    /**
     * Google Sheets spreadsheet id (the long id in the URL).
     */
    private String spreadsheetId;

    /**
     * Sheet tab name, e.g. "Winter Warrior 2025"
     */
    private String sheetName;

    /**
     * A1 notation range (without sheet name), e.g. "A1:R100"
     */
    private String range = "A1:R200";

    /**
     * Optional: path to a service account JSON on disk (NOT in repo).
     * If not set, Application Default Credentials are used.
     *
     * Example: /etc/secrets/sheets-sa.json
     */
    private String credentialsFile;

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getCredentialsFile() {
        return credentialsFile;
    }

    public void setCredentialsFile(String credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    public String fullRange() {
        // Quote sheet names to handle spaces safely
        return "'" + sheetName + "'!" + range;
    }
}
