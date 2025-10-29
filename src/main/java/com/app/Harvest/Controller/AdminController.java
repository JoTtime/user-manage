package com.app.Harvest.Controller;

import com.app.Harvest.model.User;
import com.app.Harvest.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepo;
    public AdminController(UserRepository userRepo) { this.userRepo = userRepo; }

    @GetMapping("/pending-cooperatives")
    public List<User> pendingCooperatives() {
        return userRepo.findAll().stream()
                .filter(u -> u.getRole().name().equals("COOPERATIVE") && (u.getIsValidated() == null || !u.getIsValidated()))
                .toList();
    }

    @PostMapping("/validate-cooperative/{id}")
    public ResponseEntity<?> validateCooperative(@PathVariable Long id) {
        User coop = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (coop.getRole().name().equals("COOPERATIVE")) {
            coop.setIsValidated(true);
            userRepo.save(coop);
            return ResponseEntity.ok("Cooperative validated");
        }
        return ResponseEntity.badRequest().body("User is not a cooperative");
    }
}
