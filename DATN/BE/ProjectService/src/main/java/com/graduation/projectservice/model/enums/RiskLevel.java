package com.graduation.projectservice.model.enums;

import lombok.Getter;

@Getter
public enum RiskLevel {
    VERY_HIGH(5, "5-Very high"),
    HIGH(4, "4-High"),
    MEDIUM(3, "3-Medium"),
    LOW(2, "2-Low"),
    VERY_LOW(1, "1-Very low");

    private final int value;
    private final String label;

    RiskLevel(int value, String label) {
        this.value = value;
        this.label = label;
    }

    // Helper to find Enum by label (e.g., if Frontend sends "5-Very high")
    public static RiskLevel fromLabel(String text) {
        for (RiskLevel b : RiskLevel.values()) {
            if (b.label.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return VERY_LOW; // Default fallback as requested
    }
}