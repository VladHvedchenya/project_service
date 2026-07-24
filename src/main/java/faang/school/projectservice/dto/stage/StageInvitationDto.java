package faang.school.projectservice.dto.stage;

public record StageInvitationDto(
        Long invitationId,
        Long stageId,
        Long invitedUserId,
        String description,
        String status
) {
}