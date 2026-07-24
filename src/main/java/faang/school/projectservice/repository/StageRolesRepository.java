package faang.school.projectservice.repository;

import faang.school.projectservice.model.stage.StageRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRolesRepository extends JpaRepository<StageRole, Long> {
}