package com.local.lms.mapper;

import com.local.lms.annotations.Searchable;
import com.local.lms.core.SearchRule;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FilterMapper {

    public static Map<String, SearchRule> toFilters(Object dto) {
        Map<String, SearchRule> filters = new HashMap<>();
        if (dto == null) return filters;

        Field[] fields = dto.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Searchable ann = field.getAnnotation(Searchable.class);
            if (ann == null) continue;
            try {
                Object value = field.get(dto);
                if (value == null && ann.ignoreIfNull()) {
                    continue;
                }
                String key = ann.field().isEmpty()  ? field.getName() : ann.field();
                filters.put(key, new SearchRule(ann.operator(), value));

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return filters;
    }
}
