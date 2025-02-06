package ru.yandex.practicum.filmorate.model;

public enum SortBy {

    YEAR("year"),
    LIKES("likes");

    private final String value;

    SortBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SortBy fromString(String text) {
        for (SortBy sortBy : SortBy.values()) {
            if (sortBy.value.equalsIgnoreCase(text)) {
                return sortBy;
            }
        }
        throw new IllegalArgumentException("Некорректный параметр sortBy. Допустимые значения: year, likes");
    }

}
