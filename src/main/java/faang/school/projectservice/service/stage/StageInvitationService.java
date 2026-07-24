package faang.school.projectservice.service.stage;

import faang.school.projectservice.model.stage.Stage;

public interface StageInvitationService {
    void createInvitation(Stage stage, Long authorUserId, Long invitedMember);

    void processInvitations(Stage stage, Long authorId);

    void acceptInvitation(Long userId, Long invitationId);

    void rejectInvitation(Long userId, Long invitationId);
}