package org.lollivecalculator.ui;

import org.lollivecalculator.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Factory pattern for creating consistently styled Swing components.
 * All methods return pre-configured components using ThemeConfig.
 */
public final class UIComponentFactory {

    private static final ThemeConfig T = ThemeConfig.getInstance();

    private UIComponentFactory() { }

    // ── Buttons ──────────────────────────────────────────────────────────

    public static JButton createButton(String text) {
        return createButton(text, T.BG_BUTTON);
    }

    public static JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(T.FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorder(T.BUTTON_BORDER);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createToggleLiveButton() {
        return createButton("Start Live Tracking", T.GREEN_ACTIVE);
    }

    // ── Labels ───────────────────────────────────────────────────────────

    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    public static JLabel createTitle(String text) {
        return createLabel(text, T.FONT_TITLE, T.TEXT_PRIMARY);
    }

    public static JLabel createSubtitle(String text) {
        return createLabel(text, T.FONT_SUBTITLE, T.TEXT_SECONDARY);
    }

    public static JLabel createStatusLabel(String text) {
        return createLabel(text, T.FONT_STATUS, T.TEXT_SECONDARY);
    }

    public static JPanel createStatLine(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(T.BG_PANEL);
        panel.setMaximumSize(new Dimension(T.CARD_MAX_WIDTH, T.STAT_LINE_HEIGHT));

        JLabel lblLabel = createLabel(label, T.FONT_STAT_LABEL, T.TEXT_SECONDARY);
        JLabel lblVal  = createLabel(value, T.FONT_STAT_VALUE, T.TEXT_ACCENT);
        panel.add(lblLabel, BorderLayout.WEST);
        panel.add(lblVal, BorderLayout.EAST);
        return panel;
    }

    // ── Panels ───────────────────────────────────────────────────────────

    public static JPanel createPanel() {
        return createPanel(T.BG_PANEL);
    }

    public static JPanel createPanel(Color bg) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        return p;
    }

    public static JPanel createPlaceholderCard(String message) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(T.BG_PANEL);
        card.setBorder(T.CARD_BORDER);
        JLabel lbl = createLabel(message, T.FONT_CARD_PLACEHOLDER, T.TEXT_DISABLED);
        card.add(lbl);
        return card;
    }

    // ── Scrolling ────────────────────────────────────────────────────────

    public static JScrollPane createScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        return sp;
    }
}