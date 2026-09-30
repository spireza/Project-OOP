package project_oop;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvFundRepository implements FundRepository {

    private final Path dataDirectory;

    private final List<Student> students = new ArrayList<>();
    private final List<MonthlyPayment> payments = new ArrayList<>();
    private final List<MissionExpense> missions = new ArrayList<>();

    public CsvFundRepository() {
        this(Path.of("data"));
    }

    public CsvFundRepository(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(
                dataDirectory, "dataDirectory must not be null");

        students.addAll(readStudents());
        payments.addAll(readPayments());
        missions.addAll(readMissions());
    }

    @Override
    public List<Student> findAllStudents() {
        return new ArrayList<>(students);
    }

    @Override
    public List<MonthlyPayment> findPayments(int year) {
        List<MonthlyPayment> result = new ArrayList<>();

        for (MonthlyPayment payment : payments) {
            if (payment.getPeriod().getYear() == year) {
                result.add(payment);
            }
        }

        return result;
    }

    @Override
    public List<MissionExpense> findMissions(int year) {
        List<MissionExpense> result = new ArrayList<>();

        for (MissionExpense mission : missions) {
            if (mission.getExpenseDate().getYear() == year) {
                result.add(mission);
            }
        }

        return result;
    }

    @Override
    public void saveStudent(Student student) {
        Objects.requireNonNull(student, "student must not be null");

        for (Student existing : students) {
            if (existing.getStudentId().equals(student.getStudentId())) {
                throw new IllegalArgumentException(
                        "This list already exists.");
            }
        }

        students.add(student);
        writeStudents();
    }

    @Override
    public void savePayment(MonthlyPayment payment) {
        payments.add(Objects.requireNonNull(
                payment, "payment must not be null"));

        writePayments();
    }

    @Override
    public void saveMission(MissionExpense mission) {
        missions.add(Objects.requireNonNull(
                mission, "mission must not be null"));

        writeMissions();
    }

    private List<Student> readStudents() {
        List<Student> result = new ArrayList<>();

        for (List<String> row : readCsv("students.csv")) {
            if (row.get(0).equals("studentId")) {
                continue; 
            }

            requireColumns(row, 2);
            result.add(new Student(row.get(1), row.get(0)));
        }

        return result;
    }

    private List<MonthlyPayment> readPayments() {
        List<MonthlyPayment> result = new ArrayList<>();

        for (List<String> row : readCsv("payments.csv")) {
            if (row.get(0).equals("studentId")) {
                continue;
            }

            requireColumns(row, 4);
            result.add(new MonthlyPayment(
                    row.get(0),
                    YearMonth.parse(row.get(1)),
                    new BigDecimal(row.get(2)),
                    LocalDate.parse(row.get(3))));
        }

        return result;
    }

    private List<MissionExpense> readMissions() {
        List<MissionExpense> result = new ArrayList<>();

        for (List<String> row : readCsv("missions.csv")) {
            if (row.get(0).equals("missionId")) {
                continue;
            }

            requireColumns(row, 6);
            result.add(new MissionExpense(
                    row.get(0),
                    row.get(1),
                    row.get(2),
                    row.get(3),
                    new BigDecimal(row.get(4)),
                    LocalDate.parse(row.get(5))));
        }

        return result;
    }

    private List<List<String>> readCsv(String fileName) {
        Path file = dataDirectory.resolve(fileName);
        List<List<String>> rows = new ArrayList<>();

        if (!Files.exists(file)) {
            return rows;
        }

        try {
            for (String line : Files.readAllLines(
                    file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    rows.add(parseCsvLine(line));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Cannot Read File: " + file, ex);
        }

        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);

            if (current == '"') {
                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (current == ',' && !insideQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }

        fields.add(field.toString());

        if (!fields.isEmpty() && fields.get(0).startsWith("\uFEFF")) {
            fields.set(0, fields.get(0).substring(1));
        }

        return fields;
    }

    private String csv(String value) {
        
        String singleLine = value
                .replace("\r", " ")
                .replace("\n", " ");

        return "\"" + singleLine.replace("\"", "\"\"") + "\"";
    }

    private void writeStudents() {
        List<String> lines = new ArrayList<>();
        lines.add("\uFEFFstudentId,name");

        for (Student student : students) {
            lines.add(csv(student.getStudentId())
                    + "," + csv(student.getName()));
        }

        writeCsv("students.csv", lines);
    }

    private void writePayments() {
        List<String> lines = new ArrayList<>();
        lines.add("\uFEFFstudentId,period,amount,receivedDate");

        for (MonthlyPayment payment : payments) {
            lines.add(csv(payment.getStudentId())
                    + "," + csv(payment.getPeriod().toString())
                    + "," + csv(payment.getAmount().toPlainString())
                    + "," + csv(payment.getReceivedDate().toString()));
        }

        writeCsv("payments.csv", lines);
    }

    private void writeMissions() {
        List<String> lines = new ArrayList<>();
        lines.add("\uFEFFmissionId,activityName,category,detail,amount,expenseDate");

        for (MissionExpense mission : missions) {
            lines.add(csv(mission.getMissionId())
                    + "," + csv(mission.getActivityName())
                    + "," + csv(mission.getCategory())
                    + "," + csv(mission.getDetail())
                    + "," + csv(mission.getAmount().toPlainString())
                    + "," + csv(mission.getExpenseDate().toString()));
        }

        writeCsv("missions.csv", lines);
    }

    private void writeCsv(String fileName, List<String> lines) {
        try {
            Files.createDirectories(dataDirectory);

            Files.write(
                    dataDirectory.resolve(fileName),
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Cannot Save file: " + fileName, ex);
        }
    }

    private void requireColumns(List<String> row, int expectedCount) {
        if (row.size() != expectedCount) {
            throw new IllegalStateException(
                    "Row is Wrong: " + row);
        }
    }
}