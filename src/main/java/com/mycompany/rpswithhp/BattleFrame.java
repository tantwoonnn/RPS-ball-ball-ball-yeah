package com.mycompany.rpswithhp;

import javax.swing.*;
import java.awt.*;

public class BattleFrame extends JFrame {

    private static final int BOX_WIDTH = 600;
    private static final int BOX_HEIGHT = 400;

    private final int p1Lives;
    private final int p2Lives;

    private Ball ball1;
    private Ball ball2;
    private JPanel arena;
    private Timer timer;
    private StatsFrame statsFrame;

    public BattleFrame(Choice p1Choice, Choice p2Choice, int p1Lives, int p2Lives) {
        this.p1Lives = p1Lives;
        this.p2Lives = p2Lives;

        setTitle("Battle!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        Choice c1 = (AdminSettings.godPlayer == 1) ? Choice.ROCK : p1Choice;
        Choice c2 = (AdminSettings.godPlayer == 2) ? Choice.ROCK : p2Choice;
        boolean cogInPlay = CogSettings.cogPlayer != 0;
        Choice.Matchup m1 = cogInPlay ? Choice.Matchup.EVEN : c1.matchupAgainst(c2);
        Choice.Matchup m2 = cogInPlay ? Choice.Matchup.EVEN : c2.matchupAgainst(c1);

        double startSpeed = 10;
        double v1x = startSpeed * Math.cos(Math.toRadians(39));
        double v1y = startSpeed * Math.sin(Math.toRadians(39));
        double v2x = startSpeed * Math.cos(Math.toRadians(215));
        double v2y = startSpeed * Math.sin(Math.toRadians(215));

        // P1 starts on the left, P2 on the right, heading toward each other
        if (CogSettings.cogPlayer == 1) {
            ball1 = Ball.createCog(100, 100, v1x, v1y);
        } else if (AdminSettings.godPlayer == 1) {
            ball1 = Ball.createGod(c1, m1, 100, 100, v1x, v1y);
        } else {
            ball1 = new Ball(c1, m1, 100, 100, v1x, v1y, new Color(90, 160, 255));
        }
        if (CogSettings.cogPlayer == 2) {
            ball2 = Ball.createCog(BOX_WIDTH - 100, BOX_HEIGHT - 100, v2x, v2y);
        } else if (AdminSettings.godPlayer == 2) {
            ball2 = Ball.createGod(c2, m2, BOX_WIDTH - 100, BOX_HEIGHT - 100, v2x, v2y);
        } else {
            ball2 = new Ball(c2, m2, BOX_WIDTH - 100, BOX_HEIGHT - 100, v2x, v2y, new Color(255, 120, 120));
        }

        JLabel livesLabel = new JLabel(
                "P1 Lives: " + p1Lives + "     P2 Lives: " + p2Lives,
                SwingConstants.CENTER);
        add(livesLabel, BorderLayout.NORTH);


        arena = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                double shake = Math.max(ball1.getShake(), ball2.getShake());
                if (shake > 0) {
                    g2.translate((Math.random() - 0.5) * 2 * shake, (Math.random() - 0.5) * 2 * shake);
                }
                ball1.draw(g2);
                ball2.draw(g2);
                ball1.drawFistEffects(g2, getWidth(), getHeight());
                ball2.drawFistEffects(g2, getWidth(), getHeight());
            }
        };
        arena.setPreferredSize(new Dimension(BOX_WIDTH, BOX_HEIGHT));
        arena.setBackground(Color.WHITE);
        arena.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        add(arena, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        statsFrame = new StatsFrame(ball1, ball2, p1Lives, p2Lives);
        statsFrame.setLocation(getX() + getWidth() + 10, getY());
        statsFrame.setVisible(true);


        timer = new Timer(16, e -> update());
        timer.start();
    }

    private void update() {
        ball1.move(arena.getWidth(), arena.getHeight());
        ball2.move(arena.getWidth(), arena.getHeight());

        
        if (!ball1.isStuck() && !ball2.isStuck() && ball1.collidesWith(ball2)) {
            if (ball1.canBleedOpponent()) {
                ball2.applyBleed(ball1.bleedRate());
                ball1.startBleedCooldown();
            }
            if (ball2.canBleedOpponent()) {
                ball1.applyBleed(ball2.bleedRate());
                ball2.startBleedCooldown();
            }
            boolean p1Sticks = ball1.canStick();
            boolean p2Sticks = ball2.canStick();

            if (p1Sticks || p2Sticks) {
                
                if (p1Sticks) {
                    ball1.startStick(ball2);
                }
                if (p2Sticks) {
                    ball2.startStick(ball1);
                }
            } else {
                
                double damageToBall1 = ball2.getCollisionDamage();
                double damageToBall2 = ball1.getCollisionDamage();

                boolean mirror = ball1.getMatchup() == Choice.Matchup.EVEN
                        && !ball1.isCog() && !ball2.isCog();
                boolean tieBreakers = GameSettings.tieBreakersEnabled && mirror;

                boolean flash = tieBreakers && Math.random() < GameSettings.blackFlashChance;
                boolean flashHitsBall1 = Math.random() < 0.5;   


                if (ball1.isCog() && Math.random() < CogSettings.blackFlashChance) {
                    flash = true;
                    flashHitsBall1 = false;     
                } else if (ball2.isCog() && Math.random() < CogSettings.blackFlashChance) {
                    flash = true;
                    flashHitsBall1 = true;
                }

                if (flash && flashHitsBall1) {
                    ball1.takeBlackFlash(GameSettings.blackFlashDamage(damageToBall1));
                } else {
                    ball1.takeHit(damageToBall1);
                }
                if (flash && !flashHitsBall1) {
                    ball2.takeBlackFlash(GameSettings.blackFlashDamage(damageToBall2));
                } else {
                    ball2.takeHit(damageToBall2);
                }

                ball1.bounceOff(ball2);
                ball1.onHitOpponent();
                ball2.onHitOpponent();
                if (ball1.canDivergentFist()) {
                    ball1.startDivergentFist(ball2);
                }
                if (ball2.canDivergentFist()) {
                    ball2.startDivergentFist(ball1);
                }
            }
        }

       
        boolean released1 = ball1.updateStick(arena.getWidth(), arena.getHeight());
        boolean released2 = ball2.updateStick(arena.getWidth(), arena.getHeight());
        if (released1 || released2) {
            ball1.bounceOff(ball2);   
        }
        ball1.updateBleed();
        ball2.updateBleed();
        ball1.updateDivergentFist();
        ball2.updateDivergentFist();
        arena.repaint();
        ball1.updatePopups();
        ball2.updatePopups();
        statsFrame.refresh();

        if (ball1.isDead() || ball2.isDead()) {
            endRound();
            
        }
    }
    
        private void endRound() {
        timer.stop();
        statsFrame.dispose();

        boolean p1Dead = ball1.isDead();
        boolean p2Dead = ball2.isDead();
        int newP1Lives = p1Lives;
        int newP2Lives = p2Lives;
        String message;

        if (p1Dead && p2Dead) {
            message = "Draw! Both balls died at the same time. Nobody loses a life.";
        } else if (p1Dead) {
            newP1Lives--;
            message = "Player 1's ball died! Player 1 loses a life.";
        } else {
            newP2Lives--;
            message = "Player 2's ball died! Player 2 loses a life.";
        }

        JOptionPane.showMessageDialog(this, message);

        if (newP1Lives <= 0 || newP2Lives <= 0) {

            String winner = (newP1Lives <= 0) ? "Player 2" : "Player 1";
            int again = JOptionPane.showConfirmDialog(this,winner + " wins the game! Play again?","Game Over", JOptionPane.YES_NO_OPTION);
            dispose();
            if (again == JOptionPane.YES_OPTION) {
                new ChoiceFrame(3, 3).setVisible(true);
            } else {
                System.exit(0);
            }
        } else {
   
            dispose();
            new ChoiceFrame(newP1Lives, newP2Lives).setVisible(true);
        }
    }
}