package faang.school.projectservice.controller.stage;

import faang.school.projectservice.dto.stage.CreateStageDto;
import faang.school.projectservice.dto.stage.StageDto;
import faang.school.projectservice.dto.stage.UpdateStageDto;
import faang.school.projectservice.model.TaskStatus;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.DeleteStageStrategy;
import faang.school.projectservice.service.stage.StageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stage Management", description = "APIs for project stages, workflow steps, and sub-tasks organization")
public class StageController {

    private final StageService stageService;

    @PostMapping
    @Operation(
            summary = "Create a new project stage",
            description = "Creates a new workflow stage for a specific project." +
                    " Validates user permissions before creation."
    )
    public StageDto createStage(
            @Parameter(description = "ID of the project", required = true, example = "10")
            @PathVariable Long projectId,

            @Valid @RequestBody CreateStageDto createStageDto,

            @Parameter(name = "x-user-id",
                    in = ParameterIn.HEADER,
                    description = "ID of the authenticated user making the request", required = true, example = "1")
            @RequestHeader("x-user-id") Long userId
    ) {
        return stageService.createStage(projectId, createStageDto, userId);
    }

    @DeleteMapping("/{stageId}")
    @Operation(
            summary = "Delete a project stage",
            description = "Deletes an existing stage from the project using a specified strategy" +
                    " (e.g., cascade delete or task migration)."
    )
    public void deleteStage(
            @Parameter(description = "ID of the project", required = true, example = "10")
            @PathVariable Long projectId,

            @Parameter(description = "ID of the stage to delete", required = true, example = "5")
            @PathVariable Long stageId,

            @Parameter(description = "Strategy for handling remaining tasks in the stage",
                    required = true,
                    example = "CASCADE_DELETION")
            @RequestParam DeleteStageStrategy deleteStageStrategy,

            @Parameter(name = "x-user-id",
                    in = ParameterIn.HEADER,
                    description = "ID of the authenticated user making the request", required = true, example = "1")
            @RequestHeader("x-user-id") Long userId
    ) {
        stageService.deleteStage(projectId, stageId, deleteStageStrategy, userId);
    }

    @GetMapping
    @Operation(
            summary = "Get filtered stages of a project",
            description = "Retrieves a list of project stages," +
                    " filtered optionally by participant team roles and internal task statuses."
    )
    public List<StageDto> getStages(
            @Parameter(description = "ID of the project", required = true, example = "10")
            @PathVariable Long projectId,

            @Parameter(name = "x-user-id",
                    in = ParameterIn.HEADER,
                    description = "ID of the authenticated user making the request",
                    required = true,
                    example = "1")
            @RequestHeader("x-user-id") Long userId,

            @Parameter(description = "Filter stages by team member roles")
            @RequestParam(required = false) List<TeamRole> roles,

            @Parameter(description = "Filter stages by specific task statuses")
            @RequestParam(required = false) TaskStatus status
    ) {
        return stageService.getStages(projectId, userId, roles, status);
    }

    @GetMapping("/{stageId}")
    @Operation(
            summary = "Get project stage details by ID",
            description = "Fetches complete information for a specific stage belonging to a project," +
                    " including verification predicates."
    )
    public StageDto getStageById(
            @Parameter(description = "ID of the project", required = true, example = "10")
            @PathVariable Long projectId,

            @Parameter(name = "x-user-id",
                    in = ParameterIn.HEADER,
                    description = "ID of the authenticated user making the request",
                    required = true,
                    example = "1")
            @RequestHeader("x-user-id") Long userId,

            @Parameter(description = "ID of the specific stage", required = true, example = "5")
            @PathVariable Long stageId
    ) {
        return stageService.getStageById(projectId, userId, stageId);
    }

    @PutMapping("/{stageId}")
    @Operation(
            summary = "Update an existing project stage",
            description = "Updates configuration, required roles," +
                    " or details of a project stage. Applies validation constraints."
    )
    public StageDto updateStage(
            @Valid @RequestBody UpdateStageDto updateStageDto,

            @Parameter(name = "x-user-id",
                    in = ParameterIn.HEADER,
                    description = "ID of the authenticated user making the request",
                    required = true,
                    example = "1")
            @RequestHeader("x-user-id") Long userId,

            @Parameter(description = "ID of the project", required = true, example = "10")
            @PathVariable Long projectId,

            @Parameter(description = "ID of the stage to update", required = true, example = "5")
            @PathVariable Long stageId
    ) {
        return stageService.updateStage(projectId, stageId, updateStageDto, userId);
    }
}