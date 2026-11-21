package com.inventory.service;

import com.inventory.dto.UserRequest;
import com.inventory.dto.UserResponse;
import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.RoleRepository;
import com.inventory.repository.UserRepository;
import com.inventory.util.Constants;
import com.inventory.util.CurrentUser;
import com.inventory.util.Text;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, LogService logService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> search(String q) {
        return userRepository.findAll().stream()
                .filter(u -> Text.contains(u.getName(), q) || Text.contains(u.getEmail(), q) || Text.contains(u.getMobile(), q))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = Text.email(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("A user with email '" + email + "' already exists");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters");
        }
        User user = new User();
        apply(user, request, email);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        logService.log("USER_ADDED", "User added: " + saved.getEmail() + " (" + saved.getRole().getName() + ")");
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = find(id);
        String email = Text.email(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("A user with email '" + email + "' already exists");
        }
        boolean wasActiveAdmin = Constants.ROLE_ADMIN.equals(user.getRole().getName())
                && Constants.ACTIVE.equals(user.getStatus());
        apply(user, request, email);
        boolean stillActiveAdmin = Constants.ROLE_ADMIN.equals(user.getRole().getName())
                && Constants.ACTIVE.equals(user.getStatus());
        if (wasActiveAdmin && !stillActiveAdmin
                && userRepository.countByRoleNameAndStatus(Constants.ROLE_ADMIN, Constants.ACTIVE) <= 1) {
            throw new BusinessException("At least one active administrator must remain");
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new BusinessException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        User saved = userRepository.save(user);
        logService.log("USER_UPDATED", "User updated: " + saved.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = find(id);
        if (user.getEmail().equalsIgnoreCase(CurrentUser.email())) {
            throw new BusinessException("You cannot delete your own account");
        }
        if (Constants.ROLE_ADMIN.equals(user.getRole().getName())
                && Constants.ACTIVE.equals(user.getStatus())
                && userRepository.countByRoleNameAndStatus(Constants.ROLE_ADMIN, Constants.ACTIVE) <= 1) {
            throw new BusinessException("At least one active administrator must remain");
        }
        userRepository.delete(user);
        userRepository.flush();
        logService.log("USER_DELETED", "User deleted: " + user.getEmail());
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void apply(User user, UserRequest request, String email) {
        Role role = roleRepository.findByName(request.role().trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("Role must be ADMIN or EMPLOYEE"));
        user.setName(request.name().trim());
        user.setMobile(Text.clean(request.mobile()));
        user.setEmail(email);
        user.setAddress(Text.clean(request.address()));
        user.setStatus(CategoryService.normalizeStatus(request.status()));
        user.setRole(role);
        if (user.getEmail().equalsIgnoreCase(CurrentUser.email()) && !Constants.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("You cannot deactivate your own account");
        }
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getMobile(), u.getEmail(), u.getAddress(),
                u.getStatus(), u.getRole().getName());
    }
}
