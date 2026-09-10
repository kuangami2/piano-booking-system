package com.roomreservation.dto;

/**
 * 排行榜条目，name 为脱敏昵称
 */
public record ActivityRankItem(int rank, Integer userId, String name, double score) {
}
