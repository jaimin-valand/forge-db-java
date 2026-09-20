package com.jaimin.db.storage;

/** Physical role of a database page. Stored in every page header. */
public enum PageType {
    FREE(0),
    DATA(1),
    INDEX(2),
    CATALOG(3),
    WAL_CHECKPOINT(4);

    private final int code;
    PageType(int code) { this.code = code; }
    public int code() { return code; }

    public static PageType fromCode(int code) {
        for (PageType type : values()) if (type.code == code) return type;
        throw new IllegalStateException("unknown page type: " + code);
    }
}
