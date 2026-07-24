package faang.school.projectservice.controller.stage;

import faang.school.projectservice.service.stage.StageInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("stages/invitations")
@RequiredArgsConstructor
@Slf4j
public class StageInvitationController {
    private final StageInvitationService stageInvitationService;

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable Long invitationId,
            @RequestHeader("x-user-id") Long userId) {
        log.info("Получен запрос на ПРИНЯТИЕ инвайта ID: {} от пользователя ID: {}", invitationId, userId);
        stageInvitationService.acceptInvitation(userId, invitationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvitation(
            @PathVariable Long invitationId,
            @RequestHeader("x-user-id") Long userId
    ) {
        log.info("Получен запрос на ОТКЛОНЕНИЕ инвайта ID: {} от пользователя ID: {}", invitationId, userId);
        stageInvitationService.rejectInvitation(userId, invitationId);
        return ResponseEntity.ok().build();
    }
}