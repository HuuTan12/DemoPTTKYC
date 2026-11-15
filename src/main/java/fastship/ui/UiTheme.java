package fastship.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Standard Java Swing helper methods (default Swing look & feel, no custom colors/shadows).
 */
public class UiTheme {

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_CARD_TITLE = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BUTTON = new Font("SansSerif", Font.PLAIN, 12);

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatVnd(long amount) {
        return String.format(Locale.US, "%,d VND", amount);
    }

    public static String formatNumber(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(DT_FORMATTER);
    }

    public static JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        if (title != null && !title.isEmpty()) {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(title),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
        } else {
            panel.setBorder(createCardBorder());
        }
        return panel;
    }

    public static JPanel createCardPanel() {
        return createCardPanel(null);
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        return btn;
    }

    public static JButton createOutlineButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        return btn;
    }

    public static JTextField createTextField(String initialText) {
        JTextField tf = new JTextField(initialText);
        tf.setFont(FONT_BODY);
        return tf;
    }

    public static JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        return lbl;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
}
