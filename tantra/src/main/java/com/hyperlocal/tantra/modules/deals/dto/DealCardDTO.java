package com.hyperlocal.tantra.modules.deals.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealCardDTO {
    private Integer id;
    private String groupKey;
    private String icon;
    private String labelEn;
    private String labelHi;
    private String badgeEn;
    private String badgeHi;
    private String accentColor;
    private Boolean backendUi;
    private String backgroundImageUrl;
    private Long listingCount;
    private BigDecimal minPrice;
    private String unitEn;
    private String unitHi;
    private String ctaType;
    private String ctaValue;
    private Integer displayOrder;
}
