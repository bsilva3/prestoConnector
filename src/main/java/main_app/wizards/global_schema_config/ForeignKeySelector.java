package main_app.wizards.global_schema_config;

import helper_classes.GlobalColumnData;
import helper_classes.GlobalTableData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ForeignKeySelector extends JFrame {
    private JPanel mainPanel;
    private JComboBox tableComboBox;
    private JComboBox columnComboBox;
    private DefaultComboBoxModel tableModel;
    private DefaultComboBoxModel colModel;
    private JButton confirmButton;
    private List<GlobalTableData> globalTabs;
    private GlobalSchemaConfiguration schemaConfig;

    public ForeignKeySelector(GlobalSchemaConfiguration schemaConfig, List<GlobalTableData> globalTabs) {
        this.schemaConfig = schemaConfig;
        this.globalTabs = globalTabs;
        fillTablesComboBox();
        tableComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setColumnsComboBox(globalTabs.get(tableComboBox.getSelectedIndex()));
            }
        });
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                GlobalTableData g = globalTabs.get(tableComboBox.getSelectedIndex());
                GlobalColumnData c = g.getGlobalColumnData(columnComboBox.getSelectedItem().toString());
                schemaConfig.addForeignKey(g, c);
                dispose();
            }
        });
        this.setPreferredSize(new Dimension(600, 600));
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setTitle("Add Foreign Key");
        pack();
        this.setVisible(true);
    }

    private void fillTablesComboBox() { //TODO: error when primary keys do not exist anywhere
        String[] s = new String[globalTabs.size()];
        for (int i = 0; i < globalTabs.size(); i++) {
            s[i] = globalTabs.get(i).getTableName();
        }
        tableModel = new DefaultComboBoxModel(s);
        tableComboBox.setModel(tableModel);
        tableComboBox.setSelectedIndex(0);
        setColumnsComboBox(globalTabs.get(0));
    }

    private boolean setColumnsComboBox(GlobalTableData t) {
        List<GlobalColumnData> primKeyCols = t.getGlobalColumnDataList();
        if (primKeyCols.size() == 0) {
            JOptionPane.showMessageDialog(mainPanel, "No primary keys defined on global tables", "No primary keys", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        String[] s = new String[primKeyCols.size()];
        for (int i = 0; i < primKeyCols.size(); i++) {
            s[i] = primKeyCols.get(i).getName();
        }
        colModel = new DefaultComboBoxModel(s);
        columnComboBox.setModel(colModel);
        columnComboBox.setSelectedIndex(0);
        return true;
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
        tableComboBox = new JComboBox();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 0, 0);
        mainPanel.add(tableComboBox, gbc);
        columnComboBox = new JComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 0, 0);
        mainPanel.add(columnComboBox, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 3;
        gbc.weightx = 0.2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spacer1, gbc);
        confirmButton = new JButton();
        confirmButton.setText("Confirm");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(10, 0, 0, 0);
        mainPanel.add(confirmButton, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Select a Global table:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(20, 0, 0, 0);
        mainPanel.add(label1, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Select a Primary Key to be referenced:");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(20, 0, 0, 0);
        mainPanel.add(label2, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weighty = 0.1;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(spacer2, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
