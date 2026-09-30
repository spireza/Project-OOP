package project_oop;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;



public class ReportService {
	
	private final FundRepository repository;
	
	public ReportService(FundRepository repository) {
		this.repository =  Objects.requireNonNull(repository,"Repository is not be null");
	}
	
	public Map<Integer,BigDecimal> getMonthlySummary(int year)
	{
		Map<Integer,BigDecimal> monthlytotal = new LinkedHashMap<>();
		
		for(int month = 1; month <= 12; month++)
		{
			monthlytotal.put(month,BigDecimal.ZERO);
		}
		
		for (MonthlyPayment payment : repository.findPayments(year)) {
				
			int month = payment.getPeriod().getMonthValue();
			BigDecimal currentotals = monthlytotal.get(month);
			monthlytotal.put(month, currentotals.add(payment.getAmount()));
		}
		
		return monthlytotal;
	}
	
	public BigDecimal getFundBalance(int year)
	{
		BigDecimal income = BigDecimal.ZERO;
		
		for (MonthlyPayment payment : repository.findPayments(year)) {
			income = income.add(payment.getAmount());
		}
		
	  BigDecimal expense = BigDecimal.ZERO;
	  
	  for (MissionExpense mission : repository.findMissions(year)) {
		  	expense = expense.add(mission.getAmount());
	}
		return income.subtract(expense);
	}
}
