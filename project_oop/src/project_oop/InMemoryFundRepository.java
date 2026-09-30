package project_oop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryFundRepository implements FundRepository {

	private final List<Student> students;
	private final List<MonthlyPayment> payment;
	private final List<MissionExpense> mission;

	public InMemoryFundRepository() {
		students = new ArrayList<Student>();
		payment = new ArrayList<MonthlyPayment>();
		mission = new ArrayList<MissionExpense>();
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
	public void saveMission(MissionExpense mission) {
		this.mission.add(Objects.requireNonNull(mission,"mission is not be null"));

	}

	@Override
	public void saveStudent(Student student) {
		 students.add(Objects.requireNonNull(student, "student is not be null"));
	}

}
