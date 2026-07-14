package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.material.storage.MinioHealthIndicator;
import com.suilearn.api.material.storage.MinioObjectGateway;
import com.suilearn.api.material.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinioHealthIndicatorTest {
    @Test
    void reportsMinioDependencyWithoutExposingCredentials() {
        var up = new MinioHealthIndicator(new BucketGateway(true), "assets").health();
        var down = new MinioHealthIndicator(new BucketGateway(false), "assets").health();

        assertThat(up.getStatus().getCode()).isEqualTo("UP");
        assertThat(down.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(up.getDetails()).doesNotContainKeys("accessKey", "secretKey");
    }

    private static final class BucketGateway implements MinioObjectGateway {
        private final boolean exists;
        private BucketGateway(boolean exists) { this.exists = exists; }
        @Override public void putPrivate(String bucket, String key, InputStream stream, String contentType) { }
        @Override public InputStream getPrivate(String bucket, String key) { return new ByteArrayInputStream(new byte[0]); }
        @Override public void copy(String bucket, String sourceKey, String targetKey) { }
        @Override public void delete(String bucket, String key) { }
        @Override public List<StoredObject> list(String bucket, String prefix) { return List.of(); }
        @Override public boolean bucketExists(String bucket) { return exists; }
        @Override public void createPrivateBucket(String bucket) { }
    }
}
