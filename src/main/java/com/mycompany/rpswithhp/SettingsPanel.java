package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JDialog {

    private final JCheckBox tieBreakerBox = new JCheckBox(
            "<html>Enable Tie breakers<br>"
            + "<small>(Enables critical hit chances when both players pick the same choice)</small></html>");

    public SettingsPanel(JFrame owner) {
        super(owner, "Settings", true);               
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 0, 10));

  
        JPanel general = new JPanel();
        general.setLayout(new BoxLayout(general, BoxLayout.Y_AXIS));
        general.setBorder(BorderFactory.createTitledBorder("General Settings"));

  
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
        JPanel aiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        aiPanel.setBorder(BorderFactory.createTitledBorder("AI (picks a random choice each round)"));
        JCheckBox aiP1Box = new JCheckBox("Player 1", GameSettings.aiPlayer1);
        JCheckBox aiP2Box = new JCheckBox("Player 2", GameSettings.aiPlayer2);
        aiP1Box.addActionListener(e -> GameSettings.aiPlayer1 = aiP1Box.isSelected());
        aiP2Box.addActionListener(e -> GameSettings.aiPlayer2 = aiP2Box.isSelected());
        aiPanel.add(aiP1Box);
        aiPanel.add(aiP2Box);

        general.setAlignmentX(Component.LEFT_ALIGNMENT);
        aiPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.add(general);
        sections.add(aiPanel);
        add(sections, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}