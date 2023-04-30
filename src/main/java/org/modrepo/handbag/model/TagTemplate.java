/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag.model;

import java.util.List;

public class TagTemplate {

    private final String name;
    private final boolean edit;
    private final List<String> values;

    public TagTemplate(String name, boolean edit, List<String> values) {
        this.name = name;
        this.edit = edit;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public boolean isEditable() {
        return edit;
    }

    public List<String> getValues() {
        return values;
    }
}
