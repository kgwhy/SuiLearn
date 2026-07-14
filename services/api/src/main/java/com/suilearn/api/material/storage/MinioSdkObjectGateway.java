package com.suilearn.api.material.storage;

import io.minio.CopyObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.messages.Item;
import io.minio.CopySource;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class MinioSdkObjectGateway implements MinioObjectGateway {
    private static final long STREAM_PART_SIZE = 10L * 1024L * 1024L;
    private final MinioClient client;

    public MinioSdkObjectGateway(MinioClient client) { this.client = client; }

    @Override
    public void putPrivate(String bucket, String key, InputStream stream, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(stream, -1, STREAM_PART_SIZE).contentType(contentType).build());
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO upload failed", exception);
        }
    }

    @Override
    public InputStream getPrivate(String bucket, String key) {
        try { return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception exception) { throw new IllegalStateException("MinIO read failed", exception); }
    }

    @Override
    public void copy(String bucket, String sourceKey, String targetKey) {
        try {
            client.copyObject(CopyObjectArgs.builder().bucket(bucket).object(targetKey)
                .source(CopySource.builder().bucket(bucket).object(sourceKey).build()).build());
        } catch (Exception exception) { throw new IllegalStateException("MinIO promotion failed", exception); }
    }

    @Override
    public void delete(String bucket, String key) {
        try { client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception exception) { throw new IllegalStateException("MinIO delete failed", exception); }
    }

    @Override
    public List<StoredObject> list(String bucket, String prefix) {
        try {
            List<StoredObject> objects = new ArrayList<>();
            for (var result : client.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(true).build())) {
                Item item = result.get();
                objects.add(new StoredObject(item.objectName(), item.lastModified().toInstant()));
            }
            return objects;
        } catch (Exception exception) { throw new IllegalStateException("MinIO list failed", exception); }
    }

    @Override
    public boolean bucketExists(String bucket) {
        try { return client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()); }
        catch (Exception exception) { throw new IllegalStateException("MinIO bucket check failed", exception); }
    }

    @Override
    public void createPrivateBucket(String bucket) {
        try { client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build()); }
        catch (Exception exception) { throw new IllegalStateException("MinIO bucket creation failed", exception); }
    }
}
