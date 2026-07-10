package faang.school.projectservice.dto.project;

import faang.school.projectservice.model.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectDto {
    @NotBlank
    private String name;

    private String description;

    private ProjectVisibility visibility = ProjectVisibility.PRIVATE;
}