package faang.school.projectservice.service.project;

import faang.school.projectservice.dto.file.ProcessedFile;
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
import faang.school.projectservice.service.file.FileStorageService;
import faang.school.projectservice.service.file.ProjectCoverImageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectCoverImageProcessor coverImageProcessor;
    private final FileStorageService fileStorageService;

    @Transactional
    public ProjectDto createProject(CreateProjectDto createProjectDto, Long userId) {
        log.info("Creating project with name={}", createProjectDto.getName());

        if (projectRepository.existsByOwnerIdAndName(userId, createProjectDto.getName())) {
            throw new DataValidationException("Project with this name already exists");
        }

        Project project = projectMapper.toProject(createProjectDto);
        project.setOwnerId(userId);
        project = projectRepository.save(project);
        log.info("Project created successfully: id={}, name={}", project.getId(), project.getName());
        return projectMapper.toProjectDto(project);
    }

    @Transactional
    public ProjectDto updateProject(UpdateProjectDto updateProjectDto, Long projectId, Long userId) {
        log.info("Updating project id={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project with this id does not exist"));

        if (!Objects.equals(project.getOwnerId(), userId)) {
            log.warn("User id={} tried to update project id={} without ownership", userId, projectId);
            throw new AccessDeniedException("No permission to update this project");
        }
        String name = project.getName();
        projectMapper.updateProjectFromDto(updateProjectDto, project);
        project = projectRepository.save(project);
        log.info("Project updated: id={}, name={}", projectId, name);
        return projectMapper.toProjectDto(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getFilteredProjects(Long userId, ProjectFilterDto projectFilterDto) {
        List<Project> projects = projectRepository.findAccessibleProjects(userId, ProjectVisibility.PUBLIC);

        return projects.stream()
                .filter(project -> projectFilterDto.namePattern() == null
                        || project.getName().toLowerCase().contains(projectFilterDto.namePattern().toLowerCase()))
                .filter(project -> projectFilterDto.statusPattern() == null
                        || project.getStatus() == projectFilterDto.statusPattern())
                .map(projectMapper::toProjectDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects(Long userId) {
        log.info("Fetching all projects accessible to user id={}", userId);
        List<Project> projects = projectRepository.findAccessibleProjects(userId, ProjectVisibility.PUBLIC);
        log.info("Found {} projects accessible to user id={}", projects.size(), userId);
        return projects.stream()
                .map(projectMapper::toProjectDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long projectId, Long userId) {
        log.info("Fetching project id={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project with this id does not exist"));

        boolean isOwner = Objects.equals(project.getOwnerId(), userId);
        boolean isVisible = project.getVisibility() == ProjectVisibility.PUBLIC;

        if (!isOwner && !isVisible) {
            log.warn("User id={} tried to fetch private project id={} without access", userId, projectId);
            throw new AccessDeniedException("No permission to fetch this project");
        }

        log.info("Project id={} was fetched successfully by user id={}", projectId, userId);

        return projectMapper.toProjectDto(project);
    }

    @Transactional
    public ProjectDto uploadCoverImage(Long projectId, MultipartFile file, Long userId) {
        Project project = getOwnedProject(projectId, userId);
        ProcessedFile processedFile = coverImageProcessor.process(file);
        String oldCoverImageId = project.getCoverImageId();
        String newCoverImageId = fileStorageService.upload(
                processedFile.content(),
                processedFile.contentType(),
                processedFile.extension(),
                "projects/%d/cover".formatted(projectId)
        );

        try {
            project.setCoverImageId(newCoverImageId);
            Project savedProject = projectRepository.save(project);
            fileStorageService.delete(oldCoverImageId);
            return projectMapper.toProjectDto(savedProject);
        } catch (RuntimeException e) {
            fileStorageService.delete(newCoverImageId);
            throw e;
        }
    }

    @Transactional
    public void deleteCoverImage(Long projectId, Long userId) {
        Project project = getOwnedProject(projectId, userId);
        String coverImageId = project.getCoverImageId();
        if (coverImageId == null || coverImageId.isBlank()) {
            return;
        }

        project.setCoverImageId(null);
        projectRepository.save(project);
        fileStorageService.delete(coverImageId);
    }

    private Project getOwnedProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project with this id does not exist"));

        if (!Objects.equals(project.getOwnerId(), userId)) {
            log.warn("User id={} tried to modify project id={} without ownership", userId, projectId);
            throw new AccessDeniedException("No permission to modify this project");
        }

        return project;
    }
}
