package strongmancast.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class NavigationModelAdvice {

    @ModelAttribute("organizerLoggedIn")
    public boolean organizerLoggedIn(Principal principal) {
        return principal != null;
    }
}
