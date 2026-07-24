package faang.school.projectservice.controller.stage;

import faang.school.projectservice.dto.stage.CreateStageDto;
import faang.school.projectservice.dto.stage.StageDto;
import faang.school.projectservice.dto.stage.UpdateStageDto;
import faang.school.projectservice.model.TaskStatus;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.DeleteStageStrategy;
import faang.school.projectservice.service.stage.StageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @PostMapping
    public StageDto createStage(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateStageDto createStageDto,
            @RequestHeader("x-user-id") Long userId
    ) {
        return stageService.createStage(projectId, createStageDto, userId);
    }

    @DeleteMapping("/{stageId}")
    public void deleteStage(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @RequestParam DeleteStageStrategy deleteStageStrategy,
            @RequestHeader("x-user-id") Long userId
    ) {
        stageService.deleteStage(projectId, stageId, deleteStageStrategy, userId);
    }

    @GetMapping
    public List<StageDto> getStages(
            @PathVariable Long projectId,
            @RequestHeader("x-user-id") Long userId,
            @RequestParam(required = false) List<TeamRole> roles,
            @RequestParam(required = false) TaskStatus status
    ) {
        return stageService.getStages(projectId, userId, roles, status);
    }

    @GetMapping("/{stageId}")
    public StageDto getStageById(
            @PathVariable Long projectId,
            @RequestHeader("x-user-id") Long userId,
            @PathVariable Long stageId
    ) {
        return stageService.getStageById(projectId, userId, stageId);
    }

    @PutMapping("/{stageId}")
    public StageDto updateStage(
            @Valid @RequestBody UpdateStageDto updateStageDto,
            @RequestHeader("x-user-id") Long userId,
            @PathVariable Long projectId,
            @PathVariable Long stageId
    ) {
        return stageService.updateStage(projectId, stageId, updateStageDto, userId);
    }
}