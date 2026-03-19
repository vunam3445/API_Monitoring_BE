package com.example.demo.common.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

public abstract class PageMixIn<T> {
    @JsonCreator
    public PageMixIn(@JsonProperty("content") List<T> content,
                     @JsonProperty("number") int number,
                     @JsonProperty("size") int size,
                     @JsonProperty("totalElements") long totalElements) {}
}
