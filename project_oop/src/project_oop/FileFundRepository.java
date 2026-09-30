package project_oop;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileFundRepository implements FundRepository {

    private final Path dataFile;
    private final FundData data;

    public FileFundRepository() {
        this(Path.of("data", "funds.dat"));
    }

    public FileFundRepository(Path dataFile) {
        this.dataFile = Objects.requireNonNull(
                dataFile, "dataFile must not be null");
        this.data = loadData();
    }

    @Override
    public List<Student> findAllStudents() {
        return new ArrayList<>(data.students);
    }

    @Override
    public List<MonthlyPayment> findPayments(int year) {
        List<MonthlyPayment> result = new ArrayList<>();

        for (MonthlyPayment payment : data.payments) {
            if (payment.getPeriod().getYear() == year) {
                result.add(payment);
            }
        }

        return result;
    }

    @Override
    public List<MissionExpense> findMissions(int year) {
        List<MissionExpense> result = new ArrayList<>();

        for (MissionExpense mission : data.missions) {
            if (mission.getExpenseDate().getYear() == year) {
                result.add(mission);
            }
        }

        return result;
    }

    @Override
    public void saveStudent(Student student) {
        Objects.requireNonNull(student, "student must not be null");

        for (Student existing : data.students) {
            if (existing.getStudentId().equals(student.getStudentId())) {
                throw new IllegalArgumentException(
                        "This list already exists");
            }
        }

        data.students.add(student);
        saveData();
    }

    @Override
    public void savePayment(MonthlyPayment payment) {
        data.payments.add(
                Objects.requireNonNull(payment, "payment must not be null"));
        saveData();
    }

    @Override
    public void saveMission(MissionExpense mission) {
        data.missions.add(
                Objects.requireNonNull(mission, "mission must not be null"));
        saveData();
    }

    private FundData loadData() {
        if (!Files.exists(dataFile)) {
            return new FundData();
        }

        try (ObjectInputStream input = new ObjectInputStream(
                Files.newInputStream(dataFile))) {

            Object loaded = input.readObject();

            if (!(loaded instanceof FundData)) {
                throw new IllegalStateException(
                        "filed is Wrong");
            }

            return (FundData) loaded;

        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Read is not Success: " + dataFile, ex);
        }
    }

    private void saveData() {
        try {
            Path parent = dataFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (ObjectOutputStream output = new ObjectOutputStream(
                    Files.newOutputStream(dataFile))) {
                output.writeObject(data);
            }

        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Save is not Success: " + dataFile, ex);
        }
    }

    private static class FundData implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private final List<Student> students = new ArrayList<>();
        private final List<MonthlyPayment> payments = new ArrayList<>();
        private final List<MissionExpense> missions = new ArrayList<>();
    }
    @Override
    public void deleteStudent(String studentId) {
        Objects.requireNonNull(studentId, "studentId must not be null");

        boolean removed = data.students.removeIf(
                student -> student.getStudentId().equals(studentId));

        if (!removed) {
            throw new IllegalArgumentException("ไม่พบรหัสนิสิตนี้");
        }

        saveData();
    }
}