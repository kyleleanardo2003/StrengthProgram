package strongmancast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.model.CompetitionEvent;
import strongmancast.model.EventResult;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.EventResultRepository;
import strongmancast.service.CompetitionContextService;
import strongmancast.service.WeightClassService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class BackupExportController {

    private final AthleteRepository athleteRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final EventResultRepository eventResultRepository;
    private final CompetitionContextService competitionContextService;
    private final WeightClassService weightClassService;

    public BackupExportController(
            AthleteRepository athleteRepository,
            CompetitionEventRepository competitionEventRepository,
            EventResultRepository eventResultRepository,
            CompetitionContextService competitionContextService,
            WeightClassService weightClassService
    ) {
        this.athleteRepository = athleteRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.eventResultRepository = eventResultRepository;
        this.competitionContextService = competitionContextService;
        this.weightClassService = weightClassService;
    }

    @GetMapping("/backup/live.xlsx")
    public void downloadLiveBackup(@RequestParam(required = false) Long competitionId, HttpServletResponse response) throws IOException {
        Competition competition = competitionContextService.currentCompetition(competitionId);
        List<Athlete> athletes = athleteRepository.findByCompetitionOrderByDivisionAscNameAsc(competition);
        List<CompetitionEvent> events = competitionEventRepository.findByCompetitionOrderBySortOrderAscIdAsc(competition);
        List<EventResult> results = eventResultRepository.findByCompetition(competition);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dataStyle = centeredStyle(workbook);
            writeOverviewSheet(workbook, headerStyle, dataStyle, competition, athletes, events, results);
            writeAthletesSheet(workbook, headerStyle, dataStyle, athletes);
            writeEventsSheet(workbook, headerStyle, dataStyle, events);
            writeRawResultsSheet(workbook, headerStyle, dataStyle, results);
            writeScoreSheet(workbook, headerStyle, dataStyle, athletes, events, results);

            String filename = safeFilename(competition.getName()) + "-backup-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            workbook.write(response.getOutputStream());
        }
    }

    private void writeOverviewSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            Competition competition,
            List<Athlete> athletes,
            List<CompetitionEvent> events,
            List<EventResult> results
    ) {
        Sheet sheet = workbook.createSheet("Overview");
        writeHeader(sheet.createRow(0), headerStyle, "Field", "Value");
        writeRow(sheet, dataStyle, 1, "Competition", competition.getName());
        writeRow(sheet, dataStyle, 2, "Status", competition.getStatus());
        writeRow(sheet, dataStyle, 3, "Date", competition.getCompetitionDate());
        writeRow(sheet, dataStyle, 4, "Location", competition.getLocation());
        writeRow(sheet, dataStyle, 5, "Exported At", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        writeRow(sheet, dataStyle, 6, "Athletes", athletes.size());
        writeRow(sheet, dataStyle, 7, "Events", events.size());
        writeRow(sheet, dataStyle, 8, "Saved Results", results.size());
        autosize(sheet, 2);
    }

    private void writeAthletesSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, List<Athlete> athletes) {
        Sheet sheet = workbook.createSheet("Athletes");
        writeHeader(sheet.createRow(0), headerStyle,
                "Name", "Membership", "Class Group", "Body Weight (kg)", "Calculated Division", "Class Override", "Event Group");
        int rowIndex = 1;
        for (Athlete athlete : athletes) {
            writeRow(sheet, dataStyle, rowIndex++,
                    athlete.getName(),
                    athlete.getMembership(),
                    athlete.getGender(),
                    athlete.getBodyweight(),
                    weightClassService.resolveDivision(athlete),
                    athlete.getDivision(),
                    athlete.getEventGroup());
        }
        autosize(sheet, 7);
    }

    private void writeEventsSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, List<CompetitionEvent> events) {
        Sheet sheet = workbook.createSheet("Events");
        writeHeader(sheet.createRow(0), headerStyle,
                "Order", "Event", "Result Label", "Default Unit", "Higher Wins", "Uses Time",
                "Time Tie Break", "Complete Target First", "Completion Target", "Active Slots", "Notes");
        int rowIndex = 1;
        for (CompetitionEvent event : events) {
            writeRow(sheet, dataStyle, rowIndex++,
                    event.getSortOrder(),
                    event.getEventName(),
                    event.getResultLabel(),
                    event.getDefaultUnit(),
                    event.isHigherIsBetter(),
                    event.isUsesTime(),
                    event.isTimeIsTieBreaker(),
                    event.isRequiresCompletionForTimeRanking(),
                    event.getCompletionTarget(),
                    event.getActiveSlots(),
                    event.getExtraFields());
        }
        autosize(sheet, 11);
    }

    private void writeRawResultsSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, List<EventResult> results) {
        Sheet sheet = workbook.createSheet("Raw Results");
        writeHeader(sheet.createRow(0), headerStyle, "Athlete", "Event", "Result", "Unit", "Time (sec)");
        int rowIndex = 1;
        List<EventResult> orderedResults = results.stream()
                .sorted(Comparator
                        .comparing((EventResult result) -> result.getAthlete().getName(), Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(EventResult::getEventName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        for (EventResult result : orderedResults) {
            writeRow(sheet, dataStyle, rowIndex++,
                    result.getAthlete().getName(),
                    result.getEventName(),
                    result.getResult(),
                    result.getUnit(),
                    result.getTime());
        }
        autosize(sheet, 5);
    }

    private void writeScoreSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            List<Athlete> athletes,
            List<CompetitionEvent> events,
            List<EventResult> results
    ) {
        Sheet sheet = workbook.createSheet("Score Sheet");
        Row header = sheet.createRow(0);
        writeHeaderCells(header, headerStyle, 0, "Athlete", "Division", "Body Weight (kg)");
        int column = 3;
        for (CompetitionEvent event : events) {
            writeHeaderCells(header, headerStyle, column, event.getEventName() + " Result", event.getEventName() + " Time (sec)");
            column += 2;
        }

        Map<String, EventResult> resultsByAthleteAndEvent = results.stream()
                .collect(Collectors.toMap(
                        result -> result.getAthlete().getId() + "|" + result.getEventName(),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new));

        int rowIndex = 1;
        for (Athlete athlete : athletes) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, athlete.getName(), dataStyle);
            writeCell(row, 1, weightClassService.resolveDivision(athlete), dataStyle);
            writeCell(row, 2, athlete.getBodyweight(), dataStyle);
            column = 3;
            for (CompetitionEvent event : events) {
                EventResult result = resultsByAthleteAndEvent.get(athlete.getId() + "|" + event.getEventName());
                writeCell(row, column, formatResult(result), dataStyle);
                writeCell(row, column + 1, result == null ? null : result.getTime(), dataStyle);
                column += 2;
            }
        }
        autosize(sheet, Math.max(3, column));
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle centeredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void writeHeader(Row row, CellStyle headerStyle, String... values) {
        writeHeaderCells(row, headerStyle, 0, values);
    }

    private void writeHeaderCells(Row row, CellStyle headerStyle, int startColumn, String... values) {
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(startColumn + index);
            cell.setCellValue(values[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRow(Sheet sheet, CellStyle dataStyle, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int column = 0; column < values.length; column++) {
            writeCell(row, column, values[column], dataStyle);
        }
    }

    private void writeCell(Row row, int column, Object value, CellStyle dataStyle) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        cell.setCellStyle(dataStyle);
    }

    private String formatResult(EventResult result) {
        if (result == null || result.getResult() == null) {
            return "";
        }
        if (result.getUnit() == null || result.getUnit().isBlank()) {
            return String.valueOf(result.getResult());
        }
        return result.getResult() + " " + result.getUnit();
    }

    private void autosize(Sheet sheet, int columns) {
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
        }
    }

    private String safeFilename(String value) {
        if (value == null || value.isBlank()) {
            return "strongmancast";
        }
        return value.replaceAll("[^A-Za-z0-9._-]+", "-");
    }
}
