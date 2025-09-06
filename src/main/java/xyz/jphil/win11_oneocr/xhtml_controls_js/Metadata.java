package xyz.jphil.win11_oneocr.xhtml_controls_js;

public record Metadata(
    String filename,
    int imageWidth,
    int imageHeight,
    double angle,
    double averageConfidence,
    int totalWords,
    int totalLines
) {}