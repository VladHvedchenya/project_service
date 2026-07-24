package faang.school.projectservice.dto.stage;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateStageDto(
        @NotBlank
        String stageName,
        List<RoleRequirementDto> roleRequirementDto
) {
}