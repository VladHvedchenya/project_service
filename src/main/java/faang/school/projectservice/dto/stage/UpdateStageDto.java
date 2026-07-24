package faang.school.projectservice.dto.stage;

import java.util.List;

public record UpdateStageDto(
        String stageName,
        List<Long> participantIds,
        List<RoleRequirementDto> roles
) {
}