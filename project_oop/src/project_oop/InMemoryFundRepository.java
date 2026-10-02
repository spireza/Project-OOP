package project_oop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.YearMonth;
import java.util.Objects;

public class InMemoryFundRepository implements FundRepository {

	private final List<Student> students;
	private final List<MonthlyPayment> payment;
	private final List<MissionExpense> mission;
	private final Map<String, String> paymentDetails;

	public InMemoryFundRepository() {
		students = new ArrayList<Student>();
		payment = new ArrayList<MonthlyPayment>();
		mission = new ArrayList<MissionExpense>();
		paymentDetails = new HashMap<>();
	}

	@Override
	public List<Student> findAllStudents() {
		return new ArrayList<>(students);
	}

	@Override
	public List<MonthlyPayment> findPayments(int year) {
		List<MonthlyPayment> result = new ArrayList<>();
		for (MonthlyPayment payment : this.payment) {
			if (payment.getPeriod().getYear() == year) {
				result.add(payment);
			}
		}
		return result;
	}

	@Override
	public List<MissionExpense> findMissions(int year) {
		List<MissionExpense> result = new ArrayList<>();
		
		for (MissionExpense missionExpense : mission) {
			if(missionExpense.getExpenseDate().getYear() == year)
			{
				result.add(missionExpense);
			}
		}
		return result;
	}

	@Override
	public void savePayment(MonthlyPayment payment) {
		this.payment.add(Objects.requireNonNull(payment,"payment is not be null"));
	}

	@Override
	public void deletePayment(String studentId, YearMonth period) {
		Objects.requireNonNull(studentId, "studentId must not be null");
		Objects.requireNonNull(period, "period must not be null");
		boolean removed = payment.removeIf(item -> item.getStudentId().equals(studentId)
				&& item.getPeriod().equals(period));
		if (!removed) {
			throw new IllegalArgumentException("ไม่พบรายการชำระของเดือนนี้");
		}
	}

	@Override
	public int deletePayments(YearMonth period) {
		Objects.requireNonNull(period, "period must not be null");
		int originalSize = payment.size();
		payment.removeIf(item -> item.getPeriod().equals(period));
		return originalSize - payment.size();
	}

	@Override
	public String findPaymentDetail(String studentId, int year) {
		return paymentDetails.getOrDefault(paymentDetailKey(studentId, year), "");
	}

	@Override
	public void savePaymentDetail(String studentId, int year, String detail) {
		Objects.requireNonNull(studentId, "studentId must not be null");
		paymentDetails.put(paymentDetailKey(studentId, year), Objects.requireNonNull(detail, "detail must not be null"));
	}

	private String paymentDetailKey(String studentId, int year) {
		return year + "|" + studentId;
	}

	@Override
	public void saveMission(MissionExpense mission) {
		this.mission.add(Objects.requireNonNull(mission,"mission is not be null"));

	}

	@Override
	public void saveStudent(Student student) {
		 students.add(Objects.requireNonNull(student, "student is not be null"));
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
	}

}
