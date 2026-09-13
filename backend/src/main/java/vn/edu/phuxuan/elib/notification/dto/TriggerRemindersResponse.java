package vn.edu.phuxuan.elib.notification.dto;

public record TriggerRemindersResponse(
        int dueRemindersCreated,
        int overdueCreated,
        int dispatched
) {}
