package Common;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.relevantcodes.extentreports.ExtentReports;

public abstract class BasePage {


    /** Timeout for finding page element in seconds */
    protected static final long FIND_ELEMENT_TIMEOUT = 30L;
	
	/** The Selenium2 web driver. */
    protected static WebDriver driver;
    
    public BasePage(WebDriver driver)
    {
        BasePage.driver = driver;
    }
    
    
    /**
     * Find the element in the DOM by id.
     * 
     * @param elementId the element id
     * @return the element found
     */
    public static WebElement findElement(String elementId, String locType)
    {
    	WebElement element = null;
    	
        try
        {
        	if(locType.equalsIgnoreCase("id")){
            return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                    .visibilityOfElementLocated(By.id(elementId)));
            
        	} else if (locType.equalsIgnoreCase("name")){        		
            return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                    .visibilityOfElementLocated(By.name(elementId)));
            
            } else if (locType.equalsIgnoreCase("linktext")){        		
                return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                        .visibilityOfElementLocated(By.linkText(elementId)));
                
            } else if (locType.equalsIgnoreCase("partiallinktext")){        		
                return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                        .visibilityOfElementLocated(By.partialLinkText(elementId)));
                
            } else if (locType.equalsIgnoreCase("css")){        		
                return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                        .visibilityOfElementLocated(By.cssSelector(elementId)));
                
            } else if (locType.equalsIgnoreCase("class")){        		
                return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                        .visibilityOfElementLocated(By.className(elementId)));
                
            } else{        		
            return new WebDriverWait(driver, java.time.Duration.ofSeconds(FIND_ELEMENT_TIMEOUT)).until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath(elementId)));
            }
        }
        catch (Exception e)
        {
            System.out.println("Element is not found");
            return element;
            
        	}
    	}

    
		    /**
		     * This method will click a web element
		     * 
		     * @param locator
		     * 
		     * @author lenard.g.magpantay
		     * 
		     */
	        
	        public static void click (String locator, String locType){
	        	
	    	 WebElement element = findElement(locator, locType);
	    	 element.click();
	        	
	        }
	        public static void mouseOverClick(String elementId, String locType) throws InterruptedException{
	        	Actions builder = new Actions(driver);
	        	WebElement object = findElement(elementId, locType);
	        	Action MoveToAcertainObject = builder.moveToElement(object).click().build();
	        	MoveToAcertainObject.perform();
	        }
	        /**
	         * This method will enter text in a web element
	         * 
	         * @param locator
	         * @param value
	         *
	         * @author lenard.g.magpantay
	            element.submit();
	         * 
	         */
	        public static void enterText(String locator, String locType, String value){
	    	
	    		WebElement element = findElement(locator, locType);
	    		element.sendKeys(value);
	
	        }
	        public static void clearText(String locator, String locType){
		    	
	    		WebElement element = findElement(locator, locType);
	    		element.clear();
	
	        }
        
        /**
         * This method will select a text value from a dropdown
         * 
         * @param locator
         * @param value
         *
         * @author lenard.g.magpantay
         * 
         */
        public void selectDropdownByVisibleText(String locator, String locType, String value){
        	
        new Select(findElement(locator, locType)).selectByVisibleText(value);
        }
        
        
        
        
}
