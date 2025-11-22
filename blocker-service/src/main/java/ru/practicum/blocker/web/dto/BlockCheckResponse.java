package ru.practicum.blocker.web.dto;

public record BlockCheckResponse(
        Boolean allowed,
        String reason
) {
    public static BlockCheckResponse allow() {
        return new BlockCheckResponse(true, null);
    }

    public static BlockCheckResponse block(String reason) {
        return new BlockCheckResponse(false, reason);
    }
}
