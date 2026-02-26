package com.ucms_backend.repository;

import com.ucms_backend.model.entity.Profile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for profile persistence operations.
 */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByStudentId(String studentId);
}
