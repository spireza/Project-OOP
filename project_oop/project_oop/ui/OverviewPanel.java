package project_oop.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import project_oop.FundRepository;
import project_oop.MissionExpense;
import project_oop.MonthlyPayment;
import project_oop.ReportService;
import project_oop.Student;

public class OverviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] MONTH_NAMES = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private static final Color BACKGROUND =
            new Color(245, 247, 250);
    private static final Color GREEN =
            new Color(28, 120, 85);
    private static final Color RED =
            new Color(215, 91, 78);
    private static final Color TEXT =
            new Color(45, 55, 65);
    private static final Color MUTED =
            new Color(115, 125, 135);

    private final ReportService reportService;
    private final FundRepository repository;

    private final JComboBox<String> yearComboBox;

    // รายการปี (พ.ศ.) ที่ใช้อยู่ตอนนี้ ดึงจากรายการปีของหน้า List and Check
    private List<Integer> buddhistYears;
    private boolean updatingYears;

    private final JLabel incomeValueLabel;
    private final JLabel expenseValueLabel;
    private final JLabel balanceValueLabel;
    private final JLabel paidStudentsValueLabel;

    private final DefaultTableModel monthlyTableModel;
    private final MonthlyBarChart monthlyBarChart;

    private final NumberFormat numberFormat =
            NumberFormat.getNumberInstance(
                    Locale.forLanguageTag("th-TH"));

    public OverviewPanel(
            ReportService reportService,
            FundRepository repository) {

        this.reportService = Objects.requireNonNull(
                reportService,
                "reportService must not be null");
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null");

        setLayout(new BorderLayout(12, 12));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Overview");
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 25f));
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel =
                new JLabel("Summary of fund income and expenses");
        subtitleLabel.setFont(
                subtitleLabel.getFont().deriveFont(13f));
        subtitleLabel.setForeground(MUTED);

        JPanel titleTexts = new JPanel(new BorderLayout(0, 4));
        titleTexts.setOpaque(false);
        titleTexts.add(titleLabel, BorderLayout.NORTH);
        titleTexts.add(subtitleLabel, BorderLayout.SOUTH);

        buddhistYears = PaymentPanel.loadSavedYears();
        yearComboBox = new JComboBox<>(createYearModel());
        yearComboBox.setSelectedItem("2568");

        JPanel yearPanel = new JPanel(new BorderLayout(8, 0));
        yearPanel.setOpaque(false);
        yearPanel.add(new JLabel("Year:"), BorderLayout.WEST);
        yearPanel.add(yearComboBox, BorderLayout.CENTER);

        headerPanel.add(titleTexts, BorderLayout.WEST);
        headerPanel.add(yearPanel, BorderLayout.EAST);

        // Summary cards
        incomeValueLabel = createValueLabel();
        expenseValueLabel = createValueLabel();
        balanceValueLabel = createValueLabel();
        paidStudentsValueLabel = createValueLabel();

        JPanel summaryPanel =
                new JPanel(new GridLayout(1, 4, 12, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.add(
                createSummaryCard(
                        "Total income",
                        incomeValueLabel,
                        GREEN));
        summaryPanel.add(
                createSummaryCard(
                        "Withdrawn",
                        expenseValueLabel,
                        RED));
        summaryPanel.add(
                createSummaryCard(
                        "Remaining balance",
                        balanceValueLabel,
                        new Color(55, 105, 170)));
        summaryPanel.add(
                createSummaryCard(
                        "Students paid",
                        paidStudentsValueLabel,
                        new Color(125, 90, 165)));

        JPanel topPanel = new JPanel(new BorderLayout(0, 16));
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(summaryPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Monthly summary table
        monthlyTableModel = new DefaultTableModel(
                new Object[] {
                    "Month",
                    "Income (Bath)",
                    "Withdrawn (Bath)",
                    "Net (Bath)"
                },
                0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable monthlyTable = new JTable(monthlyTableModel);
        monthlyTable.setRowHeight(28);
        monthlyTable.setFillsViewportHeight(true);
        monthlyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        monthlyTable.getColumnModel()
                .getColumn(0).setPreferredWidth(85);
        monthlyTable.getColumnModel()
                .getColumn(1).setPreferredWidth(135);
        monthlyTable.getColumnModel()
                .getColumn(2).setPreferredWidth(150);
        monthlyTable.getColumnModel()
                .getColumn(3).setPreferredWidth(120);

        JTableHeader tableHeader = monthlyTable.getTableHeader();
        tableHeader.setReorderingAllowed(false);

        JScrollPane tableScrollPane = new JScrollPane(monthlyTable);
        tableScrollPane.setBorder(
                BorderFactory.createTitledBorder("Monthly summary"));
        tableScrollPane.setPreferredSize(new Dimension(390, 360));
        tableScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Monthly chart
        monthlyBarChart = new MonthlyBarChart();

        JSplitPane contentSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                monthlyBarChart,
                tableScrollPane);
        contentSplitPane.setResizeWeight(0.68);
        contentSplitPane.setDividerSize(8);
        contentSplitPane.setBorder(null);

        add(contentSplitPane, BorderLayout.CENTER);

        yearComboBox.addActionListener(event -> {
            if (!updatingYears) {
                refreshData();
            }
        });

        // ทุกครั้งที่สลับมาที่แท็บนี้ ให้อัปเดตรายการปี (เผื่อเพิ่มปีใหม่ที่หน้า List and Check)
        // และโหลดตัวเลขใหม่ เพื่อให้ตรงกับข้อมูลล่าสุด
        addHierarchyListener(event -> {
            if ((event.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing()) {
                refreshYearOptions();
                refreshData();
            }
        });

        refreshData();
    }

    private DefaultComboBoxModel<String> createYearModel() {
        DefaultComboBoxModel<String> model =
                new DefaultComboBoxModel<>();
        model.addElement("All");
        for (Integer year : buddhistYears) {
            model.addElement(String.valueOf(year));
        }
        return model;
    }

    /** อ่านรายการปีล่าสุด ถ้ามีปีใหม่ให้สร้างตัวเลือกใหม่และคงปีที่เลือกไว้ */
    private void refreshYearOptions() {
        List<Integer> latestYears = PaymentPanel.loadSavedYears();
        if (latestYears.equals(buddhistYears)) {
            return;
        }

        Object previousSelection = yearComboBox.getSelectedItem();
        updatingYears = true;
        try {
            buddhistYears = new ArrayList<>(latestYears);
            DefaultComboBoxModel<String> model = createYearModel();
            yearComboBox.setModel(model);

            if (previousSelection != null
                    && model.getIndexOf(previousSelection) >= 0) {
                model.setSelectedItem(previousSelection);
            } else {
                model.setSelectedItem("2568");
            }
        } finally {
            updatingYears = false;
        }
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("0 Bath", SwingConstants.CENTER);
        label.setFont(
                label.getFont().deriveFont(Font.BOLD, 20f));
        label.setForeground(TEXT);
        return label;
    }

    private JPanel createSummaryCard(
            String title,
            JLabel valueLabel,
            Color accentColor) {

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 230, 235)),
                        BorderFactory.createEmptyBorder(
                                14, 14, 14, 14))));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 13f));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void refreshData() {
        String selectedYear = (String) yearComboBox.getSelectedItem();
        if (selectedYear == null) {
            return;
        }

        boolean allYears = "All".equals(selectedYear);

        BigDecimal[] monthlyIncome = createZeroAmounts();
        BigDecimal[] monthlyExpenses = createZeroAmounts();
        Set<String> paidStudentIds = new HashSet<>();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Integer buddhistYear : buddhistYears) {
            if (!allYears
                    && !selectedYear.equals(
                            String.valueOf(buddhistYear))) {
                continue;
            }

            int christianYear = buddhistYear - 543;

            Map<Integer, BigDecimal> yearIncome =
                    reportService.getMonthlySummary(christianYear);

            for (int month = 1; month <= 12; month++) {
                BigDecimal amount = yearIncome.get(month);
                if (amount != null) {
                    monthlyIncome[month - 1] =
                            monthlyIncome[month - 1].add(amount);
                    totalIncome = totalIncome.add(amount);
                }
            }

            List<MonthlyPayment> payments =
                    repository.findPayments(christianYear);

            for (MonthlyPayment payment : payments) {
                if (payment.getStudentId() != null) {
                    paidStudentIds.add(payment.getStudentId());
                }
            }

            List<MissionExpense> missions =
                    repository.findMissions(christianYear);

            for (MissionExpense mission : missions) {
                if (mission == null
                        || mission.getExpenseDate() == null
                        || mission.getAmount() == null) {
                    continue;
                }

                int monthIndex =
                        mission.getExpenseDate().getMonthValue() - 1;

                if (monthIndex >= 0 && monthIndex < 12) {
                    monthlyExpenses[monthIndex] =
                            monthlyExpenses[monthIndex]
                                    .add(mission.getAmount());
                    totalExpenses =
                            totalExpenses.add(mission.getAmount());
                }
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpenses);
        int totalStudents = repository.findAllStudents().size();

        incomeValueLabel.setText(
                formatMoney(totalIncome) + " Bath");
        expenseValueLabel.setText(
                formatMoney(totalExpenses) + " Bath");
        balanceValueLabel.setText(
                formatMoney(balance) + " Bath");
        paidStudentsValueLabel.setText(
                paidStudentIds.size() + " / " + totalStudents);

        updateMonthlyTable(monthlyIncome, monthlyExpenses);
        monthlyBarChart.setValues(monthlyIncome, monthlyExpenses);
    }

    private BigDecimal[] createZeroAmounts() {
        BigDecimal[] amounts = new BigDecimal[12];

        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = BigDecimal.ZERO;
        }

        return amounts;
    }

    private void updateMonthlyTable(
            BigDecimal[] monthlyIncome,
            BigDecimal[] monthlyExpenses) {

        monthlyTableModel.setRowCount(0);

        for (int month = 0; month < 12; month++) {
            BigDecimal income = monthlyIncome[month];
            BigDecimal withdrawn = monthlyExpenses[month];
            BigDecimal net = income.subtract(withdrawn);

            monthlyTableModel.addRow(new Object[] {
                MONTH_NAMES[month],
                formatMoney(income),
                formatMoney(withdrawn),
                formatMoney(net)
            });
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        return numberFormat.format(amount);
    }

    private static class MonthlyBarChart extends JPanel {

        private static final long serialVersionUID = 1L;

        private final BigDecimal[] incomeValues = new BigDecimal[12];
        private final BigDecimal[] expenseValues = new BigDecimal[12];

        MonthlyBarChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(620, 380));
            setBorder(BorderFactory.createLineBorder(
                    new Color(210, 220, 230)));

            for (int i = 0; i < 12; i++) {
                incomeValues[i] = BigDecimal.ZERO;
                expenseValues[i] = BigDecimal.ZERO;
            }
        }

        void setValues(
                BigDecimal[] monthlyIncome,
                BigDecimal[] monthlyExpenses) {

            for (int i = 0; i < 12; i++) {
                incomeValues[i] = monthlyIncome[i];
                expenseValues[i] = monthlyExpenses[i];
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();

            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Chart title
                g2.setColor(TEXT);
                g2.setFont(
                        getFont().deriveFont(Font.BOLD, 13f));
                g2.drawString(
                        "Income and withdrawn by month",
                        14,
                        22);

                // Legend
                int legendY = 48;
                int legendX = 16;

                g2.setColor(GREEN);
                g2.fillRoundRect(
                        legendX, legendY - 10, 14, 14, 3, 3);
                g2.setColor(TEXT);
                g2.setFont(getFont().deriveFont(12f));
                g2.drawString(
                        "Income", legendX + 21, legendY + 2);

                int secondLegendX = legendX + 105;
                g2.setColor(RED);
                g2.fillRoundRect(
                        secondLegendX, legendY - 10, 14, 14, 3, 3);
                g2.setColor(TEXT);
                g2.drawString(
                        "Withdrawn",
                        secondLegendX + 21,
                        legendY + 2);

                int left = 60;
                int right = 20;
                int top = 72;
                int bottom = 55;

                int chartWidth = getWidth() - left - right;
                int chartHeight = getHeight() - top - bottom;

                if (chartWidth <= 0 || chartHeight <= 0) {
                    return;
                }

                double maxValue = 0;

                for (int i = 0; i < 12; i++) {
                    maxValue = Math.max(
                            maxValue,
                            incomeValues[i].doubleValue());
                    maxValue = Math.max(
                            maxValue,
                            expenseValues[i].doubleValue());
                }

                if (maxValue <= 0) {
                    maxValue = 1;
                }

                g2.setFont(getFont().deriveFont(11f));
                FontMetrics metrics = g2.getFontMetrics();

                // Grid lines and scale labels
                for (int tick = 0; tick <= 4; tick++) {
                    int y = top + chartHeight
                            - (chartHeight * tick / 4);
                    double tickValue = maxValue * tick / 4.0;

                    g2.setColor(new Color(230, 234, 238));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine(
                            left,
                            y,
                            left + chartWidth,
                            y);

                    String tickText = compactNumber(tickValue);
                    g2.setColor(MUTED);
                    g2.drawString(
                            tickText,
                            left - metrics.stringWidth(tickText) - 8,
                            y + 4);
                }

                // Monthly bars and labels
                double slotWidth = chartWidth / 12.0;
                int barWidth = Math.max(
                        5, (int) (slotWidth * 0.28));

                for (int i = 0; i < 12; i++) {
                    int centerX = left
                            + (int) Math.round(slotWidth * (i + 0.5));

                    int incomeHeight = (int) Math.round(
                            chartHeight
                                    * incomeValues[i].doubleValue()
                                    / maxValue);
                    int expenseHeight = (int) Math.round(
                            chartHeight
                                    * expenseValues[i].doubleValue()
                                    / maxValue);

                    int incomeX = centerX - barWidth - 1;
                    int expenseX = centerX + 1;

                    int incomeY =
                            top + chartHeight - incomeHeight;
                    int expenseY =
                            top + chartHeight - expenseHeight;

                    g2.setPaint(new GradientPaint(
                            incomeX,
                            incomeY,
                            GREEN.brighter(),
                            incomeX,
                            top + chartHeight,
                            GREEN));
                    g2.fillRoundRect(
                            incomeX,
                            incomeY,
                            barWidth,
                            incomeHeight,
                            5,
                            5);

                    g2.setPaint(new GradientPaint(
                            expenseX,
                            expenseY,
                            RED.brighter(),
                            expenseX,
                            top + chartHeight,
                            RED));
                    g2.fillRoundRect(
                            expenseX,
                            expenseY,
                            barWidth,
                            expenseHeight,
                            5,
                            5);

                    g2.setColor(MUTED);
                    String month = MONTH_NAMES[i];
                    g2.drawString(
                            month,
                            centerX - metrics.stringWidth(month) / 2,
                            top + chartHeight + 20);
                }

                // Baseline
                g2.setColor(new Color(160, 170, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawLine(
                        left,
                        top + chartHeight,
                        left + chartWidth,
                        top + chartHeight);

            } finally {
                g2.dispose();
            }
        }

        private String compactNumber(double value) {
            if (value >= 1_000_000) {
                return String.format(
                        Locale.US,
                        "%.1fM",
                        value / 1_000_000);
            }

            if (value >= 1_000) {
                return String.format(
                        Locale.US,
                        "%.1fk",
                        value / 1_000);
            }

            return String.format(Locale.US, "%.0f", value);
        }
    }
}