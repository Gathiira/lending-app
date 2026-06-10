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
public class LoanSearchRequest extends PageParams {
    @Searchable(operator = Operator.LIKE, field = "loan_reference")
    private String loanReference;
}
