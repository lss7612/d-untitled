package com.example.demo.club.untitled.domain;

public enum BookCategory {
    LITERATURE("문학"),
    HUMANITIES("인문"),
    SELF_DEVELOPMENT("자기계발"),
    ARTS("예술"),
    IT("IT"),
    COMICS("만화"),
    ECONOMY("경제"),
    LIFESTYLE("라이프스타일"),
    SCIENCE("과학"),
    ETC("기타");

    private final String label;

    BookCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
