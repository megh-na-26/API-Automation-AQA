package com.aqa.api.tests;

import com.aqa.api.client.APIClient;
import com.aqa.api.config.APIConfig;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.lang.reflect.Method;

/**
 * BaseAPITest
 * -----------
 * TestNG lifecycle base class for all API tests.
 * Manages:
 *   - Session-scoped APIClient (single HTTP connection pool for the run)
 *   - ExtentReports node creation and result logging
 */
public abstract class BaseAPITest {

    protected static final Logger log = LoggerFactory.getLogger(BaseAPITest.class);

    protected APIClient client;

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        new File(APIConfig.REPORTS_DIR).mkdirs();
        ExtentSparkReporter spark = new ExtentSparkReporter(
                APIConfig.REPORTS_DIR + "/APITestReport.html");
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("AQA API Test Report");
        spark.config().setReportName("Open-Meteo API Tests");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Framework", "Java 17 + REST Assured + TestNG");
        extent.setSystemInfo("API",       "Open-Meteo Forecast API");
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        client = new APIClient();
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        ExtentTest test = extent.createTest(method.getName());
        testThread.set(test);
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        ExtentTest test = testThread.get();
        if (result.isSuccess()) {
            test.pass("PASSED");
        } else if (result.getStatus() == ITestResult.FAILURE) {
            test.fail(result.getThrowable());
        } else {
            test.skip("SKIPPED");
        }
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        if (extent != null) extent.flush();
    }

    protected static ExtentTest getTest() {
        return testThread.get();
    }
}
