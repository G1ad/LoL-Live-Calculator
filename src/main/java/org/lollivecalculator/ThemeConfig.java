package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Singleton that centralizes all UI theme constants for consistent styling
 * across the entire application. Change values here to update the look globally.
 */
public final class ThemeConfig {

    private static final ThemeConfig INSTANCE = new ThemeConfig();

    // ─── Background colors ───────────────────────────────────────────────
    public final Color BG_DARK        = new Color(14, 18, 22);
    public final Color BG_PANEL       = new Color(22, 28, 35);
    public final Color BG_HEADER      = new Color(30, 38, 48);
    public final Color BG_TOOLBAR     = new Color(22, 28, 35);
    public final Color BG_STATUS_BAR  = new Color(10, 12, 16);
    public final Color BG_CARD_BORDER = new Color(35, 45, 55);
    public final Color BG_BUTTON      = new Color(40, 50, 65);

    // ─── Foreground / Text colors ────────────────────────────────────────
    public final Color TEXT_PRIMARY     = new Color(240, 244, 248);
    public final Color TEXT_SECONDARY   = new Color(140, 155, 170);
    public final Color TEXT_ACCENT      = new Color(255, 193, 7);
    public final Color TEXT_DISABLED    = new Color(90, 105, 120);
    public final Color TEXT_ABILITY_NAME = new Color(160, 175, 190);

    // ─── Status / functional colors ──────────────────────────────────────
    public final Color GREEN_ACTIVE    = new Color(40, 167, 69);
    public final Color RED_DANGER      = new Color(220, 53, 69);
    public final Color DAMAGE_BAR_FG   = new Color(180, 40, 55);
    public final Color DAMAGE_BAR_BG   = new Color(45, 52, 60);
    public final Color DAMAGE_BAR_BORDER = new Color(60, 70, 85);

    // ─── Fonts ───────────────────────────────────────────────────────────
    public final Font FONT_BUTTON        = new Font("Segoe UI", Font.BOLD, 13);
    public final Font FONT_TITLE         = new Font("Segoe UI", Font.BOLD, 18);
    public final Font FONT_SUBTITLE      = new Font("Segoe UI", Font.PLAIN, 12);
    public final Font FONT_STATUS        = new Font("Segoe UI", Font.PLAIN, 13);
    public final Font FONT_CARD_PLACEHOLDER = new Font("Segoe UI", Font.ITALIC, 13);
    public final Font FONT_ABILITY_SLOT  = new Font("Segoe UI", Font.BOLD, 14);
    public final Font FONT_ABILITY_DESC  = new Font("Segoe UI", Font.PLAIN, 11);
    public final Font FONT_PROGRESS_BAR  = new Font("Consolas", Font.BOLD, 11);
    public final Font FONT_STAT_VALUE    = new Font("Segoe UI", Font.BOLD, 12);
    public final Font FONT_STAT_LABEL    = new Font("Segoe UI", Font.PLAIN, 12);

    // ─── Dimensions ──────────────────────────────────────────────────────
    public final int  CARD_MAX_WIDTH     = 300;
    public final int  CARD_ABILITY_HEIGHT = 48;
    public final int  STAT_LINE_HEIGHT   = 20;
    public final int  PROGRESS_BAR_HEIGHT = 18;
    public final int  PROGRESS_BAR_WIDTH  = 100;

    // ─── Sizing & spacing ────────────────────────────────────────────────
    public final int  GAP_HORIZONTAL     = 15;
    public final int  GAP_VERTICAL       = 15;
    public final int  PADDING_SMALL      = 10;
    public final int  PADDING_MEDIUM     = 12;
    public final int  PADDING_LARGE      = 15;
    public final int  PADDING_TOOLBAR    = 15;

    // ─── Menu/Button ─────────────────────────────────────────────────────
    public final int  BUTTON_PADDING_TOP    = 8;
    public final int  BUTTON_PADDING_SIDE   = 18;
    public final int  BUTTON_BORDER_WIDTH   = 1;

    // ─── Layout ──────────────────────────────────────────────────────────
    public final int  FRAME_WIDTH   = 1250;
    public final int  FRAME_HEIGHT  = 720;
    public final int  ENEMY_CARDS   = 5;

    // ─── Borders ─────────────────────────────────────────────────────────
    public final Border BUTTON_BORDER;
    public final Border CARD_BORDER;

    private ThemeConfig() {
        BUTTON_BORDER = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BG_BUTTON.brighter(), BUTTON_BORDER_WIDTH),
                BorderFactory.createEmptyBorder(BUTTON_PADDING_TOP, BUTTON_PADDING_SIDE,
                        BUTTON_PADDING_TOP, BUTTON_PADDING_SIDE)
        );

        CARD_BORDER = BorderFactory.createLineBorder(BG_CARD_BORDER, 1);
    }

    public static ThemeConfig getInstance() {
        return INSTANCE;
    }

    /** Helper: create a progress bar pre-configured with theme settings. */
    public JProgressBar createProgressBar() {
        JProgressBar bar = new JProgressBar(0, 1000);
        bar.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT));
        bar.setStringPainted(true);
        bar.setFont(FONT_PROGRESS_BAR);
        bar.setForeground(DAMAGE_BAR_FG);
        bar.setBackground(DAMAGE_BAR_BG);
        bar.setBorder(BorderFactory.createLineBorder(DAMAGE_BAR_BORDER, 1));
        return bar;
    }

    /** Helper: create a standard panel with theme background. */
    public JPanel createPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG_PANEL);
        return p;
    }
}