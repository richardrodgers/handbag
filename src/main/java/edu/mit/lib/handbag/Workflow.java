/**
 * Copyright 2014 MIT Libraries
 * Licensed under: http://www.apache.org/licenses/LICENSE-2.0
 */
package edu.mit.lib.handbag;

import java.util.ArrayList;
import java.util.List;

/**
 * Value class holding information about the workflow profile.
 * Will be read in from either locally bundled JSON files or
 * from a network data source.
 *
 * @author richardrodgers
 */
public class Workflow {
    private String name;
    private String icon;
    private String destinationName;
    private String destinationUrl;
    private String destinationEmail;
    private String bagNameGenerator;
    private String packageFormat;
    private String checksumType;
    private List<MetadataSpec> metadata;

    public Workflow() {
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

    public String getDestinationEmail() {
        return destinationEmail;
    }

    public void setDestinationEmail(String destinationEmail) {
        this.destinationEmail = destinationEmail;
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
    
    public String getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(String checksumType) {
        this.checksumType = checksumType;
    }

    public List<MetadataSpec> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataSpec> metadata) {
        this.metadata = metadata;
    }

    // needed to display name in ChoiceBox
    @Override
    public String toString() {
        return name;
    }
}
