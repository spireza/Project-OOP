package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import project_oop.FundRepository;
import project_oop.MonthlyPayment;
import project_oop.ReportService;
import project_oop.Student;

public class OverviewPanel extends JPanel {

	private final ReportService reportService;
	private final FundRepository repository;

	private final JComboBox<Integer> yearComboBox;
	private final JLabel incomeValueLabel;
	private final JLabel expenseValueLabel;
	private final JLabel balanceValueLabel;
	private final DefaultTableModel tableModel;

	private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("th-TH"));

	public OverviewPanel(ReportService reportService, FundRepository repository) {

		this.reportService = Objects.requireNonNull(reportService, "reportService must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");

		setLayout(new BorderLayout(10, 10));

		JPanel headerPanel = new JPanel(new BorderLayout());
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.add(new JLabel("Balance"), BorderLayout.WEST);

		yearComboBox = new JComboBox<>(new Integer[] { 2568, 2569, 2570 });
		yearComboBox.setSelectedItem(2568);

		JPanel yearPanel = new JPanel();
		yearPanel.add(new JLabel("Year:"));
		yearPanel.add(yearComboBox);
		titlePanel.add(yearPanel, BorderLayout.EAST);
		headerPanel.add(titlePanel, BorderLayout.NORTH);

		JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 10));
		incomeValueLabel = new JLabel("0 Bath", SwingConstants.CENTER);
		expenseValueLabel = new JLabel("0 Bath", SwingConstants.CENTER);
		balanceValueLabel = new JLabel("0 Bath", SwingConstants.CENTER);

		summaryPanel.add(createSummaryCard("ALL Income", incomeValueLabel));
		summaryPanel.add(createSummaryCard("Withdrawn", expenseValueLabel));
		summaryPanel.add(createSummaryCard("Remaining balance", balanceValueLabel));

		headerPanel.add(summaryPanel, BorderLayout.CENTER);
		add(headerPanel, BorderLayout.NORTH);

		String[] columns = { "No.", "Student-ID", "Name", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
				"Oct", "Nov", "Dec" };

		tableModel = new DefaultTableModel(columns, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 0) {
					return Integer.class;
				}
				return columnIndex >= 3 ? Boolean.class : String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JTable paymentTable = new JTable(tableModel);
		paymentTable.setRowHeight(28);
		paymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer();
		numberRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		paymentTable.getColumnModel().getColumn(0).setCellRenderer(numberRenderer);
		setColumnWidths(paymentTable);
		
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		paymentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
		add(new JScrollPane(paymentTable), BorderLayout.CENTER);

		yearComboBox.addActionListener(e -> refreshData());
		refreshData();
	}

	private JPanel createSummaryCard(String title, JLabel valueLabel) {

		JPanel card = new JPanel(new GridLayout(2, 1, 4, 4));
		card.add(new JLabel(title, SwingConstants.CENTER));
		card.add(valueLabel);
		return card;
	}

	private void refreshData() {
		int buddhistYear = (Integer) yearComboBox.getSelectedItem();
		int christianYear = buddhistYear - 543;

		updateSummary(christianYear);
		loadStudentPayments(christianYear);
	}

	private void updateSummary(int year) {
		Map<Integer, BigDecimal> monthlyTotals = reportService.getMonthlySummary(year);

		BigDecimal income = BigDecimal.ZERO;
		for (BigDecimal amount : monthlyTotals.values()) {
			income = income.add(amount);
		}

		BigDecimal balance = reportService.getFundBalance(year);
		BigDecimal expense = income.subtract(balance);

		incomeValueLabel.setText(formatMoney(income));
		expenseValueLabel.setText(formatMoney(expense));
		balanceValueLabel.setText(formatMoney(balance));
	}

	private void loadStudentPayments(int year) {
		tableModel.setRowCount(0);
		List<MonthlyPayment> payments = repository.findPayments(year);

		int rowNumber = 1;
		for (Student student : repository.findAllStudents()) {

			Object[] row = new Object[15];
			row[0] = rowNumber++;
			row[1] = student.getStudentId();
			row[2] = student.getName();

			for (int column = 3; column < row.length; column++) {
				row[column] = false;
			}

			for (MonthlyPayment payment : payments) {
				if (payment.getStudentId().equals(student.getStudentId())) {
					int monthColumn = payment.getPeriod().getMonthValue() + 2;
					row[monthColumn] = true;
				}
			}

			tableModel.addRow(row);
		}
	}

	private void setColumnWidths(JTable table) {

		table.getColumnModel().getColumn(0).setPreferredWidth(45);
		table.getColumnModel().getColumn(1).setPreferredWidth(110);
		table.getColumnModel().getColumn(2).setPreferredWidth(150);

		for (int column = 3; column < 15; column++) {
			table.getColumnModel().getColumn(column).setPreferredWidth(55);
		}
	}

	private String formatMoney(BigDecimal amount) {
		return numberFormat.format(amount) + " Bath";
	}
}
