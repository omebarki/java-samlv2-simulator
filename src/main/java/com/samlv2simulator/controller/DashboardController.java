package com.samlv2simulator.controller;

import com.samlv2simulator.model.SamlExchangeLog;
import com.samlv2simulator.model.SamlProfile;
import com.samlv2simulator.repository.SamlExchangeLogRepository;
import com.samlv2simulator.repository.SamlProfileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final SamlProfileRepository profileRepository;
    private final SamlExchangeLogRepository logRepository;

    public DashboardController(SamlProfileRepository profileRepository,
                               SamlExchangeLogRepository logRepository) {
        this.profileRepository = profileRepository;
        this.logRepository = logRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<SamlProfile> profiles = profileRepository.findAllByOrderByUpdatedAtDesc();
        List<SamlExchangeLog> recentLogs = logRepository.findTop20ByOrderByTimestampDesc();
        model.addAttribute("profiles", profiles);
        model.addAttribute("recentLogs", recentLogs);
        return "dashboard";
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        List<SamlExchangeLog> logs = logRepository.findAllByOrderByTimestampDesc();
        model.addAttribute("logs", logs);
        return "logs";
    }
}
