package project_oop;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;

public class PaymentService {

	private final FundRepository repository;

	public PaymentService(FundRepository repository) {

		this.repository = Objects.requireNonNull(repository,"Repository is not be null");
	}

	public void markReceived(String studentId, YearMonth month, BigDecimal amount) {

		if(studentId== null || studentId.isBlank())
		{
			throw new IllegalArgumentException("Studen not be Empty");
		}
		Objects.requireNonNull(month, "month not be null");
	    Objects.requireNonNull(amount, "amount not be null");

	    if (amount.signum() < 0) {
	        throw new IllegalArgumentException("amount is not be nagative");
	    }

		boolean studentExsit = false;

		for (Student students : repository.findAllStudents()) {
			if (students.getStudentId().equals(studentId)) {
				studentExsit = true;
				break;
			}
		}

		if (!studentExsit) {
			throw new IllegalArgumentException("Students-ID is not found");
		}

		List<MonthlyPayment> payments = repository.findPayments(month.getYear());
		boolean findmonth = false;

		for (MonthlyPayment payment : payments) {
			if (payment.getStudentId().equals(studentId) && payment.getPeriod().equals(month)) {
				findmonth = true;
				break;
			}

		}

		if (studentExsit) {
			if (findmonth) {
				throw new IllegalArgumentException("This Student-ID is already paid this month.");
			}
		}

		MonthlyPayment newPayment = new MonthlyPayment(studentId, month, amount, LocalDate.now());

		repository.savePayment(newPayment);
	}

	public void cancelReceived(String studentId, YearMonth month) {
		if (studentId == null || studentId.isBlank()) {
			throw new IllegalArgumentException("Student ID must not be empty");
		}
		Objects.requireNonNull(month, "month must not be null");
		repository.deletePayment(studentId, month);
	}

	public int clearMonth(YearMonth month) {
		Objects.requireNonNull(month, "month must not be null");
		return repository.deletePayments(month);
	}


}
