package io.odpf.firehose.objectstorage.gcs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import io.odpf.firehose.objectstorage.ObjectStorage;
import io.odpf.firehose.objectstorage.ObjectStorageException;
import io.odpf.firehose.objectstorage.gcs.error.GCSErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GCSObjectStorage implements ObjectStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GCSObjectStorage.class);
    private final GCSConfig gcsConfig;
    private final Storage storage;

    public GCSObjectStorage(GCSConfig gcsConfig) throws IOException {
        this.gcsConfig = gcsConfig;
        this.storage = StorageOptions.newBuilder()
                .setProjectId(gcsConfig.getGcsProjectId())
                .setCredentials(GoogleCredentials.fromStream(new FileInputStream(gcsConfig.getCredentialPath())))
                .build().getService();
    }

    @Override
    public void store(String localPath) throws ObjectStorageException {
        String objectName = gcsConfig.getLocalBasePath().relativize(Paths.get(localPath)).toString();
        byte[] content;
        try {
            content = Files.readAllBytes(Paths.get(localPath));
        } catch (IOException e) {
            LOGGER.error("Failed to read local file " + localPath);
            throw new ObjectStorageException("file_io_error", "File Read failed", e);
        }
        store(objectName, content);
    }

    @Override
    public void store(String objectName, byte[] content) throws ObjectStorageException {
        BlobId blobId = BlobId.of(gcsConfig.getGcsBucketName(), objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try {
            storage.create(blobInfo, content);
            LOGGER.info("Created object in GCS " + blobInfo.getBucket() + "/" + blobInfo.getName());
        } catch (StorageException e) {
            LOGGER.error("Failed to create object in GCS " + blobInfo.getBucket() + "/" + blobInfo.getName());
            String gcsErrorType = GCSErrorType.valueOfCode(e.getCode()).name();
            throw new ObjectStorageException(gcsErrorType, "GCS Upload failed", e);
        }
    }
}