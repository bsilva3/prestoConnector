package main_app.query_ui;

import javax.swing.*;
import java.awt.*;

/**
 * Simple Frame to show text in a text area
 */
public class InfoViewer extends JFrame {
    private JTextArea textArea;
    private JPanel mainPanel;
    private JButton closeButton;
    private JLabel labelT;

    public InfoViewer(String labelText, String text, String title) {
        this.setPreferredSize(new Dimension(450, 600));
        setContentPane(mainPanel);
        labelT.setText(labelText);
        textArea.setText(text);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setTitle(title);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        textArea.setLineWrap(true);
        closeButton.addActionListener(e -> InfoViewer.this.dispose());
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        textArea = new JTextArea();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(textArea, gbc);
        closeButton = new JButton();
        closeButton.setText("Close");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(closeButton, gbc);
        labelT = new JLabel();
        labelT.setHorizontalAlignment(0);
        labelT.setHorizontalTextPosition(0);
        labelT.setText("Label");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(labelT, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
