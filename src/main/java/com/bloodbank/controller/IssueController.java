package com.bloodbank.controller;

import com.bloodbank.dto.request.IssueRequest;
import com.bloodbank.dto.response.IssueRecordResponse;
import com.bloodbank.dto.response.IssueResponse;
import com.bloodbank.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Blood Issuing", description = "APIs for safely issuing blood units following FEFO (First Expire, First Out) rules")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    @Operation(
        summary = "Issue blood units for a patient/hospital",
        description = "Allocates safe AVAILABLE blood units using FEFO order. " +
                      "Rejects requests if stock is insufficient or if units are expired/near-expiry."
    )
    public ResponseEntity<IssueResponse> issueBlood(@Valid @RequestBody IssueRequest request) {
        IssueResponse response = issueService.issueBlood(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated list of blood issue records", description = "Retrieves audit history of all issued blood units")
    public ResponseEntity<Page<IssueRecordResponse>> getAllIssues(
            @PageableDefault(size = 10, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<IssueRecordResponse> issues = issueService.getAllIssues(pageable);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get issue record by ID", description = "Retrieves full details for a specific blood issue transaction")
    public ResponseEntity<IssueRecordResponse> getIssueById(@PathVariable Long id) {
        IssueRecordResponse response = issueService.getIssueById(id);
        return ResponseEntity.ok(response);
    }
}
