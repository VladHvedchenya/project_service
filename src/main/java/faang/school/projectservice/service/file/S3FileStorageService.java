package faang.school.projectservice.service.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import faang.school.projectservice.config.s3.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "services.s3", name = "isMocked", havingValue = "false", matchIfMissing = true)
public class S3FileStorageService implements FileStorageService {
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    @Override
    public String upload(byte[] content, String contentType, String extension, String keyPrefix) {
        ensureBucketExists();

        String fileKey = "%s/%s.%s".formatted(keyPrefix, UUID.randomUUID(), extension);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.setContentType(contentType);

        PutObjectRequest request = new PutObjectRequest(
                s3Properties.getBucketName(),
                fileKey,
                new ByteArrayInputStream(content),
                metadata
        );
        amazonS3.putObject(request);
        return fileKey;
    }

    @Override
    public void delete(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        amazonS3.deleteObject(s3Properties.getBucketName(), fileKey);
    }

    private void ensureBucketExists() {
        String bucketName = s3Properties.getBucketName();
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
    }
}
