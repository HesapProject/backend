package uz.hesap.service.main.model;

import java.util.List;

/** Wrapper for blog list with total count for pagination. */
public record NewsListResponse(List<NewsResponse> list, Long count) {}
