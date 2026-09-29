/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.rpswithhp;

/**
 *
 * @author erasm
 */
public enum Choice {
    ROCK, PAPER, SCISSORS;

  
    public enum Matchup { STRONG, WEAK, EVEN }

    public Matchup matchupAgainst(Choice other) {
        if (this == other) {
            return Matchup.EVEN;      
        }
        if (beats(other)) {
            return Matchup.STRONG;
        }
        return Matchup.WEAK;
    }

    private boolean beats(Choice other) {
        return (this == ROCK && other == SCISSORS)
            || (this == PAPER && other == ROCK)
            || (this == SCISSORS && other == PAPER);
    }


    public double damageMultiplier(Matchup m) {
        if (m == Matchup.STRONG) {
            return 1.2;
        }
        return 1.0;
    }

    public double speedMultiplier(Matchup m) {
        if (this == ROCK) {
            return 0.5;
        }
        return 1.0;
    }

    public double speedGainPerBounce(Matchup m) {
        if (this == ROCK) {
            if (m == Matchup.STRONG) {
                return 1.0;
            }
            if (m == Matchup.WEAK) {
                return 0.5;
            }
            return 0.7;                 
        }
        return 0;
    }
    public String abilityDescription(Matchup m) {
        if (this == ROCK) {
            return String.format("Starts slow and gains +%.1f speed on every wall bounce. "
                    + "Every +1 speed adds +1 damage. Resets to normal speed and damage "
                    + "after hitting the opponent.", speedGainPerBounce(m));
        }
        if (this == PAPER) {
            if (m == Matchup.WEAK) {
                return "Sticks to the opponent for 1.5s, dealing 1.5 damage per second "
                     + "(only paper deals damage while stuck). 5s cooldown.";
            }
            return "Sticks to the opponent for 2s, dealing 2 damage per second "
                 + "(only paper deals damage while stuck). 5s cooldown.";
        }
        double rate = (m == Matchup.WEAK) ? 0.5 : 1.0;
        return String.format("Every hit makes the opponent bleed for %.1f hp per second "
                + "for 5s. 7s cooldown.", rate);
    }
}
