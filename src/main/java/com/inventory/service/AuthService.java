package com.inventory.service;

import com.inventory.dto.MeResponse;
import com.inventory.dto.PasswordChangeRequest;
import com.inventory.dto.ProfileRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.entity.Role;
import com.inventory.entity.User;
import com.inventory.exception.BusinessException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.RoleRepository;
import com.inventory.repository.UserRepository;
import com.inventory.util.Constants;
import com.inventory.util.CurrentUser;
import com.inventory.util.Text;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingService settingService;
    private final LogService logService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       SettingService settingService, LogService logService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.settingService = settingService;
        this.logService = logService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("Password and confirm password do not match");
        }
        String email = Text.email(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with this email already exists");
        }
        Role role = roleRepository.findByName(Constants.ROLE_EMPLOYEE)
                .orElseThrow(() -> new BusinessException("Employee role is not configured"));
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setMobile(Text.clean(request.mobile()));
        user.setAddress(Text.clean(request.address()));
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(Constants.ACTIVE);
        user.setRole(role);
        userRepository.save(user);
        logService.log(email, "USER_REGISTERED", "New employee account registered: " + email);
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        User user = current();
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getMobile(), user.getAddress(),
                user.getRole().getName(), settingService.get("business_name", "Inventory Management System"),
                settingService.currency());
    }

    @Transactional
    public MeResponse updateProfile(ProfileRequest request) {
        User user = current();
        user.setName(request.name().trim());
        user.setMobile(Text.clean(request.mobile()));
        user.setAddress(Text.clean(request.address()));
        userRepository.save(user);
        logService.log("PROFILE_UPDATED", "Profile updated");
        return me();
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User user = current();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password and confirm password do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from the old password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        logService.log("PASSWORD_CHANGED", "Password changed");
    }

    private User current() {
        return userRepository.findByEmailIgnoreCase(CurrentUser.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
