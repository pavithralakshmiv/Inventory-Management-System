package com.inventory.controller;

import com.inventory.service.SettingService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public Map<String, String> get() {
        return settingService.all();
    }

    @PutMapping
    public Map<String, String> update(@RequestBody Map<String, String> values) {
        return settingService.update(values);
    }
}
