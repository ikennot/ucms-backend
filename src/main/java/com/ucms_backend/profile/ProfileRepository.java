package com.ucms_backend.profile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for profile persistence operations.
 */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByStudentId(String studentId);
}
