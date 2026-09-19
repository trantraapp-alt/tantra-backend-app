package com.hyperlocal.tantra.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.home.dto.HomeResponse;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import lombok.Data;

import java.util.List;

/** Combined search result: matching listings + matching business profiles. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {

    private long totalListings;
    private long totalPages;
    private int currentPage;
    private int pageSize;
    private String query;

    private List<ListingCardDTO> listings;
    private List<HomeResponse.BusinessProfileCard> businessProfiles;
}
