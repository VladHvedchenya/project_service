package faang.school.projectservice.repository;

import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.ProjectVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END 
            FROM Project p 
            WHERE p.ownerId = :ownerId AND p.name = :name
            """
    )
    boolean existsByOwnerIdAndName(Long ownerId, String name);

    @Query("""
                SELECT p FROM Project p
                LEFT JOIN p.teams t
                LEFT JOIN t.teamMembers tm
                WHERE p.visibility = :visibility
                OR tm.userId = :userId
            """)
    List<Project> findAccessibleProjects(
            @Param("userId") Long userId,
            @Param("visibility") ProjectVisibility visibility
    );
}