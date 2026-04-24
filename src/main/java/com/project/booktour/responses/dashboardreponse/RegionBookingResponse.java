// com.project.booktour.responses.dashboardreponse.RegionBookingDTO.java
package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionBookingResponse {
    private String name;  // Tên miền: "Miền Bắc", "Miền Trung", "Miền Nam"
    private Long value;  // Số lượt đặt
}