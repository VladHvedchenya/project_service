package faang.school.projectservice.service.stage;

import faang.school.projectservice.dto.stage.CreateStageDto;
import faang.school.projectservice.dto.stage.StageDto;
import faang.school.projectservice.dto.stage.UpdateStageDto;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.exception.EntityNotFoundException;
import faang.school.projectservice.mapper.stage.StageMapper;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.TaskStatus;
import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.DeleteStageStrategy;
import faang.school.projectservice.model.stage.Stage;
import faang.school.projectservice.model.stage.StageRole;
import faang.school.projectservice.repository.ProjectRepository;
import faang.school.projectservice.repository.StageRepository;
import faang.school.projectservice.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {
    private final StageRepository stageRepository;
    private final ProjectRepository projectRepository;
    private final StageMapper stageMapper;
    private final TeamMemberRepository teamMemberRepository;
    private final StageInvitationService stageInvitationService;

    @Override
    @Transactional
    public StageDto createStage(Long projectId, CreateStageDto createStageDto, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project is not found"));

        validateUserPermissions(projectId, userId);


        Stage stage = new Stage();
        stage.setStageName(createStageDto.stageName());
        stage.setProject(project);

        List<StageRole> roles = createStageDto.roleRequirementDto()
                .stream()
                .map(role -> {
                    StageRole stageRole = new StageRole();
                    stageRole.setTeamRole(role.teamRole());
                    stageRole.setCount(role.count());
                    stageRole.setStage(stage);
                    return stageRole;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        stage.setStageRoles(roles);
        stage.setTasks(new ArrayList<>());
        stage.setExecutors(new ArrayList<>());
        Stage saved = stageRepository.save(stage);

        return stageMapper.toStageDto(saved);
    }

    private void validateUserPermissions(Long projectId, Long userId) {
        TeamMember member = validateUserIsProjectMember(projectId, userId);

        boolean hasPermission = member.getRoles().stream()
                .anyMatch(role -> role == TeamRole.MANAGER || role == TeamRole.OWNER);

        if (!hasPermission) {
            throw new DataValidationException("This user has no permission for this action");
        }
    }

    private TeamMember validateUserIsProjectMember(Long projectId, Long userId) {
        return teamMemberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new DataValidationException("User is not a member of this project"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageDto> getStages(Long projectId, Long userId, List<TeamRole> roles, TaskStatus status) {
        validateUserIsProjectMember(projectId, userId);

        if ((roles == null || roles.isEmpty()) && status == null) {
            return getAllStages(projectId);
        }

        return getFilteredStages(projectId, roles, status);
    }

    private List<StageDto> getAllStages(Long projectId) {
        List<Stage> stages = stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId);

        return stages.stream()
                .map(stageMapper::toStageDto)
                .toList();
    }

    private List<StageDto> getFilteredStages(Long projectId, List<TeamRole> roles, TaskStatus status) {
        List<Stage> stages = stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId);

        return stages.stream()
                .filter(stage -> {
                    if (roles == null || roles.isEmpty()) {
                        return true;
                    }

                    return stage.getStageRoles().stream()
                            .anyMatch(stageRole -> roles.contains(stageRole.getTeamRole()));
                })
                .filter(stage -> {
                    if (status == null) {
                        return true;
                    }

                    if (status == TaskStatus.DONE) {
                        return stage.getTasks().stream()
                                .allMatch(task -> task.getStatus() == status);
                    }

                    return stage.getTasks().stream()
                            .anyMatch(task -> task.getStatus() == status);
                })
                .map(stageMapper::toStageDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteStage(Long projectId, Long stageId, DeleteStageStrategy deleteStageStrategy, Long userId) {
        validateUserPermissions(projectId, userId);
        Stage stage = stageRepository.findById(stageId).orElseThrow(
                () -> new EntityNotFoundException("Stage not found")
        );

        if (!stage.getProject().getId().equals(projectId)) {
            throw new DataValidationException("Stage does not belong to this project");
        }

        if (deleteStageStrategy == DeleteStageStrategy.MOVE_TASKS_TO_NEXT) {
            Stage nextStage = findNextStage(stage);
            nextStage.getTasks().addAll(stage.getTasks());
        } else if (deleteStageStrategy == DeleteStageStrategy.MOVE_TASKS_TO_PREVIOUS) {
            Stage previousStage = findPreviousStage(stage);
            previousStage.getTasks().addAll(stage.getTasks());
        }

        stage.getTasks().clear();
        stageRepository.delete(stage);
    }

    private Stage findNextStage(Stage currentStage) {
        return findNeighborStage(currentStage, false, s -> s.getStageId() > currentStage.getStageId(), "next");
    }

    private Stage findPreviousStage(Stage currentStage) {
        return findNeighborStage(currentStage, true, s -> s.getStageId() < currentStage.getStageId(), "previous");
    }

    private Stage findNeighborStage(Stage currentStage, boolean isReversed,
                                    Predicate<Stage> filterPredicate, String errorDirection) {

        List<Stage> stages = stageRepository.findAllByProjectIdWithRolesAndExecutors(currentStage.getProject().getId());

        Comparator<Stage> stageComparator = Comparator.comparing(Stage::getStageId);

        if (isReversed) {
            stageComparator = stageComparator.reversed();
        }

        return stages.stream()
                .sorted(stageComparator)
                .filter(filterPredicate)
                .findFirst()
                .orElseThrow(() -> new DataValidationException(
                        String.format("There is no %s stage in this project to move tasks to", errorDirection)
                ));
    }

    @Override
    @Transactional
    public StageDto updateStage(Long projectId, Long stageId, UpdateStageDto updateStageDto, Long userId) {
        validateUserPermissions(projectId, userId);

        Stage stage = stageRepository.findById(stageId).orElseThrow(
                () -> new EntityNotFoundException("Stage is not found")
        );

        if (!stage.getProject().getId().equals(projectId)) {
            throw new DataValidationException("Stage does not belong to this project");
        }

        if (updateStageDto.stageName() != null && !updateStageDto.stageName().isBlank()) {
            stage.setStageName(updateStageDto.stageName());
        }

        if (updateStageDto.roles() != null) {
            stage.getStageRoles().clear();
            List<StageRole> newRoles = updateStageDto.roles().stream()
                    .map(role ->
                            StageRole.builder()
                                    .stage(stage)
                                    .teamRole(role.teamRole())
                                    .count(role.count())
                                    .build()
                    )
                    .toList();
            stage.getStageRoles().addAll(newRoles);
        }

        if (updateStageDto.participantIds() != null) {
            List<TeamMember> members = teamMemberRepository.findAllById(updateStageDto.participantIds());
            stage.getExecutors().clear();
            stage.getExecutors().addAll(members);
        }

        stageInvitationService.processInvitations(stage, userId);

        return stageMapper.toStageDto(stage);
    }

    @Override
    @Transactional(readOnly = true)
    public StageDto getStageById(Long projectId, Long userId, Long stageId) {
        validateUserIsProjectMember(projectId, userId);
        Stage stage = stageRepository.findById(stageId).orElseThrow(
                () -> new EntityNotFoundException("Stage is not found")
        );

        if (!stage.getProject().getId().equals(projectId)) {
            throw new DataValidationException("Stage does not belong to the specified project");
        }

        return stageMapper.toStageDto(stage);
    }
}