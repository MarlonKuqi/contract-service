package com.mk.contractservice.controllers.contract.pagination;

import com.mk.contractservice.controllers.shared.InvalidPaginationException;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ValidatingPageableArgumentResolver extends PageableHandlerMethodArgumentResolver {

    private final int defaultPageSize;
    private final int maxPageSize;

    public ValidatingPageableArgumentResolver(final int defaultPageSize, final int maxPageSize) {
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        setFallbackPageable(PageRequest.of(0, defaultPageSize,
                Sort.by(Sort.Direction.DESC, "lastModified")));
        setMaxPageSize(maxPageSize);
    }

    @Override
    public Pageable resolveArgument(final MethodParameter methodParameter,
                                    final ModelAndViewContainer mavContainer,
                                    final NativeWebRequest webRequest,
                                    final WebDataBinderFactory binderFactory) {

        final String pageParam = webRequest.getParameter(getPageParameterName());
        final String sizeParam = webRequest.getParameter(getSizeParameterName());

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
                if (size > maxPageSize) {
                    throw new InvalidPaginationException(
                            "Page size must not exceed " + maxPageSize + ", but was: " + size);
                }
            } catch (NumberFormatException e) {
                throw new InvalidPaginationException("Invalid page size format: " + sizeParam);
            }
        }

        return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    }
}
