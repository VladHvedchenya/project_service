package faang.school.projectservice.dto.stage;

import java.util.List;

public record StageDto(
        Long stageId,
        String stageName,
        Long projectId,
        List<RoleRequirementDto> stageRoles,
        List<Long> executorIds,
        List<Long> taskIds
) {
}