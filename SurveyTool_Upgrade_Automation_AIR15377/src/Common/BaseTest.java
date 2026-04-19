package Common;

import static org.testng.AssertJUnit.assertEquals;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.AssertJUnit;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import com.codoid.products.exception.FilloException;
import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Fillo;
import com.codoid.products.fillo.Recordset;
import com.relevantcodes.extentreports.ExtentReports;
import com.relevantcodes.extentreports.ExtentTest;
import com.relevantcodes.extentreports.LogStatus;

public class BaseTest {
	public static int ScenarioCounter;
	protected String ModuleName;
	protected static int FIND_ELEMENT_TIMEOUT = 1;
	public static WebDriver driver;
	public static int TestCaseRow;
	private StringBuffer verificationErrors = new StringBuffer();
	protected static ExtentReports extent;
	protected static ExtentTest ExtentReporter;
	public BasePage basePageGetter;
	protected int[] solutionArray = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25};
	protected int j = 1;
	protected String TestCaseName;
	protected File currentDirFile;
	protected String userLogged ;
	protected String path;
	protected String folderName;
	protected File pathChecker;
	protected File folderChecker;
	protected String scrFolder1;
	protected String scrFolder;
	protected File scrFolderChecker;
	protected String folderNameMain;
	protected String TestScenario;
	protected String helper;
	protected int counterForOpenDriver;
	
	@BeforeTest 
	public void startReport() {
		ScenarioCounter = ScenarioCounter + 1;
		extent = new ExtentReports(scrFolder1 + "_"+ModuleName+".html", true);
		extent
		.addSystemInfo("Project", "CIO-Collaboration_ITI")
		.addSystemInfo("Application","Survey Tool")
		.addSystemInfo("User Name", System.getProperty("user.name"))
		.addSystemInfo("Module Name", ModuleName);
		extent.loadConfig(new File(System.getProperty("user.dir") + "\\extent-config.xml"));
	}
	@AfterMethod
	public void getResult(ITestResult result) {
		if (result.getStatus() == ITestResult.FAILURE) {
			ExtentReporter.log(LogStatus.FAIL, result.getThrowable());
		}
		extent.endTest(ExtentReporter);
		ScenarioCounter++;
	}
	
	@AfterTest
	public void endreport() {
		extent.flush();
		extent.close();
		ScenarioCounter = 0;
	}	
	
	@AfterSuite
	public void closeBrowser(){
		if (driver != null) {
			driver.quit();
		}
	}
	/*public void startTest(String URL) {		
		TestCaseName = this.getClass().getSimpleName();
		currentDirFile = new File("");
		String helper = currentDirFile.getAbsolutePath() + "\\WebDrivers\\32\\chromedriver.exe";
		this.helper = helper;
		log("");
		log(">>>>> Executing " + TestCaseName + " <<<<<");
		log("");	
		userLogged = System.getProperty("user.name");
		path = currentDirFile.getAbsolutePath();
		folderName = path + "\\test-output\\"+ModuleName;
		pathChecker = new File(path);
		folderChecker = new File(folderName);
		if(!TestCaseName.equals("webDriverStarter")){
			if(pathChecker.exists()){
			}
			else{
				pathChecker.mkdir();
			}	
			if(folderChecker.exists()){
			}
			else{
				folderChecker.mkdir();
			}		
			scrFolder = folderChecker + "\\"
		            + new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime()).toString();		
			scrFolderChecker = new File(scrFolder);
			if(scrFolderChecker.exists()){
			}
			else{
				scrFolderChecker.mkdir();
			}	
			scrFolder1 = scrFolder + "\\"
		            + new SimpleDateFormat("yyyy_MM_dd_HHmmss").format(Calendar.getInstance().getTime()).toString();		
		    System.setProperty("scr.folder", scrFolder1);
		    System.out.println("");
		    System.out.println(scrFolder1);
		    folderNameMain  = "";
		}
//	    if (counterForOpenDriver==0){
//	    	openDriver(URL);
//	    }
	}*/
	
	public void startTest(String URL) {		
		TestCaseName = this.getClass().getSimpleName();
		currentDirFile = new File("");
		String helper = currentDirFile.getAbsolutePath() + "\\WebDrivers\\32\\msedgedriver.exe";
		this.helper = helper;
		log("");
		log(">>>>> Executing " + TestCaseName + " <<<<<");
		log("");	
		userLogged = System.getProperty("user.name");
		path = currentDirFile.getAbsolutePath();
        if (!ModuleName.equalsIgnoreCase("")){
            folderName = path + "\\test-output\\"+ new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime()).toString()+"\\"+ModuleName+"\\"+TestCaseName;
            pathChecker = new File(path);
            folderChecker = new File(folderName);
            if(pathChecker.exists()){
            }
            else{
                   pathChecker.mkdir();
            }      
            if(folderChecker.exists()){
            }
            else{
                   folderChecker.mkdir();
            } 
            scrFolder = folderChecker.toString();
//            scrFolder = folderChecker + "\\"
//                  + new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime()).toString();         
            scrFolderChecker = new File(scrFolder);
            if(scrFolderChecker.exists()){
            }
            else{
                   scrFolderChecker.mkdir();
            }      
            scrFolder1 = scrFolder + "\\"
                  + new SimpleDateFormat("yyyy_MM_dd_HHmmss").format(Calendar.getInstance().getTime()).toString();
            String testOutputHtml = scrFolder + "\\"+ TestCaseName+"_"
                  + new SimpleDateFormat("yyyy_MM_dd_HHmmss").format(Calendar.getInstance().getTime()).toString();
          System.setProperty("scr.folder", testOutputHtml);
          System.out.println("");
          System.out.println(scrFolder1);
          folderNameMain  = "";

		}
//	    if (counterForOpenDriver==0){
//	    	openDriver(URL);
//	    }
	}
	
	public void openDriver(String URL){
		try {
//			automaticLogin();
			driver = initializeEdgeDriver();
//			System.out.println("Test is running in Edge");
			driver.get(URL);
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(60));
			driver.manage().window().maximize();
			log("Opened " + URL);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize Microsoft Edge WebDriver", e);
		}		
	}
	
	public void openDriverEdge(String URL){
		try {
//			automaticLogin();
			driver = initializeEdgeDriver();
//			System.out.println("Test is running in Edge");
			driver.get(URL);
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(60));
			driver.manage().window().maximize();
			log("Opened " + URL);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize Microsoft Edge WebDriver", e);
		}		
	}

	private WebDriver initializeEdgeDriver() {
		Exception webDriverManagerException = null;
		try {
			WebDriverManager.edgedriver().setup();
			return new EdgeDriver();
		} catch (Exception e) {
			webDriverManagerException = e;
			log("WebDriverManager failed. Falling back to local EdgeDriver at: " + helper);
		}

		try {
			if (helper != null && !helper.trim().isEmpty() && new File(helper).exists()) {
				System.setProperty("webdriver.edge.driver", helper);
				return new EdgeDriver();
			}
		} catch (Exception localDriverException) {
			if (webDriverManagerException != null) {
				localDriverException.addSuppressed(webDriverManagerException);
			}
			throw new RuntimeException("Unable to initialize EdgeDriver using both WebDriverManager and local driver: " + helper, localDriverException);
		}

		throw new RuntimeException("Unable to initialize EdgeDriver. WebDriverManager could not download driver and local driver was not found at: " + helper, webDriverManagerException);
	}
	

	public void startTestIE(String URL) {		
		TestCaseName = this.getClass().getSimpleName();
		currentDirFile = new File("");
		String helper = currentDirFile.getAbsolutePath() + "\\WebDrivers\\32\\IEDriverServer.exe";
		this.helper = helper;
		log("");
		log(">>>>> Executing " + TestCaseName + " <<<<<");
		log("");	
		userLogged = System.getProperty("user.name");
		path = currentDirFile.getAbsolutePath();
        if (!ModuleName.equalsIgnoreCase("")){
            folderName = path + "\\test-output\\"+ new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime()).toString()+"\\"+ModuleName+"\\"+TestCaseName;
            pathChecker = new File(path);
            folderChecker = new File(folderName);
            if(pathChecker.exists()){
            }
            else{
                   pathChecker.mkdir();
            }      
            if(folderChecker.exists()){
            }
            else{
                   folderChecker.mkdir();
            } 
            scrFolder = folderChecker.toString();
//            scrFolder = folderChecker + "\\"
//                  + new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime()).toString();         
            scrFolderChecker = new File(scrFolder);
            if(scrFolderChecker.exists()){
            }
            else{
                   scrFolderChecker.mkdir();
            }      
            scrFolder1 = scrFolder + "\\"
                  + new SimpleDateFormat("yyyy_MM_dd_HHmmss").format(Calendar.getInstance().getTime()).toString();
            String testOutputHtml = scrFolder + "\\"+ TestCaseName+"_"
                  + new SimpleDateFormat("yyyy_MM_dd_HHmmss").format(Calendar.getInstance().getTime()).toString();
          System.setProperty("scr.folder", testOutputHtml);
          System.out.println("");
          System.out.println(scrFolder1);
          folderNameMain  = "";

		}
//	    if (counterForOpenDriver==0){
//	    	openDriver(URL);
//	    }
	}
	
	public void openDriverIE(String URL){
		try {
//			System.setProperty("webdriver.chrome.driver", helper);
//			driver = new ChromeDriver();
			System.setProperty("webdriver.ie.driver", helper);
			InternetExplorerOptions capabilities = new InternetExplorerOptions();
			capabilities.ignoreZoomSettings();
			driver = new InternetExplorerDriver(capabilities);
//			System.out.println("Test is running in Chrome");
			driver.get(URL);
			driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(60));
			driver.manage().window().maximize();
			log("Opened " + URL);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	@AfterClass
	public void endTest() {
		String TestCaseName = this.getClass().getSimpleName();
		log("");
		log(">>>>> Terminating " + TestCaseName + " <<<<<");
		log("");
//		
//		driver.close();
//		driver.quit();
	}
	
	
	public static void log(String message) {
		System.out.println(message);
		Reporter.log(message);
	}
	
	/*
	public void takescreenshot() throws IOException {
		try {
			File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			FileUtils.copyFile(scrFile, new File("C:\\Users\\justin.josef.d.bulan\\Desktop\\Selenium\\screenshot\\"
					+ getFileName(this.getClass().getSimpleName())));
		} catch (Exception e) {
			log("Screenshot is not created.");
			e.printStackTrace();
		}
	} */
	
	public String getFileName(String nameTest) throws IOException {
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy_hh.mm.ss");
		Date date = new Date();
		return dateFormat.format(date) + "_" + nameTest + ".png";
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
	public void assertTextPresentInElement(String locator, String locType, String StringToBeCheck) throws Exception {
		String valueToCheck = StringToBeCheck;
		System.out.println("valueTocheck" + valueToCheck);
		driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
		try {
			if (locType.equalsIgnoreCase("id")) {
				Assert.assertTrue(driver.findElement(By.id(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + "is present in the web element.");
				 ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + "is present in the web element.");
			} else if (locType.equalsIgnoreCase("name")) {
				Assert.assertTrue(driver.findElement(By.name(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + "is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + "is present in the web element.");
			} else if (locType.equalsIgnoreCase("class")) {
				Assert.assertTrue(driver.findElement(By.className(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + "is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + "is present in the web element.");
			} else if (locType.equalsIgnoreCase("css")) {
				Assert.assertTrue(driver.findElement(By.cssSelector(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + "is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + "is present in the web element.");
			} else {
				Assert.assertTrue(driver.findElement(By.xpath(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + "is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + "is present in the web element.");
			}
		} catch (Exception e) {
			log("Text is not present in the web element.");
			ExtentReporter.log(LogStatus.FAIL, "Text is not present in the web element.");
			e.printStackTrace();	       
		}
	}
	
	public void assertCorrectTextPresent(String locator, String locType, String valueToCheck) throws Exception {
		driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
		try {
			if (locType.equalsIgnoreCase("id")) {
				AssertJUnit.assertTrue(driver.findElement(By.id(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + " is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + " is present in the web element.");
			} else if (locType.equalsIgnoreCase("name")) {
				AssertJUnit.assertTrue(driver.findElement(By.name(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + " is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + " is present in the web element.");			
			} else if (locType.equalsIgnoreCase("class")) {
				AssertJUnit.assertTrue(driver.findElement(By.className(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + " is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + " is present in the web element.");			
			} else if (locType.equalsIgnoreCase("css")) {
				AssertJUnit.assertTrue(driver.findElement(By.cssSelector(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + " is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + " is present in the web element.");			
			} else {
				AssertJUnit.assertTrue(driver.findElement(By.xpath(locator)).getText().equals(valueToCheck));
				log("The text " + valueToCheck + " is present in the web element.");
				ExtentReporter.log(LogStatus.PASS, "The text " + valueToCheck + " is present in the web element.");				
			}
		} catch (Exception e) {
			log("Text is not present in the web element.");
			ExtentReporter.log(LogStatus.FAIL, "The text " + valueToCheck + " is not present in the web element.");			
			e.printStackTrace();
		}
	}
	public static void assertElementPresentInPage(String locator, String locType, String ElementDescription) {
		BaseTest ff = new BaseTest();
		try {
			driver.manage().timeouts().implicitlyWait(100, TimeUnit.SECONDS);
			Assert.assertTrue(ff.isElementPresent(locator, locType));
			log("Element is present in the page.");
			ExtentReporter.log(LogStatus.PASS, ElementDescription+" is present in the page.");
		} catch (Exception e) {
			log("Element is not present in the page.");
			ExtentReporter.log(LogStatus.FAIL, ElementDescription+" is not present in the page.");
			e.printStackTrace();
		}
	}	
	public Boolean isElementPresent(String locator, String locType) {
		try {
			if (locType.equalsIgnoreCase("id")) {

				driver.findElement(By.id(locator));

			} else if (locType.equalsIgnoreCase("name")) {

				driver.findElement(By.name(locator));

			} else if (locType.equalsIgnoreCase("class")) {

				driver.findElement(By.className(locator));

			} else if (locType.equalsIgnoreCase("link")) {

				driver.findElement(By.linkText(locator));

			} else if (locType.equalsIgnoreCase("css")) {

				driver.findElement(By.cssSelector(locator));

			} else {

				driver.findElement(By.xpath(locator));
			}

		} catch (NoSuchElementException e) {
			return false;
		}

		return true;
	}
	public boolean checkRedirection(WebElement urlToBeCheck){
		urlToBeCheck.click();
		String URL = urlToBeCheck.getAttribute("href");
		String CurrentURL=driver.getCurrentUrl();
		System.out.println("Object URL is "+URL);
		System.out.println("Current URL is "+CurrentURL);
		if (URL.equalsIgnoreCase(CurrentURL)){
			System.out.println(CurrentURL+" is correct. Passed");
			ExtentReporter.log(LogStatus.PASS, "The URL " + CurrentURL + " is Correct.");
			return true;
		}
		else{
			System.out.println(CurrentURL+" is incorrect. Failed");
			ExtentReporter.log(LogStatus.FAIL, "The URL " + CurrentURL + " is Inorrect.");
			return false;
		}	
	}

	/*
	 *******************************************************************************************************************
	 * Function Name - objectClick Description - This function will click the
	 * object/element. Created By - justin.josef.d.bulan Created On - 11/15/2016
	 * Reviewed By - Reviewed On � Input Parameter - WebDriver, WebElement
	 * Output Parameter - None Expected Result - Object/Element will be clicked
	 * Note � (Mention about any notes/comments/prerequisites required to
	 * perform the test set execution.)
	 *******************************************************************************************************************
	 */
	public static void objectClick(WebElement var_object) {
		var_object.click();
	}
	public void clickAction(WebDriver driver, WebElement var_object) throws InterruptedException {
		Actions builder = new Actions(driver);
		WebElement manageElement = var_object;
		System.out.println("Manage My Site is set");
		Action mouseOverManageElement = builder.moveToElement(manageElement).build();
		mouseOverManageElement.perform();
		System.out.println("Manage My Site is hovered");
		Thread.sleep(5000);
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - waitForAcertainObject Description - This function will
	 * wait for a certain object to exist Created By - justin.josef.d.bulan
	 * Created On - 11/15/2016 Reviewed By - Reviewed On � Input Parameter -
	 * WebElement Output Parameter - None Expected Result - Code wont move to
	 * the next line if object is not yet present or during a certain time. Note
	 * � (Mention about any notes/comments/prerequisites required to perform the
	 * test set execution.)
	 *******************************************************************************************************************
	 */
	public void waitForAcertainObject(WebElement var_object) throws Exception {
		for (int i = 0; i < 5; i++) {
			try {
				var_object.isDisplayed();
				break;
			} catch (Exception e) {
				Thread.sleep(2000);
			}
		}
	}
	
	public void waitForAcertainObjectToBeGone(WebElement var_object) throws Exception {
		for (int i = 0; i < 5; i++) {
			try {
				var_object.isDisplayed();
			} catch (Exception e) {
				break;
			}
		}
	}
	//xpath string only
	public void waitForAcertainObject(String xpath) throws Exception {
		for (int i = 0; i < 60; i++) {
			try {
				driver.findElement(By.xpath(xpath)).isDisplayed();
				break;
			} catch (Exception e) {
				Thread.sleep(2000);
			}
		}
	}
	//xpath string only
	public void waitForAcertainObjectToBeGone(String xpath) throws Exception {
		for (int i = 0; i < 60; i++) {
			try {
				driver.findElement(By.xpath(xpath)).isDisplayed();
			} catch (Exception e) {
				break;
			}
		}
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - isElementDisplayed 
	 * Description - This function would check if an object exists Created By - justin.josef.d.bulan 
	 * Created On - 11/15/2016 
	 * Reviewed By - 
	 * Reviewed On � 
	 * Input Parameter - String , String, String
	 * Output Parameter - indicator Expected Result - still experimental Note �
	 * (Mention about any notes/comments/prerequisites required to perform the
	 * test set execution.)
	 *******************************************************************************************************************
	 */
	public void isElementDisplayed(String locator, String locType, String ElementDescription) throws InterruptedException{
		try{	
			driver.findElement(By.xpath(locator)).isDisplayed();
			ExtentReporter.log(LogStatus.PASS, ElementDescription+" is currently displayed in the page.");
			
		}catch (Exception e){
			ExtentReporter.log(LogStatus.FAIL, ElementDescription+" is not displayed in the page.");
		}	
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - elementShouldNotBeDisplayed
	 * Description - This function would check if an object exists Created By - justin.josef.d.bulan 
	 * Created On - 11/15/2016 
	 * Reviewed By - 
	 * Reviewed On � 
	 * Input Parameter - String , String, String
	 * Output Parameter - indicator Expected Result - still experimental Note �
	 * (Mention about any notes/comments/prerequisites required to perform the
	 * test set execution.)
	 *******************************************************************************************************************
	 */
	
	public void elementShouldNotBeDisplayed(String locator, String locType, String ElementDescription) throws InterruptedException{	
		try{
			driver.findElement(By.xpath(locator)).isDisplayed();
			ExtentReporter.log(LogStatus.FAIL, ElementDescription+" is currently displayed in the page.");
			
		}catch (Exception e){
			ExtentReporter.log(LogStatus.PASS, ElementDescription+" is not displayed in the page.");
		}		
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - logStep Description - This function would increment the
	 * logstepcounter and helps you track what scenario you are automating
	 * Created By - justin.josef.d.bulan Created On - 11/15/2016 Reviewed By -
	 * Reviewed On � Input Parameter - String Output Parameter - None Expected
	 * Result - logstepcounter increments and display the scenario name Note �
	 * (Mention about any notes/comments/prerequisites required to perform the
	 * test set execution.)
	 *******************************************************************************************************************
	 */
	public void logstep(String Desc){
		String FinalScenarioCounter = String.format("%03d",ScenarioCounter);
		TestScenario = "Test Scenario " + FinalScenarioCounter + ": ";
		ExtentReporter=extent.startTest(TestScenario+ Desc);
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - stopSelenium Description - This function would stop the
	 * current test Created By - justin.josef.d.bulan Created On - 11/15/2016
	 * Reviewed By - Reviewed On � Input Parameter - WebDriver Output Parameter
	 * - None Expected Result - WebDriver stops Note � (Mention about any
	 * notes/comments/prerequisites required to perform the test set execution.)
	 *******************************************************************************************************************
	 */
	public void stopSelenium(WebDriver driver) {
		driver.quit();
	}	
	/*
	 *******************************************************************************************************************
	 * Function Name - decodeStr Description - This function would decode
	 * encrypted string Created By - justin.josef.d.bulan Created On -
	 * 11/15/2016 Reviewed By - Reviewed On � Input Parameter - String Output
	 * Parameter - boolean Expected Result - decode encrypted string Note �
	 * (Mention about any notes/comments/prerequisites required to perform the
	 * test set execution.)
	 *******************************************************************************************************************
	 */
	// public String encodeStr(String str){
	// byte[] bytesEncoded = Base64.encodeBase64(str .getBytes());
	// return new String(bytesEncoded);
	// }
	//
	// public String decodeStr(String ecodedStr){
	// byte [] decoded = Base64.decodeBase64(ecodedStr);
	// return new String(decoded);
	// }

	/*
	 *******************************************************************************************************************
	 * Function Name - highlightObject Description - This function will
	 * highlight an object Created By - justin.josef.d.bulan Created On -
	 * 11/15/2016 Reviewed By - Reviewed On � Input Parameter - WebDriver,
	 * WebElement Output Parameter - none Expected Result - decode encrypted
	 * string Note � (Mention about any notes/comments/prerequisites required to
	 * perform the test set execution.)
	 *******************************************************************************************************************
	 */
	public static void highlightObject(WebElement element) throws InterruptedException {
		JavascriptExecutor js = (JavascriptExecutor) driver;// Creating
															// JavaScriptExecuter
															// Interface
		for (int iCnt = 0; iCnt < 2; iCnt++) {
			// Execute javascript
			js.executeScript("arguments[0].style.border='4px groove green'", element);
			Thread.sleep(1000);
			js.executeScript("arguments[0].style.border=''", element);
		}
	}
	public void highlightObject(String string) throws InterruptedException {
		WebElement element = driver.findElement(By.xpath(string));
		JavascriptExecutor js = (JavascriptExecutor) driver;// Creating
															// JavaScriptExecuter
															// Interface
		for (int iCnt = 0; iCnt < 2; iCnt++) {
			// Execute javascript
			js.executeScript("arguments[0].style.border='6px groove green'", element);
			js.executeScript("arguments[0].style.border=''", element);
		}
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - hoverObject Description - This function will hover the
	 * object specified Created By - justin.josef.d.bulan Created On -
	 * 11/15/2016 Reviewed By - Reviewed On � Input Parameter - WebDriver,
	 * WebElement Output Parameter - none Expected Result - move to the object
	 * Note � (Mention about any notes/comments/prerequisites required to
	 * perform the test set execution.)
	 *******************************************************************************************************************
	 */
	public void hoverObject(WebDriver driver, WebElement element) throws Exception {
		Actions builder = new Actions(driver);
		builder.moveToElement(element).perform();
		Thread.sleep(2000);
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - navigateToSite Description - Navigate a site Created By -
	 * justin.josef.d.bulan Created On - 11/15/2016 Reviewed By - Reviewed On �
	 * Input Parameter - WebDriver, String Output Parameter - none Expected
	 * Result - site navigated Note � (Mention about any
	 * notes/comments/prerequisites required to perform the test set execution.)
	 *******************************************************************************************************************
	 */
	public void navigateToSite(WebDriver driver, String URL) {
		driver.get(URL);
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - inputText Description - This function will input strings
	 * on textbox or inputbox Created By - justin.josef.d.bulan Created On -
	 * 11/15/2016 Reviewed By - Reviewed On � Input Parameter - WebElement,
	 * String Output Parameter - None Expected Result - String will be inputed
	 * to the textbox/inputbox Note � (Mention about any
	 * notes/comments/prerequisites required to perform the test set execution.)
	 *******************************************************************************************************************
	 */
	public void inputText(WebElement var_object, String stringToBeInputted) {
		var_object.sendKeys(stringToBeInputted);
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - checkIFObjecetIsEqual Description - check if object has
	 * the same value Created By - justin.josef.d.bulan Created On - 12/06/2016
	 * Reviewed By - Reviewed On � Input Parameter - WebDriver, String,
	 * WebElement Output Parameter - none Expected Result - Note � (Mention
	 * about any notes/comments/prerequisites required to perform the test set
	 * execution.)
	 *******************************************************************************************************************
	 */
	public void checkIFObjecetIsEqual(WebDriver driver, String strValue, WebElement object) {
		try {
			assertEquals("Lorem Ipsum", object.getText());
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
	}
	public static void navigateToASiteFromALink(WebElement urlToBeCheck) {
		String URL = urlToBeCheck.getAttribute("href");
		driver.get(URL);
	}
//	public boolean checkIfURLIsCorrect(WebElement urlToBeCheck, String ValueToBeChecked) {
//		String URL = urlToBeCheck.getAttribute("href");
//		urlToBeCheck.click();
//		String CurrentURlSample = driver.getCurrentUrl();
//		if (URL.equalsIgnoreCase(CurrentURlSample)) {
//			return true;
//		}
//		return false;
//	}	
	public static boolean checkIfURLIsCorrect(WebElement urlToBeCheck, String ValueToBeChecked){
		String URL = urlToBeCheck.getAttribute("href");
		System.out.println("Object URL is "+URL);
		if (URL.equalsIgnoreCase(ValueToBeChecked)){
			System.out.println("URL is correct. Passed");
			ExtentReporter.log(LogStatus.PASS, "The URL " + ValueToBeChecked + " is Correct.");		
			return true;
		}
		else{
			System.out.println("URL is incorrect. Failed");
			ExtentReporter.log(LogStatus.FAIL, "The URL " + ValueToBeChecked + " is Incorrect.");
			return false;
		}	
	}
	public static void mouseOverClick(String elementId, String locType) throws InterruptedException{
    	Actions builder = new Actions(driver);
    	WebElement object = findElement(elementId, locType);
    	Action MoveToAcertainObject = builder.moveToElement(object).click().build();
    	MoveToAcertainObject.perform();
    }
    public static void mouseOver(String elementId, String locType) throws InterruptedException{
    	Actions builder = new Actions(driver);
    	WebElement object = findElement(elementId, locType);
    	Action MoveToAcertainObject = builder.moveToElement(object).build();
    	MoveToAcertainObject.perform();
    }
    public void mouseClick(String elementId, String locType) throws InterruptedException{
    	Actions builder = new Actions(driver);
    	WebElement object = findElement(elementId, locType);
    	Action MoveToAcertainObject = builder.moveToElement(object).click().build();
    	MoveToAcertainObject.perform();
    }
    public void mouseMoveToObject(String elementId, String locType) throws AWTException{
    	WebElement object = findElement(elementId, locType);
    	Point coordinates = object.getLocation();
    	Robot robot = new Robot();
    	int x = coordinates.getX();
    	int y = coordinates.getY();
    	Dimension size = object.getSize();
    	int centerHeight = (size.getHeight())/2;
    	int CenterWidth = (size.getWidth())/2;
      //System.out.println("location x: "+x+" location y: "+y+ "  centerHeight: "+centerHeight +"  CenterWidth: "+CenterWidth); 
    	robot.mouseMove(x+CenterWidth,y+centerHeight+80);	
    }
    public static void manualLogin(){
    	String variable;
    	do{
    		variable = driver.getCurrentUrl();		
    	}
    	while(variable.contains("adfs") || variable.contains("login"));
    }
    
   public static void automaticLogin() {
	   ChromeOptions options = new ChromeOptions();
	   options.addArguments("user-data-dir=C:\\Users\\alyzza.marie.mendoza\\AppData\\Local\\Google\\Chrome\\User Data");
	   options.addArguments("profile-directory=Default");
	   
   }
    public void testIDLogIn(){
		WebElement inputEID = findElement("userNameInput","id");
		inputText(inputEID,"justin.josef.d.bulan");
		WebElement inputPW = findElement("passwordInput","id");
		inputText(inputPW,"********");
		click("submitButton","id");
		manualLogin();
	}   
    public static File test;
	public void findFile(String name,File file)
    {
        File[] list = file.listFiles();
        if(list!=null)
        for (File fil : list)
        {
            if (fil.isDirectory())
            {
                findFile(name,fil);
            }
            else if (name.equalsIgnoreCase(fil.getName()))
            {
            	test = fil.getParentFile();
//                System.out.println(fil.getParentFile());
            }
        }
    }
	public String filename(String className){
		String test = className+".java";
		return test;
		
	}
    public void getModuleName(String ModuleName2) 
    {
    	BaseTest ff = new BaseTest();
        ff.findFile(ff.filename(ModuleName2),new File(System.getProperty("user.dir")));
        String finalString = ""+test;
        String[] items= finalString.split("\\\\");
//        System.out.println(Arrays.toString(items));
        ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(items));
        int elementsize = arrayList.size();
        ModuleName = arrayList.get(elementsize-2);
        System.out.println(arrayList.get(elementsize-2));
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
    
    public void typeInRichTextEditor(String Locator, String valueToBeWritten) {

        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0,"+ driver.findElement(By.xpath(Locator)).getLocation().y + ")");
        WebElement descriptionElement = driver.findElement(By.xpath(Locator));
        driver.switchTo().frame(descriptionElement);
        WebElement editable = driver.switchTo().activeElement();
        editable.sendKeys(valueToBeWritten);
        driver.switchTo().defaultContent();
		driver.quit();
    }   
    public void focusToTHatElement(String Locator){
    	WebElement element = driver.findElement(By.xpath(Locator));
    	Actions actions = new Actions(driver);
    	actions.moveToElement(element);
    	actions.perform();
    }
	public void keyboardCommands(int Value) throws AWTException{
		Robot robot = new Robot();
		robot.keyPress(Value);
		robot.keyRelease(Value);
	}
    public void keyboardCommandsWithCombo(int Value, int Value2) throws AWTException{
    	Robot robot = new Robot();
    	robot.keyPress(Value);
    	robot.keyPress(Value2);
    	robot.keyRelease(Value);
		robot.keyRelease(Value2);  	
	}
	public void deleteCurrentString(String xpath, String Locator) throws InterruptedException, AWTException{
		click(xpath, Locator);
		keyboardCommandsWithCombo(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
		keyboardCommands(KeyEvent.VK_DELETE);
	}
	
	public void checkSliderFrame(){
		try{
			if(driver.findElement(By.xpath("//*[@class='acn-pplv2-onboarding-body']")).isDisplayed()){
				driver.findElement(By.xpath("//*[@class='acn-pplv2-onboarding-close-btn']")).click();		
			}
		}catch(NoSuchElementException e){
			System.out.println("No People Popup");
		}
	}
	
	public void doubleClick(String XpathValue){
		Actions action = new Actions(driver);
		action.moveToElement(driver.findElement(By.xpath(XpathValue))).doubleClick().build().perform();
	}


	/*
	 *******************************************************************************************************************
	 * Function Name - inputStringInEditor 
	 * Description - input of String in any field/file explorer/richtext
	 * the same value Created By - ranier.a.rivera Created On - 7/1/2017
	 * This function inputs any string into a visible field object
	 *******************************************************************************************************************
	 */
	public void inputStringInEditor(String element, String testData) {
		//WebElement field = driver.findElement(By.xpath(element));
		Actions action = new Actions(driver);
		action.sendKeys(testData).perform() ;
	}
	public String databaseConnection(String Sheet, String recordNumber, String whatColumn)throws FilloException {
		String WhatToReturn = null;
		Fillo fillo = new Fillo();
		Connection connection = fillo.getConnection(System.getProperty("user.dir") + "\\Files\\Database\\ExportExcel.xlsx");
		String strQuery = "Select "+whatColumn+" from " + Sheet + " where ID= " + recordNumber;
		Recordset recordset = connection.executeQuery(strQuery);
		while (recordset.next()) {
				WhatToReturn = recordset.getField(whatColumn);
		}
		recordset.close();
		connection.close();
		return WhatToReturn;
	}
	
	public void checkStringValue(String object, String valueToCompare){
		String valueToCheck;
		try{
			valueToCheck = driver.findElement(By.xpath(object)).getText();
			if (valueToCheck.isEmpty() || valueToCheck.equals("")){
				valueToCheck = driver.findElement(By.xpath(object)).getAttribute("aria-label");	
			}
			
			System.out.println(valueToCheck);
			if(valueToCheck.equals(valueToCompare) || valueToCheck.contains(valueToCompare)){
				ExtentReporter.log(LogStatus.PASS, "The text '"+valueToCompare+"' is correct");
			}else{
				ExtentReporter.log(LogStatus.FAIL, "The text '"+valueToCompare+"' is wrong");
			}
			
		}catch(Exception e){
			ExtentReporter.log(LogStatus.FAIL, "No object to be check");
			
		}
	}
	
	public void checkRedirectionAfterClickingALinkAndWillOpenANewTab(String objectToBeClick, String URLToBeCheck){
		try{
			driver.findElement(By.xpath(objectToBeClick)).click();
			ArrayList<String> tabs2 = new ArrayList<String> (driver.getWindowHandles());
		    driver.switchTo().window(tabs2.get(1));
		    String currentURL = driver.getCurrentUrl();
		    if (URLToBeCheck.contains(currentURL) ||URLToBeCheck.equals(currentURL)){
				ExtentReporter.log(LogStatus.PASS, "Current URL is "+URLToBeCheck);
			}
			else{
				ExtentReporter.log(LogStatus.FAIL, "Current URL is not "+URLToBeCheck);
			}
		    driver.close();
		    driver.switchTo().window(tabs2.get(0));
				
		}catch(Exception e){
			ExtentReporter.log(LogStatus.FAIL, "No object to be check");
		}
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - shuffleArrayForInt 
	 * Description - shuffle array
	 * the same value Created By - miguel.l.m.calalang Created On - 12/06/2016
	 * Reviewed By - Reviewed On � Input Parameter - int[]
	 * WebElement Output Parameter - none Expected Result - Note � (Mention
	 * about any notes/comments/prerequisites required to perform the test set
	 * execution.)
	 *******************************************************************************************************************
	 */
	public void shuffleArrayForInt(int[] ar){
	    Random rnd = ThreadLocalRandom.current();
	    for (int i = ar.length - 1; i > 0; i--){
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
	    }
	}
	
	public void clickSearchPicker() throws Exception {
		waitForAcertainObject("//*[@class='acn-pplv2-searchbar-result-item']");
		click("(//*[@class='acn-pplv2-searchbar-result-item-info'])[1]","xpath");
		
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - uploadFileString 
	 * Description - File Upload String
	 * the same value Created By - ranier.a.rivera Created On - 7/26/2017
	 * This function returns the path to be inputted in file explorer when uploading file
	 *******************************************************************************************************************
	 */
	public String uploadFileString(String FileDirectory, String FileName){
		String Path = System.getProperty("user.dir") + "\\Files\\"+FileDirectory+"\\" + FileName;
		return Path;
	} 
	
	
	
	public void setClipboardData(String string){
		StringSelection stringSelection = new StringSelection(string);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
	}
	


	public WebElement getWhenVisible(String locator, int timeout) {
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(timeout));
		element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
		return element;

	}
	
	
	public void clickWhenReady(String locator, int timeout) {
		WebElement element = null;
		WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(timeout));
		element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
		element.click();
	}
	
	public void checkIfOptionsAreCorrect(String[] optionsToBeCheck, String ObjectToBeCheck, String GroupName){
		List<WebElement> groupToBeCheck = new ArrayList<WebElement>();
		int finalCount = 0;
		groupToBeCheck = driver.findElements(By.xpath(ObjectToBeCheck));
		int initialCounter = groupToBeCheck.size();
		int counter = groupToBeCheck.size();
			for (int i=0;i<=counter;i++){
				for(int j=0;j<counter;i++){
				
				if (optionsToBeCheck[i].equals(groupToBeCheck.get(j).getAttribute("innerText"))){
					groupToBeCheck.remove(j);
					finalCount++;
				}
			}
			counter = groupToBeCheck.size();
		}
		if (finalCount==initialCounter){
			ExtentReporter.log(LogStatus.PASS, "Correct options are listed");
		}else{
			ExtentReporter.log(LogStatus.FAIL, "Incorrect options are listed");
		}
		
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - certainlyWaitForCertainObjectToAppear 
	 * Description - Wait for a certain object to appear forever
	 * the same value Created By miguel.l.m.calalang-  Created On - 8/22/2017
	 * This function waits forever until the element appears
	 *******************************************************************************************************************
	 */
	public void certainlyWaitForCertainObjectToAppear(String text) throws Exception {
		int x = 0;

		while (x == 0) {
			try {
				WebElement element = driver.findElement(By.xpath
						(text));
				if (element.isDisplayed()) {
					x = 1;
				}
				else{
					x = 0;
				}
			} catch (Exception e) {
				x = 0;
			}
		}

	}
	/*
	 *******************************************************************************************************************
	 * Function Name - certainlyWaitForCertainObjectToDisppear 
	 * Description - Wait for a certain object to disappear forever
	 * the same value Created By miguel.l.m.calalang-  Created On - 8/22/2017
	 * This function waits forever until the element disappears
	 *******************************************************************************************************************
	 */
	public void certainlyWaitForCertainObjectToDisppear(String text) throws Exception {
		int x = 0;
		while (x == 0) {
			try {
				WebElement element = driver.findElement(By.xpath
						(text));
				if (element.isDisplayed()) {
					x = 0;
				}
				else{
					x = 1;
				}
			} catch (Exception e) {
				x = 1;
			}
		}
	}
	/*
	 *******************************************************************************************************************
	 * Function Name - waitForCertainObjectToAppear 
	 * Description - Wait for a certain object to appear in certain time in seconds provided by the user.
	 * the same value
	 * Created By miguel.l.m.calalang-  Created On - 8/22/2017
	 * This function waits for an element to appear depending on the time given by the user
	 *******************************************************************************************************************
	 */
	public void waitForCertainObjectToAppear(String text, int max) throws Exception {
		int counter = 100;

		while (counter <= max) {
				if(counter<max){
				try {
					WebElement element = driver.findElement(By.xpath
							(text));
					if (element.isDisplayed()) {
						counter = max + 100;
					}
					else{
						counter = counter+100;
					}
				} catch (Exception e) {
						counter = counter+100;
				}
			}
		}

	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - waitForCertainObjectToDisppear 
	 * Description - Wait for a certain object to disappear in certain time in seconds provided by the user.
	 * the same value Created By miguel.l.m.calalang-  Created On - 8/22/2017
	 * This function waits for an element to disappear depending on the time given by the user
	 *******************************************************************************************************************
	 */
	public void waitForCertainObjectToDisppear(String text, int max) throws Exception {
		int counter = 100;
		while (counter <= max) {
			try {
				WebElement element = driver.findElement(By.xpath
						(text));
				if (element.isDisplayed()) {
					counter = counter+100;
				}
				else{
					counter = max + 100;
				}
			} catch (Exception e) {
				counter = max + 100;
			}
		}
		
	}
	
	
	/*
	 *******************************************************************************************************************
	 * Function Name - ScrollintoView 
	 * Description - This function will scroll into view.
	 * Created By - jhon.m.l.pestanas 
	 * Created On - 08/25/2017
	 * Reviewed By - justin.josef.d.bulan 
	 * Reviewed On - 08/25/2017
	 * Input Parameter - String, string
	 * Output Parameter - None 
	 * Expected Result - It will be scrolled
	 * Note: 
	 *******************************************************************************************************************
	 */
	
	public static void ScrollintoView(String string) {
		JavascriptExecutor je = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		je.executeScript("arguments[0].scrollIntoView(true);",element);
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - waitForCertainObjectToAppearWithMsg 
	 * Description - Wait for a certain object to appear in certain time in seconds provided by the user.
	 * the same value Created By miguel.l.m.calalang-  Created On - 9/6/2017
	 * This function waits for an element to appear depending on the time given by the user and fails if it does not appear
	 *******************************************************************************************************************
	 */
	public void waitForCertainObjectToAppearWithMsg(String text, int max) throws Exception {
		int counter = 100;
		int x = 0;

		while (counter <= max) {
			try {
				WebElement element = driver.findElement(By.xpath
						(text));
				if (element.isDisplayed()) {
					counter = max + 100;
					x = 1;
				}
				else{
					counter = counter+100;
					x = 0;
				}
			} catch (Exception e) {
					counter = counter+100;
			}
		}
		
		if(x==1){
			ExtentReporter.log(LogStatus.PASS, "Element is displayed within the time range.");
		}
		else{
			ExtentReporter.log(LogStatus.FAIL, "Element is not displayed within the time range.");
		}

	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - waitForCertainObjectToDisppearWithMsg 
	 * Description - Wait for a certain object to disappear in certain time in seconds provided by the user.
	 * the same value Created By miguel.l.m.calalang-  Created On - 9/6/2017
	 * This function waits for an element to disappear depending on the time given by the user and fails if it does not appear
	 *******************************************************************************************************************
	 */
	public void waitForCertainObjectToDisppearWithMsg(String text, int max) throws Exception {
		int counter = 100;
		int x=0;
		
		while (counter <= max) {
			try {
				WebElement element = driver.findElement(By.xpath
						(text));
				if (element.isDisplayed()) {
					counter = counter+100;
					x=0;
				}
				else{
					counter = max + 100;
					x = 1;
				}
			} catch (Exception e) {
				counter = max + 100;
				x = 1;
			}
		}
		
		if(x==1){
			ExtentReporter.log(LogStatus.PASS, "Element disappeared within the time range.");
		}
		else{
			ExtentReporter.log(LogStatus.FAIL, "Element did not disappear within the time range.");
		}
		
	}

	public void waitForElement( String element, int timeout) {
		WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofMillis(timeout));
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.xpath(element))));
        
    }
	
	
	/*
	 *******************************************************************************************************************
	 * Function Name - getExcelData 
	 * Description - Get data on specific sheet number, row number and column number
	 * the same value Created By - miguel.l.m.calalang Created On - 09/29/2017
	 * Get data from excel
	 *******************************************************************************************************************
	 */
	
	  public String getExcelData(int sheetNum, int rowNum, int columnNum, String DatabaseName) throws Exception{

			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			FileInputStream fis = new FileInputStream
					(s+"\\Files\\Database\\"+DatabaseName+".xlsx");
//			System.out.println("Logged User: "+loggedIn);
			
			XSSFWorkbook workbook = new XSSFWorkbook(fis);
			
			XSSFSheet sheet = workbook.getSheetAt(sheetNum);
	                    //I have added test data in the cell A1 as "SoftwareTestingMaterial.com"
	                    //Cell A1 = row 0 and column 0. It reads first row as 0 and Column A as 0.
			Row row = sheet.getRow(rowNum);
			Cell cell = row.getCell(columnNum);
	        
//			System.out.println(cell);
//			System.out.println(sheet.getRow(0).getCell(2));
			
			String cellval = cell.getStringCellValue();
//			System.out.println(cellval);
			return cellval;
		}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - countRows 
	 * Description - Count the number of rows with data for the sheet number provided
	 * the same value Created By - miguel.l.m.calalang Created On - 09/29/2017
	 * Count the number of rows then return the data
	 *******************************************************************************************************************
	 */
	  
	public int countRows(int sheetNum, String DatabaseName) throws Exception{
		int noOfColumns = 0;
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		FileInputStream fis = new FileInputStream
				(s+"\\Files\\Database\\"+DatabaseName+".xlsx");
		XSSFWorkbook workbook = new XSSFWorkbook(fis);
		XSSFSheet sheet = workbook.getSheetAt(sheetNum);
                    //I have added test data in the cell A1 as "SoftwareTestingMaterial.com"
                    //Cell A1 = row 0 and column 0. It reads first row as 0 and Column A as 0.
		noOfColumns = sheet.getLastRowNum();
        
//		System.out.println("Number of columns: "+noOfColumns);
		
		return noOfColumns;
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - waitForLoad 
	 * Description - Waits for the page to load
	 * the same value Created By - miguel.l.m.calalang Created On - 04/17/2018
	 * Waits for the page to load
	 *******************************************************************************************************************
	 */
	public void waitForLoad(WebDriver driver) {
	    ExpectedCondition<Boolean> pageLoadCondition = new ExpectedCondition<Boolean>() {
	        public Boolean apply(WebDriver driver) {
	            return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
	        }
	    };
		WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(180));
	    wait.until(pageLoadCondition);
	}

	
	/*
	 *******************************************************************************************************************
	 * Function Name - hideGDPR
	 * Description - hides footer
	 * the same value Created By - miguel.l.m.calalang Created On - 04/17/2018
	 * Waits for the page to load
	 *******************************************************************************************************************
	 */
	
	public void hideGdpr() throws Exception{
		Thread.sleep(1500);
		
		try{
			if(driver.findElement(By.xpath("(//*[@class='acn-pplv2-gdpr']//*[@data-toggle='collapse'])[2]")).isDisplayed()){
				driver.findElement(By.xpath("(//*[@class='acn-pplv2-gdpr']//*[@data-toggle='collapse'])[2]")).click();		
			}
		}catch(NoSuchElementException e){
			System.out.println("Hidden already");
		}
	}
	
	public void hideToast() throws Exception{
		Thread.sleep(1500);
		
		try{
			if(driver.findElement(By.xpath("//*[@id='privacyToast']")).isDisplayed()){
				driver.findElement(By.xpath("//*[@id='closeToast']")).click();		
			}
		}catch(NoSuchElementException e){
			System.out.println("Hidden already");
		}
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - scrollToElement
	 * Description - Scroll to element with bottom padding to avoid gdpr
	 * the same value Created By - miguel.l.m.calalang Created On - 08/15/2018
	 * Waits for the page to load
	 *******************************************************************************************************************
	 */
	public void scrollToElement(String string){
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		int xAxis = element.getLocation().getX();
		int yAxis = element.getLocation().getY();
	    js.executeScript("javascript:window.scrollTo("+xAxis+","+yAxis+")");
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - scrollToElementPlusScrollUp
	 * Description - Scroll to element then scroll up by 20px
	 * the same value Created By - miguel.l.m.calalang Created On - 08/15/2018
	 * Waits for the page to load
	 *******************************************************************************************************************
	 */
	public void scrollToElementPlusScrollUp(String string){
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		js.executeScript("arguments[0].scrollIntoView(true);",element);
		js.executeScript("javascript:window.scrollBy(0,200)");
	}
	
	public void scrollToElementPlusScrollUpSpecifypx(String string, String pixel){
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		js.executeScript("arguments[0].scrollIntoView(true);",element);
		js.executeScript("javascript:window.scrollBy(0,"+pixel+")");
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - scrollToElementPlusScrollDown
	 * Description - Scroll to element then scroll down by 200px
	 * the same value Created By - miguel.l.m.calalang Created On - 08/15/2018
	 * Waits for the page to load
	 *******************************************************************************************************************
	 */
	
	public static void scrollToElementPlusScrollDown(String string){
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		js.executeScript("arguments[0].scrollIntoView(true);",element);
		js.executeScript("javascript:window.scrollBy(0,-200)");
	}
	
	public void scrollToElementPlusScrollDownSpecifypx(String string, String pixel) throws Exception{
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		js.executeScript("arguments[0].scrollIntoView(true);",element);
		Thread.sleep(2000);
		js.executeScript("javascript:window.scrollBy(0,-"+pixel+")");
	}
	
	public void scrollByFunction(String string){
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("javascript:window.scrollBy(0,"+string+")");
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - 
	 * Description - 
	 * the same value Created By - miguel.l.m.calalang Created On - 08/15/2018
	 * Waits for the page to load
	 *******************************************************************************************************************
	 */
	
	public void slowEnterText(String xpath, String text) throws Exception{
		WebElement element = driver.findElement(By.xpath
				(xpath));
		char[] perChar = text.toCharArray();
		for(int i = 0; i<perChar.length; i++) {
//			System.out.println(perChar[i]);
			char getCurrentChar = perChar[i];
			String toSend = new StringBuilder().append(getCurrentChar).toString();
			element.sendKeys(toSend);
			Thread.sleep(500);
		}
	}
	
	
	/*
	 *******************************************************************************************************************
	 * Function Name - Scrolling to specific component
	 * Description - Need to input xpath of the specific component
	 * the same value Created By - jhon.m.l.pestanas Created On - 09/17/2019
	 * Scroll to a specific component
	 *******************************************************************************************************************
	 */
	
	
	public void scrollToViewElement(String string) throws Exception {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(string));
		Thread.sleep(2000);
		int xAxis = element.getLocation().getX();
		int xHeight = element.getSize().height;
		System.out.println("x value "+xAxis);
		int yAxis = element.getLocation().getY();
		int yWidth = element.getSize().width;
		System.out.println("y value "+yAxis);
		
		Thread.sleep(5000);
	    js.executeScript("javascript:window.scrollTo("+xAxis+",("+yAxis+")"+(-105)+")");
	}
	
	/*
	 *******************************************************************************************************************
	 * Function Name - Real time scrolling
	 * Description - Need to input targetXPath, markerElement and scrollSpeed
	 * Created by - marjorie.s.kho 
	 * Created On - 06/23/2020
	 * Scroll to a specific component, mimicking the usual user's action.
	 *******************************************************************************************************************
	 */
	
	public void realTimeScrolling(String targetXPath, String markerElement, int scrollSpeed) throws Exception{
		//RECOMMENDED SCROLLING SPEED: 10-15
		//Target Xpath: the element you want to scroll to
		//Marker Element: the position of element in your current viewport where you want the targeted element to be at
		
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.xpath(targetXPath));
		int yAxis = element.getLocation().getY();
//		System.out.println("yaxis: " + yAxis);
		Dimension windowSize = driver.manage().window().getSize();
		int windowHeight = windowSize.getHeight();
//		System.out.println("Window height: " + windowHeight);
		WebElement element2 = driver.findElement(By.xpath(markerElement));
		int marker = element2.getLocation().getY();
//		System.out.println("Marker: " + marker);
		
		if (yAxis > marker) {
			int contentAllowance = (int) (yAxis + ((yAxis - marker) / windowHeight * 40));
			
			for(int i = marker; i < contentAllowance; i += scrollSpeed) {
		        js.executeScript("window.scrollBy(0," + scrollSpeed + ")", "");
//		        System.out.println("Scrolling..." + "\t Marker pos: " + i + "\t Element pos: " + contentAllowance);
		    }
		}else {
			scrollSpeed = -scrollSpeed;
			int markerAllowance = (int) (marker - ((marker - yAxis) / windowHeight * 20));
			
			for(int i = markerAllowance; i > yAxis; i += scrollSpeed) {
		        js.executeScript("window.scrollBy(0," + scrollSpeed + ")", "");
//		        System.out.println("Scrolling..." + "\t Marker pos: " + i + "\t Element pos: " + yAxis);
		    }
		}		
		
		highlightObject(element);	
		Thread.sleep(2000);		
	}
	
	public void testIdAzureLogin(String email, String pass) throws Exception{
		WebElement msEmail = driver.findElement(By.xpath
				("//*[@name='loginfmt']"));
		msEmail.sendKeys(email);
		
		WebElement nextBtn = driver.findElement(By.xpath
				("//*[@id='idSIButton9']"));
		nextBtn.click();
		
		Thread.sleep(3000);
		
		WebElement password = driver.findElement(By.xpath
				("//*[@name='passwd']"));
		password.sendKeys(pass);
		
		WebElement signInBtn = driver.findElement(By.xpath
				("//*[contains(@id,'idSIButton')]"));
		signInBtn.click();
		
		Thread.sleep(2000);
		
		WebElement noBtn = driver.findElement(By.xpath
				("//*[@id='idBtn_Back']"));
		noBtn.click();
	}
	
	public void closeModal() throws Exception{
		try {
			Thread.sleep(2000);
			WebElement closeModal = driver.findElement(By.xpath
					("//*[@class='acn-modal-del-icon']"));
			closeModal.click();
		}
		catch (Exception e) {
			
		}
	}
	
	public void closeNf() throws Exception{
		// 3rd item
		try {
			WebElement thiNf = driver.findElement(By.xpath
					("(//*[@class='acn-alert-close-icon'])[3]"));
			thiNf.click();
			
			
		}
		catch(Exception e) {
			
		}
		// 2nd item
		try {
			WebElement secNf = driver.findElement(By.xpath
					("(//*[@class='acn-alert-close-icon'])[2]"));
			secNf.click();
		}
		catch(Exception e) {
			
		}
		// 1st item
		try {
			WebElement firstNf = driver.findElement(By.xpath
					("(//*[@class='acn-alert-close-icon'])[1]"));
			firstNf.click();
		}
		catch(Exception e) {
			
		}
	}
}