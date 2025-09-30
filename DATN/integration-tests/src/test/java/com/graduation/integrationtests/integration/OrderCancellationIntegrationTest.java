// src/test/java/com/graduation/integrationtests/OrderCancellationIntegrationTest.java
package com.graduation.integrationtests.integration;

import com.graduation.integrationtests.operations.OrderOperations;
import com.graduation.integrationtests.model.OrderInputModel;
import com.graduation.integrationtests.utils.WebDriverUtils;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for order cancellation workflows
 *
 * This test validates the order cancellation functionality across different order statuses:
 * 1. Cancellation attempt during CREATED status (should fail)
 * 2. Cancellation attempt during PAYMENT CONFIRMED status (should succeed)
 *
 * Test Architecture:
 * - Uses Page Object Model pattern through OrderOperations
 * - Separates test data (OrderInputModel) from test logic
 * - Tests both successful and failed cancellation scenarios
 * - Verifies proper notification messages are displayed
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderCancellationIntegrationTest {

    @Autowired
    private OrderOperations orderOperations;

    @Value("${browser.type:chrome}")
    private String browserType;

    @Value("${test.timeout:20}")
    private int testTimeout;

    // Test data storage for verification
    private static String cancelledOrderIdCreated;
    private static String cancelledOrderIdConfirmed;

    /**
     * Setup method - runs once before all tests
     * Initializes test environment for cancellation tests
     */
    @BeforeAll
    static void setupClass() {
        System.out.println("🔧 Setting up order cancellation integration test environment...");
        System.out.println("📋 Test Plan:");
        System.out.println("   1. Test cancellation in CREATED status (expect failure)");
        System.out.println("   2. Test cancellation in PAYMENT CONFIRMED status (expect success)");
    }

    /**
     * Setup method - runs before each test
     * Ensures clean state for each test
     */
    @BeforeEach
    void setUp() {
        System.out.println("🚀 Starting new cancellation test - initializing WebDriver...");
        WebDriverUtils.initializeDriver(browserType);
        System.out.println("✅ WebDriver initialized successfully");
    }

    /**
     * Cleanup method - runs after each test
     * Ensures WebDriver is properly closed
     */
    @AfterEach
    void tearDown() {
        System.out.println("🧹 Cleaning up test - closing WebDriver...");
        WebDriverUtils.quitDriver();
        System.out.println("✅ WebDriver closed successfully");
    }

    /**
     * TEST CASE 1: Order Cancellation in CREATED Status
     *
     * This test verifies that orders cannot be cancelled when they are in CREATED status.
     * The system should display a "Cancellation Failed" message.
     *
     * Test Flow:
     * 1. Create a new order
     * 2. Navigate to order details
     * 3. Wait for order to reach CREATED status
     * 4. Attempt to cancel the order
     * 5. Verify "Cancellation Failed" notification appears
     */
    @Test
    @Order(1)
    @DisplayName("Should show 'Cancellation Failed' when cancelling order in CREATED status")
    void testCancelOrderInCreatedStatus() {
        // ARRANGE
        System.out.println("📋 TEST 1: Order Cancellation in CREATED Status");
        OrderInputModel testOrder = OrderInputModel.createRandomCustomerNameSampleOrder();
        System.out.println("Test order data: " + testOrder);

        // ACT & ASSERT
        try {
            System.out.println("🔄 Executing cancellation workflow for CREATED status...");

            boolean cancellationFailed = orderOperations.executeCancellationInCreatedStatus(testOrder);

            // Store the order ID for summary (we can get it from the URL or other means if needed)
            cancelledOrderIdCreated = "CREATED_STATUS_TEST";

            // ASSERT
            Assertions.assertTrue(cancellationFailed,
                    "Cancellation should fail for orders in CREATED status - 'Cancellation Failed' message should appear");

            System.out.println("✅ TEST 1 PASSED: Cancellation correctly failed for CREATED status order");

        } catch (Exception e) {
            System.err.println("❌ TEST 1 FAILED: " + e.getMessage());
            throw e;
        }
    }

    /**
     * TEST CASE 2: Order Cancellation in PAYMENT CONFIRMED Status
     *
     * This test verifies that orders can be successfully cancelled when they are in PAYMENT CONFIRMED status.
     * The system should display a "Cancellation Initiated" message.
     *
     * Test Flow:
     * 1. Create a new order
     * 2. Navigate to order details
     * 3. Wait for order to reach PAYMENT CONFIRMED status
     * 4. Cancel the order
     * 5. Verify "Cancellation Initiated" notification appears
     */
    @Test
    @Order(2)
    @DisplayName("Should show 'Cancellation Initiated' when cancelling order in PAYMENT CONFIRMED status")
    void testCancelOrderInPaymentConfirmedStatus() {
        // ARRANGE
        System.out.println("📋 TEST 2: Order Cancellation in PAYMENT CONFIRMED Status");
        OrderInputModel testOrder = OrderInputModel.createRandomCustomerNameSampleOrder();
        System.out.println("Test order data: " + testOrder);

        // ACT & ASSERT
        try {
            System.out.println("🔄 Executing cancellation workflow for PAYMENT CONFIRMED status...");

            boolean cancellationInitiated = orderOperations.executeCancellationInPaymentConfirmedStatus(testOrder);

            // Store the order ID for summary
            cancelledOrderIdConfirmed = "PAYMENT_CONFIRMED_TEST";

            // ASSERT
            Assertions.assertTrue(cancellationInitiated,
                    "Cancellation should succeed for orders in PAYMENT CONFIRMED status - 'Cancellation Initiated' message should appear");

            System.out.println("✅ TEST 2 PASSED: Cancellation correctly initiated for PAYMENT CONFIRMED status order");

        } catch (Exception e) {
            System.err.println("❌ TEST 2 FAILED: " + e.getMessage());
            throw e;
        }
    }

    /**
     * TEST CASE 3: Verify Order Creation and Navigation to Details
     *
     * This is a supporting test that verifies the basic flow of creating an order
     * and navigating to its details page works correctly before attempting cancellations.
     * This helps isolate any issues with the setup vs. the actual cancellation logic.
     */
    @Test
    @Order(3)
    @DisplayName("Should successfully create order and navigate to details page")
    void testOrderCreationAndNavigationToDetails() {
        // ARRANGE
        System.out.println("📋 TEST 3: Order Creation and Navigation to Details");
        OrderInputModel testOrder = OrderInputModel.createRandomCustomerNameSampleOrder();
        System.out.println("Test order data: " + testOrder);

        // ACT
        try {
            String createdOrderId = orderOperations.executeOrderCreationAndNavigateToDetails(testOrder);

            // ASSERT
            Assertions.assertNotNull(createdOrderId,
                    "Order ID should not be null after creation and navigation");
            Assertions.assertTrue(createdOrderId.startsWith("ORD_"),
                    "Order ID should start with 'ORD_' prefix, but was: " + createdOrderId);

            // Verify we're on the order details page
            String currentUrl = com.graduation.integrationtests.utils.IntegrationTestUtils.getCurrentUrl();
            Assertions.assertTrue(currentUrl.contains(createdOrderId),
                    "Should be on order details page with order ID in URL");

            System.out.println("✅ TEST 3 PASSED: Order created and navigation to details successful");
            System.out.println("📍 Current URL: " + currentUrl);

        } catch (Exception e) {
            System.err.println("❌ TEST 3 FAILED: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cleanup method - runs once after all tests
     * Provides summary of cancellation test results
     */
    @AfterAll
    static void tearDownClass() {
        System.out.println("🏁 All order cancellation integration tests completed");
        System.out.println("📊 Test Summary:");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("🔴 CREATED Status Test: " + (cancelledOrderIdCreated != null ? "COMPLETED" : "NOT RUN"));
        System.out.println("   └─ Expected: Cancellation Failed ❌");
        System.out.println("🟢 PAYMENT CONFIRMED Status Test: " + (cancelledOrderIdConfirmed != null ? "COMPLETED" : "NOT RUN"));
        System.out.println("   └─ Expected: Cancellation Initiated ✅");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("💡 Key Learnings:");
        System.out.println("   • Orders in CREATED status cannot be cancelled");
        System.out.println("   • Orders in PAYMENT CONFIRMED status can be cancelled");
        System.out.println("   • System provides appropriate feedback for both scenarios");
    }
}