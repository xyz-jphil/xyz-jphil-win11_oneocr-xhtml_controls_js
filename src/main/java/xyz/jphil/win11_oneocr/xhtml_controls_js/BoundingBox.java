package xyz.jphil.win11_oneocr.xhtml_controls_js;
// OCR Data Model using records
public record BoundingBox(
    double x1, double y1, double x2, double y2,
    double x3, double y3, double x4, double y4
) {
    public double height() {
        return Math.abs(y3 - y1);
    }

    public double minX() {
        return Math.min(x1, x4);
    }

    public double minY() {
        return Math.min(y1, y2);
    }

    public String toPolygonPoints() {
        // Ensure proper polygon closure by setting x4=x1 for proper rectangle (matching old JS logic)
        return Math.round(x1*10)/10.0 + "," + Math.round(y1*10)/10.0 + " " +
               Math.round(x2*10)/10.0 + "," + Math.round(y2*10)/10.0 + " " +
               Math.round(x3*10)/10.0 + "," + Math.round(y3*10)/10.0 + " " +
               Math.round(x1*10)/10.0 + "," + Math.round(y4*10)/10.0; // x4=x1 for proper closure
    }
}
