package com.mycompany.rpswithhp;

import javax.swing.SwingUtilities;

public class RpsWithHP {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChoiceFrame(3, 3).setVisible(true));
    }
}