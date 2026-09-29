package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JDialog {

    private final JComboBox<String> godPlayerBox =
            new JComboBox<>(new String[] { "Nobody", "Player 1", "Player 2" });
    private final JSpinner hpSpinner = makeSpinner(1, 100000, 10);
    private final JSpinner damageSpinner = makeSpinner(0, 1000, 1);
    private final JSpinner radiusSpinner = makeSpinner(5, 150, 1);
    private final JSpinner startSpeedSpinner = makeSpinner(0.05, 3, 0.05);
    private final JSpinner gainSpinner = makeSpinner(0, 1, 0.005);
    private final JSpinner maxGainSpinner = makeSpinner(0, 100, 1);

    public AdminPanel(JFrame owner) {
        super(owner, "Admin Panel", true);              // true = blocks the choice frame while open
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        form.add(new JLabel("Give God Ball to:"));
        form.add(godPlayerBox);
        form.add(new JLabel("HP:"));
        form.add(hpSpinner);
        form.add(new JLabel("Base damage:"));
        form.add(damageSpinner);
        form.add(new JLabel("Ball radius:"));
        form.add(radiusSpinner);
        form.add(new JLabel("Start speed (1.0 = normal):"));
        form.add(startSpeedSpinner);
        form.add(new JLabel("Speed gain per frame:"));
        form.add(gainSpinner);
        form.add(new JLabel("Max speed gain (= max +damage):"));
        form.add(maxGainSpinner);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton applyButton = new JButton("Apply");
        JButton resetButton = new JButton("Reset defaults");
        JButton cancelButton = new JButton("Cancel");
        applyButton.addActionListener(e -> apply());
        resetButton.addActionListener(e -> {
            AdminSettings.resetDefaults();
            loadFromSettings();
        });
        cancelButton.addActionListener(e -> dispose());
        buttons.add(applyButton);
        buttons.add(resetButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        loadFromSettings();
        pack();
        setLocationRelativeTo(owner);
    }

    private static JSpinner makeSpinner(double min, double max, double step) {
        return new JSpinner(new SpinnerNumberModel(min, min, max, step));
    }

    private static double value(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    // Fill the controls with whatever is currently saved
    private void loadFromSettings() {
        godPlayerBox.setSelectedIndex(AdminSettings.godPlayer);
        hpSpinner.setValue(AdminSettings.godHp);
        damageSpinner.setValue(AdminSettings.godBaseDamage);
        radiusSpinner.setValue(AdminSettings.godRadius);
        startSpeedSpinner.setValue(AdminSettings.godStartSpeedMultiplier);
        gainSpinner.setValue(AdminSettings.godSpeedGainPerFrame);
        maxGainSpinner.setValue(AdminSettings.godMaxSpeedGain);
    }

    // Save the controls into AdminSettings and close
    private void apply() {
        AdminSettings.godPlayer = godPlayerBox.getSelectedIndex();
        AdminSettings.godHp = value(hpSpinner);
        AdminSettings.godBaseDamage = value(damageSpinner);
        AdminSettings.godRadius = value(radiusSpinner);
        AdminSettings.godStartSpeedMultiplier = value(startSpeedSpinner);
        AdminSettings.godSpeedGainPerFrame = value(gainSpinner);
        AdminSettings.godMaxSpeedGain = value(maxGainSpinner);
        dispose();
    }
}