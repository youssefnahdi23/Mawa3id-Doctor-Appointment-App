package com.mawa3id.notification;

import com.mawa3id.notification.dto.DeviceRegistrationRequest;
import com.mawa3id.notification.dto.DeviceUnregisterRequest;
import com.mawa3id.notification.dto.NotificationResponse;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    public NotificationController(NotificationService notificationService,
                                  DeviceTokenService deviceTokenService) {
        this.notificationService = notificationService;
        this.deviceTokenService = deviceTokenService;
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

    /** Register (or refresh) this device's push token for the authenticated user. */
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(@AuthenticationPrincipal AppUserDetails principal,
                               @Valid @RequestBody DeviceRegistrationRequest request) {
        deviceTokenService.register(principal.getUserId(), request.token(), request.platform());
    }

    /** Remove a device's push token (on logout). Idempotent. */
    @DeleteMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDevice(@AuthenticationPrincipal AppUserDetails principal,
                                 @Valid @RequestBody DeviceUnregisterRequest request) {
        deviceTokenService.unregister(request.token());
    }
}
