package com.app.Harvest.Service;

import com.app.Harvest.dto.response.UserResponse;
import com.app.Harvest.Entity.Role;

import java.util.List;

public interface UserService {

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByRole(Role role);

    List<UserResponse> getPendingApprovals();

    UserResponse approveUser(Long userId);

    UserResponse rejectUser(Long userId);

    void deleteUser(Long userId);
}