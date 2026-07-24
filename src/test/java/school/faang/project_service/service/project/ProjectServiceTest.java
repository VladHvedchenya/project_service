package school.faang.project_service.service.project;

import faang.school.projectservice.dto.project.CreateProjectDto;
import faang.school.projectservice.dto.project.ProjectDto;
import faang.school.projectservice.dto.project.ProjectFilterDto;
import faang.school.projectservice.dto.project.UpdateProjectDto;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.exception.EntityNotFoundException;
import faang.school.projectservice.mapper.project.ProjectMapper;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.ProjectStatus;
import faang.school.projectservice.model.ProjectVisibility;
import faang.school.projectservice.repository.ProjectRepository;
import faang.school.projectservice.service.project.ProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Test
    public void testCreateProjectIfAlreadyExist() {
        Long userId = 1L;
        String name = "Java";
        when(projectRepository.existsByOwnerIdAndName(userId, "Java")).thenReturn(true);
        CreateProjectDto createProjectDto = new CreateProjectDto();
        createProjectDto.setName(name);

        assertThrows(
                DataValidationException.class,
                () -> projectService.createProject(createProjectDto, userId));
        verify(projectRepository).existsByOwnerIdAndName(userId, "Java");
        verify(projectRepository, never()).save(any());
        verifyNoInteractions(projectMapper);
    }

    @Test
    public void testCreateProjectIfUnique() {
        Long userId = 1L;

        CreateProjectDto createProjectDto = new CreateProjectDto();
        createProjectDto.setName("Java");

        Project project = new Project();
        project.setName("Java");
        project.setOwnerId(1L);

        Project savedProject = new Project();
        project.setId(1L);
        project.setOwnerId(1L);
        project.setName("Java");

        ProjectDto expectedDto = new ProjectDto(
                1L, "Java",
                null,
                userId, null,
                null,
                null,
                null);
        when(projectRepository.existsByOwnerIdAndName(userId, "Java")).thenReturn(false);
        when(projectMapper.toProject(createProjectDto)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(savedProject);
        when(projectMapper.toProjectDto(savedProject)).thenReturn(expectedDto);

        ProjectDto projectDto = projectService.createProject(createProjectDto, userId);

        assertEquals(expectedDto, projectDto);
        assertEquals(expectedDto.name(), projectDto.name());
        assertEquals(expectedDto.ownerId(), projectDto.ownerId());

        verify(projectRepository).existsByOwnerIdAndName(userId, "Java");
        verify(projectMapper).toProject(createProjectDto);
        verify(projectRepository).save(project);
        verify(projectMapper).toProjectDto(savedProject);
    }

    @Test
    public void testUpdateProjectWithNoSuchEntity() {
        when(projectRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.updateProject(new UpdateProjectDto(), 1L, 1L)
        );
        verifyNoInteractions(projectMapper);
        verify(projectRepository, never()).save(any());
    }

    @Test
    public void testUpdateProjectWithNoRoots() {
        Project project = new Project();
        project.setId(1L);
        project.setOwnerId(10L);
        Long userId = 1L;
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(project));

        assertThrows(
                AccessDeniedException.class,
                () -> projectService.updateProject(new UpdateProjectDto(), 1L, userId)
        );
    }

    @Test
    public void testUpdateProjectWithRoots() {
        Long userId = 1L;
        Project project = new Project();
        project.setName("Java");
        project.setId(1L);
        project.setOwnerId(userId);

        UpdateProjectDto updateProjectDto = new UpdateProjectDto();
        updateProjectDto.setDescription("Java Language");

        ProjectDto expected = new ProjectDto(
                1L,
                "Java",
                "Java Language",
                1L,
                null,
                null,
                null,
                null
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toProjectDto(project)).thenReturn(expected);

        ProjectDto result = projectService.updateProject(updateProjectDto, 1L, 1L);

        assertEquals(expected, result);
        assertEquals(expected.name(), result.name());
        assertEquals(expected.id(), result.id());
        assertEquals(expected.ownerId(), result.ownerId());
        assertEquals(expected.description(), result.description());

        verify(projectRepository).findById(1L);
        verify(projectMapper).updateProjectFromDto(updateProjectDto, project);
        verify(projectRepository).save(project);
        verify(projectMapper).toProjectDto(project);
    }

    @Test
    public void getAllProjects() {
        Long userId = 1L;
        Project first = new Project();
        first.setOwnerId(1L);
        first.setId(1L);
        Project second = new Project();
        second.setOwnerId(1L);
        second.setId(2L);
        List<Project> projects = List.of(first, second);

        ProjectDto firstDto = new ProjectDto(
                1L,
                null,
                null,
                1L,
                null,
                null,
                null,
                null
        );
        ProjectDto secondDto = new ProjectDto(
                2L,
                null,
                null,
                1L,
                null,
                null,
                null,
                null
        );
        List<ProjectDto> expected = List.of(firstDto, secondDto);
        when(projectRepository.findAccessibleProjects(userId, ProjectVisibility.PUBLIC)).thenReturn(projects);
        when(projectMapper.toProjectDto(first)).thenReturn(firstDto);
        when(projectMapper.toProjectDto(second)).thenReturn(secondDto);

        List<ProjectDto> result = projectService.getAllProjects(userId);

        assertEquals(expected.size(), result.size());

        verify(projectRepository).findAccessibleProjects(1L, ProjectVisibility.PUBLIC);
        verify(projectMapper).toProjectDto(first);
        verify(projectMapper).toProjectDto(second);
    }

    @Test
    public void testGetProjectByIdWithNoSuchEntity(){
        when(projectRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.getProjectById(1L, 1L)
        );
    }

    @Test
    public void testGetProjectByIdWithNoRoots(){
        Project project = new Project();
        project.setOwnerId(10L);
        Long userId = 1L;

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(
                AccessDeniedException.class,
                () -> projectService.getProjectById(1L, userId)
        );
    }

    @Test
    public void testGetProjectByIdWithPrivateVisibility(){
        Project project = new Project();
        Long userId = 1L;
        project.setOwnerId(10L);
        project.setVisibility(ProjectVisibility.PRIVATE);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(
                AccessDeniedException.class,
                () -> projectService.getProjectById(1L, userId)
        );
    }

    @Test
    public void testGetFilteredProjectsWithNoFilter(){
        Project project = new Project();
        project.setName("Java");
        project.setId(1L);
        project.setOwnerId(1L);
        project.setVisibility(ProjectVisibility.PUBLIC);
        ProjectFilterDto projectFilterDto = new ProjectFilterDto(
                null,
                null
        );
        List<Project> projects = List.of(project);
        when(projectRepository.findAccessibleProjects(1L, ProjectVisibility.PUBLIC)).thenReturn(projects);

        List<ProjectDto> result = projectService.getFilteredProjects(1L, projectFilterDto);

        assertEquals(1, result.size());

        verify(projectRepository).findAccessibleProjects(1L, ProjectVisibility.PUBLIC);
        verify(projectMapper).toProjectDto(project);
    }

    @Test
    public void testGetFilteredProjectsWithNameFilter(){
        Project first = new Project();
        first.setName("Java");
        Project second = new Project();
        second.setName("Python");
        List<Project> projects = List.of(first, second);
        ProjectFilterDto projectFilterDto = new ProjectFilterDto(
                "J",
                null
        );
        ProjectDto projectDto = new ProjectDto(
                1L,
                "Java",
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(projectRepository.findAccessibleProjects(1L, ProjectVisibility.PUBLIC)).thenReturn(projects);
        when(projectMapper.toProjectDto(first)).thenReturn(projectDto);

        List<ProjectDto> result = projectService.getFilteredProjects(1L, projectFilterDto);

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).name());

        verify(projectRepository).findAccessibleProjects(1L, ProjectVisibility.PUBLIC);
        verify(projectMapper).toProjectDto(first);
    }

    @Test
    public void testGetFilteredProjectsWithStatusFilter(){
        Project first = new Project();
        first.setStatus(ProjectStatus.IN_PROGRESS);
        first.setName("Python Dev");
        Project second = new Project();
        second.setStatus(ProjectStatus.COMPLETED);
        second.setName("Java Dev");
        List<Project> projects = List.of(first, second);
        ProjectFilterDto projectFilterDto = new ProjectFilterDto(
                null,
                ProjectStatus.COMPLETED
        );
        ProjectDto projectDto = new ProjectDto(
                1L,
                "Java Dev",
                null,
                null,
                ProjectStatus.COMPLETED,
                null,
                null,
                null
        );
        when(projectRepository.findAccessibleProjects(1L, ProjectVisibility.PUBLIC)).thenReturn(projects);
        when(projectMapper.toProjectDto(second)).thenReturn(projectDto);

        List<ProjectDto> result = projectService.getFilteredProjects(1L, projectFilterDto);

        assertEquals(1, result.size());
        assertEquals("Java Dev", result.get(0).name());

        verify(projectRepository).findAccessibleProjects(1L, ProjectVisibility.PUBLIC);
        verify(projectMapper).toProjectDto(second);
    }
}