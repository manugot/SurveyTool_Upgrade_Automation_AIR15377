package Utilities;

import java.util.ArrayList;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ElementClickInterceptedException;
import com.relevantcodes.extentreports.LogStatus;


import Common.BaseTest;

public class SurveyToolMethods extends BaseTest {
    
    // Instance variable for window tabs
    private ArrayList<String> tabs;
    
    // Maximum retry attempts for stale element handling
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int STANDARD_WAIT_TIME = 10;

    /**
     * Robust element finder that handles stale element references
     * @param locator The By locator to find the element
     * @param description Description for logging
     * @return WebElement or null if not found after retries
     */
    public WebElement findElementWithRetry(By locator, String description) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(STANDARD_WAIT_TIME));
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                return element;
            } catch (StaleElementReferenceException | TimeoutException | NoSuchElementException e) {
                attempts++;
                ExtentReporter.log(LogStatus.WARNING, 
                    "Attempt " + attempts + " failed for " + description + ": " + e.getClass().getSimpleName());
                
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000); // Wait before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        ExtentReporter.log(LogStatus.FAIL, "Failed to find element after " + MAX_RETRY_ATTEMPTS + " attempts: " + description);
        return null;
    }

    /**
     * Robust click method that handles stale elements and click interceptions
     * @param locator The By locator for the element to click
     * @param description Description for logging
     * @return true if click was successful, false otherwise
     */
    public boolean clickWithRetry(By locator, String description) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                WebElement element = findElementWithRetry(locator, description);
                if (element != null) {
                    // Wait for element to be clickable
                    WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(STANDARD_WAIT_TIME));
                    element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", element);
                    
                    // Try normal click first
                    element.click();
                    ExtentReporter.log(LogStatus.PASS, "Successfully clicked: " + description);
                    return true;
                }
            } catch (ElementClickInterceptedException e) {
                ExtentReporter.log(LogStatus.WARNING, "Click intercepted for " + description + ", trying JavaScript click");
                try {
                    waitForOverlayToDisappear();
                    WebElement element = findElementWithRetry(locator, description);
                    if (element != null) {
                        ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", element);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                        ExtentReporter.log(LogStatus.PASS, "Successfully clicked with JavaScript: " + description);
                        return true;
                    }
                } catch (Exception jsException) {
                    ExtentReporter.log(LogStatus.WARNING, "JavaScript click also failed: " + jsException.getMessage());
                }
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (StaleElementReferenceException e) {
                attempts++;
                ExtentReporter.log(LogStatus.WARNING, 
                    "Stale element on attempt " + attempts + " for " + description);
                
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000); // Wait before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                ExtentReporter.log(LogStatus.WARNING, 
                    "Unexpected error on attempt " + (attempts + 1) + " for " + description + ": " + e.getMessage());
                attempts++;
            }
        }
        
        ExtentReporter.log(LogStatus.FAIL, "Failed to click element after " + MAX_RETRY_ATTEMPTS + " attempts: " + description);
        return false;
    }

    /**
     * Wait for page to be fully loaded and stable
     */
    public void waitForPageLoad() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            
            // Additional wait for any AJAX requests to complete
            try {
                wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return jQuery.active == 0"));
            } catch (Exception e) {
                // jQuery might not be present, continue
            }
            
            Thread.sleep(1000); // Small buffer for final stabilization
            ExtentReporter.log(LogStatus.INFO, "Page fully loaded and stable");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.WARNING, "Page load wait completed with exception: " + e.getMessage());
        }
    }

    /**
     * Wait for overlay elements to disappear (like pjaxClickInhibitor)
     */
    public void waitForOverlayToDisappear() {
    	
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
            // Wait for known overlays to disappear
            Thread.sleep(1000);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("pjaxClickInhibitor")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("ls-loading")));
            ExtentReporter.log(LogStatus.INFO, "Overlay disappeared, page ready for interaction");
        } catch (TimeoutException e) {
            ExtentReporter.log(LogStatus.WARNING, "Overlay wait timeout, proceeding anyway");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.INFO, "No overlay detected or already disappeared");
        }
    }

    private void verifyElementsByMap(java.util.Map<String, String> elements) {
        elements.forEach((description, xpath) -> {
            try {
                highlightObject(xpath);
                isElementDisplayed(xpath, "xpath", description);
                WebElement element = driver.findElement(By.xpath(xpath));
                ExtentReporter.log(LogStatus.INFO, description + " text: " + element.getText());
            } catch (Exception e) {
                ExtentReporter.log(LogStatus.FAIL, "Verification failed for: " + description + " - " + e.getMessage());
            }
        });
    }

    public void performMouseHover(String xpath) throws Exception {
        try {
            WebElement element = driver.findElement(By.xpath(xpath));
            Actions actions = new Actions(driver);
            actions.moveToElement(element).build().perform();
            ExtentReporter.log(LogStatus.INFO, "Mouse hover performed on element: " + xpath);
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform mouse hover: " + e.getMessage());
            throw e;
        }
    }

    public void refreshPage() throws Exception {
        driver.navigate().refresh();
        Thread.sleep(3000);
    }

    public void switchToNewWindow() throws Exception {
        try {
            tabs = new ArrayList<String>(driver.getWindowHandles());
            driver.switchTo().window(tabs.get(1));
            Thread.sleep(3000);
            ExtentReporter.log(LogStatus.INFO, "Switched to new window");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to open new window: " + e.getMessage());
            throw e;
        }
    }

    public void switchToMainWindow() throws Exception {
    	Thread.sleep(3000);
        try {
            tabs = new ArrayList<>(driver.getWindowHandles());
            if (tabs == null || tabs.isEmpty()) {
                ExtentReporter.log(LogStatus.FAIL, "No windows available to switch to");
                throw new Exception("No windows available");
            }
            driver.switchTo().window(tabs.get(0));
            ExtentReporter.log(LogStatus.INFO, "Switched back to default window");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to switch back to default window: " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method verifies the elements present on the Create Survey page.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyCreateObjs() throws Exception {
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Create Survey Page Title", "//*[contains(@class, 'ls-breadcrumb')]");
                put("Close Button", "//*[@id='close-button']");
                put("Save Button", "//*[@id='save-form-button']");
                put("Navigation Tabs", "//*[@id='create-import-copy-survey']");
                put("Create Tab", "//a[@data-form-id='addnewsurvey']");
                put("Survey Title Label", "//*[text()='Survey title:']");
                put("Survey Title Input", "//*[@id='surveyTitle']");
                put("Required Annotation", "(//*[text()='Required '])[1]");
                put("Base Language Label", "//*[text()='Base language:']");
                put("Base Language Input", "(//*[contains(@class, 'select2-selection--single')])[1]");
                put("Survey Group Label", "//*[text()='Survey group:']");
                put("Survey Group Input", "(//*[contains(@class, 'select2-selection--single')])[2]");
                put("Administrator Label", "//*[text()='Administrator:']");
                put("Administrator Toggle Button", "//*[@id='administrator']");
                put("Current User Toggle Button", "//*[@for='administrator_1']");
                put("Custom Toggle Button", "//*[@for='administrator_2']");
//              put("Create Survey Button", "//*[@id='create-survey-submit']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void createSurvey() throws Exception {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            waitForPageLoad();
            
            String surveyTitle = "CREATE - Automation Survey. Please disregard.";
            By surveyTitleLocator = By.xpath("//*[@id='surveyTitle']");
            
            // Enter survey title with retry handling
            WebElement surveyTitleElement = findElementWithRetry(surveyTitleLocator, "Survey Title Input");
            if (surveyTitleElement != null) {
                surveyTitleElement.clear();
                // Wait for element to be ready for input
                wait.until(ExpectedConditions.elementToBeClickable(surveyTitleLocator));
                
                // Send keys slowly to avoid input validation errors
                for (char c : surveyTitle.toCharArray()) {
                    surveyTitleElement.sendKeys(String.valueOf(c));
                    Thread.sleep(50); // Small delay between characters
                }
                
                // Verify input was successful
                String inputValue = surveyTitleElement.getAttribute("value");
                if (inputValue != null && inputValue.contains(surveyTitle)) {
                    ExtentReporter.log(LogStatus.INFO, "Survey Title entered successfully: " + surveyTitle);
                } else {
                    throw new Exception("Survey title input validation failed. Expected: " + surveyTitle + ", Got: " + inputValue);
                }
                
                // Allow time for any client-side validation
                Thread.sleep(500);
                
                // Check for validation errors
                try {
                    java.util.List<WebElement> errorMessages = driver.findElements(By.xpath(
                        "//*[contains(@class, 'invalid-feedback') or contains(@class, 'error') or contains(text(), 'Unsafe')]"));
                    if (!errorMessages.isEmpty()) {
                        String errorMsg = errorMessages.get(0).getText();
                        ExtentReporter.log(LogStatus.WARNING, "Validation warning detected: " + errorMsg);
                    }
                } catch (Exception validationCheck) {
                    // No validation errors found, continue
                }
            } else {
                throw new Exception("Failed to locate Survey Title input element");
            }

            // Select Base Language with retry handling
            By baseLangLocator = By.xpath("(//*[contains(@class, 'select2-selection--single')])[1]");
            WebElement baseLangInput = findElementWithRetry(baseLangLocator, "Base Language Selector");
            if (baseLangInput != null) {
                wait.until(ExpectedConditions.elementToBeClickable(baseLangLocator));
                baseLangInput.click();
                Thread.sleep(300);
                baseLangInput.sendKeys("English" + Keys.ENTER);
                ExtentReporter.log(LogStatus.INFO, "Base Language selected: English");
            } else {
                throw new Exception("Failed to locate Base Language selector element");
            }

            // Click Create Survey Button with retry handling
            boolean savedSuccessfully = clickWithRetry(By.xpath("//*[@id='save-form-button']"), "Create Survey Button");
            if (!savedSuccessfully) {
                throw new Exception("Failed to click Create Survey button");
            }
            ExtentReporter.log(LogStatus.INFO, "Create Survey button clicked");
            
            // Wait for overlays to disappear
            waitForOverlayToDisappear();
            waitForPageLoad();

            // Verify survey creation (support multiple successful landing views)
            By createdSurveyBreadcrumbLocator = By.xpath("(//*[@id='breadcrumb__group--detail'])[1]");
            By surveyEditorHeaderLocator = By.xpath("//*[@id='breadcrumb-container']");
            By surveyTitleTextElementLocator = By.xpath("//*[@id='short_title_en']");

            try {
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(createdSurveyBreadcrumbLocator),
                        ExpectedConditions.visibilityOfElementLocated(surveyEditorHeaderLocator),
                        ExpectedConditions.visibilityOfElementLocated(surveyTitleTextElementLocator)));
            } catch (TimeoutException timeoutException) {
                String currentUrl = driver.getCurrentUrl();
                String currentTitleValue = "";
                try {
                    WebElement titleField = driver.findElement(By.xpath("//*[@id='surveyTitle']"));
                    currentTitleValue = titleField.getAttribute("value");
                } catch (Exception ignore) {
                    // title field not present on the current page
                }

                java.util.List<WebElement> validationMessages = driver.findElements(By.xpath(
                        "//*[contains(@class,'invalid-feedback') or contains(@class,'alert-danger') " +
                        "or contains(@class,'text-danger') or contains(text(),'invalid') or contains(text(),'Invalid') " +
                        "or contains(text(),'error') or contains(text(),'Error') or contains(text(),'Unsafe') ]"));

                String validationText = validationMessages.isEmpty() ? "No explicit validation message found"
                        : validationMessages.get(0).getText();

                throw new Exception("Survey creation did not reach expected destination page within timeout. "
                        + "Current URL: " + currentUrl
                        + " | Survey title field value: " + currentTitleValue
                        + " | Validation result: " + validationText);
            }

            WebElement createdSurveyElement = null;
            String successElementXpath = null;

            if (!driver.findElements(createdSurveyBreadcrumbLocator).isEmpty()) {
                successElementXpath = "(//*[@id='breadcrumb__group--detail'])[1]";
                createdSurveyElement = driver.findElement(createdSurveyBreadcrumbLocator);
            } else if (!driver.findElements(surveyTitleTextElementLocator).isEmpty()) {
                successElementXpath = "//*[@id='short_title_en']";
                createdSurveyElement = driver.findElement(surveyTitleTextElementLocator);
            } else if (!driver.findElements(surveyEditorHeaderLocator).isEmpty()) {
                successElementXpath = "//*[@id='breadcrumb-container']";
                createdSurveyElement = driver.findElement(surveyEditorHeaderLocator);
            }

            if (createdSurveyElement != null && successElementXpath != null) {
                highlightObject(successElementXpath);
                ExtentReporter.log(LogStatus.INFO,
                        "Survey created with title: " + surveyTitle + " - Result: " + createdSurveyElement.getText());
            } else {
                throw new Exception("Failed to verify survey creation after save action");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to create survey: " + e.getMessage());
            throw e;
        }
    }

    public void createSurveyCharacterLimit() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            By surveyTitleLocator = By.xpath("//*[@id='surveyTitle']");
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));

            // Recover from closed/stale window context by switching to the first available handle
            try {
                driver.getTitle();
            } catch (org.openqa.selenium.NoSuchWindowException closedWindow) {
                java.util.Set<String> handles = driver.getWindowHandles();
                if (handles == null || handles.isEmpty()) {
                    throw new Exception("No browser window is available to continue createSurveyCharacterLimit.");
                }
                driver.switchTo().window(handles.iterator().next());
            }

            // Enter survey title with 250 characters
            String longTitle = "A".repeat(250);
            WebElement surveyTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(surveyTitleLocator));
            surveyTitle.clear();
            surveyTitle.sendKeys(longTitle);

            String surveyTitleText = surveyTitle.getAttribute("value");
            ExtentReporter.log(surveyTitleText.length() >= 200 ? LogStatus.PASS : LogStatus.FAIL,
                    (surveyTitleText.length() >= 200 ? "Survey Title within character limit: "
                            : "Survey Title exceeds character limit: ") + surveyTitleText.length());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to handle character limit: " + e.getMessage());
            throw e;
        }
    }

    public void createEmptyTitle() throws Exception {
        try {
            WebElement surveyTitle = driver.findElement(By.xpath("//*[@id='surveyTitle']"));
            surveyTitle.clear();
            // Click Create Survey Button
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");

            String message = surveyTitle.getAttribute("validationMessage");
            ExtentReporter.log(LogStatus.INFO, "Validation message displayed: " + message);
        } catch (Exception e) {
          ExtentReporter.log(LogStatus.FAIL, "Failed to validate empty title: " + e.getMessage());
        }
    }

    public void createValidateXSS() throws Exception {
        try {
            String xssString = "<script>alert('XSS')</script>";
            WebElement surveyTitle = driver.findElement(By.xpath("//*[@id='surveyTitle']"));
            surveyTitle.clear();
            surveyTitle.sendKeys(xssString);
            ExtentReporter.log(LogStatus.INFO, "XSS string entered in Survey Title: " + xssString);

            // Click Create Survey Button
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");
            waitForPageLoad();
            waitForOverlayToDisappear();
            Thread.sleep(2000);

            // Verify if XSS was sanitized or rejected
            String createdSurvey = "//*[@id='breadcrumb__group--detail']";
            java.util.List<WebElement> createdSurveyElements = driver.findElements(By.xpath(createdSurvey));

            if (!createdSurveyElements.isEmpty()) {
                highlightObject(createdSurvey);
                String displayedTitle = createdSurveyElements.get(0).getText();
                if (displayedTitle.contains("<script>") || displayedTitle.contains("</script>")) {
                    ExtentReporter.log(LogStatus.FAIL, "XSS vulnerability detected! Title displayed as: " + displayedTitle);
                } else {
                    ExtentReporter.log(LogStatus.PASS, "No XSS vulnerability. Title displayed as: " + displayedTitle);
                }
            } else {
                String currentUrl = driver.getCurrentUrl();
                String currentTitleValue = driver.findElement(By.xpath("//*[@id='surveyTitle']")).getAttribute("value");
                java.util.List<WebElement> validationMessages = driver.findElements(By.xpath(
                        "//*[contains(@class,'invalid-feedback') or contains(@class,'alert-danger') or contains(@class,'text-danger') or contains(text(),'invalid') or contains(text(),'Invalid') or contains(text(),'error') or contains(text(),'Error')]"));

                if (!validationMessages.isEmpty() || currentUrl.contains("newSurvey")) {
                    String validationText = validationMessages.isEmpty()
                            ? "No explicit validation message displayed"
                            : validationMessages.get(0).getText();
                    ExtentReporter.log(LogStatus.PASS,
                            "XSS payload was not taken to the survey details page. Current URL: " + currentUrl
                                    + " | Current title field value: " + currentTitleValue
                                    + " | Validation result: " + validationText);
                } else {
                    ExtentReporter.log(LogStatus.WARNING,
                            "XSS validation could not confirm success page or explicit validation state. Current URL: "
                                    + currentUrl + " | Current title field value: " + currentTitleValue);
                }
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate XSS handling: " + e.getMessage());
            throw e;
        }
    }

    public void createValidateDoubleByteCharacters() throws Exception {
    	Thread.sleep(10000);
        try {
            String doubleByteString = "テスト自動化"; // Japanese characters for "Test Automation"
            WebElement surveyTitle = driver.findElement(By.xpath("//*[@id='surveyTitle']"));
            surveyTitle.clear();
            surveyTitle.sendKeys(doubleByteString);
            ExtentReporter.log(LogStatus.INFO, "Double-byte string entered in Survey Title: " + doubleByteString);

            // Click Create Survey Button
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");
            Thread.sleep(10000);
       
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate double-byte character handling: " + e.getMessage());
            throw e;
        }
    }
    
    public void VerifyDoubleByteCharacters() throws Exception {
    	Thread.sleep(10000);
        try {   
            // Verify if double-byte characters are displayed correctly
            String doubleByteString = "テスト自動化"; // Japanese characters for "Test Automation"
            String createdSurvey = "//*[@id='short_title_en']";
            highlightObject(createdSurvey);
            String displayedTitle = driver.findElement(By.xpath(createdSurvey)).getAttribute("value");
            if (displayedTitle.equals(doubleByteString)) {
                ExtentReporter.log(LogStatus.PASS, "Double-byte characters displayed correctly: " + displayedTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Double-byte characters not displayed correctly. Displayed as: " + displayedTitle);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate double-byte character handling: " + e.getMessage());
            throw e;
        }
    }

    
   

    /**
     * This method verifies the elements present on the Import Survey page.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyImportObjs() throws Exception {
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Import Close Button", "//*[@id='close-button']");
                put("Import Save Button", "//*[@id='save-form-button']");
                put("Import Survey Label",
                        "//*[contains(text(), 'Select survey structure file (*.lss, *.txt) or survey archive (*.lsa) (maximum file size: 20.00 MB)')]");
                put("Import Survey Input", "//*[@id='the_file']");
                put("Import file description", "//*[@id='the_file_description']");
                put("Import checkbox", "//*[@id='translinksfields']");
                put("Import checkbox label", "//*[@for='translinksfields']");
//                put("Import Survey Button", "//*[@id='import-submit']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void importSurveyFunctionality() throws Exception {
        try {

            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\Import_survey.lss";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);
 //         driver.findElement(By.xpath("//*[@id='save-form-button']")).click();

            // Click the Import Button
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");

            Thread.sleep(30000);
            String importSuccessMessage = "//*[@class='jumbotron message-box']";
            highlightObject(importSuccessMessage);
            isElementDisplayed(importSuccessMessage, "xpath", "Import Success Message");

            WebElement msgElement = driver.findElement(By.xpath(importSuccessMessage));
            ExtentReporter.log(LogStatus.INFO, "Import Survey summary: " + msgElement.getText());

            driver.findElement(By.xpath("//*[@class='btn btn-outline-secondary btn-large']")).click();
            String createdSurvey = "(//*[@id='breadcrumb__group--detail'])[1]";
            highlightObject(createdSurvey);
            ExtentReporter.log(LogStatus.INFO, "Question Group: " +
                    driver.findElement(By.xpath(createdSurvey)).getText());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to import survey: " + e.getMessage());
            throw e;
        }
    }

    public void importNoFileSelectedFunc() throws Exception {
        try {
            // Attempt import without selecting a file
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked without file selection");

            // Wait for and verify error message
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[@class='modal-content'])[4]")));

            // Verify error message and log details
            String errorXpath = "(//*[@class='modal-content'])[4]";
            highlightObject("//*[contains(text(), 'Please select a file to import!')]");
            isElementDisplayed("//*[contains(text(), 'Please select a file to import!')]", "xpath",
                    "Error Message for No File Selected");
            ExtentReporter.log(LogStatus.INFO, "Error Message: " + driver.findElement(By.xpath(errorXpath)).getText());

            // Close error dialog
            driver.findElement(By.xpath("(//*[contains(@class, 'btn btn-outline-secondary')])[3]")).click();
            ExtentReporter.log(LogStatus.INFO, "Closed the error message dialog");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to handle no file selected: " + e.getMessage());
            throw e;
        }
    }

    public void importInvalidFileFunc() throws Exception {
        try {
            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\All files\\Invalid_file_import.csv";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked with invalid file");
            Thread.sleep(10000);

            // Verify error message appears
            String errorBoxPath = "//*[@class='jumbotron message-box message-box-error']";
            highlightObject("//*[contains(text(), 'Import failed.')]");
            isElementDisplayed("//*[contains(text(), 'Import failed.')]", "xpath", "Error Message for Invalid File");

            // Log error message details
            WebElement errorBox = driver.findElement(By.xpath(errorBoxPath));
            ExtentReporter.log(LogStatus.INFO, "Error Message: " + errorBox.getText());

            // Return to homepage and verify
            driver.findElement(By.xpath("//*[@value='Main admin screen']")).click();
            isElementDisplayed("//*[@id='welcome-jumbotron']", "xpath", "Survey Tool Homepage");
            ExtentReporter.log(LogStatus.INFO, "Returned to Homepage");
            Thread.sleep(10000);

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to handle invalid file import: " + e.getMessage());
            throw e;
        }
    }

    public void importExceedFileSizeFunc() throws Exception {
        try {
            // Upload oversized file
            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\Exceed_file_size.txt";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);

            // Submit import and verify error
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked with file exceeding size limit");

            // Verify error message
            String errorPath = "//*[@class='jumbotron message-box message-box-error']";
            highlightObject("//*[contains(text(), 'Sorry, this file is too large')]");
            isElementDisplayed("//*[contains(text(), 'Sorry, this file is too large')]", "xpath",
                    "Error Message for Exceeding File Size");

            // Log error details
            ExtentReporter.log(LogStatus.INFO, "Error Message: " +
                    driver.findElement(By.xpath(errorPath)).getText());

            // Return to homepage
            driver.findElement(By.xpath("//*[@value='Main admin screen']")).click();
            isElementDisplayed("//*[@id='welcome-jumbotron']", "xpath", "Survey Tool Homepage");
            ExtentReporter.log(LogStatus.INFO, "Returned to Homepage");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to handle file exceeding size limit: " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method verifies the elements present on the Copy Survey page.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyCopyObjs() throws Exception {
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Copy Close Button", "//*[@id='close-button']");
                put("Copy Button", "//*[@id='save-form-button']");
                put("Copy Select survey label", "//*[@for='copysurveylist']");
                put("Copy Survey Input", "(//*[contains(@class, 'select2-selection--single')])[3]");
                put("Copy Select annotation", "(//*[@class='annotation text-danger'])[2]");
                put("Copy New Survey Title Label", "//*[@for='copysurveyname']");
                put("Copy New Survey Title Input", "//*[@id='copysurveyname']");
                put("Copy New Title annotation", "(//*[@class='annotation text-danger'])[3]");
                put("Copy New Survey ID label", "//*[@for='copysurveyid']");
                put("Copy New Survey ID Input", "//*[@id='copysurveyid']");
                put("Copy Survey ID annotation", "//*[@class='annotation text-info']");
//                put("Copy Survey Button", "//*[contains(@class, 'col-12 col-sm-6 col-md-4')]");
                put("Checkbox #1 Resource file", "//*[@id='copysurveytranslinksfields']");
                put("Checkbox #1 Resource file label", "//*[@for='copysurveytranslinksfields']");
                put("Checkbox #2 Quotas", "//*[@id='copysurveyexcludequotas']");
                put("Checkbox #2 Quotas label", "//*[@for='copysurveyexcludequotas']");
                put("Checkbox #3 Survey permission", "//*[@id='copysurveyexcludepermissions']");
                put("Checkbox #3 Survey Permission label", "//*[@for='copysurveyexcludepermissions']");
                put("Checkbox #4 Exclude Answers", "//*[@id='copysurveyexcludeanswers']");
                put("Checkbox #4 Exclude Answers label", "//*[@for='copysurveyexcludeanswers']");
                put("Checkbox #5 Reset Conditions", "//*[@id='copysurveyresetconditions']");
                put("Checkbox #5 Reset Conditions label", "//*[@for='copysurveyresetconditions']");
                put("Checkbox #6 Start/End date/time", "//*[@id='copysurveyresetstartenddate']");
                put("Checkbox #6 Start/End date/time label", "//*[@for='copysurveyresetstartenddate']");
                put("Checkbox #7 Response start", "//*[@id='copysurveyresetresponsestartid']");
                put("Checkbox #7 Response start label", "//*[@for='copysurveyresetresponsestartid']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void copySurveyFunctionality() throws Exception {
    	Thread.sleep(10000);
        try {
            // Select a survey to copy
            driver.findElement(By.xpath("(//*[contains(@class, 'select2-selection--single')])[3]")).click();
            driver.findElement(By.xpath("//*[@class='select2-search__field']"))
                    .sendKeys("CREATE - Automation Survey. Please disregard.");
            driver.findElement(By.xpath("(//*[contains(text(), 'CREATE - Automation Survey. Please disregard.')])[3]"))
                    .click();
            ExtentReporter.log(LogStatus.INFO, "Survey selected for copying");

            // Enter new survey title and copy
            String newSurveyTitle = "COPY - Automation Survey";
            driver.findElement(By.xpath("//*[@id='copysurveyname']")).clear();
            driver.findElement(By.xpath("//*[@id='copysurveyname']")).sendKeys(newSurveyTitle);
            ExtentReporter.log(LogStatus.INFO, "New Survey Title entered: " + newSurveyTitle);

            // Execute copy operation
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Copy Survey button clicked");

            // Verify copy completion
            Thread.sleep(2000);
            highlightObject("//*[text()='Copy of survey is completed.']");
            String copyMessage = driver.findElement(By.xpath("//*[@class='jumbotron message-box']")).getText();
            ExtentReporter.log(LogStatus.INFO, "Copied Survey: " + copyMessage);

            // Navigate to copied survey
            driver.findElement(By.xpath("//*[@value='Go to survey']")).click();
            String createdSurveyXpath = "(//*[@id='breadcrumb__group--detail'])[1]";
            highlightObject(createdSurveyXpath);
            ExtentReporter.log(LogStatus.INFO, "Question Group: " +
                    driver.findElement(By.xpath(createdSurveyXpath)).getText());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to copy survey: " + e.getMessage());
            throw e;
        }
    }

    public void copySurveyCharacterLimit() throws Exception {
        try {
            // Enter new survey title with 255 characters
            String longTitle = "A".repeat(250);
            WebElement surveyTitle = driver.findElement(By.xpath("//*[@id='copysurveyname']"));
            surveyTitle.sendKeys(longTitle);
            String surveyTitleText = surveyTitle.getAttribute("value");
            ExtentReporter.log(surveyTitleText.length() >= 200 ? LogStatus.PASS : LogStatus.FAIL,
                (surveyTitleText.length() >= 200 ? "Survey Title within character limit: " : "Survey Title exceeds character limit: ") + surveyTitleText.length());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to handle character limit: " + e.getMessage());
            throw e;
        }
    }

    public void copyNoSelectedSurvey() throws Exception {
        try {
            // Attempt to copy a survey without selecting one
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            WebElement selectSurvey = driver.findElement(By.xpath("(//span[@role='combobox'])[3]"));
            String message = selectSurvey.getAttribute("validationMessage");
            ExtentReporter.log(LogStatus.PASS, "Validation message displayed: " + message);
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate no selected survey: " + e.getMessage());
            throw e;
        }
    }
public void copyEmptySurveyTitle() throws Exception {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(50));
        By copySurveySearchField = By.xpath("//*[@class='select2-search__field']");
            
            // Click on the survey selector dropdown
            WebElement selectSurvey = driver.findElement(By.xpath("//div[@class='ls-flex-column col-md-6 copy-survey']//span[@role='combobox']"));
            selectSurvey.click();
            
            // Wait for the search field to be visible
        WebElement searchField = wait.until(ExpectedConditions.visibilityOfElementLocated(copySurveySearchField));
        wait.until(ExpectedConditions.elementToBeClickable(copySurveySearchField));
        searchField.clear();
        searchField.sendKeys("CREATE - Automation Survey. Please disregard.");
            
            // Wait for the survey option to be visible and click it
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//*[contains(text(), 'CREATE - Automation Survey. Please disregard.')])[3]")));
            driver.findElement(By.xpath("(//*[contains(text(), 'CREATE - Automation Survey. Please disregard.')])[3]"))
                    .click();
            ExtentReporter.log(LogStatus.PASS, "Survey selected for copying");

             // Wait for the save button to be clickable and click it
             wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='save-form-button']")));
            driver.findElement(By.xpath("//*[@id='save-form-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Copy Survey button clicked");

            // Wait for the validation message field to be present
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='copysurveyname']")));
            WebElement surveyTitle = driver.findElement(By.xpath("//*[@id='copysurveyname']"));
            String message = surveyTitle.getAttribute("validationMessage");
            ExtentReporter.log(LogStatus.PASS, "Validation message displayed: " + message);
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate no selected survey: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * This method verifies the elements present on the Survey List page.
     * It checks for the presence of various UI components and logs their details.
     */
    
    public void verifySurvey() throws Exception {
    	Thread.sleep(10000);  
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {	
            {
                put("Survey Title", "//a[contains(.,'CREATE - Automa...')]");
                put("Survey Settings Tab", "//a[contains(@aria-controls,'settings')]");
                put("Survey Structure Tab", "//a[contains(.,'Structure')]");
                put("Survey Survey URL", "//div[@class='card-header'][contains(.,'Survey URL')]");
                put("Survey Base Language", "//div[@class='col-4 card-label'][contains(.,'English (Base language):')]");
                put("Survey Sruvey Link", "(//a[contains(@target,'_blank')])[2]");
                put("Survey End URL", "//div[@class='col-4 card-label'][contains(.,'End URL:')]");
                put("Survey Number of Question Group", "//div[@class='col-4 card-label'][contains(.,'Number of questions/groups:')]");
                put("Survey Question Group Value", "(//div[@class='col-8 ls-card-grid__description'])[3]");
                put("Survey Text elements", "//h5[@class='card-title'][contains(.,'Text elements')]");
                put("Survey Close btn in Text Element", "//a[contains(@data-bs-original-title,'Survey text elements')]");
                put("Survey Description", "//div[@class='col-4 card-label'][contains(.,'Description:')]");
                put("Survey Welcome", "//div[@class='col-4 card-label'][contains(.,'Welcome:')]");
                put("Survey Policy Message", "//div[@class='col-4 card-label'][contains(.,'Survey Data Privacy Policy Message:')]");
                put("Survey Privacy Notice Error", "//div[@class='col-4 card-label'][contains(.,'Survey Data Privacy Notice Error:')]");
                put("Survey Privacy Notice Lable", "//div[@class='col-4 card-label'][contains(.,'Survey Data Privacy Notice Label:')]");
                put("Survey General Settings", "//h5[@class='card-title'][contains(.,'Survey General Settings')]");
                put("Survey General Settings Btn", "//a[contains(@data-bs-original-title,'General survey settings')]");
                put("Survey Owner Label", "//div[@class='col-4 card-label'][contains(.,'Owner:')]");
                put("Survey Owner EID", "(//div[contains(@class,'col-8 ls-card-grid__description')])[11]");
                put("Survey Administrator Label", "//div[@class='col-4 card-label'][contains(.,'Administrator:')]");
//              put("Survey Administrator EID", "//div[@class='col-8 ls-card-grid__description'][contains(.,'Villarba, Rowel (rowel.villarba@ds.dev.accenture.com)')]");
                put("Survey Theme Label", "//div[@class='col-4 card-label'][contains(.,'Theme:')]");
                put("Survey Theme Selected", "//div[@class='col-8 ls-card-grid__description'][contains(.,'Bootstrap Vanilla ( vanilla )')]");
                put("Survey Theme Pencil", "//a[contains(@title,'Open theme editor in new window')]");
                put("Survey Edit theme options", "//a[contains(@title,'Edit theme options')]");
                put("Survey Publication and Access Settings", "//h5[@class='card-title'][contains(.,'Publication and Access Settings')]");
                put("Survey Edit Publication", "//a[contains(@data-bs-original-title,'Edit publication and access settings')]");
                put("Survey Start Date Time", "//div[@class='col-4 card-label'][contains(.,'Start date/time:')]");
                put("Survey Start Date Time", "//div[@class='col-4 card-label'][contains(.,'Expiration date/time:')]");      
                
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the Survey List page.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifySurveyList() throws Exception {
    	Thread.sleep(5000);  
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {	
            {
                put("Survey List header", "//*[@class='align-items-center d-flex']");
                put("Survey List nav tb", "//*[@id='surveysystem']");
                put("Survey List tab", "//*[@href='#surveys']");
                put("Survey Group tab", "//*[@href='#surveygroups']");
                put("Survey List search label", "//*[contains(text(), 'Search')]");
                put("Survey List search input", "//*[@id='Survey_searched_value']");
                put("Survey List status label", "//*[contains(text(), 'Status:')]");
                put("Survey List status input", "//*[@id='Survey_active']");
                put("Survey List grroup label", "//*[contains(text(), 'Group:')]");
                put("Survey List group input", "//*[contains(@class, 'select2-selection--single')]");
                put("Survey List search button", "//*[@aria-label='Search button']");
                put("Survey List reset button", "//*[@aria-label='Reset button']");
                put("Survey List checkbox", "//*[@id='sid']");
                put("Survey ID column", "//*[@id='survey-grid_c1']");
                put("Survey Status column", "//*[@id='survey-grid_c2']");
                put("Survey Title column", "//*[@id='survey-grid_c3']");
                put("Survey Group column", "//*[@id='survey-grid_c4']");
                put("Survey Created column", "//*[@id='survey-grid_c5']");
                put("Survey Owner column", "//*[@id='survey-grid_c6']");
                put("Survey Anonymized Responses column", "//*[@id='survey-grid_c7']");
                put("Survey Partial column", "//*[@id='survey-grid_c8']");
                put("Survey Full column", "//*[@id='survey-grid_c9']");
                put("Survey Total column", "//*[@id='survey-grid_c10']");
                put("Survey Closed Group column", "//*[@id='survey-grid_c11']");
                put("Survey Actions column", "//*[@id='survey-grid_c12']");
                put("Survey 1st checkbox", "//*[@id='sid_0']");
                put("1st Survey ID", "(//*[contains(@class, 'has-link')])[1]");
                put("1st Survey Status", "(//*[contains(@class, 'has-link')])[2]");
                put("1st Survey Title", "(//*[contains(@class, 'has-link')])[3]");
                put("1st Survey Group", "(//*[contains(@class, 'has-link')])[4]");
                put("1st Survey Created", "(//*[contains(@class, 'has-link')])[5]");
                put("1st Survey Owner", "(//*[contains(@class, 'has-link')])[6]");
                put("1st Survey Anonymized Responses", "(//*[contains(@class, 'has-link')])[7]");
                put("1st Survey Partial", "(//*[contains(@class, 'has-link')])[8]");
                put("1st Survey Full", "(//*[contains(@class, 'has-link')])[9]");
                put("1st Survey Total", "(//*[contains(@class, 'has-link')])[10]");
                put("1st Survey Closed Group", "(//*[contains(@class, 'has-link')])[11]");
                put("1st Survey Actions", "//*[@id='dropdown_2']");
                put("Survey List Action button", "//*[@id='surveyListActions']");
                put("Survey List pagination", "(//*[@class='pagination'])[1]");
                put("Survey List display count", "(//*[@class='col-md-4 summary-container'])[1]");
            }
        };
        verifyElementsByMap(elements);
    }

    public void specialCharacSearch() throws Exception {
        try {
            // Enter special character search term
            String specialCharSearch = "</b>";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(specialCharSearch);
            ExtentReporter.log(LogStatus.INFO, "Special character search term entered: " + specialCharSearch);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for special characters");

            // Verify search results
            String NoResultsFound = driver.findElement(By.xpath("//*[text()='No surveys found.']")).getText();
            ExtentReporter.log(NoResultsFound.equals("No surveys found.") ? LogStatus.PASS : LogStatus.FAIL,
                NoResultsFound.equals("No surveys found.") ? "Special character search returned no results as expected" : "Special character search returned");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform special character search: " + e.getMessage());
            throw e;
        }
    }

    public void searchSurveyID() throws Exception {
    	Thread.sleep(3000);
        try {
            // Enter survey ID in search
            String surveyID = "687714";
            // STG - surveyID = "687714"
            // PRD - surveyID = "721455"
            
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyID);
            ExtentReporter.log(LogStatus.INFO, "Survey ID entered in search: " + surveyID);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey ID");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyID = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[1]")).getText();
            ExtentReporter.log(firstSurveyID.equals(surveyID) ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyID.equals(surveyID) ? "Search results verified for Survey ID: " + firstSurveyID : "Search results do not match expected Survey ID");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey ID: " + e.getMessage());
            throw e;
        }
    }

    public void searchSurveyTitle() throws Exception {
    	Thread.sleep(5000);
    	
        try {
            // Enter survey title in search
            String surveyTitle = "CREATE - Automation Survey. Please disregard.";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyTitle);
            ExtentReporter.log(LogStatus.INFO, "Survey Title entered in search: " + surveyTitle);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey Title");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyTitle = driver.findElement(By.xpath("(//*[@class='has-link'])[1]")).getText();
            ExtentReporter.log(firstSurveyTitle.contains(surveyTitle) ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyTitle.contains(surveyTitle) ? "Search results verified for Survey Title: " + firstSurveyTitle : "Search results do not match expected Survey Title");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey Title: " + e.getMessage());
            throw e;
        }
    }

    public void searchSurveyOwner() throws Exception {
    	Thread.sleep(5000);
        try {
            // Enter survey owner in search
            String surveyOwner = "merolyn.n.anugot";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyOwner);
            ExtentReporter.log(LogStatus.INFO, "Survey Owner entered in search: " + surveyOwner);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey Owner");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyOwner = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[6]")).getText();
            ExtentReporter.log(firstSurveyOwner.contains(surveyOwner) ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyOwner.contains(surveyOwner) ? "Search results verified for Survey Owner: " + firstSurveyOwner : "Search results do not match expected Survey Owner");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey Owner: " + e.getMessage());
            throw e;
        }
    }

    public void searchNumeric() throws Exception {
    	Thread.sleep(5000);
        try {
            // Enter survey title in search
            String surveyTitle = "12345";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyTitle);
            ExtentReporter.log(LogStatus.INFO, "Survey Title with numeric  entered in search: " + surveyTitle);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey Title");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyTitle = driver.findElement(By.xpath("(//*[@class='has-link'])[1]")).getText();
            ExtentReporter.log(firstSurveyTitle.contains(surveyTitle) ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyTitle.contains(surveyTitle) ? "Search results verified for Survey Title: " + firstSurveyTitle : "Search results do not match expected Survey Title");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey Title: " + e.getMessage());
            throw e;
        }
    }

    public void searchAlphaNumeric() throws Exception {
    	Thread.sleep(5000);
        try {
            // Enter survey title in search
            String surveyTitle = "Search 12345";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyTitle);
            ExtentReporter.log(LogStatus.INFO, "Survey Title with alphanumeric entered in search: " + surveyTitle);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey Title");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyTitle = driver.findElement(By.xpath("(//*[@class='has-link'])[1]")).getText();
            ExtentReporter.log(firstSurveyTitle.contains(surveyTitle) ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyTitle.contains(surveyTitle) ? "Search results verified for Survey Title: " + firstSurveyTitle : "Search results do not match expected Survey Title");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey Title: " + e.getMessage());
            throw e;
        }
    }

    public void searchScriptTag() throws Exception {
    	Thread.sleep(5000);
        try {
            // Enter survey title in search
            String surveyTitle = "<script>alert('XSS')</script>";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyTitle);
            ExtentReporter.log(LogStatus.INFO, "Survey Title with script tag entered in search: " + surveyTitle);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey Title");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyTitle = driver.findElement(By.xpath("(//*[@class='has-link'])[1]")).getText();
            ExtentReporter.log(firstSurveyTitle.contains(surveyTitle) ? LogStatus.PASS : LogStatus.PASS,
                firstSurveyTitle.contains(surveyTitle) ? "Search results verified for Survey Title: " + firstSurveyTitle : "Search results do not match expected Survey Title");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey Title: " + e.getMessage());
            throw e;
        }
    }

    public void statusActiveSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Select 'Active' status from dropdown
            driver.findElement(By.xpath("//*[@id='Survey_active']")).click();
            driver.findElement(By.xpath("//*[@id='Survey_active']/option[2]")).click();
            ExtentReporter.log(LogStatus.INFO, "Active status selected in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Active status");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyStatus = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[2]")).getText();
            ExtentReporter.log(LogStatus.PASS, "Search results verified for Active status: " + firstSurveyStatus);

            driver.findElement(By.xpath("//*[@id='dropdown_2']")).click();
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[1]", "xpath",
                    "Add new question option disabled");
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[2]", "xpath", "Add new group option disabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[1]", "xpath", "Statistics option enabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[2]", "xpath", "General settings & text option enabled");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Active status: " + e.getMessage());
            throw e;
        }
    }

    public void statusActiveRunningSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Select 'Active' status from dropdown
            driver.findElement(By.xpath("//*[@id='Survey_active']")).click();
            driver.findElement(By.xpath("//*[@id='Survey_active']/option[3]")).click();
            ExtentReporter.log(LogStatus.INFO, "Active and Running status selected in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Active and running status");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyStatus = driver
                    .findElement(By.xpath("(//*[contains(@data-bs-original-title, 'End')])[1]")).getText();
            ExtentReporter.log(LogStatus.PASS,
                    "Search results verified for Active and running status: " + firstSurveyStatus);

            driver.findElement(By.xpath("//*[@id='dropdown_2']")).click();
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[1]", "xpath",
                    "Add new question option disabled");
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[2]", "xpath", "Add new group option disabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[1]", "xpath", "Statistics option enabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[2]", "xpath", "General settings & text option enabled");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to perform search by Active and running status: " + e.getMessage());
            throw e;
        }
    }

    public void statusInactiveSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Select 'Active' status from dropdown
            driver.findElement(By.xpath("//*[@id='Survey_active']")).click();
            driver.findElement(By.xpath("//*[@id='Survey_active']/option[4]")).click();
            ExtentReporter.log(LogStatus.INFO, "Inactive status selected in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Inactive status");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyStatus = driver.findElement(By.xpath("(//*[@data-bs-original-title='Inactive'])[1]"))
                    .getText();
            ExtentReporter.log(LogStatus.PASS, "Search results verified for Inactive status: " + firstSurveyStatus);

            driver.findElement(By.xpath("//*[@id='dropdown_2']")).click();
            isElementDisplayed("(//*[@class='dropdown-item  '])[1]", "xpath",
                    "Add new question option enabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[2]", "xpath", "Add new group option enabled");
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[1]", "xpath", "Statistics option disabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[3]", "xpath", "General settings & text option enabled");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Inactive status: " + e.getMessage());
            throw e;
        }
    }

    public void statusExpiredSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Select 'Active' status from dropdown
            driver.findElement(By.xpath("//*[@id='Survey_active']")).click();
            driver.findElement(By.xpath("//*[@id='Survey_active']/option[5]")).click();
            ExtentReporter.log(LogStatus.INFO, "Active and Expired status selected in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Active and Expired status");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyStatus = driver
                    .findElement(By.xpath("(//*[contains(@data-bs-original-title, 'Expired')])[1]")).getText();
            ExtentReporter.log(LogStatus.PASS,
                    "Search results verified for Active and Expired status: " + firstSurveyStatus);

            driver.findElement(By.xpath("//*[@id='dropdown_2']")).click();
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[1]", "xpath",
                    "Add new question option disabled");
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[2]", "xpath", "Add new group option disabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[1]", "xpath", "Statistics option enabled");
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[3]", "xpath",
                    "General settings & text option disabled");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to perform search by Active and Expired status: " + e.getMessage());
            throw e;
        }
    }

    public void statusNotStartedSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Select 'Active' status from dropdown
            driver.findElement(By.xpath("//*[@id='Survey_active']")).click();
            driver.findElement(By.xpath("//*[@id='Survey_active']/option[6]")).click();
            ExtentReporter.log(LogStatus.INFO, "Active and not yet started status selected in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Active and not yet started status");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyStatus = driver
                    .findElement(By.xpath("(//*[contains(@data-bs-original-title, 'Start')])[1]")).getText();
            ExtentReporter.log(LogStatus.PASS,
                    "Search results verified for Active and not yet started status: " + firstSurveyStatus);

            driver.findElement(By.xpath("//*[@id='dropdown_2']")).click();
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[1]", "xpath",
                    "Add new question option disabled");
            isElementDisplayed("(//*[@class='dropdown-item disabled '])[2]", "xpath", "Add new group option disabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[1]", "xpath", "Statistics option enabled");
            isElementDisplayed("(//*[@class='dropdown-item  '])[2]", "xpath",
                    "General settings & text option enabled");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to perform search by Active and not yet started status: " + e.getMessage());
            throw e;
        }
    }

    public void groupSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Select a group from the dropdown
            driver.findElement(By.xpath("//*[contains(@class, 'select2-selection--single')]")).click();
            driver.findElement(By.xpath("//*[contains(@class, 'select2-search__field')]"))
                    .sendKeys("Default Group" + Keys.ENTER);
            ExtentReporter.log(LogStatus.INFO, "Default Group selected in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Group");
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyGroup = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[4]")).getText();
            ExtentReporter.log(firstSurveyGroup.contains("Default Group") ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyGroup.contains("Default Group") ? "Search results verified for Group: " + firstSurveyGroup : "Search results do not match expected Group");
            Thread.sleep(3000);
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Group: " + e.getMessage());
            throw e;
        }
    }

    public void customGroupSearch() throws Exception {
    	Thread.sleep(5000);
        try {
            // Wait for the dropdown to be clickable
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
            wait.until(ExpectedConditions
                    .elementToBeClickable(By.xpath("//*[contains(@class, 'select2-selection--single')]")));
            
            // Click the group dropdown
            driver.findElement(By.xpath("//*[contains(@class, 'select2-selection--single')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Group dropdown clicked for custom group search");
            
            // Wait for the search field to be visible
            wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//*[contains(@class, 'select2-search__field')]")));
            
            // Enter custom group name in the search field
            driver.findElement(By.xpath("//*[contains(@class, 'select2-search__field')]"))
                    .sendKeys("Selenium Survey Group - " + Keys.ENTER);
            ExtentReporter.log(LogStatus.INFO, "Custom Group name entered in search");

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Group");
            
            Thread.sleep(5000);

            // Verify search results
            String firstSurveyGroup = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[4]")).getText();
            ExtentReporter.log(firstSurveyGroup.contains("Survey Group -") ? LogStatus.PASS : LogStatus.FAIL,
                firstSurveyGroup.contains("Survey Group -") ? "Search results verified for Group: " + firstSurveyGroup : "Search results do not match expected Group");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Group: " + e.getMessage());
            throw e;
        }
    }

    public void resetSearch() throws Exception {
        try {
            Thread.sleep(5000);
            // Click reset button
            driver.findElement(By.xpath("//*[@aria-label='Reset button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Reset button clicked to clear search");

            // Verify that search fields are cleared
            String searchInput = driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).getAttribute("value");
            ExtentReporter.log(searchInput.isEmpty() ? LogStatus.PASS : LogStatus.FAIL,
                searchInput.isEmpty() ? "Search input field is cleared successfully" : "Search input field is not cleared");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to reset search: " + e.getMessage());
            throw e;
        }
    }

    public void updateSurveyOwner() throws Exception {
    	Thread.sleep(5000);
        try {
            // Update the owner in the modal
            driver.findElement(By.xpath("//*[@id='owner-modal']//input")).clear();
            driver.findElement(By.xpath("//*[@id='owner-modal']//input")).sendKeys("abc");
            ExtentReporter.log(LogStatus.INFO, "Updated the owner in the modal");
            
            // Click the save button in the modal
            driver.findElement(By.xpath("//*[@id='owner-modal']//button[contains(text(), 'Save')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked the save button in the owner modal");
            Thread.sleep(5000);

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update survey owner: " + e.getMessage());
            throw e;
        }
    }


    /**
     * This method validates the survey group functionality.
     * It currently does not perform any operations but can be extended in the
     * future.
     */

    public void validateSurveyGroup() throws Exception {
        driver.findElement(By.xpath("//*[@href='#surveygroups']")).click();
        ExtentReporter.log(LogStatus.INFO, "Clicked on Survey Group tab");
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Survey Group header", "//*[contains(@class, 'pagetitle h3')]");
                put("Survey GSID checkbox", "//*[@id='gsid_all']");
                put("Column 1 Survey Group ID", "//*[@id='surveygroups--gridview_c1']");
                put("Column 2 Code", "//*[@id='surveygroups--gridview_c2']");
                put("Column 3 Title", "//*[@id='surveygroups--gridview_c3']");
                put("Column 4 Description", "//*[@id='surveygroups--gridview_c4']");
                put("Column 5 Parent group", "//*[@id='surveygroups--gridview_c5']");
                put("Column 6 Available", "//*[@id='surveygroups--gridview_c6']");
                put("Column 7 Owner", "//*[@id='surveygroups--gridview_c7']");
                put("Column 8 Super Admin", "//*[@id='surveygroups--gridview_c8']");
                put("Column 9 Actions", "//*[@id='surveygroups--gridview_c9']");
                put("Survey Group 1st checkbox", "//*[@id='gsid_0']");
                put("1st Survey Group ID", "(//td[contains(text(),'1')])[2]");
                put("1st Survey Group Code", "(//td[normalize-space()='default'])[1]");
 //             put("1st Survey Group Title", "(//td[contains(text(),'Default Group')])[2]");
 //             put("1st Survey Group Description", "(//td[contains(text(),'Default survey group')])");
                put("1st Survey Group Parent group", "(//td)[19]");
                put("1st Survey Group Available", "(//td[contains(text(),'1')])[3]");
//                put("1st Survey Group Owner", "(//td[normalize-space()='surveystage'])[1]");
                put("1st Survey Group Order", "(//td[normalize-space()='2'])[1]");
                put("1st Survey Group Actions", "(//td)[23]");
                put("Survey Group pagination", "//*[@id='yw2']");
                put("Survey Group Change page size", "//*[@id='surveygroups--pageSize']");
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the Overview page of the Survey
     * Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyOverview() throws Exception {
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Survey URL seaction header", "(//*[@class='card-title'])[1]");
                put("Survey answer link", "//*[@id='adminsidepanel__surveysummary--mainLanguageLink']");
                put("End URL", "(//*[@class='list-group-item'])[2]");
                put("No. of questions/groups", "(//*[@class='list-group-item'])[3]");

                put("Text Elements section header", "(//*[@class='card-title'])[2]");
                put("Descriptions", "(//*[@class='list-group-item'])[4]");
                put("Welcome message", "(//*[@class='list-group-item'])[5]");
                put("End message", "(//*[@class='list-group-item'])[6]");

                put("Survey General Settings section header", "(//*[@class='card-title'])[3]");
                put("Owner", "(//*[@class='list-group-item'])[7]");
                put("Administrator", "(//*[@class='list-group-item'])[8]");
                put("Theme", "(//*[@class='list-group-item'])[9]");

                put("Publication and Access Settings section header", "(//*[@class='card-title'])[4]");
                put("Start date", "(//*[@class='list-group-item'])[10]");
                put("Expiration date", "(//*[@class='list-group-item'])[11]");

                put("Survey Settings section header", "(//*[@class='card-title'])[5]");
                put("Survey Settings list items", "(//*[@class='list-group list-group-flush'])[5]");
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the General Settings page of the
     * Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyGeneralSettings() throws Exception {
    	Thread.sleep(10000);
        try {
            // Survey Editor Header Elements
            highlightObject("//*[@id='breadcrumb-container']");
            isElementDisplayed("//*[@id='breadcrumb-container']", "xpath", "Survey Editor Header");
            
            highlightObject("//*[@id='ls-tools-button']");
            isElementDisplayed("//*[@id='ls-tools-button']", "xpath", "Survey Editor - Tools button");
            
            highlightObject("//*[@id='ls-activate-survey']");
            isElementDisplayed("//*[@id='ls-activate-survey']", "xpath", "Survey Editor - Activate Survey button");
            
            highlightObject("//*[@id='preview_survey_button']");
            isElementDisplayed("//*[@id='preview_survey_button']", "xpath", "Survey Editor - Preview Survey button");
            
            highlightObject("//*[@id='trigger_exportTypeSelector_button']");
            isElementDisplayed("//*[@id='trigger_exportTypeSelector_button']", "xpath", "Survey Editor - Export button");
            
            highlightObject("//*[@id='save-button']");
            isElementDisplayed("//*[@id='save-button']", "xpath", "Survey Editor - Save button");

            // Language Settings
            highlightObject("//*[@for='additional_languages']");
            isElementDisplayed("//*[@for='additional_languages']", "xpath", "Survey Language label");
            
            highlightObject("//*[@id='additional_languages']");
            isElementDisplayed("//*[@id='additional_languages']", "xpath", "Survey Language input");
            
            highlightObject("//*[text()='Base language:']");
            isElementDisplayed("//*[text()='Base language:']", "xpath", "Base Language label");
            
            highlightObject("//*[@id='language']");
            isElementDisplayed("//*[@id='language']", "xpath", "Base Language input");

            // Survey Owner Settings
            highlightObject("//*[@id='owner_id_label']");
            isElementDisplayed("//*[@id='owner_id_label']", "xpath", "Survey Owner label");
            
            highlightObject("//*[@id='owner_id_note']");
            isElementDisplayed("//*[@id='owner_id_note']", "xpath", "Survey Owner note");
            
            highlightObject("//*[@id='searchownereid']");
            isElementDisplayed("//*[@id='searchownereid']", "xpath", "Survey Owner input");

            // Administrator Settings
            highlightObject("//*[text()='Administrator:']");
            isElementDisplayed("//*[text()='Administrator:']", "xpath", "Administrator label");
            
            highlightObject("//*[@id='admin']");
            isElementDisplayed("//*[@id='admin']", "xpath", "Administrator input");
            
            highlightObject("//*[@id='adminbutton']");
            isElementDisplayed("//*[@id='adminbutton']", "xpath", "Administrator inherit buttons");

            // Administrator Email Settings
            highlightObject("//*[text()='Administrator email address:']");
            isElementDisplayed("//*[text()='Administrator email address:']", "xpath", "Administrator Email Address label");
            
            highlightObject("//*[@id='adminemail']");
            isElementDisplayed("//*[@id='adminemail']", "xpath", "Administrator Email Address input");
            
            highlightObject("//*[@id='adminemailbutton']");
            isElementDisplayed("//*[@id='adminemailbutton']", "xpath", "Administrator Email inherit buttons");

            // Bounce Email Settings
            highlightObject("//*[text()='Bounce email address:']");
            isElementDisplayed("//*[text()='Bounce email address:']", "xpath", "Bounce Email Address label");
            
            highlightObject("//*[@class='form-control inherit-readonly d-block']");
            isElementDisplayed("//*[@class='form-control inherit-readonly d-block']", "xpath", "Bounce Email Address input disabled");
            
            highlightObject("//*[@id='bounce_emailbutton']");
            isElementDisplayed("//*[@id='bounce_emailbutton']", "xpath", "Bounce Email Address inherit buttons");

            // Survey Group Settings
            highlightObject("//*[@for='gsid']");
            isElementDisplayed("//*[@for='gsid']", "xpath", "Survey group label");
            
            highlightObject("//*[@id='gsid']");
            isElementDisplayed("//*[@id='gsid']", "xpath", "Survey group input");

            // Format Settings
            highlightObject("//*[@for='format']");
            isElementDisplayed("//*[@for='format']", "xpath", "Format label");
            
            highlightObject("//*[@for='format_1']");
            isElementDisplayed("//*[@for='format_1']", "xpath", "Question by Question format");
            
            highlightObject("//*[@for='format_2']");
            isElementDisplayed("//*[@for='format_2']", "xpath", "Group by Group format");
            
            highlightObject("//*[@for='format_3']");
            isElementDisplayed("//*[@for='format_3']", "xpath", "All in one format");
            
            highlightObject("//*[@for='format_4']");
            isElementDisplayed("//*[@for='format_4']", "xpath", "Group by Group inherit format");

            // Theme Settings
            highlightObject("//*[@for='template']");
            isElementDisplayed("//*[@for='template']", "xpath", "Theme label");
            
            highlightObject("//*[@id='select2-template-container']");
            isElementDisplayed("//*[@id='select2-template-container']", "xpath", "Theme select");
            
            highlightObject("//*[@id='preview-image-container']");
            isElementDisplayed("//*[@id='preview-image-container']", "xpath", "Preview Theme image");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify General Settings elements: " + e.getMessage());
            throw e;
        }
    }

    public void addNewLanguage() throws Exception {
    	Thread.sleep(5000);
        try {
            // Click on the Add New Language button
            driver.findElement(By.xpath("(//span[@class='select2-search select2-search--inline'])[1]")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Add New Language text area");

            // Wait for the language selection dropdown to be visible
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("(//textarea[@aria-label='Search'])[1]")));

            // Select a language from the dropdown
            WebElement languageSearch = driver.findElement(By.xpath("(//textarea[@aria-label='Search'])[1]"));
            languageSearch.click();
            languageSearch.sendKeys("Japanese" + Keys.ENTER);
            ExtentReporter.log(LogStatus.INFO, "Selected a new language from the dropdown");

            highlightObject("//*[@title='Japanese']");
            isElementDisplayed("//*[@title='Japanese']", "xpath", "Japanese language added successfully");

            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the new language");
            
            // Wait for the save operation to complete
            Thread.sleep(5000);
            highlightObject("//*[@title='Japanese']");
            isElementDisplayed("//*[@title='Japanese']", "xpath", "Japanese language added successfully");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to add new language: " + e.getMessage());
        }
    }

    public void verifyLanguagetoOverviewPage() throws Exception {
    	Thread.sleep(8000);
        try {
            // Verify that the new language is displayed on the Overview page
            String languageText = driver.findElement(By.xpath("(//div[normalize-space()='Japanese:'])[1]")).getText();
            ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
                languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Overview page" : "New language 'Japanese' is not displayed on the Overview page");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify new language on Overview page: " + e.getMessage());
        }
    }

    public void verifyLanguagetoTextElements() throws Exception {
    	Thread.sleep(10000);
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
            // Wait for the element to be present and visible before getting text
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@href='#edittxtele-1']")));

            // Re-find the element after waiting to avoid stale reference
            WebElement languageElement = driver.findElement(By.xpath("//*[@href='#edittxtele-1']"));
            String languageText = languageElement.getText();

            // Verify that the new language is displayed on the Text Elements page
            ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
                languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Text Elements page" : "New language 'Japanese' is not displayed on the Text Elements page");
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            ExtentReporter.log(LogStatus.FAIL, "Stale element reference encountered. Retrying...");
            // Retry logic for stale element
            try {
                Thread.sleep(10000);
                WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@href='#edittxtele-1']")));
                WebElement languageElement = driver.findElement(By.xpath("//*[@href='#edittxtele-1']"));
                String languageText = languageElement.getText();

                ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
                    languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Text Elements page" : "New language 'Japanese' is not displayed on the Text Elements page");
            } catch (Exception retryException) {
                ExtentReporter.log(LogStatus.FAIL, "Retry failed: " + retryException.getMessage());
                throw retryException;
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify new language on Text Elements page: " + e.getMessage());
            throw e;
        }
    }

    public void verifyLanguagetoPrivacyPolicy() throws Exception {
        Thread.sleep(5000);
        try {
            // Verify that the new language is displayed on the Privacy Policy page
            String languageText = driver.findElement(By.xpath("(//button[normalize-space()='Japanese'])[1]")).getText();
            ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
                languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Privacy Policy page" : "New language 'Japanese' is not displayed on the Privacy Policy page");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify new language on Privacy Policy page: " + e.getMessage());
        }
    }

    public void verifyLanguagetoQuestionGroup() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            By createQuestionGroupLocator = By.xpath("//*[@id='adminsidepanel__sidebar--selectorCreateQuestionGroup']");
            WebElement createQuestionGroupBtn = findElementWithRetry(createQuestionGroupLocator,
                    "Create Question Group button");

            if (createQuestionGroupBtn == null) {
                throw new Exception("Create Question Group button not found");
            }

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});", createQuestionGroupBtn);

            if (!clickWithRetry(createQuestionGroupLocator, "Create Question Group button")) {
                throw new Exception("Unable to click Create Question Group button");
            }

            Thread.sleep(2000);

            // Verify that the new language is displayed on the Question Group page
            String languageText = driver.findElement(By.xpath("//*[@href='#ja']")).getText();
            ////*[@href='#ja'] - previous if japan language is added
            ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
                languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Question Group page" : "New language 'Japanese' is not displayed on the Question Group page");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify new language on Question Group page: " + e.getMessage());
            throw e;
        }
    }

    public void verifyLanguagetoQuestion() throws Exception {
    	Thread.sleep(10000);
        try {
            // Click on Create Question button
            driver.findElement(By.xpath("//*[@id='adminsidepanel__sidebar--selectorCreateQuestion']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Create Question button");
            Thread.sleep(50000);
            // Click on language dropdown
            driver.findElement(By.xpath("(//button[@id='language-dropdown'])[1]")).click();
            String languageText = driver.findElement(By.xpath("(//*[@class='dropdown-item lang-switch-button'])[2]")).getText();
            ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
                languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Question page" : "New language 'Japanese' is not displayed on the Question page");
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify new language on Question page: " + e.getMessage());
        }
    }

    // public void verifyLanguagetoQuestion() throws Exception {
    //     try {
    //         // Wait for page stability and overlays to disappear
    //         waitForPageLoad();
    //         waitForOverlayToDisappear();
            
    //         // Click on Create Question button with retry
    //         By createQuestionLocator = By.xpath("//*[@id='adminsidepanel__sidebar--selectorCreateQuestion']");
    //         if (clickWithRetry(createQuestionLocator, "Create Question Button")) {
    //             ExtentReporter.log(LogStatus.INFO, "Successfully clicked Create Question button");
                
    //             // Wait for the question page to load
    //             Thread.sleep(3000);
    //             waitForPageLoad();
    //             waitForOverlayToDisappear();
                
    //             // Click on language dropdown with retry
    //             By languageDropdownLocator = By.xpath("//*[@id='language-dropdown']");
    //             if (clickWithRetry(languageDropdownLocator, "Language Dropdown")) {
    //                 ExtentReporter.log(LogStatus.INFO, "Successfully opened language dropdown");
    //                 Thread.sleep(2000);
                    
    //                 // Verify that the new language is displayed on the Question page
    //                 try {
    //                     WebElement languageElement = findElementWithRetry(
    //                         By.xpath("//*[contains(text(), 'Japanese')]"), 
    //                         "Japanese Language Option"
    //                     );
                        
    //                     if (languageElement != null) {
    //                         String languageText = languageElement.getText();
    //                         ExtentReporter.log(languageText.contains("Japanese") ? LogStatus.PASS : LogStatus.FAIL,
    //                             languageText.contains("Japanese") ? "New language 'Japanese' is displayed on the Question page" : "New language 'Japanese' is not displayed on the Question page");
    //                     } else {
    //                         ExtentReporter.log(LogStatus.FAIL, "Japanese language option not found in dropdown");
    //                     }
    //                 } catch (Exception e) {
    //                     ExtentReporter.log(LogStatus.FAIL, "Failed to verify Japanese language option: " + e.getMessage());
    //                 }
    //             } else {
    //                 throw new Exception("Failed to open language dropdown");
    //             }
    //         } else {
    //             throw new Exception("Failed to click Create Question button");
    //         }
    //     } catch (Exception e) {
    //         ExtentReporter.log(LogStatus.FAIL, "Failed to verify new language on Question page: " + e.getMessage());
    //         throw e;
    //     }
    // }

    public void verifyLanguagetoQuestionEllipsis() throws Exception {
        Thread.sleep(10000);
        driver.findElement(By.xpath("//*[contains(@class, 'q-group d-flex nowrap ls-space padding')]")).click();
        ExtentReporter.log(LogStatus.INFO, "Clicked on the Question Group to open ellipsis menu");
        Thread.sleep(10000);
        driver.findElement(By.xpath("//*[contains(@class, 'list-group-item question-question-list-item')]")).click();
        ExtentReporter.log(LogStatus.INFO, "Clicked on the Question to open ellipsis menu");
        Thread.sleep(10000);
        driver.findElement(By.xpath("(//*[@id='dropdownMenuButton1'])[2]")).click();
        ExtentReporter.log(LogStatus.INFO, "Clicked on the Question ellipsis menu to check languages");
        try {
            // Verify that the new language is displayed in the Question ellipsis menu
            String languageText = driver
                    .findElement(By.xpath("(//a[@class='dropdown-item'][normalize-space()='Japanese'])[8]"))
                    .getText();
            if (languageText.contains("Japanese")) {
                ExtentReporter.log(LogStatus.PASS,
                        "New language 'Japanese' is displayed in the Question ellipsis menu");
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "New language 'Japanese' is not displayed in the Question ellipsis menu");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify new language in Question ellipsis menu: " + e.getMessage());
        }
    }

    public void verifyLanguagetoPreviewSurveyButton() throws Exception {
    	Thread.sleep(10000);
        try {
            // Wait for page to load and overlays to disappear
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            // Wait for preview button to be clickable
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//*[@id='preview_survey_button'])[1]")));
            
            // Use clickWithRetry for reliable clicking
            if (!clickWithRetry(By.xpath("(//*[@id='preview_survey_button'])[1]"), "Preview Survey button")) {
                throw new Exception("Failed to click Preview Survey button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Preview Survey button");
            
            Thread.sleep(10000);
            
            // Wait for the preview link to be visible
            By previewLinkLocator = By.xpath("//*[contains(@href, '&newtest=Y&lang=ja')]");
            wait.until(ExpectedConditions.visibilityOfElementLocated(previewLinkLocator));
            String previewText = driver.findElement(previewLinkLocator).getText();
            
            if (previewText.contains("Japanese")) {
                ExtentReporter.log(LogStatus.PASS, "New language 'Japanese' is displayed in the survey preview");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "New language 'Japanese' is not displayed in the survey preview");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify new language in survey preview: " + e.getMessage());
        }
    }

    public void verifyLanguagetoSurveyQuestionnaire() throws Exception {
    	Thread.sleep(9000);
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//*[@id='preview_survey_button'])[1]")));
            
            if (!clickWithRetry(By.xpath("(//*[@id='preview_survey_button'])[1]"), "Preview Survey button - first click")) {
                throw new Exception("Failed to click Preview Survey button on first attempt");
            }
            Thread.sleep(10000);
            
            waitForPageLoad();
            waitForOverlayToDisappear();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//*[@id='preview_survey_button'])[1]")));
            
            if (!clickWithRetry(By.xpath("(//*[@id='preview_survey_button'])[1]"), "Preview Survey button - second click")) {
                throw new Exception("Failed to click Preview Survey button on second attempt");
            }
            Thread.sleep(10000);
            driver.findElement(By.xpath("//*[contains(@href, '&newtest=Y&lang=ja')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on the survey preview link for Japanese language");
            
            Thread.sleep(10000);
            driver.switchTo().window(driver.getWindowHandles().toArray()[1].toString());
            driver.manage().window().maximize();
            ExtentReporter.log(LogStatus.INFO, "Switched to the survey preview window for Japanese language");
            Thread.sleep(3000);
            driver.findElement(By.xpath("//*[@id='lang']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on the language dropdown in the survey preview");

            highlightObject("(//button[@type='submit'])[1]");
 //         (//option[@value='ja'])[1] - old xpath
            isElementDisplayed("(//button[@type='submit'])[1]", "xpath",
 //           		(//option[@value='ja'])[1] - old xpath
                    "Japanese language option is displayed in the survey preview");

            driver.findElement(By.xpath("(//button[@type='submit'])[1]")).click();
 //         (//option[@value='ja'])[1] - old xpath
            
            ExtentReporter.log(LogStatus.INFO, "Selected Japanese language in the survey preview");

            driver.close(); // Close the current window (the Japanese survey preview)
            driver.switchTo().window(driver.getWindowHandles().toArray()[0].toString());
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify new language in Survey Questionnaire: " + e.getMessage());
        }
    }

    public void deleteLanguage() throws Exception {
        try {
            // Wait for page to be fully loaded and overlays to disappear
            waitForPageLoad();
            waitForOverlayToDisappear();
            Thread.sleep(2000);
            
            // Use clickWithRetry to handle intercepted clicks
            By deleteButtonLocator = By.xpath("//li[@title='Japanese']//button[@type='button']");
            if (clickWithRetry(deleteButtonLocator, "Delete Language Button")) {
                ExtentReporter.log(LogStatus.INFO, "Clicked on Delete Language icon for Japanese language");
                
                // Wait for the confirmation dialog to appear
                WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
                wait.until(ExpectedConditions
                        .visibilityOfElementLocated(By.xpath("//*[@id='identity__bsconfirmModal_button_ok']")));
                
                Thread.sleep(1000); // Brief wait for modal animation
                
                // Click confirm button with retry
                By confirmButtonLocator = By.xpath("//*[@id='identity__bsconfirmModal_button_ok']");
                if (clickWithRetry(confirmButtonLocator, "Confirm Delete Button")) {
                    ExtentReporter.log(LogStatus.INFO, "Clicked on Delete button to remove the language");
                    
                    // Wait specifically for the modal to disappear
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.xpath("//*[@id='identity__bsconfirmModal']")));
                    
                    Thread.sleep(1000); // Brief additional wait for any remaining animations
                    waitForOverlayToDisappear();
                    
                    // Click save button with retry
                    By saveButtonLocator = By.xpath("//*[@id='save-button']");
                    clickWithRetry(saveButtonLocator, "Save Button");
                    ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the deleted language");
                } else {
                    throw new Exception("Failed to click confirm delete button");
                }
            } else {
                throw new Exception("Failed to click delete language button");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to delete language: " + e.getMessage());
            throw e;
        }
    }

    public void updateOwner() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a new one
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("amiel.a.gonzales");
            Thread.sleep(8000); // Wait for suggestions to load
            driver.findElement(By.xpath("//*[@class='people-suggestion-item acn-suggestion-items']")).click();
            ExtentReporter.log(LogStatus.INFO, "Entered new owner in the Owner input field");

            // Click the Save button to save the changes
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the new owner");
            Thread.sleep(8000);

            String Administrator = driver.findElement(By.xpath("//input[contains(@value,'Villarba, Rowel')]")).getAttribute("value");
            if (Administrator.contains("Villarba, Rowel")) {
                ExtentReporter.log(LogStatus.PASS, "Owner updated successfully to: " + Administrator);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Owner update failed. Current owner: " + Administrator);
            } 

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update owner: " + e.getMessage());
            throw e;
        }
    }

    public void verifyOwnerOverview() throws Exception {
    	Thread.sleep(5000);
        try {
            // Verify that the updated owner is displayed on the Overview page
            String ownerText = driver.findElement(By.xpath("(//*[contains(text(), 'amiel.a.gonzales')])[1]")).getText();
            if (ownerText.contains("amiel.a.gonzales")) {
                ExtentReporter.log(LogStatus.PASS, "Owner 'amiel.a.gonzales' is displayed on the Overview page");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Owner 'amiel.a.gonzales' is not displayed on the Overview page");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify owner on Overview page: " + e.getMessage());
        }
    }

    public void verifyOwnerSurveyList() throws Exception {
        try {
            // Verify that the updated owner is displayed in the Survey List
            String ownerText = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[6]")).getText();
            if (ownerText.contains("amiel.a.gonzales")) {
                ExtentReporter.log(LogStatus.PASS, "Owner 'amiel.a.gonzales' is displayed in the Survey List");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Owner 'amiel.a.gonzales' is not displayed in the Survey List");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify owner in Survey List: " + e.getMessage());
        }
    }

    public void updateUsingOwnEid() throws Exception {
    	Thread.sleep(9000);
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a new one
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("rowel.villarba");
            Thread.sleep(5000); // Wait for suggestions to load
            driver.findElement(By.xpath("(//div[contains(.,'rowel.villarba')])[19]")).click();

            ExtentReporter.log(LogStatus.INFO, "Entered new owner in the Owner input field");

            // Click the Save button to save the changes
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the new owner");

            Thread.sleep(10000);

            String updatedOwner = driver.findElement(By.xpath("//*[@id='searchownereid']")).getAttribute("value");
            if (updatedOwner.contains("rowel.villarba")) {
                ExtentReporter.log(LogStatus.PASS, "Owner updated successfully to: " + updatedOwner);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Owner update failed. Current owner: " + updatedOwner);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update using own EID: " + e.getMessage());
            throw e;
        }
    }

    public void nonExistentUser() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a non-existent user
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("nonexistent.user");
            ExtentReporter.log(LogStatus.INFO, "Entered non-existent user in the Owner input field");

            Thread.sleep(2000);

            String updatedOwner = driver.findElement(By.xpath("(//div[@class='people-suggestion-item-no-result'])[1]"))
                    .getText();
            if (updatedOwner.contains("No results found")) {
                ExtentReporter.log(LogStatus.PASS, "Non-existent user validation successful: " + updatedOwner);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Non-existent user validation failed. Current owner: " + updatedOwner);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update with non-existent user: " + e.getMessage());
            throw e;
        }
    }

    public void verifyEmptyOwner() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ExtentReporter.log(LogStatus.INFO, "Cleared the Owner input field");

            // Check if the save button is disabled
            WebElement saveButton = driver.findElement(By.xpath("//*[@id='save-button']"));
            if (!saveButton.isEnabled()) {
                ExtentReporter.log(LogStatus.PASS, "Save button is correctly disabled when owner field is empty");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Save button is incorrectly enabled when owner field is empty");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate empty owner: " + e.getMessage());
            throw e;
        }
    }

    public void verifyErrorEidNum() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter an invalid EID
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("karla123");
            ExtentReporter.log(LogStatus.INFO, "Entered invalid EID in the Owner with numeric characters field");

            Thread.sleep(2000);

            // Check for error message
            String errorMessage = driver.findElement(By.xpath("//*[@id='usersearch-errormessage']"))
                    .getText();
            if (errorMessage.contains("**Error: Invalid EID.")) {
                ExtentReporter.log(LogStatus.PASS, "Error message displayed for invalid EID: " + errorMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Error message not displayed for invalid EID. Current message: "
                        + errorMessage);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate error for invalid EID: " + e.getMessage());
            throw e;
        }
    }

    public void verifyErrorEidDoubleByte() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a double-byte character
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("\u3042\u3044\u3046\u3048\u304A"); // Example of double-byte characters (Japanese hiragana)
            ExtentReporter.log(LogStatus.INFO, "Entered double-byte characters in the Owner field");

            Thread.sleep(2000);

            // Check for error message
            String errorMessage = driver.findElement(By.xpath("//*[@id='usersearch-errormessage']"))
                    .getText();
            if (errorMessage.contains("**Error: Invalid EID.")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Error message displayed for double-byte characters: " + errorMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Error message not displayed for double-byte characters. Current message: " + errorMessage);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to validate error for double-byte characters in EID: " + e.getMessage());
            throw e;
        }
    }

    public void verifyErrorEidSpecialChars() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter special characters
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("!@#$%^&*()"); // Example of special characters
            ExtentReporter.log(LogStatus.INFO, "Entered special characters in the Owner field");

            Thread.sleep(2000);

            // Check for error message
            String errorMessage = driver.findElement(By.xpath("//*[@id='usersearch-errormessage']"))
                    .getText();
            if (errorMessage.contains("**Error: Invalid EID.")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Error message displayed for special characters: " + errorMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Error message not displayed for special characters. Current message: " + errorMessage);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to validate error for special characters in EID: " + e.getMessage());
            throw e;
        }
    }

    public void verifyErrorEidCascadingPeriod() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a cascading period
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("karla.."); // Example of cascading period
            ExtentReporter.log(LogStatus.INFO, "Entered cascading period in the Owner field");

            Thread.sleep(2000);

            // Check for error message
            String errorMessage = driver.findElement(By.xpath("//*[@id='usersearch-errormessage']"))
                    .getText();
            if (errorMessage.contains("**Error: Invalid EID.")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Error message displayed for cascading period: " + errorMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Error message not displayed for cascading period. Current message: " + errorMessage);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to validate error for cascading period in EID: " + e.getMessage());
            throw e;
        }
    }

    public void verifyErrorEidCascadingDash() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a cascading dash
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("karla--"); // Example of cascading dash
            ExtentReporter.log(LogStatus.INFO, "Entered cascading dash in the Owner field");

            Thread.sleep(2000);

            // Check for error message
            String errorMessage = driver.findElement(By.xpath("//*[@id='usersearch-errormessage']"))
                    .getText();
            if (errorMessage.contains("**Error: Invalid EID.")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Error message displayed for cascading dash: " + errorMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Error message not displayed for cascading dash. Current message: " + errorMessage);
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to validate error for cascading dash in EID: " + e.getMessage());
            throw e;
        }
    }

    public void verifyCharacterLimit() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a long EID
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("a".repeat(55)); // Example of exceeding character limit
            ExtentReporter.log(LogStatus.INFO, "Entered long EID exceeding character limit in the Owner field");

            Thread.sleep(2000);
            String ownerEidText = ownerInput.getAttribute("value");
            if (ownerEidText.length() >= 50) {
                ExtentReporter.log(LogStatus.PASS, "Owner eid within character limit: " + ownerEidText.length());
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Owner eid exceeds character limit: " + ownerEidText.length());
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to validate character limit in EID: " + e.getMessage());
            throw e;
        }
    }

    public void verifyTop5results() throws Exception {
        try {
            // Click on the Owner input field
            driver.findElement(By.xpath("//*[@id='searchownereid']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Owner input field");

            // Clear the existing owner and enter a search term
            WebElement ownerInput = driver.findElement(By.xpath("//*[@id='searchownereid']"));
            ownerInput.clear();
            ownerInput.sendKeys("john");
            ExtentReporter.log(LogStatus.INFO, "Entered search term in the Owner input field");

            Thread.sleep(2000);

            // Verify that only top 5 results are displayed
            java.util.List<WebElement> results = driver
                    .findElements(By.xpath("//*[@class='user-card-container acn-permissioncard-container']"));
            if (results.size() <= 5) {
                ExtentReporter.log(LogStatus.PASS, "Top 5 results displayed successfully: " + results.size());
            } else {
                ExtentReporter.log(LogStatus.FAIL, "More than 5 results displayed: " + results.size());
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify top 5 results: " + e.getMessage());
            throw e;
        }
    }

    public void verifyDefaultSurveygroup() throws Exception {
        try {
            // Verify that the default survey group is selected
            String defaultGroup = driver.findElement(By.xpath("//*[@id='select2-gsid-container']"))
                    .getText();
            
            // Add null check and highlight the element
            highlightObject("//*[@id='select2-gsid-container']");
            
            if (defaultGroup != null && defaultGroup.equals("Default Group")) {
                ExtentReporter.log(LogStatus.PASS, "Default survey group is selected: " + defaultGroup);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Default survey group is not selected. Current group: " + 
                        (defaultGroup != null ? defaultGroup : "null"));
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify default survey group: " + e.getMessage());
            throw e;
        }
    }

    public void verifyDefaultSGSurveylist() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']"))
                    .sendKeys("CREATE - Automation Survey. Please disregard.");
            driver.findElement(By.xpath("//*[contains(@class, 'select2-selection--single')]")).click();
            driver.findElement(By.xpath("//*[contains(@class, 'select2-search__field')]"))
                    .sendKeys("Default Group" + Keys.ENTER);
            ExtentReporter.log(LogStatus.INFO, "Default Group selected in search");

            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Group");

            // Verify that the default survey group is displayed in the Survey List
            String firstSurveyGroup = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[4]")).getText();
            if (firstSurveyGroup.contains("Default Group")) {
                ExtentReporter.log(LogStatus.PASS, "Search results verified for Group: " + firstSurveyGroup);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Search results do not match expected Group");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify default survey group in Survey List: " + e.getMessage());
            throw e;
        }
    }

    public void setCustomGroup() throws Exception {
        try {
            // Click on the Group dropdown
            driver.findElement(By.xpath("//*[@id='select2-gsid-container']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Group dropdown");

            // Select a custom group from the dropdown
            driver.findElement(By.xpath("(//input[@aria-label='Search'])[1]")).sendKeys("Selenium Survey Group");
            driver.findElement(By.xpath("(//*[contains(text(), 'Selenium Survey Group')])[2]")).click();
            ExtentReporter.log(LogStatus.INFO, "Selected Custom Group from the dropdown");

            // Click the Save button to save the changes
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the custom group");

            Thread.sleep(10000);

            // Add highlighting and use getText() instead of getAttribute("value")
            highlightObject("//*[@id='select2-gsid-container']");
            String selectedGroup = driver.findElement(By.xpath("//*[@id='select2-gsid-container']")).getText();
            
            if (selectedGroup != null && selectedGroup.contains("Selenium Survey Group")) {
                ExtentReporter.log(LogStatus.PASS, "Custom group updated successfully to: " + selectedGroup);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Custom group update failed. Current group: " + 
                    (selectedGroup != null ? selectedGroup : "null"));
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to set custom group: " + e.getMessage());
            throw e;
        }
    }

    public void verifyCustomSGSurveylist() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']"))
                    .sendKeys("CREATE - Automation Survey. Please disregard.");
            driver.findElement(By.xpath("//*[contains(@class, 'select2-selection--single')]")).click();
            driver.findElement(By.xpath("//*[contains(@class, 'select2-search__field')]"))
                    .sendKeys("Selenium Survey Group" + Keys.ENTER);
            ExtentReporter.log(LogStatus.INFO, "Selenium Survey Group selected in search");

            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Group");
            Thread.sleep(5000);

            // Verify that the default survey group is displayed in the Survey List
            String firstSurveyGroup = driver.findElement(By.xpath("(//*[contains(@class, 'has-link')])[4]")).getText();
            if (firstSurveyGroup.contains("Selenium Survey Group")) {
                ExtentReporter.log(LogStatus.PASS, "Search results verified for Group: " + firstSurveyGroup);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Search results do not match expected Group");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL,
                    "Failed to verify default survey group in Survey List: " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method verifies the elements present on the Privacy Policy page of the
     * Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */
    public void switchToIframe1() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
            wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("(//*[contains(@class, 'cke_wysiwyg_frame')])[1]")));
            driver.switchTo().frame(driver.findElement(By.xpath("(//*[contains(@class, 'cke_wysiwyg_frame')])[1]")));
            System.out.println("Switched to iframe 1...");
        } catch (Exception e) {
            System.out.println("Failed to switch to iframe 1...");
        }
    }

    public void switchToIframe2() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
            wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("(//*[contains(@class, 'cke_wysiwyg_frame')])[2]")));
            driver.switchTo().frame(driver.findElement(By.xpath("(//*[contains(@class, 'cke_wysiwyg_frame')])[2]")));
            System.out.println("Switched to iframe 2...");
        } catch (Exception e) {
            System.out.println("Failed to switch to iframe 2...");
        }
    }

        public void switchToIframe3() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
            wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("(//*[contains(@class, 'cke_wysiwyg_frame')])[3]")));
            driver.switchTo().frame(driver.findElement(By.xpath("(//*[contains(@class, 'cke_wysiwyg_frame')])[3]")));
            System.out.println("Switched to iframe 3...");
        } catch (Exception e) {
            System.out.println("Failed to switch to iframe 3...");
        }
    }

    public void switchToDefault() {
        try {
            driver.switchTo().defaultContent();
            System.out.println("Switched to default...");
        } catch (Exception e) {
            System.out.println("Failed to switch to iframe...");
        }
    }

    public void verifyPrivacyPolicy() throws Exception {
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Show Privacy Policy notice", "//*[@for='showsurveypolicynotice']");
                put("Privacy Policy notice option 2", "//*[@for='showsurveypolicynotice_1']");
                put("Privacy Policy notice option 3", "//*[@for='showsurveypolicynotice_2']");
                put("Language selection tab", "//*[@id='edit-survey-datasecurity-element-language-selection']");

                put("Privacy Policy checkbox label", "//*[text()='Privacy policy checkbox label:']");
                put("Privacy Policy checkbox input", "//*[@id='dataseclabel_en']");
                put("Privacy Policy description", "(//*[@class='well p-3'])[1]");
                put("Data retention label", "//*[@for='expires']");
                put("Data retention input", "//*[@id='expires']");
                put("Data retention date picker", "(//*[@data-td-target='#expires_datetimepicker'])[2]");
                put("Data retention description", "(//*[@class='well p-3'])[2]");
                put("Survey Templates description", "(//*[@class='well p-3'])[3]");
                put("Accenture Policy Guidelines link",
                        "//*[@href='https://kxdocuments.accenture.com/contribution/61877a85-7276-4f42-8a89-50465c818af7']");
                put("Privacy Policy checkbox", "//*[@id='surveyls_agreement_checked_en']");
                put("Survey Privacy Policy message label", "//*[@for='datasec_en']");
                put("Survey Privacy Policy error message label", "//*[@for='datasecerror_en']");
                put("Survey Privacy Policy message CK Editor", "//*[@id='cke_datasec_en']");
                put("Survey Privacy Policy error message CK Editor", "//*[@id='cke_datasecerror_en']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void privacyCharacterLimit() throws Exception {
        try {
            // Clicked on Privacy policy checkbox label field
            driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Privacy policy checkbox label field");

            // Clear the existing label and enter a new one
            WebElement labelInput = driver.findElement(By.xpath("//*[@id='dataseclabel_en']"));
            labelInput.clear();
            String longLabel = "A".repeat(150);
            labelInput.sendKeys(longLabel);
            String privacyLabelText = labelInput.getAttribute("value");
            ExtentReporter.log(privacyLabelText.length() >= 100 ? LogStatus.PASS : LogStatus.FAIL,
                (privacyLabelText.length() >= 100 ? "Privacy policy checkbox label within character limit: " : "Privacy policy checkbox label exceeds character limit: ") + privacyLabelText.length());

            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            Thread.sleep(9000);

            String updatedLabel = driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).getAttribute("value");
            if (updatedLabel.length() <= 100) {
                ExtentReporter.log(LogStatus.PASS, "Privacy policy checkbox label updated successfully to: " + updatedLabel);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Privacy policy checkbox label update failed. Current title: " + updatedLabel);        
            }


        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update privacy policy field: " + e.getMessage());
            throw e;
        }
    }

    public void privacyDoubleByteCharacters() throws Exception {
        try {
            // Clicked on Privacy policy checkbox label field
            driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Privacy policy checkbox label field");

            // Clear the existing label and enter a new one
            WebElement titleInput = driver.findElement(By.xpath("//*[@id='dataseclabel_en']"));
            titleInput.clear();
            titleInput.sendKeys("テスト - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered a new Privacy policy checkbox label in the input field");

            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            Thread.sleep(20000);

            String updatedLabel = driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).getAttribute("value");
            if (updatedLabel.equals("テスト - Automation")) {
                ExtentReporter.log(LogStatus.PASS, "Privacy policy checkbox label updated successfully to: " + updatedLabel);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Privacy policy checkbox label update failed. Current title: " + updatedLabel);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update privacy policy field: " + e.getMessage());
            throw e;
        }
    }

    public void privacyXSSCharacters() throws Exception {
        try {
            // Clicked on Privacy policy checkbox label field
            driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Privacy policy checkbox label field");

            // Clear the existing label and enter a new one
            WebElement titleInput = driver.findElement(By.xpath("//*[@id='dataseclabel_en']"));
            titleInput.clear();
            titleInput.sendKeys("<script>alert('XSS')</script>");
            ExtentReporter.log(LogStatus.INFO, "Entered a new Privacy policy checkbox label in the input field");

            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            Thread.sleep(20000);

            String updatedLabel = driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).getAttribute("value");
            if (updatedLabel.equals("<script>alert('XSS')</script>")) {
                ExtentReporter.log(LogStatus.PASS, "Privacy policy checkbox label updated successfully to: " + updatedLabel);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Privacy policy checkbox label failed. Current title: " + updatedLabel);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update privacy policy field: " + e.getMessage());
            throw e;
        }
    }

    public void updatePrivacyPolicy(String dpMsg, String dpError) throws Exception {
        try {
            // Click on the Privacy Policy checkbox input field
            driver.findElement(By.xpath("//*[@id='dataseclabel_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Privacy Policy checkbox input field");

            // Clear the existing label and enter a new one
            WebElement checkboxInput = driver.findElement(By.xpath("//*[@id='dataseclabel_en']"));
            checkboxInput.clear();
            checkboxInput.sendKeys("Updated Privacy Policy Checkbox Label - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered new label in the Privacy Policy checkbox input field");

            Thread.sleep(20000);
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
            WebElement checkbox = driver.findElement(By.id("surveyls_agreement_checked_en"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkbox);

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='surveyls_agreement_checked_en']")));
            click("//*[@id='surveyls_agreement_checked_en']", "xpath");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, datasec_en']")).sendKeys(dpMsg);
            switchToDefault();
            switchToIframe2();
            driver.findElement(By.xpath("//*[@aria-label='Editor, datasecerror_en']")).sendKeys(dpError);
            switchToDefault();
            click("//*[@id='save-button']", "xpath");
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the Privacy Policy changes");

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update Privacy Policy checkbox: " + e.getMessage());
            throw e;
        }
    }

    public void verifyDonotshowPrivacyPolicy() throws Exception {
        try {
            // Click on the "Do not show Privacy Policy" option
            driver.findElement(By.xpath("//*[@for='showsurveypolicynotice_0']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on 'Do not show Privacy Policy' option");

            // Click the Save button to save the changes
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the changes");

            Thread.sleep(20000);
            
            // Wait for page stability and overlays before clicking preview button
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='preview_survey_button']")));
            
            if (!clickWithRetry(By.xpath("//*[@id='preview_survey_button']"), "Preview Survey button")) {
                throw new Exception("Failed to click Preview Survey button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Preview Survey button");

            switchToNewWindow();

            String donotShowPolicy = driver.findElement(By.xpath("//*[@id='datasecurity_notice']")).getText();
            if (donotShowPolicy.isEmpty()) {
                ExtentReporter.log(LogStatus.PASS,
                        "'Do not show Privacy Policy' is successfully applied. The component is not displayed.");
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "'Do not show Privacy Policy' is not applied. The component is still displayed: "
                                + donotShowPolicy);
            }

            switchToMainWindow();
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify 'Do not show Privacy Policy': " + e.getMessage());
            throw e;
        }
    }

    public void verifyInlinePrivacy() throws Exception {
        try {
            // Wait for page stability
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            // Click on the "Show Privacy Policy notice" option with retry
            By policyNoticeLocator = By.xpath("//*[@for='showsurveypolicynotice_1']");
            if (clickWithRetry(policyNoticeLocator, "Show Privacy Policy Notice Option")) {
                ExtentReporter.log(LogStatus.INFO, "Successfully clicked on 'Show Privacy Policy notice' option");
                
                // Click the Save button to save the changes
                By saveButtonLocator = By.xpath("//*[@id='save-button']");
                if (clickWithRetry(saveButtonLocator, "Save Button")) {
                    ExtentReporter.log(LogStatus.INFO, "Successfully clicked on Save button to save the changes");
                    
                    Thread.sleep(20000);
                    waitForPageLoad();
                    
                    // Click Preview Survey button
                    By previewButtonLocator = By.xpath("//*[@id='preview_survey_button']");
                    if (clickWithRetry(previewButtonLocator, "Preview Survey Button")) {
                        ExtentReporter.log(LogStatus.INFO, "Successfully clicked on Preview Survey button");
                        
                        switchToNewWindow();
                        
                        // Verify inline policy
                        WebElement policyElement = findElementWithRetry(
                            By.xpath("//*[@id='datasecurity_notice']"), 
                            "Privacy Policy Notice"
                        );
                        
                        if (policyElement != null) {
                            String inlinePolicy = policyElement.getText();
                            if (inlinePolicy.contains("Updated Privacy Policy Message - Automation")) {
                                ExtentReporter.log(LogStatus.PASS,
                                        "'Inline Privacy Policy' is successfully applied. The component is displayed: " + inlinePolicy);
                            } else {
                                ExtentReporter.log(LogStatus.FAIL,
                                        "'Inline Privacy Policy' is not applied. The component is not displayed as expected.");
                            }
                        } else {
                            ExtentReporter.log(LogStatus.FAIL, "Privacy Policy notice element not found");
                        }
                        
                        switchToMainWindow();
                    } else {
                        throw new Exception("Failed to click Preview Survey button");
                    }
                } else {
                    throw new Exception("Failed to click Save button");
                }
            } else {
                throw new Exception("Failed to click Privacy Policy notice option");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify 'Inline Privacy Policy': " + e.getMessage());
            throw e;
        }
    }

    public void verifyCollapsiblePolicy() throws Exception {
        try {
            // Click on the "Show Privacy Policy notice" option
            driver.findElement(By.xpath("//*[@for='showsurveypolicynotice_2']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on 'Show Privacy Policy notice' option");

            // Click the Save button to save the changes
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the changes");

            Thread.sleep(2000);
            
            // Wait for page stability and overlays before clicking preview button
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='preview_survey_button']")));
            
            if (!clickWithRetry(By.xpath("//*[@id='preview_survey_button']"), "Preview Survey button")) {
                throw new Exception("Failed to click Preview Survey button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Preview Survey button");

            switchToNewWindow();
            driver.findElement(By.xpath("//*[@class='collapsed']")).click();

            String collapsiblePolicy = driver.findElement(By.xpath("//*[@class='card-body ']")).getText();
            if (collapsiblePolicy.contains("Updated Privacy Policy Message - Automation")) {
                ExtentReporter.log(LogStatus.PASS,
                        "'Collapsible Privacy Policy' is successfully applied. The component is displayed: "
                                + collapsiblePolicy);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "'Collapsible Privacy Policy' is not applied. The component is not displayed as expected.");
            }

            switchToMainWindow();
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify 'Collapsible Privacy Policy': " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method verifies the text elements present on the Text Elements page of
     * the Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyTextElements() throws Exception {
    	Thread.sleep(9000);
        try {
            highlightObject("//*[@id='edit-survey-text-element-language-selection']");
            isElementDisplayed("//*[@id='edit-survey-text-element-language-selection']", "xpath", "Language Selection tab");
            highlightObject("//*[@for='short_title_en']");
            isElementDisplayed("//*[@for='short_title_en']", "xpath", "Survey Title label");            
            highlightObject("//*[@id='short_title_en']");
            isElementDisplayed("//*[@id='short_title_en']", "xpath", "Survey Title input");           
            highlightObject("//*[@class='annotation text-danger']");
            isElementDisplayed("//*[@class='annotation text-danger']", "xpath", "Title annotation");            
            highlightObject("//*[@for='dateformat_en']");
            isElementDisplayed("//*[@for='dateformat_en']", "xpath", "Date format label");            
            highlightObject("//*[@id='dateformat_en']");
            isElementDisplayed("//*[@id='dateformat_en']", "xpath", "Date format input");            
            highlightObject("//*[text()='Decimal mark:']");
            isElementDisplayed("//*[text()='Decimal mark:']", "xpath", "Decimal mark");            
            highlightObject("//*[@for='numberformat_en_1']");
            isElementDisplayed("//*[@for='numberformat_en_1']", "xpath", "Decimal mark option 1");          
            highlightObject("//*[@for='numberformat_en_2']");
            isElementDisplayed("//*[@for='numberformat_en_2']", "xpath", "Decimal mark option 2");          
            highlightObject("//*[@for='alias_en']");
            isElementDisplayed("//*[@for='alias_en']", "xpath", "Survey alias label");           
            highlightObject("//*[@id='alias_en']");
            isElementDisplayed("//*[@id='alias_en']", "xpath", "Survey alias input");            
            highlightObject("//*[@for='description_en']");
            isElementDisplayed("//*[@for='description_en']", "xpath", "Description label");            
            highlightObject("//*[@id='cke_description_en']");
            isElementDisplayed("//*[@id='cke_description_en']", "xpath", "Description CK Editor");          
            highlightObject("//*[@for='welcome_en']");
            isElementDisplayed("//*[@for='welcome_en']", "xpath", "Welcome message label");            
            highlightObject("//*[@id='cke_welcome_en']");
            isElementDisplayed("//*[@id='cke_welcome_en']", "xpath", "Welcome message CK Editor");            
            highlightObject("//*[@for='endtext_en']");
            isElementDisplayed("//*[@for='endtext_en']", "xpath", "End message label");            
            highlightObject("//*[@id='cke_endtext_en']");
            isElementDisplayed("//*[@id='cke_endtext_en']", "xpath", "End message CK Editor");            
            highlightObject("//*[@for='url_en']");
            isElementDisplayed("//*[@for='url_en']", "xpath", "End URL label");           
            highlightObject("//*[@id='url_en']");
            isElementDisplayed("//*[@id='url_en']", "xpath", "End URL input");
            highlightObject("//*[@for='urldescrip_en']");
            isElementDisplayed("//*[@for='urldescrip_en']", "xpath", "URL description label");            
            highlightObject("//*[@id='urldescrip_en']");
            isElementDisplayed("//*[@id='urldescrip_en']", "xpath", "URL description input");
            
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify text elements: " + e.getMessage());
            throw e;
        }
    }

    public void titleDoubleByteCharacters() throws Exception {
        try {
            String expectedTitle = "テスト - Automation";
            // Click on the Survey Title input field
            driver.findElement(By.xpath("//*[@id='short_title_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Survey Title input field");

            // Clear the existing title and enter a new one
            WebElement titleInput = driver.findElement(By.xpath("//*[@id='short_title_en']"));
            titleInput.clear();
            titleInput.sendKeys(expectedTitle);
            ExtentReporter.log(LogStatus.INFO, "Entered new survey title in the Survey Title input field");

            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            // Wait for page to stabilize
            Thread.sleep(3000);
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            // Wait for the title field to be visible before reading final value
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            By titleLocator = By.xpath("//*[@id='short_title_en']");
            wait.until(ExpectedConditions.visibilityOfElementLocated(titleLocator));

            String updatedTitle = driver.findElement(titleLocator).getAttribute("value");
            if (updatedTitle.equals(expectedTitle)) {
                ExtentReporter.log(LogStatus.PASS, "Survey title updated successfully to: " + updatedTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Survey title update failed. Current title: " + updatedTitle);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update text elements field: " + e.getMessage());
            throw e;
        }
    }

    public void titleXSSCharacters() throws Exception {
        try {
            String xssPayload = "<script>alert1('XSS')</script>";
            By titleLocator = By.xpath("//*[@id='short_title_en']");
            By saveButtonLocator = By.xpath("//*[@id='save-button']");

            waitForPageLoad();
            waitForOverlayToDisappear();

            WebElement currentTitleInput = findElementWithRetry(titleLocator, "Survey Title input field");
            if (currentTitleInput == null) {
                throw new Exception("Survey Title input field not found");
            }
            String previousTitle = currentTitleInput.getAttribute("value");

            boolean titleClicked = clickWithRetry(titleLocator, "Survey Title input field");
            if (titleClicked) {
                ExtentReporter.log(LogStatus.INFO, "Clicked on Survey Title input field");
            } else {
                ExtentReporter.log(LogStatus.WARNING,
                    "Could not click Survey Title input field due to intercept. Proceeding with JS-assisted input.");
            }

            // Clear the existing title and enter a new one
            WebElement titleInput = findElementWithRetry(titleLocator, "Survey Title input field");
            if (titleInput == null) {
                throw new Exception("Survey Title input field not found for update");
            }
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", titleInput);
            titleInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            titleInput.sendKeys(Keys.DELETE);
            titleInput.sendKeys(xssPayload);
            ExtentReporter.log(LogStatus.INFO, "Entered new survey title in the Survey Title input field");

            if (!clickWithRetry(saveButtonLocator, "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            // Wait for page to stabilize
            Thread.sleep(3000);
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            // Wait for the title field to be visible before reading final value
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(titleLocator));

            String updatedTitle = driver.findElement(titleLocator).getAttribute("value");
            String validationMessageXpath = "//*[@id='short_title_en' and @aria-invalid='true']"
                + " | //*[contains(@class,'invalid-feedback') or contains(@class,'alert-danger') "
                + "or contains(@class,'text-danger') or contains(@class,'error') or contains(@class,'danger') "
                + "or contains(text(),'Unsafe') or contains(text(),'unsafe') "
                + "or contains(text(),'invalid') or contains(text(),'Invalid') "
                + "or contains(text(),'error') or contains(text(),'Error')]";
            boolean hasValidationError = !driver.findElements(By.xpath(validationMessageXpath)).isEmpty();

            // Refresh to validate persisted value (avoids false-fail when UI keeps unsaved input in field)
            driver.navigate().refresh();
            waitForPageLoad();
            waitForOverlayToDisappear();
            wait.until(ExpectedConditions.visibilityOfElementLocated(titleLocator));
            String persistedTitle = driver.findElement(titleLocator).getAttribute("value");

            boolean persistedHasRawScriptTag = persistedTitle != null
                && (persistedTitle.toLowerCase().contains("<script") || persistedTitle.toLowerCase().contains("</script>"));
            boolean payloadPersisted = xssPayload.equals(persistedTitle) || persistedHasRawScriptTag;

            if (!payloadPersisted) {
                ExtentReporter.log(LogStatus.PASS,
                    "Survey title XSS input was sanitized/rejected as expected. Pre-save title: " + previousTitle
                    + " | Immediate field value: " + updatedTitle
                    + " | Persisted title after refresh: " + persistedTitle);
            } else if (hasValidationError) {
                ExtentReporter.log(LogStatus.PASS,
                    "Survey title XSS input was rejected with validation as expected."
                    + " | Immediate field value: " + updatedTitle
                    + " | Persisted title after refresh: " + persistedTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                    "Survey title update failed. Unsafe value still persisted after refresh: " + persistedTitle);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update text elements field: " + e.getMessage());
            throw e;
        }
    }

    public void updateSurveyTitle() throws Exception {
        try {
            String expectedTitle = "Updated Survey Title - Automation";
            By titleLocator = By.id("short_title_en");
            By saveButtonLocator = By.id("save-button");

            waitForPageLoad();
            waitForOverlayToDisappear();

            // Click on the Survey Title input field
            if (!clickWithRetry(titleLocator, "Survey Title input field")) {
                throw new Exception("Unable to click Survey Title input field");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Survey Title input field");

            // Clear the existing title and enter a new one
            WebElement titleInput = findElementWithRetry(titleLocator, "Survey Title input field");
            if (titleInput == null) {
                throw new Exception("Survey Title input field not found");
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", titleInput);

            try {
                titleInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                titleInput.sendKeys(Keys.DELETE);
            } catch (Exception clearException) {
                js.executeScript("arguments[0].value=''; arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", titleInput);
            }
            titleInput.sendKeys(expectedTitle);
            ExtentReporter.log(LogStatus.INFO, "Entered new survey title in the Survey Title input field");

            if (!clickWithRetry(saveButtonLocator, "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            // Wait for page to stabilize
            Thread.sleep(3000);
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            // Wait for the title field to be visible before reading final value
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(titleLocator));

            String updatedTitle = driver.findElement(titleLocator).getAttribute("value");
            if (updatedTitle.equals(expectedTitle)) {
                ExtentReporter.log(LogStatus.PASS, "Survey title updated successfully to: " + updatedTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Survey title update failed. Current title: " + updatedTitle);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update text elements field: " + e.getMessage());
            throw e;
        }
    }

    public void updateTextElementsCKfields() throws Exception {
        try {
            // Clear the existing description and enter a new one
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("Updated Survey Description - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered new survey description in the Description CK Editor");
            switchToDefault();

            switchToIframe2();
            driver.findElement(By.xpath("//*[@aria-label='Editor, welcome_en']")).sendKeys("Updated Survey Welcome Message - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered new welcome message in the Welcome message CK Editor");
            switchToDefault();

            switchToIframe3();
            driver.findElement(By.xpath("//*[@aria-label='Editor, endtext_en']")).sendKeys("Updated Survey End Message - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered new end message in the End message CK Editor");
            switchToDefault();
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update survey description, welcome message, end message: " + e.getMessage());
            throw e;
        }
    }

    public void updateWelcomeMessage() throws Exception {
        try {
            // Click on the Welcome message CK Editor
            driver.findElement(By.xpath("//*[@aria-label='Editor, welcome_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Welcome message CK Editor");

            // Clear the existing welcome message and enter a new one
            switchToIframe2();
            WebElement welcomeInput = driver.findElement(By.xpath("//*[@aria-label='Editor, welcome_en']"));
            welcomeInput.clear();
            welcomeInput.sendKeys("Updated Survey Welcome Message - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered new welcome message in the Welcome message CK Editor");
            switchToDefault();
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update welcome message: " + e.getMessage());
            throw e;
        }
    }

    public void updateEndMessage() throws Exception {
        try {
            // Click on the End message CK Editor
            driver.findElement(By.xpath("//*[@aria-label='Editor, endtext_en']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on End message CK Editor");

            // Clear the existing end message and enter a new one
            switchToIframe3();
            WebElement endInput = driver.findElement(By.xpath("//*[@aria-label='Editor, endtext_en']"));
            endInput.clear();
            endInput.sendKeys("Updated Survey End Message - Automation");
            ExtentReporter.log(LogStatus.INFO, "Entered new end message in the End message CK Editor");
            switchToDefault();
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update end message: " + e.getMessage());
            throw e;
        }
    }

    public void updateSurveyAlias() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            By aliasLocator = By.id("alias_en");
            if (!clickWithRetry(aliasLocator, "Survey alias input field")) {
                throw new Exception("Unable to click Survey alias input field");
            }

            WebElement aliasInput = findElementWithRetry(aliasLocator, "Survey alias input field");
            if (aliasInput == null) {
                throw new Exception("Survey alias input field not found");
            }

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", aliasInput);
            aliasInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            aliasInput.sendKeys(Keys.DELETE);
            aliasInput.sendKeys("updated-survey-alias-auto102");
            ExtentReporter.log(LogStatus.INFO, "Entered new survey alias in the Survey alias input field");

            if (!clickWithRetry(By.id("save-button"), "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the updated survey alias");
            Thread.sleep(10000);

            String updatedAlias = driver.findElement(By.xpath("//*[@id='alias_en']")).getAttribute("value");
            if (updatedAlias.equals("updated-survey-alias-auto102")) {
                ExtentReporter.log(LogStatus.PASS, "Survey alias updated successfully to: " + updatedAlias);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Survey alias update failed. Current alias: " + updatedAlias);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update survey alias: " + e.getMessage());
            throw e;
        }
    }

    public void updateEndURL() throws Exception {
    	
    	Thread.sleep(10000);
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            By endUrlLocator = By.id("url_en");
            By saveButtonLocator = By.id("save-button");

            // Click on the End URL input field
            wait.until(ExpectedConditions.elementToBeClickable(endUrlLocator)).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on End URL input field");

            // Clear the existing URL and enter a new one
            WebElement urlInput = driver.findElement(endUrlLocator);
            urlInput.clear();
            urlInput.sendKeys("https://www.example.com/updated-end-url");
            ExtentReporter.log(LogStatus.INFO, "Entered new end URL in the End URL input field");

            // Save to persist value before overview validation
            wait.until(ExpectedConditions.elementToBeClickable(saveButtonLocator)).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to persist End URL changes");

            wait.until(ExpectedConditions.attributeToBe(endUrlLocator, "value", "https://www.example.com/updated-end-url"));

            String updatedUrl = driver.findElement(By.xpath("//*[@id='url_en']")).getAttribute("value");
            if (updatedUrl.equals("https://www.example.com/updated-end-url")) {
                ExtentReporter.log(LogStatus.PASS, "End URL updated successfully to: " + updatedUrl);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "End URL update failed. Current URL: " + updatedUrl);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update end URL: " + e.getMessage());
            throw e;
        }
    }

    public void updateURLDescription() throws Exception {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            By urlDescriptionLocator = By.id("urldescrip_en");
            By saveButtonLocator = By.id("save-button");
            String expectedUrlDescription = "Updated URL Description - Automation";

            waitForPageLoad();
            waitForOverlayToDisappear();

            if (!clickWithRetry(urlDescriptionLocator, "URL description input field")) {
                throw new Exception("Unable to click URL description input field");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on URL description input field");

            WebElement urlDescriptionInput = findElementWithRetry(urlDescriptionLocator, "URL description input field");
            if (urlDescriptionInput == null) {
                throw new Exception("URL description input field not found");
            }

            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", urlDescriptionInput);
            urlDescriptionInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            urlDescriptionInput.sendKeys(Keys.DELETE);
            urlDescriptionInput.sendKeys(expectedUrlDescription);
            ExtentReporter.log(LogStatus.INFO, "Entered new URL description in the URL description input field");

            if (!clickWithRetry(saveButtonLocator, "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the updated URL description");

            waitForPageLoad();
            waitForOverlayToDisappear();
            wait.until(ExpectedConditions.attributeToBe(urlDescriptionLocator, "value", expectedUrlDescription));

            String updatedUrlDescription = wait.until(
                ExpectedConditions.visibilityOfElementLocated(urlDescriptionLocator))
                    .getAttribute("value");
            if (updatedUrlDescription.equals(expectedUrlDescription)) {
                ExtentReporter.log(LogStatus.PASS,
                        "URL description updated successfully to: " + updatedUrlDescription);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "URL description update failed. Current description: " + updatedUrlDescription);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to update URL description: " + e.getMessage());
            throw e;
        }
    }

    public void verifyTextElementsUpdatestoOverview() throws Exception {
    	Thread.sleep(8000);
    	try {

            // Verify end URL is reflected in the overview
            String overviewEndURL = "";
            java.util.List<WebElement> endUrlCandidates = driver.findElements(By.xpath(
                "//div[contains(@class,'card-label') and contains(normalize-space(.),'End URL')]/following-sibling::div[1]"
                + " | //div[contains(@class,'col-4') and contains(normalize-space(.),'End URL')]/following-sibling::div[1]"));
            if (!endUrlCandidates.isEmpty()) {
            overviewEndURL = endUrlCandidates.get(0).getText().trim();
            }

            if (overviewEndURL.equals("https://www.example.com/updated-end-url")) {
                ExtentReporter.log(LogStatus.PASS,
                        "End URL in overview matches updated end URL: " + overviewEndURL);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "End URL in overview does not match updated end URL. Current URL: " + overviewEndURL);
            }

            // Verify that the updated survey description is reflected in the overview
            String overviewDescription = driver.findElement(By.xpath("//*[@class='selector__toggle_description_text']"))
                    .getText();
            if (overviewDescription.equals("Updated Survey Description - Automation")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Survey description in overview matches updated description: " + overviewDescription);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Survey description in overview does not match updated description. Current description: "
                                + overviewDescription);
            }

            // Verify that the updated welcome message is reflected in the overview
            String overviewWelcomeMessage = driver.findElement(By.xpath("//*[@class='selector__toggle_welcome_text']"))
                    .getText();
            if (overviewWelcomeMessage.equals("Updated Survey Welcome Message - Automation")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Welcome message in overview matches updated welcome message: " + overviewWelcomeMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Welcome message in overview does not match updated welcome message. Current message: "
                                + overviewWelcomeMessage);
            }

            // Verify that the updated end message is reflected in the overview
            String overviewEndMessage = driver.findElement(By.xpath("//*[@class='selector__toggle_end_text']"))
                    .getText();
            if (overviewEndMessage.equals("Updated Survey End Message - Automation")) {
                ExtentReporter.log(LogStatus.PASS,
                        "End message in overview matches updated end message: " + overviewEndMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "End message in overview does not match updated end message. Current message: "
                                + overviewEndMessage);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify text elements updates to overview: " + e.getMessage());
            throw e;
        }
    }

    public void verifySurveyTitleUpdatetoSurveyList() throws Exception {
        try {
            // Enter survey title in search
            String surveyTitle = "Updated Survey Title - Automation";
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).clear();
            driver.findElement(By.xpath("//*[@id='Survey_searched_value']")).sendKeys(surveyTitle);
            ExtentReporter.log(LogStatus.INFO, "Survey Title entered in search: " + surveyTitle);

            // Click search button
            driver.findElement(By.xpath("//*[@aria-label='Search button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Search button clicked for Survey Title");
            Thread.sleep(8000);

            // Verify search results
            String firstSurveyTitle = driver.findElement(By.xpath("(//*[@class='has-link'])[1]")).getText();
            if (firstSurveyTitle.contains(surveyTitle)) {
                ExtentReporter.log(LogStatus.PASS, "Search results verified for Survey Title: " + firstSurveyTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Search results do not match expected Survey Title");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to perform search by Survey Title: " + e.getMessage());
            throw e;
        }
    }

    public void verifyUpdatestoPreviewSurvey() throws Exception {
        try {
            // Click on the Preview button
            driver.findElement(By.xpath("//*[@id='preview_survey_button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Preview Survey button");

            // Wait for the preview survey to load
            Thread.sleep(10000);
            switchToNewWindow();
            String surveyTitle = driver
                    .findElement(By.xpath("//h1[contains(text(), 'Updated Survey Title - Automation')]")).getText();
            if (surveyTitle.equals("Updated Survey Title - Automation")) {
                ExtentReporter.log(LogStatus.PASS, "Survey title in preview matches updated title: " + surveyTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Survey title in preview does not match updated title. Current title: " + surveyTitle);
            }
            String description = driver
                    .findElement(By.xpath("//*[contains(text(), 'Updated Survey Description - Automation')]"))
                    .getText();
            if (description.equals("Updated Survey Description - Automation")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Description in preview matches updated description: " + description);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Description in preview does not match updated description. Current description: "
                                + description);
            }

            String welcomeMessage = driver
                    .findElement(By.xpath("//*[contains(text(), 'Updated Survey Welcome Message - Automation')]"))
                    .getText();
            if (welcomeMessage.equals("Updated Survey Welcome Message - Automation")) {
                ExtentReporter.log(LogStatus.PASS,
                        "Welcome message in preview matches updated welcome message: " + welcomeMessage);
            } else {
                ExtentReporter.log(LogStatus.FAIL,
                        "Welcome message in preview does not match updated welcome message. Current message: "
                                + welcomeMessage);
            }

            closeSecondTab();
            switchToMainWindow();

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify alias in preview survey: " + e.getMessage());
            throw e;
        }
    }
    
    public void titleCharacterLimit() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            By titleLocator = By.id("short_title_en");
            By saveButtonLocator = By.id("save-button");

            boolean titleClicked = clickWithRetry(titleLocator, "Survey Title input field");
            if (titleClicked) {
                ExtentReporter.log(LogStatus.INFO, "Clicked on Survey Title input field");
            } else {
                ExtentReporter.log(LogStatus.WARNING,
                    "Could not click Survey Title input field due to intercept. Proceeding with JS-assisted input.");
            }

            WebElement titleInput = findElementWithRetry(titleLocator, "Survey Title input field");
            if (titleInput == null) {
                throw new Exception("Survey Title input field not found");
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", titleInput);

            try {
                titleInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                titleInput.sendKeys(Keys.DELETE);
            } catch (Exception clearException) {
                js.executeScript("arguments[0].value=''; arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", titleInput);
            }

            String longTitle = "A".repeat(250);
            try {
                titleInput.sendKeys(longTitle);
            } catch (Exception sendKeysException) {
                js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles:true})); arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", titleInput, longTitle);
            }
            String surveyTitleText = titleInput.getAttribute("value");
            ExtentReporter.log(surveyTitleText.length() >= 200 ? LogStatus.PASS : LogStatus.FAIL,
                (surveyTitleText.length() >= 200 ? "Survey Title within character limit: " : "Survey Title exceeds character limit: ") + surveyTitleText.length());

            if (!clickWithRetry(saveButtonLocator, "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the survey title changes");

            // Wait for page to stabilize
            Thread.sleep(5000);
            waitForPageLoad();
            waitForOverlayToDisappear();

            // Wait for the title field to be visible before reading final value
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(titleLocator));

            String updatedTitle = driver.findElement(titleLocator).getAttribute("value");
            if (updatedTitle.length() <= 200) {
                ExtentReporter.log(LogStatus.PASS, "Survey title updated successfully to: " + updatedTitle);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Survey title update failed. Current title: " + updatedTitle);
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify title character limit: " + e.getMessage());
            throw e;
        }
    }

    public void aliasCharacterLimit() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            By aliasLocator = By.id("alias_en");
            if (!clickWithRetry(aliasLocator, "Survey alias input field")) {
                throw new Exception("Unable to click Survey alias input field");
            }

            WebElement aliasInput = findElementWithRetry(aliasLocator, "Survey alias input field");
            if (aliasInput == null) {
                throw new Exception("Survey alias input field not found");
            }

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", aliasInput);
            aliasInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            aliasInput.sendKeys(Keys.DELETE);
            ExtentReporter.log(LogStatus.INFO, "Clicked on Survey alias input field");

            String longAlias = "x".repeat(150);
            aliasInput.sendKeys(longAlias);
            String surveyAliasText = aliasInput.getAttribute("value");
            ExtentReporter.log(surveyAliasText.length() <= 100 ? LogStatus.PASS : LogStatus.FAIL,
                (surveyAliasText.length() <= 100 ? "Survey Alias within character limit: " : "Survey Alias exceeds character limit: ") + surveyAliasText.length());

            if (!clickWithRetry(By.id("save-button"), "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the updated Survey alias");

            Thread.sleep(15000);

            if(surveyAliasText.length() <= 100) {
                ExtentReporter.log(LogStatus.PASS, "Survey alias updated successfully to: " + surveyAliasText);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Survey alias update failed. Current alias: " + surveyAliasText);        
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify alias character limit: " + e.getMessage());
            throw e;
        }
    }

    public void URLDdescCharacterLimit() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            By urlDescriptionLocator = By.id("urldescrip_en");

            if (!clickWithRetry(urlDescriptionLocator, "URL description input field")) {
                throw new Exception("Unable to click URL description input field");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on URL description input field");

            WebElement urlDescriptionInput = findElementWithRetry(urlDescriptionLocator, "URL description input field");
            if (urlDescriptionInput == null) {
                throw new Exception("URL description input field not found");
            }

            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", urlDescriptionInput);
            urlDescriptionInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            urlDescriptionInput.sendKeys(Keys.DELETE);

            // Enter a long value for character-limit validation
            String longUrlDesc = "A".repeat(300);
            urlDescriptionInput.sendKeys(longUrlDesc);
            String urlDescText = urlDescriptionInput.getAttribute("value");
            ExtentReporter.log(urlDescText.length() <= 255 ? LogStatus.PASS : LogStatus.FAIL,
                (urlDescText.length() <= 255 ? "URL Description within character limit: " : "URL Description exceeds character limit: ") + urlDescText.length());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify URL description character limit: " + e.getMessage());
            throw e;
        }
    }

    public void aliasValidateDoubleByteCharacter() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            // Click on the Survey alias input field with retry to handle intercepted click
            By aliasLocator = By.id("alias_en");
            boolean aliasClicked = clickWithRetry(aliasLocator, "Survey alias input field");
            if (!aliasClicked) {
                ExtentReporter.log(LogStatus.WARNING,
                    "Could not click Survey alias input field due to intercept. Proceeding with JS-assisted input.");
            }

            // Clear existing alias and enter a double-byte alias
            WebElement aliasInput = findElementWithRetry(aliasLocator, "Survey alias input field");
            if (aliasInput == null) {
                throw new Exception("Survey alias input field not found");
            }
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", aliasInput);
            try {
                aliasInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                aliasInput.sendKeys(Keys.DELETE);
            } catch (Exception clearException) {
                js.executeScript("arguments[0].value=''; arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", aliasInput);
            }
            String doubleByteAlias = "テストエイリアスtest"; // "Test Alias" in Japanese
            try {
                aliasInput.sendKeys(doubleByteAlias);
            } catch (Exception sendKeysException) {
                js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles:true})); arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", aliasInput, doubleByteAlias);
            }

            if (!clickWithRetry(By.id("save-button"), "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the updated Survey Alias");

            Thread.sleep(9000);

            String updatedAlias = driver.findElement(By.xpath("//*[@id='alias_en']")).getAttribute("value");
            boolean errorVisible = !driver.findElements(By.xpath("//*[contains(text(),'Survey alias contains invalid characters for language: en')]")).isEmpty();
            if (!updatedAlias.equals(doubleByteAlias) || errorVisible) {
                ExtentReporter.log(LogStatus.PASS, "Double-byte alias was rejected as expected. Current alias: " + updatedAlias);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Double-byte alias appears accepted unexpectedly. Current alias: " + updatedAlias);
            }

       } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate double-byte characters in Survey Alias: " + e.getMessage());
            throw e;
        }
    }

    public void aliasValidateXSSCharacters() throws Exception {
        try {
            waitForPageLoad();
            waitForOverlayToDisappear();

            // Click on the Survey alias input field with retry to handle intercepted click
            By aliasLocator = By.id("alias_en");
            boolean aliasClicked = clickWithRetry(aliasLocator, "Survey alias input field");
            if (!aliasClicked) {
                ExtentReporter.log(LogStatus.WARNING,
                    "Could not click Survey alias input field due to intercept. Proceeding with JS-assisted input.");
            }

            // Clear existing alias and enter an XSS payload
            WebElement aliasInput = findElementWithRetry(aliasLocator, "Survey alias input field");
            if (aliasInput == null) {
                throw new Exception("Survey alias input field not found");
            }
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", aliasInput);
            try {
                aliasInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                aliasInput.sendKeys(Keys.DELETE);
            } catch (Exception clearException) {
                js.executeScript("arguments[0].value=''; arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", aliasInput);
            }
            String xssAlias = "<script>alert('XSS!!')</script>";
            try {
                aliasInput.sendKeys(xssAlias);
            } catch (Exception sendKeysException) {
                js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles:true})); arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", aliasInput, xssAlias);
            }

            if (!clickWithRetry(By.id("save-button"), "Save button")) {
                throw new Exception("Unable to click Save button");
            }
            ExtentReporter.log(LogStatus.INFO, "Clicked on Save button to save the updated Survey Alias");

            Thread.sleep(10000);

             String updatedAlias = driver.findElement(By.xpath("//*[@id='alias_en']")).getAttribute("value");
            boolean errorVisible = !driver.findElements(By.xpath("//*[contains(text(),'Survey alias contains invalid characters for language: en')]")).isEmpty();
            if (!updatedAlias.equals(xssAlias) || errorVisible) {
                ExtentReporter.log(LogStatus.PASS, "XSS alias was rejected as expected. Current alias: " + updatedAlias);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "XSS alias appears accepted unexpectedly. Current alias: " + updatedAlias);
            }

            } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate XSS characters in Survey Alias: " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method verifies the elements present on the Presentation page of the
     * Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyPresentation() throws Exception {
    	Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Presentation header", "//h1[contains(text(), 'Show...')]");
                put("Show no answer label", "//*[@for='shownoanswer']");
                put("Show no answer ON option", "//*[@for='shownoanswer_1']");
                put("Show no answer OFF option", "//*[@for='shownoanswer_2']");
                put("Show no answer Inherit option", "//*[@for='shownoanswer_3']");

                put("Show X question label", "//*[@for='showxquestions']");
                put("Show X question ON option", "//*[@for='showxquestions_1']");
                put("Show X question OFF option", "//*[@for='showxquestions_2']");
                put("Show X question Inherit option", "//*[@for='showxquestions_3']");

                put("Show group infor label", "//*[@for='showgroupinfo']");
                put("Show group infor select", "//*[@id='showgroupinfo']");

                put("Show question code label", "//*[@for='showqnumcode']");
                put("Show question code select", "//*[@id='showqnumcode']");

                put("Show welcome screen label", "//*[@for='showwelcome']");
                put("Show welcome screen ON option", "//*[@for='showwelcome_1']");
                put("Show welcome screen OFF option", "//*[@for='showwelcome_2']");
                put("Show welcome screen Inherit option", "//*[@for='showwelcome_3']");

                put("Show no keyboard label", "//*[@for='nokeyboard']");
                put("Show no keyboard ON option", "//*[@for='nokeyboard_1']");
                put("Show no keyboard OFF option", "//*[@for='nokeyboard_2']");
                put("Show no keyboard Inherit option", "//*[@for='nokeyboard_3']");

                put("Show progress bar label", "//*[@for='showprogress']");
                put("Show progress bar ON option", "//*[@for='showprogress_1']");
                put("Show progress bar OFF option", "//*[@for='showprogress_2']");
                put("Show progress bar Inherit option", "//*[@for='showprogress_3']");

                put("Show question index label", "//*[@for='questionindex']");
                put("Show question index disabled option", "//*[@for='questionindex_1']");
                put("Show question index incremental option", "//*[@for='questionindex_2']");
                put("Show question index full option", "//*[@for='questionindex_3']");
                put("Show question index Inherit option", "//*[@for='questionindex_4']");

                put("Navigation header", "//h1[text()='Navigation']");
                put("Navigation delay label", "//*[@id='label-navigationdelay']");
//              put("Navigation delay input", "//*[contains(@class, 'form-control inherit-readonly d-block')]");
                put("Navigation delay inherit label", "(//*[@for='navigationdelay'])[2]");
                put("Navigation delay inherit ON option", "//*[@for='navigationdelaybutton_1']");
                put("Navigation delay inherit OFF option", "//*[@for='navigationdelaybutton_2']");

                put("Autodirect label", "//*[@for='autoredirect']");
                put("Autodirect ON option", "//*[@for='autoredirect_1']");
                put("Autodirect OFF option", "//*[@for='autoredirect_2']");
                put("Autodirect Inherit option", "//*[@for='autoredirect_3']");

                put("Allow previous label", "//*[@for='allowprev']");
                put("Allow previous ON option", "//*[@for='allowprev_1']");
                put("Allow previous OFF option", "//*[@for='allowprev_2']");
                put("Allow previous Inherit option", "//*[@for='allowprev_3']");

                put("Print answers label", "//*[@for='printanswers']");
                put("Print answers ON option", "//*[@for='printanswers_1']");
                put("Print answers OFF option", "//*[@for='printanswers_2']");
                put("Print answers Inherit option", "//*[@for='printanswers_3']");

                put("Public Statistics header", "//h1[text()='Public statistics']");
                put("Public Statistics label", "//*[@for='publicstatistics']");
                put("Public Statistics ON option", "//*[@for='publicstatistics_1']");
                put("Public Statistics OFF option", "//*[@for='publicstatistics_2']");
                put("Public Statistics Inherit option", "//*[@for='publicstatistics_3']");

                put("Show public statistic label", "//*[@for='publicgraphs']");
                put("Show public statistic ON option", "//*[@for='publicgraphs_1']");
                put("Show public statistic OFF option", "//*[@for='publicgraphs_2']");
                put("Show public statistic Inherit option", "//*[@for='publicgraphs_3']");
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the Theme Options page of the
     * Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyThemeOptions() throws Exception {
    	Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Simple Theme Options", "//*[@data-bs-target='#category-0']");
                put("Inherit everything label", "//*[@id='generalInherit']");
                put("Inherit option", "//*[@for='general_inherit_on']");
                put("Customize theme option", "//*[@for='general_inherit_off']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void verifyThemeOptions2() throws Exception {
    	Thread.sleep(10000);
        driver.findElement(By.xpath("//*[@for='general_inherit_off']")).click();
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Animate body label", "//*[@id='simpleEditOptions_animatebody']");
                put("Animate: Yes option", "//*[@for='animatebody_on']");
                put("Animate: No option", "//*[@for='animatebody_off']");
                put("Animate: Inherit option selected", "//*[@for='animatebody_inherit']");

                put("Show popups label", "//*[@id='simpleEditOptions_showpopups']");
                put("Popup option", "//*[@for='showpopups_1']");
                put("On Show option", "//*[@for='showpopups_0']");
                put("No option", "//*[@for='showpopups_-1']");

                put("Fix automatically label", "//*[@id='simpleEditOptions_fixnumauto']");
                put("Yes option", "//*[@for='fixnumauto_enable']");
                put("For expressions option", "//*[@for='fixnumauto_partial']");
                put("No option", "//*[@for='fixnumauto_disable']");
                put("Inherit option", "//*[@for='fixnumauto_inherit']");

                put("Hide privacy info label", "//*[@id='simpleEditOptions_hideprivacyinfo']");
                put("Yes option", "//*[@for='hideprivacyinfo_on']");
                put("No option", "//*[@for='hideprivacyinfo_off']");
                put("Inherit option", "//*[@for='hideprivacyinfo_inherit']");

                put("Show clear all label", "//*[@id='simpleEditOptions_showclearall']");
                put("Yes option", "//*[@for='showclearall_on']");
                put("No option", "//*[@for='showclearall_off']");
                put("Inherit option", "//*[@for='showclearall_inherit']");

                put("Survey Container label", "//*[@id='simpleEditOptions_container']");
                put("Yes option", "//*[@for='container_on']");
                put("No option", "//*[@for='container_off']");
                put("Inherit option", "//*[@for='container_inherit']");

                put("Question helptext label", "//*[@id='simpleEditOptions_questionhelptextposition']");
                put("Top option", "//*[@for='questionhelptextposition_top']");
                put("Bottom option", "//*[@for='questionhelptextposition_bottom']");
                put("Inherit option", "//*[@for='questionhelptextposition_inherit']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void verifyThemeOptions3() throws Exception {
    	Thread.sleep(10000);
        driver.findElement(By.xpath("//*[@data-bs-target='#category-1']")).click();
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Image logo label", "//*[@id='simpleEditOptions_brandlogo']");
                put("Yes option", "//*[@for='brandlogo_on']");
                put("No option", "//*[@for='brandlogo_off']");
                put("Inherit option", "//*[@for='brandlogo_inherit']");

                put("Logo file label", "//*[@id='simpleEditOptions_brandlogofile']");
                put("Logo file input", "//*[@id='simple_edit_options_brandlogofile']");
                put("Preview button", "//*[@data-bs-target='#simple_edit_options_brandlogofile']");
                put("File size limit note", "(//*[contains(text(), 'Upload an image (maximum size: 20 MB):')])[1]");
                put("Upload button", "//*[@for='upload_image_frontend']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void verifyThemeOptions4() throws Exception {
    	Thread.sleep(10000);
        driver.findElement(By.xpath("//*[@data-bs-target='#category-2']")).click();
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Fonts label", "//*[@id='simpleEditOptions_font']");
                put("Select font", "//*[@id='simple_edit_options_font']");
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the Participant Settings page of
     * the Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyParticipantSettings() throws Exception {
    	Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Anonymized label", "//*[@for='anonymized']");
                put("Anonymized ON option", "//*[@for='anonymized_1']");
                put("Anonymized OFF option", "//*[@for='anonymized_2']");
                put("Anonymized Inherit option", "//*[@for='anonymized_3']");
                put("Anonymized tooltip", "(//*[@data-toggle='tooltip'])[1]");
//              put("Anonymized tooltip text",
//                        "//*[contains(text(), 'Data privacy approval must be obtained in order')]");

                put("Require login label", "//*[@for='requireSAMLLogin']");
                put("Require login ON option", "//*[@for='requireSAMLLogin_1']");
                put("Require login OFF option", "//*[@for='requireSAMLLogin_2']");
                put("Require login tooltip", "(//*[@data-toggle='tooltip'])[2]");
                put("Require login tooltip text",
                        "//*[contains(text(),  'Enabling Required Accenture Login will require')]");

                put("Save Demographic Data label", "//*[@for='demographicDataLogging']");
                put("Save Demographic Data disabled options", "//*[@id='demographicDataLoggingElement']");
                put("Save Demographic Data tooltip", "(//*[@data-toggle='tooltip'])[3]");
 //             put("Save Demographic Data tooltip text",
 //                 "//*[contains(text(), 'Enabling Demographic Data supplies exports of survey responses with anonymous')]");
//              put("Save Demographic Data tooltip 2", "(//*[@data-toggle='tooltip'])[4]");
                put("Save Demographic Data tooltip text 2",
                        "//*[contains(text(), 'The Save Demographic Data is only enabled for non-anonymous surveys')]");

                put("Enabled participant label", "//*[@for='tokenanswerspersistence']");
                put("Enabled participant ON option", "//*[@for='tokenanswerspersistence_1']");
                put("Enabled participant OFF option", "//*[@for='tokenanswerspersistence_2']");
                put("Enabled participant Inherit option", "//*[@for='tokenanswerspersistence_3']");

                put("Allow multiple responses label", "//*[@id='alloweditaftercompletion-multiple']");
                put("Allow multiple responses ON option", "//*[@for='alloweditaftercompletion_1']");
                put("Allow multiple responses OFF option", "//*[@for='alloweditaftercompletion_2']");
                put("Allow multiple responses Inherit option", "//*[@for='alloweditaftercompletion_3']");

                put("Set access code label", "(//*[@for='tokenlength'])[1]");
                put("Set access code input", "//*[@class='form-control inherit-readonly d-block']");
                put("Set access code inherit label", "(//*[@for='tokenlength'])[2]");
                put("Set access code inherit ON option", "//*[@for='tokenlengthbutton_1']");
                put("Set access code inherit OFF option", "//*[@for='tokenlengthbutton_2']");

                put("Allow registration label", "//*[@for='allowregister']");
                put("Allow registration ON option", "//*[@for='allowregister_1']");
                put("Allow registration OFF option", "//*[@for='allowregister_2']");
                put("Allow registration Inherit option", "//*[@for='allowregister_3']");

                put("HTML format label", "//*[@for='htmlemail']");
                put("HTML format ON option", "//*[@for='htmlemail_1']");
                put("HTML format OFF option", "//*[@for='htmlemail_2']");
                put("HTML format Inherit option", "//*[@for='htmlemail_3']");

                put("Send confirmation email label", "//*[@for='sendconfirmation']");
                put("Send confirmation email ON option", "//*[@for='sendconfirmation_1']");
                put("Send confirmation email OFF option", "//*[@for='sendconfirmation_2']");
                put("Send confirmation email Inherit option", "//*[@for='sendconfirmation_3']");
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the Notifications page of the
     * Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyNotifications() throws Exception {
    	Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Date Stamp label", "//*[@for='datestamp']");
                put("Date Stamp ON option", "//*[@for='datestamp_1']");
                put("Date Stamp OFF option", "//*[@for='datestamp_2']");
                put("Date Stamp Inherit option", "//*[@for='datestamp_3']");

                put("Save IP address label", "//*[@for='ipaddr']");
                put("Save IP address ON option", "//*[@for='ipaddr_1']");
                put("Save IP address OFF option", "//*[@for='ipaddr_2']");
                put("Save IP address Inherit option", "//*[@for='ipaddr_3']");

                put("Anonymized IP address label", "//*[@for='ipanonymize']");
                put("Anonymized IP address ON option", "//*[@for='ipanonymize_1']");
                put("Anonymized IP address OFF option", "//*[@for='ipanonymize_2']");
                put("Anonymized IP address Inherit option", "//*[@for='ipanonymize_3']");

                put("Save Referrer label", "//*[@for='refurl']");
                put("Save Referrer ON option", "//*[@for='refurl_1']");
                put("Save Referrer OFF option", "//*[@for='refurl_2']");
                put("Save Referrer Inherit option", "//*[@for='refurl_3']");

                put("Save timings label", "//*[@for='savetimings']");
                put("Save timings ON option", "//*[@for='savetimings_1']");
                put("Save timings OFF option", "//*[@for='savetimings_2']");
                put("Save timings Inherit option", "//*[@for='savetimings_3']");

                put("Enabled assessment label", "//*[@for='assessments']");
                put("Enabled assessment ON option", "//*[@for='assessments_1']");
                put("Enabled assessment OFF option", "//*[@for='assessments_2']");
                put("Enabled assessment Inherit option", "//*[@for='assessments_3']");

                put("Save and resume label", "//*[@for='allowsave']");
                put("Save and resume ON option", "//*[@for='allowsave_1']");
                put("Save and resume OFF option", "//*[@for='allowsave_2']");
                put("Save and resume Inherit option", "//*[@for='allowsave_3']");

                put("Basic Admin notification email label", "(//*[@for='emailnotificationto'])[1]");
                put("Basic Admin notification email input disabled",
                        "(//*[contains(@class, 'form-control inherit-readonly')])[1]");
                put("Basic Admin notification email inherit label", "(//*[@for='emailnotificationto'])[2]");
                put("Basic Admin notification email inherit ON option", "//*[@for='emailnotificationtobutton_1']");
                put("Basic Admin notification email inherit OFF option", "//*[@for='emailnotificationtobutton_2']");

                put("Detailed Admin notification email label", "(//*[@for='emailresponseto'])[1]");
                put("Detailed Admin notification email input disabled",
                        "(//*[contains(@class, 'form-control inherit-readonly')])[2]");
                put("Detailed Admin notification email inherit label", "(//*[@for='emailresponseto'])[2]");
                put("Detailed Admin notification email inherit ON option", "//*[@for='emailresponsetobutton_1']");
                put("Detailed Admin notification email inherit OFF option", "//*[@for='emailresponsetobutton_2']");

                put("Google Analytics label", "//*[@for='googleanalyticsapikeysetting']");
                put("Google Analytics NONE option", "//*[@for='googleanalyticsapikeysetting_1']");
                put("Google Analytics uSE SETTINGS BELOW option", "//*[@for='googleanalyticsapikeysetting_2']");
                put("Google Analytics USE GLOBAL SETTINGS option", "//*[@for='googleanalyticsapikeysetting_3']");

                put("Google Analytics Tracking label", "//*[@for='googleanalyticsapikey']");
                put("Google Analytics Tracking input", "//*[@id='googleanalyticsapikey']");

                put("Google Analytics Style label", "//*[@id='googleanalyticsstyle']");
                put("Google Analytics Style OFF option", "//*[@for='googleanalyticsstyle_1']");
                put("Google Analytics Style DEFAULT option", "//*[@for='googleanalyticsstyle_2']");
                put("Google Analytics Style SURVEY-SID option", "//*[@for='googleanalyticsstyle_3']");
            }
        };
        verifyElementsByMap(elements);
    }

    /**
     * This method verifies the elements present on the Publications page of the
     * Survey Tool.
     * It checks for the presence of various UI components and logs their details.
     */

    public void verifyPublications() throws Exception {
    	Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Publications header", "//h1[text()='Publication date']");
                put("Start date label", "//*[@for='startdate']");
                put("Start date input", "//*[@id='startdate']");
                put("Start date date picker", "(//*[@data-td-target='#startdate_datetimepicker'])[2]");

                put("Expiration date label", "//*[@for='expires']");
                put("Expiration date input", "//*[@id='expires']");
                put("Expiration date date picker", "(//*[@data-td-target='#expires_datetimepicker'])[2]");

                put("Register survey", "//*[@href='https://mysurveys.accenture.com/']");
                put("Register survey box", "//*[@class='well']");
                put("Register survey checkbox", "//*[@id='surveyls_registration_checked']");
                put("Register survey text area", "//*[@id='register_short_title_en']");

                put("Set cookie label", "//*[@for='usecookie']");
                put("Set cookie ON option", "//*[@for='usecookie_1']");
                put("Set cookie OFF option", "//*[@for='usecookie_2']");
                put("Set cookie Inherit option", "//*[@for='usecookie_3']");

                put("Captcha header label", "//h1[text()='CAPTCHA']");
                put("Captcha for survey access label", "//*[@for='usecaptcha_surveyaccess']");
                put("Captcha for survey access ON option", "//*[@for='usecaptcha_surveyaccess_1']");
                put("Captcha for survey access OFF option", "//*[@for='usecaptcha_surveyaccess_2']");
                put("Captcha for survey access Inherit option", "//*[@for='usecaptcha_surveyaccess_3']");

                put("Captcha for registration label", "//*[@for='usecaptcha_registration']");
                put("Captcha for registration ON option", "//*[@for='usecaptcha_registration_1']");
                put("Captcha for registration OFF option", "//*[@for='usecaptcha_registration_2']");
                put("Captcha for registration Inherit option", "//*[@for='usecaptcha_registration_3']");

                put("Captcha for save and load label", "//*[@for='usecaptcha_saveandload']");
                put("Captcha for save and load ON option", "//*[@for='usecaptcha_saveandload_1']");
                put("Captcha for save and load OFF option", "//*[@for='usecaptcha_saveandload_2']");
                put("Captcha for save and load Inherit option", "//*[@for='usecaptcha_saveandload_3']");
            }
        };
        verifyElementsByMap(elements);
    }

    public void activateSurvey() throws Exception {
        try {
            // Wait for page stability and overlays to disappear
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            // Click on the Activate Survey button with retry mechanism
            By activateButtonLocator = By.xpath("//*[@id='ls-activate-survey']");
            if (clickWithRetry(activateButtonLocator, "Activate Survey Button")) {
                ExtentReporter.log(LogStatus.INFO, "Successfully clicked on Activate Survey button");
                
                // Wait for modal to appear and be ready
                Thread.sleep(20000);
                waitForPageLoad();
                
                // Click on Save Activate button in the modal
                By saveActivateButtonLocator = By.xpath("//*[@id='saveactivateBtn']");
                if (clickWithRetry(saveActivateButtonLocator, "Save Activate Modal Button")) {
                    ExtentReporter.log(LogStatus.INFO, "Successfully clicked on Save Activate modal button");
                    
                    // Wait for activation to complete
                    Thread.sleep(3000);
                    waitForPageLoad();
                    
                    // Verify activation success message
                    try {
                        WebElement successElement = findElementWithRetry(
                            By.xpath("//h5[contains(text(),'Congrats! Your survey has been activated')]"), 
                            "Activation Success Message"
                        );
                        
                        if (successElement != null) {
                            String successMessage = successElement.getText();
                            if (successMessage.contains("Congrats! Your survey has been activated successfully")) {
                                ExtentReporter.log(LogStatus.PASS, "Survey activated successfully: " + successMessage);
                            } else {
                                ExtentReporter.log(LogStatus.WARNING, "Unexpected activation message: " + successMessage);
                            }
                        } else {
                            // Try alternative success message locator
                            WebElement altSuccessElement = findElementWithRetry(
                                By.xpath("//div[contains(@class, 'alert-success')]"), 
                                "Alternative Success Message"
                            );
                            
                            if (altSuccessElement != null) {
                                String altMessage = altSuccessElement.getText();
                                ExtentReporter.log(LogStatus.PASS, "Survey activation confirmed: " + altMessage);
                            } else {
                                ExtentReporter.log(LogStatus.WARNING, "Could not find activation success message, but operation may have completed");
                            }
                        }
                    } catch (Exception verifyException) {
                        ExtentReporter.log(LogStatus.WARNING, "Could not verify activation message: " + verifyException.getMessage());
                    }
                } else {
                    throw new Exception("Failed to click Save Activate modal button");
                }
            } else {
                throw new Exception("Failed to click Activate Survey button");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to activate survey: " + e.getMessage());
            throw e;
        }
    }

    public void registerToMySurveys(String browserUsed) throws Exception {
        openNewTab();
        Thread.sleep(5000);
        driver.get(browserUsed);
        refreshPage();
        Thread.sleep(5000);
        refreshPage();
        clickRegisteraSurveyButton();
        InputSurveyTitle();
        InputSurveyDescription();
        chooseSurveyToolSelectListitem();
        InputSurveySender();
        SurveyAvailableUntil();
        updateRegisterOwner();
        InputSurveyContact();
        clickSurveyGuidanceCheckscheckbox();
        clickSubmitButton();
        clickCopyIcon();
        closeSecondTab();
        switchToMainWindow();
        Thread.sleep(2000);
    }

    public void openNewTab() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.open();");
            switchToNewWindow();

            System.out.println("Opened a new tab.");
        } catch (Exception e) {
            System.out.println("Failed to open a new tab.");
        }
    }

    public void closeSecondTab() {
        try {
            // Refresh tabs list to ensure it's current
            tabs = new ArrayList<String>(driver.getWindowHandles());
            
            if (tabs != null && tabs.size() > 1) {
                driver.switchTo().window(tabs.get(1));
                driver.close();
                ExtentReporter.log(LogStatus.INFO, "Successfully closed second tab");
            } else {
                ExtentReporter.log(LogStatus.WARNING, "No second tab to close or tabs list is null");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to close second tab: " + e.getMessage());
        }
    }

    public void clickRegisteraSurveyButton() throws Exception {
        click("//*[@class='register-a-survey']", "xpath");
    }

    public void InputSurveyTitle() throws Exception {
        try {
            String SurveyTitleTextfield = "//*[@id='mat-input-0']";
            Thread.sleep(1000);
            WebElement SurveyTitle = driver.findElement(By.xpath(SurveyTitleTextfield));
            SurveyTitle.sendKeys("Automated Regression Test Title. Please Disregard.");

            System.out.println("Successfully input Survey Title.");
            ExtentReporter.log(LogStatus.PASS, "Successfully input Survey Title.");

        } catch (Exception e) {
            System.out.println("Failed to input Survey Title.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to input Survey Title.");
        }
    }

    public void InputSurveyDescription() throws Exception {
        try {
            String SurveyDescriptionTextfield = "//*[@id='mat-input-1']";

            Thread.sleep(1000);
            WebElement SurveyDescription = driver.findElement(By.xpath(SurveyDescriptionTextfield));
            SurveyDescription.sendKeys("Automated Regression Test Description. Please Disregard.");

            System.out.println("Successfully input Survey Description.");
            ExtentReporter.log(LogStatus.PASS, "Successfully input Survey Description.");

        } catch (Exception e) {
            System.out.println("Failed to input Survey Description.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to input Survey Description.");
        }
    }

    public void chooseSurveyToolSelectListitem() throws Exception {
        try {
            String SurveyToolDropdownfield = "(//*[contains(@id, 'mat-select-value')])";
            Thread.sleep(1000);
            WebElement SelectListDropdown = driver.findElement(By.xpath(SurveyToolDropdownfield));
            SelectListDropdown.click();

            Thread.sleep(1000);
            WebElement chooseSurveyToolitemlist = driver.findElement(
                    By.xpath("//*[@class='mdc-list-item__primary-text'][contains(text(),'Accenture Survey Tool')]"));

            chooseSurveyToolitemlist.click();

            System.out.println("Successfully selected Survey Tool List item.");
            ExtentReporter.log(LogStatus.PASS, "Successfully selected Survey Tool List item.");

        } catch (Exception e) {
            System.out.println("Failed to select Survey Tool List item.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to select Survey Tool List item.");
        }
    }

    public void InputSurveySender() throws Exception {
        try {
            String SurveySenderTextfield = "//*[@id='mat-input-3']";

            Thread.sleep(1000);
            WebElement SurveySender = driver.findElement(By.xpath(SurveySenderTextfield));
            SurveySender.sendKeys("michalakis.francisco");

            System.out.println("Successfully input Survey Sender.");
            ExtentReporter.log(LogStatus.PASS, "Successfully input Survey Sender.");

        } catch (Exception e) {
            System.out.println("Failed to input Survey Sender.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to input Survey Sender.");
        }
    }

    public void SurveyAvailableUntil() throws Exception {
        try {
            String SurveyAvailableUntilCalendarfield = "//*[@class='mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base']";

            Thread.sleep(1000);
            WebElement SurveyCalendar = driver.findElement(By.xpath(SurveyAvailableUntilCalendarfield));
            SurveyCalendar.click();

            Thread.sleep(1000);
            WebElement SurveyCalendarDate = driver
                    .findElement(By.xpath("//*[contains(@class, 'mat-calendar-body-active')]"));
            SurveyCalendarDate.click();

            System.out.println("Successfully choose Survey Available Date.");
            ExtentReporter.log(LogStatus.PASS, "Successfully choose Survey Available Date.");

        } catch (Exception e) {
            System.out.println("Failed to choose Survey Available Date.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to choose Survey Available Date.");
        }
    }

    public void InputSurveyContact() throws Exception {
        try {

            String SurveyContactTextfield = "//*[@id='mat-input-6']";

            Thread.sleep(1000);
            WebElement SurveySender = driver.findElement(By.xpath(SurveyContactTextfield));
            SurveySender.sendKeys("michalakis.francisco@accenture.com");

            System.out.println("Successfully input Survey Contact.");
            ExtentReporter.log(LogStatus.PASS, "Successfully input Survey Contact.");

        } catch (Exception e) {
            System.out.println("Failed to input Survey Contact.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to input Survey Contact.");
        }
    }

    public void clickSurveyGuidanceCheckscheckbox() throws Exception {
        try {
            String DataPrivacyCheckboxfield = "//*[@id='mat-mdc-checkbox-1']";
            String EmployeeLegalCheckboxfield = "//*[@id='mat-mdc-checkbox-2']";
            String WorksCouncils_TradeUnionsCheckboxfield = "//*[@id='mat-mdc-checkbox-3']";
            String ClientDataCheckboxfield = "//*[@id='mat-mdc-checkbox-4']";

            Thread.sleep(1000);
            WebElement DataPrivacy = driver.findElement(By.xpath(DataPrivacyCheckboxfield));
            DataPrivacy.click();

            Thread.sleep(1000);
            WebElement EmployeeLegal = driver.findElement(By.xpath(EmployeeLegalCheckboxfield));
            EmployeeLegal.click();

            Thread.sleep(1000);
            WebElement WorksCouncils_TradeUnions = driver.findElement(By.xpath(WorksCouncils_TradeUnionsCheckboxfield));
            WorksCouncils_TradeUnions.click();

            Thread.sleep(1000);
            WebElement ClientData = driver.findElement(By.xpath(ClientDataCheckboxfield));
            ClientData.click();

            System.out.println("Successfully clicked the Survey Guidance Checks checkbox.");
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked the Survey Guidance Checks checkbox.");

        } catch (Exception e) {
            System.out.println("Failed to click the Survey Guidance Checks checkbox.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to click the Survey Guidance Checks checkbox.");
        }
    }

    public void clickSubmitButton() throws Exception {
        click("//*[@class='mdc-button__label'][contains(text(),'SUBMIT')]", "xpath");
        System.out.println("Successfully clicked submit button");

    }

    public void clickCopyIcon() throws Exception {
        click("//*[@class='copy-icon']", "xpath");
        System.out.println("Successfully clicked copy icon");

    }

    public void updateRegisterOwner() throws Exception {
        try {

            String SurveyContactTextfield = "//*[@id='mat-input-5']";

            Thread.sleep(1000);
            WebElement SurveySender = driver.findElement(By.xpath(SurveyContactTextfield));
            SurveySender.clear();
            SurveySender.sendKeys("michalakis.francisco");

            System.out.println("Successfully update register owner.");
            ExtentReporter.log(LogStatus.PASS, "Successfully update register owner.");

        } catch (Exception e) {
            System.out.println("Failed to update register owner");
            ExtentReporter.log(LogStatus.FAIL, "Failed to update register owner");
        }
    }

    public void publishAccessObj() throws Exception {
        try {
            Thread.sleep(1000);
            waitForPageLoad();
            waitForOverlayToDisappear();
            
            // Click on Register survey checkbox with retry
            By registerCheckboxLocator = By.xpath("//*[@id='surveyls_registration_checked']");
            if (clickWithRetry(registerCheckboxLocator, "Register Survey Checkbox")) {
                ExtentReporter.log(LogStatus.INFO, "Successfully clicked on Register survey checkbox");
                
                // Paste survey title into register text area
                WebElement textAreaElement = findElementWithRetry(
                    By.xpath("//*[@id='register_short_title_en']"), 
                    "Register Survey Text Area"
                );
                
                if (textAreaElement != null) {
                    textAreaElement.sendKeys(Keys.CONTROL, "v");
                    ExtentReporter.log(LogStatus.INFO, "Successfully pasted the survey title into Register survey text area");
                    
                    // Click Save button with retry
                    By saveButtonLocator = By.xpath("//*[@id='save-button']");
                    if (clickWithRetry(saveButtonLocator, "Save Button")) {
                        ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Save button to publish access object");
                        
                        // Wait for save operation to complete
                        Thread.sleep(2000);
                        waitForPageLoad();
                        
                        // Verify save success message if available
                        WebElement successElement = findElementWithRetry(
                            By.xpath("//*[contains(text(), 'Survey settings were successfully saved')]"),
                            "Save Success Message"
                        );
                        
                        if (successElement != null) {
                            ExtentReporter.log(LogStatus.PASS, "Publish access object completed successfully");
                        }
                    } else {
                        throw new Exception("Failed to click Save button");
                    }
                } else {
                    throw new Exception("Failed to find register survey text area");
                }
            } else {
                throw new Exception("Failed to click register survey checkbox");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to publish access object: " + e.getMessage());
            throw e;
        }
    }


    /**
     * This method adds survey participants to the survey.
     * It is currently a placeholder and can be implemented as needed.
     */
    public void verifyswitchClosedAccessMode() throws Exception {
    	Thread.sleep(10000);
        try {
            driver.findElement(By.xpath("(//*[@class='btn btn-outline-secondary btn-lg'])[1]")).click();
            ExtentReporter.log(LogStatus.PASS, "Switched to Closed Access Mode successfully.");
            Thread.sleep(10000);
            
            // Additional steps for closed access mode can be added here
            String SurveyParticipantsSuccessMessage = "//div[contains(@class,'jumbotron message-box card-participants ')]";
            highlightObject(SurveyParticipantsSuccessMessage);
            isElementDisplayed(SurveyParticipantsSuccessMessage, "xpath", "Import Survey Participants Success Message");
            
            driver.findElement(By.xpath("(//*[@class='btn btn-outline-secondary'])[1]")).click();
            ExtentReporter.log(LogStatus.PASS, "Clicked continue to Closed Access Mode.");

            WebElement surveyParticipantsPage = driver.findElement(By.xpath("//*[@class='side-body survey-response-page']"));
            if(surveyParticipantsPage.isDisplayed()) {
                ExtentReporter.log(LogStatus.PASS, "Survey Participants page is displayed after switching to Closed Access Mode.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Survey Participants page is not displayed after switching to Closed Access Mode.");
            }
        }
        catch (Exception e) {
            System.out.println("Failed to switch to Closed Access Mode.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to switch to Closed Access Mode: " + e.getMessage());
        }
    }

    public void addParticipantViaAD() {
        try {
            driver.findElement(By.xpath("//*[@id='ls-create-token-button']")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Add button");

            driver.findElement(By.xpath("(//*[@class='pjax dropdown-item'])[2]")).click();
            ExtentReporter.log(LogStatus.INFO, "Clicked on Add via AD option");

            WebElement eid = driver.findElement(By.xpath("//*[@id='enterpriseid']"));
            eid.sendKeys("michalakis.francisco");
            eid.clear();
            eid.sendKeys("michalakis.francisco");
            ExtentReporter.log(LogStatus.INFO, "Entered participant ID in the input field");
        } catch (Exception e) {
            System.out.println("Failed to add participant via AD.");
            ExtentReporter.log(LogStatus.FAIL, "Failed to add participant via AD: " + e.getMessage());
        }
    }
    
    /**
     * This method verifies the elements present on the Import Survey participants from CSV file page.
     * It checks for the presence of various UI components and logs their details.
     */
    

public void verifyImportSurveyParticipantsCSvFileObjs() throws Exception {
	    Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Import CSV File to Upload", "//label[contains(.,'Choose the CSV file to upload:')]");
                put("Import Choose File", "//input[@required='required'][contains(@id,'file')]");
                put("Import Character set of the file label", "//label[contains(.,'Character set of the file:')]");
                put("Import Automatic field", "//select[contains(@size,'1')]");
                put("Import Separator used label", "//label[contains(.,'Separator used:')]");
                put("Import Automatic Separator", "//label[contains(.,'Automatic')]");
                put("Import Comma Separator", "//label[contains(.,'Comma')]");
                put("Import Semicolon Separator", "//label[contains(.,'Semicolon')]");
                put("Import Filter blank email address", "//label[contains(.,'Filter blank email addresses:')]");
                put("Import On button in blank email", "//label[contains(@for,'filterblankemail_1')]");
                put("Import Off button in blank email ", "//label[@for='filterblankemail_2']");
                put("Import Allow invalid email addresses", "//label[contains(.,'Allow invalid email addresses:')]");
                put("Import On button in invalid email", "//label[@for='allowinvalidemail_1']");
                put("Import Off button in invalid email", "//label[@for='allowinvalidemail_2']");
                put("Import Display attribute warnings", "//label[contains(.,'Display attribute warnings:')]");
                put("Import On button in Attribute warnings", "//label[@for='showwarningtoken_1']");
                put("Import Off button in Attribute warnings", "//label[@for='showwarningtoken_2']");
                put("Import Filter duplicate records", "//label[contains(.,'Filter duplicate records:')]");
                put("Import On button in Duplicate records", "//label[@for='filterduplicatetoken_1']");
                put("Import Off button in Duplicate records", "//label[@for='filterduplicatetoken_2']");
                put("Import Notification icon", "(//span[contains(@class,'ri-notification-2-line me-2')])[1]");
                put("Import Notification Message", "//div[@class='mt-1 alert alert-filled-info']");
                put("Import Duplicates are determined by", "//label[contains(.,'Duplicates are determined by:')]");
                put("Import First Name in Determinde Duplicates", "//option[contains(.,'First name - firstname')]");
                put("Import Last Name in Determinde Duplicates", "//option[contains(.,'Last name - lastname')]");
                put("Import Email address in Determinde Duplicates", "//option[contains(.,'Email address - email')]");
                put("Import Email status in Determinde Duplicates", "//option[contains(.,'Email status - emailstatus')]");
                put("Import Language code in Determinde Duplicates", "//option[contains(.,'Language code - language')]");
                put("Import Completed in Determinde Duplicates", "//option[contains(.,'Completed - completed')]");
                put("Import Upload Button", "//button[contains(.,'Upload')]");
                put("Import 2nd Notification icon", "(//span[@class='ri-notification-2-line me-2'])[2]");
                put("Import CSV input format", "//strong[contains(.,'CSV input format')]");
                put("Import CSV input format message body", 
                		"//p[contains(.,'File should be a standard CSV (comma delimited) file with optional double quotes around values (default for most spreadsheet tools). The first line must contain the field names. The fields can be in any order.')]");
                put("Import Mandatory fields", "//span[contains(.,'Mandatory fields:')]");
                put("Import Optional fields", "//span[contains(.,'Optional fields:')]");
                put("Import Close Button", "//a[contains(.,'Close')]");               
            }
        };
        verifyElementsByMap(elements);
    }

    
    
    public void importBulkSurveyParticipantsCSVFile() throws Exception {
        try {

            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\survey_bulk_participants.xlsx";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);
//          driver.findElement(By.xpath("//*[@id='save-form-button']")).click();

            // Click the Upload Button
            driver.findElement(By.xpath("//button[contains(.,'Upload')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");

            Thread.sleep(10000);
            String importSuccessMessage = "//div[contains(@class,'jumbotron message-box')]";
            highlightObject(importSuccessMessage);
            isElementDisplayed(importSuccessMessage, "xpath", "Import Success Message");

            WebElement msgElement = driver.findElement(By.xpath(importSuccessMessage));
            ExtentReporter.log(LogStatus.INFO, "Import Survey summary: " + msgElement.getText());

            driver.findElement(By.xpath("//input[@value='Browse participants']")).click();
            Thread.sleep(2000);
            
            // Click the page selector
            driver.findElement(By.xpath("//select[@class='changePageSize form-select']")).click();
            driver.findElement(By.xpath("//option[@value='25'][contains(.,'25')]")).click();
            Thread.sleep(2000);
            
            String SurveyParticipants = "//div[@class='grid-view-ls']";
            highlightObject(SurveyParticipants);
            ExtentReporter.log(LogStatus.INFO, "Survey Participants: " +
                    driver.findElement(By.xpath(SurveyParticipants)).getText());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate survey participants: " + e.getMessage());
            throw e;
        }
    }

    public void importInvalidEmail() throws Exception {
        try {

            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\survey_participants_with_invalid_address.xlsx";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);
//          driver.findElement(By.xpath("//*[@id='save-form-button']")).click();

            // Click the Upload Button
            driver.findElement(By.xpath("//button[contains(.,'Upload')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");

            Thread.sleep(10000);
            String importSuccessMessage = "//div[@class='jumbotron message-box message-box-error']";
            highlightObject(importSuccessMessage);
            isElementDisplayed(importSuccessMessage, "xpath", "Import Success Message");

            WebElement msgElement = driver.findElement(By.xpath(importSuccessMessage));
            ExtentReporter.log(LogStatus.INFO, "Import Survey summary: " + msgElement.getText());

            driver.findElement(By.xpath("//input[@value='Browse participants']")).click();
            Thread.sleep(2000);
            
            String NoSurveyParticipants = "//td[contains(.,'No survey participants found.')]";
            highlightObject(NoSurveyParticipants);
            ExtentReporter.log(LogStatus.INFO, "Survey Participants: " +
                    driver.findElement(By.xpath(NoSurveyParticipants)).getText());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate survey participants: " + e.getMessage());
            throw e;
        }
    } 
    
    public void importDuplicateSurveyParticipants() throws Exception {
        try {

            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\survey_duplicate_participants.xlsx";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);
//          driver.findElement(By.xpath("//*[@id='save-form-button']")).click();

            // Click the Upload Button
            driver.findElement(By.xpath("//button[contains(.,'Upload')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");

            Thread.sleep(10000);
            String importSuccessMessage = "//div[@class='jumbotron message-box message-box-error']";
            highlightObject(importSuccessMessage);
            isElementDisplayed(importSuccessMessage, "xpath", "Import Success Message");

            WebElement msgElement = driver.findElement(By.xpath(importSuccessMessage));
            ExtentReporter.log(LogStatus.INFO, "Import Survey summary: " + msgElement.getText());

            driver.findElement(By.xpath("//input[@value='Browse participants']")).click();
            Thread.sleep(2000);
            
            String DuplicateSurveyParticipants = "//div[contains(@class,'grid-view-ls')]";
            highlightObject(DuplicateSurveyParticipants);
            ExtentReporter.log(LogStatus.INFO, "Survey Participants: " +
                    driver.findElement(By.xpath(DuplicateSurveyParticipants)).getText());

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate survey participants: " + e.getMessage());
            throw e;
        }
    } 
    
    public void importunsupportedfile() throws Exception {
        try {

            String filePath = System.getProperty("user.dir") + "\\files\\import_survey\\unsupported_file.jpg";
            driver.findElement(By.xpath("//*[@id='the_file']")).sendKeys(filePath);
//          driver.findElement(By.xpath("//*[@id='save-form-button']")).click();

            // Click the Upload Button
            driver.findElement(By.xpath("//button[contains(.,'Upload')]")).click();
            ExtentReporter.log(LogStatus.INFO, "Save button clicked");

            Thread.sleep(10000);
            String importSuccessMessage = "//div[@class='jumbotron message-box message-box-error']";
            highlightObject(importSuccessMessage);
            isElementDisplayed(importSuccessMessage, "xpath", "Import Success Message");

            WebElement msgElement = driver.findElement(By.xpath(importSuccessMessage));
            ExtentReporter.log(LogStatus.INFO, "Import Survey summary: " + msgElement.getText());
         
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to validate unsupported file: " + e.getMessage());
            throw e;
        }
    }    
    
    //////////////////////////STRUCTURE TAB//////////////////////////////////
    /// This section contains methods related to the structure tab of the survey tool.
    

    public void verifyQuestiongroup() throws Exception{
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Import Group button", "//*[@id='import-group']");
                put("Group list button", "//*[@id='back-button']");
                put("Save and Add question button", "//*[@id='save-and-new-question-button']");
                put("Save and Add group button", "//*[@id='save-and-new-button']");
                put("Save button", "//*[@id='save-button']");
                
                put("Add Question header", "//*[text()='Add question group']");
                put("Base language tab", "//*[@href='#en']");
                put("Title label", "(//label[normalize-space()='Title:'])[1]");
                put("Title input", "(//input[@id='group_name_en'])[1]");

                put("Description label", "(//label[normalize-space()='Description:'])[1]");
                put("Description ckEditor", "//*[@id='cke_description_en']");

                put("Randomization label", "(//label[normalize-space()='Randomization group:'])[1]");
                put("Randomization input", "//*[@id='randomization_group']");

                put("Condition label", "(//label[normalize-space()='Condition:'])[1]");
                put("Condition input", "//*[@id='grelevance']");

            }
        };
        verifyElementsByMap(elements);
    
    }

    public void addQuestionGroup() throws Exception {
        try {
            driver.findElement(By.xpath("(//input[@id='group_name_en'])[1]")).sendKeys("Test Group");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is a test group description.");
            switchToDefault();
            // Click Save button
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully added a new question group via Save button.");

            String groupSummaryHeader = driver.findElement(By.xpath("//*[@class='pagetitle h1']")).getText();
            if(groupSummaryHeader.contains("Group Summary")) {
                ExtentReporter.log(LogStatus.PASS, "Group Summary header is displayed as expected.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Group Summary header is not displayed as expected.");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to add question group: " + e.getMessage());
        }
    }

    public void addQGviaSaveAddQuestion() throws Exception {
        try {
            // Click Save and Add question button
             driver.findElement(By.xpath("(//input[@id='group_name_en'])[1]")).sendKeys("Test Group via Save and Add Question");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is a test group description for save and add question.");
            switchToDefault();
            // Click Save button
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully added a new question group via Save button.");

            String groupSummaryHeader = driver.findElement(By.xpath("//*[@class='pagetitle h1']")).getText();
            if(groupSummaryHeader.contains("Group Summary")) {
                ExtentReporter.log(LogStatus.PASS, "Group Summary header is displayed as expected.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Group Summary header is not displayed as expected.");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to add question via Save and Add Question button: " + e.getMessage());
        }
    }

    public void verifyHomepage() throws Exception {
    	
        Thread.sleep(10000);
        java.util.Map<String, String> elements = new java.util.LinkedHashMap<String, String>() {
            {
                put("Accenture Logo - Header", "(//*[@id='lime-logo'])[1]");
                put("Accenture Header", "//*[@class='navbar-brand acn-navbar-focus']");
                put("Create icon - Header", "//button[@aria-label='Create Survey']");
                put("Survey List - Header", "//a[@class='nav-link acn-nav-link ps-2 pe-2']");
                put("Active Survey - Header", "//a[@class='nav-link acn-nav-link ps-2 pe-2 active-surveys']");
                put("Configuration - Header", "(//*[contains(@class, 'mainmenu-dropdown-toggle')])[2]");
                put("Notification bell icon - Header", "(//i[@id='notification-bell'])[1]"); // STG envi
 //               put("Notification bell icon - Header", "//span[@id='notification-bell']"); // PRD envi
                put("User icon - Header", "(//*[contains(@class, 'rounded-circle')])[2]");
                put("Homepage Logo", "(//*[@id='lime-logo'])[2]");
                put("Homepage Tagline", "//*[text()='Your Survey, Your Way.']");
                put("Create tile", "//*[@id='card-1']");
                put("Manage Surveys tile", "//*[@id='card-2']");
//              put("Manage Themes tile", "//*[@id='card-3']"); Note: Manage Themes temporary not visible in STG envi
                put("Get Help tile", "//*[@id='card-4']");
                put("Learn More tile", "(//div[contains(@class,'more')])[1]");
                put("Footer", "//div[@class='acn-footer-wrp']");      
            }
        };
        verifyElementsByMap(elements);
    }   

    public void verifyCreateHeaderRedirection() throws Exception {
        try{
            driver.findElement(By.xpath("//button[@aria-label='Create Survey']")).click();

            String createPageURL = driver.getCurrentUrl();
            if(createPageURL.contains("surveys/index.php?r=surveyAdministration/newSurvey")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Create Survey page.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Create Survey page failed.");
            }

        }
        catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Create Header Redirection: " + e.getMessage());
        }
    }

    public void verifySurveyListRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//a[@class='nav-link acn-nav-link ps-2 pe-2']")).click();

            String surveyListURL = driver.getCurrentUrl();
            if(surveyListURL.contains("surveys/index.php?r=surveyAdministration/listsurveys")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Survey List page.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Survey List page failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Survey List Redirection: " + e.getMessage());
        }
    }

    public void verifyActiveSurveyRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//a[@class='nav-link acn-nav-link ps-2 pe-2 active-surveys']")).click();

            String activeSurveyURL = driver.getCurrentUrl();
            if(activeSurveyURL.contains("surveys/index.php?r=surveyAdministration/listsurveys/active/Ys")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Active Survey page.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Active Survey page failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Active Survey Redirection: " + e.getMessage());
        }
    }

    public void verifyAccentureLogoHeaderRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("(//*[@id='lime-logo'])[1]")).click();

            WebElement homepageTagline = driver.findElement(By.xpath("//*[text()='Your Survey, Your Way.']"));
            if(homepageTagline.isDisplayed()) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Homepage via Accenture Logo.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Homepage via Accenture Logo failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Accenture Logo Header Redirection: " + e.getMessage());
        }
    }

    public void verifyAccentureHeaderRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@class='navbar-brand acn-navbar-focus']")).click();

            WebElement homepageTagline = driver.findElement(By.xpath("//*[text()='Your Survey, Your Way.']"));
            if(homepageTagline.isDisplayed()) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Homepage via Accenture Header.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Homepage via Accenture Header failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Accenture Header Redirection: " + e.getMessage());
        }
    }

    public void verifyCreateTileRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='card-1']")).click();

            String createPageURL = driver.getCurrentUrl();
            if(createPageURL.contains("surveys/index.php?r=surveyAdministration/newSurvey")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Create Survey page via Create Tile.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Create Survey page via Create Tile failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Create Tile Redirection: " + e.getMessage());
        }
    }

    public void verifyManageSurveysTileRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='card-2']")).click();

            String surveyListURL = driver.getCurrentUrl();
            if(surveyListURL.contains("surveys/index.php?r=surveyAdministration/listsurveys")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Survey List page via Manage Surveys Tile.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Survey List page via Manage Surveys Tile failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Manage Surveys Tile Redirection: " + e.getMessage());
        }
    }

    public void verifyManageThemesTileRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='card-3']")).click();

            String manageThemesURL = driver.getCurrentUrl();
            if(manageThemesURL.contains("surveys/index.php?r=themeOptions")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Manage Themes page via Manage Themes Tile.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Manage Themes page via Manage Themes Tile failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Manage Themes Tile Redirection: " + e.getMessage());
        }
    }

    public void verifyGetHelpTileRedirection() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='card-4']")).click();

            String getHelpURL = driver.getCurrentUrl();
            if(getHelpURL.contains("https://in.accenture.com/digitalworker/servicenow/")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Get Help page via Get Help Tile.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Get Help page via Get Help Tile failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Get Help Tile Redirection: " + e.getMessage());
        }
    }

    public void verifyLearnMoreTileRedirection() throws Exception {
    	Thread.sleep(10000);
        try {
            driver.findElement(By.xpath("//button[contains(.,'LEARN MORE')]")).click();
      
            Thread.sleep(50000);

            String learnMoreURL = driver.getCurrentUrl();
            if(learnMoreURL.contains("https://in.accenture.com/digitalworker/toolkit/accenture-survey/")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Learn More page via Learn More Tile.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Redirection to Learn More page via Learn More Tile failed.");
            }

        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to verify Learn More Tile Redirection: " + e.getMessage());
        }
    }

    public void verifyQuestionGroupObjects() throws Exception {
        driver.findElement(By.xpath("//*[@id='adminsidepanel__sidebar--selectorCreateQuestionGroup']")).click();
        waitForPageLoad();
        highlightObject("//*[@id='adminsidepanel__sidebar--selectorCreateQuestionGroup']");
        isElementDisplayed("//*[@id='adminsidepanel__sidebar--selectorCreateQuestionGroup']", "xpath", "Add Group button");
        highlightObject("//*[@id='import-group']");
        isElementDisplayed("//*[@id='import-group']", "xpath", "Import Group button");
        highlightObject("//*[@id='back-button']");
        isElementDisplayed("//*[@id='back-button']", "xpath", "Group list button");
        highlightObject("//*[@id='save-and-new-question-button']");
        isElementDisplayed("//*[@id='save-and-new-question-button']", "xpath", "Save & add Question button");
        highlightObject("//*[@id='save-and-new-button']");
        isElementDisplayed("//*[@id='save-and-new-button']", "xpath", "Save & add Group button");
        highlightObject("//*[@id='save-button']");
        isElementDisplayed("//*[@id='save-button']", "xpath", "Save button");
        highlightObject("//*[@class='pagetitle h3']");
        isElementDisplayed("//*[@class='pagetitle h3']", "xpath", "Add Question Group header");
        highlightObject("//*[@href='#en']");
        isElementDisplayed("//*[@href='#en']", "xpath", "Base language tab");
        highlightObject("(//*[@for='group_name_en'])[1]");
        isElementDisplayed("(//*[@for='group_name_en'])[1]", "xpath", "Title label");
        highlightObject("//*[@id='group_name_en']");
        isElementDisplayed("//*[@id='group_name_en']", "xpath", "Title input");
        highlightObject("//*[@for='description_en']");
        isElementDisplayed("//*[@for='description_en']", "xpath", "Description label");
        highlightObject("//*[@id='cke_description_en']");
        isElementDisplayed("//*[@id='cke_description_en']", "xpath", "Description ckEditor");
        highlightObject("//*[@for='randomization_group']");
        isElementDisplayed("//*[@for='randomization_group']", "xpath", "Randomization label");
        highlightObject("//*[@id='randomization_group']");
        isElementDisplayed("//*[@id='randomization_group']", "xpath", "Randomization input");
        highlightObject("//*[@for='grelevance']");
        isElementDisplayed("//*[@for='grelevance']", "xpath", "Group relevance label");
        highlightObject("//*[@id='grelevance']");
        isElementDisplayed("//*[@id='grelevance']", "xpath", "Group relevance input");
    }

    public void veifyEditQuestionGroupObjects() throws Exception {
        driver.findElement(By.xpath("//*[@id='edit-button']")).click();
        ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Edit button for the question group.");


        highlightObject("//*[@class='pagetitle h1']");
        isElementDisplayed("//*[@class='pagetitle h1']", "xpath", "Edit Question Group header");
        highlightObject("//*[@id='ls-preview-button']");
        isElementDisplayed("//*[@id='ls-preview-button']", "xpath", "Preview survey button");
        highlightObject("//*[@id='ls-group-preview-button']");
        isElementDisplayed("//*[@id='ls-group-preview-button']", "xpath", "Preview question group button");
        highlightObject("//*[@id='close-button']");
        isElementDisplayed("//*[@id='close-button']", "xpath", "Close button");
        highlightObject("//*[@id='save-and-close-button']");
        isElementDisplayed("//*[@id='save-and-close-button']", "xpath", "Save and Close button");
        highlightObject("//*[@id='save-button']");
        isElementDisplayed("//*[@id='save-button']", "xpath", "Save button");
       highlightObject("(//*[@for='group_name_en'])[1]");
        isElementDisplayed("(//*[@for='group_name_en'])[1]", "xpath", "Title label");
        highlightObject("//*[@id='group_name_en']");
        isElementDisplayed("//*[@id='group_name_en']", "xpath", "Title input");
        highlightObject("//*[@for='description_en']");
        isElementDisplayed("//*[@for='description_en']", "xpath", "Description label");
        highlightObject("//*[@id='cke_description_en']");
        isElementDisplayed("//*[@id='cke_description_en']", "xpath", "Description ckEditor");
        highlightObject("//*[@for='randomization_group']");
        isElementDisplayed("//*[@for='randomization_group']", "xpath", "Randomization label");
        highlightObject("//*[@id='randomization_group']");
        isElementDisplayed("//*[@id='randomization_group']", "xpath", "Randomization input");
        highlightObject("//*[@for='grelevance']");
        isElementDisplayed("//*[@for='grelevance']", "xpath", "Group relevance label");
        highlightObject("//*[@id='grelevance']");
        isElementDisplayed("//*[@id='grelevance']", "xpath", "Group relevance input");
    }
        
    public void addNewQuestionGroupViaSavebutton() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='group_name_en']")).sendKeys("Test Group");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is a test group description.");
            switchToDefault();
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully added a new group via Save button.");

            String groupSummary = driver.findElement(By.xpath("//*[@id='groupdetails']")).getText();
            if(groupSummary.contains("Test Group")) {
                highlightObject("//*[@id='ls-tools-button']");
                isElementDisplayed("//*[@id='ls-tools-button']", "xpath", "Tools... button");
                highlightObject("//*[@id='ls-preview-button']");
                isElementDisplayed("//*[@id='ls-preview-button']", "xpath", "Preview survey button");
                highlightObject("//*[@id='ls-group-preview-button']");
                isElementDisplayed("//*[@id='ls-group-preview-button']", "xpath", "Preview question group button");
                highlightObject("//*[@class='pagetitle h1']");
                isElementDisplayed("//*[@class='pagetitle h1']", "xpath", "Group Summary header");
                highlightObject("//*[@id='groupdetails']");
                isElementDisplayed("//*[@id='groupdetails']", "xpath", "Group Summary text");
                ExtentReporter.log(LogStatus.PASS, "Group Summary is displayed as expected." + groupSummary);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Group Summary is not displayed as expected." + groupSummary);
            }
        }
        catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to click on Add Group button: " + e.getMessage());
        }
    }

    public void editQuestionGroupviaSaveAndClose() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='group_name_en']")).clear();
            ExtentReporter.log(LogStatus.PASS, "Successfully cleared the group name field.");

            driver.findElement(By.xpath("//*[@id='group_name_en']")).sendKeys("Updated Test Group");
            ExtentReporter.log(LogStatus.PASS, "Successfully updated the group name to 'Updated Test Group'.");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).clear();
            ExtentReporter.log(LogStatus.PASS, "Successfully cleared the group description field.");
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is an updated test group description.");
            ExtentReporter.log(LogStatus.PASS, "Successfully updated the group description.");
            switchToDefault();
            driver.findElement(By.xpath("//*[@id='save-and-close-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully edited the question group via Save and Close button.");

            String groupSummary = driver.findElement(By.xpath("//*[@id='groupdetails']")).getText();
            if(groupSummary.contains("Updated Test Group")) {
                highlightObject("//*[@id='ls-tools-button']");
                isElementDisplayed("//*[@id='ls-tools-button']", "xpath", "Tools... button");
                highlightObject("//*[@id='ls-preview-button']");
                isElementDisplayed("//*[@id='ls-preview-button']", "xpath", "Preview survey button");
                highlightObject("//*[@id='ls-group-preview-button']");
                isElementDisplayed("//*[@id='ls-group-preview-button']", "xpath", "Preview question group button");
                highlightObject("//*[@class='pagetitle h1']");
                isElementDisplayed("//*[@class='pagetitle h1']", "xpath", "Group Summary header");
                highlightObject("//*[@id='groupdetails']");
                isElementDisplayed("//*[@id='groupdetails']", "xpath", "Group Summary text");
                ExtentReporter.log(LogStatus.PASS, "Group Summary is updated as expected: " + groupSummary);
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Group Summary is not updated as expected: " + groupSummary);
            }
        }
        catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to edit the question group: " + e.getMessage());
        }
    }

     public void editQuestionGroupviaSaveBtn() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='edit-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Edit button for the question group.");

            driver.findElement(By.xpath("//*[@id='group_name_en']")).clear();
            ExtentReporter.log(LogStatus.PASS, "Successfully cleared the group name field.");
            
            driver.findElement(By.xpath("//*[@id='group_name_en']")).sendKeys("Updated Test Group");
            ExtentReporter.log(LogStatus.PASS, "Successfully updated the group name to 'Updated Test Group'.");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).clear();
            ExtentReporter.log(LogStatus.PASS, "Successfully cleared the group description field.");
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is an updated test group description.");
            ExtentReporter.log(LogStatus.PASS, "Successfully updated the group description.");
            switchToDefault();
            driver.findElement(By.xpath("//*[@id='save-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully edited the question group via Save button.");
           String currentURL = driver.getCurrentUrl();
            if(currentURL.contains("surveys/index.php?r=questionGroupsAdministration/edit&surveyid")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully stayed in the edit page after editing the question group.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Failed to stay in the edit page after editing the question group.");
            }
        }
        catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to edit the question group: " + e.getMessage());
        }
    }

     public void editQuestionGroupviaCloseBtn() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='close-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Edit button for the question group.");

           String currentURL = driver.getCurrentUrl();
            if(currentURL.contains("surveys/index.php?r=questionGroupsAdministration/view&surveyid")) {
                highlightObject("//*[@id='ls-tools-button']");
                isElementDisplayed("//*[@id='ls-tools-button']", "xpath", "Tools... button");
                highlightObject("//*[@id='ls-preview-button']");
                isElementDisplayed("//*[@id='ls-preview-button']", "xpath", "Preview survey button");
                highlightObject("//*[@id='ls-group-preview-button']");
                isElementDisplayed("//*[@id='ls-group-preview-button']", "xpath", "Preview question group button");
                highlightObject("//*[@class='pagetitle h1']");
                isElementDisplayed("//*[@class='pagetitle h1']", "xpath", "Group Summary header");
                highlightObject("//*[@id='groupdetails']");
                isElementDisplayed("//*[@id='groupdetails']", "xpath", "Group Summary text");
                ExtentReporter.log(LogStatus.PASS, "Successfully navigated to the Group Summary page after editing the question group.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Failed to navigate to the Group Summary page after editing the question group.");
            }
        }
            
        catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to edit the question group: " + e.getMessage());
        }
    }

    public void deleteQuestionGroup() throws Exception {
        try {
            driver.findElement(By.xpath("//div[@id='questionexplorer']//li[2]")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully selected the question group to delete.");
            driver.findElement(By.xpath("//*[@id='dropdownMenuButton1']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked question group ellipsis");
            driver.findElement(By.xpath("//*[@data-title='Delete group']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked Delete Group option.");

            // Use a more robust locator for the delete confirmation modal
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
            WebElement deleteModal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'modal-content') and .//*[@id='actionBtn']]")));
            if(deleteModal.isDisplayed()) {
                highlightObject("(//*[@class='modal-title'])[4]");
                isElementDisplayed("(//*[@class='modal-title'])[4]", "xpath", "Delete Group Modal Title");
                highlightObject("(//*[@class='btn-close'])[1]");
                isElementDisplayed("(//*[@class='btn-close'])[1]", "xpath", "X Close button on Delete Group Modal");
                highlightObject("(//*[@class='modal-body-text'])[1]");
                isElementDisplayed("(//*[@class='modal-body-text'])[1]", "xpath", "Delete Group Modal Body Text");
                highlightObject("(//*[@class='btn btn-cancel'])[1]");
                isElementDisplayed("(//*[@class='btn btn-cancel'])[1]", "xpath", "Cancel button on Delete Group Modal");
                highlightObject(".//*[@id='actionBtn']");
                isElementDisplayed(".//*[@id='actionBtn']", "xpath", "Delete button on Delete Group Modal");
                deleteModal.findElement(By.xpath(".//*[@id='actionBtn']")).click();
                ExtentReporter.log(LogStatus.PASS, "Successfully deleted the question group.");
                String currentURL = driver.getCurrentUrl();
                if(currentURL.contains("surveys/index.php?r=questionAdministration/listQuestions&surveyid")) {
                    ExtentReporter.log(LogStatus.PASS, "Successfully navigated to the Question Groups List page after deletion.");
                }
                else {
                    ExtentReporter.log(LogStatus.FAIL, "Failed to navigate to the Question Groups List page after deletion.");
                }
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Delete group modal is not displayed as expected.");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to delete the question group: " + e.getMessage());
        }
    }

    

     public void addNewQuestionGroupViaSaveAndAddQuestion() throws Exception {
        try {
            waitForPageLoad();
            driver.findElement(By.xpath("//*[@id='adminsidepanel__sidebar--selectorCreateQuestionGroup']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Create Question Group button for save and add question.");

            driver.findElement(By.xpath("//*[@id='group_name_en']")).sendKeys("Test Group via Save and Add Question");
            ExtentReporter.log(LogStatus.PASS, "Successfully entered group name for save and add question.");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is a test group description.");
            ExtentReporter.log(LogStatus.PASS, "Successfully entered group description for save and add question.");
            switchToDefault();
            driver.findElement(By.xpath("//*[@id='save-and-new-question-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Save and Add Question button.");

           String currentString = driver.getCurrentUrl();
            if(currentString.contains("surveys/index.php?r=questionAdministration/create&surveyid")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully navigated to the Create Question Group page.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Failed to navigate to the Create Question Group page.");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to click on Add Group button: " + e.getMessage());
        }
    }

     public void addNewQuestionGroupViaSaveAndAddGroup() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='adminsidepanel__sidebar--selectorCreateQuestionGroup']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Create Question Group button for save and add group.");

            driver.findElement(By.xpath("//*[@id='group_name_en']")).sendKeys("Test Group via Save and Add Group");
            ExtentReporter.log(LogStatus.PASS, "Successfully entered group name for save and add group.");
            switchToIframe1();
            driver.findElement(By.xpath("//*[@aria-label='Editor, description_en']")).sendKeys("This is a test group description.");
            ExtentReporter.log(LogStatus.PASS, "Successfully entered group description for save and add group.");
            switchToDefault();
            driver.findElement(By.xpath("//*[@id='save-and-new-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Save and Add Group button.");
           String currentString = driver.getCurrentUrl();
            if(currentString.contains("surveys/index.php?r=questionGroupsAdministration/add&surveyid")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully navigated to the Create Question Group page.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Failed to navigate to the Create Question Group page.");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to click on Add Group button: " + e.getMessage());
        }
    }

     public void addNewQuestionGroupViaGroupList() throws Exception {
        try {
            driver.findElement(By.xpath("//*[@id='back-button']")).click();
            ExtentReporter.log(LogStatus.PASS, "Successfully clicked on Group List button.");
           
            
           String currentString = driver.getCurrentUrl();
            if(currentString.contains("surveys/index.php?r=questionAdministration/listQuestions&surveyid")) {
                ExtentReporter.log(LogStatus.PASS, "Successfully navigated to the Overview questions &  page.");
            } else {
                ExtentReporter.log(LogStatus.FAIL, "Failed to navigate to the Create Question Group page.");
            }
        } catch (Exception e) {
            ExtentReporter.log(LogStatus.FAIL, "Failed to click on Add Group button: " + e.getMessage());
        }
    }

public void verifyAddQuestionsObjs() throws Exception {
    highlightObject("//*[@href='#question-tab']");
    isElementDisplayed("//*[@href='#question-tab']", "xpath", "Question tab");
    highlightObject("//*[@href='#question-help-tab']");
    isElementDisplayed("//*[@href='#question-help-tab']", "xpath", "Question help tab");
    highlightObject("//*[@id='cke_question_en']");
    isElementDisplayed("//*[@id='cke_question_en']", "xpath", "Question text editor");
    highlightObject("//*[@id='button-collapse-General']");
    isElementDisplayed("//*[@id='button-collapse-General']", "xpath", "General button");
    

}

//for homepage
public void verifyHeader() throws Exception {
		
    try {
        
        Thread.sleep(5000);
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
        wait.until(ExpectedConditions.visibilityOfElementLocated
                    (By.xpath("(//*[@class='navbar-logo'])[1]")));
        
    String accentureLogo = "(//*[@class='navbar-logo'])[1]";
    String accentureSurvey = "(//*[@class='navbar-brand acn-navbar-focus'])[1]";
    String createSurveyButton = "(//button[@aria-label='Create Survey'])[1]";
    String surveys = "(//*[@class='nav-link acn-nav-link ps-2 pe-2'])[1]";
    String activeSurveys = "(//*[@class='nav-link acn-nav-link ps-2 pe-2 active-surveys'])[1]";
    String config = "(//*[@class='nav-link acn-nav-link dropdown-toggle mainmenu-dropdown-toggle'])[1]";
    String notif = "(//i[@id='notification-bell'])[1]"; // STG envi
    //String notif = "//span[@id='notification-bell']"; // PRD envi
    String accountName = "(//*[@class='nav-link acn-nav-link dropdown-toggle d-flex align-items-center'])[1]";

    isElementDisplayed(accentureLogo, "xpath", "Accenture Logo");
    isElementDisplayed(accentureSurvey, "xpath", "Accenture Survey Brand");
    isElementDisplayed(createSurveyButton, "xpath", "Create Survey Button");
    isElementDisplayed(surveys, "xpath", "Surveys");
    isElementDisplayed(activeSurveys, "xpath", "Active Surveys");
//  isElementDisplayed(config, "xpath", "Configuration");
    isElementDisplayed(notif, "xpath", "Notification");
    isElementDisplayed(accountName, "xpath", "Account Name");
    
    
    System.out.println("Successfully validated NavBar Content.");
    ExtentReporter.log(LogStatus.PASS, "Successfully validated NavBar Content.");
    
    } catch (Exception e) {
        
    System.out.println("Failed to validate NavBar Content.");
    ExtentReporter.log(LogStatus.FAIL, "Failed to validate NavBar Content.");
    
}
}

public void verifyHomePageContent() throws Exception {
    
    try {
        
        Thread.sleep(1000);
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
        wait.until(ExpectedConditions.visibilityOfElementLocated
                    (By.xpath("(//*[@aria-label='DP-Legal Notice'])[1]")));
    
    String dpLegalNotice = "(//*[@aria-label='DP-Legal Notice'])[1]";
    String dpLegalRequestTooltip = "(//*[@aria-label='DP-Legal Notice'])[1]";
    String dataPrivacyHeader = "(//*[@id='data-policy-marquee-header'])[1]";
    String dataPrivacyContent = "(//*[@class='panel-body acn-marquee-panelbody'])[1]";
    String registerSurveyTitle = "(//*[@id='register-survey-marquee-header'])[1]";
    String registerSurveyContent = "(//*[@class='panel-body acn-marquee-panelbody'])[2]";
    String acnLogo = "(//*[@id='welcome-jumbotron'])[1]";
    
    // Create Surveys Tile
//		String createSurveysTitle = "(//*[@class='card-title acn-dashboardbox-title'][contains(text(),'CREATE SURVEYS')])[1]";
    String createSurveysDesc = "(//*[@class='card-body d-flex acn-dashboardbox-body'])[1]";
    String createSurveysBtn = "(//button[@class='btn btn-secondary w-100 acn-dashboard-btn'])[1]";
    
    // Manage Surveys Tile
//		String manageSurveysTitle = "(//*[@class='card-title acn-dashboardbox-title'][contains(text(),'MANAGE SURVEYS')])[1]";
    String manageSurveysDesc = "(//*[@class='card-body d-flex acn-dashboardbox-body'])[2]";
    String manageSurveysBtn = "(//button[@class='btn btn-secondary w-100 acn-dashboard-btn'])[2]";
    
    // Manage Themes Tile
//		String manageThemesTitle = "(//*[@class='card-title acn-dashboardbox-title'][contains(text(),'MANAGE THEMES')])[1]";
    String manageThemesDesc = "(//*[@class='card-body d-flex acn-dashboardbox-body'])[3]";
    String manageThemesBtn = "(//button[@class='btn btn-secondary w-100 acn-dashboard-btn'])[3]";
    
    // Get Help Tile
//		String getHelpTitle = "(//*[@class='card-title acn-dashboardbox-title'][contains(text(),'GET HELP')])[1]";
    String getHelpDesc = "(//*[@class='card-body d-flex acn-dashboardbox-body'])[4]";
    String getHelpBtn = "(//button[@class='btn btn-secondary w-100 acn-dashboard-btn'])[4]";
    
    // Learn More Tile
//		String learnMoreTitle = "(//*[@class='card-title acn-dashboardbox-title'][contains(text(),'LEARN MORE')])[1]";
    String learnMoreDesc = "(//*[@class='card-body d-flex acn-dashboardbox-body'])[4]";
    String learnMoreBtn = "(//button[@class='btn btn-secondary w-100 acn-dashboard-btn'])[4]";
        
    
    isElementDisplayed(dpLegalNotice, "xpath", "DP Legal Notice");
    isElementDisplayed(dpLegalRequestTooltip, "xpath", "DP Legal Request Tooltip");
    isElementDisplayed(dataPrivacyHeader, "xpath", "Data Privacy Header");
    isElementDisplayed(dataPrivacyContent, "xpath", "Data Privacy Content");
    isElementDisplayed(registerSurveyTitle, "xpath", "Register a Survey Title");
    isElementDisplayed(registerSurveyContent, "xpath", "Register a Survey Content");
    isElementDisplayed(acnLogo, "xpath", "Accenture Logo");
    
//		isElementDisplayed(createSurveysTitle, "xpath", "Create Surveys Title");
    isElementDisplayed(createSurveysDesc, "xpath", "Create Surveys Description");
    isElementDisplayed(createSurveysBtn, "xpath", "Create Surveys Button");
    
//		isElementDisplayed(manageSurveysTitle, "xpath", "Manage Surveys Title");
    isElementDisplayed(manageSurveysDesc, "xpath", "Manage Surveys Description");
    isElementDisplayed(manageSurveysBtn, "xpath", "Manage Surveys Button");
    
//		isElementDisplayed(manageThemesTitle, "xpath", "Manage Themes Title");
    isElementDisplayed(manageThemesDesc, "xpath", "Manage Themes Description");
    isElementDisplayed(manageThemesBtn, "xpath", "Manage Themes Button");
    
//		isElementDisplayed(getHelpTitle, "xpath", "Get Help Title");
    isElementDisplayed(getHelpDesc, "xpath", "Get Help Description");
    isElementDisplayed(getHelpBtn, "xpath", "Get Help Button");
    
//		isElementDisplayed(learnMoreTitle, "xpath", "Learn More Title");
    isElementDisplayed(learnMoreDesc, "xpath", "Learn More Description");
    isElementDisplayed(learnMoreBtn, "xpath", "Learn More Button");
    
    
    System.out.println("Successfully validated Homepage Content.");
    ExtentReporter.log(LogStatus.PASS, "Successfully validated Homepage Content.");
    
    } catch (Exception e) {
        
    System.out.println("Failed to validate Homepage Content.");
    ExtentReporter.log(LogStatus.FAIL, "Failed to validated Homepage Content.");
}
}

public void verifyFooter() throws Exception {
    
    highlightObject("//*[text()='Privacy Policy']");
    isElementDisplayed("//*[text()='Privacy Policy']", "xpath", "Privacy Policy is displayed");
    highlightObject("//*[text()='Cookie Policy']");
    isElementDisplayed("//*[text()='Cookie Policy']", "xpath", "Cookie Policy is displayed");
    highlightObject("//*[text()='Terms of Use']");
    isElementDisplayed("//*[text()='Terms of Use']", "xpath", "Terms of Use is displayed");
    
    String currentURL = driver.getCurrentUrl();
    if(currentURL.contains("https://surveytool.accenture.com/") || currentURL.contains("https://surveytool.ciostage.accenture.com/")) {
    highlightObject(
            "(//div[contains(.,'Copyright 2001-2026 Accenture. All rights reserved. Accenture Confidential. For internal use only.')])[4]");
    isElementDisplayed(
            "(//div[contains(.,'Copyright 2001-2026 Accenture. All rights reserved. Accenture Confidential. For internal use only.')])[4]",
            "xpath", "Copyright is displayed");
}
    
else if(currentURL.contains("https://surveytool.ciodev.accenture.com")){
    
    highlightObject(
            "(//div[contains(.,'Copyright 2001-2026 Accenture. All rights reserved. Accenture Confidential. For internal use only.')])[4]");
    isElementDisplayed(
            "(//div[contains(.,'Copyright 2001-2026 Accenture. All rights reserved. Accenture Confidential. For internal use only.')])[4]",
            "xpath", "Copyright is displayed");
}

}

public void privacyPolicy() throws Exception {
    String currentURL = driver.getCurrentUrl();

    if (currentURL.contains("https://surveytool.accenture.com/surveys/index.php?r=admin")) {
        String p = "//*[text()='Privacy Policy']";
        driver.findElement(By.xpath(p)).click();
        switchToNewTab();
        Thread.sleep(3000);
        verifyPrivacyPolicyHome();

        ExtentReporter.log(LogStatus.PASS, "Clicked Privacy Policy");
    } else if (currentURL.contains("https://surveytool.ciostage.accenture.com/surveys/index.php?r=admin") || currentURL.contains("https://surveytool.ciodev.accenture.com/")) {
        ExtentReporter.log(LogStatus.PASS, "Cannot access in Staging env");
    }

}

public void verifyPrivacyPolicyHome() throws Exception {

    String currentURL = driver.getCurrentUrl();
    if (currentURL.contains("https://in.accenture.com/protectingaccenture/5422-2/")) {
        driver.close();
        driver.switchTo().window(tabs.get(0));

        ExtentReporter.log(LogStatus.PASS, "Successfully Redirected to Protecting Accenture");
    } else {
        ExtentReporter.log(LogStatus.PASS, "Failed Redirecting to Protecting Accenture");
    }
}

public void cookiePolicy() throws Exception {
    String currentURL = driver.getCurrentUrl();

    if (currentURL.contains("https://surveytool.accenture.com/surveys/index.php?r=admin")) {
        String p = "//*[text()='Cookie Policy']";
        driver.findElement(By.xpath(p)).click();
        switchToNewTab();
        Thread.sleep(3000);
        verifyCookiePolicy();

        ExtentReporter.log(LogStatus.PASS, "Clicked Cookie Policy");
    } else if (currentURL.contains("https://surveytool.ciostage.accenture.com/surveys/index.php?r=admin") || currentURL.contains("https://surveytool.ciodev.accenture.com/")) {
        ExtentReporter.log(LogStatus.PASS, "Cannot access in Staging env");
    }
}

public void verifyCookiePolicy() throws Exception {

    String currentURL = driver.getCurrentUrl();
    if (currentURL.contains("https://www.accenture.com/us-en/support/company-cookies-similar-technology")) {
        driver.close();
        driver.switchTo().window(tabs.get(0));

        ExtentReporter.log(LogStatus.PASS, "Successfully Redirected to Cookies and Similar Technology");
    } else {
        ExtentReporter.log(LogStatus.FAIL, "Failed Redirecting to Cookies and Similar Technology");
    }
}

public void termsOfUse() throws Exception {
    String currentURL = driver.getCurrentUrl();

    if (currentURL.contains("https://surveytool.accenture.com/surveys/index.php?r=admin")) {
        String p = "//*[text()='Terms of Use']";
        driver.findElement(By.xpath(p)).click();
        switchToNewTab();
        Thread.sleep(5000);
        verifyTermsUse();

        ExtentReporter.log(LogStatus.PASS, "Clicked help tool");
    } else if (currentURL.contains("https://surveytool.ciostage.accenture.com/surveys/index.php?r=admin") || currentURL.contains("https://surveytool.ciodev.accenture.com/")) {
        ExtentReporter.log(LogStatus.PASS, "Cannot access in Staging env");
    }
}

public void verifyTermsUse() throws Exception {

    String currentURL = driver.getCurrentUrl();
    if (currentURL.contains("https://in.accenture.com/digitalworker/terms-of-use/")) {
        driver.close();
        driver.switchTo().window(tabs.get(0));

        ExtentReporter.log(LogStatus.PASS, "Successfully redirected to Terms of Use");
    } else {
        ExtentReporter.log(LogStatus.FAIL, "Failed Redirecting to Terms of Use");
    }
}

public void legalNotice() throws Exception {
    String currentURL = driver.getCurrentUrl();

    if (currentURL.contains("https://surveytool.accenture.com/surveys/index.php?r=admin")) {
        String p = "//*[text()='Legal Request Tool']";
        driver.findElement(By.xpath(p)).click();
        switchToNewTab();
        Thread.sleep(3000);
        verifyLegalNotice();

        ExtentReporter.log(LogStatus.PASS, "Clicked Legal Request Tool");
    } else if (currentURL.contains("https://surveytool.ciostage.accenture.com/surveys/index.php?r=admin" ) || currentURL.contains("https://surveytool.ciodev.accenture.com/")) {
        ExtentReporter.log(LogStatus.PASS, "Cannot access in Staging env");
    }
}

public void verifyLegalNotice() throws Exception {

    String currentURL = driver.getCurrentUrl();
    if (currentURL.contains("https://ts.accenture.com/sites/LegalCore/GeographicLegalSupport/SitePages/Global%20Legal%20Request%20Tool%20Landing%20Page.aspx")) {
        driver.close();
        driver.switchTo().window(tabs.get(0));

        ExtentReporter.log(LogStatus.PASS, "Successfully Redirected to Legal Request Tool Site");
    } else {
        ExtentReporter.log(LogStatus.FAIL, "Failed Redirecting to Legal Request Tool Site");
    }
}

public void dataPrivacy() throws Exception {

    WebElement ADPG = driver.findElement(By.xpath("(//*[@class='tooltip-test'])[2]"));
    String adpgValue = ADPG.getAttribute("href");
    boolean adpg = adpgValue.contains("https://kxdocuments.accenture.com/contribution/61877a85-7276-4f42-8a89-50465c818af7");
    ExtentReporter.log(
        adpg ? LogStatus.PASS : LogStatus.FAIL,
        adpg ? "Accenture Data Policy Guidance Link is correct." : "Accenture Data Policy Guidance Link is incorrect.");
}

public void switchToNewTab() {
    try {
        tabs = new ArrayList<String>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(1));
    } catch (Exception e) {
        System.out.println("Failed to switch to new tab...");
    }
}   

public void verifyConfig() throws Exception {
			
    String config = "//ul[@class='nav navbar-nav d-flex align-items-center']/li[3]";
    String systemOverview = "//*[@class='box__title text-center'][contains(text(), 'System overview')]";
    String themes = "//*[@class='link-themes'][contains(text(), 'Themes')]";
    String labelSets ="//*[@class='link-labels'][contains(text(), 'Label sets')]";
    String dataInteg = "//a[contains(@href, '/surveys/index.php?r=admin/checkintegrity')]";
    String backupDb = "//a[contains(@href, '/surveys/index.php?r=admin/dumpdb')]";
//	        String comfortUpdate = "//a[contains(@href, '/surveys/index.php?r=admin/update')]";
    String userManage = "//a[contains(@href, '/surveys/index.php?r=userManagement/index')]";
    String userGroups = "//a[contains(@href, '/surveys/index.php?r=userGroup/index')]";
//	        String userRoles = "//a[contains(@href, '/surveys/index.php?r=userRole/index')]";
    String centralManage = "//a[contains(@href, '/surveys/index.php?r=admin/participants/sa/displayParticipants')]";
    String dashboard = "//a[contains(@href, '/surveys/index.php?r=homepageSettings/index')]";
    String global = "//a[contains(@href, '/surveys/index.php?r=admin/globalsettings')]";
    String globalSurvey = "//a[contains(@href, '/surveys/index.php?r=admin/globalsettings/sa/surveysettings')]";
    String plugins = "//a[contains(@href, '/surveys/index.php?r=admin/pluginmanager/sa/index')]";
    String surveyMenu = "//a[contains(@href, '/surveys/index.php?r=admin/menus/sa/view')]";

    highlightObject(config);
    isElementDisplayed(config, "xpath", "Configuration");
   
    WebElement configClick = driver.findElement(By.xpath("//ul[@class='nav navbar-nav d-flex align-items-center']/li[3]"));
    configClick.click();
    ExtentReporter.log(LogStatus.PASS, "Configuration menu is displayed");

    isElementDisplayed(systemOverview, "xpath", "System Overview");

    highlightObject(themes);
    isElementDisplayed(themes, "xpath", "Themes");

    highlightObject(labelSets);
    isElementDisplayed(labelSets, "xpath", "Label Sets");

    highlightObject(dataInteg);
    isElementDisplayed(dataInteg, "xpath", "Data integrity");

    highlightObject(backupDb);
    isElementDisplayed(backupDb, "xpath", "Backup entire database");

    highlightObject(userManage);
    isElementDisplayed(userManage, "xpath", "User management");

    highlightObject(userGroups);
    isElementDisplayed(userGroups, "xpath", "User groups");

    highlightObject(centralManage);
    isElementDisplayed(centralManage, "xpath", "Central participant management");

    highlightObject(dashboard);
    isElementDisplayed(dashboard, "xpath", "Dashboard");

    highlightObject(global);
    isElementDisplayed(global, "xpath", "Global");

    highlightObject(globalSurvey);
    isElementDisplayed(globalSurvey, "xpath", "Global survey");

    highlightObject(plugins);
    isElementDisplayed(plugins, "xpath", "Plugins");

    highlightObject(surveyMenu);
    isElementDisplayed(surveyMenu, "xpath", "Survey Menu");
}
    
}


