package faang.school.projectservice.service.stage;

import faang.school.projectservice.dto.stage.CreateStageDto;
import faang.school.projectservice.dto.stage.StageDto;
import faang.school.projectservice.dto.stage.UpdateStageDto;
import faang.school.projectservice.model.TaskStatus;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.DeleteStageStrategy;

import java.util.List;

public interface StageService {
    StageDto createStage(Long projectId, CreateStageDto createStageDto, Long userId);

    void deleteStage(Long projectId, Long stageId, DeleteStageStrategy deleteStageStrategy, Long userId);

    StageDto updateStage(Long projectId, Long stageId, UpdateStageDto updateStageDto, Long userId);

    List<StageDto> getStages(Long projectId, Long userId, List<TeamRole> roles, TaskStatus status);

    StageDto getStageById(Long projectId, Long stageId, Long userId);
}