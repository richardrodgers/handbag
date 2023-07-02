/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import javafx.beans.DefaultProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

/*
 * Trivial extension to leverage DefaultProperty in FXML
 */

@DefaultProperty(value="panes")
public class AccordionDP extends Accordion {

    public AccordionDP() {
        super();
    }

    public AccordionDP(TitledPane ... titledPanes) {
        super(titledPanes);
    }    
}
