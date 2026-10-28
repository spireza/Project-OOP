package project_oop;

import java.util.List;

public interface FundRepository {

	List<Student> findAllStudents();

    List<MonthlyPayment> findPayments(int year);

    List<MissionExpense> findMissions(int year);

	void savePayment(MonthlyPayment payment);

	void deletePayment(String studentId, java.time.YearMonth period);

	int deletePayments(java.time.YearMonth period);

	String findPaymentDetail(String studentId, int year);

	void savePaymentDetail(String studentId, int year, String detail);

    void saveMission(MissionExpense mission);

    void updateMission(MissionExpense mission);

    void deleteMission(String missionId);
    
    void saveStudent(Student student);
    
    void deleteStudent(String studentId);
}
