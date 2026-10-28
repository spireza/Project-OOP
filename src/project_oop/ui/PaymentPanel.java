package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
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
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import project_oop.FundRepository;
import project_oop.MonthlyPayment;
import project_oop.PaymentService;
import project_oop.Student;

public class PaymentPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Color PAGE_BACKGROUND = new Color(245, 247, 250);
	private static final Color CARD_BORDER = new Color(220, 227, 234);
	private static final Color TEXT_COLOR = new Color(45, 55, 65);
	private static final Color MUTED_COLOR = new Color(115, 125, 135);
	private static final int PAGE_SIZE = 20;
	private static final int MAX_VISIBLE_PAGE_BUTTONS = 3;
	private static final Preferences UI_PREFERENCES = Preferences.userNodeForPackage(PaymentPanel.class);
	private static final String PREF_YEAR = "payment.selectedYear";
	private static final String PREF_YEARS = "payment.availableYears";
	private static final String PREF_MONTHS = "payment.displayedMonths";
	private static final String PREF_AMOUNTS = "payment.monthlyAmounts";
	private static final String PREF_SELECTED_MONTH = "payment.selectedMonth";

	// ปีเริ่มต้น (พ.ศ.) และช่วงปีที่อนุญาตให้เพิ่มได้
	private static final Integer[] DEFAULT_YEARS = { 2568, 2569, 2570 };
	private static final int MIN_YEAR = 2500;
	private static final int MAX_YEAR = 2700;

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
	private int activeYear;
	private int selectedMonth = -1;
	private int selectedCleanMonth = -1;
	private final JButton cleanButton;
	private final JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
	private final JLabel pageInfoLabel = new JLabel(" ");
	private final TableRowSorter<DefaultTableModel> rowSorter;
	private final List<Runnable> yearsChangedListeners = new ArrayList<>();
	private String searchQuery = "";
	private int currentPage;

	private boolean loadingTable;

	public PaymentPanel(FundRepository repository) {

		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.paymentService = new PaymentService(repository);

		setLayout(new BorderLayout(12, 12));
		setBackground(PAGE_BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

		Insets compactButtonMargin = new Insets(2, 4, 2, 4);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(java.awt.Color.WHITE);
		topPanel.setOpaque(true);
		topPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CARD_BORDER), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		controlsPanel.setOpaque(false);
		controlsPanel.add(new JLabel("Year"));

		// โหลดรายการปีที่เคยเพิ่มไว้ (รวมปีเริ่มต้น) จาก Preferences
		yearComboBox = new JComboBox<>(loadSavedYears().toArray(new Integer[0]));
		controlsPanel.add(yearComboBox);

		JButton addYearButton = new JButton("Add Year");
		addYearButton.setMargin(compactButtonMargin);
		addYearButton.setFont(addYearButton.getFont().deriveFont(java.awt.Font.PLAIN, 12f));
		addYearButton.setToolTipText("Add a new year (B.E.) to the year list");
		addYearButton.addActionListener(e -> addYear());
		controlsPanel.add(addYearButton);

		controlsPanel.add(new JLabel("Amount/month:"));
		monthlyAmountField = new JTextField("100", 6);
		controlsPanel.add(monthlyAmountField);

		JButton addStudentButton = new JButton("Add Student");
		JButton deleteStudentButton = new JButton("Delete Student");
		JButton checkButton = new JButton("Check");
		cleanButton = new JButton("Clean");
		cleanButton.setToolTipText("Click a month header, then click Clean to remove that month's data");
		deleteStudentButton.setMargin(compactButtonMargin);
		addStudentButton.setMargin(compactButtonMargin);
		checkButton.setMargin(compactButtonMargin);
		cleanButton.setMargin(compactButtonMargin);
		java.awt.Font controlFont = cleanButton.getFont().deriveFont(java.awt.Font.PLAIN, 12f);
		deleteStudentButton.setFont(controlFont);
		addStudentButton.setFont(controlFont);
		checkButton.setFont(controlFont);
		cleanButton.setFont(controlFont);
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
		searchPanel.setOpaque(false);
		searchPanel.add(new JLabel("Search:"));
		JTextField searchField = new JTextField(9);
		searchField.setPreferredSize(new java.awt.Dimension(105, 26));
		searchPanel.add(searchField);
		topPanel.add(searchPanel, BorderLayout.EAST);
		JPanel heading = new JPanel(new BorderLayout(12, 0));
		heading.setOpaque(false);
		JPanel titlePanel = new JPanel(new BorderLayout(0, 4));
		titlePanel.setOpaque(false);
		JLabel titleLabel = new JLabel("List and Check");
		titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 25f));
		titleLabel.setForeground(TEXT_COLOR);
		
		
		titlePanel.add(titleLabel, BorderLayout.NORTH);
		

		JPanel pageHeader = new JPanel(new BorderLayout(0, 12));
		pageHeader.setOpaque(false);
		pageHeader.add(titlePanel, BorderLayout.NORTH);
		pageHeader.add(topPanel, BorderLayout.SOUTH);
		add(pageHeader, BorderLayout.NORTH);

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
		paymentTable.setRowHeight(20);
		paymentTable.setForeground(TEXT_COLOR);
		paymentTable.setBackground(java.awt.Color.WHITE);
		paymentTable.setSelectionBackground(new Color(222, 236, 248));
		paymentTable.setSelectionForeground(TEXT_COLOR);
		paymentTable.setGridColor(CARD_BORDER);
		paymentTable.setFillsViewportHeight(true);
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
		tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		tableScrollPane.getViewport().setBackground(java.awt.Color.WHITE);
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);
		paginationPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		paginationPanel.setOpaque(false);
		pageInfoLabel.setForeground(MUTED_COLOR);
		JPanel paginationContainer = new JPanel(new BorderLayout());
		paginationContainer.setOpaque(false);
		paginationContainer.setBorder(BorderFactory.createEmptyBorder(5, 2, 0, 2));
		paginationContainer.add(pageInfoLabel, BorderLayout.WEST);
		paginationContainer.add(paginationPanel, BorderLayout.CENTER);
		tablePanel.add(paginationContainer, BorderLayout.SOUTH);
		add(tablePanel, BorderLayout.CENTER);

		restoreUiState();
		yearComboBox.addActionListener(e -> {
			saveUiState(); // บันทึกการตั้งค่าของปีเดิมก่อนสลับ
			switchToSelectedYear();
		});
		loadStudents();
	}

	// ---------------------------------------------------------------------
	// Year list (Add Year)
	// ---------------------------------------------------------------------

	/** อ่านรายการปีที่บันทึกไว้ รวมกับปีเริ่มต้น เรียงจากน้อยไปมาก */
	public static List<Integer> loadSavedYears() {
		TreeSet<Integer> years = new TreeSet<>(Arrays.asList(DEFAULT_YEARS));
		for (String part : UI_PREFERENCES.get(PREF_YEARS, "").split(",")) {
			try {
				int year = Integer.parseInt(part.trim());
				if (year >= MIN_YEAR && year <= MAX_YEAR) {
					years.add(year);
				}
			} catch (NumberFormatException ignored) {
				// ข้ามค่าที่อ่านไม่ได้
			}
		}
		return new ArrayList<>(years);
	}

	private void addYear() {
		String input = JOptionPane.showInputDialog(this,
				"Input year (B.E.), e.g. 2571", "Add Year", JOptionPane.QUESTION_MESSAGE);
		if (input == null) {
			return;
		}

		int year;
		try {
			year = Integer.parseInt(input.trim());
		} catch (NumberFormatException ex) {
			showError("Please enter the year as a number, e.g. 2571");
			return;
		}
		if (year < MIN_YEAR || year > MAX_YEAR) {
			showError("Year must be between " + MIN_YEAR + " and " + MAX_YEAR);
			return;
		}

		// ถ้ามีปีนี้อยู่แล้ว ให้แค่เลือกปีนั้น
		for (int index = 0; index < yearComboBox.getItemCount(); index++) {
			if (yearComboBox.getItemAt(index) == year) {
				yearComboBox.setSelectedIndex(index);
				return;
			}
		}

		// แทรกให้เรียงลำดับปีจากน้อยไปมาก
		int insertAt = 0;
		while (insertAt < yearComboBox.getItemCount() && yearComboBox.getItemAt(insertAt) < year) {
			insertAt++;
		}
		yearComboBox.insertItemAt(year, insertAt);
		// การเลือกปีใหม่จะเรียก listener ของ combo box -> saveUiState() + loadStudents()
		yearComboBox.setSelectedIndex(insertAt);
		saveUiState();
		notifyYearsChanged();
	}

	/** รายการปีทั้งหมดที่เลือกได้ (พ.ศ.) ให้หน้า Overview / Export เรียกใช้ */
	public List<Integer> getAvailableYears() {
		List<Integer> years = new ArrayList<>();
		for (int index = 0; index < yearComboBox.getItemCount(); index++) {
			years.add(yearComboBox.getItemAt(index));
		}
		return years;
	}

	/** หน้าอื่นลงทะเบียนไว้ เพื่อรีเฟรชตัวเลือกปีเมื่อมีการเพิ่มปีใหม่ */
	public void addYearsChangedListener(Runnable listener) {
		if (listener != null) {
			yearsChangedListeners.add(listener);
		}
	}

	private void notifyYearsChanged() {
		for (Runnable listener : yearsChangedListeners) {
			listener.run();
		}
	}

	// ---------------------------------------------------------------------

	// ---------------------------------------------------------------------
	// ตั้งค่าเดือนที่เก็บเงินและยอดต่อเดือน: แยกเก็บตามปี
	// ---------------------------------------------------------------------

	private static String yearKey(String base, int buddhistYear) {
		return base + "." + buddhistYear;
	}

	private static final class YearState {
		final boolean[] displayed = new boolean[12];
		final BigDecimal[] amounts = new BigDecimal[12];
		int selectedMonth = -1;
	}

	private static boolean hasDisplayedMonth(boolean[] displayed) {
		for (boolean value : displayed) {
			if (value) {
				return true;
			}
		}
		return false;
	}

	/** อ่านการตั้งค่าของปีนั้น ถ้าปีนั้นยังไม่เคยตั้งค่า จะดูจากรายการรับเงินที่มีอยู่จริง */
	private YearState readYearState(int buddhistYear) {
		YearState state = new YearState();
		String savedMonths = UI_PREFERENCES.get(yearKey(PREF_MONTHS, buddhistYear), null);

		if (savedMonths != null) {
			String[] savedAmounts = UI_PREFERENCES.get(yearKey(PREF_AMOUNTS, buddhistYear), "").split(",", -1);
			for (int index = 0; index < MONTH_NAMES.length; index++) {
				state.displayed[index] = index < savedMonths.length() && savedMonths.charAt(index) == '1';
				if (index < savedAmounts.length && !savedAmounts[index].isBlank()) {
					try {
						state.amounts[index] = new BigDecimal(savedAmounts[index]);
					} catch (NumberFormatException ignored) {
						state.amounts[index] = null;
					}
				}
			}
			state.selectedMonth = UI_PREFERENCES.getInt(yearKey(PREF_SELECTED_MONTH, buddhistYear), -1);
		} else {
			for (MonthlyPayment payment : repository.findPayments(buddhistYear - 543)) {
				int index = payment.getPeriod().getMonthValue() - 1;
				if (!state.displayed[index]) {
					state.displayed[index] = true;
					state.amounts[index] = payment.getAmount();
				}
			}
		}

		if (state.selectedMonth < 1 || state.selectedMonth > 12 || !state.displayed[state.selectedMonth - 1]) {
			state.selectedMonth = -1;
		}
		return state;
	}

	private YearState snapshotState() {
		YearState state = new YearState();
		System.arraycopy(displayedMonthColumns, 0, state.displayed, 0, 12);
		System.arraycopy(monthlyAmounts, 0, state.amounts, 0, 12);
		state.selectedMonth = selectedMonth;
		return state;
	}

	/** นำการตั้งค่าของปีไปใช้กับตารางที่แสดงอยู่ */
	private void applyYearState(YearState state) {
		System.arraycopy(state.displayed, 0, displayedMonthColumns, 0, 12);
		System.arraycopy(state.amounts, 0, monthlyAmounts, 0, 12);
		selectedMonth = state.selectedMonth;

		for (int index = 0; index < MONTH_NAMES.length; index++) {
			if (displayedMonthColumns[index] && monthlyAmounts[index] != null) {
				allColumns[index + 3].setHeaderValue(MONTH_NAMES[index] + " ("
						+ monthlyAmounts[index].stripTrailingZeros().toPlainString() + " Bath)");
			} else {
				allColumns[index + 3].setHeaderValue(MONTH_NAMES[index]);
			}
		}

		if (selectedMonth > 0 && monthlyAmounts[selectedMonth - 1] != null) {
			monthlyAmountField.setText(monthlyAmounts[selectedMonth - 1].stripTrailingZeros().toPlainString());
		}

		if (hasDisplayedMonth(displayedMonthColumns)) {
			showDisplayedMonths();
		} else {
			showStudentColumnsOnly();
		}
	}

	private void saveYearState(int buddhistYear) {
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
		UI_PREFERENCES.put(yearKey(PREF_MONTHS, buddhistYear), months.toString());
		UI_PREFERENCES.put(yearKey(PREF_AMOUNTS, buddhistYear), amounts.toString());
		UI_PREFERENCES.putInt(yearKey(PREF_SELECTED_MONTH, buddhistYear), selectedMonth);
	}

	private void restoreUiState() {
		int savedYear = UI_PREFERENCES.getInt(PREF_YEAR, 2568);
		if (yearComboBox.getItemCount() > 0) {
			yearComboBox.setSelectedItem(savedYear);
		}

		Object selected = yearComboBox.getSelectedItem();
		activeYear = selected instanceof Integer ? (Integer) selected : savedYear;
		applyYearState(readYearState(activeYear));
	}

	/** เรียกหลังผู้ใช้เปลี่ยนปีใน combo box (ต้อง saveUiState() ของปีเดิมก่อน) */
	private void switchToSelectedYear() {
		Object selected = yearComboBox.getSelectedItem();
		if (!(selected instanceof Integer)) {
			return;
		}

		activeYear = (Integer) selected;
		selectCleanMonth(-1);
		applyYearState(readYearState(activeYear));
		loadStudents();
	}

	private void saveUiState() {
		Object selectedYear = yearComboBox.getSelectedItem();
		if (selectedYear instanceof Integer) {
			UI_PREFERENCES.putInt(PREF_YEAR, (Integer) selectedYear);
		}

		// บันทึกรายการปีทั้งหมดใน combo box
		StringBuilder years = new StringBuilder();
		for (int index = 0; index < yearComboBox.getItemCount(); index++) {
			if (index > 0) {
				years.append(',');
			}
			years.append(yearComboBox.getItemAt(index));
		}
		UI_PREFERENCES.put(PREF_YEARS, years.toString());

		// บันทึกการตั้งค่าของปีที่ใช้อยู่ตอนนี้
		if (activeYear > 0) {
			saveYearState(activeYear);
		}
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
					"Please select a student row first.",
					"Notify",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		int selectedRow = paymentTable.convertRowIndexToModel(selectedViewRow);

		String studentId = (String) tableModel.getValueAt(selectedRow, 1);
		String studentName = String.valueOf(tableModel.getValueAt(selectedRow, 2));

		int confirm = JOptionPane.showConfirmDialog(
				this,
				"Delete " + studentId + " (" + studentName + ")?\n"
						+ "All payment records of this student will also be deleted.",
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
			repository.saveStudent(new Student(name.trim(), studentId.trim()));
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

	public List<String> buildPaymentReportCsv(boolean allYears) {
		if (!allYears) {
			YearState current = snapshotState();
			if (!hasDisplayedMonth(current.displayed)) {
				throw new IllegalStateException("Select at least one collection month in List and Check first.");
			}
			return buildSingleYearReport(activeYear, current);
		}

		// ทุกปี: ใช้เดือนที่เก็บของแต่ละปี (ข้ามปีที่ยังไม่ได้ตั้งค่าเดือน)
		Map<Integer, YearState> states = new TreeMap<>();
		for (int year : getAvailableYears()) {
			YearState state = year == activeYear ? snapshotState() : readYearState(year);
			if (hasDisplayedMonth(state.displayed)) {
				states.put(year, state);
			}
		}
		if (states.isEmpty()) {
			throw new IllegalStateException(
					"No collection months are set for any year. Select at least one month in List and Check first.");
		}
		return buildAllYearsReport(states);
	}

	private Map<String, boolean[]> findPaidMonths(int christianYear) {
		Map<String, boolean[]> paidByStudent = new HashMap<>();
		for (MonthlyPayment payment : repository.findPayments(christianYear)) {
			paidByStudent.computeIfAbsent(payment.getStudentId(), ignored -> new boolean[12])
					[payment.getPeriod().getMonthValue() - 1] = true;
		}
		return paidByStudent;
	}

	private List<String> buildSingleYearReport(int buddhistYear, YearState state) {
		List<Integer> selectedMonths = new ArrayList<>();
		for (int month = 0; month < state.displayed.length; month++) {
			if (state.displayed[month]) {
				selectedMonths.add(month);
			}
		}

		int christianYear = buddhistYear - 543;
		Map<String, boolean[]> paidByStudent = findPaidMonths(christianYear);

		List<String> rows = new ArrayList<>();
		List<String> header = new ArrayList<>();
		header.add("Academic Year");
		header.add("Student-ID");
		header.add("Name");
		for (int month : selectedMonths) {
			BigDecimal amount = state.amounts[month];
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

	/** รายงานหลายปี: คอลัมน์ Jan-Dec คงที่ เดือนที่ปีนั้นไม่ได้เก็บเงินจะเว้นว่าง */
	private List<String> buildAllYearsReport(Map<Integer, YearState> states) {
		List<String> rows = new ArrayList<>();
		List<String> header = new ArrayList<>();
		header.add("Academic Year");
		header.add("Student-ID");
		header.add("Name");
		for (String monthName : MONTH_NAMES) {
			header.add(monthName);
		}
		header.add("Months Paid");
		header.add("Months Required");
		header.add("Status");
		header.add("Missing Months");
		header.add("Detail");
		rows.add("\uFEFF" + csvRow(header));

		List<Student> students = repository.findAllStudents();
		for (Map.Entry<Integer, YearState> entry : states.entrySet()) {
			int buddhistYear = entry.getKey();
			int christianYear = buddhistYear - 543;
			YearState state = entry.getValue();
			Map<String, boolean[]> paidByStudent = findPaidMonths(christianYear);

			int requiredCount = 0;
			for (boolean displayed : state.displayed) {
				if (displayed) {
					requiredCount++;
				}
			}

			for (Student student : students) {
				boolean[] studentPayments = paidByStudent.getOrDefault(student.getStudentId(), new boolean[12]);
				int paidCount = 0;
				List<String> missingMonths = new ArrayList<>();
				List<String> row = new ArrayList<>();
				row.add(String.valueOf(buddhistYear));
				row.add(student.getStudentId());
				row.add(student.getName());
				for (int month = 0; month < MONTH_NAMES.length; month++) {
					if (!state.displayed[month]) {
						row.add("");
					} else if (studentPayments[month]) {
						row.add("Paid");
						paidCount++;
					} else {
						row.add("Missing");
						missingMonths.add(MONTH_NAMES[month]);
					}
				}
				row.add(String.valueOf(paidCount));
				row.add(String.valueOf(requiredCount));
				row.add(paidCount == requiredCount ? "Complete" : "Incomplete");
				row.add(String.join("; ", missingMonths));
				row.add(repository.findPaymentDetail(student.getStudentId(), christianYear));
				rows.add(csvRow(row));
			}
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