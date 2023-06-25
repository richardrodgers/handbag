package org.modrepo.handbag;

import javafx.beans.DefaultProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

@DefaultProperty(value="panes")
public class AccordionDP extends Accordion {

    public AccordionDP() {
        super();
    }

    public AccordionDP(TitledPane ... titledPanes) {
        super(titledPanes);
    }    
}
