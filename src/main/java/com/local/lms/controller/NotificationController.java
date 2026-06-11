package com.local.lms.controller;

import com.local.lms.dto.request.NotificationTemplateRequest;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.dto.response.NotificationTemplateResponse;
import com.local.lms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification template management APIs")
public class NotificationController extends  BaseController {

    private final NotificationService notificationService;

    @GetMapping("/templates")
    @Operation(summary = "Get all notification templates")
    public ResponseResult<List<NotificationTemplateResponse>> getTemplates() {
        return ResponseResult.success(notificationService.getAllTemplates());
    }

    @PostMapping("/templates")
    @Operation(summary = "Create a notification template")
    public ResponseResult<NotificationTemplateResponse> createTemplate(
            @Validated @RequestBody NotificationTemplateRequest request) {
        return ResponseResult.success("Template created", notificationService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a notification template")
    public ResponseResult<NotificationTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Validated @RequestBody NotificationTemplateRequest request) {
        return ResponseResult.success("Template updated",notificationService.updateTemplate(id, request));
    }

    @PostMapping("/reminders/trigger")
    @Operation(summary = "Manually trigger due date reminders")
    public ResponseResult<Void> triggerReminders(@RequestParam(defaultValue = "3") int daysAhead) {
        notificationService.sendDueDateReminders(daysAhead);
        return ResponseResult.success("Reminders sent");
    }
}
