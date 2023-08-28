/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;

import org.modrepo.bagmatic.model.Result;
import org.modrepo.handbag.model.WorkSpec;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpAccess {

    private static HttpClient client = HttpClient.newBuilder()
                                        .version(Version.HTTP_1_1)
                                        .followRedirects(Redirect.NORMAL)
                                        .build();
    private static ObjectMapper mapper = new ObjectMapper();

    public static Result<WorkSpec> getWork(String workAddr) {
        Result<WorkSpec> result = new Result<>();
        try {
            WorkSpec work = null;
            if (workAddr.startsWith("http")) {
                var request = HttpRequest.newBuilder()
                              .uri(URI.create(workAddr))
                              .header("Accept", "application/json")
                              .build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    work = mapper.readValue(response.body(), WorkSpec.class);
                } else {
                    result.addError("Non-success code from host: " + response.statusCode());
                }
            } else {
                work = mapper.readValue(new FileInputStream(workAddr), WorkSpec.class);
            }
            if (result.success()) {
                result.setObject(work);
            }
        } catch (Exception e) {
            result.addError(e.getMessage());
        }
        return result;
    }

    public static Result<Long> headFile(URI file) {
        Result<Long> result = new Result<>();
         try {
            HttpRequest request = HttpRequest.newBuilder()
                                .uri(file)
                                .method("HEAD", BodyPublishers.noBody())
                                .build();
            HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                Long size = Long.parseLong(response.headers().map().get("Content-Length").toString());
                result.setObject(size);
            } else {
                result.addError("Non-success code from host: " + response.statusCode());
            }
        } catch(Exception e) {
            result.addError(e.getMessage());
        }
        return result;
    }

    public static Result<String> postPackage(URI destAddr, Path bagPackage, String pkgFmt) {
        Result<String> result = new Result<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                                .uri(destAddr)
                                .POST(BodyPublishers.ofFile(bagPackage))
                                .header("Content-Type", "application/" + pkgFmt)
                                .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                result.setObject("Successful transfer");
            } else {
                result.addError("Non-success code from host: " + response.statusCode());
            }
        } catch(Exception e) {
            result.addError(e.getMessage());
        }
        return result;
    }
}
