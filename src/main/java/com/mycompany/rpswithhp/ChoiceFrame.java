package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class ChoiceFrame extends JFrame {

    private Choice p1Choice;
    private Choice p2Choice;
    private final int p1Lives;
    private final int p2Lives;

    private final JLabel p1Status = new JLabel("Not picked yet", SwingConstants.CENTER);
    private final JLabel p2Status = new JLabel("Not picked yet", SwingConstants.CENTER);
    private final JButton startButton = new JButton("Start Battle");
    private final JButton[][] choiceButtons = new JButton[2][Choice.values().length];
    private final boolean[] aiPicked = new boolean[2];
    // Konami code: Up Up Down Down Left Right Left Right B A
    private final int[] konami = {
        KeyEvent.VK_UP, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_DOWN,
        KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
        KeyEvent.VK_B, KeyEvent.VK_A
    };
    private int konamiIndex = 0;
    private KeyEventDispatcher konamiDispatcher;
    // Settings code: type S E T T I N G S (change these keys to change the code)
    private final int[] settingsCode = {
        KeyEvent.VK_S, KeyEvent.VK_E, KeyEvent.VK_T, KeyEvent.VK_T,
        KeyEvent.VK_I, KeyEvent.VK_N, KeyEvent.VK_G, KeyEvent.VK_S
    };
    private int settingsIndex = 0;
    // Sparks code: type S P A R K S to open The Cog panel
    private final int[] sparksCode = {
        KeyEvent.VK_S, KeyEvent.VK_P, KeyEvent.VK_A,
        KeyEvent.VK_R, KeyEvent.VK_K, KeyEvent.VK_S
    };
    private int sparksIndex = 0;

    public ChoiceFrame(int p1Lives, int p2Lives) {
        this.p1Lives = p1Lives;
        this.p2Lives = p2Lives;

        setTitle("Pick your choice");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));


        JLabel livesLabel = new JLabel("P1 Lives: " + p1Lives + "     P2 Lives: " + p2Lives,SwingConstants.CENTER);
        add(livesLabel, BorderLayout.NORTH);


        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.add(buildPlayerPanel(1));
        center.add(buildPlayerPanel(2));
        add(center, BorderLayout.CENTER);


        startButton.setEnabled(false);
        startButton.addActionListener(e -> startBattle());
        add(startButton, BorderLayout.SOUTH);
        applyAiSettings();

        setSize(450, 250);
        setLocationRelativeTo(null);
        konamiDispatcher = e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && isActive()) {
                handleKonami(e.getKeyCode());
                handleSettingsCode(e.getKeyCode());
                handleSparksCode(e.getKeyCode());
            }
            return false;         
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(konamiDispatcher);
    }

    private JPanel buildPlayerPanel(int player) {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Player " + player));

        for (Choice c : Choice.values()) {
            JButton button = new JButton(c.name());
            choiceButtons[player - 1][c.ordinal()] = button;
            button.addActionListener(e -> pick(player, c));
            panel.add(button);
        }

        panel.add(player == 1 ? p1Status : p2Status);
        return panel;
    }

    private void pick(int player, Choice choice) {
        if (player == 1) {
            p1Choice = choice;
            p1Status.setText("Picked!");
        } else {
            p2Choice = choice;
            p2Status.setText("Picked!");
        }
        startButton.setEnabled(p1Choice != null && p2Choice != null);
    }
    private boolean isAi(int player) {
        return player == 1 ? GameSettings.aiPlayer1 : GameSettings.aiPlayer2;
    }

    private void applyAiSettings() {
        for (int player = 1; player <= 2; player++) {
            boolean ai = isAi(player);
            for (JButton b : choiceButtons[player - 1]) {
                b.setEnabled(!ai);
            }

            if (ai && !aiPicked[player - 1]) {
                Choice[] all = Choice.values();
                pick(player, all[(int) (Math.random() * all.length)]);
                (player == 1 ? p1Status : p2Status).setText("AI picked!");
                aiPicked[player - 1] = true;
            } else if (!ai && aiPicked[player - 1]) {
             
                if (player == 1) {
                    p1Choice = null;
                    p1Status.setText("Not picked yet");
                } else {
                    p2Choice = null;
                    p2Status.setText("Not picked yet");
                }
                aiPicked[player - 1] = false;
                startButton.setEnabled(p1Choice != null && p2Choice != null);
            }
        }
    }

    private void startBattle() {
        new BattleFrame(p1Choice, p2Choice, p1Lives, p2Lives).setVisible(true);
        dispose();
    }
    
    private void handleKonami(int keyCode) {
        if (keyCode == konami[konamiIndex]) {
            konamiIndex++;
            if (konamiIndex == konami.length) {
                konamiIndex = 0;
                new AdminPanel(this).setVisible(true);
            }
        } else {
            konamiIndex = (keyCode == konami[0]) ? 1 : 0;
        }
    }
    
    private void handleSettingsCode(int keyCode) {
        if (keyCode == settingsCode[settingsIndex]) {
            settingsIndex++;
            if (settingsIndex == settingsCode.length) {
                settingsIndex = 0;
                new SettingsPanel(this).setVisible(true);
                applyAiSettings();
            }
        } else {
            settingsIndex = (keyCode == settingsCode[0]) ? 1 : 0;
        }
    }
    private void handleSparksCode(int keyCode) {
        if (keyCode == sparksCode[sparksIndex]) {
            sparksIndex++;
            if (sparksIndex == sparksCode.length) {
                sparksIndex = 0;
                new CogPanel(this).setVisible(true);
            }
        } else {
            sparksIndex = (keyCode == sparksCode[0]) ? 1 : 0;
        }
    }


    @Override
    public void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(konamiDispatcher);
        super.dispose();
    }
}