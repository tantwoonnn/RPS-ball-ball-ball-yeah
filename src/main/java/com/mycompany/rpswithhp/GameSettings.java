package com.mycompany.rpswithhp;

public class GameSettings {

    // When on, mirror matches (both players pick the same choice) can roll critical hits
    public static boolean tieBreakersEnabled = false;
    public static double blackFlashChance = 0.20;        // 20% per hit
    public static final double BLACK_FLASH_POWER = 2.5;

    public static double blackFlashDamage(double normalDamage) {
        
        return Math.max(normalDamage, Math.pow(normalDamage, BLACK_FLASH_POWER));
    }
}