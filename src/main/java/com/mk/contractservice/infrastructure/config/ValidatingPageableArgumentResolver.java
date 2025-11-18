package com.mk.contractservice.infrastructure.config;

import com.mk.contractservice.infrastructure.exception.InvalidPaginationException;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ValidatingPageableArgumentResolver extends PageableHandlerMethodArgumentResolver {

    private final PaginationProperties paginationProperties;

    public ValidatingPageableArgumentResolver(PaginationProperties paginationProperties) {
        this.paginationProperties = paginationProperties;
        setFallbackPageable(PageRequest.of(0, paginationProperties.getDefaultPageSize(),
                Sort.by(Sort.Direction.DESC, "lastModified")));
        setMaxPageSize(paginationProperties.getMaxPageSize());
    }

    @Override
    public Pageable resolveArgument(MethodParameter methodParameter,
                                    ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest,
                                    WebDataBinderFactory binderFactory) {

        String pageParam = webRequest.getParameter(getPageParameterName());
        String sizeParam = webRequest.getParameter(getSizeParameterName());

        if (pageParam != null) {
            try {
                int page = Integer.parseInt(pageParam);
                if (page < 0) {
                    throw new InvalidPaginationException(
                            "Page number must not be less than zero, but was: " + page);
                }
            } catch (NumberFormatException e) {
                throw new InvalidPaginationException("Invalid page number format: " + pageParam);
            }
        }

        if (sizeParam != null) {
            try {
                int size = Integer.parseInt(sizeParam);
                if (size < 1) {
                    throw new InvalidPaginationException(
                            "Page size must not be less than one, but was: " + size);
                }
                if (size > paginationProperties.getMaxPageSize()) {
                    throw new InvalidPaginationException(
                            "Page size must not exceed " + paginationProperties.getMaxPageSize() + ", but was: " + size);
                }
            } catch (NumberFormatException e) {
                throw new InvalidPaginationException("Invalid page size format: " + sizeParam);
            }
        }

        return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    }
}

