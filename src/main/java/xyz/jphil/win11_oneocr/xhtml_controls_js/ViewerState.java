package xyz.jphil.win11_oneocr.xhtml_controls_js;

// State management using records
public record ViewerState(
    boolean showLineBoxes,
    boolean showWordBoxes,
    boolean showXHTMLText,
    boolean showSVGText,
    boolean enableHoverControls,
    boolean showSVGSection,
    boolean showSVGBackground,
    boolean initialized
) {
    public static final ViewerState DEFAULT = new ViewerState(
        false,  // showLineBoxes - OFF for clean initial experience
        false,  // showWordBoxes - OFF for clean initial experience  
        true,   // showXHTMLText - Keep original XHTML content visible
        false,  // showSVGText - OFF for clean initial experience
        false,  // enableHoverControls - OFF for clean initial experience
        false,  // showSVGSection - OFF for clean initial experience
        false,  // showSVGBackground - OFF for clean initial experience
        false   // initialized
    );

    public ViewerState withInitialized(boolean initialized) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withShowLineBoxes(boolean showLineBoxes) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withShowWordBoxes(boolean showWordBoxes) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withShowXHTMLText(boolean showXHTMLText) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withShowSVGText(boolean showSVGText) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withEnableHoverControls(boolean enableHoverControls) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withShowSVGSection(boolean showSVGSection) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }

    public ViewerState withShowSVGBackground(boolean showSVGBackground) {
        return new ViewerState(showLineBoxes, showWordBoxes, showXHTMLText,
            showSVGText, enableHoverControls, showSVGSection, showSVGBackground, initialized);
    }
}

