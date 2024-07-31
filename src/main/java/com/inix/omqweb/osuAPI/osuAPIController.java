package com.inix.omqweb.osuAPI;

import com.inix.omqweb.DTO.AccessTokenDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class osuAPIController {
    private final osuAPIService osuAPIService;

    @GetMapping("/getme")
    public ResponseEntity<Player> getMe(HttpServletRequest request) {
        Player userInfo = (Player) request.getAttribute("userInfo");

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/getRefreshToken")
    public ResponseEntity<AccessTokenDTO> getRefreshToken(@RequestParam String refreshToken) {
        // Return the new access token using the given refresh token
        AccessTokenDTO accessTokenDTO = osuAPIService.getRefreshToken(refreshToken);

        if (accessTokenDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }else{
            return ResponseEntity.ok(accessTokenDTO);
        }
    }
}
