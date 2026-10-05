package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Color;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingConstants;

import project_oop.FundRepository;
import project_oop.MonthlyPayment;
import project_oop.PaymentService;
import project_oop.Student;

public class PaymentPanel extends JPanel {
	private static final int PAGE_SIZE = 20;
	private static final int MAX_VISIBLE_PAGE_BUTTONS = 3;
	private static final Preferences UI_PREFERENCES = Preferences.userNodeForPackage(PaymentPanel.class);
	private static final String PREF_YEAR = "payment.selectedYear";
	private static final String PREF_MONTHS = "payment.displayedMonths";
	private static final String PREF_AMOUNTS = "payment.monthlyAmounts";
	private static final String PREF_SELECTED_MONTH = "payment.selectedMonth";

	private static final String[] MONTH_NAMES = {
			"Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

	private final JComboBox<Integer> yearComboBox;
	private final JTable paymentTable;
	private final DefaultTableModel tableModel;
	private final FundRepository repository;
	private final JTextField monthlyAmountField;
	private final PaymentService paymentService;
	private final TableColumn[] allColumns;
	private final boolean[] displayedMonthColumns = new boolean[12];
	private final BigDecimal[] monthlyAmounts = new BigDecimal[12];
	private int selectedMonth = -1;
	private int selectedCleanMonth = -1;
	private final JButton cleanButton;
	private final JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
	private final JLabel pageInfoLabel = new JLabel(" ");
	private final TableRowSorter<DefaultTableModel> rowSorter;
	private String searchQuery = "";
	private int currentPage;

	private boolean loadingTable;

	public PaymentPanel(FundRepository repository) {

		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.paymentService = new PaymentService(repository);

		setLayout(new BorderLayout(10, 10));

		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		controlsPanel.add(new JLabel("Year"));

		yearComboBox = new JComboBox<>(new Integer[] { 2568, 2569, 2570 });
		controlsPanel.add(yearComboBox);

		controlsPanel.add(new JLabel("Amount/month:"));
		monthlyAmountField = new JTextField("100", 6);
		controlsPanel.add(monthlyAmountField);

		JButton addStudentButton = new JButton("Add Student");
		JButton deleteStudentButton = new JButton("Delete Student");
		JButton checkButton = new JButton("Check");
		cleanButton = new JButton("Clean");
		cleanButton.setToolTipText("Click a month header, then click Clean to remove that month's data");
		Insets compactButtonMargin = new Insets(3, 6, 3, 6);
		deleteStudentButton.setMargin(compactButtonMargin);
		addStudentButton.setMargin(compactButtonMargin);
		checkButton.setMargin(compactButtonMargin);
		cleanButton.setMargin(compactButtonMargin);
		deleteStudentButton.addActionListener(e -> deleteSelectedStudent());
		controlsPanel.add(deleteStudentButton);
		addStudentButton.addActionListener(e -> addStudent());
		controlsPanel.add(addStudentButton);
		checkButton.addActionListener(e -> chooseMonthToCollect());
		controlsPanel.add(checkButton);
		cleanButton.addActionListener(e -> cleanSelectedMonth());
		controlsPanel.add(cleanButton);
		topPanel.add(controlsPanel, BorderLayout.WEST);

		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		searchPanel.add(new JLabel("Search:"));
		JTextField searchField = new JTextField(12);
		searchPanel.add(searchField);
		topPanel.add(searchPanel, BorderLayout.EAST);
		add(topPanel, BorderLayout.NORTH);

		String[] columns = { "No.", "Student-ID", "Name", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
				"Oct", "Nov", "Dec", "Check", "Detail" };

		tableModel = new DefaultTableModel(columns, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 0) {
					return Integer.class;
				}
				return columnIndex >= 3 && columnIndex <= 14 ? Boolean.class : String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column >= 3 && column <= 14 && displayedMonthColumns[column - 3];
			}
		};

		paymentTable = new JTable(tableModel);
		paymentTable.setRowHeight(26);
		// คงความกว้างคอลัมน์หลักไว้ ไม่ขยาย No./ID/Name ให้เต็มหน้าต่าง
		// เดือนที่เลือกจะถูกต่อเพิ่มทางขวาตามความกว้างจริงของแต่ละคอลัมน์
		paymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setColumnWidths();
		allColumns = new TableColumn[paymentTable.getColumnModel().getColumnCount()];
		for (int column = 0; column < allColumns.length; column++) {
			allColumns[column] = paymentTable.getColumnModel().getColumn(column);
		}
		paymentTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				int viewColumn = paymentTable.getTableHeader().columnAtPoint(event.getPoint());
				if (viewColumn < 0) {
					selectCleanMonth(-1);
					return;
				}
				int modelColumn = paymentTable.convertColumnIndexToModel(viewColumn);
				int monthIndex = modelColumn >= 3 && modelColumn <= 14 ? modelColumn - 3 : -1;
				selectCleanMonth(monthIndex >= 0 && displayedMonthColumns[monthIndex] ? monthIndex : -1);
			}
		});
		allColumns[15].setPreferredWidth(120);
		allColumns[16].setPreferredWidth(105);
		allColumns[15].setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			{
				setHorizontalAlignment(SwingConstants.CENTER);
			}
		});
		allColumns[16].setCellRenderer(new DetailButtonRenderer());
		showStudentColumnsOnly();
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		paymentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
		paymentTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

		paymentTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

		rowSorter = new TableRowSorter<>(tableModel);
		// Clicking a month header selects it for Clean; don't let that click sort
		// the payment column and unexpectedly reorder students.
		for (int column = 3; column <= 14; column++) {
			rowSorter.setSortable(column, false);
		}
		paymentTable.setRowSorter(rowSorter);
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			private void filterStudents() {
				searchQuery = searchField.getText().trim();
				currentPage = 0;
				refreshTableFilter();
			}

			@Override
			public void insertUpdate(DocumentEvent event) {
				filterStudents();
			}

			@Override
			public void removeUpdate(DocumentEvent event) {
				filterStudents();
			}

			@Override
			public void changedUpdate(DocumentEvent event) {
				filterStudents();
			}
		});
		tableModel.addTableModelListener(event -> handleTableChange(event));
		paymentTable.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				int viewColumn = paymentTable.columnAtPoint(event.getPoint());
				int viewRow = paymentTable.rowAtPoint(event.getPoint());
				if (viewRow >= 0 && viewColumn >= 0
						&& paymentTable.convertColumnIndexToModel(viewColumn) == 16) {
					openPaymentDetail(paymentTable.convertRowIndexToModel(viewRow));
				}
			}
		});
		JScrollPane tableScrollPane = new JScrollPane(
				paymentTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);
		paginationPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		JPanel paginationContainer = new JPanel(new BorderLayout());
		paginationContainer.add(pageInfoLabel, BorderLayout.WEST);
		paginationContainer.add(paginationPanel, BorderLayout.CENTER);
		tablePanel.add(paginationContainer, BorderLayout.SOUTH);
		add(tablePanel, BorderLayout.CENTER);

		restoreUiState();
		yearComboBox.addActionListener(e -> {
			saveUiState();
			loadStudents();
		});
		loadStudents();
	}

	private void restoreUiState() {
		Object savedYear = UI_PREFERENCES.getInt(PREF_YEAR, 2568);
		if (savedYear instanceof Integer && yearComboBox.getItemCount() > 0) {
			yearComboBox.setSelectedItem(savedYear);
		}

		String savedMonths = UI_PREFERENCES.get(PREF_MONTHS, "");
		String[] savedAmounts = UI_PREFERENCES.get(PREF_AMOUNTS, "").split(",", -1);
		for (int index = 0; index < MONTH_NAMES.length; index++) {
			displayedMonthColumns[index] = index < savedMonths.length() && savedMonths.charAt(index) == '1';
			if (index < savedAmounts.length && !savedAmounts[index].isBlank()) {
				try {
					monthlyAmounts[index] = new BigDecimal(savedAmounts[index]);
				} catch (NumberFormatException ignored) {
					monthlyAmounts[index] = null;
				}
			}
			if (displayedMonthColumns[index] && monthlyAmounts[index] != null) {
				allColumns[index + 3].setHeaderValue(MONTH_NAMES[index] + " ("
						+ monthlyAmounts[index].stripTrailingZeros().toPlainString() + " Bath)");
			}
		}

		selectedMonth = UI_PREFERENCES.getInt(PREF_SELECTED_MONTH, -1);
		if (selectedMonth < 1 || selectedMonth > 12 || !displayedMonthColumns[selectedMonth - 1]) {
			selectedMonth = -1;
		}
		if (selectedMonth > 0 && monthlyAmounts[selectedMonth - 1] != null) {
			monthlyAmountField.setText(monthlyAmounts[selectedMonth - 1].stripTrailingZeros().toPlainString());
		}

		boolean hasDisplayedMonth = false;
		for (boolean displayed : displayedMonthColumns) {
			hasDisplayedMonth |= displayed;
		}
		if (hasDisplayedMonth) {
			showDisplayedMonths();
		} else {
			showStudentColumnsOnly();
		}
	}

	private void saveUiState() {
		Object selectedYear = yearComboBox.getSelectedItem();
		if (selectedYear instanceof Integer) {
			UI_PREFERENCES.putInt(PREF_YEAR, (Integer) selectedYear);
		}

		StringBuilder months = new StringBuilder(MONTH_NAMES.length);
		StringBuilder amounts = new StringBuilder();
		for (int index = 0; index < MONTH_NAMES.length; index++) {
			months.append(displayedMonthColumns[index] ? '1' : '0');
			if (index > 0) {
				amounts.append(',');
			}
			if (monthlyAmounts[index] != null) {
				amounts.append(monthlyAmounts[index].toPlainString());
			}
		}
		UI_PREFERENCES.put(PREF_MONTHS, months.toString());
		UI_PREFERENCES.put(PREF_AMOUNTS, amounts.toString());
		UI_PREFERENCES.putInt(PREF_SELECTED_MONTH, selectedMonth);
		try {
			UI_PREFERENCES.flush();
		} catch (BackingStoreException ex) {
			System.err.println("Could not save payment screen preferences: " + ex.getMessage());
		}
	}

	private void refreshTableFilter() {
		List<Integer> matchingRows = new ArrayList<>();
		String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT);
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String studentId = String.valueOf(tableModel.getValueAt(row, 1)).toLowerCase(Locale.ROOT);
			String studentName = String.valueOf(tableModel.getValueAt(row, 2)).toLowerCase(Locale.ROOT);
			if (normalizedQuery.isEmpty() || studentId.contains(normalizedQuery)
					|| studentName.contains(normalizedQuery)) {
				matchingRows.add(row);
			}
		}

		int pageCount = Math.max(1, (matchingRows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		currentPage = Math.min(currentPage, pageCount - 1);
		int firstRow = currentPage * PAGE_SIZE;
		int lastRow = Math.min(firstRow + PAGE_SIZE, matchingRows.size());
		HashSet<Integer> visibleRows = new HashSet<>(matchingRows.subList(firstRow, lastRow));
		rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
				return visibleRows.contains(entry.getIdentifier());
			}
		});
		updatePagination(matchingRows.size(), pageCount, firstRow, lastRow);
	}

	private void updatePagination(int matchingCount, int pageCount, int firstRow, int lastRow) {
		paginationPanel.removeAll();
		pageInfoLabel.setText(matchingCount == 0 ? "No students found"
				: "Showing " + (firstRow + 1) + "-" + lastRow + " of " + matchingCount);

		JButton previousButton = new JButton("<");
		previousButton.setMargin(new Insets(2, 8, 2, 8));
		previousButton.setEnabled(currentPage > 0);
		previousButton.addActionListener(event -> {
			currentPage--;
			refreshTableFilter();
		});
		paginationPanel.add(previousButton);

		int firstVisible = Math.max(0, currentPage - 1);
		int lastVisible = Math.min(pageCount - 1, firstVisible + MAX_VISIBLE_PAGE_BUTTONS - 1);
		if (lastVisible == pageCount - 1) {
			firstVisible = Math.max(0, lastVisible - MAX_VISIBLE_PAGE_BUTTONS + 1);
		}
		List<Integer> pageButtons = new ArrayList<>();
		pageButtons.add(0);
		for (int page = firstVisible; page <= lastVisible; page++) {
			if (!pageButtons.contains(page)) {
				pageButtons.add(page);
			}
		}
		if (!pageButtons.contains(pageCount - 1)) {
			pageButtons.add(pageCount - 1);
		}
		pageButtons.sort(Integer::compareTo);

		int previousPage = -1;
		for (int page : pageButtons) {
			if (previousPage >= 0 && page - previousPage > 1) {
				paginationPanel.add(new JLabel("…"));
			}
			JButton pageButton = new JButton(String.valueOf(page + 1));
			pageButton.setMargin(new Insets(2, 9, 2, 9));
			if (page == currentPage) {
				pageButton.setForeground(new Color(230, 91, 22));
				pageButton.setBorder(BorderFactory.createLineBorder(new Color(230, 91, 22), 2));
			}
			pageButton.addActionListener(event -> {
				currentPage = page;
				refreshTableFilter();
			});
			paginationPanel.add(pageButton);
			previousPage = page;
		}

		JButton nextButton = new JButton(">");
		nextButton.setMargin(new Insets(2, 8, 2, 8));
		nextButton.setEnabled(currentPage < pageCount - 1);
		nextButton.addActionListener(event -> {
			currentPage++;
			refreshTableFilter();
		});
		paginationPanel.add(nextButton);
		paginationPanel.revalidate();
		paginationPanel.repaint();
	}

	private void chooseMonthToCollect() {
		Object initialSelection = selectedMonth > 0 ? MONTH_NAMES[selectedMonth - 1] : MONTH_NAMES[0];
		Object selected = JOptionPane.showInputDialog(
				this,
				"Select the month to collect:",
				"Choose month",
				JOptionPane.QUESTION_MESSAGE,
				null,
				MONTH_NAMES,
				initialSelection);

		if (selected == null) {
			return;
		}

		int monthIndex = Arrays.asList(MONTH_NAMES).indexOf(selected);
		if (monthIndex < 0) {
			return;
		}

		BigDecimal enteredAmount;
		try {
			enteredAmount = new BigDecimal(monthlyAmountField.getText().trim());
			if (enteredAmount.signum() <= 0) {
				throw new IllegalArgumentException("Amount must be greater than 0");
			}
		} catch (RuntimeException ex) {
			showError(ex.getMessage() == null ? "Please enter a valid amount" : ex.getMessage());
			return;
		}

		// ถ้าเลือกเดือนที่เพิ่มไว้แล้ว ใช้ยอดเดิมของเดือนนั้นเพื่อให้หัวคอลัมน์ตรงกับยอดที่บันทึก
		if (monthlyAmounts[monthIndex] == null) {
			monthlyAmounts[monthIndex] = enteredAmount;
		} else {
			monthlyAmountField.setText(monthlyAmounts[monthIndex].toPlainString());
		}

		displayedMonthColumns[monthIndex] = true;
		selectedMonth = monthIndex + 1;
		updateSelectedMonthHeader();
		showDisplayedMonths();
		saveUiState();
		loadStudents();
	}

	private void showDisplayedMonths() {
		while (paymentTable.getColumnModel().getColumnCount() > 0) {
			paymentTable.getColumnModel().removeColumn(paymentTable.getColumnModel().getColumn(0));
		}

		paymentTable.getColumnModel().addColumn(allColumns[0]);
		paymentTable.getColumnModel().addColumn(allColumns[1]);
		paymentTable.getColumnModel().addColumn(allColumns[2]);
		for (int index = 0; index < displayedMonthColumns.length; index++) {
			if (displayedMonthColumns[index]) {
				paymentTable.getColumnModel().addColumn(allColumns[index + 3]);
			}
		}
		paymentTable.getColumnModel().addColumn(allColumns[15]);
		paymentTable.getColumnModel().addColumn(allColumns[16]);
		paymentTable.getTableHeader().repaint();
	}

	private void selectCleanMonth(int monthIndex) {
		selectedCleanMonth = monthIndex;
		cleanButton.setText(monthIndex >= 0 ? "Clean " + MONTH_NAMES[monthIndex] : "Clean");
	}

	private void cleanSelectedMonth() {
		if (selectedCleanMonth < 0 || !displayedMonthColumns[selectedCleanMonth]) {
			JOptionPane.showMessageDialog(this, "Please click the month column header you want to remove first.",
					"Select month", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		int monthIndex = selectedCleanMonth;
		int buddhistYear = (Integer) yearComboBox.getSelectedItem();
		int christianYear = buddhistYear - 543;
		String monthName = MONTH_NAMES[monthIndex];
		int confirm = JOptionPane.showConfirmDialog(this,
				"Remove the " + monthName + " column and all students' payment records for " + buddhistYear + "?",
				"Confirm clean month", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		try {
			int deletedCount = paymentService.clearMonth(YearMonth.of(christianYear, monthIndex + 1));
			displayedMonthColumns[monthIndex] = false;
			monthlyAmounts[monthIndex] = null;
			allColumns[monthIndex + 3].setHeaderValue(monthName);
			if (selectedMonth == monthIndex + 1) {
				selectedMonth = -1;
			}
			selectCleanMonth(-1);
			saveUiState();
			boolean hasDisplayedMonth = false;
			for (boolean displayed : displayedMonthColumns) {
				hasDisplayedMonth |= displayed;
			}
			if (!hasDisplayedMonth) {
				showStudentColumnsOnly();
			} else {
				showDisplayedMonths();
			}
			loadStudents();
			JOptionPane.showMessageDialog(this,
					"Removed " + monthName + " for " + buddhistYear + ". Deleted payment records: " + deletedCount,
					"Clean complete", JOptionPane.INFORMATION_MESSAGE);
		} catch (RuntimeException ex) {
			showError(ex.getMessage());
		}
	}

	private void showStudentColumnsOnly() {
		while (paymentTable.getColumnModel().getColumnCount() > 0) {
			paymentTable.getColumnModel().removeColumn(paymentTable.getColumnModel().getColumn(0));
		}

		paymentTable.getColumnModel().addColumn(allColumns[0]);
		paymentTable.getColumnModel().addColumn(allColumns[1]);
		paymentTable.getColumnModel().addColumn(allColumns[2]);
		paymentTable.getColumnModel().addColumn(allColumns[15]);
		paymentTable.getColumnModel().addColumn(allColumns[16]);
		paymentTable.getTableHeader().repaint();
	}

	private void updateSelectedMonthHeader() {
		if (selectedMonth < 1 || selectedMonth > 12 || allColumns == null) {
			return;
		}

		BigDecimal amount = monthlyAmounts[selectedMonth - 1];
		if (amount == null) {
			return;
		}
		allColumns[selectedMonth + 2].setHeaderValue(
				MONTH_NAMES[selectedMonth - 1] + " (" + amount.stripTrailingZeros().toPlainString() + " Bath)");
		paymentTable.getTableHeader().repaint();
	}

	private void deleteSelectedStudent() {
	    int selectedViewRow = paymentTable.getSelectedRow();

	    if (selectedViewRow < 0) {
	        JOptionPane.showMessageDialog(
	                this,
	                "Please Click Column Student-ID",
	                "Notify",
	                JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    int selectedRow =
	            paymentTable.convertRowIndexToModel(selectedViewRow);

	    // 
	    String studentId = (String) tableModel.getValueAt(selectedRow, 1);

	    int confirm = JOptionPane.showConfirmDialog(
	            this,
	            "Do you want Delete " + studentId + " Yes Or NO",
	            "Confirm",
	            JOptionPane.YES_NO_OPTION,
	            JOptionPane.WARNING_MESSAGE);

	    if (confirm != JOptionPane.YES_OPTION) {
	        return;
	    }

	    try {
	        repository.deleteStudent(studentId);
	        loadStudents();
	    } catch (RuntimeException ex) {
	        showError(ex.getMessage());
	    }
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
		if (loadingTable || event.getType() != TableModelEvent.UPDATE
				|| event.getColumn() < 3 || event.getColumn() > 14) {
			return;
		}

		int row = event.getFirstRow();
		int column = event.getColumn();
		boolean checked = Boolean.TRUE.equals(tableModel.getValueAt(row, column));

		if (!checked) {
			try {
				String studentId = (String) tableModel.getValueAt(row, 1);
				int buddhistYear = (Integer) yearComboBox.getSelectedItem();
				int christianYear = buddhistYear - 543;
				int month = column - 2;
				paymentService.cancelReceived(studentId, YearMonth.of(christianYear, month));
				loadStudents();
			} catch (RuntimeException ex) {
				loadStudents();
				showError(ex.getMessage());
			}
			return;
		}

		try {
			int month = column - 2;
			BigDecimal amount = monthlyAmounts[month - 1];
			if (amount == null || amount.signum() <= 0) {
				throw new IllegalArgumentException("Please select the month and enter a valid amount first");
			}

			String studentId = (String) tableModel.getValueAt(row, 1);
			int buddhistYear = (Integer) yearComboBox.getSelectedItem();
			int christianYear = buddhistYear - 543;

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
				Object[] row = new Object[17];
				row[0] = rowNumber++;
				row[1] = student.getStudentId();
				row[2] = student.getName();

				for (int column = 3; column <= 14; column++) {
					row[column] = false;
				}

				for (MonthlyPayment payment : payments) {
					if (payment.getStudentId().equals(student.getStudentId())) {
						int monthColumn = payment.getPeriod().getMonthValue() + 2;
						row[monthColumn] = true;
					}
				}

				int requiredMonths = 0;
				int paidMonths = 0;
				for (int monthIndex = 0; monthIndex < displayedMonthColumns.length; monthIndex++) {
					if (displayedMonthColumns[monthIndex]) {
						requiredMonths++;
						if (Boolean.TRUE.equals(row[monthIndex + 3])) {
							paidMonths++;
						}
					}
				}
				row[15] = requiredMonths == 0 ? "—"
						: paidMonths == requiredMonths ? "Complete" : "-" + (requiredMonths - paidMonths);
				row[16] = requiredMonths == 0 || paidMonths < requiredMonths ? "Detail" : "None";

				tableModel.addRow(row);
			}
		} finally {
			loadingTable = false;
			refreshTableFilter();
		}
	}

	public void reloadStudents() {
		currentPage = 0;
		loadStudents();
	}

	public List<String> buildPaymentReportCsv() {
		List<Integer> selectedMonths = new ArrayList<>();
		for (int month = 0; month < displayedMonthColumns.length; month++) {
			if (displayedMonthColumns[month]) {
				selectedMonths.add(month);
			}
		}
		if (selectedMonths.isEmpty()) {
			throw new IllegalStateException("Select at least one collection month in List and Check first.");
		}

		int buddhistYear = (Integer) yearComboBox.getSelectedItem();
		int christianYear = buddhistYear - 543;
		Map<String, boolean[]> paidByStudent = new HashMap<>();
		for (MonthlyPayment payment : repository.findPayments(christianYear)) {
			paidByStudent.computeIfAbsent(payment.getStudentId(), ignored -> new boolean[12])
					[payment.getPeriod().getMonthValue() - 1] = true;
		}

		List<String> rows = new ArrayList<>();
		List<String> header = new ArrayList<>();
		header.add("Academic Year");
		header.add("Student-ID");
		header.add("Name");
		for (int month : selectedMonths) {
			BigDecimal amount = monthlyAmounts[month];
			header.add(amount == null ? MONTH_NAMES[month]
					: MONTH_NAMES[month] + " (" + amount.stripTrailingZeros().toPlainString() + " Bath)");
		}
		header.add("Months Paid");
		header.add("Months Required");
		header.add("Status");
		header.add("Missing Months");
		header.add("Detail");
		rows.add("\uFEFF" + csvRow(header));

		for (Student student : repository.findAllStudents()) {
			boolean[] studentPayments = paidByStudent.getOrDefault(student.getStudentId(), new boolean[12]);
			int paidCount = 0;
			List<String> missingMonths = new ArrayList<>();
			List<String> row = new ArrayList<>();
			row.add(String.valueOf(buddhistYear));
			row.add(student.getStudentId());
			row.add(student.getName());
			for (int month : selectedMonths) {
				if (studentPayments[month]) {
					row.add("Paid");
					paidCount++;
				} else {
					row.add("Missing");
					missingMonths.add(MONTH_NAMES[month]);
				}
			}
			row.add(String.valueOf(paidCount));
			row.add(String.valueOf(selectedMonths.size()));
			row.add(paidCount == selectedMonths.size() ? "Complete" : "Incomplete");
			row.add(String.join("; ", missingMonths));
			row.add(repository.findPaymentDetail(student.getStudentId(), christianYear));
			rows.add(csvRow(row));
		}
		return rows;
	}

	private String csvRow(List<String> values) {
		List<String> escaped = new ArrayList<>(values.size());
		for (String value : values) {
			String singleLine = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
			escaped.add("\"" + singleLine.replace("\"", "\"\"") + "\"");
		}
		return String.join(",", escaped);
	}

	private void openPaymentDetail(int modelRow) {
		String studentId = (String) tableModel.getValueAt(modelRow, 1);
		int requiredMonths = 0;
		int paidMonths = 0;
		for (int monthIndex = 0; monthIndex < displayedMonthColumns.length; monthIndex++) {
			if (displayedMonthColumns[monthIndex]) {
				requiredMonths++;
				if (Boolean.TRUE.equals(tableModel.getValueAt(modelRow, monthIndex + 3))) {
					paidMonths++;
				}
			}
		}

		int buddhistYear = (Integer) yearComboBox.getSelectedItem();
		int christianYear = buddhistYear - 543;
		String savedDetail = repository.findPaymentDetail(studentId, christianYear);

		if (requiredMonths == 0) {
			JOptionPane.showMessageDialog(this, "Select a month first to check payment status.",
					"Payment detail", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if (paidMonths == requiredMonths) {
			String message = savedDetail.isBlank() ? "Complete — no missing payment detail."
					: "Complete. Saved detail: " + savedDetail;
			JOptionPane.showMessageDialog(this, message, "Payment detail", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JTextArea detailArea = new JTextArea(savedDetail, 6, 30);
		detailArea.setLineWrap(true);
		detailArea.setWrapStyleWord(true);
		JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
		detailPanel.add(new JLabel("Missing " + (requiredMonths - paidMonths)
				+ " month(s). Enter or review the reason:"), BorderLayout.NORTH);
		detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

		int result = JOptionPane.showConfirmDialog(this, detailPanel,
				"Detail — " + studentId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			try {
				repository.savePaymentDetail(studentId, christianYear, detailArea.getText().trim());
				loadStudents();
			} catch (RuntimeException ex) {
				showError(ex.getMessage());
			}
		}
	}

	private void setColumnWidths() {
		paymentTable.getColumnModel().getColumn(0).setPreferredWidth(45);
		paymentTable.getColumnModel().getColumn(1).setPreferredWidth(110);
		paymentTable.getColumnModel().getColumn(2).setPreferredWidth(150);

		for (int column = 3; column < 15; column++) {
			// เผื่อพื้นที่ให้หัวคอลัมน์ เช่น "Jan (100 Bath)" แสดงได้ครบ
			paymentTable.getColumnModel().getColumn(column).setPreferredWidth(145);
		}
	}

	private static class DetailButtonRenderer extends JButton implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		private DetailButtonRenderer() {
			setOpaque(true);
			setFocusPainted(false);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			setText(value == null ? "Detail" : value.toString());
			return this;
		}
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message == null ? "Unknown error" : message, "Operation failed",
				JOptionPane.ERROR_MESSAGE);
	}
}
