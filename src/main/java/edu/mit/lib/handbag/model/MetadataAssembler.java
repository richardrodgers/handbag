/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package edu.mit.lib.handbag.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.modrepo.packr.Bag.MetadataName.*;

import edu.mit.lib.handbag.MetadataSpec;

public class MetadataAssembler {
    
    private static Map<String, TagConstraint> defaultConstraints = defaultConstraints();

    private Map<String, TagConstraint> constraints = new LinkedHashMap<>();
    private Map<String, TagTemplate> templates = new LinkedHashMap<>();

    public List<MetadataSpec> getMetadata() {
        var tcs = constraints.isEmpty() ? defaultConstraints : constraints;
        return getMetadata(tcs, templates);
    }

    public boolean addConstraints(List<TagConstraint> tcs) {
        for (TagConstraint tc : tcs) {
            var name = tc.getName();
            var curTc = constraints.containsKey(name) ? constraints.get(name) : defaultConstraints.get(name);
            if (curTc != null && ! curTc.isRefinedBy(tc)) {
                return false;
            }
        }
        for (TagConstraint tc : tcs) {
            constraints.put(tc.getName(), tc);
        }
        return true;
    }

    public boolean addTemplates(List<TagTemplate> tmps) {
        // no checks for now
        for (TagTemplate tmp : tmps) {
            templates.put(tmp.getName(), tmp);
        }
        return true;
    }

    private static List<MetadataSpec> getMetadata(Map<String, TagConstraint> tcs,
                                                  Map<String, TagTemplate> temps) {
        List<MetadataSpec> specs = new ArrayList<>();
        for (String name : tcs.keySet()) {
            specs.add(new MetadataSpec(tcs.get(name), Optional.ofNullable(temps.get(name))));
        }
        return specs;
    }

    private static Map<String, TagConstraint> defaultConstraints() {
        LinkedHashMap<String, TagConstraint> constraints = new LinkedHashMap<>();
        // just list all reserved metadata names except computed ones
        constraints.put(SOURCE_ORG.getName(), new TagConstraint(SOURCE_ORG.getName(), false, true, Set.of()));
        constraints.put(ORG_ADDR.getName(), new TagConstraint(ORG_ADDR.getName(), false, true, Set.of()));
        constraints.put(CONTACT_NAME.getName(), new TagConstraint(CONTACT_NAME.getName(), false, true, Set.of()));
        constraints.put(CONTACT_PHONE.getName(), new TagConstraint(CONTACT_PHONE.getName(), false, true, Set.of()));
        constraints.put(CONTACT_EMAIL.getName(), new TagConstraint(CONTACT_EMAIL.getName(), false, true, Set.of()));
        constraints.put(EXTERNAL_DESC.getName(), new TagConstraint(EXTERNAL_DESC.getName(), false, true, Set.of()));
        constraints.put(EXTERNAL_ID.getName(), new TagConstraint(EXTERNAL_ID.getName(), false, true, Set.of()));
        constraints.put(BAG_GROUP_ID.getName(), new TagConstraint(BAG_GROUP_ID.getName(), false, false, Set.of()));
        constraints.put(BAG_COUNT.getName(), new TagConstraint(BAG_COUNT.getName(), false, false, Set.of()));
        constraints.put(INTERNAL_SENDER_ID.getName(), new TagConstraint(INTERNAL_SENDER_ID.getName(), false, true, Set.of()));
        constraints.put(INTERNAL_SENDER_DESC.getName(), new TagConstraint(INTERNAL_SENDER_DESC.getName(), false, true, Set.of()));
        return constraints;
    }
}
