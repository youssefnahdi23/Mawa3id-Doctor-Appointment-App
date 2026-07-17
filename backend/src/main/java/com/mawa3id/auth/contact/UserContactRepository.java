package com.mawa3id.auth.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {

    Optional<UserContact> findByTypeAndNormalizedValue(ContactType type, String normalizedValue);

    boolean existsByTypeAndNormalizedValue(ContactType type, String normalizedValue);

    List<UserContact> findByUserId(Long userId);

    Optional<UserContact> findByUserIdAndTypeAndPrimaryTrue(Long userId, ContactType type);
}
