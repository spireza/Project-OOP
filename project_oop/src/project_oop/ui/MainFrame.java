package project_oop.ui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.UIManager;
import project_oop.InMemoryFundRepository;
import project_oop.ReportService;
import project_oop.CsvFundRepository;
import project_oop.FileFundRepository;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;

public class MainFrame extends JFrame {

	public MainFrame() {
		applyApplicationFonts();

		CsvFundRepository repository = new CsvFundRepository();
		ReportService reportService = new ReportService(repository);

		JTabbedPane tabs = new JTabbedPane();

		setTitle("กองกลางเจ้อ");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 700);
		setLocationRelativeTo(null);

		PaymentPanel paymentPanel = new PaymentPanel(repository);
		tabs.addTab("List and Check", paymentPanel);
		tabs.addTab("View all", new OverviewPanel(reportService, repository));
		tabs.addTab("Mission", new MissionPanel(repository));
		tabs.addTab("File", new StudentCsvPanel(repository, paymentPanel::reloadStudents,
				paymentPanel::buildPaymentReportCsv));

		add(tabs, BorderLayout.CENTER);
	}

	private void applyApplicationFonts() {
		// Tahoma includes Thai glyphs on Windows, unlike some logical-font mappings
		// that can render imported Thai names as empty square boxes.
		Font regularFont = new Font("Tahoma", Font.PLAIN, 14);
		Font boldFont = regularFont.deriveFont(Font.BOLD);

		// ฟอนต์หลักให้เหมือนกันทุกแท็บ
		UIManager.put("Label.font", boldFont);
		UIManager.put("Button.font", boldFont);
		UIManager.put("TabbedPane.font", boldFont);
		UIManager.put("TableHeader.font", boldFont);

		// ข้อความในช่องกรอกและข้อมูลตารางใช้ฟอนต์ปกติเพื่อให้อ่านง่าย
		UIManager.put("TextField.font", regularFont);
		UIManager.put("TextArea.font", regularFont);
		UIManager.put("ComboBox.font", regularFont);
		UIManager.put("Table.font", regularFont);
		UIManager.put("List.font", regularFont);
	}

	private JPanel createPlaceholder(String text) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.CENTER);
		return panel;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			MainFrame frame = new MainFrame();
			frame.setVisible(true);
		});
	}

}
