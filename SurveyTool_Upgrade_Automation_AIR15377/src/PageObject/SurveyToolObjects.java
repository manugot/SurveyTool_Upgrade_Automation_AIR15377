package PageObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.ElementClickInterceptedException;
import Common.BasePage;
public class SurveyToolObjects extends BasePage {
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int STANDARD_WAIT_TIME = 10;
    public SurveyToolObjects(WebDriver driver) {
        super(driver);
    }
    /**
     * Robust click method with retry mechanism for stale elements
     */
    private boolean clickWithRetry(String xpath, String description) {
        By locator = By.xpath(xpath);
        int attempts = 0;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(STANDARD_WAIT_TIME));
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                
                // Try normal click first
                element.click();
                return true;
                
            } catch (ElementClickInterceptedException e) {
                try {
                    WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(STANDARD_WAIT_TIME));
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    return true;
                } catch (Exception jsException) {
                    // Continue to retry
                }
            } catch (StaleElementReferenceException e) {
                attempts++;
                
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000); // Wait before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                attempts++;
            }
        }
        
        // Fallback to original click method
        try {
            click(xpath, "xpath");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Wait for page stability before proceeding
     */
    private void waitForPageStability() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            
            // Wait for jQuery if available
            try {
                wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return jQuery.active == 0"));
            } catch (Exception e) {
                // jQuery might not be available, continue
            }
            
            Thread.sleep(1000); // Small buffer for final stabilization
        } catch (Exception e) {
            // Continue if page stability check fails
        }
    }
    public void clickLogin() {
        waitForPageStability();
        clickWithRetry("//*[@id='samlbutton']/img", "Login button");
    }
    public void clickAccentureLogo() {
        waitForPageStability();
        clickWithRetry("//*[@id='lime-logo']", "Accenture Logo");
    }
    public void clickCreateSurveyTile() {
        waitForPageStability();
        clickWithRetry("(//*[contains(@class, 'acn-dashboard-btn')])[1]", "Create Survey Tile");
    }
    public void clickImportSurveyTab() {
        waitForPageStability();
        clickWithRetry("//*[@href='#import']", "Import Survey Tab");
    }
    public void clickCopySurveyTab() {
        waitForPageStability();
        clickWithRetry("//*[@href='#copy']", "Copy Survey Tab");
    }
    public void clickManageTile() {
        waitForPageStability();
        clickWithRetry("(//*[contains(@class, 'acn-dashboard-btn')])[2]", "Manage Tile");
    }
    public void clickSettingsTab() {
        waitForPageStability();
        clickWithRetry("//*[@href='#settings']", "Settings Tab");
    }
    public void clickStructureTab() {
        waitForPageStability();
        clickWithRetry("//*[@href='#structure']", "Structure Tab");
    }
    public void clickOverviewTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_overview']", "Overview Tab");
    }
    public void 
    clickGeneralSettingsTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_generalsettings']", "General Settings Tab");
    }
    public void clickPrivacyPolicyTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_datasecurity']", "Privacy Policy Tab");
    }
    public void clickTextElementsTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_surveytexts']", "Text Elements Tab");
    }
    public void clickThemeOptionsTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_theme_options']", "Theme Options Tab");
    }
    public void clickPresentationTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_presentation']", "Presentation Tab");
    }
    public void clickParticipantSettingsTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_tokens']", "Participant Settings Tab");
    }
    public void clickNotificationsTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_notification']", "Notifications Tab");
    }
    public void clickPublicationTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_publication']", "Publication Tab");
    }
    public void clickSurveyListHeader() {
        waitForPageStability();
        clickWithRetry("(//a[@class='nav-link acn-nav-link ps-2 pe-2'])[1]", "Survey List Header");
    }
    public void click1stSurvey() {
        waitForPageStability();
        clickWithRetry("(//td[contains(.,'CREATE - Automation Survey. Please disregard.')])[1]", "1st Survey");
    }
    public void clickOtherSurvey() {
        waitForPageStability();
        clickWithRetry("(//td[contains(.,'Updated Survey Title - Automation')])[1]", "Other Survey");
    }
    
    public void click1stUpdatedSurvey() {
        waitForPageStability();
        clickWithRetry("(//td[normalize-space()='Updated Survey Title - Automation'])[1]", "First Updated Survey");
    }
    public void clickSurveyGroupTab() {
        waitForPageStability();
        clickWithRetry("//*[@href='#surveygroups']", "Survey Group Tab");
    }
    public void clickSurveyParticipantsTab() {
        waitForPageStability();
        clickWithRetry("//*[@id='sidemenu_participants']", "Survey Participants Tab");
    }
    
    public void clickAdddropdown() {
        waitForPageStability();
        clickWithRetry("(//i[contains(@class,'ri-more-fill')])[1]", "Add dropdwon");
    }

    
    public void clickCSVFile() {
        waitForPageStability();
        clickWithRetry("//a[@class='pjax dropdown-item'][contains(.,'CSV file')]", "CSV FIle");
    }

}
