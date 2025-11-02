package com.app.Harvest.Controller;

import com.app.Harvest.dto.response.ApiResponse;
import com.app.Harvest.dto.response.UserResponse;
import com.app.Harvest.Entity.Role;
import com.app.Harvest.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GOVERNMENT', 'COOPERATIVE')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GOVERNMENT')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GOVERNMENT')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable Role role) {
        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingApprovals() {
        List<UserResponse> users = userService.getPendingApprovals();
        return ResponseEntity.ok(ApiResponse.success("Pending approvals retrieved successfully", users));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(@PathVariable Long id) {
        UserResponse user = userService.approveUser(id);
        return ResponseEntity.ok(ApiResponse.success("User approved successfully", user));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> rejectUser(@PathVariable Long id) {
        UserResponse user = userService.rejectUser(id);
        return ResponseEntity.ok(ApiResponse.success("User rejected successfully", user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}