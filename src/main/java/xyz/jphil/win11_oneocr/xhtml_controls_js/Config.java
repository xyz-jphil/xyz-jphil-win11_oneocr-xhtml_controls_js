package xyz.jphil.win11_oneocr.xhtml_controls_js;

public record Config(
    ConfidenceThresholds confidenceThresholds
) {
    public static final Config DEFAULT = new Config(
        new ConfidenceThresholds(0.8, 0.5)
    );
}