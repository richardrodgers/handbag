/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.util.ArrayDeque;
import java.util.Deque;

public class ConsoleLog {
    
    private final Deque<String> logQueue = new ArrayDeque<>(12);

    public ConsoleLog() {
    }

    public String currentEntry() {
        var cur = logQueue.peekFirst();
        return (cur != null) ? cur : "";
    }

    public String previousEntry(String entry) {
        var iter = logQueue.iterator();
        while (iter.hasNext()) {
            var logEnt = iter.next();
            if (logEnt.equals(entry)) {
                return iter.hasNext() ? iter.next() : currentEntry();
            }
        }
        return "";
    }

    public void log(String entry) {
        while (! logQueue.offerFirst(entry)) {
            logQueue.removeLast();
        }
    }
}
