package school.faang.project_service.service.stage;

import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.exception.EntityNotFoundException;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.model.stage.Stage;
import faang.school.projectservice.model.stage.StageRole;
import faang.school.projectservice.model.stage_invitation.StageInvitation;
import faang.school.projectservice.model.stage_invitation.StageInvitationStatus;
import faang.school.projectservice.repository.StageInvitationRepository;
import faang.school.projectservice.repository.TeamMemberRepository;
import faang.school.projectservice.service.stage.StageInvitationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageInvitationServiceImplTest {

    @Mock
    private StageInvitationRepository stageInvitationRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private StageInvitationServiceImpl invitationService;

    @Test
    @DisplayName("createInvitation: Успешное создание инвайта (Happy Path)")
    public void testCreateInvitation_Success() {
        Long authorUserId = 1L;
        Long invitedMemberId = 2L;

        Stage stage = new Stage();
        stage.setStageName("Фронтенд");

        TeamMember author = new TeamMember();
        TeamMember invited = new TeamMember();

        when(teamMemberRepository.findById(authorUserId)).thenReturn(Optional.of(author));
        when(teamMemberRepository.findById(invitedMemberId)).thenReturn(Optional.of(invited));

        invitationService.createInvitation(stage, authorUserId, invitedMemberId);

        ArgumentCaptor<StageInvitation> captor = ArgumentCaptor.forClass(StageInvitation.class);
        verify(stageInvitationRepository).save(captor.capture());

        StageInvitation captured = captor.getValue();
        assertEquals(StageInvitationStatus.PENDING, captured.getStatus());
        assertEquals("Приглашение на этап: Фронтенд", captured.getDescription());
        assertEquals(author, captured.getAuthor());
        assertEquals(invited, captured.getInvited());
        assertEquals(stage, captured.getStage());
    }

    @Test
    @DisplayName("createInvitation: Ошибка, если автор не найден в БД")
    public void testCreateInvitation_AuthorNotFound_ThrowsException() {
        Long authorUserId = 1L;
        Long invitedMemberId = 2L;
        Stage stage = new Stage();

        when(teamMemberRepository.findById(authorUserId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> invitationService.createInvitation(stage, authorUserId, invitedMemberId)
        );

        verifyNoInteractions(stageInvitationRepository);
    }

    @Test
    @DisplayName("createInvitation: Ошибка, если участник не найден в БД")
    public void testCreateInvitation_MemberNotFound_ThrowsException() {
        Long authorUserId = 1L;
        Long invitedMemberId = 2L;
        Stage stage = new Stage();

        when(teamMemberRepository.findById(authorUserId)).thenReturn(Optional.of(new TeamMember()));
        when(teamMemberRepository.findById(invitedMemberId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> invitationService.createInvitation(stage, authorUserId, invitedMemberId)
        );

        verifyNoInteractions(stageInvitationRepository);
    }

    @Test
    @DisplayName("processInvitations: Успешный расчет и генерация инвайтов (Happy Path)")
    public void testProcessInvitations_Success() {
        Long authorId = 1L;
        Project project = new Project();
        project.setId(100L);

        Stage stage = new Stage();
        stage.setStageId(50L);
        stage.setStageName("Тесты");
        stage.setProject(project);
        stage.setExecutors(new ArrayList<>());

        StageRole requiredRole = new StageRole();
        requiredRole.setTeamRole(TeamRole.DEVELOPER);
        requiredRole.setCount(1); // Нужен 1 разработчик
        stage.setStageRoles(List.of(requiredRole));

        TeamMember author = new TeamMember();
        TeamMember candidate = new TeamMember();

        when(teamMemberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(teamMemberRepository.findMembersByProjectIdAndRoleExcludingStage(100L, TeamRole.DEVELOPER, 50L))
                .thenReturn(List.of(candidate));

        invitationService.processInvitations(stage, authorId);

        ArgumentCaptor<StageInvitation> captor = ArgumentCaptor.forClass(StageInvitation.class);
        verify(stageInvitationRepository, times(1)).save(captor.capture());

        StageInvitation generated = captor.getValue();
        assertEquals(StageInvitationStatus.PENDING, generated.getStatus());
        assertTrue(generated.getDescription().contains("DEVELOPER"));
        assertEquals(candidate, generated.getInvited());
    }

    @Test
    @DisplayName("processInvitations: Ошибка, если в проекте не хватает людей нужной роли")
    public void testProcessInvitations_NotEnoughMembers_ThrowsException() {
        Long authorId = 1L;
        Project project = new Project();
        project.setId(100L);

        Stage stage = new Stage();
        stage.setStageId(50L);
        stage.setProject(project);
        stage.setExecutors(new ArrayList<>());

        StageRole requiredRole = new StageRole();
        requiredRole.setTeamRole(TeamRole.DEVELOPER);
        requiredRole.setCount(1);
        stage.setStageRoles(List.of(requiredRole));

        TeamMember author = new TeamMember();

        when(teamMemberRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(teamMemberRepository.findMembersByProjectIdAndRoleExcludingStage(100L, TeamRole.DEVELOPER, 50L))
                .thenReturn(List.of());

        assertThrows(
                DataValidationException.class,
                () -> invitationService.processInvitations(stage, authorId)
        );

        verify(stageInvitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation: Успешное принятие инвайта и добавление в команду")
    public void testAcceptInvitation_Success() {
        Long userId = 77L;
        Long invitationId = 990L;

        TeamMember invitedMember = new TeamMember();
        invitedMember.setUserId(userId);

        Stage stage = new Stage();
        stage.setExecutors(new ArrayList<>());

        StageInvitation invitation = new StageInvitation();
        invitation.setStatus(StageInvitationStatus.PENDING);
        invitation.setInvited(invitedMember);
        invitation.setStage(stage);

        when(stageInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        invitationService.acceptInvitation(userId, invitationId);

        assertEquals(StageInvitationStatus.ACCEPTED, invitation.getStatus(), "Статус должен измениться на ACCEPTED");
        assertEquals(1, stage.getExecutors().size(), "Юзер должен добавиться в исполнители этапа");
        assertEquals(invitedMember, stage.getExecutors().get(0));
    }

    @Test
    @DisplayName("acceptInvitation: Ошибка безопасности, если чужой пользователь принимает инвайт")
    public void testAcceptInvitation_AccessForbidden_ThrowsException() {
        Long wrongUserId = 666L;
        Long invitationId = 990L;

        TeamMember invitedMember = new TeamMember();
        invitedMember.setUserId(77L);

        StageInvitation invitation = new StageInvitation();
        invitation.setInvited(invitedMember);

        when(stageInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThrows(
                DataValidationException.class,
                () -> invitationService.acceptInvitation(wrongUserId, invitationId)
        );
    }

    @Test
    @DisplayName("acceptInvitation: Ошибка, если инвайт уже был принят ранее")
    public void testAcceptInvitation_AlreadyAccepted_ThrowsException() {
        Long userId = 77L;
        Long invitationId = 990L;

        TeamMember invitedMember = new TeamMember();
        invitedMember.setUserId(userId);

        StageInvitation invitation = new StageInvitation();
        invitation.setInvited(invitedMember);
        invitation.setStatus(StageInvitationStatus.ACCEPTED);

        when(stageInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThrows(
                DataValidationException.class,
                () -> invitationService.acceptInvitation(userId, invitationId)
        );
    }

    @Test
    @DisplayName("rejectInvitation: Успешное отклонение инвайта")
    public void testRejectInvitation_Success() {
        Long userId = 77L;
        Long invitationId = 990L;

        TeamMember invitedMember = new TeamMember();
        invitedMember.setUserId(userId);

        StageInvitation invitation = new StageInvitation();
        invitation.setStatus(StageInvitationStatus.PENDING);
        invitation.setInvited(invitedMember);

        when(stageInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        invitationService.rejectInvitation(userId, invitationId);

        assertEquals(StageInvitationStatus.REJECTED, invitation.getStatus());
    }

    @Test
    @DisplayName("rejectInvitation: Ошибка безопасности, если чужой пользователь пытается отклонить инвайт")
    public void testRejectInvitation_AccessForbidden_ThrowsException() {
        Long wrongUserId = 666L;
        Long invitationId = 990L;

        TeamMember invitedMember = new TeamMember();
        invitedMember.setUserId(77L);

        StageInvitation invitation = new StageInvitation();
        invitation.setInvited(invitedMember);

        when(stageInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> invitationService.rejectInvitation(wrongUserId, invitationId)
        );

        assertEquals("Access forbidden", exception.getMessage());
    }

    @Test
    @DisplayName("rejectInvitation: Ошибка, если инвайт уже был отклонен ранее")
    public void testRejectInvitation_AlreadyRejected_ThrowsException() {
        Long userId = 77L;
        Long invitationId = 990L;

        TeamMember invitedMember = new TeamMember();
        invitedMember.setUserId(userId);

        StageInvitation invitation = new StageInvitation();
        invitation.setInvited(invitedMember);
        invitation.setStatus(StageInvitationStatus.REJECTED);

        when(stageInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> invitationService.rejectInvitation(userId, invitationId)
        );

        assertEquals("It's already rejected", exception.getMessage());
    }
}