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
import java.awt.event.HierarchyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
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

    private static final long serialVersionUID = 1L;

    private final FundRepository repository;
    private final MissionService missionService;

    private final JComboBox<Integer> yearComboBox;
    // รายการปี (พ.ศ.) ใช้ชุดเดียวกับหน้า List and Check
    private List<Integer> availableYears;
    private boolean updatingYears;
    private final JTextField activityNameField;
    private final JTextField amountField;
    private final JTextField dateField;
    private final JComboBox<String> categoryComboBox;
    private final JTextArea detailArea;

    private final DefaultListModel<String> historyModel;
    private final JList<String> historyList;
    private final JButton saveButton;
    private final JButton cancelEditButton;

    private final List<MissionExpense> historyExpenses = new ArrayList<>();

    private String editingMissionId;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public MissionPanel(FundRepository repository) {
        this.repository = Objects.requireNonNull(
                repository, "Repository must not be null");

        this.missionService = new MissionService(repository);

        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(244, 247, 244));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Title and year selector
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Record of withdrawal");
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 25f));

        JLabel subtitleLabel = new JLabel(
                "Record activity expenses and review past transactions.");
        subtitleLabel.setForeground(new Color(105, 120, 111));

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitleLabel);

        availableYears = PaymentPanel.loadSavedYears();
        yearComboBox = new JComboBox<>(
                availableYears.toArray(new Integer[0]));
        yearComboBox.setSelectedItem(2569);

        JPanel yearPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0));
        yearPanel.setOpaque(false);
        yearPanel.add(new JLabel("Year"));
        yearPanel.add(yearComboBox);

        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setOpaque(false);
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(yearPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Expense form
        JPanel formPanel = new JPanel(new BorderLayout(12, 12));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 229, 223)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JPanel fieldPanel = new JPanel(new GridBagLayout());
        fieldPanel.setBackground(Color.WHITE);

        activityNameField = new JTextField();
        amountField = new JTextField();
        dateField = new JTextField(
                LocalDate.now().format(dateFormatter));

        categoryComboBox = new JComboBox<>(new String[] {
            "Activity equipment",
            "Food and drinks",
            "Travel expenses",
            "other"
        });

        detailArea = new JTextArea(4, 20);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);

        addFormField(
                fieldPanel,
                createField("Activity Name:", activityNameField),
                0, 0, 2, 1, 8);

        addFormField(
                fieldPanel,
                createField(
                        "How much will this activity cost:",
                        amountField),
                0, 1, 2, 1, 8);

        addFormField(
                fieldPanel,
                createField("Date (day/month/year):", dateField),
                0, 2, 1, 0.5, 8);

        addFormField(
                fieldPanel,
                createField("Expense category:", categoryComboBox),
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

        saveButton = new JButton("Save");
        saveButton.setBackground(new Color(23, 103, 71));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(event -> saveExpense());

        cancelEditButton = new JButton("Cancel edit");
        cancelEditButton.setVisible(false);
        cancelEditButton.addActionListener(event -> clearForm());

        JPanel formActions = new JPanel(new BorderLayout(8, 0));
        formActions.setOpaque(false);
        formActions.add(saveButton, BorderLayout.CENTER);
        formActions.add(cancelEditButton, BorderLayout.EAST);

        formPanel.add(fieldPanel, BorderLayout.CENTER);
        formPanel.add(formActions, BorderLayout.SOUTH);

        // History list
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);

        JPanel historyPanel = new JPanel(new BorderLayout(8, 8));
        historyPanel.setBackground(Color.WHITE);
        historyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 229, 223)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        historyPanel.add(new JLabel("History:"), BorderLayout.NORTH);
        historyPanel.add(
                new JScrollPane(historyList),
                BorderLayout.CENTER);

        JPanel historyActions =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        historyActions.setOpaque(false);

        JButton viewDetailButton = new JButton("View Detail");
        JButton editButton = new JButton("Edit selected");
        JButton deleteButton = new JButton("Delete selected");

        viewDetailButton.addActionListener(
                event -> viewSelectedExpenseDetail());
        editButton.addActionListener(
                event -> editSelectedExpense());
        deleteButton.addActionListener(
                event -> deleteSelectedExpense());

        historyActions.add(viewDetailButton);
        historyActions.add(editButton);
        historyActions.add(deleteButton);

        historyPanel.add(historyActions, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(formPanel);
        contentPanel.add(historyPanel);

        add(contentPanel, BorderLayout.CENTER);

        yearComboBox.addActionListener(event -> {
            if (!updatingYears) {
                loadingHistory();
            }
        });

        // ทุกครั้งที่สลับมาแท็บนี้ ให้อัปเดตรายการปี เผื่อเพิ่มปีใหม่ที่หน้า List and Check
        addHierarchyListener(event -> {
            if ((event.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing()) {
                refreshYearOptions();
            }
        });

        loadingHistory();
    }

    /** อ่านรายการปีล่าสุด ถ้ามีปีใหม่ให้สร้างตัวเลือกใหม่และคงปีที่เลือกไว้ */
    private void refreshYearOptions() {
        List<Integer> latestYears = PaymentPanel.loadSavedYears();
        if (latestYears.equals(availableYears)) {
            return;
        }

        Object previousSelection = yearComboBox.getSelectedItem();
        updatingYears = true;
        try {
            availableYears = new ArrayList<>(latestYears);
            DefaultComboBoxModel<Integer> model = new DefaultComboBoxModel<>(
                    availableYears.toArray(new Integer[0]));
            yearComboBox.setModel(model);

            if (previousSelection != null
                    && model.getIndexOf(previousSelection) >= 0) {
                model.setSelectedItem(previousSelection);
            } else {
                model.setSelectedItem(2569);
            }
        } finally {
            updatingYears = false;
        }
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

    private void addFormField(
            JPanel panel,
            JComponent field,
            int column,
            int row,
            int columnSpan,
            double weightX,
            int bottomGap) {

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
            String activityName = activityNameField.getText().trim();
            String category =
                    (String) categoryComboBox.getSelectedItem();
            String detail = detailArea.getText().trim();

            BigDecimal amount =
                    new BigDecimal(amountField.getText().trim());

            if (amount.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Amount must be more than 0");
            }

            LocalDate expenseDate =
                    LocalDate.parse(dateField.getText().trim(), dateFormatter);

            boolean editing = editingMissionId != null;

            MissionExpense expense = new MissionExpense(
                    editing
                            ? editingMissionId
                            : UUID.randomUUID().toString(),
                    activityName,
                    category,
                    detail,
                    amount,
                    expenseDate);

            if (editing) {
                missionService.updateExpense(expense);
            } else {
                missionService.recordExpense(expense);
            }

            JOptionPane.showMessageDialog(
                    this,
                    editing ? "Mission updated." : "Mission saved.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            loadingHistory();

        } catch (NumberFormatException ex) {
            showERROR("Please enter a valid amount.");
        } catch (RuntimeException ex) {
            showERROR(ex.getMessage());
        }
    }

    private void loadingHistory() {
        historyModel.clear();
        historyExpenses.clear();

        Integer buddhistYear =
                (Integer) yearComboBox.getSelectedItem();

        if (buddhistYear == null) {
            return;
        }

        int christianYear = buddhistYear - 543;
        List<MissionExpense> expenses =
                repository.findMissions(christianYear);

        for (MissionExpense expense : expenses) {
            historyExpenses.add(expense);

            String item = String.format(
                    "%s | %s | %s | %s Bath",
                    expense.getExpenseDate().format(dateFormatter),
                    expense.getActivityName(),
                    expense.getCategory(),
                    expense.getAmount().toPlainString());

            historyModel.addElement(item);
        }
    }

    private void viewSelectedExpenseDetail() {
        int selectedIndex = historyList.getSelectedIndex();

        if (selectedIndex < 0
                || selectedIndex >= historyExpenses.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a Mission from History first.",
                    "Select Mission",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MissionExpense expense = historyExpenses.get(selectedIndex);

        String message =
                "Activity: " + expense.getActivityName()
                + "\nDate: "
                + expense.getExpenseDate().format(dateFormatter)
                + "\nCategory: " + expense.getCategory()
                + "\nAmount: "
                + expense.getAmount().toPlainString() + " Bath"
                + "\n\nDetail:\n" + expense.getDetail();

        JTextArea detailTextArea = new JTextArea(message, 10, 36);
        detailTextArea.setEditable(false);
        detailTextArea.setLineWrap(true);
        detailTextArea.setWrapStyleWord(true);
        detailTextArea.setCaretPosition(0);

        JScrollPane detailScrollPane =
                new JScrollPane(detailTextArea);
        detailScrollPane.setPreferredSize(new Dimension(420, 230));

        JOptionPane.showMessageDialog(
                this,
                detailScrollPane,
                "Mission detail",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void editSelectedExpense() {
        int selectedIndex = historyList.getSelectedIndex();

        if (selectedIndex < 0
                || selectedIndex >= historyExpenses.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a Mission from History first.",
                    "Select Mission",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MissionExpense expense = historyExpenses.get(selectedIndex);

        editingMissionId = expense.getMissionId();
        activityNameField.setText(expense.getActivityName());
        amountField.setText(expense.getAmount().toPlainString());
        dateField.setText(expense.getExpenseDate().format(dateFormatter));
        categoryComboBox.setSelectedItem(expense.getCategory());
        detailArea.setText(expense.getDetail());

        saveButton.setText("Update Mission");
        cancelEditButton.setVisible(true);
        activityNameField.requestFocusInWindow();
    }

    private void deleteSelectedExpense() {
        int selectedIndex = historyList.getSelectedIndex();

        if (selectedIndex < 0
                || selectedIndex >= historyExpenses.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a Mission from History first.",
                    "Select Mission",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MissionExpense expense = historyExpenses.get(selectedIndex);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete '" + expense.getActivityName()
                        + "' (" + expense.getAmount().toPlainString()
                        + " Bath)?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            missionService.deleteExpense(expense.getMissionId());

            if (expense.getMissionId().equals(editingMissionId)) {
                clearForm();
            }

            loadingHistory();

        } catch (RuntimeException ex) {
            showERROR(ex.getMessage());
        }
    }

    private void clearForm() {
        activityNameField.setText("");
        amountField.setText("");
        dateField.setText(LocalDate.now().format(dateFormatter));
        detailArea.setText("");
        categoryComboBox.setSelectedIndex(0);

        editingMissionId = null;
        saveButton.setText("Save");
        cancelEditButton.setVisible(false);
    }

    private void showERROR(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Not Success",
                JOptionPane.ERROR_MESSAGE);
    }
}