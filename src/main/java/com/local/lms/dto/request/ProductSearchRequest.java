package com.local.lms.dto.request;

import com.local.lms.annotations.Searchable;
import com.local.lms.domain.enums.Operator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class ProductSearchRequest extends PageParams {
    @Searchable(operator = Operator.LIKE)
    private String name;
}
