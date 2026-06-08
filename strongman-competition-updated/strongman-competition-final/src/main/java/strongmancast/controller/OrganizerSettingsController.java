package strongmancast.controller;

import strongmancast.service.OrganizerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrganizerSettingsController {

    private final OrganizerAccountService organizerAccountService;

    public OrganizerSettingsController(OrganizerAccountService organizerAccountService) {
        this.organizerAccountService = organizerAccountService;
    }

    @GetMapping("/settings/organizer")
    public String organizerSettings(Model model) {
        model.addAttribute("organizerAccount", organizerAccountService.currentAccount());
        return "organizer_settings";
    }

    @PostMapping("/settings/organizer")
    public String updateOrganizerSettings(
            @RequestParam String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        if (username == null || username.isBlank()) {
            redirectAttributes.addFlashAttribute("settingsError", "Username cannot be blank.");
            return "redirect:/settings/organizer";
        }

        if (password != null && !password.isBlank() && !password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("settingsError", "Passwords do not match.");
            return "redirect:/settings/organizer";
        }

        organizerAccountService.updateCredentials(username, password);
        redirectAttributes.addFlashAttribute("settingsMessage", "Organizer login updated.");
        return "redirect:/settings/organizer";
    }
}
