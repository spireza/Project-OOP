package project_oop;

import java.time.LocalDate;
import java.util.Objects;
import java.math.BigDecimal;
import java.io.Serializable;

public class MissionExpense implements Serializable{

	private final String missionId;
	private final String activityName;
	private final String category;
	private final String detail;
	private final BigDecimal amount;
	private final LocalDate expenseDate;
	private static final long serialVersionUID = 1L;

	public MissionExpense(String missionId, String activityName, String category, String detail, BigDecimal amount,
			LocalDate expenseDate) {
		if (missionId == null || missionId.isBlank()) {
			throw new IllegalArgumentException("Mission Must not Empty");
		}

		if (activityName == null || activityName.isBlank()) {
			throw new IllegalArgumentException("Activity Must not Empty");
		}

		if (category == null || category.isBlank()) {
			throw new IllegalArgumentException("Category Must not Empty");
		}
		
		if(detail == null || detail.isBlank())
		{
			throw new IllegalArgumentException("Detail Must not Empty");
		}
		
		Objects.requireNonNull(amount,"Amount Must not Empty");
		Objects.requireNonNull(expenseDate,"expenseDate Must not Empty");
		
		if(amount.signum() < 0)
		{
			throw new IllegalArgumentException("Amount Must not be negative");
		}
		this.missionId = missionId;
		this.activityName = activityName;
		this.category = category;
		this.detail = detail;
		this.amount = amount;
		this.expenseDate = expenseDate;
	}
	
	

	public final String getMissionId() {
		return missionId;
	}



	public final String getActivityName() {
		return activityName;
	}



	public final String getCategory() {
		return category;
	}



	public final String getDetail() {
		return detail;
	}



	public final LocalDate getExpenseDate() {
		return expenseDate;
	}



	public final BigDecimal getAmount() {
		return amount;
	}

}
