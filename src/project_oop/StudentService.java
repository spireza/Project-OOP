package project_oop;

import java.util.Objects;

public class StudentService {
	private final FundRepository repository;

	public StudentService(FundRepository repository) {
		this.repository = Objects.requireNonNull(repository,"Repository is not be null");
	}
	public void registerStudent(Student studentID) {
        Objects.requireNonNull(studentID, "student ต้องไม่เป็น null");
        repository.saveStudent(studentID);
    }
}
