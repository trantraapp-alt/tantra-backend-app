package com.hyperlocal.tantra.modules.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One tile in the home screen stats ribbon. */
@Data
@AllArgsConstructor
public class StatTileDTO {

    /** Machine key — frontend can use for special rendering if needed. */
    private String statKey;

    private String labelEn;
    private String labelHi;

    /** Emoji or icon key. */
    private String icon;

    /** Pre-formatted display value, e.g. "247", "18", "₹50L+". */
    private String value;

    private int displayOrder;
}
