package faang.school.projectservice.service.file;

public interface FileStorageService {
    String upload(byte[] content, String contentType, String extension, String keyPrefix);

    void delete(String fileKey);
}
