package com.mycompany.rpswithhp;

public class GameSettings {


    public static boolean tieBreakersEnabled = false;
    public static boolean aiPlayer1 = false;
    public static boolean aiPlayer2 = false;
    public static double blackFlashChance = 0.20;      
    public static final double BLACK_FLASH_POWER = 2.5;

    public static double blackFlashDamage(double normalDamage) {
        
        return Math.max(normalDamage, Math.pow(normalDamage, BLACK_FLASH_POWER));
    }
}