package com.persado.counters.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicUpdatesTest {


    private static MongoClient mongo = null;


    private WriteConcern[] wc = new WriteConcern[]{
            WriteConcern.UNACKNOWLEDGED,
            WriteConcern.ACKNOWLEDGED,
            WriteConcern.FSYNC_SAFE,
            WriteConcern.JOURNAL_SAFE
    };


    private static String MONGO_HOST = "stgmongo01.ath.persado.com";
    //private static String MONGO_HOST = "localhost";

    private static int MONGO_PORT = 27017;

    private static String COUNTER_NAME = "users";
    private static String COUNTER_FIELD = "count";

    private static String DB_NAME = "MONGO3COUNTERS_TEST";

    private final Document id = new Document("_id", COUNTER_NAME);
    private Document upd = new Document("$inc", new Document(COUNTER_FIELD, 1));
    private UpdateOptions updateOptions = new UpdateOptions().upsert(true);

    @BeforeClass
    public static void initialize() {
        mongo = new MongoClient(MONGO_HOST, MONGO_PORT);
        System.out.println("Mongo is :" + mongo.toString());
    }


    static final AtomicLong at = new AtomicLong(System.currentTimeMillis());

    @Test
    public void atomicUpdates() throws Exception {

        Arrays.asList(wc).stream().forEach(w -> {
            MongoDatabase db = mongo.getDatabase(DB_NAME + at.incrementAndGet()).withWriteConcern(w);
            db.drop();
            System.out.println("Running test with " + db.getName() + " " + db.getWriteConcern());
            final String collectionName = "testCollection";
            atomicTestCode(db, collectionName);
            System.out.println("-----------------------------------");
        });
    }

    private void atomicTestCode(final MongoDatabase db, final String collectionName) {


        ExecutorService executor = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 10000; i++) {
            executor.submit(() -> incrementCount(db, collectionName));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(20, TimeUnit.SECONDS);

            while (!executor.isShutdown()) {
                Thread.sleep(100);
            }
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Executor is :" + executor);
        long res = db.getCollection(collectionName).find(new Document(id)).first().getInteger(COUNTER_FIELD);
        if (res != 10000) {
            System.out.println("!!!!!!! Test failed; expected 10000 but got " + res);
        } else {
            System.out.println("!!!!!!! Test OK!  got " + res);
        }
    }

    private void incrementCount(MongoDatabase db, String collectionName) {
        try {
            db.getCollection(collectionName).updateOne(id, upd, updateOptions);
        } catch (Exception e) {
            if (e.getMessage().contains("E11000")) {
                System.out.println("Thread " + Thread.currentThread().getName() + " failed; error: " + e.getMessage());
            } else {
                throw new RuntimeException("Exception in write: " + e.getMessage(), e);
            }
        }
    }
}
