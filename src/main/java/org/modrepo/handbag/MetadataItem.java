/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.util.Optional;

import org.controlsfx.control.PropertySheet;

import javafx.beans.value.ObservableValue;

/**
 * Value class specification of a metadata property
 */

public class MetadataItem implements PropertySheet.Item {
    private String name;
    private String realName;
    private String value;
    private boolean optional;
    private boolean sticky;

    public MetadataItem(MetadataSpec spec) {
        this.realName = spec.getName();
        this.optional = spec.isOptional();
        this.sticky = spec.isSticky();
        String baseName = sticky ? ("!" + realName) : realName;
        this.name = optional ? ("*" + baseName) : baseName;
        if (spec.getPresetValue() != null) {
            this.value = spec.getPresetValue();
        } else if (spec.getDefaultValue() != null) {
            this.value = spec.getDefaultValue();
        }
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

    @Override
    public String getCategory() {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getRealName() {
        return realName;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isSticky() {
        return sticky;
    }

    @Override
    public String getDescription() {
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void setValue(Object o) {
        this.value = (String) o;
    }

    @Override
    public Optional<ObservableValue<? extends Object>> getObservableValue() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }
}
