package com.ocean.common;

public class Constant {

    public static final String TOKEN_HEADER = "token";
    public static final String TOKEN_REDIS_PREFIX = "token:";
    public static final String VOTE_REDIS_PREFIX = "vote:";
    public static final String CACHE_EBOOK_LIST = "cache:ebook:list";
    public static final String CACHE_DOC_TREE_PREFIX = "cache:doc:tree:";
    public static final String CACHE_STATISTIC = "cache:statistic";

    // Redis 实时今日计数器（使用时拼接日期: stat:today:viewCount:20260617）
    public static final String TODAY_VIEW_COUNT_PREFIX = "stat:today:viewCount:";
    public static final String TODAY_VOTE_COUNT_PREFIX = "stat:today:voteCount:";

    // 在线用户
    public static final String ONLINE_USER_PREFIX = "online:user:";
    public static final long ONLINE_USER_TTL_MINUTES = 30;

    public static final String ROLE_ADMIN = "admin";

    // 阅读历史
    public static final String HISTORY_REDIS_PREFIX = "history:";
    public static final int HISTORY_MAX_SIZE = 50;
    public static final long HISTORY_TTL = 30; // 天

    public static final long TOKEN_EXPIRATION = 86400000L; // 24h

    private Constant() {
    }
}
