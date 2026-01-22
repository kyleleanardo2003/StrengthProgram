package com.example.strongman916.strongman_competition.sheets;

import com.example.strongman916.strongman_competition.model.Competitor;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.example.strongman916.strongman_competition.model.EventNames.LOG_AND_KB;

@Service
public class GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsService.class);

    private final Sheets sheets;
    private final GoogleSheetsProperties props;

    public GoogleSheetsService(Sheets sheets, GoogleSheetsProperties props) {
        this.sheets = sheets;
        this.props = props;
    }

    public List<Competitor> readCompetitors() {
        requireConfig(props);

        try {
            ValueRange response = sheets.spreadsheets()
                    .values()
                    .get(props.getSpreadsheetId(), props.fullRange())
                    .execute();

            List<List<Object>> rows = response.getValues();
            if (rows == null || rows.isEmpty()) {
                log.warn("No rows returned from Google Sheets: spreadsheetId={}, range={}", props.getSpreadsheetId(), props.fullRange());
                return List.of();
            }

            return parseCompetitors(rows);

        } catch (Exception e) {
            log.error("Failed to read competitors from Google Sheets: spreadsheetId={}, range={}",
                    props.getSpreadsheetId(), props.fullRange(), e);
            return List.of();
        }
    }

    private List<Competitor> parseCompetitors(List<List<Object>> rows) {
        List<Competitor> competitors = new ArrayList<>();

        String currentGender = "";
        String currentWeightClass = "";

        for (int r = 0; r < rows.size(); r++) {
            List<Object> row = rows.get(r);

            String colA = cell(row, 0).trim();
            String colALower = colA.toLowerCase(Locale.ROOT);

            // Section markers: "Women" / "Men"
            if (isGenderMarker(colALower)) {
                currentGender = capitalize(colALower);
                continue;
            }

            // Weight class header row (based on your original logic)
            if (looksLikeWeightClassRow(row)) {
                currentWeightClass = colA;
                continue;
            }

            // Skip empty / too-short rows
            if (colA.isEmpty() || row.size() < 3) {
                continue;
            }

            // Skip obvious header rows (common in sheets)
            if (looksLikeHeaderRow(row)) {
                continue;
            }

            Optional<Competitor> maybe = mapRowToCompetitor(row, currentGender, currentWeightClass, r);
            maybe.ifPresent(competitors::add);
        }

        log.info("Parsed {} competitors from sheet tab '{}'", competitors.size(), props.getSheetName());
        return competitors;
    }

    private Optional<Competitor> mapRowToCompetitor(List<Object> row, String gender, String weightClass, int rowIndex) {
        // Column positions based on your current sheet layout
        // A: Name
        // B: Membership
        // C: Total Points
        // D/E: Log & KB score/points
        // F/G: Truck Pull time/points
        // H/I: Yoke & Frame score/points
        // J/K: Car Deadlift score/points
        // L/M: Stone Load score/points
        // N: Total Points (again) [your original code used index 13]
        // P: Place [your original code used index 15]
        String name = cell(row, 0);
        if (name.isBlank()) return Optional.empty();

        Double totalPoints = number(row, 2);
        if (totalPoints == null) {
            log.debug("Skipping row {}: invalid totalPoints cell", rowIndex + 1);
            return Optional.empty();
        }

        Competitor c = new Competitor();
        c.setName(name);
        c.setMembership(cell(row, 1));
        c.setGender(gender);
        c.setWeightClass(weightClass);

        // Prefer total points from C; fall back to N if present and valid.
        Double totalPointsAlt = number(row, 13);
        c.setTotalPoints(totalPointsAlt != null ? totalPointsAlt : totalPoints);

        // Events
        putScoreAndPoints(c, LOG_AND_KB, number(row, 3), number(row, 4));
        putTimeAndPoints(c, "Truck Pull", number(row, 5), number(row, 6));
        putScoreAndPoints(c, "Yoke And Frame", number(row, 7), number(row, 8));
        putScoreAndPoints(c, "Car Deadlift", number(row, 9), number(row, 10));
        putScoreAndPoints(c, "Stone Load", number(row, 11), number(row, 12));

        Integer place = integer(row, 15);
        if (place != null) c.setPlace(place);

        return Optional.of(c);
    }

    private void putScoreAndPoints(Competitor c, String eventName, Double score, Double points) {
        if (score != null) c.getScores().put(eventName, score);
        if (points != null) c.getEventPoints().put(eventName, points);
    }

    private void putTimeAndPoints(Competitor c, String eventName, Double timeSeconds, Double points) {
        if (timeSeconds != null) c.getTimes().put(eventName, timeSeconds);
        if (points != null) c.getEventPoints().put(eventName, points);
    }

    private boolean isGenderMarker(String cellLower) {
        return "women".equals(cellLower) || "men".equals(cellLower);
    }

    private boolean looksLikeWeightClassRow(List<Object> row) {
        // Your original logic: col C empty and col B == 0
        // row[2] empty AND row[1] numeric==0
        String colC = cell(row, 2);
        Double colB = number(row, 1);
        return colC.isBlank() && colB != null && colB == 0.0;
    }

    private boolean looksLikeHeaderRow(List<Object> row) {
        // crude but effective: common header keywords
        String a = cell(row, 0).toLowerCase(Locale.ROOT);
        String b = cell(row, 1).toLowerCase(Locale.ROOT);
        return a.contains("name") || b.contains("membership");
    }

    private String cell(List<Object> row, int idx) {
        if (idx < 0 || idx >= row.size() || row.get(idx) == null) return "";
        return row.get(idx).toString().trim();
    }

    private Double number(List<Object> row, int idx) {
        String s = cell(row, idx);
        if (s.isBlank()) return null;

        try {
            // Sheets sometimes returns formatted numbers like "1,234"
            String cleaned = s.replace(",", "");
            return Double.parseDouble(cleaned);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer integer(List<Object> row, int idx) {
        Double n = number(row, idx);
        if (n == null) return null;
        return (int) Math.round(n);
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase(Locale.ROOT);
    }

    private void requireConfig(GoogleSheetsProperties props) {
        if (props.getSpreadsheetId() == null || props.getSpreadsheetId().isBlank()) {
            throw new IllegalStateException("Missing config: app.google.sheets.spreadsheet-id");
        }
        if (props.getSheetName() == null || props.getSheetName().isBlank()) {
            throw new IllegalStateException("Missing config: app.google.sheets.sheet-name");
        }
    }
}
