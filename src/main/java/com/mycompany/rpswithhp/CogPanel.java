package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;

public class CogPanel extends JDialog {

    private final JComboBox<String> cogPlayerBox =
            new JComboBox<>(new String[] { "Nobody", "Player 1", "Player 2" });
    private final JSpinner hpSpinner = makeSpinner(1, 100000, 10);
    private final JSpinner damageSpinner = makeSpinner(0, 1000, 1);
    private final JSpinner radiusSpinner = makeSpinner(5, 150, 1);
    private final JSpinner chanceSpinner = makeSpinner(0, 100, 1);
    private final JSpinner cooldownSpinner = makeSpinner(1, 120, 1);

    public CogPanel(JFrame owner) {
        super(owner, "The Vessel", true);                  // true = blocks the choice frame while open
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        form.add(new JLabel("Give The Vessel to:"));
        form.add(cogPlayerBox);
        form.add(new JLabel("HP:"));
        form.add(hpSpinner);
        form.add(new JLabel("Base damage:"));
        form.add(damageSpinner);
        form.add(new JLabel("Ball radius:"));
        form.add(radiusSpinner);
        form.add(new JLabel("Black Flash chance (%):"));
        form.add(chanceSpinner);
        form.add(new JLabel("Divergent Fist cooldown (sec):"));
        form.add(cooldownSpinner);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton applyButton = new JButton("Apply");
        JButton resetButton = new JButton("Reset defaults");
        JButton cancelButton = new JButton("Cancel");
        applyButton.addActionListener(e -> apply());
        resetButton.addActionListener(e -> {
            CogSettings.resetDefaults();
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


    private void loadFromSettings() {
        cogPlayerBox.setSelectedIndex(CogSettings.cogPlayer);
        hpSpinner.setValue(CogSettings.hp);
        damageSpinner.setValue(CogSettings.baseDamage);
        radiusSpinner.setValue(CogSettings.radius);
        chanceSpinner.setValue(CogSettings.blackFlashChance * 100);
        cooldownSpinner.setValue(CogSettings.fistCooldownSec);
    }


    private void apply() {
        CogSettings.cogPlayer = cogPlayerBox.getSelectedIndex();
        CogSettings.hp = value(hpSpinner);
        CogSettings.baseDamage = value(damageSpinner);
        CogSettings.radius = value(radiusSpinner);
        CogSettings.blackFlashChance = value(chanceSpinner) / 100.0;
        CogSettings.fistCooldownSec = value(cooldownSpinner);
        dispose();
    }
}