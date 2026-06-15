package com.ocean.common;

public class Constant {

    public static final String TOKEN_HEADER = "token";
    public static final String TOKEN_REDIS_PREFIX = "token:";
    public static final String VOTE_REDIS_PREFIX = "vote:";
    public static final String CACHE_EBOOK_LIST = "cache:ebook:list";
    public static final String CACHE_DOC_TREE_PREFIX = "cache:doc:tree:";
    public static final String CACHE_STATISTIC = "cache:statistic";

    public static final String ROLE_ADMIN = "admin";

    public static final long TOKEN_EXPIRATION = 86400000L; // 24h

    private Constant() {
    }
}
