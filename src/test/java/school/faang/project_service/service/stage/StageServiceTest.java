package school.faang.project_service.service.stage;

import faang.school.projectservice.dto.stage.CreateStageDto;
import faang.school.projectservice.dto.stage.RoleRequirementDto;
import faang.school.projectservice.dto.stage.StageDto;
import faang.school.projectservice.dto.stage.UpdateStageDto;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.exception.EntityNotFoundException;
import faang.school.projectservice.mapper.stage.StageMapper;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.Task;
import faang.school.projectservice.model.TaskStatus;
import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.DeleteStageStrategy;
import faang.school.projectservice.model.stage.Stage;
import faang.school.projectservice.model.stage.StageRole;
import faang.school.projectservice.repository.ProjectRepository;
import faang.school.projectservice.repository.StageRepository;
import faang.school.projectservice.repository.TeamMemberRepository;
import faang.school.projectservice.service.stage.StageInvitationServiceImpl;
import faang.school.projectservice.service.stage.StageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StageServiceTest {
    @InjectMocks
    private StageServiceImpl stageService;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private StageMapper stageMapper;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private StageInvitationServiceImpl stageInvitationService;

    @Test
    public void testCreateStageIfUserIsNotProjectMember() {
        Long projectId = 1L;
        Long userId = 1L;
        CreateStageDto dto = new CreateStageDto("All", null);
        Project project = new Project();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.empty());

        assertThrows(
                DataValidationException.class,
                () -> stageService.createStage(projectId, dto, userId)
        );

        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verify(projectRepository).findById(projectId);
        verifyNoInteractions(stageMapper, stageInvitationService);
    }

    @Test
    public void testCreateStageIfUserHasNoPermission() {
        Long projectId = 1L;
        Long userId = 2L;
        CreateStageDto dto = new CreateStageDto("All", null);
        TeamMember member = new TeamMember();
        member.setRoles(List.of(TeamRole.INTERN));
        Project project = new Project();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(member));

        assertThrows(
                DataValidationException.class,
                () -> stageService.createStage(projectId, dto, userId)
        );

        verify(projectRepository).findById(projectId);
        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verifyNoInteractions(stageInvitationService, stageMapper, stageRepository);
    }

    @Test
    public void testCreateProjectWithNoProjectEntity() {
        Long projectId = 1L;
        Long userId = 2L;
        CreateStageDto dto = new CreateStageDto("All", null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> stageService.createStage(projectId, dto, userId)
        );

        verifyNoInteractions(stageInvitationService, stageMapper, stageRepository);
    }

    @Test
    public void testCreateStageSuccessfully() {
        Long projectId = 1L;
        Long userId = 2L;
        Project project = new Project();
        project.setId(projectId);
        TeamMember member = new TeamMember();
        member.setRoles(List.of(TeamRole.MANAGER));
        CreateStageDto createDto = new CreateStageDto("Разработка", List.of());
        StageDto expectedResponseDto =
                new StageDto(10L, "Разработка", 1L, List.of(), List.of(), List.of());
        Stage saved = new Stage();
        saved.setStageId(10L);
        saved.setProject(project);
        saved.setStageName("Разработка");
        saved.setStageRoles(List.of());
        saved.setExecutors(List.of());
        saved.setTasks(List.of());
        Stage stage = new Stage();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.save(any(Stage.class))).thenReturn(saved);
        when(stageMapper.toStageDto(saved)).thenReturn(expectedResponseDto);

        StageDto result = stageService.createStage(projectId, createDto, userId);

        assertNotNull(result);
        assertEquals(10L, result.stageId());
        assertEquals("Разработка", result.stageName());

        verify(projectRepository).findById(projectId);
        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verify(stageRepository).save(any(Stage.class));
        verify(stageMapper).toStageDto(saved);
        verifyNoInteractions(stageInvitationService);
    }

    @Test
    public void testGetStagesWithNoFilers() {
        Long projectId = 2L;
        Long userId = 10L;
        Stage firstStage = new Stage();
        firstStage.setStageId(3L);
        Stage secondStage = new Stage();
        secondStage.setStageId(8L);
        List<Stage> stages = List.of(firstStage, secondStage);
        StageDto firstDto
                = new StageDto(3L, "Stage1", projectId, List.of(), List.of(), List.of());
        StageDto secondDto
                = new StageDto(8L, "Stage2", projectId, List.of(), List.of(), List.of());
        TeamMember member = new TeamMember();
        member.setRoles(List.of(TeamRole.MANAGER));

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(stages);
        when(stageMapper.toStageDto(firstStage)).thenReturn(firstDto);
        when(stageMapper.toStageDto(secondStage)).thenReturn(secondDto);

        List<StageDto> result = stageService.getStages(projectId, userId, null, null);

        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).stageId());
        assertEquals(8L, result.get(1).stageId());

        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verify(stageRepository).findAllByProjectIdWithRolesAndExecutors(projectId);
        verify(stageMapper).toStageDto(firstStage);
        verify(stageMapper).toStageDto(secondStage);
        verifyNoInteractions(stageInvitationService);
    }

    @Test
    public void testGetStagesWithRoleFilter() {
        Long projectId = 10L;
        Long userId = 9L;
        TeamMember member = new TeamMember();

        StageRole firstRole = new StageRole();
        firstRole.setTeamRole(TeamRole.MANAGER);
        Stage firstStage = new Stage();
        firstStage.setStageRoles(List.of(firstRole));
        StageRole secondRole = new StageRole();
        secondRole.setTeamRole(TeamRole.DESIGNER);
        Stage secondStage = new Stage();
        secondStage.setStageRoles(List.of(secondRole));

        List<Stage> stages = List.of(firstStage, secondStage);

        StageDto dto = new StageDto(
                10L,
                "Разработка",
                projectId,
                List.of(),
                List.of(),
                List.of()
        );

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(stages);
        when(stageMapper.toStageDto(secondStage)).thenReturn(dto);

        List<StageDto> result = stageService.getStages(projectId, userId, List.of(TeamRole.DESIGNER), null);

        assertEquals(1, result.size());
        assertEquals("Разработка", result.get(0).stageName());
        assertEquals(10L, result.get(0).stageId());

        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verify(stageRepository).findAllByProjectIdWithRolesAndExecutors(projectId);
        verify(stageMapper).toStageDto(secondStage);
        verifyNoInteractions(stageInvitationService);
    }

    @Test
    public void testGetStagesWithStatusFilter() {
        Long projectId = 8L;
        Long userId = 3L;
        TaskStatus status = TaskStatus.DONE;
        TeamMember member = new TeamMember();

        Stage firstStage = new Stage();
        Task task1 = new Task();
        task1.setStatus(TaskStatus.IN_PROGRESS);
        firstStage.setTasks(List.of(task1));
        Stage secondStage = new Stage();
        Task task2 = new Task();
        task2.setStatus(TaskStatus.DONE);
        secondStage.setTasks(List.of(task2));

        List<Stage> stages = List.of(firstStage, secondStage);

        StageDto dto = new StageDto(
                1L,
                "Разработка",
                projectId,
                List.of(),
                List.of(),
                List.of()
        );

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(stages);
        when(stageMapper.toStageDto(secondStage)).thenReturn(dto);


        List<StageDto> result = stageService.getStages(projectId, userId, null, status);

        assertEquals(1, result.size());
        assertEquals("Разработка", result.get(0).stageName());
        assertEquals(1L, result.get(0).stageId());

        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verify(stageRepository).findAllByProjectIdWithRolesAndExecutors(projectId);
        verify(stageMapper).toStageDto(secondStage);
        verifyNoInteractions(stageInvitationService);
    }

    @Test
    @DisplayName("Удаление этапа: ошибка, если этап принадлежит другому проекту")
    public void testDeleteStageWithIncorrectProjectId() {
        Long projectId = 1L;
        Long wrongProjectId = 99L;
        Long stageId = 10L;
        Long userId = 2L;

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Project foreignProject = new Project();
        foreignProject.setId(wrongProjectId);

        Stage stage = new Stage();
        stage.setStageId(stageId);
        stage.setProject(foreignProject);

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));

        assertThrows(
                DataValidationException.class,
                () -> stageService.deleteStage(projectId, stageId, DeleteStageStrategy.DELETE_WITH_TASKS, userId)
        );

        verify(stageRepository, never()).delete(any(Stage.class));
        verifyNoInteractions(stageInvitationService, stageMapper);
    }

    @Test
    @DisplayName("Успешное удаление этапа со стратегией CASCADE (задачи очищаются, этап удаляется)")
    public void testDeleteStageWithTasks() {
        Long projectId = 1L;
        Long stageId = 10L;
        Long userId = 2L;

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Stage stage = new Stage();
        stage.setStageId(stageId);
        stage.setProject(project);
        stage.setTasks(new ArrayList<>(List.of(new Task(), new Task())));

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));

        stageService.deleteStage(projectId, stageId, DeleteStageStrategy.DELETE_WITH_TASKS, userId);

        assertTrue(stage.getTasks().isEmpty(), "Задачи удаляемого этапа должны быть полностью очищены");
        verify(stageRepository).delete(stage);
        verifyNoInteractions(stageInvitationService, stageMapper);
    }

    @Test
    @DisplayName("Успешное удаление этапа: стратегия MOVE_TASKS_TO_NEXT переносит задачи на ближайший следующий этап")
    public void testDeleteStageWithMovingTasksToTheNextStage() {
        Long projectId = 1L;
        Long userId = 2L;
        Long currentStageId = 10L;
        Long nextStageId = 20L;
        Long farStageId = 30L;

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Task task1 = new Task();
        Task task2 = new Task();

        Stage currentStage = new Stage();
        currentStage.setStageId(currentStageId);
        currentStage.setProject(project);
        currentStage.setTasks(new ArrayList<>(List.of(task1, task2)));

        Stage nextStage = new Stage();
        nextStage.setStageId(nextStageId);
        nextStage.setProject(project);
        nextStage.setTasks(new ArrayList<>());

        Stage farStage = new Stage();
        farStage.setStageId(farStageId);
        farStage.setProject(project);
        farStage.setTasks(new ArrayList<>());

        List<Stage> allStages = List.of(farStage, currentStage, nextStage);

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(currentStageId)).thenReturn(Optional.of(currentStage));
        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(allStages);

        stageService.deleteStage(projectId, currentStageId, DeleteStageStrategy.MOVE_TASKS_TO_NEXT, userId);

        assertEquals(2, nextStage.getTasks().size(), "Ближайший следующий этап (ID 20) должен перенять 2 задачи");
        assertTrue(farStage.getTasks().isEmpty(), "Далекий этап (ID 30) должен остаться пустым");
        assertTrue(currentStage.getTasks().isEmpty(), "Удаляемый этап должен остаться без задач");
        verify(stageRepository).delete(currentStage);
    }

    @Test
    @DisplayName("Удаление этапа: ошибка MOVE_TASKS_TO_NEXT, если следующего этапа не существует")
    public void testDeleteStageWithMovingTaskToTheNextStageWithNoNextStage() {
        Long projectId = 1L;
        Long userId = 2L;
        Long currentStageId = 30L;

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Stage currentStage = new Stage();
        currentStage.setStageId(currentStageId);
        currentStage.setProject(project);
        currentStage.setTasks(new ArrayList<>(List.of(new Task())));

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(currentStageId)).thenReturn(Optional.of(currentStage));
        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(List.of(currentStage));

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> stageService.deleteStage(projectId, currentStageId, DeleteStageStrategy.MOVE_TASKS_TO_NEXT, userId)
        );

        assertTrue(exception.getMessage().contains("next"));
        verify(stageRepository, never()).delete(any(Stage.class));
    }

    @Test
    @DisplayName("Успешное удаление этапа: стратегия MOVE_TASKS_TO_PREVIOUS переносит задачи на ближайший предыдущий этап")
    public void testDeleteStageWithMovingTasksToThePrevious() {
        Long projectId = 1L;
        Long userId = 2L;
        Long oldStageId = 10L;
        Long previousStageId = 20L;
        Long currentStageId = 30L;

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Task task1 = new Task();

        Stage oldStage = new Stage();
        oldStage.setStageId(oldStageId);
        oldStage.setProject(project);
        oldStage.setTasks(new ArrayList<>());

        Stage previousStage = new Stage();
        previousStage.setStageId(previousStageId);
        previousStage.setProject(project);
        previousStage.setTasks(new ArrayList<>());

        Stage currentStage = new Stage();
        currentStage.setStageId(currentStageId);
        currentStage.setProject(project);
        currentStage.setTasks(new ArrayList<>(List.of(task1)));

        List<Stage> allStages = List.of(oldStage, currentStage, previousStage);

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(currentStageId)).thenReturn(Optional.of(currentStage));
        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(allStages);

        stageService.deleteStage(projectId, currentStageId, DeleteStageStrategy.MOVE_TASKS_TO_PREVIOUS, userId);

        assertEquals(1, previousStage.getTasks().size(), "Ближайший предыдущий этап (ID 20) должен забрать задачу");
        assertTrue(oldStage.getTasks().isEmpty(), "Далекий этап (ID 10) должен остаться пустым");
        assertTrue(currentStage.getTasks().isEmpty(), "Задачи удаляемого этапа должны быть очищены");
        verify(stageRepository).delete(currentStage);
    }

    @Test
    @DisplayName("Удаление этапа: ошибка MOVE_TASKS_TO_PREVIOUS, если предыдущего этапа не существует")
    public void testDeleteStageWithMovingTasksWithNoPrevious() {
        Long projectId = 1L;
        Long userId = 2L;
        Long currentStageId = 10L;

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Stage currentStage = new Stage();
        currentStage.setStageId(currentStageId);
        currentStage.setProject(project);
        currentStage.setTasks(new ArrayList<>(List.of(new Task())));

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(currentStageId)).thenReturn(Optional.of(currentStage));

        when(stageRepository.findAllByProjectIdWithRolesAndExecutors(projectId)).thenReturn(List.of(currentStage));

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> stageService.deleteStage(projectId, currentStageId, DeleteStageStrategy.MOVE_TASKS_TO_PREVIOUS, userId)
        );

        assertTrue(exception.getMessage().contains("previous"), "Сообщение об ошибке должно содержать слово 'previous'");

        verify(stageRepository, never()).delete(any(Stage.class));
    }

    @Test
    @DisplayName("Обновление этапа: ошибка, если этап не найден в базе данных")
    public void testUpdateStage_StageNotFound_ThrowsEntityNotFoundException() {
        Long projectId = 1L;
        Long stageId = 99L;
        Long userId = 2L;
        UpdateStageDto updateDto = new UpdateStageDto("Новое имя", List.of(), List.of());

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(stageId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> stageService.updateStage(projectId, stageId, updateDto, userId)
        );

        verifyNoInteractions(stageMapper, stageInvitationService);
    }

    @Test
    @DisplayName("Обновление этапа: ошибка, если этап привязан к другому проекту")
    public void testUpdateStage_WrongProject_ThrowsDataValidationException() {
        Long projectId = 1L;
        Long stageId = 10L;
        Long userId = 2L;
        UpdateStageDto updateDto = new UpdateStageDto("Новое имя", List.of(), List.of());

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Project foreignProject = new Project();
        foreignProject.setId(99L); // Чужой проект

        Stage stage = new Stage();
        stage.setStageId(stageId);
        stage.setProject(foreignProject);

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));

        assertThrows(
                DataValidationException.class,
                () -> stageService.updateStage(projectId, stageId, updateDto, userId)
        );

        verifyNoInteractions(stageMapper, stageInvitationService);
    }

    @Test
    @DisplayName("Успешное частичное обновление этапа (изменение только имени)")
    public void testUpdateStage_OnlyNameUpdated_Success() {
        Long projectId = 1L;
        Long stageId = 10L;
        Long userId = 2L;

        UpdateStageDto updateDto = new UpdateStageDto("Только новое имя", null, null);

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Stage stage = new Stage();
        stage.setStageId(stageId);
        stage.setProject(project);
        stage.setStageName("Старое имя");

        List<StageRole> oldRoles = new ArrayList<>(List.of(new StageRole()));
        List<TeamMember> oldExecutors = new ArrayList<>(List.of(new TeamMember()));
        stage.setStageRoles(oldRoles);
        stage.setExecutors(oldExecutors);

        StageDto expectedResponseDto = new StageDto(stageId, "Только новое имя", projectId, List.of(), List.of(), List.of());

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(stageMapper.toStageDto(stage)).thenReturn(expectedResponseDto);

        StageDto result = stageService.updateStage(projectId, stageId, updateDto, userId);

        assertNotNull(result);
        assertEquals("Только новое имя", stage.getStageName(), "Имя этапа должно измениться");
        assertEquals(1, stage.getStageRoles().size(), "Список ролей не должен был обнулиться или измениться");
        assertEquals(1, stage.getExecutors().size(), "Список исполнителей не должен был измениться");

        verify(stageInvitationService).processInvitations(stage, userId);
        verify(stageMapper).toStageDto(stage);
    }

    @Test
    @DisplayName("Успешное полное обновление этапа (имя, роли и участники)")
    public void testUpdateStage_FullUpdate_Success() {
        Long projectId = 1L;
        Long stageId = 10L;
        Long userId = 2L;

        RoleRequirementDto roleReq = new RoleRequirementDto(TeamRole.DEVELOPER, 3);
        UpdateStageDto updateDto = new UpdateStageDto("Финальная разработка", List.of(30L, 40L), List.of(roleReq));

        Project project = new Project();
        project.setId(projectId);

        TeamMember manager = new TeamMember();
        manager.setRoles(List.of(TeamRole.MANAGER));

        Stage stage = new Stage();
        stage.setStageId(stageId);
        stage.setProject(project);
        stage.setStageName("Старое название");
        stage.setStageRoles(new ArrayList<>());
        stage.setExecutors(new ArrayList<>());

        TeamMember dev1 = new TeamMember();
        dev1.setId(30L);
        TeamMember dev2 = new TeamMember();
        dev2.setId(40L);
        List<TeamMember> mappedMembers = List.of(dev1, dev2);

        StageDto expectedResponseDto = new StageDto(stageId, "Финальная разработка", projectId, List.of(), List.of(), List.of());

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(manager));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(teamMemberRepository.findAllById(List.of(30L, 40L))).thenReturn(mappedMembers);
        when(stageMapper.toStageDto(stage)).thenReturn(expectedResponseDto);

        StageDto result = stageService.updateStage(projectId, stageId, updateDto, userId);

        assertNotNull(result);
        assertEquals("Финальная разработка", stage.getStageName());

        assertEquals(1, stage.getStageRoles().size(), "Должна добавиться одна новая роль");
        assertEquals(TeamRole.DEVELOPER, stage.getStageRoles().get(0).getTeamRole());
        assertEquals(3, stage.getStageRoles().get(0).getCount());

        assertEquals(2, stage.getExecutors().size(), "В исполнители должны были добавиться 2 участника");

        verify(teamMemberRepository).findAllById(List.of(30L, 40L));
        verify(stageInvitationService).processInvitations(stage, userId);
        verify(stageMapper).toStageDto(stage);
    }

    @Test
    @DisplayName("Получение этапа по ID: ошибка, если этап не найден в базе данных")
    public void testGetStageById_StageNotFound_ThrowsEntityNotFoundException() {
        Long projectId = 1L;
        Long userId = 2L;
        Long wrongStageId = 99L;

        TeamMember member = new TeamMember();
        member.setRoles(List.of(TeamRole.DEVELOPER));

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.findById(wrongStageId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> stageService.getStageById(projectId, userId, wrongStageId)
        );

        verifyNoInteractions(stageMapper, stageInvitationService);
    }

    @Test
    @DisplayName("Получение этапа по ID: ошибка, если этап привязан к другому проекту")
    public void testGetStageById_WrongProject_ThrowsDataValidationException() {
        Long projectId = 1L;
        Long userId = 2L;
        Long stageId = 10L;

        TeamMember member = new TeamMember();
        member.setRoles(List.of(TeamRole.DEVELOPER));

        Project foreignProject = new Project();
        foreignProject.setId(99L);

        Stage stageInDb = new Stage();
        stageInDb.setStageId(stageId);
        stageInDb.setProject(foreignProject);

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stageInDb));

        assertThrows(
                DataValidationException.class,
                () -> stageService.getStageById(projectId, userId, stageId)
        );

        verifyNoInteractions(stageMapper, stageInvitationService);
    }

    @Test
    @DisplayName("Успешное получение этапа по ID (Happy Path)")
    public void testGetStageById_Success() {
        Long projectId = 1L;
        Long userId = 2L;
        Long stageId = 10L;

        TeamMember member = new TeamMember();
        member.setRoles(List.of(TeamRole.DEVELOPER));

        Project project = new Project();
        project.setId(projectId);

        Stage stageInDb = new Stage();
        stageInDb.setStageId(stageId);
        stageInDb.setProject(project);
        stageInDb.setStageName("Тестирование");

        StageDto expectedDto = new StageDto(stageId, "Тестирование", projectId, List.of(), List.of(), List.of());

        when(teamMemberRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(member));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stageInDb));
        when(stageMapper.toStageDto(stageInDb)).thenReturn(expectedDto);

        StageDto result = stageService.getStageById(projectId, userId, stageId);

        assertNotNull(result);
        assertEquals(stageId, result.stageId());
        assertEquals("Тестирование", result.stageName());

        verify(teamMemberRepository).findByUserIdAndProjectId(userId, projectId);
        verify(stageRepository).findById(stageId);
        verify(stageMapper).toStageDto(stageInDb);
        verifyNoInteractions(stageInvitationService);
    }

    @Test
    void testPipelineShouldFail() {
        org.junit.jupiter.api.Assertions.fail("Специальный падающий тест для проверки CI пайплайна!");
    }
}