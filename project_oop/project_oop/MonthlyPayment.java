package project_oop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.io.Serializable;

public class MonthlyPayment implements Serializable{

	private final String studentId;
	private final YearMonth period;
	private final BigDecimal amount;
	private final LocalDate receivedDate;
	private static final long serialVersionUID = 1L;

	public MonthlyPayment(String studentId,YearMonth period,BigDecimal amount,LocalDate receivedDate) {
		
		
		if (studentId == null || studentId.isBlank()) {
	        throw new IllegalArgumentException("studentId ต้องไม่ว่าง");
	    }

	    Objects.requireNonNull(period, "period must not be null");
	    Objects.requireNonNull(amount, "amount must not be null");
	    Objects.requireNonNull(receivedDate, "receivedDate must not be null");

	    if (amount.signum() < 0) {
	        throw new IllegalArgumentException("amount ต้องไม่ติดลบ");
	    }

	    this.studentId = studentId;
	    this.period = period;
	    this.amount = amount;
	    this.receivedDate = receivedDate;
		
	}

	
	
	public String getStudentId() {
		return studentId;
	}


	public YearMonth getPeriod() {
		return period;
	}


	public LocalDate getReceivedDate() {
		return receivedDate;
	}


	public BigDecimal getAmount() {
		return amount;

	}
}
