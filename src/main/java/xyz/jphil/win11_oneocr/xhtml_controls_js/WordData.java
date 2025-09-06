package xyz.jphil.win11_oneocr.xhtml_controls_js;

import java.util.Optional;

public record WordData(
    String text,
    double confidence,
    int index,
    Optional<BoundingBox> boundingBox
) {}