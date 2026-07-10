package faang.school.projectservice.service.project;

import faang.school.projectservice.dto.project.CreateProjectDto;
import faang.school.projectservice.dto.project.ProjectDto;
import faang.school.projectservice.dto.project.ProjectFilterDto;
import faang.school.projectservice.dto.project.UpdateProjectDto;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Интерфейс сервисного слоя для управления жизненным циклом проектов.
 * Обеспечивает бизнес-логику создания, модификации, фильтрации и валидации прав доступа к проектам
 * в рамках эпика «Работа с проектами».
 *
 * @author VladHvedchenya
 */
public interface ProjectService {
    /**
     * Создает новый проект в системе на основе переданных валидных данных.
     *
     * @param createProjectDto объект переноса данных (DTO) с параметрами нового проекта
     *
     * @param userId           идентификатор пользователя (инициатора/владельца проекта), полученный из контекста
     *
     * @return {@link ProjectDto} объект, представляющий структуру созданного проекта с присвоенным ID
     *
     * @throws faang.school.projectservice.exception.DataValidationException если проект с таким именем уже существует
     */
    ProjectDto createProject(@Valid CreateProjectDto createProjectDto, Long userId);

    /**
     * Обновляет существующий проект по его идентификатору.
     * Метод выполняет валидацию прав пользователя на редактирование (проверка на владение или участие в команде).
     *
     * @param updateProjectDto объект переноса данных (DTO) с обновленными полями проекта
     *
     * @param projectId        идентификатор обновляемого проекта
     *
     * @param userId           идентификатор пользователя, выполняющего операцию (для проверки прав доступа)
     *
     * @return {@link ProjectDto} объект с актуальным состоянием проекта после обновления
     *
     * @throws faang.school.projectservice.exception.EntityNotFoundException если проект с указанным projectId не найден
     *
     * @throws org.springframework.security.access.AccessDeniedException    если у пользователя недостаточно прав для изменения проекта
     */
    ProjectDto updateProject(UpdateProjectDto updateProjectDto, Long projectId, Long userId);

    /**
     * Возвращает список проектов, доступных текущему пользователю, с применением кастомных фильтров.
     * Фильтрация может происходить по имени, статусу или другим бизнес-критериям, переданным в DTO.
     *
     * @param userId           идентификатор пользователя для определения уровня видимости проектов (Public/Private)
     *
     * @param projectFilterDto объект, содержащий критерии фильтрации (может содержать null поля)
     *
     * @return список {@link List} из {@link ProjectDto}, соответствующих заданным фильтрам, либо пустой список
     */
    List<ProjectDto> getFilteredProjects(Long userId, ProjectFilterDto projectFilterDto);

    /**
     * Возвращает полный список проектов, к которым текущий пользователь имеет легитимный доступ.
     * Включает публичные проекты системы и приватные проекты, где пользователь является участником.
     *
     * @param userId идентификатор пользователя для фильтрации прав доступа и видимости проектов
     *
     * @return список {@link List} всех доступных {@link ProjectDto}
     */
    List<ProjectDto> getAllProjects(Long userId);

    /**
     * Возвращает детальную информацию о конкретном проекте по его идентификатору.
     * Перед возвратом обязательно проверяется право пользователя просматривать данный проект.
     *
     * @param projectId идентификатор запрашиваемого проекта
     *
     * @param userId    идентификатор пользователя, запрашивающего информацию (для валидации прав доступа)
     *
     * @return {@link ProjectDto} объект с подробными данными о проекте
     *
     * @throws faang.school.projectservice.exception.EntityNotFoundException если проект с указанным projectId не найден
     *
     * @throws org.springframework.security.access.AccessDeniedException    если проект приватный, а пользователь не имеет к нему доступа
     */
    ProjectDto getProjectById(Long projectId, Long userId);
}