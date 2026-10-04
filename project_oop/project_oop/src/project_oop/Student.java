package project_oop;

import java.io.Serializable;
public class Student implements Serializable {
	private final String name;
	private final String studentId;
	private static final long serialVersionUID = 1L;
	public Student(String name ,String id){
		if(name == null || name.isBlank())
		{
			throw new IllegalArgumentException("Name is not Empty");
		}
		
		if(id == null || id.isBlank())
		{
			throw new IllegalArgumentException("Student-ID Must  not Be Empty");
		}
		
		this.name = name;
		this.studentId = id;
	}
	
	
	public String getName() {
		return name;
	}

	

	public String getStudentId() {
		return studentId;
	}

	
}
