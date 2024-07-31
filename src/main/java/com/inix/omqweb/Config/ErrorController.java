package com.inix.omqweb.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    @Value("${mainUrl}")
    private String mainUrl;

    @RequestMapping("/error")
    public String handleError(Model model) {
        model.addAttribute("mainUrl", mainUrl);
        return "error";
    }
}
