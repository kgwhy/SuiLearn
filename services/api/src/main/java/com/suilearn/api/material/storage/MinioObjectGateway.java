package com.suilearn.api.material.storage;

import java.io.InputStream;
import java.util.List;

public interface MinioObjectGateway {
    void putPrivate(String bucket, String key, InputStream stream, String contentType);
    InputStream getPrivate(String bucket, String key);
    void copy(String bucket, String sourceKey, String targetKey);
    void delete(String bucket, String key);
    List<StoredObject> list(String bucket, String prefix);
    boolean bucketExists(String bucket);
    void createPrivateBucket(String bucket);
}
