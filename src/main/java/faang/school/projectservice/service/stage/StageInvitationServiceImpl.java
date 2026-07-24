package faang.school.projectservice.service.stage;

import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.exception.EntityNotFoundException;
import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.Stage;
import faang.school.projectservice.model.stage.StageRole;
import faang.school.projectservice.model.stage_invitation.StageInvitationStatus;
import faang.school.projectservice.repository.StageInvitationRepository;
import faang.school.projectservice.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import faang.school.projectservice.model.stage_invitation.StageInvitation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StageInvitationServiceImpl implements StageInvitationService {
    private final StageInvitationRepository stageInvitationRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public void createInvitation(Stage stage, Long authorUserId, Long invitedMember) {
        TeamMember author = teamMemberRepository.findById(authorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Author team member is not found"));
        TeamMember invited = teamMemberRepository.findById(invitedMember)
                .orElseThrow(() -> new EntityNotFoundException("Invited team member is not found"));
        StageInvitation stageInvitation = StageInvitation.builder()
                .stage(stage)
                .invited(invited)
                .author(author)
                .status(StageInvitationStatus.PENDING)
                .description("Приглашение на этап: " + stage.getStageName())
                .build();
        stageInvitationRepository.save(stageInvitation);
    }

    @Override
    public void processInvitations(Stage stage, Long authorId) {
        TeamMember author = teamMemberRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author is not found"));

        Map<TeamRole, Long> currentRoleCounts = stage.getExecutors().stream()
                .flatMap(executor -> executor.getRoles().stream())
                .collect(Collectors.groupingBy(role -> role, Collectors.counting()));

        for (StageRole stageRole : stage.getStageRoles()) {
            TeamRole role = stageRole.getTeamRole();
            Integer count = stageRole.getCount();

            Long existedCount = currentRoleCounts.getOrDefault(role, 0L);

            if (existedCount < count) {
                Long invitationsNeeded = count - existedCount;
                List<TeamMember> availableProjectMembers = teamMemberRepository
                        .findMembersByProjectIdAndRoleExcludingStage(stage.getProject().getId(), role, stage.getStageId());

                int invitationsSent = 0;

                for (TeamMember member : availableProjectMembers) {
                    if (invitationsSent >= invitationsNeeded) {
                        break;
                    }

                    StageInvitation invitation = StageInvitation.builder()
                            .stage(stage)
                            .author(author)
                            .invited(member)
                            .status(StageInvitationStatus.PENDING)
                            .description(String.format("Автоматическое приглашение на роль %s в этап %s", role, stage.getStageName()))
                            .build();

                    stageInvitationRepository.save(invitation);

                    invitationsSent++;
                }

                if (invitationsSent < invitationsNeeded) {
                    throw new DataValidationException(String.format(
                            "Not enough project members with role '%s' to fill the stage. Needed: %d, Sent invites: %d",
                            role, invitationsNeeded, invitationsSent
                    ));
                }
            }
        }
    }

    @Override
    public void acceptInvitation(Long userId, Long invitationId) {
        StageInvitation invitation = stageInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation is not found"));

        if (!invitation.getInvited().getUserId().equals(userId)) {
            throw new DataValidationException("Access forbidden");
        }

        if (invitation.getStatus() == StageInvitationStatus.ACCEPTED) {
            throw new DataValidationException("It's already accepted");
        }

        invitation.setStatus(StageInvitationStatus.ACCEPTED);

        Stage stage = invitation.getStage();

        if (!stage.getExecutors().contains(invitation.getInvited())) {
            stage.getExecutors().add(invitation.getInvited());
        }
    }

    @Override
    public void rejectInvitation(Long userId, Long invitationId) {
        StageInvitation invitation = stageInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation is not found"));

        if (!invitation.getInvited().getUserId().equals(userId)) {
            throw new DataValidationException("Access forbidden");
        }

        if (invitation.getStatus() == StageInvitationStatus.REJECTED) {
            throw new DataValidationException("It's already rejected");
        }

        invitation.setStatus(StageInvitationStatus.REJECTED);
    }
}