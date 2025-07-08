package io.github.lgatodu47.screenshot_viewer.config;

public enum CompressionRatio {
    NONE,
    HALF,
    QUARTER,
    EIGHTH;

    public int scale(int size) {
        return switch (this) {
            case HALF -> size / 2;
            case QUARTER -> size / 4;
            case EIGHTH -> size / 8;
            default -> size;
        };
    }
}
