package xyz.jphil.win11_oneocr.xhtml_controls_js;

import java.util.List;
import java.util.Optional;

public record OCRData(
    Metadata metadata,
    List<LineData> lines,
    Optional<String> backgroundImagePath
) {}