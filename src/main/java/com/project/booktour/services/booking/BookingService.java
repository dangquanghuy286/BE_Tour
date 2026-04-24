package com.project.booktour.services.booking;

import com.project.booktour.dtos.BookingDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.*;
import com.project.booktour.repositories.*;
import com.project.booktour.services.PromotionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BookingService implements IBookingService {
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final PromotionRepository promotionRepository;
    private final BookingRepository bookingRepository;
    private final CheckoutRepository checkoutRepository;
    private final TourImageRepository tourImageRepository;
    private final PromotionService promotionService;
    private final ModelMapper modelMapper;

    public boolean hasUserUnpaidBooking(Long userId) {
        List<Booking> userBookings = bookingRepository.findByUserUserId(userId);

        for (Booking booking : userBookings) {
            if (booking.getBookingStatus() == BookingStatus.PENDING ||
                    booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                Optional<Checkout> checkout = checkoutRepository.findByBookingBookingId(booking.getBookingId());
                if (checkout.isPresent() && checkout.get().getPaymentStatus() == PaymentStatus.PENDING) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Booking createBooking(BookingDTO bookingDTO) throws Exception {
        User user = userRepository.findById(bookingDTO.getUserId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + bookingDTO.getUserId()));
        Tour tour = tourRepository.findById(bookingDTO.getTourId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với id: " + bookingDTO.getTourId()));

        Integer totalBookedTickets = tourRepository.getTotalBookedTicketsByTourId(bookingDTO.getTourId());
        Integer requestedTickets = bookingDTO.getNumAdults() + bookingDTO.getNumChildren();
        Integer availableSlots = tour.getQuantity() - (totalBookedTickets != null ? totalBookedTickets : 0);

        if (availableSlots < requestedTickets) {
            throw new Exception("Không đủ slot trống cho tour này. Số slot còn lại: " + availableSlots);
        }

        Double totalPrice = (bookingDTO.getNumAdults() * tour.getPriceAdult()) +
                (bookingDTO.getNumChildren() * tour.getPriceChild());

        Promotion promotion = null;
        if (bookingDTO.getPromotionCode() != null && !bookingDTO.getPromotionCode().isEmpty()) {
            promotion = promotionService.validatePromotion(bookingDTO.getPromotionCode());
            if (hasUserUsedPromotion(bookingDTO.getUserId(), promotion.getPromotionId())) {
                throw new IllegalArgumentException("Bạn đã sử dụng mã giảm giá này trước đó!");
            }
            totalPrice = totalPrice - (totalPrice * promotion.getDiscount() / 100);
            promotionService.decreasePromotionQuantity(promotion.getPromotionId());
            bookingDTO.setPromotionId(promotion.getPromotionId());
        }

        Booking booking = new Booking();
        booking.setNumAdults(bookingDTO.getNumAdults());
        booking.setNumChildren(bookingDTO.getNumChildren());
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(bookingDTO.getBookingStatus() != null ? bookingDTO.getBookingStatus() : BookingStatus.PENDING);
        booking.setSpecialRequests(bookingDTO.getSpecialRequests());
        booking.setFullName(bookingDTO.getFullName());
        booking.setPhoneNumber(bookingDTO.getPhoneNumber());
        booking.setEmail(bookingDTO.getEmail());
        booking.setAddress(bookingDTO.getAddress());
        booking.setUser(user);
        booking.setTour(tour);
        booking.setPromotion(promotion);
        Booking savedBooking = bookingRepository.save(booking);

        Checkout checkout = Checkout.builder()
                .booking(savedBooking)
                .paymentMethod(bookingDTO.getPaymentMethod() != null ? bookingDTO.getPaymentMethod() : "OFFICE")
                .paymentDetails(bookingDTO.getPaymentMethod() != null && bookingDTO.getPaymentMethod().equalsIgnoreCase("VNPAY")
                        ? "Khởi tạo thanh toán VNPAY" : "Thanh toán tại văn phòng")
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId("BOOK-" + savedBooking.getBookingId())
                .paymentDate(LocalDateTime.now())
                .build();
        checkoutRepository.save(checkout);

        return savedBooking;
    }

    @Override
    public Page<BookingDTO> getAllBookings(String keyword, PageRequest pageRequest) {
        Page<Booking> bookingPage = bookingRepository.findAll(keyword, pageRequest);
        LocalDate currentDate = LocalDate.now();

        return bookingPage.map(booking -> {
            Optional<Checkout> checkoutOpt = checkoutRepository.findByBookingBookingId(booking.getBookingId());
            String paymentMethod = checkoutOpt.map(Checkout::getPaymentMethod).orElse(null);
            PaymentStatus paymentStatus = checkoutOpt.map(Checkout::getPaymentStatus).orElse(null);
            String transactionCode = checkoutOpt.map(Checkout::getTransactionId).orElse(null);
            LocalDateTime paymentDate = checkoutOpt.map(Checkout::getPaymentDate).orElse(null);

            String formattedTourId = String.format("Tour%03d", booking.getTour().getTourId());

            if (booking.getBookingStatus() == BookingStatus.CONFIRMED && booking.getTour().getEndDate().isBefore(currentDate)) {
                booking.setBookingStatus(BookingStatus.COMPLETED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }

            List<String> imageUrls = tourImageRepository.findByTourTourId(booking.getTour().getTourId()).stream()
                    .map(TourImage::getImageUrl)
                    .filter(Objects::nonNull)
                    .map(url -> "http://localhost:8088/api/v1/tours/images/" + url)
                    .collect(Collectors.toList());

            String imageUrl = imageUrls.isEmpty() ? "http://localhost:8088/api/v1/tours/images/notfound.jpeg" : imageUrls.get(0);

            return BookingDTO.builder()
                    .title(booking.getTour().getTitle())
                    .bookingId(booking.getBookingId())
                    .userId(booking.getUser().getUserId())
                    .tourId(booking.getTour().getTourId())
                    .formattedTourId(formattedTourId)
                    .img(imageUrl)
                    .numAdults(booking.getNumAdults())
                    .numChildren(booking.getNumChildren())
                    .priceAdult(String.format("%,.0f VNĐ", booking.getTour().getPriceAdult()))
                    .priceChild(String.format("%,.0f VNĐ", booking.getTour().getPriceChild()))
                    .totalPrice(booking.getTotalPrice())
                    .bookingStatus(booking.getBookingStatus())
                    .specialRequests(booking.getSpecialRequests())
                    .promotionId(booking.getPromotion() != null ? booking.getPromotion().getPromotionId() : null)
                    .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                    .promotionDiscount(booking.getPromotion() != null ? booking.getPromotion().getDiscount() : null)
                    .promotionDescription(booking.getPromotion() != null ? booking.getPromotion().getDescription() : null)
                    .fullName(booking.getFullName())
                    .email(booking.getEmail())
                    .address(booking.getAddress())
                    .phoneNumber(booking.getPhoneNumber())
                    .createdAt(booking.getCreatedAt())
                    .updatedAt(booking.getUpdatedAt())
                    .paymentMethod(paymentMethod)
                    .paymentStatus(paymentStatus != null ? paymentStatus.name() : null)
                    .transactionCode(transactionCode)
                    .paymentDate(paymentDate)
                    .startDate(booking.getTour().getStartDate())
                    .endDate(booking.getTour().getEndDate())
                    .departurePoint(booking.getTour().getDeparturePoint())
                    .build();
        });
    }

    @Override
    public BookingDTO getBooking(Long id) throws DataNotFoundException {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy booking với id: " + id));

        Optional<Checkout> checkoutOpt = checkoutRepository.findByBookingBookingId(booking.getBookingId());
        String paymentMethod = checkoutOpt.map(Checkout::getPaymentMethod).orElse(null);
        PaymentStatus paymentStatus = checkoutOpt.map(Checkout::getPaymentStatus).orElse(null);
        String transactionCode = checkoutOpt.map(Checkout::getTransactionId).orElse(null);
        LocalDateTime paymentDate = checkoutOpt.map(Checkout::getPaymentDate).orElse(null);
        Tour tour = booking.getTour();

        LocalDate currentDate = LocalDate.now();
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED && tour.getEndDate().isBefore(currentDate)) {
            booking.setBookingStatus(BookingStatus.COMPLETED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }

        List<String> imageUrls = tourImageRepository.findByTourTourId(tour.getTourId()).stream()
                .map(TourImage::getImageUrl)
                .filter(Objects::nonNull)
                .map(url -> "http://localhost:8088/api/v1/tours/images/" + url)
                .collect(Collectors.toList());

        String imageUrl = imageUrls.isEmpty() ? "http://localhost:8088/api/v1/tours/images/notfound.jpeg" : imageUrls.get(0);

        return BookingDTO.builder()
                .bookingId(booking.getBookingId())
                .title(tour.getTitle())
                .userId(booking.getUser().getUserId())
                .tourId(tour.getTourId())
                .formattedTourId(String.format("Tour%03d", tour.getTourId()))
                .img(imageUrl)
                .numAdults(booking.getNumAdults())
                .numChildren(booking.getNumChildren())
                .totalPrice(booking.getTotalPrice())
                .bookingStatus(booking.getBookingStatus())
                .specialRequests(booking.getSpecialRequests())
                .promotionId(booking.getPromotion() != null ? booking.getPromotion().getPromotionId() : null)
                .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                .promotionDiscount(booking.getPromotion() != null ? booking.getPromotion().getDiscount() : null)
                .promotionDescription(booking.getPromotion() != null ? booking.getPromotion().getDescription() : null)
                .fullName(booking.getFullName())
                .email(booking.getEmail())
                .address(booking.getAddress())
                .phoneNumber(booking.getPhoneNumber())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .priceAdult(String.format("%,.0f VNĐ", tour.getPriceAdult()))
                .priceChild(String.format("%,.0f VNĐ", tour.getPriceChild()))
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus != null ? paymentStatus.name() : null)
                .transactionCode(transactionCode)
                .paymentDate(paymentDate)
                .departurePoint(tour.getDeparturePoint())
                .build();
    }

    @Override
    public Booking updateBooking(Long id, BookingDTO bookingDTO) throws DataNotFoundException {
        if (bookingDTO == null) {
            throw new IllegalArgumentException("BookingDTO không được để trống");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy booking với id: " + id));
        System.out.println("Đang cập nhật booking với ID: " + id);

        User user = userRepository.findById(bookingDTO.getUserId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + bookingDTO.getUserId()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        System.out.println("Đã cập nhật người dùng với ID: " + bookingDTO.getUserId());

        Tour tour = tourRepository.findById(bookingDTO.getTourId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với id: " + bookingDTO.getTourId()));
        Promotion promotion = bookingDTO.getPromotionId() != null ? promotionRepository.findById(bookingDTO.getPromotionId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy khuyến mãi với id: " + bookingDTO.getPromotionId())) : null;

        booking.setNumAdults(bookingDTO.getNumAdults() != null ? bookingDTO.getNumAdults() : booking.getNumAdults());
        booking.setNumChildren(bookingDTO.getNumChildren() != null ? bookingDTO.getNumChildren() : booking.getNumChildren());
        booking.setTotalPrice(bookingDTO.getTotalPrice() != null ? bookingDTO.getTotalPrice() : booking.getTotalPrice());
        booking.setSpecialRequests(bookingDTO.getSpecialRequests() != null ? bookingDTO.getSpecialRequests() : booking.getSpecialRequests());
        booking.setFullName(bookingDTO.getFullName() != null ? bookingDTO.getFullName() : booking.getFullName());
        booking.setPhoneNumber(bookingDTO.getPhoneNumber() != null ? bookingDTO.getPhoneNumber() : booking.getPhoneNumber());
        booking.setEmail(bookingDTO.getEmail() != null ? bookingDTO.getEmail() : booking.getEmail());
        booking.setAddress(bookingDTO.getAddress() != null ? bookingDTO.getAddress() : booking.getAddress());
        booking.setUser(user);
        booking.setTour(tour);
        booking.setPromotion(promotion);

        if (bookingDTO.getBookingStatus() != null) {
            try {
                BookingStatus newStatus = BookingStatus.valueOf(bookingDTO.getBookingStatus().toString());
                if (booking.getBookingStatus() == BookingStatus.PENDING && newStatus == BookingStatus.CONFIRMED) {
                    booking.setBookingStatus(BookingStatus.CONFIRMED);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái booking không hợp lệ: " + bookingDTO.getBookingStatus());
            }
        }

        booking.setUpdatedAt(LocalDateTime.now());
        System.out.println("Đang lưu booking với ID: " + id);

        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("Đã lưu booking thành công với ID: " + savedBooking.getBookingId());
        return savedBooking;
    }

    @Transactional
    public void cancelBooking(Long id) throws DataNotFoundException {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy booking: " + id));

        if (booking.getBookingStatus() != BookingStatus.PENDING &&
                booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể hủy booking ở trạng thái PENDING hoặc CONFIRMED");
        }

        if (booking.getTour().getStartDate().isBefore(LocalDate.now().plusDays(3))) {
            throw new IllegalStateException("Không thể hủy booking trong vòng 3 ngày trước khi tour bắt đầu");
        }

        Tour tour = booking.getTour();
        int cancelledTickets = booking.getNumAdults() + booking.getNumChildren();

        Integer totalBookedTickets = tourRepository.getTotalBookedTicketsByTourId(tour.getTourId());
        if (totalBookedTickets == null) {
            totalBookedTickets = 0;
        }

        Integer currentAvailableSlots = tour.getQuantity() - totalBookedTickets;
        Integer newAvailableSlots = currentAvailableSlots + cancelledTickets;
        Integer newTotalBookedTickets = totalBookedTickets - cancelledTickets;
        tour.setQuantity(newTotalBookedTickets + newAvailableSlots);

        if (booking.getPromotion() != null) {
            promotionService.increasePromotionQuantity(booking.getPromotion().getPromotionId());
        }

        booking.setTotalPrice(0.0);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());

        Optional<Checkout> checkoutOpt = checkoutRepository.findByBookingBookingId(booking.getBookingId());
        if (checkoutOpt.isPresent()) {
            Checkout checkout = checkoutOpt.get();
            checkout.setAmount(0.0);
            checkout.setPaymentStatus(PaymentStatus.PENDING);
            checkout.setPaymentDetails("Đã hủy booking - " + checkout.getPaymentDetails());
            checkoutRepository.save(checkout);
        }

        tourRepository.save(tour);
        bookingRepository.save(booking);

        System.out.println("Đã hủy booking ID: " + id + " - Trả lại " + cancelledTickets + " vé cho tour ID: " + tour.getTourId());
        System.out.println("Available slots trước khi hủy: " + currentAvailableSlots);
        System.out.println("Available slots sau khi hủy: " + newAvailableSlots);
        System.out.println("Quantity được cập nhật thành: " + tour.getQuantity());
    }

    @Override
    public List<BookingDTO> findByUserId(Long userId) throws DataNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));

        List<Booking> bookings = bookingRepository.findByUserUserId(userId);
        LocalDate currentDate = LocalDate.now();

        return bookings.stream().map(booking -> {
            Optional<Checkout> checkoutOpt = checkoutRepository.findByBookingBookingId(booking.getBookingId());
            String paymentMethod = checkoutOpt.map(Checkout::getPaymentMethod).orElse(null);
            PaymentStatus paymentStatus = checkoutOpt.map(Checkout::getPaymentStatus).orElse(null);
            String transactionCode = checkoutOpt.map(Checkout::getTransactionId).orElse(null);
            LocalDateTime paymentDate = checkoutOpt.map(Checkout::getPaymentDate).orElse(null);
            Tour tour = booking.getTour();

            if (booking.getBookingStatus() == BookingStatus.CONFIRMED && tour.getEndDate().isBefore(currentDate)) {
                booking.setBookingStatus(BookingStatus.COMPLETED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }

            List<String> imageUrls = tourImageRepository.findByTourTourId(tour.getTourId()).stream()
                    .map(TourImage::getImageUrl)
                    .filter(Objects::nonNull)
                    .map(url -> "http://localhost:8088/api/v1/tours/images/" + url)
                    .collect(Collectors.toList());

            String imageUrl = imageUrls.isEmpty() ? "http://localhost:8088/api/v1/tours/images/notfound.jpeg" : imageUrls.get(0);

            return BookingDTO.builder()
                    .bookingId(booking.getBookingId())
                    .title(tour.getTitle())
                    .userId(booking.getUser().getUserId())
                    .tourId(tour.getTourId())
                    .formattedTourId(String.format("Tour%03d", tour.getTourId()))
                    .img(imageUrl)
                    .numAdults(booking.getNumAdults())
                    .numChildren(booking.getNumChildren())
                    .totalPrice(booking.getTotalPrice())
                    .bookingStatus(booking.getBookingStatus())
                    .specialRequests(booking.getSpecialRequests())
                    .promotionId(booking.getPromotion() != null ? booking.getPromotion().getPromotionId() : null)
                    .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                    .promotionDiscount(booking.getPromotion() != null ? booking.getPromotion().getDiscount() : null)
                    .promotionDescription(booking.getPromotion() != null ? booking.getPromotion().getDescription() : null)
                    .fullName(booking.getFullName())
                    .email(booking.getEmail())
                    .address(booking.getAddress())
                    .phoneNumber(booking.getPhoneNumber())
                    .startDate(tour.getStartDate())
                    .endDate(tour.getEndDate())
                    .priceAdult(String.format("%,.0f VNĐ", tour.getPriceAdult()))
                    .priceChild(String.format("%,.0f VNĐ", tour.getPriceChild()))
                    .createdAt(booking.getCreatedAt())
                    .updatedAt(booking.getUpdatedAt())
                    .paymentMethod(paymentMethod)
                    .paymentStatus(paymentStatus != null ? paymentStatus.name() : null)
                    .transactionCode(transactionCode)
                    .paymentDate(paymentDate)
                    .departurePoint(tour.getDeparturePoint())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<BookingDTO> findByUserIdAndStatus(Long userId, BookingStatus status) throws DataNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));

        List<Booking> bookings = bookingRepository.findByUserUserIdAndBookingStatus(userId, status);
        LocalDate currentDate = LocalDate.now();

        return bookings.stream().map(booking -> {
            Optional<Checkout> checkoutOpt = checkoutRepository.findByBookingBookingId(booking.getBookingId());
            String paymentMethod = checkoutOpt.map(Checkout::getPaymentMethod).orElse(null);
            PaymentStatus paymentStatus = checkoutOpt.map(Checkout::getPaymentStatus).orElse(null);
            String transactionCode = checkoutOpt.map(Checkout::getTransactionId).orElse(null);
            LocalDateTime paymentDate = checkoutOpt.map(Checkout::getPaymentDate).orElse(null);
            Tour tour = booking.getTour();

            if (booking.getBookingStatus() == BookingStatus.CONFIRMED && tour.getEndDate().isBefore(currentDate)) {
                booking.setBookingStatus(BookingStatus.COMPLETED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }

            List<String> imageUrls = tourImageRepository.findByTourTourId(tour.getTourId()).stream()
                    .map(TourImage::getImageUrl)
                    .filter(Objects::nonNull)
                    .map(url -> "http://localhost:8088/api/v1/tours/images/" + url)
                    .collect(Collectors.toList());

            String imageUrl = imageUrls.isEmpty() ? "http://localhost:8088/api/v1/tours/images/notfound.jpeg" : imageUrls.get(0);

            return BookingDTO.builder()
                    .bookingId(booking.getBookingId())
                    .title(tour.getTitle())
                    .userId(booking.getUser().getUserId())
                    .tourId(tour.getTourId())
                    .formattedTourId(String.format("Tour%03d", tour.getTourId()))
                    .img(imageUrl)
                    .numAdults(booking.getNumAdults())
                    .numChildren(booking.getNumChildren())
                    .totalPrice(booking.getTotalPrice())
                    .bookingStatus(booking.getBookingStatus())
                    .specialRequests(booking.getSpecialRequests())
                    .promotionId(booking.getPromotion() != null ? booking.getPromotion().getPromotionId() : null)
                    .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                    .promotionDiscount(booking.getPromotion() != null ? booking.getPromotion().getDiscount() : null)
                    .promotionDescription(booking.getPromotion() != null ? booking.getPromotion().getDescription() : null)
                    .fullName(booking.getFullName())
                    .email(booking.getEmail())
                    .address(booking.getAddress())
                    .phoneNumber(booking.getPhoneNumber())
                    .startDate(tour.getStartDate())
                    .endDate(tour.getEndDate())
                    .priceAdult(String.format("%,.0f VNĐ", tour.getPriceAdult()))
                    .priceChild(String.format("%,.0f VNĐ", tour.getPriceChild()))
                    .createdAt(booking.getCreatedAt())
                    .updatedAt(booking.getUpdatedAt())
                    .paymentMethod(paymentMethod)
                    .paymentStatus(paymentStatus != null ? paymentStatus.name() : null)
                    .transactionCode(transactionCode)
                    .paymentDate(paymentDate)
                    .departurePoint(tour.getDeparturePoint())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public boolean hasUserBookedTour(Long userId, Long tourId) {
        try {
            return bookingRepository.existsByUserUserIdAndTourTourId(userId, tourId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<Booking> findById(Long id) throws DataNotFoundException {
        return Optional.of(bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy booking với id: " + id)));
    }

    @Override
    public boolean hasUserUsedPromotion(Long userId, Long promotionId) {
        List<BookingStatus> validStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        return bookingRepository.existsByUserUserIdAndPromotionPromotionIdAndBookingStatusIn(
                userId, promotionId, validStatuses);
    }

    @Override
    public List<Booking> findBookingsByUserId(Long userId) {
        return bookingRepository.findByUserUserId(userId);
    }


    public Long countBookingsByTourId(Long tourId) {
        return bookingRepository.countBookingsByTourId(tourId);
    }


    public List<Booking> findBookingsByUserIdAndStatus(Long userId, BookingStatus status) {
        return bookingRepository.findByUserUserIdAndBookingStatus(userId, status);
    }
}