package com.inventory.service;

import com.inventory.dto.LogResponse;
import com.inventory.entity.UserLog;
import com.inventory.repository.UserLogRepository;
import com.inventory.util.CurrentUser;
import com.inventory.util.Text;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

    private final UserLogRepository userLogRepository;

    public LogService(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    @Transactional
    public void log(String action, String description) {
        log(CurrentUser.email(), action, description);
    }

    @Transactional
    public void log(String username, String action, String description) {
        UserLog entry = new UserLog();
        entry.setUsername(username);
        entry.setAction(action);
        entry.setDescription(description != null && description.length() > 500 ? description.substring(0, 500) : description);
        userLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<LogResponse> search(String q, String action, LocalDate from, LocalDate to) {
        return userLogRepository.findTop1000ByOrderByIdDesc().stream()
                .filter(l -> action == null || action.isBlank() || l.getAction().equalsIgnoreCase(action))
                .filter(l -> from == null || !l.getCreatedAt().toLocalDate().isBefore(from))
                .filter(l -> to == null || !l.getCreatedAt().toLocalDate().isAfter(to))
                .filter(l -> Text.contains(l.getUsername(), q) || Text.contains(l.getDescription(), q) || Text.contains(l.getAction(), q))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LogResponse> recent() {
        return userLogRepository.findTop8ByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    private LogResponse toResponse(UserLog l) {
        return new LogResponse(l.getId(), l.getUsername(), l.getAction(), l.getDescription(), l.getCreatedAt());
    }
}
