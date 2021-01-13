/**
 * Copyright 2014 MIT Libraries
 * Licensed under: http://www.apache.org/licenses/LICENSE-2.0
 */
package edu.mit.lib.handbag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.mit.lib.handbag.model.TagConstraint;
import edu.mit.lib.handbag.model.TagTemplate;

/**
 * Value class specification of a metadata property
 *
 * @author richardrodgers
 */

public class MetadataSpec {
    private String name;
    private String presetValue;
    private String defaultValue;
    private List<String> valueList;
    private boolean optional;
    private boolean sticky;

    public MetadataSpec() {}

    public MetadataSpec(TagConstraint constraint, Optional<TagTemplate> templateOpt) {
        this.name = constraint.getName();
        this.optional = ! constraint.isRequired();
        this.valueList = new ArrayList<String>(constraint.getValues());
        this.sticky = false;
        if (templateOpt.isPresent()) {
            TagTemplate template = templateOpt.get();
            if (template.isEditable()) {
                defaultValue = template.getValues().get(0);
            } else {
                presetValue = template.getValues().get(0);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPresetValue() {
        return presetValue;
    }

    public void setPresetValue(String presetValue) {
        this.presetValue = presetValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public boolean needsValue() {
        return (presetValue == null) && (defaultValue == null) && (valueList == null);
    }
}
