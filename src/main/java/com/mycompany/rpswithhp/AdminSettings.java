package com.mycompany.rpswithhp;

public class AdminSettings {

    // Who gets the god ball: 0 = nobody, 1 = player 1, 2 = player 2
    public static int godPlayer = 0;

    // God ball stats (the defaults mirror a normal rock ball)
    public static double godHp = 100;
    public static double godBaseDamage = 5;
    public static double godRadius = 30;
    public static double godStartSpeedMultiplier = 0.5;   // 1.0 = normal ball speed
    public static double godSpeedGainPerFrame = 0.03;
    public static double godMaxSpeedGain = 10;           

    public static void resetDefaults() {
        godPlayer = 0;
        godHp = 100;
        godBaseDamage = 5;
        godRadius = 30;
        godStartSpeedMultiplier = 0.5;
        godSpeedGainPerFrame = 0.03;
        godMaxSpeedGain = 10;
    }
}