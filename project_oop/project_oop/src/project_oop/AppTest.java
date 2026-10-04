package project_oop;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.LocalDate;

public class AppTest {
	public static void main(String[] args) {
		// สร้าง repository หนึ่งตัวให้ทุก service ใช้ข้อมูลชุดเดียวกัน
		FundRepository repository = new InMemoryFundRepository();

		StudentService studentService = new StudentService(repository);
		PaymentService paymentService = new PaymentService(repository);
		MissionService missionService = new MissionService(repository);
		ReportService reportService = new ReportService(repository);

		// เพิ่มนักเรียนก่อนบันทึกการชำระเงิน
		studentService.registerStudent(new Student("สมชาย ใจดี", "6820501811"));

		// บันทึกรับเงินเดือนกันยายน 2026
		paymentService.markReceived("6820501811", YearMonth.of(2026, 9), new BigDecimal("10000"));
		
		MissionExpense expense = new MissionExpense(
		        "M001",
		        "ซื้ออุปกรณ์",
		        "อุปกรณ์",
		        "ซื้ออุปกรณ์สำหรับกิจกรรม",
		        new BigDecimal("100.00"),
		        LocalDate.of(2026, 9, 28)
		);

		missionService.recordExpense(expense);

		// ดูยอดรายรับแยกเดือนและยอดคงเหลือ
		System.out.println("ยอดรับแยกตามเดือน: " + reportService.getMonthlySummary(2026));
		System.out.println("ยอดกองทุนคงเหลือ: " + reportService.getFundBalance(2026));
	}
}