package project_oop.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
		setLayout(new BorderLayout(10, 10));

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(new JLabel("Expense recording activities"), BorderLayout.WEST);

		yearComboBox = new JComboBox<Integer>(new Integer[] { 2568, 2569, 2570 });
		yearComboBox.setSelectedItem(2569);

		JPanel yearPanel = new JPanel();
		yearPanel.add(new JLabel("Year :"));
		yearPanel.add(yearComboBox);
		topPanel.add(yearPanel, BorderLayout.EAST);

		add(topPanel, BorderLayout.NORTH);

		// form Activity

		JPanel formPanel = new JPanel(new BorderLayout(8, 8));
		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 8, 8));

		activityNameField = new JTextField();
		amountField = new JTextField();
		dateField = new JTextField(LocalDate.now().format(dateFormatter));

		categoryComboBox = new JComboBox<>(
				new String[] { "Activity equipment", "Food and drinks", "Travel expenses", "other" });

		detailArea = new JTextArea(5, 20);
		detailArea.setLineWrap(true);
		detailArea.setWrapStyleWord(true);

		fieldPanel.add(new JLabel("Activity Name:"));
		fieldPanel.add(activityNameField);

		fieldPanel.add(new JLabel("How much will this activity cost:"));
		fieldPanel.add(amountField);

		fieldPanel.add(new JLabel("Date (day/month/year):"));
		fieldPanel.add(dateField);

		fieldPanel.add(new JLabel("Expense category:"));
		fieldPanel.add(categoryComboBox);

		fieldPanel.add(new JLabel("Detail:"));
		fieldPanel.add(detailArea);

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> saveExpense());

		formPanel.add(fieldPanel, BorderLayout.CENTER);
		formPanel.add(saveButton, BorderLayout.SOUTH);

		historyModel = new DefaultListModel<>();
		JList<String> historyList = new JList<>(historyModel);

		JPanel historyPanel = new JPanel(new BorderLayout(8, 8));
		historyPanel.add(new JLabel("History:"), BorderLayout.NORTH);
		historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);

		JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 10));
		contentPanel.add(formPanel);
		contentPanel.add(historyPanel);

		add(contentPanel, BorderLayout.CENTER);
		yearComboBox.addActionListener(e -> loadingHistory());
		loadingHistory();
	}

	private void saveExpense() {
		try {

			String activityname = activityNameField.getText().trim();
			String category = (String) categoryComboBox.getSelectedItem();
			String detail = detailArea.getText().trim();

			BigDecimal amount = new BigDecimal(amountField.getText().trim());

			if (amount.signum() < 0) {
				throw new IllegalArgumentException("Amount must more than 0");
			}

			LocalDate expenseDate = LocalDate.parse(dateField.getText().trim(), dateFormatter);

			MissionExpense expense = new MissionExpense(UUID.randomUUID().toString(), activityname, category, detail,
					amount, expenseDate);
			
		 missionService.recordExpense(expense);
		 
		 JOptionPane.showMessageDialog(this,"Save !!","Success!!",JOptionPane.INFORMATION_MESSAGE);
		 clearForm();
		 loadingHistory();
		 
		 
		} catch (NumberFormatException ex) {
			showERROR("ERROR");
		} catch (RuntimeException e) {
			showERROR(e.getMessage());
		}
	}
	
	private void loadingHistory()
	{
		 historyModel.clear();

	        int buddhistYear = (Integer) yearComboBox.getSelectedItem();
	        int christianYear = buddhistYear - 543;

	        List<MissionExpense> expenses =
	                repository.findMissions(christianYear);

	        for (MissionExpense expense : expenses) {
	            String item = String.format(
	                    "%s | %s | %s | %s Bath",
	                    expense.getExpenseDate().format(dateFormatter),
	                    expense.getActivityName(),
	                    expense.getCategory(),
	                    expense.getAmount().toPlainString());
	            historyModel.addElement(item);
	        }
	}
	
	private void clearForm()
	{
		 activityNameField.setText("");
	        amountField.setText("");
	        dateField.setText(LocalDate.now().format(dateFormatter));
	        detailArea.setText("");
	        categoryComboBox.setSelectedIndex(0);
	}
	
	private void showERROR(String message) {
		
		 JOptionPane.showMessageDialog(
	                this,
	                message,
	                "Not Success is Wrong",
	                JOptionPane.ERROR_MESSAGE);
	}

}
