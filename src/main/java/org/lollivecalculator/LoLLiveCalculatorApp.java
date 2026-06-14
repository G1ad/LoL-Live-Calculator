package org.lollivecalculator;

import org.lollivecalculator.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * <p>
 * Separated from {@link MainFrame} so the main method doesn't tie
 * bootstrap logic to Swing lifecycle. Handles JVM-level setup
 * (look & feel, splash screen, etc.) before delegating to the UI.
 * </p>
 */
public final class LoLLiveCalculatorApp {

    private LoLLiveCalculatorApp() { }

    public static void main(String[] args) {
        // Set system look & feel before creating any Swing components
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default (Metal / Nimbus)
        }

        // Launch the main window on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}