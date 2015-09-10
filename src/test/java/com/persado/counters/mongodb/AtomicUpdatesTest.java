package com.persado.counters.mongodb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

public class AtomicUpdatesTest {

	private static MongoClient mongo = null;
	private static MongoDatabase db = null;

	private static String MONGO_HOST = "qamongo.ath.persado.com";
	private static int MONGO_PORT = 29017;

	private static String COUNTER_NAME = "users";
	private static String COUNTER_FIELD = "count";
	private static String COLLECTION_NAME = "counters";
	
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
		ExecutorService executor = Executors.newFixedThreadPool(8);
		for (int i = 0; i < 1000; i++) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					incrementCount();
				}
			});
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}

		long res = db.getCollection(COLLECTION_NAME).find(new Document(id)).first().getInteger(COUNTER_FIELD);
		Assert.assertEquals(1000, res);
	}
	
	private void incrementCount() {
		db.getCollection(COLLECTION_NAME).updateOne(id, upd, updateOptions);
	}
}
