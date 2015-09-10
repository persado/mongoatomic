package com.persado.counters.mongodb;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

@RunWith(Parameterized.class)
public class AtomicUpdatesTest {

	@Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[10][0]);
    }

    private static MongoClient mongo = null;
	private static MongoDatabase db = null;

	private static String MONGO_HOST = "127.0.0.1";
	private static int MONGO_PORT = 27017;

	private static String COUNTER_NAME = "users";
	private static String COUNTER_FIELD = "count";

	private static String DB_NAME = "COUNTERS_TEST";

	private final Document id = new Document("_id", COUNTER_NAME);
	private Document upd = new Document("$inc", new Document(COUNTER_FIELD, 1));
	private UpdateOptions updateOptions = new UpdateOptions().upsert(true);

	@BeforeClass
	public static void initialize() {
		mongo = new MongoClient(MONGO_HOST, MONGO_PORT);
        db = mongo.getDatabase(DB_NAME);
	}

	@AfterClass
	public static void after() {
        db.drop();
	}

	@Test
	public void atomicUpdates() throws Exception {
		final String collectionName = UUID.randomUUID().toString();
		ExecutorService executor = Executors.newFixedThreadPool(8);
		for (int i = 0; i < 1000; i++) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					incrementCount(collectionName);
				}
			});
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}

		long res = db.getCollection(collectionName).find(new Document(id)).first().getInteger(COUNTER_FIELD);
		Assert.assertEquals(1000, res);
		db.getCollection(collectionName).drop();
	}
	
	private void incrementCount(String collectionName) {
		db.getCollection(collectionName).updateOne(id, upd, updateOptions);
	}
}
