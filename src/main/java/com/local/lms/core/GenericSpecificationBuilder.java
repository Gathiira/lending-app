package com.local.lms.core;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

import java.util.Map;

public class GenericSpecificationBuilder<T> {

    public Specification<T> build(Map<String, SearchRule> filters) {

        return (root, query, cb) -> {

            if (filters == null || filters.isEmpty()) {
                return cb.conjunction();
            }

            Predicate predicate = cb.conjunction();

            for (var entry : filters.entrySet()) {

                Path<?> path = resolvePath(root, entry.getKey());
                SearchRule rule = entry.getValue();

                predicate = cb.and(predicate,
                        buildPredicate(cb, path, rule));
            }

            return predicate;
        };
    }

    private Path<?> resolvePath(Root<T> root, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = root;

        for (String p : parts) {
            path = path.get(p);
        }

        return path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(CriteriaBuilder cb,
                                     Path<?> path,
                                     SearchRule rule) {

        return switch (rule.getOperator()) {

            case EQUAL -> cb.equal(path, rule.getValue());

            case LIKE -> cb.like(cb.lower((Path<String>) path),
                    "%" + rule.getValue().toString().toLowerCase() + "%");

            case GREATER_OR_EQUAL -> cb.greaterThanOrEqualTo(
                    (Path<Comparable>) path,
                    (Comparable) rule.getValue());

            case LESS_OR_EQUAL -> cb.lessThanOrEqualTo(
                    (Path<Comparable>) path,
                    (Comparable) rule.getValue());

            case GREATER_THAN -> cb.greaterThan(
                    (Path<Comparable>) path,
                    (Comparable) rule.getValue());

            case LESS_THAN -> cb.lessThan(
                    (Path<Comparable>) path,
                    (Comparable) rule.getValue());

            case IN -> path.in(rule.getValue());
        };
    }
}
