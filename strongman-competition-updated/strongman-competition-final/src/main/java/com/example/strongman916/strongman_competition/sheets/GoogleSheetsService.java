package com.example.strongman916.strongman_competition.sheets;

import com.example.strongman916.strongman_competition.model.Competitor;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import static com.example.strongman916.strongman_competition.model.EventNames.*;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Strongman Sheets Integration";
    private static final String SPREADSHEET_ID = "1qfwZCXJ33x6nW918L-4IfL1uS0Vj8NnmN7qeGbn3TmE";
    private static final String RANGE = "'Winter Warrior 2025'!A1:R100";

    public List<Competitor> readCompetitorSheet() throws Exception {
        Sheets sheetsService = getSheetsService();
        ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, RANGE).execute();
        List<List<Object>> values = response.getValues();
        List<Competitor> competitors = new ArrayList<>();

        if (values == null || values.size() < 3) return competitors;

        String currentGender = "";
        String currentWeightClass = "";

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            String cell0 = getString(row, 0).toLowerCase();

            if (cell0.equals("women") || cell0.equals("men")) {
                currentGender = capitalize(cell0);
                continue;
            }

            if (row.size() > 2 && getString(row, 2).isEmpty() && getDouble(row, 1) == 0) {
                currentWeightClass = getString(row, 0).trim();
                continue;
            }

            if (row.size() < 3 || getString(row, 0).isEmpty()) {
                logSkip(i, "Missing name or too few columns");
                continue;
            }

            double totalPoints = getDouble(row, 2);
            if (Double.isNaN(totalPoints)) {
                logSkip(i, "Invalid Total Points (NaN)");
                continue;
            }

            Competitor comp = new Competitor();
            comp.setName(getString(row, 0));
            comp.setMembership(getString(row, 1));
            comp.setTotalPoints(totalPoints);
            comp.setGender(currentGender);
            comp.setWeightClass(currentWeightClass);

            comp.getScores().put(LOG_AND_KB, getDouble(row, 3));
            comp.getEventPoints().put(LOG_AND_KB, getDouble(row, 4));
            comp.getTimes().put("Truck Pull", getDouble(row, 5));
            comp.getEventPoints().put("Truck Pull", getDouble(row, 6));
            comp.getScores().put("Yoke And Frame", getDouble(row, 7));
            comp.getEventPoints().put("Yoke And Frame", getDouble(row, 8));
            comp.getScores().put("Car Deadlift", getDouble(row, 9));
            comp.getEventPoints().put("Car Deadlift", getDouble(row, 10));
            comp.getScores().put("Stone Load", getDouble(row, 11));
            comp.getEventPoints().put("Stone Load", getDouble(row, 12));

            comp.setTotalPoints(getDouble(row, 13));
            comp.setPlace((int) getDouble(row, 15));

            competitors.add(comp);
        }

        return competitors;
    }

    private Sheets getSheetsService() throws Exception {
        InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("sheets/sheets-key.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets.readonly"));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
        .setApplicationName(APPLICATION_NAME)
        .build();
    }

    private String getString(List<Object> row, int index) {
        return index < row.size() ? row.get(index).toString().trim() : "";
    }

    private double getDouble(List<Object> row, int index) {
        try {
            String val = getString(row, index);
            return val.isEmpty() ? 0.0 : Double.parseDouble(val);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void logSkip(int rowIndex, String reason) {
        System.out.println("[SKIPPED ROW " + (rowIndex + 1) + "] " + reason);
    }
}
