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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CsvFundRepository implements FundRepository {

	private final Path dataDirectory;

	private final List<Student> students = new ArrayList<>();
	private final List<MonthlyPayment> payments = new ArrayList<>();
	private final List<MissionExpense> missions = new ArrayList<>();
	private final Map<String, String> paymentDetails = new HashMap<>();

	public CsvFundRepository() {
		this(Path.of("data"));
	}

	public CsvFundRepository(Path dataDirectory) {
		this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory must not be null");

		students.addAll(readStudents());
		payments.addAll(readPayments());
		missions.addAll(readMissions());
		paymentDetails.putAll(readPaymentDetails());
	}

	@Override
	public List<Student> findAllStudents() {
		
		List<Student> sortedStudents = new ArrayList<>(students);
		sortedStudents.sort(Comparator.comparing(Student::getStudentId));

		return sortedStudents;

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
				throw new IllegalArgumentException("This list already exists.");
			}
		}

		students.add(student);
		writeStudents();
	}

	@Override
	public void savePayment(MonthlyPayment payment) {
		payments.add(Objects.requireNonNull(payment, "payment must not be null"));

		writePayments();
	}

	@Override
	public void deletePayment(String studentId, YearMonth period) {
		Objects.requireNonNull(studentId, "studentId must not be null");
		Objects.requireNonNull(period, "period must not be null");
		boolean removed = payments.removeIf(payment -> payment.getStudentId().equals(studentId)
				&& payment.getPeriod().equals(period));
		if (!removed) {
			throw new IllegalArgumentException("ไม่พบรายการชำระของเดือนนี้");
		}
		writePayments();
	}

	@Override
	public int deletePayments(YearMonth period) {
		Objects.requireNonNull(period, "period must not be null");
		int originalSize = payments.size();
		payments.removeIf(payment -> payment.getPeriod().equals(period));
		int deletedCount = originalSize - payments.size();
		if (deletedCount > 0) {
			writePayments();
		}
		return deletedCount;
	}

	@Override
	public String findPaymentDetail(String studentId, int year) {
		return paymentDetails.getOrDefault(paymentDetailKey(studentId, year), "");
	}

	@Override
	public void savePaymentDetail(String studentId, int year, String detail) {
		Objects.requireNonNull(studentId, "studentId must not be null");
		Objects.requireNonNull(detail, "detail must not be null");
		paymentDetails.put(paymentDetailKey(studentId, year), detail);
		writePaymentDetails();
	}

	@Override
	public void saveMission(MissionExpense mission) {
		missions.add(Objects.requireNonNull(mission, "mission must not be null"));

		writeMissions();
	}
	@Override
	public void deleteStudent(String studentId) {
	    Objects.requireNonNull(studentId, "studentId must not be null");

	    boolean removed = students.removeIf(
	            student -> student.getStudentId().equals(studentId));

	    if (!removed) {
	        throw new IllegalArgumentException("ไม่พบรหัสนิสิตนี้");
	    }
	    paymentDetails.keySet().removeIf(key -> key.endsWith("|" + studentId));

	    writeStudents();
	    writePaymentDetails();
	}

	private String paymentDetailKey(String studentId, int year) {
		return year + "|" + studentId;
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
			result.add(new MonthlyPayment(row.get(0), YearMonth.parse(row.get(1)), new BigDecimal(row.get(2)),
					LocalDate.parse(row.get(3))));
		}

		return result;
	}

	private Map<String, String> readPaymentDetails() {
		Map<String, String> result = new HashMap<>();
		for (List<String> row : readCsv("payment-details.csv")) {
			if (row.get(0).equals("studentId")) {
				continue;
			}
			requireColumns(row, 3);
			result.put(paymentDetailKey(row.get(0), Integer.parseInt(row.get(1))), row.get(2));
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
			result.add(new MissionExpense(row.get(0), row.get(1), row.get(2), row.get(3), new BigDecimal(row.get(4)),
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
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (!line.isBlank()) {
					rows.add(parseCsvLine(line));
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot Read File: " + file, ex);
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
				if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
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

		String singleLine = value.replace("\r", " ").replace("\n", " ");

		return "\"" + singleLine.replace("\"", "\"\"") + "\"";
	}

	private void writeStudents() {
		List<String> lines = new ArrayList<>();
		lines.add("\uFEFFstudentId,name");

		for (Student student : students) {
			lines.add(csv(student.getStudentId()) + "," + csv(student.getName()));
		}

		writeCsv("students.csv", lines);
	}

	private void writePayments() {
		List<String> lines = new ArrayList<>();
		lines.add("\uFEFFstudentId,period,amount,receivedDate");

		for (MonthlyPayment payment : payments) {
			lines.add(csv(payment.getStudentId()) + "," + csv(payment.getPeriod().toString()) + ","
					+ csv(payment.getAmount().toPlainString()) + "," + csv(payment.getReceivedDate().toString()));
		}

		writeCsv("payments.csv", lines);
	}

	private void writePaymentDetails() {
		List<String> lines = new ArrayList<>();
		lines.add("\uFEFFstudentId,year,detail");
		for (Map.Entry<String, String> entry : paymentDetails.entrySet()) {
			String[] key = entry.getKey().split("\\|", 2);
			lines.add(csv(key[1]) + "," + csv(key[0]) + "," + csv(entry.getValue()));
		}
		writeCsv("payment-details.csv", lines);
	}

	private void writeMissions() {
		List<String> lines = new ArrayList<>();
		lines.add("\uFEFFmissionId,activityName,category,detail,amount,expenseDate");

		for (MissionExpense mission : missions) {
			lines.add(csv(mission.getMissionId()) + "," + csv(mission.getActivityName()) + ","
					+ csv(mission.getCategory()) + "," + csv(mission.getDetail()) + ","
					+ csv(mission.getAmount().toPlainString()) + "," + csv(mission.getExpenseDate().toString()));
		}

		writeCsv("missions.csv", lines);
	}

	private void writeCsv(String fileName, List<String> lines) {
		try {
			Files.createDirectories(dataDirectory);

			Files.write(dataDirectory.resolve(fileName), lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot Save file: " + fileName, ex);
		}
	}

	private void requireColumns(List<String> row, int expectedCount) {
		if (row.size() != expectedCount) {
			throw new IllegalStateException("Row is Wrong: " + row);
		}
	}
}
