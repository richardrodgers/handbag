/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag.model;

import java.util.Set;

public class TagConstraint {

    private final String name;
    private final boolean required;
    private final boolean repeat;
    private final Set<String> values;

    public TagConstraint(String name, boolean required, boolean repeat, Set<String> values) {
        this.name = name;
        this.required = required;
        this.repeat = repeat;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRepeatable() {
        return repeat;
    }

    public Set<String> getValues() {
        return values;
    }

    public boolean isRefinedBy(TagConstraint tc) {
        return name.equals(tc.name) &&
               repeat == tc.repeat &&
               values.containsAll(tc.values);
    }
}
