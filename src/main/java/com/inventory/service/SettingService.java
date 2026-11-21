package com.inventory.service;

import com.inventory.entity.Setting;
import com.inventory.exception.BusinessException;
import com.inventory.repository.SettingRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

    public static final Set<String> KEYS = Set.of("business_name", "business_address", "business_phone",
            "business_email", "currency_symbol", "low_stock_threshold");

    private final SettingRepository settingRepository;
    private final LogService logService;

    public SettingService(SettingRepository settingRepository, LogService logService) {
        this.settingRepository = settingRepository;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public String get(String key, String fallback) {
        return settingRepository.findBySettingKey(key)
                .map(Setting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public Map<String, String> all() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : KEYS.stream().sorted().toList()) {
            result.put(key, get(key, ""));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public int lowStockThreshold() {
        try {
            return Integer.parseInt(get("low_stock_threshold", "5").trim());
        } catch (NumberFormatException ex) {
            return 5;
        }
    }

    @Transactional(readOnly = true)
    public String currency() {
        return get("currency_symbol", "₹");
    }

    @Transactional
    public Map<String, String> update(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!KEYS.contains(entry.getKey())) {
                throw new BusinessException("Unknown setting: " + entry.getKey());
            }
        }
        String threshold = values.get("low_stock_threshold");
        if (threshold != null) {
            try {
                if (Integer.parseInt(threshold.trim()) < 0) {
                    throw new BusinessException("Low-stock threshold cannot be negative");
                }
            } catch (NumberFormatException ex) {
                throw new BusinessException("Low-stock threshold must be a whole number");
            }
        }
        String name = values.get("business_name");
        if (name != null && name.isBlank()) {
            throw new BusinessException("Business name is required");
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.length() > 500) {
                throw new BusinessException("Setting value is too long: " + entry.getKey());
            }
            Setting setting = settingRepository.findBySettingKey(entry.getKey()).orElseGet(() -> {
                Setting created = new Setting();
                created.setSettingKey(entry.getKey());
                return created;
            });
            setting.setSettingValue(value);
            settingRepository.save(setting);
        }
        logService.log("SETTINGS_UPDATED", "Application settings updated");
        return all();
    }
}
