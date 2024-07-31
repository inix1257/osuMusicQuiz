package com.inix.omqweb.History;

import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class LobbyHistoryController {
    private final LobbyHistoryDetailRepository lobbyHistoryDetailRepository;

    @GetMapping
    public ResponseEntity<List<LobbyHistoryDetail>> getHistory(HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        List<LobbyHistoryDetail> lobbyHistoryDetails = lobbyHistoryDetailRepository.findLobbyHistoryDetailByPlayer(player);

        return ResponseEntity.ok(lobbyHistoryDetails);
    }
}
