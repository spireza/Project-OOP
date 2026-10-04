package project_oop;

import java.math.BigDecimal;
import java.util.Objects;

public class MissionService {
	
	private final FundRepository repository;

	public MissionService(FundRepository repository) {
		
		this.repository = Objects.requireNonNull(repository,"Repository is not be null");
		
	}
	
	public void  recordExpense(MissionExpense expense)
	{
		Objects.requireNonNull(expense, "Expense is not be null");
		int year = expense.getExpenseDate().getYear();
		
		for (MissionExpense  exsiting : repository.findMissions(year)) {
			if(exsiting.getMissionId().equals(expense.getMissionId()))
			{
				throw new IllegalArgumentException(" บันทึกรายการซ้ำเเล้ว.");
			}
		}
		repository.saveMission(expense);
	}

}
