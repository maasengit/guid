package com.abc.guid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GUIDGeneratorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GUIDGeneratorTest.class);

    public static void main(String[] args) throws Exception {
        ConcurrentHashMap uuids = new ConcurrentHashMap();
        ExecutorService threadPool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 500; i++) {
            threadPool.submit(() -> {
                long id = GUIDGenerator.nextId();
                if (uuids.contains(id)) {
                    LOGGER.error("" + id);
                } else {
                    uuids.put(id, 1);
                    LOGGER.info("" + id);
                }
            });
        }
        id2Str();
        Thread.sleep(10000);
        System.exit(0);
    }

    public static void id2Str() {
        System.out.println(GUIDGenerator.id2Str(113274030270189578L));
        System.out.println(GUIDGenerator.id2Str(113274030270189579L));
        System.out.println(GUIDGenerator.id2Str(113274030270189580L));
        System.out.println(GUIDGenerator.id2Str(113274030270189581L));
    }
}
