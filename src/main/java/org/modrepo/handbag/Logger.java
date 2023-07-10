/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

public class Logger {
    
    private final Deque<String> logQueue = new LinkedBlockingDeque<>(12);

    public Logger() {
    }

    public String currentEntry() {
        var cur = logQueue.peekFirst();
        return (cur != null) ? cur : "Empty";
    }

    public String previousEntry(String entry) {
        var iter = logQueue.iterator();
        while (iter.hasNext()) {
            var logEnt = iter.next();
            if (logEnt.equals(entry)) {
                return iter.hasNext() ? iter.next() : currentEntry();
            }
        }
        return "None";
    }

    public void log(String entry) {
        while (! logQueue.offerFirst(entry)) {
            logQueue.removeLast();
        }
    }

    public void logTransfer(Path logDir, boolean append,
                            String bagName, long size, String destination) {
        // no-op if no logDir defined
        if (logDir.toString().isEmpty()) return;
        var xferLog = logDir.resolve("handbag-xfer.log");
        var logExists = Files.exists(xferLog);
        StandardOpenOption openOpt;
        if (logExists & append) {
            openOpt = StandardOpenOption.APPEND;
        } else if (logExists) {
            openOpt = StandardOpenOption.TRUNCATE_EXISTING;
        } else {
            openOpt = StandardOpenOption.CREATE;
        }
        var truncNow = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        var entry = String.format("%s %s %d %s\n", truncNow, bagName, size, destination);
        try {
            Files.write(xferLog, entry.getBytes(), openOpt);
        } catch (Exception e) {
            log(e.getMessage());
        }
    }
}
