package strongmancast.controller;

import strongmancast.model.Athlete;
import strongmancast.model.Competition;
import strongmancast.repository.AthleteRepository;
import strongmancast.repository.CompetitionEventRepository;
import strongmancast.repository.CompetitionRepository;
import strongmancast.repository.CompetitionSettingsRepository;
import strongmancast.repository.EventResultRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CompetitionController {

    private final CompetitionRepository competitionRepository;
    private final AthleteRepository athleteRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final CompetitionSettingsRepository competitionSettingsRepository;
    private final EventResultRepository eventResultRepository;

    public CompetitionController(
            CompetitionRepository competitionRepository,
            AthleteRepository athleteRepository,
            CompetitionEventRepository competitionEventRepository,
            CompetitionSettingsRepository competitionSettingsRepository,
            EventResultRepository eventResultRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.athleteRepository = athleteRepository;
        this.competitionEventRepository = competitionEventRepository;
        this.competitionSettingsRepository = competitionSettingsRepository;
        this.eventResultRepository = eventResultRepository;
    }

    @GetMapping("/competitions")
    public String competitions(Model model, Principal principal) {
        model.addAttribute("competition", new Competition());
        model.addAttribute("canEditCompetitions", principal != null);
        model.addAttribute("liveCompetitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("LIVE"));
        model.addAttribute("upcomingCompetitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("SETUP"));
        model.addAttribute("pastCompetitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("COMPLETE"));
        return "competitions";
    }

    @PostMapping("/competitions")
    public String createCompetition(@ModelAttribute Competition competition) {
        competition.setName(defaultCompetitionName(competition.getName()));
        if (competition.getStatus() == null || competition.getStatus().isBlank()) {
            competition.setStatus("SETUP");
        }

        Competition savedCompetition = competitionRepository.save(competition);
        return "redirect:/organizer?competitionId=" + savedCompetition.getId();
    }

    @PostMapping("/competitions/import")
    @Transactional
    public String importCompetition(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competitionDate,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "SETUP") String status,
            @RequestParam MultipartFile dataFile,
            RedirectAttributes redirectAttributes
    ) {
        if (dataFile == null || dataFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", "Choose an Excel, CSV, or TSV file to import.");
            return "redirect:/competitions";
        }

        try {
            List<ImportedAthlete> importedAthletes = readImportedAthletes(dataFile);
            if (importedAthletes.isEmpty()) {
                redirectAttributes.addFlashAttribute("importError", "No athletes were found. Include a name column and bodyweight column.");
                return "redirect:/competitions";
            }

            Competition competition = new Competition();
            competition.setName(defaultCompetitionName(name));
            competition.setCompetitionDate(competitionDate);
            competition.setLocation(location);
            competition.setStatus(status == null || status.isBlank() ? "SETUP" : status);
            Competition savedCompetition = competitionRepository.save(competition);

            for (ImportedAthlete importedAthlete : importedAthletes) {
                Athlete athlete = new Athlete();
                athlete.setCompetition(savedCompetition);
                athlete.setName(importedAthlete.name());
                athlete.setBodyweight(importedAthlete.bodyweight());
                athlete.setMembership(importedAthlete.membership());
                athlete.setGender(importedAthlete.gender());
                athlete.setDivision(importedAthlete.division());
                athleteRepository.save(athlete);
            }

            redirectAttributes.addFlashAttribute("importMessage", "Imported " + importedAthletes.size() + " athletes.");
            return "redirect:/organizer?competitionId=" + savedCompetition.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("importError", "Could not import file: " + e.getMessage());
            return "redirect:/competitions";
        }
    }

    @PostMapping("/competitions/{id}")
    public String updateCompetition(@PathVariable Long id, @ModelAttribute Competition competition) {
        Competition existingCompetition = competitionRepository.findById(id).orElseThrow();
        existingCompetition.setName(competition.getName());
        existingCompetition.setCompetitionDate(competition.getCompetitionDate());
        existingCompetition.setLocation(competition.getLocation());
        existingCompetition.setStatus(competition.getStatus());
        competitionRepository.save(existingCompetition);
        return "redirect:/competitions";
    }

    @PostMapping("/competitions/{id}/name")
    @ResponseBody
    public String updateCompetitionName(@PathVariable Long id, @RequestParam String name) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        competition.setName(name);
        competitionRepository.save(competition);
        return "ok";
    }

    @PostMapping("/competitions/{id}/status/{status}")
    public String updateStatus(@PathVariable Long id, @PathVariable String status) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        competition.setStatus(status.toUpperCase());
        competitionRepository.save(competition);
        return "redirect:/organizer?competitionId=" + id;
    }

    @PostMapping("/competitions/{id}/delete")
    @Transactional
    public String deleteCompetition(@PathVariable Long id) {
        Competition competition = competitionRepository.findById(id).orElseThrow();
        eventResultRepository.deleteByCompetition(competition);
        athleteRepository.deleteByCompetition(competition);
        competitionEventRepository.deleteByCompetition(competition);
        competitionSettingsRepository.deleteByCompetition(competition);
        competitionRepository.delete(competition);
        return "redirect:/competitions";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("competitions", competitionRepository.findByStatusOrderByCompetitionDateDescIdDesc("COMPLETE"));
        return "archive";
    }

    private List<ImportedAthlete> readImportedAthletes(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".csv")) {
            return readDelimitedAthletes(file, ",");
        }
        if (filename.endsWith(".tsv")) {
            return readDelimitedAthletes(file, "\t");
        }
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return readWorkbookAthletes(file);
        }

        throw new IllegalArgumentException("Use an .xlsx, .xls, .csv, or .tsv file.");
    }

    private String defaultCompetitionName(String requestedName) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }

        return "Competition #" + (competitionRepository.count() + 1);
    }

    private List<ImportedAthlete> readWorkbookAthletes(MultipartFile file) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                return List.of();
            }

            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                int lastCell = Math.max(row.getLastCellNum(), 0);
                for (int column = 0; column < lastCell; column++) {
                    Cell cell = row.getCell(column);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                rows.add(values);
            }
        }
        return athletesFromRows(rows);
    }

    private List<ImportedAthlete> readDelimitedAthletes(MultipartFile file, String delimiter) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseDelimitedLine(line, delimiter));
            }
        }
        return athletesFromRows(rows);
    }

    private List<ImportedAthlete> athletesFromRows(List<List<String>> rows) {
        List<List<String>> nonEmptyRows = rows.stream()
                .filter(row -> row.stream().anyMatch(value -> value != null && !value.isBlank()))
                .toList();
        if (nonEmptyRows.isEmpty()) {
            return List.of();
        }

        ColumnMap columns = columnMap(nonEmptyRows.get(0));
        int startRow = columns.hasHeader() ? 1 : 0;
        List<ImportedAthlete> athletes = new ArrayList<>();
        for (int rowIndex = startRow; rowIndex < nonEmptyRows.size(); rowIndex++) {
            List<String> row = nonEmptyRows.get(rowIndex);
            String name = valueAt(row, columns.nameColumn());
            Double bodyweight = parseBodyweight(valueAt(row, columns.bodyweightColumn()), columns.weightIsPounds());
            if (name == null || name.isBlank() || bodyweight == null) {
                continue;
            }

            athletes.add(new ImportedAthlete(
                    name.trim(),
                    bodyweight,
                    valueAt(row, columns.membershipColumn()),
                    valueAt(row, columns.genderColumn()),
                    valueAt(row, columns.divisionColumn())
            ));
        }
        return athletes;
    }

    private ColumnMap columnMap(List<String> headerRow) {
        int nameColumn = findColumn(headerRow, "name", "athlete", "competitor", "lifter");
        int bodyweightColumn = findColumn(headerRow, "bodyweight", "body weight", "body wt", "weight", "bw", "wt");
        int membershipColumn = findColumn(headerRow, "membership", "member", "id", "number");
        int genderColumn = findColumn(headerRow, "gender", "class group", "group");
        int divisionColumn = findColumn(headerRow, "division", "weight class", "class", "weightclass");
        boolean hasHeader = nameColumn >= 0 && bodyweightColumn >= 0;

        if (!hasHeader) {
            nameColumn = 0;
            bodyweightColumn = 1;
        }

        String bodyweightHeader = valueAt(headerRow, bodyweightColumn);
        boolean weightIsPounds = bodyweightHeader != null
                && (normalize(bodyweightHeader).contains("lb") || normalize(bodyweightHeader).contains("pound"));
        return new ColumnMap(nameColumn, bodyweightColumn, membershipColumn, genderColumn, divisionColumn, hasHeader, weightIsPounds);
    }

    private int findColumn(List<String> headers, String... aliases) {
        for (int index = 0; index < headers.size(); index++) {
                String normalizedHeader = normalize(headers.get(index));
            for (String alias : aliases) {
                String normalizedAlias = normalize(alias);
                if (normalizedHeader.equals(normalizedAlias) || shouldMatchContainedAlias(normalizedHeader, normalizedAlias)) {
                    return index;
                }
            }
        }
        return -1;
    }

    private boolean shouldMatchContainedAlias(String normalizedHeader, String normalizedAlias) {
        return normalizedAlias.length() > 3
                && !"class".equals(normalizedAlias)
                && !"group".equals(normalizedAlias)
                && !"weight".equals(normalizedAlias)
                && normalizedHeader.contains(normalizedAlias);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private String valueAt(List<String> row, int column) {
        if (column < 0 || column >= row.size()) {
            return "";
        }
        return row.get(column) == null ? "" : row.get(column).trim();
    }

    private Double parseBodyweight(String value, boolean pounds) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.replace(",", "").replaceAll("[^0-9.\\-]+", "");
        if (cleaned.isBlank()) {
            return null;
        }

        double parsed = Double.parseDouble(cleaned);
        double kilograms = pounds ? parsed * 0.45359237 : parsed;
        return Math.round(kilograms * 10.0) / 10.0;
    }

    private List<String> parseDelimitedLine(String line, String delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char delimiterChar = delimiter.charAt(0);

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (character == delimiterChar && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString().trim());
        return values;
    }

    private record ImportedAthlete(String name, double bodyweight, String membership, String gender, String division) {
    }

    private record ColumnMap(
            int nameColumn,
            int bodyweightColumn,
            int membershipColumn,
            int genderColumn,
            int divisionColumn,
            boolean hasHeader,
            boolean weightIsPounds
    ) {
    }
}

