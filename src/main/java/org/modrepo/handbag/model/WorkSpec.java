/**
 * Copyright 2023 MIT Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag.model;

import java.util.List;

/**
 * Value class holding information about a work specification/order.
 * Will be read in from either locally bundled or available JSON files or
 * from a network data source. A spec contains destination parameters
 * (delivery) and one or more BagIt Profile references (jobs)
 */
public class WorkSpec {
    private String name;
    private String icon;
    private String destinationName;
    private String destinationUrl;
    private String bagNameGenerator;
    private String packageFormat;
    private List<JobSpec> jobs;

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

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public void setDestinationUrl(String destinationUrl) {
        this.destinationUrl = destinationUrl;
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
