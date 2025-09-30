// src/main/java/utils/IntegrationTestUtils.java
package com.graduation.integrationtests.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

public class IntegrationTestUtils {

    @Value("${test.timeout:15}")
    private static int WAIT_TIMEOUT_SECONDS = 15;

    /**
     * Navigate to a specific page and wait for it to load
     */
    public static void navigateToPage(String url) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        driver.get(url);
        wait.until(ExpectedConditions.urlContains(extractPathFromUrl(url)));
    }

    /**
     * Fill input field by various locator strategies
     */
    public static void fillInput(String locatorType, String locatorValue, String inputValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(inputValue);
    }

    /**
     * Click element with wait
     */
    public static void clickElement(String locatorType, String locatorValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
    }

    /**
     * Select dropdown option
     */
    public static void selectDropdownOption(String locatorType, String locatorValue, String optionText) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        Select dropdown = new Select(element);
        dropdown.selectByVisibleText(optionText);
    }

    /**
     * Get text from element
     */
    public static String getElementText(String locatorType, String locatorValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        return element.getText();
    }

    /**
     * Verify element is present
     */
    public static boolean isElementPresent(String locatorType, String locatorValue) {
        try {
            WebDriver driver = WebDriverUtils.getDriver();
            By locator = getLocator(locatorType, locatorValue);
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * NEW: Wait for element with specific text to be present
     * This is useful for waiting for status changes or notifications
     */
    public static boolean waitForElementWithText(String locatorType, String locatorValue, String expectedText, int timeoutSeconds) {
        try {
            WebDriver driver = WebDriverUtils.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            By locator = getLocator(locatorType, locatorValue);

            // Wait for element to be present and contain the expected text
            wait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(locator),
                    ExpectedConditions.textToBePresentInElementLocated(locator, expectedText)
            ));

            return true;
        } catch (TimeoutException e) {
            System.out.println("⚠️ Element with text '" + expectedText + "' not found within " + timeoutSeconds + " seconds");
            return false;
        }
    }

    /**
     * NEW: Wait for notification to appear with specific title
     * Specifically designed for waiting for cancellation notifications
     */
    public static boolean waitForNotificationWithTitle(String expectedTitle, int timeoutSeconds) {
        String notificationXpath = "//h2[@id='notification-title' and contains(text(), '" + expectedTitle + "')]";
        return waitForElementToBeVisible("xpath", notificationXpath, timeoutSeconds);
    }

    /**
     * NEW: Wait for element to be visible with custom timeout
     */
    public static boolean waitForElementToBeVisible(String locatorType, String locatorValue, int timeoutSeconds) {
        try {
            WebDriver driver = WebDriverUtils.getDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            By locator = getLocator(locatorType, locatorValue);
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

            return true;
        } catch (TimeoutException e) {
            System.out.println("⚠️ Element not visible within " + timeoutSeconds + " seconds: " + locatorValue);
            return false;
        }
    }

    /**
     * NEW: Wait for order status to change to specific value
     * This method continuously checks for status changes until the expected status appears
     */
    public static boolean waitForOrderStatus(String expectedStatus, int timeoutSeconds) {
        System.out.println("Waiting for order status: " + expectedStatus);

        String statusXpath;
        switch (expectedStatus.toUpperCase()) {
            case "CREATED":
                statusXpath = "//span[contains(@class, 'ml-2') and contains(@class, 'font-medium') and contains(text(), 'CREATED')]";
                break;
            case "PAYMENT: CONFIRMED":
                statusXpath = "//span[contains(@class, 'bg-green-100') and contains(@class, 'text-green-800') and contains(text(), 'Payment: CONFIRMED')]";
                break;
            default:
                // Generic status check
                statusXpath = "//span[contains(text(), '" + expectedStatus + "')]";
                break;
        }

        return waitForElementToBeVisible("xpath", statusXpath, timeoutSeconds);
    }

    /**
     * NEW: Enhanced method to check for cancellation result
     * Returns the type of cancellation result found
     */
    public static String getCancellationResult(int timeoutSeconds) {
        System.out.println("Checking for cancellation result...");

        // Check for both possible outcomes
        boolean failedFound = waitForNotificationWithTitle("Cancellation Failed", timeoutSeconds / 2);
        if (failedFound) {
            return "FAILED";
        }

        boolean initiatedFound = waitForNotificationWithTitle("Cancellation Initiated", timeoutSeconds / 2);
        if (initiatedFound) {
            return "INITIATED";
        }

        return "NONE";
    }

    /**
     * Wait for page transition by URL change
     */
    public static void waitForPageTransition(String expectedUrlPart) {
        WebDriverWait wait = WebDriverUtils.getWait();
        wait.until(ExpectedConditions.urlContains(expectedUrlPart));
    }

    /**
     * Get current page URL
     */
    public static String getCurrentUrl() {
        return WebDriverUtils.getDriver().getCurrentUrl();
    }

    /**
     * Extract order ID from URL (for order detail pages)
     */
    public static String extractOrderIdFromUrl() {
        String currentUrl = getCurrentUrl();
        // Extract ORD_XXXXX pattern from URL
        if (currentUrl.contains("ORD_")) {
            return currentUrl.substring(currentUrl.lastIndexOf("ORD_"));
        }
        return null;
    }

    // Helper method to create locators
    private static By getLocator(String locatorType, String locatorValue) {
        switch (locatorType.toLowerCase()) {
            case "id":
                return By.id(locatorValue);
            case "name":
                return By.name(locatorValue);
            case "class":
                return By.className(locatorValue);
            case "css":
                return By.cssSelector(locatorValue);
            case "xpath":
                return By.xpath(locatorValue);
            case "tag":
                return By.tagName(locatorValue);
            default:
                throw new IllegalArgumentException("Locator type not supported: " + locatorType);
        }
    }

    private static String extractPathFromUrl(String url) {
        try {
            return url.substring(url.indexOf('/', 8)); // Skip protocol and domain
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Waits for a specific element to be present, visible, and clickable.
     *
     * @param locatorType  The type of locator (e.g., "xpath", "css", "id").
     * @param locatorValue The locator string to find the element.
     */
    public static void waitForElementToBeClickable(String locatorType, String locatorValue) {
        try {
            // Get the WebDriver instance from your utility class
            WebDriver driver = WebDriverUtils.getDriver();

            // Create a WebDriverWait instance with a defined timeout
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

            // Get the By object based on the locator type and value
            By by = getLocatorBy(locatorType, locatorValue);

            // Wait until the element is deemed clickable by Selenium
            wait.until(ExpectedConditions.elementToBeClickable(by));

        } catch (TimeoutException e) {
            // Throw a more informative exception if the element isn't clickable in time
            throw new TimeoutException("Element was not clickable within " + WAIT_TIMEOUT_SECONDS + " seconds: "
                    + locatorType + "=" + locatorValue, e);
        }
    }

    /**
     * Helper method to create a By object from a locator type and value.
     * This avoids code duplication in your other utility methods.
     *
     * @param locatorType  The type of locator (e.g., "xpath", "css", "id", "class").
     * @param locatorValue The locator string.
     * @return A By object corresponding to the locator.
     */
    private static By getLocatorBy(String locatorType, String locatorValue) {
        switch (locatorType.toLowerCase()) {
            case "id":
                return By.id(locatorValue);
            case "name":
                return By.name(locatorValue);
            case "class":
                return By.className(locatorValue);
            case "css":
                return By.cssSelector(locatorValue);
            case "xpath":
                return By.xpath(locatorValue);
            case "linktext":
                return By.linkText(locatorValue);
            case "partiallinktext":
                return By.partialLinkText(locatorValue);
            case "tag":
                return By.tagName(locatorValue);
            default:
                throw new IllegalArgumentException("Unsupported locator type: " + locatorType);
        }
    }

    /**
     * Performs a double-click on a given element.
     * @param locatorType The type of locator (e.g., "css", "xpath").
     * @param locatorValue The locator string.
     */
    public static void doubleClickElement(String locatorType, String locatorValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        WebDriverWait wait = WebDriverUtils.getWait();
        Actions actions = new Actions(driver);

        By locator = getLocator(locatorType, locatorValue);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));

        // Perform the double-click action
        actions.doubleClick(element).perform();
    }

    /**
     * Counts the number of elements matching a given locator.
     * @param locatorType The type of locator (e.g., "css", "xpath").
     * @param locatorValue The locator string.
     * @return The total number of elements found.
     */
    public static int countElements(String locatorType, String locatorValue) {
        WebDriver driver = WebDriverUtils.getDriver();
        By locator = getLocator(locatorType, locatorValue);
        // findElements returns a list of all matching elements. The size of the list is our count.
        return driver.findElements(locator).size();
    }

    /**
     * NEW: Smart wait for any of multiple conditions to be met
     * Useful when waiting for one of several possible outcomes
     */
    public static String waitForAnyOfConditions(String[] xpathConditions, String[] conditionNames, int timeoutSeconds) {
        WebDriver driver = WebDriverUtils.getDriver();
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);

        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < xpathConditions.length; i++) {
                if (isElementPresent("xpath", xpathConditions[i])) {
                    System.out.println("✅ Condition met: " + conditionNames[i]);
                    return conditionNames[i];
                }
            }

            try {
                Thread.sleep(500); // Check every 500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("⚠️ No conditions met within timeout");
        return "TIMEOUT";
    }
}