package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import project_oop.FundRepository;
import project_oop.MissionExpense;
import project_oop.MissionService;

public class MissionPanel extends JPanel {

	private final FundRepository repository;
	private final MissionService missionService;

	private final JComboBox<Integer> yearComboBox;
	private final JTextField activityNameField;
	private final JTextField amountField;
	private final JTextField dateField;
	private final JComboBox<String> categoryComboBox;
	private final JTextArea detailArea;

	private final DefaultListModel<String> historyModel;
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");

	public MissionPanel(FundRepository repository) {

		this.repository = Objects.requireNonNull(repository, "Repository must not be null");

		this.missionService = new MissionService(repository);
		setLayout(new BorderLayout(16, 16));
		setBackground(new Color(244, 247, 244));
		setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

		// หัวข้อและคำอธิบายทางซ้าย
		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
		titlePanel.setOpaque(false);

		JLabel titleLabel = new JLabel("Record of withdrawal");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 25f));

		JLabel subtitleLabel = new JLabel("Record activity expenses and review past transactions.");
		subtitleLabel.setForeground(new Color(105, 120, 111));

		titlePanel.add(titleLabel);
		titlePanel.add(Box.createVerticalStrut(5));
		titlePanel.add(subtitleLabel);

		// ช่องเลือกปีทางขวา
		yearComboBox = new JComboBox<>(new Integer[] { 2568, 2569, 2570 });
		yearComboBox.setSelectedItem(2569);

		JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		yearPanel.setOpaque(false);
		yearPanel.add(new JLabel("Year"));
		yearPanel.add(yearComboBox);

		// รวมเป็นหัวหน้าเดียว
		JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
		headerPanel.setOpaque(false);
		headerPanel.add(titlePanel, BorderLayout.WEST);
		headerPanel.add(yearPanel, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);


		// ฟอร์มบันทึกค่าใช้จ่าย
		JPanel formPanel = new JPanel(new BorderLayout(12, 12));
		formPanel.setBackground(Color.WHITE);
		formPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(220, 229, 223)),
				BorderFactory.createEmptyBorder(18, 18, 18, 18)));

		JPanel fieldPanel = new JPanel(new GridBagLayout());
		fieldPanel.setBackground(Color.WHITE);

		activityNameField = new JTextField();
		amountField = new JTextField();
		dateField = new JTextField(LocalDate.now().format(dateFormatter));

		categoryComboBox = new JComboBox<>(
				new String[] { "Activity equipment", "Food and drinks", "Travel expenses", "other" });

		detailArea = new JTextArea(4, 20);
		detailArea.setLineWrap(true);
		detailArea.setWrapStyleWord(true);

		// จำนวนเงินและชื่อกิจกรรมเต็มความกว้างของฟอร์ม
		addFormField(fieldPanel, createField("Activity Name:", activityNameField),
				0, 0, 2, 1, 8);
		addFormField(fieldPanel, createField("How much will this activity cost:", amountField),
				0, 1, 2, 1, 8);

		// วันที่และหมวดหมู่อยู่แถวเดียวกัน ความสูงพอดีกับช่องกรอก
		addFormField(fieldPanel, createField("Date (day/month/year):", dateField),
				0, 2, 1, 0.5, 8);
		addFormField(fieldPanel, createField("Expense category:", categoryComboBox),
				1, 2, 1, 0.5, 8);

		JPanel detailPanel = new JPanel(new BorderLayout(0, 6));
		detailPanel.setOpaque(false);
		JLabel detailLabel = new JLabel("Detail:");
		detailLabel.setHorizontalAlignment(JLabel.LEFT);
		detailPanel.add(detailLabel, BorderLayout.NORTH);

		JScrollPane detailScrollPane = new JScrollPane(detailArea);
		detailScrollPane.setPreferredSize(new Dimension(400, 110));
		detailPanel.add(detailScrollPane, BorderLayout.CENTER);
		GridBagConstraints detailConstraints = new GridBagConstraints();
		detailConstraints.gridx = 0;
		detailConstraints.gridy = 3;
		detailConstraints.gridwidth = 2;
		detailConstraints.weightx = 1;
		detailConstraints.weighty = 1;
		detailConstraints.fill = GridBagConstraints.BOTH;
		detailConstraints.insets = new Insets(0, 0, 8, 0);
		fieldPanel.add(detailPanel, detailConstraints);

		JButton saveButton = new JButton("Save");
		saveButton.setBackground(new Color(23, 103, 71));
		saveButton.setForeground(Color.WHITE);
		saveButton.setFocusPainted(false);
		saveButton.addActionListener(e -> saveExpense());

		formPanel.add(fieldPanel, BorderLayout.CENTER);
		formPanel.add(saveButton, BorderLayout.SOUTH);

		historyModel = new DefaultListModel<>();
		JList<String> historyList = new JList<>(historyModel);

		JPanel historyPanel = new JPanel(new BorderLayout(8, 8));
		historyPanel.setBackground(Color.WHITE);
		historyPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(220, 229, 223)),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		historyPanel.add(new JLabel("History:"), BorderLayout.NORTH);
		historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);

		JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 10));
		contentPanel.setOpaque(false);
		contentPanel.add(formPanel);
		contentPanel.add(historyPanel);

		add(contentPanel, BorderLayout.CENTER);
		yearComboBox.addActionListener(e -> loadingHistory());
		loadingHistory();
	}

	private JPanel createField(String labelText, JComponent input) {
		JPanel fieldPanel = new JPanel(new BorderLayout(0, 6));
		fieldPanel.setOpaque(false);

		JLabel label = new JLabel(labelText);
		label.setHorizontalAlignment(JLabel.LEFT);
		fieldPanel.add(label, BorderLayout.NORTH);

		input.setPreferredSize(new Dimension(200, 38));
		fieldPanel.add(input, BorderLayout.CENTER);

		return fieldPanel;
	}

	private void addFormField(JPanel panel, JComponent field, int column, int row,
			int columnSpan, double weightX, int bottomGap) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.gridwidth = columnSpan;
		constraints.weightx = weightX;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(0, 0, bottomGap, 12);
		panel.add(field, constraints);
	}

	private void saveExpense() {
		try {

			String activityname = activityNameField.getText().trim();
			String category = (String) categoryComboBox.getSelectedItem();
			String detail = detailArea.getText().trim();

			BigDecimal amount = new BigDecimal(amountField.getText().trim());

			if (amount.signum() <= 0) {
				throw new IllegalArgumentException("Amount must more than 0");
			}

			LocalDate expenseDate = LocalDate.parse(dateField.getText().trim(), dateFormatter);

			MissionExpense expense = new MissionExpense(UUID.randomUUID().toString(), activityname, category, detail,
					amount, expenseDate);

			missionService.recordExpense(expense);

			JOptionPane.showMessageDialog(this, "Save !!", "Success!!", JOptionPane.INFORMATION_MESSAGE);
			clearForm();
			loadingHistory();

		} catch (NumberFormatException ex) {
			showERROR("ERROR");
		} catch (RuntimeException e) {
			showERROR(e.getMessage());
		}
	}

	private void loadingHistory() {
		historyModel.clear();

		int buddhistYear = (Integer) yearComboBox.getSelectedItem();
		int christianYear = buddhistYear - 543;

		List<MissionExpense> expenses = repository.findMissions(christianYear);

		for (MissionExpense expense : expenses) {
			String item = String.format("%s | %s | %s | %s Bath", expense.getExpenseDate().format(dateFormatter),
					expense.getActivityName(), expense.getCategory(), expense.getAmount().toPlainString());
			historyModel.addElement(item);
		}
	}

	private void clearForm() {
		activityNameField.setText("");
		amountField.setText("");
		dateField.setText(LocalDate.now().format(dateFormatter));
		detailArea.setText("");
		categoryComboBox.setSelectedIndex(0);
	}

	private void showERROR(String message) {

		JOptionPane.showMessageDialog(this, message, "Not Success is Wrong", JOptionPane.ERROR_MESSAGE);
	}

}
