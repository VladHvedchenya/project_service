package faang.school.projectservice.cron.stage;

import faang.school.projectservice.repository.StageInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class StageInvitationCronCleaner {
    private final StageInvitationRepository stageInvitationRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanRejectedInvitations() {
        log.info("Cron-робот Хронос запущен: Начинается ночная очистка отклоненных инвайтов...");

        int deletedCount = stageInvitationRepository.deleteAllRejectedInvitations();

        log.info("Ночная очистка успешно завершена. Из таблицы stage_invitation удалено: {} записей.", deletedCount);
    }
}