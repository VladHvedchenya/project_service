package faang.school.projectservice.repository;

import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    @Query("""
            SELECT tm FROM TeamMember tm JOIN tm.team t 
            WHERE tm.userId = :userId 
            AND t.project.id = :projectId
            """
    )
    Optional<TeamMember> findByUserIdAndProjectId(long userId, long projectId);

    List<TeamMember> findByUserId(long userId);

    /**
     * Ищет участников проекта с определенной ролью,
     * исключая тех, кто уже назначен на конкретный этап.
     */
    @Query("""
                SELECT tm FROM TeamMember tm 
                JOIN tm.roles r 
                WHERE tm.team.project.id = :projectId 
                AND r = :role 
                AND tm.id NOT IN (
                    SELECT exec.id FROM Stage s 
                    JOIN s.executors exec 
                    WHERE s.stageId = :stageId
                )
            """)
    List<TeamMember> findMembersByProjectIdAndRoleExcludingStage(
            @Param("projectId") Long projectId,
            @Param("role") TeamRole role,
            @Param("stageId") Long stageId
    );
}