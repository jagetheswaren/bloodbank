package com.bloodbank.repository;

import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findTopByDonorOrderByDonationDateDescCreatedAtDesc(Donor donor);

    Optional<Donation> findTopByDonorIdOrderByDonationDateDescCreatedAtDesc(Long donorId);

    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    Optional<Donation> findByDonationCode(String donationCode);

    boolean existsByDonationCode(String donationCode);

    Page<Donation> findByDonorId(Long donorId, Pageable pageable);
}
