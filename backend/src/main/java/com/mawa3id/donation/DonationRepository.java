package com.mawa3id.donation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByProviderSessionId(String providerSessionId);

    List<Donation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
