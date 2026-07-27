package faang.school.projectservice.scheduler.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.projectservice.dto.stage.StageInvitationDto;
import faang.school.projectservice.dto.stage.StageInvitationEvent;
import faang.school.projectservice.model.stage_invitation.StageInvitation;
import faang.school.projectservice.model.stage_invitation.StageInvitationStatus;
import faang.school.projectservice.repository.StageInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StageInvitationOutboxScheduler {
    private final StageInvitationRepository stageInvitationRepository;
    private final RedisTemplate<String, StageInvitationEvent> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String INVITATION_CHANNEL = "stage_invitation_channel";

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void deliverPendingInvitations() {
        List<StageInvitation> invitations = stageInvitationRepository
                .findAllByStatus(StageInvitationStatus.PENDING, Pageable.ofSize(50));

        if (invitations.isEmpty()) {
            return;
        }

        log.info("Найдено {} инвайтов для отправки в Redis", invitations.size());

        for (var invitation : invitations) {
            try {
                StageInvitationDto invitationDto = new StageInvitationDto(
                        invitation.getId(),
                        invitation.getStage().getStageId(),
                        invitation.getInvited().getUserId(),
                        invitation.getDescription(),
                        invitation.getStatus().name()
                );

                String jsonMessage = objectMapper.writeValueAsString(invitationDto);
                redisTemplate.convertAndSend(INVITATION_CHANNEL, jsonMessage);
                invitation.setStatus(StageInvitationStatus.SENT);
                log.info("Инвайт ID {} успешно доставлен в Redis", invitation.getId());
            } catch (Exception e) {
                log.error("Критическая ошибка при отправке инвайта ID {} в Redis." +
                        " Он будет сохранен в БД для повторной отправки.", invitation.getId(), e);
            }
        }
    }
}