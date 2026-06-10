package com.local.lms.core;

import com.local.lms.domain.enums.Operator;
import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class SearchRule {
    private Operator operator;
    private Object value;
}
