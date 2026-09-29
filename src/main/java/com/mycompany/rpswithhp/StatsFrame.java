package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;

public class StatsFrame extends JFrame {

    private final Ball ball1;
    private final Ball ball2;
    private final JTextArea p1Text = makeTextArea();
    private final JTextArea p2Text = makeTextArea();
    private final JProgressBar p1Bar = makeBar();
    private final JProgressBar p2Bar = makeBar();

    public StatsFrame(Ball ball1, Ball ball2, int p1Lives, int p2Lives) {
        this.ball1 = ball1;
        this.ball2 = ball2;

        setTitle("Stats");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);  
        setLayout(new GridLayout(1, 2, 8, 8));

        add(buildPanel("Player 1 (blue)  -  Lives: " + p1Lives, p1Bar, p1Text));
        add(buildPanel("Player 2 (red)  -  Lives: " + p2Lives, p2Bar, p2Text));

        setSize(620, 460);
        refresh();
    }

    private static JTextArea makeTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        area.setMargin(new Insets(6, 8, 6, 8));
        return area;
    }

    private static JProgressBar makeBar() {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(0, 28));
        return bar;
    }

    private JPanel buildPanel(String title, JProgressBar bar, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }


    public void refresh() {
        updateBar(p1Bar, ball1);
        updateBar(p2Bar, ball2);
        p1Text.setText(buildText(ball1, ball2));
        p2Text.setText(buildText(ball2, ball1));
    }

    private void updateBar(JProgressBar bar, Ball ball) {
        int hp = (int) Math.ceil(ball.getHp());
        int max = (int) ball.getMaxHp();
        double percent = ball.getHp() / ball.getMaxHp();

        bar.setMaximum(max);
        bar.setValue(hp);
        bar.setString("HP " + hp + " / " + max);


        if (percent > 0.5) {
            bar.setForeground(new Color(60, 180, 75));
        } else if (percent > 0.25) {
            bar.setForeground(new Color(240, 160, 30));
        } else {
            bar.setForeground(new Color(220, 50, 50));
        }
    }

    private String buildText(Ball self, Ball other) {
        StringBuilder sb = new StringBuilder();
        sb.append("Choice: ").append(self.getDisplayName()).append("\n");
        sb.append("Matchup: ").append(self.getMatchup()).append("\n\n");

        sb.append("STATS\n");
        sb.append(String.format("Damage per hit: %.1f%n", self.getCollisionDamage()));
        sb.append(String.format("Damage multiplier: %.1fx%n",
                self.getChoice().damageMultiplier(self.getMatchup())));
        sb.append(String.format("Speed: %.1f%n%n", self.getSpeed()));

        sb.append("ABILITY\n");
        sb.append(self.getAbilityDescription()).append("\n\n");

        sb.append("STATUS EFFECTS\n");
        for (String effect : self.getStatusEffects(other)) {
            sb.append(effect).append("\n");
        }
        return sb.toString();
    }
}