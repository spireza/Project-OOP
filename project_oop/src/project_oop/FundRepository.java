package project_oop;

import java.util.List;

public interface FundRepository {

	List<Student> findAllStudents();

    List<MonthlyPayment> findPayments(int year);

    List<MissionExpense> findMissions(int year);

    void savePayment(MonthlyPayment payment);

    void saveMission(MissionExpense mission);
    
    void saveStudent(Student student);
}
