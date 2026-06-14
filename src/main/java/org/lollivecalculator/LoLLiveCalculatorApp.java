package org.lollivecalculator;

import org.lollivecalculator.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * <p>
 * Separated from {@link MainFrame} so the main method doesn't tie
 * bootstrap logic to Swing lifecycle.
 * </p>
 */
public final class LoLLiveCalculatorApp {

    private LoLLiveCalculatorApp() { }

    public static void main(String[] args) {
        // Use Nimbus L&F (cross-platform) instead of Windows system L&F.
        // Windows 11 dark theme overrides our button background/foreground
        // colors, making white text invisible on white buttons.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fall back to Metal (default) — still respects our custom colors
        }

        // Launch the main window on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}