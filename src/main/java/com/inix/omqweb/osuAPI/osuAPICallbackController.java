package com.inix.omqweb.osuAPI;

import com.inix.omqweb.DTO.AccessTokenDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/osuapi")
@RequiredArgsConstructor
public class osuAPICallbackController {
    private final osuAPIService osuAPIService;

    @Value("${osu.redirectUri}")
    private String redirectUri;

    @Value("${mainUrl}")
    private String mainUrl;

    @GetMapping("/callback")
    public String callback(@RequestParam String code, Model model) {
        ResponseEntity<AccessTokenDTO> responseEntity = osuAPIService.getToken(code);
        AccessTokenDTO token = responseEntity.getBody();
        model.addAttribute("accessToken", token.getAccess_token());
        model.addAttribute("refreshToken", token.getRefresh_token());
        model.addAttribute("redirectUri", mainUrl);

        return "callback";
    }

    @GetMapping("/maintenance")
    public String maintenance() {
        return "maintenance";
    }
}
