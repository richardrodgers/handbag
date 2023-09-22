/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Value class holding information about a work specification/order.
 * Will be read in from either locally bundled or available JSON files or
 * from a network data source. A spec contains destination parameters
 * (delivery) and one or more BagIt Profile references (jobs)
 */
public class WorkSpec {

    @JsonProperty("name")
    public String name;

    private String icon;

    @JsonProperty("destination")
    public String destinationName;

    @JsonProperty("destinationAddress")
    public String destinationAddr;

    private String bagNameGenerator;
    private String packageFormat;

    @JsonProperty("jobs")
    public List<JobSpec> jobs = new ArrayList<>();

    public WorkSpec() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getDestinationAddr() {
        return destinationAddr;
    }

    public void setDestinationUrl(String destinationAddr) {
        this.destinationAddr = destinationAddr;
    }

    public String getBagNameGenerator() {
        return bagNameGenerator;
    }

    public void setBagNameGenerator(String bagNameGenerator) {
        this.bagNameGenerator = bagNameGenerator;
    }

    public String getPackageFormat() {
        return packageFormat;
    }

    public void setPackageFormat(String packageFormat) {
        this.packageFormat = packageFormat;
    }

    // needed to display name in ChoiceBox
    @Override
    public String toString() {
        return name;
    }
}
