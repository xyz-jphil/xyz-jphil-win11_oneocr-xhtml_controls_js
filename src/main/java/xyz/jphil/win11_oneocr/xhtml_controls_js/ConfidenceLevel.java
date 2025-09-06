package xyz.jphil.win11_oneocr.xhtml_controls_js;

// Confidence classification 
public enum ConfidenceLevel {
    HIGH("confidence-high", "word-box-high"),
    MEDIUM("confidence-med", "word-box-med"),
    LOW("confidence-low", "word-box-low");

    private final String htmlClass;
    private final String svgClass;

    ConfidenceLevel(String htmlClass, String svgClass) {
        this.htmlClass = htmlClass;
        this.svgClass = svgClass;
    }

    public String htmlClass() { return htmlClass; }
    public String svgClass() { return svgClass; }

    public static ConfidenceLevel fromConfidence(double confidence, Config config) {
        return confidence >= config.confidenceThresholds().high() ? HIGH :
               confidence >= config.confidenceThresholds().med() ? MEDIUM : LOW;
    }
}
