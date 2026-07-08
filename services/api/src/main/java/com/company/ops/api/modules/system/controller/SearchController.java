package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.service.SearchService;
import com.company.ops.api.modules.system.service.SearchService.SearchResult;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) { this.searchService = searchService; }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<SearchResult>> search(@RequestParam String q) {
    return ApiResponse.ok(searchService.search(q));
  }
}
