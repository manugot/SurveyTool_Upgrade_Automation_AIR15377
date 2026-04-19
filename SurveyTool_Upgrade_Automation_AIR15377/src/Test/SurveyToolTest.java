
package Test;

import org.testng.annotations.Test;
import Common.TestData;
import PageObject.SurveyToolObjects;
import Utilities.SurveyToolMethods;

public class SurveyToolTest extends SurveyToolMethods {
    SurveyToolObjects obj;
    
 //  String browserUsed = TestData.browserDev;
 //  String msBrowserUsed = TestData.msBrowserDev;
    
     String browserUsed = TestData.browserStaging;
     String msBrowserUsed = TestData.msBrowserStaging;
    
//    String browserUsed = TestData.browserProd;
//    String msBrowserUsed = TestData.msBrowserProd;

    public SurveyToolTest() throws Exception {
        getModuleName(this.getClass().getSimpleName());
        startTest(browserUsed);
        openDriver(browserUsed);
        obj = new SurveyToolObjects(driver);
    }

    @Test(priority = 0)
    public void verifyLogin() throws Exception {
        logstep("Verify Homepage");
        manualLogin();
		verifyHeader();
		verifyHomePageContent();
//		verifyConfig();
		verifyFooter();
		privacyPolicy();
		cookiePolicy();
//		termsOfUse();
//		legalNotice();
//		dataPrivacy();
		verifyHomepage();
    
    
    
}
   

    @Test(priority = 1)
     public void verifyCreatePage() throws Exception {
        logstep("Verify Create Survey Page Objects");
        obj.clickCreateSurveyTile();
        verifyCreateObjs();
        createSurveyCharacterLimit();
        createEmptyTitle();
        createValidateXSS();
	 	obj.clickAccentureLogo();
	 	obj.clickCreateSurveyTile();
	 	createValidateDoubleByteCharacters();
	 	obj.clickSettingsTab();
	 	obj.clickTextElementsTab();
	 	VerifyDoubleByteCharacters();
	 	obj.clickAccentureLogo();
	 	obj.clickCreateSurveyTile();
	 	createSurvey();
     } 

   @Test(priority = 2)
     public void verifyImportPage() throws Exception {
    	 logstep("Verify Import Survey Page Objects");
    	 obj.clickAccentureLogo();
    	 obj.clickCreateSurveyTile();
    	 obj.clickImportSurveyTab();
    	 Thread.sleep(2000);
   	 	 verifyImportObjs();
    	 importSurveyFunctionality();
    	 obj.clickAccentureLogo();
    	 obj.clickCreateSurveyTile();
    	 obj.clickImportSurveyTab();
    	 Thread.sleep(2000);
    	 importNoFileSelectedFunc();
/*    	 importInvalidFileFunc();
    	 obj.clickCreateSurveyTile(); 
    	 obj.clickImportSurveyTab();
    	 importExceedFileSizeFunc(); */
     } 

   @Test(priority = 3)
    public void verifyCopyPage() throws Exception {
    	 logstep("Verify Copy Survey Page Objects");
    	 obj.clickAccentureLogo();
    	 obj.clickCreateSurveyTile();
    	 obj.clickCopySurveyTab();
    	 verifyCopyObjs();
    	 copyNoSelectedSurvey();
    	 copyEmptySurveyTitle();
    	 copySurveyCharacterLimit();
    	 copySurveyFunctionality();
     } 

    @Test(priority = 4)
    public void verifyManagePage() throws Exception {
    	logstep("Verify Manage Survey Page Objects - Search");
    	obj.clickAccentureLogo();
    	obj.clickManageTile();
    	obj.click1stSurvey();
    	verifySurvey();
    	obj.clickAccentureLogo();
    	obj.clickManageTile();
    	verifySurveyList();
    	specialCharacSearch();
    	searchSurveyID();
    	searchSurveyTitle();
    	searchSurveyOwner();
    	searchNumeric();
    	searchAlphaNumeric();
    	searchScriptTag();
    	resetSearch();
    	statusActiveSearch();
    	statusActiveRunningSearch();
    	statusInactiveSearch();
    	statusExpiredSearch();
     	statusNotStartedSearch();
    	resetSearch();
  // 	groupSearch(); 
  //  	customGroupSearch();        
    }   

   @Test(priority = 5)
    public void verifySurveyGroupPage() throws Exception {
    	logstep("Verify Survey Group Details Page Objects");
    	obj.clickSurveyGroupTab();
    	validateSurveyGroup();
    	
    } 

 @Test(priority = 6)
    public void verifyOverviewPage() throws Exception {
    	logstep("Verify Survey Details Page Objects");
    	obj.clickAccentureLogo();
    	obj.clickCreateSurveyTile();
    	createSurvey();
    	obj.clickSettingsTab();
    	obj.clickOverviewTab();
    	verifyOverview();
    	
    } 

   @Test(priority = 7)
    public void verifyGeneralSettingMultipleLanguage() throws Exception {
    	logstep("Verify General Settings Page Objects for Multiple Languages");
    	obj.clickGeneralSettingsTab();
    	verifyGeneralSettings();
    	addNewLanguage(); 
    	obj.clickOverviewTab();
    	verifyLanguagetoOverviewPage(); 
    	obj.clickTextElementsTab();
    	verifyLanguagetoTextElements(); 
    	obj.clickPrivacyPolicyTab();
    	verifyLanguagetoPrivacyPolicy(); 
    	obj.clickStructureTab();
    	verifyLanguagetoQuestionGroup();
    	verifyLanguagetoQuestion();
    	verifyLanguagetoQuestionEllipsis();
    	obj.clickSettingsTab();
    	obj.clickOverviewTab();
    	verifyLanguagetoPreviewSurveyButton();
    	verifyLanguagetoSurveyQuestionnaire();
    	obj.clickGeneralSettingsTab();
    	deleteLanguage();
     } 

/*    @Test(priority = 8)
    public void verifyGeneralSettingsUpdateOwner() throws Exception {
    	logstep("Verify General Settings Page Objects for Owner Update");
    	Thread.sleep(5000);
    	nonExistentUser();
    	verifyEmptyOwner();
    	verifyErrorEidNum();
    	verifyErrorEidDoubleByte();
    	verifyErrorEidSpecialChars();
    	verifyErrorEidCascadingPeriod();
    	verifyErrorEidCascadingDash();
    	verifyCharacterLimit();
    	verifyTop5results();
    	updateOwner(); 	
    	obj.clickOverviewTab();
    	verifyOwnerOverview();
    	obj.clickSurveyListHeader();
    	verifyOwnerSurveyList();
    	obj.clickOtherSurvey(); // Note: Locate Survey List with Survey owner is enable before running automation.
    	obj.clickGeneralSettingsTab();
    	updateUsingOwnEid(); 
    } */

/* 	@Test(priority = 9)
    	public void verifyGeneralSettingsCustomGroup() throws Exception {
    	logstep("Verify General Settings Page Objects for Custom Group");
    	Thread.sleep(5000);
    	verifyDefaultSurveygroup();
    	setCustomGroup();
    	obj.clickSurveyListHeader();
    	verifyCustomSGSurveylist();
    } */

 	@Test(priority = 10)
     public void verifyTextElementsPage() throws Exception {
         logstep("Verify Text Elements Page Objects");
	 	 obj.clickAccentureLogo();
         obj.clickCreateSurveyTile();
         createSurvey();
         obj.clickSettingsTab();
         obj.clickTextElementsTab();
         verifyTextElements();
         titleDoubleByteCharacters();
         titleXSSCharacters();
         titleCharacterLimit();
         updateSurveyTitle();
         aliasCharacterLimit();
         updateSurveyAlias(); //Note: make sure the alias value is unique before running automation
         aliasValidateDoubleByteCharacter();
         aliasValidateXSSCharacters();
         updateTextElementsCKfields();
         updateEndURL();
         URLDdescCharacterLimit();
         updateURLDescription();
         obj.clickOverviewTab();
         Thread.sleep(2000);
         verifyTextElementsUpdatestoOverview();
         verifyUpdatestoPreviewSurvey();
         obj.clickSurveyListHeader();
         Thread.sleep(3000);
         verifySurveyTitleUpdatetoSurveyList();
     } 

     @Test(priority = 11)
     public void verifyPrivacyPolicyPage() throws Exception {
    	 logstep("Verify Privacy Policy Page Objects");
    	 obj.clickAccentureLogo();
    	 obj.clickManageTile();
    	 obj.click1stSurvey();
    	 obj.clickPrivacyPolicyTab();
    	 String dpMsg = "Sample data privacy message";
    	 String dpError = "Sample data privacy error message";
    	 verifyPrivacyPolicy();
//    	 privacyCharacterLimit(); //Note: This line action temporary comment due to there is existing issue.
    	 privacyDoubleByteCharacters();
//    	 privacyXSSCharacters(); //Note: This line action temporary comment due to there is existing issue.
    	 updatePrivacyPolicy(dpMsg, dpError);
    } 

     @Test(priority = 12)
    	public void verifyThemeOptionsPage() throws Exception {
    	logstep("Verify Theme Options Page Objects");
    		obj.clickThemeOptionsTab();
    		verifyThemeOptions();
    		verifyThemeOptions2();
    		verifyThemeOptions3();
    		verifyThemeOptions4();
    } 

    @Test(priority = 13)
    public void verifyPresentationPage() throws Exception {
    	logstep("Verify Presentation Page Objects");
    	obj.clickPresentationTab();
    	verifyPresentation();
    } 

    @Test(priority = 14)
    public void verifyParticipantSettingsPage() throws Exception {
    	logstep("Verify Participant Settings Page Objects");
    	obj.clickParticipantSettingsTab();
    	verifyParticipantSettings();
    }
 
    @Test(priority = 15)
    public void verifyNotificationsPage() throws Exception {
    	logstep("Verify Notifications Page Objects");
    	obj.clickNotificationsTab();
    	verifyNotifications();
    } 

/*    @Test(priority = 16)
    public void verifyPublicationPage() throws Exception {
    	logstep("Verify Publication Page Objects");
    	obj.clickPublicationTab();
    	verifyPublications();
    	registerToMySurveys(msBrowserUsed);
    	publishAccessObj();
    	activateSurvey();
    } 

    @Test(priority = 17)
    public void verifyQuestionGroup() throws Exception {
    	logstep("Verify Question Group Page Objects");
    	verifyQuestionGroupObjects();
    	addNewQuestionGroupViaSavebutton();
    	veifyEditQuestionGroupObjects();
    	editQuestionGroupviaSaveAndClose();
    	editQuestionGroupviaSaveBtn();
    	editQuestionGroupviaCloseBtn();
    	deleteQuestionGroup();
    	obj.clickStructureTab();
    	addNewQuestionGroupViaSaveAndAddQuestion();
    	addNewQuestionGroupViaSaveAndAddGroup();
    } 
    
    @Test (priority = 18)
    public void verifySurveyParticipants() throws Exception {
    	logstep("Verify Survey Participants Page Objects");
    	obj.clickSurveyParticipantsTab();
    	verifyswitchClosedAccessMode();
    	obj.clickAdddropdown();
    	obj.clickCSVFile();
    	verifyImportSurveyParticipantsCSvFileObjs();
    	importInvalidEmail();
    	importBulkSurveyParticipantsCSVFile();
    	importDuplicateSurveyParticipants();
    	importunsupportedfile();
    	
    	
    } */
    
}
