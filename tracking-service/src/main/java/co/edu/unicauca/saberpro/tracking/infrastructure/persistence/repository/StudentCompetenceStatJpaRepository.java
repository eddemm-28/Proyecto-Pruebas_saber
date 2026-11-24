package co.edu.unicauca.saberpro.tracking.infrastructure.persistence.repository;

import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity.StudentCompetenceStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentCompetenceStatJpaRepository
        extends JpaRepository<StudentCompetenceStatEntity, StudentCompetenceStatEntity.Key> {

    List<StudentCompetenceStatEntity> findByStudentId(UUID studentId);
}
