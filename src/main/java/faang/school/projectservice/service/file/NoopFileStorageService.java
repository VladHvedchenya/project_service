package faang.school.projectservice.service.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "services.s3", name = "isMocked", havingValue = "true")
public class NoopFileStorageService implements FileStorageService {
    @Override
    public String upload(byte[] content, String contentType, String extension, String keyPrefix) {
        return "%s/%s.%s".formatted(keyPrefix, UUID.randomUUID(), extension);
    }

    @Override
    public void delete(String fileKey) {
    }
}
