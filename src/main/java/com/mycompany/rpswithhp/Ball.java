package com.mycompany.rpswithhp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.Font;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

public class Ball {

    private final Choice choice;
    private final Choice.Matchup matchup;
    private final Color color;

    private double x, y;          
    private double vx, vy;        
    private double radius = 30;

    private double hp = 100;
    private double maxHp = 100;
    private double baseDamage = 5;
    private boolean god = false;
    private boolean cog = false;
    
    private static final double MAX_SPEED_GAIN = 10;
    private double startSpeed;                       
    private double speedGained = 0; 
    
    private static final long STICK_DURATION_MS = 2000;
    private static final long STICK_COOLDOWN_MS = 5000;   
    private static final double STICK_DAMAGE_PER_SEC = 2;
    private static final long WEAK_STICK_DURATION_MS = 1500;
    private static final double WEAK_STICK_DAMAGE_PER_SEC = 1.5;
    private Ball stuckTo = null;
    private double stickOffsetX, stickOffsetY;
    private long stickEndTime = 0;
    private long lastStickTick = 0;
    private long stickCooldownEnd = 0;
    
    private static final double SHRINK_FACTOR = 0.85;       
    private static final double MIN_RADIUS = 20;         
    private static final double MULTIPLIER_LOSS = 0.2;     
    private static final double MIN_MULTIPLIER = 0.2;      
    private double multiplierPenalty = 0;
    private static final long BLEED_DURATION_MS = 5000;
    private double bleedPerSec = 0;
    private long bleedEndTime = 0;
    private long lastBleedTick = 0;
    private static final long BLEED_COOLDOWN_MS = 4000;   // scissors can't re-apply bleed right away
    private long bleedCooldownEnd = 0;
    private long slashFlashEnd = 0;  
    
    private static class DamagePopup {
        String text;
        double px, py;
        long created;
        Color color;
        boolean big;
    }
    private static final long POPUP_LIFETIME_MS = 900;
    private final List<DamagePopup> popups = new ArrayList<>();
    private double pendingDot = 0;        
    private long lastDotPopup = 0;
    private final LinkedList<double[]> trail = new LinkedList<>();
    private static final long BLACK_FLASH_MS = 500;
    private long blackFlashEnd = 0;
    private static final long FIST_DELAY_MS = 350;               
    private static final double FIST_SECOND_HIT_MULTIPLIER = 3.0; 
    private long fistCooldownEnd = 0;
    private long fistHitTime = 0;                               
    private Ball fistTarget = null;                             
    private double fistOffX, fistOffY;  
    private long fxFirstTime = 0;                  
    private long fxSecondTime = 0;                 
    private double fxImpactX, fxImpactY;          
    private long shakeEnd = 0;
    private long shakeDuration = 1;
    private double shakeStrength = 0;
    private final List<double[]> particles = new ArrayList<>();   // x, y, vx, vy, birth, life, kind
    private final LinkedList<double[]> fistGhosts = new LinkedList<>();

    public Ball(Choice choice, Choice.Matchup matchup, double x, double y,double vx, double vy, Color color) {
        this.choice = choice;
        this.matchup = matchup;
        this.x = x;
        this.y = y;
        this.color = color;


        double speed = choice.speedMultiplier(matchup);
        this.vx = vx * speed;
        this.vy = vy * speed;
        this.startSpeed = Math.hypot(this.vx, this.vy);
    }
    public static Ball createGod(Choice choice, Choice.Matchup matchup,
                                 double x, double y, double vx, double vy) {
        Ball b = new Ball(choice, matchup, x, y, vx, vy, new Color(255, 215, 0));   
        b.god = true;
        b.hp = AdminSettings.godHp;
        b.maxHp = AdminSettings.godHp;
        b.baseDamage = AdminSettings.godBaseDamage;
        b.radius = AdminSettings.godRadius;

        
        double target = Math.hypot(vx, vy) * AdminSettings.godStartSpeedMultiplier;
        double current = Math.hypot(b.vx, b.vy);
        b.vx *= target / current;
        b.vy *= target / current;
        b.startSpeed = target;
        return b;
    }
    public static Ball createCog(double x, double y, double vx, double vy) {
        Ball b = new Ball(Choice.ROCK, Choice.Matchup.EVEN, x, y, vx, vy, new Color(255, 20, 20));
        b.cog = true;
        b.hp = CogSettings.hp;
        b.maxHp = CogSettings.hp;
        b.baseDamage = CogSettings.baseDamage;
        b.radius = CogSettings.radius;

       
        double target = Math.hypot(vx, vy);
        double current = Math.hypot(b.vx, b.vy);
        b.vx *= target / current;
        b.vy *= target / current;
        b.startSpeed = target;
        return b;
    }

    public boolean isCog() { return cog; }
    public boolean canDivergentFist() {
        return cog && fistTarget == null && System.currentTimeMillis() >= fistCooldownEnd;
    }

   
    public void startDivergentFist(Ball target) {
        long now = System.currentTimeMillis();
        fistTarget = target;
        fistHitTime = now + FIST_DELAY_MS;
        fistCooldownEnd = now + (long) (CogSettings.fistCooldownSec * 1000);

    
        double dx = x - target.x;
        double dy = y - target.y;
        double dist = Math.hypot(dx, dy);
        if (dist == 0) {
            dx = 1;
            dy = 0;
            dist = 1;
        }
        fistOffX = dx / dist * target.radius;
        fistOffY = dy / dist * target.radius;

    
        fxFirstTime = now;
        fxSecondTime = 0;
        startShake(2, 90);
        double ix = target.x + fistOffX;
        double iy = target.y + fistOffY;
        for (int i = 0; i < 6; i++) {
            addParticle(ix, iy, 2.5, 2, 350);
        }
    }

    
    public void updateDivergentFist() {
        updateFistParticles();
        recordFistGhost();

        if (fistTarget == null || System.currentTimeMillis() < fistHitTime) {
            return;
        }
        double damage = getCollisionDamage() * FIST_SECOND_HIT_MULTIPLIER;
        fistTarget.takeDivergentImpact(damage);

        
        long now = System.currentTimeMillis();
        fxSecondTime = now;
        fxImpactX = fistTarget.x + fistOffX;
        fxImpactY = fistTarget.y + fistOffY;
        startShake(9, 380);
        for (int i = 0; i < 36; i++) {
            addParticle(fxImpactX, fxImpactY, 9, i % 2, 700);    
        }
        for (int i = 0; i < 8; i++) {
            addParticle(fxImpactX, fxImpactY, 5, 2, 500);        
        }
        fistTarget = null;
    }

    
    public void takeDivergentImpact(double amount) {
        applyDamage(amount);

        DamagePopup p = new DamagePopup();
        p.text = String.format("DIVERGENT FIST! -%.1f", amount);
        p.px = x - 70;
        p.py = y - radius;
        p.created = System.currentTimeMillis();
        p.color = new Color(0, 170, 190);
        p.big = true;
        popups.add(p);
    }

    public double getShake() {
        long now = System.currentTimeMillis();
        if (now >= shakeEnd) {
            return 0;
        }
        return shakeStrength * ((shakeEnd - now) / (double) shakeDuration);
    }

    private void startShake(double strength, long durationMs) {
        if (strength >= getShake()) {            
            shakeStrength = strength;
            shakeDuration = durationMs;
            shakeEnd = System.currentTimeMillis() + durationMs;
        }
    }

    // kind: 0 = black, 1 = cyan, 2 = gray/white
    private void addParticle(double px, double py, double speed, int kind, long lifeMs) {
        double angle = Math.random() * Math.PI * 2;
        double s = speed * (0.4 + Math.random() * 0.8);
        particles.add(new double[] { px, py, Math.cos(angle) * s, Math.sin(angle) * s, System.currentTimeMillis(), lifeMs, kind });
    }

    private void updateFistParticles() {
        long now = System.currentTimeMillis();
        Iterator<double[]> it = particles.iterator();
        while (it.hasNext()) {
            double[] p = it.next();
            if (now - (long) p[4] > (long) p[5]) {
                it.remove();
                continue;
            }
            p[0] += p[2];
            p[1] += p[3];
            p[2] *= 0.94;                      
            p[3] *= 0.94;
        }
    }


    private void recordFistGhost() {
        long now = System.currentTimeMillis();
        boolean active = fistTarget != null
                || (fxSecondTime > 0 && now - fxSecondTime < 250);
        if (!active) {
            fistGhosts.clear();
            return;
        }
        fistGhosts.addFirst(new double[] { x, y });
        while (fistGhosts.size() > 8) {
            fistGhosts.removeLast();
        }
    }


    public void drawFistEffects(Graphics2D g, int arenaW, int arenaH) {
        long now = System.currentTimeMillis();

   
        int gi = 0;
        for (double[] gh : fistGhosts) {
            if (gi > 0) {
                float t = 1f - (float) gi / fistGhosts.size();
                g.setColor((gi % 2 == 0) ? new Color(0, 0, 0, (int) (70 * t))
                                         : new Color(0, 230, 255, (int) (90 * t)));
                g.fillOval((int) (gh[0] - radius), (int) (gh[1] - radius),
                           (int) (radius * 2), (int) (radius * 2));
            }
            gi++;
        }

    
        if (fistTarget != null) {
            double ix = fistTarget.x + fistOffX;
            double iy = fistTarget.y + fistOffY;
            float p = 1f - Math.max(0, fistHitTime - now) / (float) FIST_DELAY_MS;   // 0 -> 1

    
            double dx = ix - x;
            double dy = iy - y;
            double dist = Math.hypot(dx, dy);
            if (dist > 1) {
                double nx = -dy / dist;
                double ny = dx / dist;
                for (int i = -1; i <= 1; i++) {
                    double bulge = i * 14 * (1 - p) + 4 * Math.sin(now / 45.0 + i * 2);
                    QuadCurve2D c = new QuadCurve2D.Double(x, y,
                            (x + ix) / 2 + nx * bulge, (y + iy) / 2 + ny * bulge, ix, iy);
                    g.setStroke(new BasicStroke(7));
                    g.setColor(new Color(0, 0, 0, (int) (120 + 100 * p)));
                    g.draw(c);
                    g.setStroke(new BasicStroke(3));
                    g.setColor(new Color(0, 230, 255, (int) (80 + 90 * p)));
                    g.draw(c);
                }
            }

   
            int ring = (int) (6 + 40 * (1 - p));
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(0, 230, 255, (int) (80 + 150 * p)));
            g.drawOval((int) ix - ring, (int) iy - ring, ring * 2, ring * 2);

        
            long sinceFirst = now - fxFirstTime;
            if (sinceFirst < 250) {
                float q = sinceFirst / 250f;
                int rr = 5 + (int) (14 * q);
                g.setStroke(new BasicStroke(2));
                g.setColor(new Color(255, 255, 255, (int) (200 * (1 - q))));
                g.drawOval((int) ix - rr, (int) iy - rr, rr * 2, rr * 2);
            }
        }

        // Second impact: flash, shockwaves, streaks and core
        if (fxSecondTime > 0) {
            long e = now - fxSecondTime;
            if (e < 600) {
                int ix = (int) fxImpactX;
                int iy = (int) fxImpactY;

                // Brief flash over the whole arena
                if (e < 90) {
                    g.setColor(new Color(210, 255, 255, (int) (120 * (1 - e / 90f))));
                    g.fillRect(-50, -50, arenaW + 100, arenaH + 100);
                }

                // Sharp shockwave: thin bright cyan ring with a black edge (plus a later second ring)
                for (int k = 0; k < 2; k++) {
                    long ek = e - k * 90;
                    if (ek < 0 || ek > 450) {
                        continue;
                    }
                    float p = ek / 450f;
                    int rr = (int) (12 + 140 * Math.sqrt(p));
                    int alpha = (int) (255 * (1 - p));
                    g.setStroke(new BasicStroke(7f - 4f * p));
                    g.setColor(new Color(0, 0, 0, alpha));
                    g.drawOval(ix - rr, iy - rr, rr * 2, rr * 2);
                    g.setStroke(new BasicStroke(3f - 2f * p));
                    g.setColor(new Color(0, 240, 255, alpha));
                    g.drawOval(ix - rr, iy - rr, rr * 2, rr * 2);
                }

                // Energy streaks bursting outward (black core, cyan edge)
                if (e < 320) {
                    float p = e / 320f;
                    int alpha = (int) (255 * (1 - p));
                    java.util.Random rnd = new java.util.Random(fxSecondTime);
                    for (int i = 0; i < 12; i++) {
                        double a = rnd.nextDouble() * Math.PI * 2;
                        double r0 = 10 + 90 * p;
                        double r1 = r0 + 25 + rnd.nextDouble() * 35;
                        int x0 = ix + (int) (Math.cos(a) * r0);
                        int y0 = iy + (int) (Math.sin(a) * r0);
                        int x1 = ix + (int) (Math.cos(a) * r1);
                        int y1 = iy + (int) (Math.sin(a) * r1);
                        g.setStroke(new BasicStroke(6));
                        g.setColor(new Color(0, 0, 0, alpha));
                        g.drawLine(x0, y0, x1, y1);
                        g.setStroke(new BasicStroke(2));
                        g.setColor(new Color(0, 230, 255, alpha));
                        g.drawLine(x0, y0, x1, y1);
                    }
                }

                // White-hot core that collapses quickly
                if (e < 160) {
                    float p = e / 160f;
                    int core = (int) (34 * (1 - p));
                    g.setColor(new Color(0, 0, 0, 220));
                    g.fillOval(ix - core - 6, iy - core - 6, (core + 6) * 2, (core + 6) * 2);
                    g.setColor(new Color(255, 255, 255, (int) (255 * (1 - p))));
                    g.fillOval(ix - core, iy - core, core * 2, core * 2);
                }
            }
        }

        // Particles (dust puff and explosion debris)
        for (double[] pt : particles) {
            float life = (float) ((now - (long) pt[4]) / pt[5]);      // 0 -> 1
            if (life > 1) {
                continue;
            }
            int alpha = (int) (255 * (1 - life));
            int kind = (int) pt[6];
            int size = (kind == 2) ? 3 : 6;
            if (kind == 0) {
                g.setColor(new Color(0, 0, 0, alpha));
            } else if (kind == 1) {
                g.setColor(new Color(0, 230, 255, alpha));
            } else {
                g.setColor(new Color(235, 235, 235, alpha));
            }
            g.fillOval((int) pt[0] - size / 2, (int) pt[1] - size / 2, size, size);
        }
        g.setStroke(new BasicStroke(1));
    }

   
    private void drawCogGlow(Graphics2D g) {
        if (!cog) {
            return;
        }
        for (int i = 4; i >= 1; i--) {
            int grow = i * 5;
            g.setColor(new Color(255, 40, 40, 45));
            g.fillOval((int) (x - radius - grow), (int) (y - radius - grow),
                       (int) ((radius + grow) * 2), (int) ((radius + grow) * 2));
        }
    }

    public boolean isGod() { return god; }

 
    private void wallBounceRamp() {
        if (!god) {
            rampSpeed();
        }
    }


    public void move(int boxWidth, int boxHeight) {
        if (stuckTo != null) {
            return;                   
        }

        if (god) {
            rampSpeed();             
        }

        x += vx;
        y += vy;

        if (x - radius < 0) {
            x = radius;
            vx = -vx;
            wallBounceRamp();
        } else if (x + radius > boxWidth) {
            x = boxWidth - radius;
            vx = -vx;
            wallBounceRamp();
        }

        if (y - radius < 0) {
            y = radius;
            vy = -vy;
            wallBounceRamp();
        } else if (y + radius > boxHeight) {
            y = boxHeight - radius;
            vy = -vy;
            wallBounceRamp();
        }

        recordTrail();
    }

    public void draw(Graphics2D g) {
        drawTrail(g);
        drawCogGlow(g);
        g.setColor(color);
        g.fillOval((int) (x - radius), (int) (y - radius),(int) (radius * 2), (int) (radius * 2));
        
        if (choice == Choice.ROCK && speedGained > 0) {
            float intensity = Math.min(1f, (float) (speedGained / (god ? AdminSettings.godMaxSpeedGain : MAX_SPEED_GAIN)));  
            g.setStroke(new BasicStroke(2f + 3f * intensity));
            g.setColor(new Color(255, (int) (200 - 120 * intensity), 0,
                                 (int) (120 + 130 * intensity)));
            g.drawOval((int) (x - radius), (int) (y - radius),
                       (int) (radius * 2), (int) (radius * 2));
            g.setStroke(new BasicStroke(1));
        }
        
        if (bleedPerSec > 0) {
            long now = System.currentTimeMillis();
            int cx = (int) x;
            int cy = (int) y;
            int r = (int) radius;


            Shape oldClip = g.getClip();
            g.setClip(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));


            int tint = 60 + (int) (40 * Math.sin(now / 150.0));
            g.setColor(new Color(200, 0, 0, tint));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);


            for (int i = -1; i <= 1; i++) {
                int offset = i * (r / 2);
                g.setStroke(new BasicStroke(5));
                g.setColor(new Color(90, 0, 0));
                g.drawLine(cx - r, cy - r + offset, cx + r, cy + r + offset);
                g.setStroke(new BasicStroke(2));
                g.setColor(new Color(255, 60, 60));
                g.drawLine(cx - r, cy - r + offset, cx + r, cy + r + offset);
            }

            g.setClip(oldClip);


            for (int i = 0; i < 3; i++) {
                double phase = ((now + i * 220) % 900) / 900.0;   
                int dx = cx + (i - 1) * (int) (radius * 0.4);
                int dy = cy + r - 2 + (int) (phase * 22);
                int alpha = (int) (255 * (1 - phase));
                g.setColor(new Color(170, 0, 0, alpha));
                g.fillOval(dx - 2, dy, 5, 7);
            }


            if (now < slashFlashEnd) {
                float t = (slashFlashEnd - now) / 250f;       
                g.setStroke(new BasicStroke(4));
                g.setColor(new Color(1f, 1f, 1f, t));
                g.drawLine(cx - r - 8, cy + r + 8, cx + r + 8, cy - r - 8);
            }

            g.setStroke(new BasicStroke(1));
        }


        g.setColor(Color.BLACK);
        g.drawString((cog ? "Vessel" : god ? "GOD" : choice.name()) + " " + (int) Math.ceil(hp),(int) (x - radius), (int) (y - radius - 5));
        drawBlackFlash(g);
        drawPopups(g);
        
    }
    private void recordTrail() {
        if (choice != Choice.ROCK || speedGained <= 0) {
            trail.clear();
            return;
        }
        trail.addFirst(new double[] { x, y });
        int maxLength = 4 + (int) (speedGained * 1.5);
        while (trail.size() > maxLength) {
            trail.removeLast();
        }
    }

    
    private void drawTrail(Graphics2D g) {
        int n = trail.size();
        int i = 0;
        for (double[] p : trail) {
            float t = 1f - (float) i / n;              
            int alpha = (int) (110 * t);
            double r = radius * (0.5 + 0.5 * t);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillOval((int) (p[0] - r), (int) (p[1] - r), (int) (r * 2), (int) (r * 2));
            i++;
        }
    }

    private void applyDamage(double amount) {
        hp -= amount;
        if (hp < 0) {
            hp = 0;
        }
    }

    
    public void takeDamage(double amount) {
        applyDamage(amount);
        pendingDot += amount;
    }

    
    public void takeHit(double amount) {
        applyDamage(amount);
        addPopup(amount, new Color(255, 50, 50), true);
    }
    public void takeBlackFlash(double amount) {
        applyDamage(amount);
        long now = System.currentTimeMillis();
        blackFlashEnd = now + BLACK_FLASH_MS;

        DamagePopup p = new DamagePopup();
        p.text = String.format("BLACK FLASH! -%.1f", amount);
        p.px = x - 70;
        p.py = y - radius;
        p.created = now;
        p.color = new Color(10, 10, 10);
        p.big = true;
        popups.add(p);
    }

    private void drawBlackFlash(Graphics2D g) {
        long now = System.currentTimeMillis();
        if (now >= blackFlashEnd) {
            return;
        }
        float t = (blackFlashEnd - now) / (float) BLACK_FLASH_MS;   // 1 -> 0
        int cx = (int) x;
        int cy = (int) y;
        int r = (int) radius;

        // Dark aura around the ball, then the ball itself turns black
        g.setColor(new Color(0, 0, 0, (int) (140 * t)));
        g.fillOval(cx - r - 12, cy - r - 12, (r + 12) * 2, (r + 12) * 2);
        g.setColor(new Color(0, 0, 0, (int) (200 * t)));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Jagged lightning, re-rolled every 60ms so it flickers
        java.util.Random rnd = new java.util.Random(now / 60);
        for (int i = 0; i < 8; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double px = cx + Math.cos(angle) * r * 0.6;
            double py = cy + Math.sin(angle) * r * 0.6;
            for (int s = 0; s < 4; s++) {
                angle += (rnd.nextDouble() - 0.5) * 1.2;
                double len = 8 + rnd.nextDouble() * (r * 0.6);
                double nx = px + Math.cos(angle) * len;
                double ny = py + Math.sin(angle) * len;

                g.setStroke(new BasicStroke(5));
                g.setColor(new Color(0, 0, 0, (int) (230 * t)));
                g.drawLine((int) px, (int) py, (int) nx, (int) ny);
                g.setStroke(new BasicStroke(2));
                g.setColor(new Color(255, 30, 30, (int) (255 * t)));
                g.drawLine((int) px, (int) py, (int) nx, (int) ny);

                px = nx;
                py = ny;
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    private void addPopup(double amount, Color c, boolean big) {
        DamagePopup p = new DamagePopup();
        p.text = String.format("-%.1f", amount);
        p.px = x + (Math.random() * 20 - 10);
        p.py = y - radius;
        p.created = System.currentTimeMillis();
        p.color = c;
        p.big = big;
        popups.add(p);
    }

    
    public void updatePopups() {
        long now = System.currentTimeMillis();
        if (pendingDot > 0.05 && now - lastDotPopup >= 500) {
            addPopup(pendingDot, new Color(255, 140, 0), false);
            pendingDot = 0;
            lastDotPopup = now;
        }
        Iterator<DamagePopup> it = popups.iterator();
        while (it.hasNext()) {
            if (now - it.next().created > POPUP_LIFETIME_MS) {
                it.remove();
            }
        }
    }

    private void drawPopups(Graphics2D g) {
        long now = System.currentTimeMillis();
        Font oldFont = g.getFont();
        for (DamagePopup p : popups) {
            double t = (now - p.created) / (double) POPUP_LIFETIME_MS;  
            if (t > 1) {
                continue;
            }
            int alpha = (int) (255 * (1 - t));
            g.setFont(oldFont.deriveFont(Font.BOLD, p.big ? 18f : 13f));
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g.drawString(p.text, (int) p.px, (int) (p.py - 30 * t));
        }
        g.setFont(oldFont);
    }

    public boolean isDead() {
        return hp <= 0;
    }
    public boolean canBleedOpponent() {
        return choice == Choice.SCISSORS
            && System.currentTimeMillis() >= bleedCooldownEnd;
    }

    public void startBleedCooldown() {
        bleedCooldownEnd = System.currentTimeMillis() + BLEED_COOLDOWN_MS;
    }

    public double bleedRate() {
        return (matchup == Choice.Matchup.WEAK) ? 0.5 : 1.0;
    }

 
    public void applyBleed(double perSec) {
        long now = System.currentTimeMillis();
        if (bleedPerSec <= 0) {
            lastBleedTick = now;      
        }
        bleedPerSec = perSec;
        bleedEndTime = now + BLEED_DURATION_MS;
        slashFlashEnd = now + 250;
    }

    public boolean isBleeding() {
        return bleedPerSec > 0;
    }

    public void updateBleed() {
        if (bleedPerSec <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long tickEnd = Math.min(now, bleedEndTime);   
        double seconds = (tickEnd - lastBleedTick) / 1000.0;
        if (seconds > 0) {
            takeDamage(bleedPerSec * seconds);
            lastBleedTick = tickEnd;
        }
        if (now >= bleedEndTime) {
            bleedPerSec = 0;
        }
    }
    private void rampSpeed() {
        if (cog) {
            return;                   // The Cog never gains speed like rock does
        }
        double gain = god ? AdminSettings.godSpeedGainPerFrame
                          : choice.speedGainPerBounce(matchup);
        double maxGain = god ? AdminSettings.godMaxSpeedGain : MAX_SPEED_GAIN;
        if (gain <= 0) {
            return;
        }
        if (speedGained + gain > maxGain) {
            gain = maxGain - speedGained;
        }
        if (gain <= 0) {
            return;                       
        }
        double current = Math.hypot(vx, vy);
        double factor = (current + gain) / current;
        vx *= factor;
        vy *= factor;
        speedGained += gain;
    }


    public void onHitOpponent() {
        if (god) {
            return;                   
        }
        if (speedGained == 0) {
            return;
        }
        double current = Math.hypot(vx, vy);
        double factor = startSpeed / current;
        vx *= factor;
        vy *= factor;
        speedGained = 0;
    }
    public void onHurtByOpponent() {
        if (choice != Choice.PAPER || matchup != Choice.Matchup.WEAK) {
            return;
        }
        radius = Math.max(MIN_RADIUS, radius * SHRINK_FACTOR);
        multiplierPenalty += MULTIPLIER_LOSS;
    }
    
    public boolean canStick() {
        return choice == Choice.PAPER
            && stuckTo == null
            && System.currentTimeMillis() >= stickCooldownEnd;
    }
    private long stickDuration() {
        return (matchup == Choice.Matchup.WEAK) ? WEAK_STICK_DURATION_MS : STICK_DURATION_MS;
    }

    private double stickDamagePerSec() {
        return (matchup == Choice.Matchup.WEAK) ? WEAK_STICK_DAMAGE_PER_SEC : STICK_DAMAGE_PER_SEC;
    }
        

    public boolean isStuck() {
        return stuckTo != null;
    }

    public void startStick(Ball target) {
        stuckTo = target;
        stickOffsetX = x - target.x;
        stickOffsetY = y - target.y;
        long now = System.currentTimeMillis();
        stickEndTime = now + stickDuration();
        lastStickTick = now;
    }

    public boolean updateStick(int boxWidth, int boxHeight) {
        if (stuckTo == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long tickEnd = Math.min(now, stickEndTime);   
        double seconds = (tickEnd - lastStickTick) / 1000.0;
        lastStickTick = tickEnd;
        stuckTo.takeDamage(stickDamagePerSec() * seconds);

        x = stuckTo.x + stickOffsetX;
        y = stuckTo.y + stickOffsetY;
        x = Math.max(radius, Math.min(boxWidth - radius, x));
        y = Math.max(radius, Math.min(boxHeight - radius, y));

        if (now >= stickEndTime) {
            stuckTo = null;
            stickCooldownEnd = now + STICK_COOLDOWN_MS;
            return true;
        }
        return false;
    }

    public boolean collidesWith(Ball other) {
        double dx = other.x - x;
        double dy = other.y - y;
        double minDist = radius + other.radius;
        return dx * dx + dy * dy < minDist * minDist;
    }


    public void bounceOff(Ball other) {
        double dx = other.x - x;
        double dy = other.y - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) {         
            dx = 1;
            dy = 0;
            dist = 1;
        }
        double nx = dx / dist;   
        double ny = dy / dist;

        double overlap = radius + other.radius - dist + 1;
        if (overlap > 0) {
            x -= nx * overlap / 2;
            y -= ny * overlap / 2;
            other.x += nx * overlap / 2;
            other.y += ny * overlap / 2;
        }

        double dot = vx * nx + vy * ny;
        if (dot > 0) {
            vx -= 2 * dot * nx;
            vy -= 2 * dot * ny;
        }


        double dotOther = other.vx * (-nx) + other.vy * (-ny);
        if (dotOther > 0) {
            other.vx -= 2 * dotOther * (-nx);
            other.vy -= 2 * dotOther * (-ny);
        }
    }


    public double getCollisionDamage() {
        double multiplier = Math.max(MIN_MULTIPLIER,
                choice.damageMultiplier(matchup) - multiplierPenalty);
        return (baseDamage + speedGained) * multiplier;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public double getHp() { return hp; }
    public Choice getChoice() { return choice; }
    public Choice.Matchup getMatchup() { return matchup; }
    public double getMaxHp() { return maxHp; }
    public double getSpeed() { return Math.hypot(vx, vy); }
    
    public String getDisplayName() {
        return cog ? "VESSEL" : god ? "GOD BALL" : choice.name();
    }

    public String getAbilityDescription() {
        if (cog) {
            return String.format("Black Flash: %.0f%% chance on each hit to deal that hit's damage "
                    + "to the power of 2.5. Divergent Fist: a hit is followed 0.35s later by a heavy "
                    + "second impact on the same spot (3x damage). %.0fs cooldown.",
                    CogSettings.blackFlashChance * 100, CogSettings.fistCooldownSec);
        }
        if (god) {
            return "Rock's abilities, but it gains speed gradually every frame and never "
                 + "loses its speed or damage after hitting the opponent.";
        }
        return choice.abilityDescription(matchup);
    }

    public List<String> getStatusEffects(Ball opponent) {
        List<String> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        if (stuckTo != null) {
            list.add(String.format("📎 Stuck to opponent (%.1fs left)",
                    Math.max(0, (stickEndTime - now) / 1000.0)));
        }
        if (opponent.stuckTo == this) {
            list.add(String.format("⛓️ Held by opponent (%.1fs left)",
                    Math.max(0, (opponent.stickEndTime - now) / 1000.0)));
        }
        if (bleedPerSec > 0) {
            list.add(String.format("🩸 Bleeding %.1f hp/s (%.1fs left)",
                    bleedPerSec, Math.max(0, (bleedEndTime - now) / 1000.0)));
        }
        if (speedGained > 0) {
            list.add(String.format("⚡ Rock speed bonus +%.1f (+%.1f damage)",
                    speedGained, speedGained));
        }
        if (choice == Choice.PAPER && now < stickCooldownEnd) {
            list.add(String.format("⏳ Stick cooldown: %.1fs", (stickCooldownEnd - now) / 1000.0));
        }
        if (choice == Choice.SCISSORS && now < bleedCooldownEnd) {
            list.add(String.format("⏳ Bleed cooldown: %.1fs", (bleedCooldownEnd - now) / 1000.0));
        }
        if (cog) {
            long left = fistCooldownEnd - now;
            if (fistTarget != null) {
                list.add("👊 Divergent Fist: second impact incoming!");
            } else if (left > 0) {
                list.add(String.format("⏳ Divergent Fist cooldown: %.1fs", left / 1000.0));
            } else {
                list.add("👊 Divergent Fist ready");
            }
        }
        if (list.isEmpty()) {
            list.add("✅ None");
        }
        return list;
    }
}