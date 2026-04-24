// DashboardService.java - Enhanced version with daily/monthly/yearly statistics
package com.project.booktour.services;

import com.project.booktour.models.*;
import com.project.booktour.repositories.BookingRepository;
import com.project.booktour.repositories.CheckoutRepository;
import com.project.booktour.repositories.TourRepository;
import com.project.booktour.repositories.UserRepository;
import com.project.booktour.responses.dashboardreponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final CheckoutRepository checkoutRepository;

    public DashboardResponse getDashboardStats() {
        Long activeTours = tourRepository.countByAvailabilityTrue() != null ? tourRepository.countByAvailabilityTrue() : 0L;
        Long totalBookings = bookingRepository.count();
        Long totalUsers = userRepository.countActiveUsers() != null ? userRepository.countActiveUsers() : 0L;
        Double totalRevenue = calculateTotalRevenue() != null ? calculateTotalRevenue() : 0.0;

        List<RegionBookingResponse> regionBookings = getRegionBookings();
        List<PaymentMethodResponse> paymentMethods = getPaymentMethods();
        List<TourStatsResponse> tourStats = getTourStats().stream().limit(5).toList();
        List<BookingStatsResponse> latestBookings = getLatestBookings();
        List<MonthlyRevenueResponse> monthlyRevenues = getMonthlyRevenues();

        // New statistics
        List<DailyStatsResponse> dailyStats = getDailyStats();
        List<YearlyStatsResponse> yearlyStats = getYearlyStats();

        return new DashboardResponse(
                activeTours, totalBookings, totalUsers, totalRevenue,
                regionBookings, paymentMethods, tourStats, latestBookings,
                monthlyRevenues, dailyStats, yearlyStats
        );
    }

    private Double calculateTotalRevenue() {
        List<Checkout> checkouts = checkoutRepository.findAll();
        return checkouts.stream()
                .filter(checkout -> checkout.getPaymentStatus() != null && checkout.getPaymentStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(Checkout::getAmount)
                .sum();
    }

    private List<RegionBookingResponse> getRegionBookings() {
        List<Object[]> bookingCounts = bookingRepository.countBookingsByRegion();

        Map<String, Long> regionMap = new HashMap<>();
        regionMap.put("Miền Bắc", 0L);
        regionMap.put("Miền Trung", 0L);
        regionMap.put("Miền Nam", 0L);

        for (Object[] result : bookingCounts) {
            Region region = (Region) result[0];
            Long count = (Long) result[1];
            if (region != null && count != null) {
                switch (region) {
                    case NORTH:
                        regionMap.put("Miền Bắc", count);
                        break;
                    case CENTRAL:
                        regionMap.put("Miền Trung", count);
                        break;
                    case SOUTH:
                        regionMap.put("Miền Nam", count);
                        break;
                }
            }
        }

        return regionMap.entrySet().stream()
                .map(entry -> new RegionBookingResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<PaymentMethodResponse> getPaymentMethods() {
        List<Checkout> checkouts = checkoutRepository.findAll();
        List<PaymentMethodResponse> paymentMethods = new ArrayList<>();

        long totalCompletedCheckouts = checkouts.stream()
                .filter(checkout -> checkout.getPaymentStatus() != null && checkout.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();

        long vnpayCount = checkouts.stream()
                .filter(c -> c.getPaymentMethod() != null && "VNPAY".equalsIgnoreCase(c.getPaymentMethod()) && c.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();
        long officeCount = checkouts.stream()
                .filter(c -> c.getPaymentMethod() != null && "OFFICE".equalsIgnoreCase(c.getPaymentMethod()) && c.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();

        double vnpayPercentage = totalCompletedCheckouts > 0 ? (vnpayCount * 100.0) / totalCompletedCheckouts : 0.0;
        double officePercentage = totalCompletedCheckouts > 0 ? (officeCount * 100.0) / totalCompletedCheckouts : 0.0;

        paymentMethods.add(new PaymentMethodResponse("VNPAY", vnpayPercentage));
        paymentMethods.add(new PaymentMethodResponse("Thanh toán tại văn phòng", officePercentage));

        return paymentMethods;
    }

    private List<TourStatsResponse> getTourStats() {
        List<Tour> tours = tourRepository.findAll();
        List<TourStatsResponse> tourStats = new ArrayList<>();

        for (Tour tour : tours) {
            List<Booking> confirmedAndCompletedBookings = bookingRepository.findByTourAndBookingStatusIn(
                    tour.getTourId(), Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

            Long bookedSlots = confirmedAndCompletedBookings.stream()
                    .mapToLong(booking -> {
                        int adults = booking.getNumAdults() != null ? booking.getNumAdults() : 0;
                        int children = booking.getNumChildren() != null ? booking.getNumChildren() : 0;
                        return adults + children;
                    })
                    .sum();

            int bookingCount = confirmedAndCompletedBookings.size();

            Integer availableSlots = tour.getQuantity() != null
                    ? (tour.getQuantity() - bookedSlots.intValue())
                    : 0;

            Double averageRating = tour.getReviews() != null && !tour.getReviews().isEmpty()
                    ? tour.getReviews().stream()
                    .mapToDouble(review -> review.getRating() != null ? review.getRating() : 0.0)
                    .average()
                    .orElse(0.0)
                    : 0.0;

            TourStatsResponse tourStat = new TourStatsResponse(
                    "Tour" + String.format("%03d", tour.getTourId()),
                    tour.getTitle(),
                    bookedSlots.intValue(),
                    availableSlots,
                    tour.getPriceAdult(),
                    Math.round(averageRating * 10.0) / 10.0,
                    tour.getDuration(),
                    bookingCount
            );
            tourStats.add(tourStat);
        }

        return tourStats.stream()
                .sorted((t1, t2) -> Integer.compare(t2.getBookingCount(), t1.getBookingCount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<BookingStatsResponse> getLatestBookings() {
        List<Booking> bookings = bookingRepository.findTop5ByOrderByCreatedAtDesc();
        List<BookingStatsResponse> latestBookings = new ArrayList<>();

        for (Booking booking : bookings) {
            String regionStr;
            switch (booking.getTour().getRegion()) {
                case NORTH:
                    regionStr = "Miền Bắc";
                    break;
                case CENTRAL:
                    regionStr = "Miền Trung";
                    break;
                case SOUTH:
                    regionStr = "Miền Nam";
                    break;
                default:
                    regionStr = "Không xác định";
            }

            Checkout checkout = checkoutRepository.findByBookingBookingId(booking.getBookingId()).orElse(null);
            String paymentMethod = checkout != null && checkout.getPaymentMethod() != null ? checkout.getPaymentMethod() : "Không xác định";
            String status = booking.getBookingStatus() != null ? booking.getBookingStatus().toString() : "PENDING";

            BookingStatsResponse bookingStat = new BookingStatsResponse(
                    "Booking" + String.format("%03d", booking.getBookingId()),
                    booking.getUser().getUsername(),
                    booking.getTour().getTitle(),
                    booking.getTotalPrice(),
                    status,
                    paymentMethod,
                    regionStr,
                    booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : ""
            );
            latestBookings.add(bookingStat);
        }

        return latestBookings;
    }

    private List<MonthlyRevenueResponse> getMonthlyRevenues() {
        List<Checkout> checkouts = checkoutRepository.findAll();

        Map<String, Double> revenueByMonthFromCheckout = checkouts.stream()
                .filter(checkout -> checkout.getPaymentStatus() != null && checkout.getPaymentStatus() == PaymentStatus.COMPLETED)
                .filter(checkout -> checkout.getPaymentDate() != null)
                .collect(Collectors.groupingBy(
                        checkout -> checkout.getPaymentDate().format(DateTimeFormatter.ofPattern("MM-yyyy")),
                        Collectors.summingDouble(Checkout::getAmount)
                ));

        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();
        List<MonthlyRevenueResponse> monthlyRevenues = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            String monthYear = String.format("%02d-%d", month, currentYear);
            Double revenue = revenueByMonthFromCheckout.getOrDefault(monthYear, 0.0);
            monthlyRevenues.add(new MonthlyRevenueResponse(monthYear, revenue));
        }

        Map<String, String> monthMap = new HashMap<>();
        monthMap.put("01", "January");
        monthMap.put("02", "February");
        monthMap.put("03", "March");
        monthMap.put("04", "April");
        monthMap.put("05", "May");
        monthMap.put("06", "June");
        monthMap.put("07", "July");
        monthMap.put("08", "August");
        monthMap.put("09", "September");
        monthMap.put("10", "October");
        monthMap.put("11", "November");
        monthMap.put("12", "December");

        return monthlyRevenues.stream()
                .map(dto -> {
                    String[] parts = dto.getMonthYear().split("-");
                    String monthName = monthMap.get(parts[0]);
                    return new MonthlyRevenueResponse(monthName, dto.getRevenue());
                })
                .collect(Collectors.toList());
    }

    private List<DailyStatsResponse> getDailyStats() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29); // Last 30 days

        List<Checkout> checkouts = checkoutRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<User> users = userRepository.findAll(); // Lấy danh sách người dùng

        // Group revenue by date
        Map<LocalDate, Double> dailyRevenue = checkouts.stream()
                .filter(checkout -> checkout.getPaymentStatus() != null &&
                        checkout.getPaymentStatus() == PaymentStatus.COMPLETED)
                .filter(checkout -> checkout.getPaymentDate() != null)
                .filter(checkout -> {
                    LocalDate paymentDate = checkout.getPaymentDate().toLocalDate(); // Chuyển đổi LocalDateTime thành LocalDate
                    return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                })
                .collect(Collectors.groupingBy(
                        checkout -> checkout.getPaymentDate().toLocalDate(),
                        Collectors.summingDouble(Checkout::getAmount)
                ));

        // Group bookings by date
        Map<LocalDate, Long> dailyBookings = bookings.stream()
                .filter(booking -> booking.getCreatedAt() != null)
                .filter(booking -> {
                    LocalDate bookingDate = booking.getCreatedAt().toLocalDate();
                    return !bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate);
                })
                .collect(Collectors.groupingBy(
                        booking -> booking.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Group users by registration date
        Map<LocalDate, Long> dailyUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> {
                    LocalDate userDate = user.getCreatedAt().toLocalDate();
                    return !userDate.isBefore(startDate) && !userDate.isAfter(endDate);
                })
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Create daily stats for each day in the range
        List<DailyStatsResponse> dailyStats = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Double revenue = dailyRevenue.getOrDefault(date, 0.0);
            Long bookingCount = dailyBookings.getOrDefault(date, 0L);
            Long userCount = dailyUsers.getOrDefault(date, 0L); // Lấy số người dùng mới

            dailyStats.add(new DailyStatsResponse(
                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    date.format(DateTimeFormatter.ofPattern("dd/MM")),
                    revenue,
                    bookingCount.intValue(),
                    userCount.intValue() // Thêm totalUsers
            ));
        }

        return dailyStats;
    }

    // NEW METHOD: Yearly Statistics
    private List<YearlyStatsResponse> getYearlyStats() {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 4; // Last 5 years including current year

        List<Checkout> checkouts = checkoutRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        // Group revenue by year
        Map<Integer, Double> yearlyRevenue = checkouts.stream()
                .filter(checkout -> checkout.getPaymentStatus() != null &&
                        checkout.getPaymentStatus() == PaymentStatus.COMPLETED)
                .filter(checkout -> checkout.getPaymentDate() != null)
                .filter(checkout -> checkout.getPaymentDate().getYear() >= startYear)
                .collect(Collectors.groupingBy(
                        checkout -> checkout.getPaymentDate().getYear(),
                        Collectors.summingDouble(Checkout::getAmount)
                ));

        // Group bookings by year
        Map<Integer, Long> yearlyBookings = bookings.stream()
                .filter(booking -> booking.getCreatedAt() != null)
                .filter(booking -> booking.getCreatedAt().getYear() >= startYear)
                .collect(Collectors.groupingBy(
                        booking -> booking.getCreatedAt().getYear(),
                        Collectors.counting()
                ));

        // Group users by year (registration year)
        Map<Integer, Long> yearlyUsers = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> user.getCreatedAt().getYear() >= startYear)
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().getYear(),
                        Collectors.counting()
                ));

        // Create yearly stats
        List<YearlyStatsResponse> yearlyStats = new ArrayList<>();
        for (int year = startYear; year <= currentYear; year++) {
            Double revenue = yearlyRevenue.getOrDefault(year, 0.0);
            Long bookingCount = yearlyBookings.getOrDefault(year, 0L);
            Long userCount = yearlyUsers.getOrDefault(year, 0L);

            yearlyStats.add(new YearlyStatsResponse(
                    year,
                    revenue,
                    bookingCount.intValue(),
                    userCount.intValue()
            ));
        }

        return yearlyStats;
    }

    public StatsByDateRangeResponse getStatsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Checkout> checkouts = checkoutRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        // Revenue in date range
        Double totalRevenue = checkouts.stream()
                .filter(checkout -> checkout.getPaymentStatus() != null &&
                        checkout.getPaymentStatus() == PaymentStatus.COMPLETED)
                .filter(checkout -> checkout.getPaymentDate() != null)
                .filter(checkout -> {
                    LocalDate paymentDate = checkout.getPaymentDate().toLocalDate(); // Chuyển đổi LocalDateTime thành LocalDate
                    return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                })
                .mapToDouble(Checkout::getAmount)
                .sum();

        // Bookings in date range
        Long totalBookings = bookings.stream()
                .filter(booking -> booking.getCreatedAt() != null)
                .filter(booking -> {
                    LocalDate bookingDate = booking.getCreatedAt().toLocalDate();
                    return !bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate);
                })
                .count();

        // New users in date range
        Long newUsers = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> {
                    LocalDate userDate = user.getCreatedAt().toLocalDate();
                    return !userDate.isBefore(startDate) && !userDate.isAfter(endDate);
                })
                .count();

        return new StatsByDateRangeResponse(
                startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                totalRevenue,
                totalBookings.intValue(),
                newUsers.intValue()
        );
    }
}