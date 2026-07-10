package faang.school.projectservice.dto.project;

import faang.school.projectservice.model.ProjectStatus;
import faang.school.projectservice.model.ProjectVisibility;

import java.time.LocalDateTime;

public record ProjectDto(
        Long id,
        String name,
        String description,
        Long ownerId,
        ProjectStatus status,
        ProjectVisibility visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}