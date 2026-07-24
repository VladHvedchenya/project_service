package faang.school.projectservice.dto.stage;

import java.io.Serializable;

public record StageInvitationEvent(
        Long memberId,
        Long stageId,
        String role
) implements Serializable {
}