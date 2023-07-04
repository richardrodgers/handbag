/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobSpec {

    @JsonProperty("name")
    public String name;

    @JsonProperty("profile")
    public String profileAddr;
    
}
