package faang.school.projectservice.controller.project;

import faang.school.projectservice.config.context.UserContext;
import faang.school.projectservice.dto.project.CreateProjectDto;
import faang.school.projectservice.dto.project.ProjectDto;
import faang.school.projectservice.dto.project.ProjectFilterDto;
import faang.school.projectservice.dto.project.UpdateProjectDto;
import faang.school.projectservice.service.project.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final UserContext userContext;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto createProject(@Valid @RequestBody CreateProjectDto createProjectDto){
        Long userId = userContext.getUserId();
        return projectService.createProject(createProjectDto, userId);
    }

    @PatchMapping("/{projectId}")
    public ProjectDto updateProject(@Valid @RequestBody UpdateProjectDto updateProjectDto, @PathVariable Long projectId){
        Long userId = userContext.getUserId();
        return projectService.updateProject(updateProjectDto, projectId, userId);
    }

    @GetMapping("/filter")
    public List<ProjectDto> getFilteredProjects(ProjectFilterDto projectFilterDto){
        Long userId = userContext.getUserId();
        return projectService.getFilteredProjects(userId, projectFilterDto);
    }

    @GetMapping
    public List<ProjectDto> getProjects(){
        Long userId = userContext.getUserId();
        return projectService.getAllProjects(userId);
    }

    @GetMapping("/{projectId}")
    public ProjectDto getProjectById(@PathVariable Long projectId){
        Long userId = userContext.getUserId();
        return projectService.getProjectById(projectId, userId);
    }
}