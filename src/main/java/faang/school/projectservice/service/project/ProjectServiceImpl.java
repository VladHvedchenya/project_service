package faang.school.projectservice.service.project;

import faang.school.projectservice.dto.project.CreateProjectDto;
import faang.school.projectservice.dto.project.ProjectDto;
import faang.school.projectservice.dto.project.ProjectFilterDto;
import faang.school.projectservice.dto.project.UpdateProjectDto;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.exception.EntityNotFoundException;
import faang.school.projectservice.mapper.project.ProjectMapper;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.ProjectVisibility;
import faang.school.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectDto createProject(CreateProjectDto createProjectDto, Long userId) {
        log.info("Старт создания проекта с названием {}", createProjectDto.getName());

        if(projectRepository.existsByOwnerIdAndName(userId, createProjectDto.getName())){
            throw new DataValidationException("Проект с таким названием уже существует!");
        }

        Project project = projectMapper.toProject(createProjectDto);
        project.setOwnerId(userId);
        project = projectRepository.save(project);
        log.info("Успешно создан новый проект: [ID: {}, Title: {}]", project.getId(), project.getName());
        return projectMapper.toProjectDto(project);
    }

    @Transactional
    public ProjectDto updateProject(UpdateProjectDto updateProjectDto, Long projectId, Long userId) {
        log.info("Старт обновления проекта c id={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Проекта с таким id не существует!"));

        if (!Objects.equals(project.getOwnerId(), userId)){
            log.warn("Пользователь {} попытался изменить чужой проект {}", userId, projectId);
            throw new AccessDeniedException("Нет прав на изменение этого проекта");
        }
        String name = project.getName();
        projectMapper.updateProjectFromDto(updateProjectDto, project);
        project = projectRepository.save(project);
        log.info("Проект обновлён: id={}, name={}", projectId, name);
        return projectMapper.toProjectDto(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getFilteredProjects(Long userId, ProjectFilterDto projectFilterDto) {
        List<Project> projects = projectRepository.findAccessibleProjects(userId, ProjectVisibility.PUBLIC);

        return projects.stream()
                .filter(project -> projectFilterDto.namePattern() == null
                        || project.getName().contains(projectFilterDto.namePattern().toLowerCase()))
                .filter(project -> projectFilterDto.statusPattern() == null
                        || project.getStatus() == projectFilterDto.statusPattern())
                .map(projectMapper::toProjectDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects(Long userId) {
        log.info("Старт получения всех проектов пользователя и всех видимых проектов");
        List<Project> projects = projectRepository.findAccessibleProjects(userId, ProjectVisibility.PUBLIC);
        log.info("Найдено {} проектов для пользователя {}", projects.size(), userId);
        return projects.stream()
                .map(projectMapper::toProjectDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long projectId, Long userId) {
        log.info("Старт получения проекта по id c id={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Проекта с таким id не существует!"));

        boolean isOwner = Objects.equals(project.getOwnerId(), userId);
        boolean isVisible = project.getVisibility() == ProjectVisibility.PUBLIC;

        if (!isOwner && !isVisible){
            log.warn("Пользователь {} попытался получить не свой проект {}", userId, projectId);
            throw new AccessDeniedException("Нет прав на получение этого проекта");
        }

        log.info("Проект {} успешно получен пользователем {}", projectId, userId);

        return projectMapper.toProjectDto(project);
    }
}