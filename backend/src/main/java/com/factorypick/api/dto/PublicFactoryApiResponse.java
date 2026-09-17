package com.factorypick.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;

public record PublicFactoryApiResponse(
        Header header,
        Body body
) {

    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    public record Body(
            int totalCount,
            int numOfRows,
            int pageNo,
            Items items
    ) {
    }

    public record Items(
            @JsonFormat(
                    with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY
            )
            List<Item> item
    ) {
    }

    public record Item(
            String fctryManageNo,
            String cmpnyNm,
            String rnAdres,
            String rprsntvNm,
            String cvplChrgOrgnztNm,
            String cmpnyTelno,
            String cmpnyFxnum,
            Integer allEmplyCo,
            String frstFctryRegistDe,
            String rprsntvIndutyCode,
            String indutyCodes,
            String indutyNm,
            String mainProductCn,
            String hmpadr,
            String irsttNm
    ) {
    }
}