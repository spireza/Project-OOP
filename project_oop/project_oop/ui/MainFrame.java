package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import project_oop.CsvFundRepository;
import project_oop.ReportService;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	public MainFrame() {
		applyApplicationFonts();

		CsvFundRepository repository = new CsvFundRepository();
		ReportService reportService = new ReportService(repository);

		setTitle("กองกลางเจ้อ");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 700);
		setLocationRelativeTo(null);

		PaymentPanel paymentPanel = new PaymentPanel(repository);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("List and Check", paymentPanel);
		tabs.addTab("View all", new OverviewPanel(reportService, repository));
		tabs.addTab("Mission", new MissionPanel(repository));

		tabs.addTab("File", new StudentCsvPanel(repository, paymentPanel::reloadStudents,
				allYears -> paymentPanel.buildPaymentReportCsv(allYears)));

		add(tabs, BorderLayout.CENTER);
	}

	private void applyApplicationFonts() {
		Font regularFont = new Font("Tahoma", Font.PLAIN, 14);
		Font boldFont = regularFont.deriveFont(Font.BOLD);

		UIManager.put("Label.font", boldFont);
		UIManager.put("Button.font", boldFont);
		UIManager.put("TabbedPane.font", boldFont);
		UIManager.put("TableHeader.font", boldFont);

		UIManager.put("TextField.font", regularFont);
		UIManager.put("TextArea.font", regularFont);
		UIManager.put("ComboBox.font", regularFont);
		UIManager.put("Table.font", regularFont);
		UIManager.put("List.font", regularFont);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			MainFrame frame = new MainFrame();
			frame.setVisible(true);
		});
	}
}