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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void deletePayment(String studentId, YearMonth period) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        boolean removed = data.payments.removeIf(payment -> payment.getStudentId().equals(studentId)
                && payment.getPeriod().equals(period));
        if (!removed) {
            throw new IllegalArgumentException("ไม่พบรายการชำระของเดือนนี้");
        }
        saveData();
    }

    @Override
    public int deletePayments(YearMonth period) {
        Objects.requireNonNull(period, "period must not be null");
        int originalSize = data.payments.size();
        data.payments.removeIf(payment -> payment.getPeriod().equals(period));
        int deletedCount = originalSize - data.payments.size();
        if (deletedCount > 0) {
            saveData();
        }
        return deletedCount;
    }

    @Override
    public String findPaymentDetail(String studentId, int year) {
        return getPaymentDetails().getOrDefault(paymentDetailKey(studentId, year), "");
    }

    @Override
    public void savePaymentDetail(String studentId, int year, String detail) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        getPaymentDetails().put(paymentDetailKey(studentId, year),
                Objects.requireNonNull(detail, "detail must not be null"));
        saveData();
    }

    @Override
    public void saveMission(MissionExpense mission) {
        data.missions.add(
                Objects.requireNonNull(mission, "mission must not be null"));
        saveData();
    }

    @Override
    public void updateMission(MissionExpense updatedMission) {
        Objects.requireNonNull(updatedMission, "mission must not be null");
        for (int index = 0; index < data.missions.size(); index++) {
            if (data.missions.get(index).getMissionId().equals(updatedMission.getMissionId())) {
                data.missions.set(index, updatedMission);
                saveData();
                return;
            }
        }
        throw new IllegalArgumentException("ไม่พบรายการ Mission ที่ต้องการแก้ไข");
    }

    @Override
    public void deleteMission(String missionId) {
        Objects.requireNonNull(missionId, "missionId must not be null");
        boolean removed = data.missions.removeIf(mission -> mission.getMissionId().equals(missionId));
        if (!removed) {
            throw new IllegalArgumentException("ไม่พบรายการ Mission ที่ต้องการลบ");
        }
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
        private Map<String, String> paymentDetails = new HashMap<>();
    }

    private String paymentDetailKey(String studentId, int year) {
        return year + "|" + studentId;
    }

    private Map<String, String> getPaymentDetails() {
        // ค่า null อาจเกิดเมื่ออ่านไฟล์ที่สร้างจาก FundData เวอร์ชันเก่า
        if (data.paymentDetails == null) {
            data.paymentDetails = new HashMap<>();
        }
        return data.paymentDetails;
    }

    @Override
    public void deleteStudent(String studentId) {
        Objects.requireNonNull(studentId, "studentId must not be null");

        boolean removed = data.students.removeIf(
                student -> student.getStudentId().equals(studentId));

        if (!removed) {
            throw new IllegalArgumentException("ไม่พบรหัสนิสิตนี้");
        }

        getPaymentDetails().keySet().removeIf(key -> key.endsWith("|" + studentId));

        saveData();
    }
}
