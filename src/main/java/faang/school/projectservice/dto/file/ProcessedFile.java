package faang.school.projectservice.dto.file;

public record ProcessedFile(
        byte[] content,
        String contentType,
        String extension
) {
}
