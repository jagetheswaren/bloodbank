package com.bloodbank.repository;

import com.bloodbank.entity.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {

    Optional<Donor> findByDonorCode(String donorCode);

    Optional<Donor> findByEmail(String email);

    Optional<Donor> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByDonorCode(String donorCode);

    Page<Donor> findByActive(boolean active, Pageable pageable);
}
