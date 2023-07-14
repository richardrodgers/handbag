/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;

import org.modrepo.bagmatic.impl.profile.BagitProfile;
import org.modrepo.bagmatic.model.Result;
import org.modrepo.handbag.model.WorkSpec;
import org.modrepo.packr.Bag;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpAccess {

    private static HttpClient client = HttpClient.newBuilder()
                                        .version(Version.HTTP_1_1)
                                        .followRedirects(Redirect.NORMAL)
                                        .build();

    public static Result<BagitProfile> getProfile(String profileAddr) {
        var mapper = new ObjectMapper();
        BagitProfile profile = null;
        Result<BagitProfile> result = new Result<>();
        try {
            if (profileAddr.startsWith("http")) {
                profile = mapper.readValue(getURI(profileAddr), BagitProfile.class);
            } else {
                profile = mapper.readValue(new FileInputStream(profileAddr), BagitProfile.class);
            }
            result.setObject(profile);
        } catch (Exception e) {
            result.addError(e.getMessage());
        }
        return result;
    }

    public static Result<WorkSpec> getWork(String workAddr) {
        var mapper = new ObjectMapper();
        WorkSpec work = null;
        Result<WorkSpec> result = new Result<>();
        try {
            if (workAddr.startsWith("http")) {
                work = mapper.readValue(getURI(workAddr), WorkSpec.class);
            } else {
                work = mapper.readValue(new FileInputStream(workAddr), WorkSpec.class);
            }
            result.setObject(work);
        } catch (Exception e) {
            result.addError(e.getMessage());
        }
        return result;
    }

    public static Result<String> postBag(Bag bag) {
         Result<String> result = new Result<>();
         return result;
    }

    private static String getURI(String address) {
        HttpRequest request = HttpRequest.newBuilder()
                              .uri(URI.create(address))
                              .header("Accept", "application/json")
                              .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {}
        return null;
    }

    private static String postURI(String address, Path bagPackage) {
        HttpRequest request = HttpRequest.newBuilder()
                              .uri(URI.create(address))
                              .POST(HttpRequest.BodyPublishers.ofFile(bagPackage))
                              .header("Content-Type", "application/octet-stream")
                              .build();s
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {}
        return null;
    }
}
