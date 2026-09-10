package com.roomreservation.dto;

/**
 * 临近取消风控状态，仅本人可见
 */
public record RiskStatus(boolean active, String until, String reason, int nearCancelCount) {
}
