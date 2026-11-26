package com.facebookclone;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Browser-level regression checks for the local FaceConnect demo. */
public class FaceConnectSeleniumTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeClass
    public void startBrowser() {
        baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            options.addArguments("--headless=new", "--window-size=1440,1000");
        }
        options.addArguments("--disable-notifications");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @BeforeMethod
    public void openCleanLoginPage() {
        driver.get(baseUrl);
        ((JavascriptExecutor) driver).executeScript("localStorage.clear();");
        driver.navigate().refresh();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginForm")));
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void loginPageShowsRequiredControls() {
        Assert.assertTrue(driver.findElement(By.id("loginUser")).isDisplayed());
        Assert.assertTrue(driver.findElement(By.id("loginPassword")).isDisplayed());
        Assert.assertTrue(driver.findElement(By.id("forgotPassword")).isDisplayed());
    }

    @Test
    public void wrongPasswordShowsClearError() {
        login("gagan", "incorrect-password");
        Assert.assertEquals(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("authMessage"))).getText(),
                "Invalid password. Please try again.");
    }

    @Test
    public void validCredentialsOpenFeed() {
        login("gagan", "gagan");
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("appView"))).isDisplayed());
        Assert.assertTrue(driver.findElement(By.id("openComposer")).isDisplayed());
    }

    @Test
    public void passwordResetRejectsWeakPassword() {
        driver.findElement(By.id("forgotPassword")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resetModal")));
        driver.findElement(By.id("resetUser")).sendKeys("gagan");
        driver.findElement(By.id("resetPassword")).sendKeys("weak");
        driver.findElement(By.cssSelector("#resetForm button.primary")).click();
        Assert.assertEquals(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resetMessage"))).getText(),
                "Use 6+ characters with a letter, number and symbol.");
    }

    @Test
    public void newTextPostRemainsAfterRefresh() {
        login("gagan", "gagan");
        String post = "Selenium post " + System.currentTimeMillis();
        createPost(post, null);
        Assert.assertTrue(driver.getPageSource().contains(post));
        driver.navigate().refresh();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("appView")));
        Assert.assertTrue(driver.getPageSource().contains(post), "Post should remain after browser refresh.");
    }

    @Test
    public void pastedPhotoUrlAppearsInFeed() {
        login("gagan", "gagan");
        String photoUrl = "https://example.com/selenium-photo.jpg";
        createPost("Selenium photo post", photoUrl);
        WebElement photo = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[src='" + photoUrl + "']")));
        Assert.assertEquals(photo.getAttribute("src"), photoUrl,
                "The posted photo URL should be rendered in the feed.");
    }

    @Test
    public void friendsNavigationAndAccountMenuOpen() {
        login("gagan", "gagan");
        driver.findElement(By.id("navFriends")).click();
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("secondaryView"))).getText()
                .contains("People you may know"));
        driver.findElement(By.id("logoutButton")).click();
        Assert.assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("accountPanel"))).getText()
                .contains("Log out"));
    }

    private void login(String username, String password) {
        driver.findElement(By.id("loginUser")).sendKeys(username);
        driver.findElement(By.id("loginPassword")).sendKeys(password);
        driver.findElement(By.cssSelector("#loginForm button[type='submit']")).click();
    }

    private void createPost(String text, String photoUrl) {
        driver.findElement(By.id("openComposer")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("postModal")));
        driver.findElement(By.id("postText")).sendKeys(text);
        if (photoUrl != null) {
            driver.findElement(By.id("mediaUrl")).sendKeys(photoUrl);
        }
        driver.findElement(By.cssSelector("#postForm button.primary")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("postModal")));
    }
}
