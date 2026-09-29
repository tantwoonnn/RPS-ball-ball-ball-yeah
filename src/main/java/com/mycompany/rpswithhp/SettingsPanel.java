package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JDialog {

    private final JCheckBox tieBreakerBox = new JCheckBox(
            "<html>Enable Tie breakers<br>"
            + "<small>(Enables critical hit chances when both players pick the same choice)</small></html>");

    public SettingsPanel(JFrame owner) {
        super(owner, "Settings", true);                 // true = blocks the choice frame while open
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 0, 10));

        // General Settings section
        JPanel general = new JPanel();
        general.setLayout(new BoxLayout(general, BoxLayout.Y_AXIS));
        general.setBorder(BorderFactory.createTitledBorder("General Settings"));

        // Black Flash chance slider (0% - 50%)
        JLabel chanceLabel = new JLabel();
        JSlider chanceSlider = new JSlider(0, 50,
                (int) Math.round(GameSettings.blackFlashChance * 100));
        chanceSlider.setMajorTickSpacing(10);
        chanceSlider.setMinorTickSpacing(5);
        chanceSlider.setPaintTicks(true);
        chanceSlider.setPaintLabels(true);
        chanceLabel.setText("Black Flash chance per hit: " + chanceSlider.getValue() + "%");
        chanceSlider.addChangeListener(e -> {
            GameSettings.blackFlashChance = chanceSlider.getValue() / 100.0;
            chanceLabel.setText("Black Flash chance per hit: " + chanceSlider.getValue() + "%");
        });

        // Tie breaker checkbox (the slider is greyed out while it's off)
        tieBreakerBox.setSelected(GameSettings.tieBreakersEnabled);
        chanceSlider.setEnabled(tieBreakerBox.isSelected());
        chanceLabel.setEnabled(tieBreakerBox.isSelected());
        tieBreakerBox.addActionListener(e -> {
            GameSettings.tieBreakersEnabled = tieBreakerBox.isSelected();
            chanceSlider.setEnabled(tieBreakerBox.isSelected());
            chanceLabel.setEnabled(tieBreakerBox.isSelected());
        });

        tieBreakerBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        chanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chanceSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        general.add(tieBreakerBox);
        general.add(Box.createVerticalStrut(8));
        general.add(chanceLabel);
        general.add(chanceSlider);
        add(general, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}