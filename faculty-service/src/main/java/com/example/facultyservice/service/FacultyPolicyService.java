package com.example.facultyservice.service;

import com.example.facultyservice.dto.FacultyPolicyDto;
import com.example.facultyservice.entity.FacultyPolicy;
import com.example.facultyservice.repository.FacultyPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service encapsulating CRUD operations for faculty policies. Policies govern reservation
 * constraints such as maximum duration and whether approval is required. Only administrators
 * should modify policies; other users may read them.
 */
@Service
public class FacultyPolicyService {

    private final FacultyPolicyRepository policyRepository;

    public FacultyPolicyService(FacultyPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional
    public FacultyPolicy createPolicy(FacultyPolicyDto dto) {
        FacultyPolicy policy = new FacultyPolicy(
                dto.getFacultyId(),
                dto.getMaxDuration(),
                dto.getRequireApproval(),
                dto.getAllowedRoles()
        );
        return policyRepository.save(policy);
    }

    @Transactional
    public FacultyPolicy updatePolicy(Long id, FacultyPolicyDto dto) {
        return policyRepository.findById(id)
                .map(policy -> {
                    policy.setFacultyId(dto.getFacultyId());
                    policy.setMaxDuration(dto.getMaxDuration());
                    policy.setRequireApproval(dto.getRequireApproval());
                    policy.setAllowedRoles(dto.getAllowedRoles());
                    return policyRepository.save(policy);
                })
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<FacultyPolicy> getPolicy(Long id) {
        return policyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<FacultyPolicy> getAllPolicies() {
        return policyRepository.findAll();
    }

    @Transactional
    public void deletePolicy(Long id) {
        if (!policyRepository.existsById(id)) {
            throw new IllegalArgumentException("Policy not found: " + id);
        }
        policyRepository.deleteById(id);
    }
}