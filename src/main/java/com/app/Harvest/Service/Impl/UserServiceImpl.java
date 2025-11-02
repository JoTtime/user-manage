package com.app.Harvest.Service.Impl;

import com.app.Harvest.dto.response.UserResponse;
import com.app.Harvest.Entity.Role;
import com.app.Harvest.Entity.User;
import com.app.Harvest.exception.ResourceNotFoundException;
import com.app.Harvest.Repository.UserRepository;
import com.app.Harvest.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getPendingApprovals() {
        return userRepository.findByIsApprovedFalseAndRole(Role.COOPERATIVE).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsApproved(true);
        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsActive(false);
        user.setIsApproved(false);
        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        userRepository.delete(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isApproved(user.getIsApproved())
                .registrationNumber(user.getRegistrationNumber())
                .cooperativeName(user.getCooperative() != null ? user.getCooperative().getName() : null)
                .cooperativeId(user.getCooperative() != null ? user.getCooperative().getId() : null)
                .region(user.getRegion())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .build();
    }
}