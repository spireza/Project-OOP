package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import project_oop.FundRepository;
import project_oop.Student;

/** UI for importing students and exporting payment reports as CSV. */
public class StudentCsvPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final FundRepository repository;
    private final Runnable onStudentsChanged;
    private final Function<Boolean, List<String>> paymentReportRowsSupplier;

    private final JLabel statusLabel =
            new JLabel(" ", SwingConstants.CENTER);

    public StudentCsvPanel(
            FundRepository repository,
            Runnable onStudentsChanged,
            Function<Boolean, List<String>> paymentReportRowsSupplier) {

        this.repository = Objects.requireNonNull(
                repository, "repository must not be null");
        this.onStudentsChanged = Objects.requireNonNull(
                onStudentsChanged, "onStudentsChanged must not be null");
        this.paymentReportRowsSupplier = Objects.requireNonNull(
                paymentReportRowsSupplier,
                "paymentReportRowsSupplier must not be null");

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        JPanel heading = new JPanel(new BorderLayout(0, 8));

        JLabel title = new JLabel(
                "Student CSV Files",
                SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

        JLabel subtitle = new JLabel(
                "Import students or export a payment report",
                SwingConstants.CENTER);

        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new java.awt.Color(215, 225, 219)),
                BorderFactory.createEmptyBorder(28, 24, 24, 24)));

        JLabel formatLabel = new JLabel(
                "Import CSV columns: studentId, name",
                SwingConstants.CENTER);
        formatLabel.setFont(
                formatLabel.getFont().deriveFont(Font.BOLD, 16f));
        content.add(formatLabel, BorderLayout.NORTH);

        JPanel actions =
                new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JButton importButton = new JButton("Import CSV");
        JButton reportButton =
                new JButton("Export Payment Report");

        importButton.setPreferredSize(
                new java.awt.Dimension(180, 42));
        reportButton.setPreferredSize(
                new java.awt.Dimension(230, 42));

        importButton.addActionListener(event -> importCsv());
        reportButton.addActionListener(
                event -> exportPaymentReport());

        actions.add(importButton);
        actions.add(reportButton);
        content.add(actions, BorderLayout.CENTER);

        JLabel helpLabel = new JLabel(
                "Import a student roster, or export payments for the selected year or all years.",
                SwingConstants.CENTER);
        content.add(helpLabel, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose student CSV file to import");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();

        try {
            List<Student> importedStudents = parseStudents(file);

            Set<String> existingIds = new HashSet<>();
            for (Student student : repository.findAllStudents()) {
                existingIds.add(student.getStudentId());
            }

            List<Student> newStudents = new ArrayList<>();
            int duplicateCount = 0;

            for (Student student : importedStudents) {
                if (!existingIds.add(student.getStudentId())) {
                    duplicateCount++;
                } else {
                    newStudents.add(student);
                }
            }

            if (newStudents.isEmpty()) {
                showMessage(
                        "No new students to import. Duplicate IDs skipped: "
                                + duplicateCount,
                        "Import CSV",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Import " + newStudents.size()
                            + " new student(s)?\nDuplicate IDs to skip: "
                            + duplicateCount,
                    "Confirm import",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            int added = 0;

            try {
                for (Student student : newStudents) {
                    repository.saveStudent(student);
                    added++;
                }
            } catch (RuntimeException ex) {
                onStudentsChanged.run();
                showError(
                        "Imported " + added
                                + " row(s), then stopped: "
                                + ex.getMessage());
                return;
            }

            onStudentsChanged.run();

            String summary = "Imported " + added
                    + " student(s). Duplicate IDs skipped: "
                    + duplicateCount;

            statusLabel.setText(summary);
            showMessage(
                    summary,
                    "Import complete",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException | IllegalArgumentException ex) {
            showError("Could not import CSV: " + ex.getMessage());
        }
    }

    private List<Student> parseStudents(Path file) throws IOException {
        List<String> lines =
                Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Student> students = new ArrayList<>();

        int idColumn = 0;
        int nameColumn = 1;
        boolean headerChecked = false;

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);

            if (line.isBlank()) {
                continue;
            }

            List<String> fields = parseCsvLine(line);

            if (!headerChecked) {
                headerChecked = true;

                int detectedIdColumn = -1;
                int detectedNameColumn = -1;

                for (int column = 0; column < fields.size(); column++) {
                    String header =
                            normalizeHeader(fields.get(column));

                    if (isIdHeader(header)) {
                        detectedIdColumn = column;
                    } else if (isNameHeader(header)) {
                        detectedNameColumn = column;
                    }
                }

                if (detectedIdColumn >= 0 && detectedNameColumn >= 0) {
                    idColumn = detectedIdColumn;
                    nameColumn = detectedNameColumn;
                    continue;
                }

                // Supports roster files with No., Student-ID, Name
                // followed by additional columns.
                if (fields.size() >= 3) {
                    idColumn = 1;
                    nameColumn = 2;

                    if (!isDigits(fields.get(0))
                            || !isDigits(fields.get(1))) {
                        continue;
                    }
                }
            }

            if (fields.size() <= Math.max(idColumn, nameColumn)) {
                throw new IllegalArgumentException(
                        "Line " + (lineNumber + 1)
                                + " does not contain the student ID "
                                + "and name columns.");
            }

            String studentId = fields.get(idColumn).trim();
            String name = fields.get(nameColumn).trim();

            if (studentId.isEmpty() || name.isEmpty()) {
                throw new IllegalArgumentException(
                        "Line " + (lineNumber + 1)
                                + " has an empty student ID or name.");
            }

            if (name.matches("[?\\s]+")) {
                throw new IllegalArgumentException(
                        "Line " + (lineNumber + 1)
                                + " has a name that appears corrupted. "
                                + "Re-save the source file as CSV UTF-8.");
            }

            students.add(new Student(name, studentId));
        }

        if (students.isEmpty()) {
            throw new IllegalArgumentException(
                    "The CSV file contains no student rows.");
        }

        return students;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);

            if (current == '"') {
                if (inQuotes
                        && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (current == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException(
                    "A CSV row has an unmatched quote.");
        }

        fields.add(field.toString());

        if (!fields.isEmpty()
                && fields.get(0).startsWith("\uFEFF")) {
            fields.set(0, fields.get(0).substring(1));
        }

        return fields;
    }

    private String normalizeHeader(String header) {
        return header.replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace(" ", "");
    }

    private boolean isIdHeader(String value) {
        return value.equals("studentid")
                || value.equals("student-id")
                || value.equals("id")
                || value.equals("รหัสนิสิต");
    }

    private boolean isNameHeader(String value) {
        return value.equals("name") || value.equals("ชื่อ");
    }

    private boolean isDigits(String value) {
        return value != null && value.trim().matches("\\d+");
    }

    private void exportPaymentReport() {
        Object[] choices = {
            "Selected year",
            "All years"
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                "Export the year selected in List and Check, "
                        + "or export all years?",
                "Choose report year",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]);

        if (choice < 0) {
            return;
        }

        boolean allYears = choice == 1;

        List<String> rows;
        try {
            rows = paymentReportRowsSupplier.apply(allYears);
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
            return;
        }

        if (rows == null || rows.isEmpty()) {
            showError("The payment report contains no data.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(
                "Save payment completion report as CSV");
        chooser.setSelectedFile(new File(
                allYears
                        ? "payment-report-all-years.csv"
                        : "payment-report-selected-year.csv"));

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = ensureCsvExtension(
                chooser.getSelectedFile().toPath());

        if (Files.exists(file)) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "The file already exists. Replace it?",
                    "Confirm overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Files.write(file, rows, StandardCharsets.UTF_8);

            String summary = "Exported " + (rows.size() - 1)
                    + " report record(s) to " + file.getFileName();

            statusLabel.setText(summary);
            showMessage(
                    summary,
                    "Report export complete",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            showError(
                    "Could not export payment report: "
                            + ex.getMessage());
        }
    }

    private Path ensureCsvExtension(Path file) {
        String name = file.getFileName().toString();

        if (name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return file;
        }

        return file.resolveSibling(name + ".csv");
    }

    private void showMessage(
            String message,
            String title,
            int messageType) {

        JOptionPane.showMessageDialog(
                this, message, title, messageType);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "CSV operation failed",
                JOptionPane.ERROR_MESSAGE);
    }
}