package faang.school.projectservice.repository;

import faang.school.projectservice.model.stage.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
    @Query("""
                SELECT DISTINCT s FROM Stage s
                LEFT JOIN FETCH s.stageRoles
                LEFT JOIN FETCH s.executors
                WHERE s.project.id = :projectId
            """)
    List<Stage> findAllByProjectIdWithRolesAndExecutors(@Param("projectId") Long projectId);
}