package project_oop.ui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import project_oop.InMemoryFundRepository;
import project_oop.ReportService;
import project_oop.CsvFundRepository;
import project_oop.FileFundRepository;

public class MainFrame extends JFrame {

	public MainFrame() {

		CsvFundRepository repository = new CsvFundRepository();
		ReportService reportService = new ReportService(repository);

		JTabbedPane tabs = new JTabbedPane();

		setTitle("กองกลางเจ้อ");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 700);
		setLocationRelativeTo(null);

		tabs.addTab("List and Check", new PaymentPanel(repository));
		tabs.addTab("View all", new OverviewPanel(reportService, repository));
		tabs.addTab("Mission", new MissionPanel(repository));

		add(tabs, BorderLayout.CENTER);
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
