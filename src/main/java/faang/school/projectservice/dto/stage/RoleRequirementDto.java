package faang.school.projectservice.dto.stage;

import faang.school.projectservice.model.TeamRole;

public record RoleRequirementDto(
        TeamRole teamRole,
        Integer count
) {
}