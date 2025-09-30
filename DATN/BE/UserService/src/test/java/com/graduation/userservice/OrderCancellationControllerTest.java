//package com.graduation.userservice;
//
//import com.graduation.userservice.constant.Constant;
//import com.graduation.userservice.payload.response.BaseResponse;
//import com.graduation.userservice.service.OrderCommandHandlerService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.ResponseEntity;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*; // Make sure Mockito.lenient() is available
//
///**
// * Unit tests for OrderController.cancelOrder - 5 Specific Status Test Cases
// */
//@ExtendWith(MockitoExtension.class)
//class OrderCancellationControllerTest {
//
//    @Mock
//    private OrderRepository orderRepository;
//
//    @Mock
//    private OrderCommandHandlerService orderCommandHandlerService;
//
//    @InjectMocks
//    private OrderController orderController;
//
//    private Long orderId;
//    private String cancelReason;
//
//    @BeforeEach
//    void setUp() {
//        orderId = 123L;
//        cancelReason = "Customer request";
//    }
//
//    /**
//     * TEST CASE 1: Order status = CREATED, no payment lock (SHOULD SUCCEED)
//     * This is the happy path where order can be cancelled without any conflicts
//     */
//    @Test
//    @DisplayName("Should successfully cancel order when status is CREATED and no payment lock exists")
//    void cancelOrder_whenStatusCreatedAndNoPaymentLock_shouldSucceed() {
//        // Arrange
//        Order mockOrder = createMockOrder(OrderStatus.CREATED);
//        Order updatedOrder = createMockOrder(OrderStatus.CANCELLATION_PENDING);
//
//        // Handle TWO sequential calls to findById
//        when(orderRepository.findById(orderId))
//                .thenReturn(Optional.of(mockOrder))      // 1st call - returns CREATED
//                .thenReturn(Optional.of(updatedOrder));  // 2nd call - returns CANCELLATION_PENDING
//
//        // No payment lock exists
//        when(orderCommandHandlerService.isPaymentInProgress(anyString())).thenReturn(false);
//
//        // Atomic status update succeeds
//        when(orderCommandHandlerService.updateOrderStatusAtomically(
//                eq(orderId), eq(OrderStatus.CREATED), eq(OrderStatus.CANCELLATION_PENDING),
//                anyString(), eq("USER_REQUEST"))).thenReturn(true);
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(1, baseResponse.getStatus());
//        assertEquals(Constant.ORDER_CANCELLATION_INITIATED, baseResponse.getMsg());
//
//        // Verify interactions
//        verify(orderRepository, times(2)).findById(orderId); // Called twice!
//        verify(orderCommandHandlerService, times(1)).isPaymentInProgress(anyString());
//        verify(orderCommandHandlerService, times(1)).updateOrderStatusAtomically(
//                eq(orderId), eq(OrderStatus.CREATED), eq(OrderStatus.CANCELLATION_PENDING),
//                anyString(), eq("USER_REQUEST"));
//        verify(orderCommandHandlerService, times(1)).initiateCancellation(eq(updatedOrder), anyString());
//    }
//
//    /**
//     * TEST CASE 2: Order status = CREATED, with payment lock (SHOULD FAIL)
//     * This tests the payment lock protection mechanism
//     */
//    @Test
//    @DisplayName("Should fail to cancel order when status is CREATED but payment is in progress")
//    void cancelOrder_whenStatusCreatedButPaymentInProgress_shouldFail() {
//        // Arrange
//        Order mockOrder = createMockOrder(OrderStatus.CREATED);
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
//
//        // Payment lock exists
//        when(orderCommandHandlerService.isPaymentInProgress(anyString())).thenReturn(true);
//        when(orderCommandHandlerService.getPaymentLockHolder(anyString())).thenReturn("payment-service-123");
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(0, baseResponse.getStatus()); // Failure code
//        assertEquals(Constant.ERROR_PAYMENT_IN_PROGRESS, baseResponse.getMsg());
//
//        // Verify payment lock was checked but no status update occurred
//        verify(orderCommandHandlerService, times(1)).isPaymentInProgress(anyString());
//        verify(orderCommandHandlerService, times(1)).getPaymentLockHolder(anyString());
//        verify(orderCommandHandlerService, never()).updateOrderStatusAtomically(anyLong(), any(), any(), anyString(), anyString());
//        verify(orderCommandHandlerService, never()).initiateCancellation(any(), anyString());
//    }
//
//    /**
//     * TEST CASE 3: Order status = CONFIRMED (SHOULD SUCCEED)
//     * This tests successful cancellation of confirmed orders
//     */
//    @Test
//    @DisplayName("Should successfully cancel order when status is CONFIRMED")
//    void cancelOrder_whenStatusConfirmed_shouldSucceed() {
//        // Arrange
//        Order mockOrder = createMockOrder(OrderStatus.CONFIRMED);
//        Order updatedOrder = createMockOrder(OrderStatus.CANCELLATION_PENDING);
//
//        // Handle TWO sequential calls to findById
//        // 1st call returns mockOrder (CONFIRMED)
//        // 2nd call returns updatedOrder (CANCELLATION_PENDING) after the atomic update
//        when(orderRepository.findById(orderId))
//                .thenReturn(Optional.of(mockOrder))       // First call to findById
//                .thenReturn(Optional.of(updatedOrder));  // Second call to findById (after atomic update)
//
//        // Atomic status update succeeds
//        when(orderCommandHandlerService.updateOrderStatusAtomically(
//                eq(orderId), eq(OrderStatus.CONFIRMED), eq(OrderStatus.CANCELLATION_PENDING),
//                anyString(), eq("USER_REQUEST"))).thenReturn(true);
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(1, baseResponse.getStatus()); // Success code
//        assertEquals(Constant.ORDER_CANCELLATION_INITIATED, baseResponse.getMsg());
//
//        // Verify no payment lock check for CONFIRMED orders
//        verify(orderCommandHandlerService, never()).isPaymentInProgress(anyString());
//        verify(orderCommandHandlerService, times(1)).updateOrderStatusAtomically(
//                eq(orderId), eq(OrderStatus.CONFIRMED), eq(OrderStatus.CANCELLATION_PENDING),
//                anyString(), eq("USER_REQUEST"));
//        verify(orderCommandHandlerService, times(1)).initiateCancellation(eq(updatedOrder), anyString());
//        // Verify findById was called twice
//        verify(orderRepository, times(2)).findById(orderId);
//    }
//
//    /**
//     * TEST CASE 4: Order status = DELIVERED (SHOULD FAIL)
//     * This tests that delivered orders cannot be cancelled
//     */
//    @Test
//    @DisplayName("Should fail to cancel order when status is DELIVERED")
//    void cancelOrder_whenStatusDelivered_shouldFail() {
//        // Arrange
//        Order mockOrder = createMockOrder(OrderStatus.DELIVERED);
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(0, baseResponse.getStatus()); // Failure code
//        assertEquals(Constant.ERROR_FAILED_TO_CANCEL_ORDER, baseResponse.getMsg());
//
//        // Verify no processing occurs for delivered orders
//        verify(orderCommandHandlerService, never()).isPaymentInProgress(anyString());
//        verify(orderCommandHandlerService, never()).updateOrderStatusAtomically(anyLong(), any(), any(), anyString(), anyString());
//        verify(orderCommandHandlerService, never()).initiateCancellation(any(), anyString());
//    }
//
//    /**
//     * TEST CASE 5: Order status = CANCELLED (SHOULD FAIL)
//     * This tests that already cancelled orders cannot be cancelled again
//     */
//    @Test
//    @DisplayName("Should fail to cancel order when status is already CANCELLED")
//    void cancelOrder_whenStatusAlreadyCancelled_shouldFail() {
//        // Arrange
//        Order mockOrder = createMockOrder(OrderStatus.CANCELLED);
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(0, baseResponse.getStatus()); // Failure code
//        assertEquals(Constant.ERROR_FAILED_TO_CANCEL_ORDER, baseResponse.getMsg());
//
//
//        // Verify no processing occurs for already cancelled orders
//        verify(orderCommandHandlerService, never()).isPaymentInProgress(anyString());
//        verify(orderCommandHandlerService, never()).updateOrderStatusAtomically(anyLong(), any(), any(), anyString(), anyString());
//        verify(orderCommandHandlerService, never()).initiateCancellation(any(), anyString());
//    }
//
//    /**
//     * BONUS TEST CASE: Order not found
//     * This tests the edge case when order doesn't exist
//     */
//    @Test
//    @DisplayName("Should fail when order does not exist")
//    void cancelOrder_whenOrderNotFound_shouldFail() {
//        // Arrange
//        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(0, baseResponse.getStatus()); // Failure code
//        assertEquals(Constant.ORDER_NOT_FOUND, baseResponse.getMsg());
//
//        // Verify no further processing occurs
//        verify(orderCommandHandlerService, never()).isPaymentInProgress(anyString());
//        verify(orderCommandHandlerService, never()).updateOrderStatusAtomically(anyLong(), any(), any(), anyString(), anyString());
//        verify(orderCommandHandlerService, never()).initiateCancellation(any(), anyString());
//    }
//
//    /**
//     * BONUS TEST CASE: Atomic status update fails
//     * This tests race condition handling during status updates
//     */
//    @Test
//    @DisplayName("Should handle atomic status update failure gracefully")
//    void cancelOrder_whenAtomicUpdateFails_shouldHandleGracefully() {
//        // Arrange
//        Order mockOrder = createMockOrder(OrderStatus.CREATED);
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
//        when(orderCommandHandlerService.isPaymentInProgress(anyString())).thenReturn(false);
//
//        // Atomic status update fails (race condition)
//        when(orderCommandHandlerService.updateOrderStatusAtomically(
//                eq(orderId), eq(OrderStatus.CREATED), eq(OrderStatus.CANCELLATION_PENDING),
//                anyString(), eq("USER_REQUEST"))).thenReturn(false);
//
//        // Act
//        ResponseEntity<?> response = orderController.cancelOrder(orderId, cancelReason);
//
//        // Assert
//        assertNotNull(response);
//        BaseResponse<?> baseResponse = (BaseResponse<?>) response.getBody();
//        assertEquals(0, baseResponse.getStatus()); // Failure code
//
//
//
//        // Verify no saga initiation occurs when atomic update fails
//        verify(orderCommandHandlerService, never()).initiateCancellation(any(), anyString());
//    }
//
//    /**
//     * Helper method to create a mock Order with specified status
//     */
//    private Order createMockOrder(OrderStatus status) {
//        Order order = mock(Order.class);
//        // Use lenient() for stubbings that might not be used in every test case
//        lenient().when(order.getId()).thenReturn(orderId);
//        lenient().when(order.getStatus()).thenReturn(status);
//        lenient().when(order.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
//        lenient().when(order.getUserId()).thenReturn("user-123");
//        return order;
//    }
//}