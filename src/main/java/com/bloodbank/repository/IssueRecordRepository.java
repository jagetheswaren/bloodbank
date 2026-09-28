package com.bloodbank.repository;

import com.bloodbank.entity.IssueRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {

    Optional<IssueRecord> findByIssueCode(String issueCode);

    boolean existsByBloodUnitId(Long bloodUnitId);

    Optional<IssueRecord> findByBloodUnitId(Long bloodUnitId);

    Page<IssueRecord> findAllByOrderByIssueDateDesc(Pageable pageable);
}
