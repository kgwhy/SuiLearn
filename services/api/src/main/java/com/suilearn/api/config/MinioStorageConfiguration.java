package com.suilearn.api.config;

import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.AssetStorageLifecycleScheduler;
import com.suilearn.api.material.storage.AssetDeletionCleanupTask;
import com.suilearn.api.material.storage.AssetRecordStore;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.JpaAssetRecordStore;
import com.suilearn.api.material.storage.MinioAssetStorage;
import com.suilearn.api.material.storage.MinioObjectGateway;
import com.suilearn.api.material.storage.MinioSdkObjectGateway;
import com.suilearn.api.material.storage.MinioHealthIndicator;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import io.minio.MinioClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.HealthIndicator;

@Configuration
public class MinioStorageConfiguration {
    @Bean
    MinioClient minioClient(SuiLearnProcessingProperties properties) {
        var builder = MinioClient.builder().endpoint(properties.minioEndpoint());
        if (!properties.minioAccessKey().isBlank() && !properties.minioSecretKey().isBlank()) {
            builder.credentials(properties.minioAccessKey(), properties.minioSecretKey());
        }
        return builder.build();
    }

    @Bean
    MinioAssetStorage minioAssetStorage(MinioClient minioClient, SuiLearnProcessingProperties properties, Clock clock) {
        return MinioAssetStorage.usingMinio(new MinioSdkObjectGateway(minioClient), properties.minioBucket(), clock);
    }

    @Bean
    AssetRecordStore assetRecordStore(MaterialAssetJpaRepository assets) { return new JpaAssetRecordStore(assets); }

    @Bean
    AssetPromotionCoordinator assetPromotionCoordinator(AssetStorage storage, AssetRecordStore records) {
        return new AssetPromotionCoordinator(storage, records);
    }

    @Bean
    AssetDeletionCleanupTask assetDeletionCleanupTask(AssetStorage storage, AssetRecordStore records) {
        return new AssetDeletionCleanupTask(storage, records);
    }

    @Bean
    HealthIndicator minioHealthIndicator(MinioClient minioClient, SuiLearnProcessingProperties properties) {
        return new MinioHealthIndicator(new MinioSdkObjectGateway(minioClient), properties.minioBucket());
    }
}
