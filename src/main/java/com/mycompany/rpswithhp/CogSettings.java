package com.mycompany.rpswithhp;

public class CogSettings {

    // Who gets The Cog: 0 = nobody, 1 = player 1, 2 = player 2
    public static int cogPlayer = 0;

    public static double hp = 120;
    public static double baseDamage = 5;
    public static double radius = 30;
    public static double blackFlashChance = 0.17;     // 17% per hit
    public static double fistCooldownSec = 10;        // Divergent Fist cooldown

    public static void resetDefaults() {
        cogPlayer = 0;
        hp = 120;
        baseDamage = 5;
        radius = 30;
        blackFlashChance = 0.17;
        fistCooldownSec = 10;
    }
}