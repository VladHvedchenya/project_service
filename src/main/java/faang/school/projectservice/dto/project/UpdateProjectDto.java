package faang.school.projectservice.dto.project;

import faang.school.projectservice.model.ProjectStatus;
import faang.school.projectservice.model.ProjectVisibility;
import lombok.Data;

@Data
public class UpdateProjectDto {
    private String description;
    private ProjectVisibility visibility;
    private ProjectStatus status;
}