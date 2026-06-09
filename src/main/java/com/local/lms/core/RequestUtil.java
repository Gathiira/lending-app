package com.local.lms.core;

import com.local.lms.exceptions.ExceptionAssert;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class RequestUtil {
    public static Integer getParaToInt(String name, Integer defaultValue) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return toInt(request.getParameter(name), defaultValue);
    }

    public static Integer toInt(String value, Integer defaultValue) {
        try {
            if (StringUtils.isBlank(value))
                return defaultValue;
            value = value.trim();
            if (value.startsWith("N") || value.startsWith("n"))
                return -Integer.parseInt(value.substring(1));
            return Integer.parseInt(value);
        } catch (Exception e) {
            ExceptionAssert.throwException("Can not parse the parameter \"" + value + "\" to Integer value.");
        }
        return null;
    }

}
