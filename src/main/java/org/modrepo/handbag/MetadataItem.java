/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.controlsfx.control.PropertySheet;

import org.modrepo.bagmatic.impl.profile.BagitTagConstraint;

import javafx.beans.value.ObservableValue;

/**
 * Value class specification of a metadata property
 */

public class MetadataItem implements PropertySheet.Item {
    private String name;
    private String realName;
    private String value;
    private BagitTagConstraint constraint;

    public MetadataItem(Map.Entry<String,BagitTagConstraint> entry) {
        this.realName = entry.getKey();
        //this.value = btc.getValues().get(0);
        // FIX
        this.name = realName;
        this.constraint = entry.getValue();
        // test
        if (isPreset()) {
            this.value = getPermitted().get(0);
        }
    }

    public MetadataItem(String name, BagitTagConstraint constraint) {
        this.realName = name;
        this.name = name;
        this.constraint = constraint;
        // test
        if (isPreset()) {
            this.value = getPermitted().get(0);
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

    @Override
    public String getName() {
        return isRequired() ? "*" + name : " " + name;
    }

    public String getRealName() {
        return realName;
    }

    public boolean isRequired() {
        return constraint.isRequired();
    }

    public boolean isRepeatable() {
        return constraint.isRepeatable();
    }

    public boolean isPreset() {
        return getPermitted().size() == 1;
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

    public List<String> getPermitted() {
        return constraint.getValues();
    }

    @Override
    public void setValue(Object o) {
        this.value = (String) o;
    }

    public BagitTagConstraint getSpec() {
        return constraint;
    }

    @Override
    public Optional<ObservableValue<? extends Object>> getObservableValue() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        // test only - name equality
        return this.name.equals(((MetadataItem)other).name);
    }
}
