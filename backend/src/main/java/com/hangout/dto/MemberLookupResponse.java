package com.hangout.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** What the cashier sees after scanning a client's QR (limited to the cashier's own restaurant). */
@Data
public class MemberLookupResponse {
    private String memberCode;
    private String name;
    private String avatar;
    private LocalDateTime memberSince;
    /** The client's points at the restaurant of the cashier. */
    private int pointsAtRestaurant;
    private String restaurantName;
    /** Vouchers still valid at this restaurant. */
    private List<VoucherResponse> activeVouchers;
    private int usedVouchers;
}
