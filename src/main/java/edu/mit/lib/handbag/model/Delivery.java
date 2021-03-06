/**
 * Copyright 2021 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package edu.mit.lib.handbag.model;

import java.util.Set;

public class Delivery {

    private String name;
    private String icon;
    private String destinationName;
    private String destinationUrl;
    private String bagNameGenerator;
    private String packageFormat = "zip";
    private Set<String> csAlgorithms = Set.of("SHA-512");
}
