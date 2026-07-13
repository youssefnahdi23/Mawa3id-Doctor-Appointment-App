package com.mawa3id.notification;

import com.mawa3id.notification.dto.NotificationResponse;
import com.mawa3id.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponse> list(@AuthenticationPrincipal AppUserDetails principal,
                                           @RequestParam(defaultValue = "false") boolean unread,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return notificationService.listForUser(principal.getUserId(), unread, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("count", notificationService.unreadCount(principal.getUserId()));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AppUserDetails principal,
                                         @PathVariable Long id) {
        return notificationService.markRead(principal.getUserId(), id);
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("updated", notificationService.markAllRead(principal.getUserId()));
    }
}
