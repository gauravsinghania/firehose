package io.odpf.firehose.sink.mongodb.client;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoClient;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.WriteModel;
import io.odpf.firehose.config.MongoSinkConfig;
import io.odpf.firehose.metrics.Instrumentation;
import io.odpf.firehose.sink.mongodb.util.MongoSinkClientUtil;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.odpf.firehose.metrics.Metrics.SINK_MESSAGES_DROP_TOTAL;

/**
 * The Mongo response handler.
 */
@AllArgsConstructor
public class MongoSinkClient implements Closeable {

    private final MongoCollection<Document> mongoCollection;
    private final Instrumentation instrumentation;
    private final List<String> mongoRetryStatusCodeBlacklist;
    private final MongoClient mongoClient;

    public MongoSinkClient(MongoSinkConfig mongoSinkConfig, Instrumentation instrumentation) {

        this.instrumentation = instrumentation;
        mongoClient = MongoSinkClientUtil.buildMongoClient(mongoSinkConfig, instrumentation);

        MongoDatabase database = mongoClient.getDatabase(mongoSinkConfig.getSinkMongoDBName());
        mongoCollection = database.getCollection(mongoSinkConfig.getSinkMongoCollectionName());
        mongoRetryStatusCodeBlacklist = MongoSinkClientUtil.getStatusCodesAsList(mongoSinkConfig.getSinkMongoRetryStatusCodeBlacklist());
    }


    /**
     * Processes the bulk request list of WriteModel.
     * This method performs a bulk write operation on the MongoCollection
     * If bulk write succeeds, an empty list is returned
     * If bulk write fails, then failure count is logged to instrumentation
     * and returns a list of BulkWriteErrors, whose status codes are
     * not present in retry status code blacklist
     *
     * @param request the bulk request
     * @return the list of non-blacklisted Bulk Write errors, if any, else returns empty list
     */
    public List<BulkWriteError> processRequest(List<WriteModel<Document>> request) {
        List<BulkWriteError> writeErrors = new ArrayList<>();
        try {
            logResults(mongoCollection.bulkWrite(request));

        } catch (MongoBulkWriteException writeException) {
            instrumentation.logWarn("Bulk request failed");

            writeErrors = writeException.getWriteErrors();
            logErrors(writeErrors);
        }
        return writeErrors.stream()
                .filter(writeError -> !mongoRetryStatusCodeBlacklist.contains(String.valueOf(writeError.getCode())))
                .collect(Collectors.toList());
    }

    private void logResults(BulkWriteResult writeResult) {

        if (writeResult.wasAcknowledged()) {
            instrumentation.logInfo("Bulk Write operation was successfully acknowledged");
        } else {
            instrumentation.logWarn("Bulk Write operation was not acknowledged");
        }
    }

    /**
     * Handle write errors.
     *
     * @param writeErrors the write errors
     */
    private void logErrors(List<BulkWriteError> writeErrors) {

        writeErrors.stream()
                .filter(writeError -> mongoRetryStatusCodeBlacklist.contains(String.valueOf(writeError.getCode())))
                .forEach(writeError -> {
                    instrumentation.logWarn("Non-retriable error due to response status: {} is under blacklisted status code", writeError.getCode());
                    instrumentation.incrementCounterWithTags(SINK_MESSAGES_DROP_TOTAL, "cause=" + writeError.getMessage());
                    instrumentation.logInfo("Message dropped because of status code: " + writeError.getCode());
                });

        instrumentation.logWarn("Bulk request failed count: {}", writeErrors.size());
    }

    @Override
    public void close() throws IOException {
        mongoClient.close();
    }
}