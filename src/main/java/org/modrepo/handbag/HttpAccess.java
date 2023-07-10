/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;

import org.modrepo.bagmatic.impl.profile.BagitProfile;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpAccess {

    private static HttpClient client = HttpClient.newBuilder()
                                        .version(Version.HTTP_1_1)
                                        .followRedirects(Redirect.NORMAL)
                                        .build();

    public static BagitProfile getProfile(String profileUri) {
        var mapper = new ObjectMapper();
        BagitProfile profile = null;
        try {
            if (profileUri.startsWith("http")) {
                profile = mapper.readValue(getURI(profileUri), BagitProfile.class);
            } else {
                profile = mapper.readValue(new FileInputStream(profileUri), BagitProfile.class);
            }
        } catch (Exception e) {}
        return profile;
    }

    private static String getURI(String address) {
        HttpRequest request = HttpRequest.newBuilder()
                              .uri(URI.create(address))
                              .header("Content-Type", "application/json")
                              .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {}
        
        return null;
    }
}
