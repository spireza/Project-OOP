package project_oop.ui;

import java.awt.BorderLayout;

import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import project_oop.FundRepository;
import project_oop.MonthlyPayment;
import project_oop.PaymentService;
import project_oop.Student;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class PaymentPanel extends JPanel {

	private final JComboBox<Integer> yearComboBox;
	private final JTable paymentTable;
	private final DefaultTableModel tableModel;
	private final FundRepository repository;
	private final JTextField monthlyAmountField;
	private final PaymentService paymentService;

	private boolean loadingTable;

	public PaymentPanel(FundRepository repository) {

		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.paymentService = new PaymentService(repository);

		setLayout(new BorderLayout(10, 10));

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Year"));

		yearComboBox = new JComboBox<>(new Integer[] { 2568, 2569, 2570 });
		topPanel.add(yearComboBox);

		topPanel.add(new JLabel("Amount/month:"));
		monthlyAmountField = new JTextField("100", 6);
		topPanel.add(monthlyAmountField);

		JButton addStudentButton = new JButton("Add Student");
		addStudentButton.addActionListener(e -> addStudent());
		topPanel.add(addStudentButton);
		add(topPanel, BorderLayout.NORTH);

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
				return column >= 3;
			}
		};

		paymentTable = new JTable(tableModel);
		paymentTable.setRowHeight(26);
		paymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setColumnWidths();
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		paymentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

		tableModel.addTableModelListener(event -> handleTableChange(event));
		add(new JScrollPane(paymentTable), BorderLayout.CENTER);

		yearComboBox.addActionListener(e -> loadStudents());
		loadStudents();
	}

	private void addStudent() {
		String studentId = JOptionPane.showInputDialog(this, "Input student-ID");
		if (studentId == null) {
			return;
		}

		String name = JOptionPane.showInputDialog(this, "Input name");
		if (name == null) {
			return;
		}

		try {
			repository.saveStudent(new Student(name, studentId));
			loadStudents();
		} catch (RuntimeException ex) {
			showError(ex.getMessage());
		}
	}

	private void handleTableChange(TableModelEvent event) {
		if (loadingTable || event.getType() != TableModelEvent.UPDATE || event.getColumn() < 3) {
			return;
		}

		int row = event.getFirstRow();
		int column = event.getColumn();
		boolean checked = Boolean.TRUE.equals(tableModel.getValueAt(row, column));

		// ยังไม่รองรับการลบรายการชำระ จึงคืน checkbox ให้เป็นสถานะจริง
		if (!checked) {
			loadStudents();
			return;
		}

		try {
			BigDecimal amount = new BigDecimal(monthlyAmountField.getText().trim());
			if (amount.signum() <= 0) {
				throw new IllegalArgumentException("Amount must be greater than 0");
			}

			String studentId = (String) tableModel.getValueAt(row, 1);
			int buddhistYear = (Integer) yearComboBox.getSelectedItem();
			int christianYear = buddhistYear - 543;
			int month = column - 2;

			paymentService.markReceived(studentId, YearMonth.of(christianYear, month), amount);
			loadStudents();
		} catch (RuntimeException ex) {
			loadStudents();
			showError(ex.getMessage());
		}
	}

	private void loadStudents() {
		loadingTable = true;
		try {
			tableModel.setRowCount(0);

			int buddhistYear = (Integer) yearComboBox.getSelectedItem();
			int christianYear = buddhistYear - 543;
			List<MonthlyPayment> payments = repository.findPayments(christianYear);

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
		} finally {
			loadingTable = false;
		}
	}

	private void setColumnWidths() {
		paymentTable.getColumnModel().getColumn(0).setPreferredWidth(45);
		paymentTable.getColumnModel().getColumn(1).setPreferredWidth(110);
		paymentTable.getColumnModel().getColumn(2).setPreferredWidth(150);

		for (int column = 3; column < 15; column++) {
			paymentTable.getColumnModel().getColumn(column).setPreferredWidth(55);
		}
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message == null ? "Unknown error" : message, "Operation failed",
				JOptionPane.ERROR_MESSAGE);
	}
}
