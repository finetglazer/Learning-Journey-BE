// src/test/java/com/graduation/integrationtests/OrderCreationIntegrationTest.java
package com.graduation.integrationtests.integration;


import com.graduation.integrationtests.operations.OrderOperations;
import com.graduation.integrationtests.model.OrderInputModel;
import com.graduation.integrationtests.utils.IntegrationTestUtils;
import com.graduation.integrationtests.utils.WebDriverUtils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


/**
 * Integration test for end-to-end order creation workflow
 *
 * This test validates the complete user journey from creating an order
 * through the UI to verifying it appears in the dashboard
 *
 * Test Architecture:
 * - Uses Page Object Model pattern through OrderOperations
 * - Separates test data (OrderInputModel) from test logic
 * - Reuses utility methods from IntegrationTestUtils
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderCreationIntegrationTest {

    @Autowired
    private OrderOperations orderOperations;

    @Value("${browser.type:chrome}")
    private String browserType;

    @Value("${test.timeout:20}")
    private int testTimeout;

    // Test data - we'll store created order ID for verification
    private static String createdOrderId;

    /**
     * Setup method - runs once before all tests
     * Initializes WebDriver and prepares test environment
     */
    @BeforeAll
    static void setupClass() {
        System.out.println("🔧 Setting up integration test environment...");
        // Driver initialization will be done per test for better isolation
    }

    /**
     * Setup method - runs before each test
     * Ensures clean state for each test
     */
    @BeforeEach
    void setUp() {
        System.out.println("🚀 Starting new test - initializing WebDriver...");
        WebDriverUtils.initializeDriver(browserType); // Use configured browser type
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
     * TEST CASE 1: Complete Order Creation Workflow - Happy Path
     */
    @Test
    @Order(1)
    @DisplayName("Should successfully create order through complete UI workflow")
    void testCompleteOrderCreationWorkflow() {
        // ARRANGE
        System.out.println("📋 TEST: Complete Order Creation Workflow");
        OrderInputModel testOrder = OrderInputModel.createRandomCustomerNameSampleOrder();
        System.out.println("Test order data: " + testOrder);

        // ACT
        try {
            createdOrderId = orderOperations.executeCompleteOrderCreationWorkflow(testOrder);

            // ASSERT
            Assertions.assertNotNull(createdOrderId,
                    "Order ID should not be null after creation");
            Assertions.assertTrue(createdOrderId.startsWith("ORD_"),
                    "Order ID should start with 'ORD_' prefix, but was: " + createdOrderId);
            System.out.println("✅ TEST PASSED: Order created successfully with ID: " + createdOrderId);
        } catch (Exception e) {
            System.err.println("❌ TEST FAILED: " + e.getMessage());
            throw e;
        }
    }

    /**
     * TEST CASE 2: Double-Click Create Order Button
     * Verifies that rapidly clicking the create button does not result in duplicate orders.
     * This is a critical test for preventing accidental duplicate submissions.
     */
    @Test
    @Order(2)
    @DisplayName("Should create only one order when create button is double-clicked")
    void testDoubleClickCreatesOnlyOneOrder() throws Exception {
        // ARRANGE
        System.out.println("📋 TEST: Double-Click Create Order");
        // Use a unique customer name to ensure the count is not affected by other test data
        OrderInputModel testOrder = OrderInputModel.createRandomCustomerNameSampleOrder();
//        testOrder.setCustomerName("Double Click User");

        orderOperations.navigateToCreateOrderPage();
        orderOperations.fillOrderFormAndAddItem(testOrder);

        // ACT
        // Double-click the create order button instead of a single click
        orderOperations.doubleClickCreateOrder();
        orderOperations.navigateToDashboard();
        orderOperations.checkExistingOrderByUserName(testOrder.getCustomerName());

        // Count how many orders appear for our specific test user on the last page
        int createdOrderCount = orderOperations.countOrdersOnDashboardPage(testOrder.getCustomerName());

        // ASSERT
        // We expect exactly one order to be created, not zero or more than one.
        Assertions.assertEquals(1, createdOrderCount,
                "Double-clicking the create button should result in exactly one order.");
        System.out.println("✅ TEST PASSED: Double-click check successful. One order created.");
    }

    /**
     * TEST CASE 3: Invalid Phone Number Submissions
     * This is a parameterized test that runs multiple times with different invalid inputs.
     * It ensures the application provides proper validation and does not proceed.
     */
    @ParameterizedTest(name = "Run {index}: phone=''{0}''")
    @ValueSource(strings = {
            "@#$%",                  // 1. Special characters
            "-12345",                // 2. Negative number
            "",                      // 3. Empty string (for null case)
            "1111111111111111111111111111111111111111111", // 4. Too large number
            "   ",                   // 5. Whitespace
            "h12h3k4",               // 6. Alphanumeric string
            "099999"                 // 7. Not a valid format
    })
    @Order(3)
    @DisplayName("Should show validation error for invalid phone numbers")
    void testCreateOrderWithInvalidPhoneNumber(String invalidPhoneNumber) throws Exception {
        // ARRANGE
        System.out.println("📋 TEST: Invalid Phone Number - " + invalidPhoneNumber);
        OrderInputModel testOrder = OrderInputModel.createValidSampleOrder();
        testOrder.setCustomerPhone(invalidPhoneNumber);

        orderOperations.navigateToCreateOrderPage();
        orderOperations.fillOrderFormAndAddItem(testOrder);

        // ACT
        orderOperations.createOrder(); // Attempt to create the order

        // ASSERT
        // The user should remain on the create page and an error should be visible.
        Assertions.assertTrue(IntegrationTestUtils.getCurrentUrl().contains("create-order"),
                "User should remain on the create order page after validation failure.");

        Assertions.assertTrue(orderOperations.isValidationErrorDisplayed(),
                "A validation error message should be displayed for invalid phone: " + invalidPhoneNumber);
        System.out.println("✅ TEST PASSED: Validation failed as expected for phone: " + invalidPhoneNumber);
    }


    /**
     * Cleanup method - runs once after all tests
     */
    @AfterAll
    static void tearDownClass() {
        System.out.println("🏁 All integration tests completed");
        System.out.println("📊 Test Summary:");
        System.out.println("- Created Order ID: " + (createdOrderId != null ? createdOrderId : "None"));
    }
}
