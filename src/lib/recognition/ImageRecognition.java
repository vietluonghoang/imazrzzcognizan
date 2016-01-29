package lib.recognition;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.opencv.core.Point;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.touch.TouchActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;

public class ImageRecognition {

    public String rotation;
    public static int counter;
    public Logger logger = LoggerFactory.getLogger(ImageRecognition.class);
    public AppiumDriver<WebElement> driver;
    public String screenshotsFolder = "pattern";
    public static final int SHORT_SLEEP = 1;
    public static final int LONG_SLEEP = 10;
    public static String automationName;

    public ImageRecognition(AppiumDriver<WebElement> driver) {
        setDriver(driver);
        if (Util.readFileYML().get("backend") != null) {
            automationName = WordUtils.capitalizeFully(Util.readFileYML().get("backend").toString());
        }
    }

    public void setDriver(AppiumDriver<WebElement> driver) {
        this.driver = driver;
    }

    public void log(String message) {
        logger.info(message);
    }

    public void sleep(int seconds) throws Exception {
        Thread.sleep(seconds * 1000);
    }

    public String getScreenshotsCounter() {
        if (counter < 10) {
            return "0" + counter;
        } else {
            return String.valueOf(counter);
        }
    }

    public void takeScreenshot(String screenshotName) throws Exception {
        counter = counter + 1;
        String device = Util.readFileYML().get("udid").toString();
        String fullFileName = device + "/" + getScreenshotsCounter() + "_" + screenshotName;
        try {
            File imgFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(imgFile, new File("screenshot/" + fullFileName + ".png"));
            logger.debug("ImageRecognition::takeScreenShot: " + fullFileName + ".png saved to screenshot folder");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Point[] findImage(String image, String scene, int score) {
        String deviceName = Util.readFileYML().get("udid").toString();
        log("Image to find: queryimages/" + screenshotsFolder + "/" + image + " in " + "screenshot/"
                + Util.readFileYML().get("udid").toString() + "/" + getScreenshotsCounter() + "_" + scene);
        AkazeFinder imageFinder = new AkazeFinder(driver);

        Point[] imgRect = new Point[0];
        try {
            imgRect = imageFinder.findImage("queryimages/" + screenshotsFolder + "/" + image,
                    "screenshot/" + deviceName + "/" + getScreenshotsCounter() + "_" + scene, score);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (imgRect != null) {
            return imgRect;
        } else {
            return null;
        }
    }

    public Point[] findImage(String image, String scene, String setRotation) {
        String deviceName = Util.readFileYML().get("udid").toString();
        log("Image to find: queryimages/" + screenshotsFolder + "/" + image + " in " + "screenshot/"
                + Util.readFileYML().get("udid").toString() + "/" + getScreenshotsCounter() + "_" + scene);
        AkazeFinder imageFinder = new AkazeFinder(driver);

        Point[] imgRect = new Point[0];
        try {
            imgRect = imageFinder.findImage("queryimages/" + screenshotsFolder + "/" + image,
                    "screenshot/" + deviceName + "/" + getScreenshotsCounter() + "_" + scene, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (imgRect != null) {
            return imgRect;
        } else {
            return null;
        }
    }

    public Point[] findImage(String image, String scene) {
        return findImage(image, scene, rotation);
    }

    public Point[] findImageOnScreen(String image, int score) throws Exception {
        int retries = 5;
        Point[] imgRect = null;
        while ((retries > 0) && (imgRect == null)) {
            if (retries < 5) {
                log("Find image failed, retries left: " + retries);
            }
            takeScreenshot(image + "_screenshot");
            imgRect = findImage(image, image + "_screenshot", score);
            retries = retries - 1;
        }
        assertNotNull(imgRect);
        return imgRect;
    }

    public Point[] findImageOnScreenWithScore(String image, int score) throws Exception {
        int retries = 5;
        Point[] imgRect = null;
        while ((retries > 0) && (imgRect == null)) {
            if (retries < 5) {
                log("Find image failed, retries left: " + retries);
            }
            takeScreenshot(image + "_screenshot");
            imgRect = findImage(image, image + "_screenshot", score);
            retries = retries - 1;
        }
        return imgRect;
    }

    public Point[] findImageOnScreen(String image, int retries, int score) throws Exception {

        Point[] imgRect = null;
        while ((retries > 0) && (imgRect == null)) {
            log("Find image started, retries left: " + retries);
            takeScreenshot(image + "_screenshot");
            imgRect = findImage(image, image + "_screenshot", score);
            if (imgRect != null) {
                if (imgRect[4].x < 0 || imgRect[4].y < 0) {
                    imgRect = null;
                }
            }
            retries = retries - 1;
        }
        return imgRect;
    }

    public Point[] findImageOnScreenNoAssert(String image, int retries) throws Exception {
        Point[] imgRect = null;

        while ((retries > 0) && (imgRect == null)) {
            if (retries < 5) {
                log("Find image failed, retries left: " + retries);
            }
            takeScreenshot(image + "_screenshot");
            imgRect = findImage(image, image + "_screenshot");
            retries = retries - 1;
        }
        return imgRect;
    }

    public Point[] findImageOnScreenAndSetRotation(String image) throws Exception {

        // used to initially determine if the screenshots need to be rotated and
        // by what degree
        int retries = 5;
        Point[] imgRect = null;
        while ((retries > 0) && (imgRect == null)) {
            if (retries < 5) {
                log("Find image failed, retries left: " + retries);
            }
            takeScreenshot(image + "_screenshot");
            imgRect = findImage(image, image + "_screenshot", "notSet");
            retries = retries - 1;
        }
        assertNotNull(imgRect);
        return imgRect;
    }

    public void tapImage(String image, String scene) throws Exception {
        Point[] imgRect = findImage(image, scene);
        // imgRect[4] will have the center of the rectangle containing the image
        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) imgRect[4].x, (int) imgRect[4].y, 1);
        } else {
            driver.tap(1, (int) imgRect[4].x, (int) imgRect[4].y, 1);
        }
        sleep(SHORT_SLEEP);
    }

    public void tapMiddleScreen() throws Exception {
        Point size = Util.get_screen_dimensions(driver);

        Point middle = new Point(size.x / 2, size.y / 2);
        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) middle.x, (int) middle.y, 1);
        } else {
            driver.tap(1, (int) middle.x, (int) middle.y, 1);
        }
        sleep(SHORT_SLEEP);
    }

    public void tapImageOnScreen(String image) throws Exception {
        Point[] imgRect = findImageOnScreen(image, 4);

        // imgRect[4] will have the center of the rectangle containing the image
        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) imgRect[4].x, (int) imgRect[4].y, 1);
        } else {
            driver.tap(1, (int) imgRect[4].x, (int) imgRect[4].y, 1);
        }
        sleep(SHORT_SLEEP);
    }

    public void tapImageOnScreen(String image, int retries, int score) throws Exception {
        Point[] imgRect = findImageOnScreen(image, retries, score);
        // imgRect[4] will have the center of the rectangle containing the image

        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) imgRect[4].x, (int) imgRect[4].y, 1);

        } else {
            driver.tap(1, (int) imgRect[4].x, (int) imgRect[4].y, 1);
        }
    }

    public void swipeScreenWithImage(String image, int repeats) throws Exception {
        Point[] imgRect = findImageOnScreen(image, 10);
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();

        if (automationName.equals("Selendroid")) {
            TouchActions action = new TouchActions(driver);
            action.down((int) imgRect[4].x, (int) imgRect[4].y).perform();

            double left_x = size.getWidth() * 0.20;
            double right_x = size.getWidth() * 0.80;
            double top_y = size.getHeight() * 0.20;

            // we will repeat the swiping based on "repeats" argument
            while (repeats > 0) {
                log("Swiping screen with image in progress..");
                action.move((int) left_x, (int) top_y).perform();
                sleep(SHORT_SLEEP);
                // swiping horizontally
                int i = 1;
                while (top_y + i * 10 < size.getHeight() * 0.9) {
                    action.move((int) right_x, (int) (top_y + i * 10)).perform();
                    Thread.sleep(50);
                    action.move((int) left_x, (int) (top_y + i * 10)).perform();
                    Thread.sleep(50);
                    i = i + 1;
                }
                // swiping vertically
                i = 1;
                action.move((int) left_x, (int) top_y);
                while (left_x + i * 10 < right_x) {
                    action.move((int) (left_x + i * 10), size.getHeight());
                    Thread.sleep(50);
                    action.move((int) (left_x + i * 10), (int) top_y);
                    Thread.sleep(50);
                    i = i + 1;
                }
                repeats = repeats - 1;
            }

            action.up(0, 0).perform();
        } else {
            TouchAction action = new TouchAction(driver);
            action.press((int) imgRect[4].x, (int) imgRect[4].y).perform();

            double left_x = size.getWidth() * 0.20;
            double right_x = size.getWidth() * 0.80;
            double top_y = size.getHeight() * 0.20;

            // we will repeat the swiping based on "repeats" argument
            while (repeats > 0) {
                log("Swiping screen with image in progress..");
                action.moveTo((int) left_x, (int) top_y).perform();
                sleep(SHORT_SLEEP);
                // swiping horizontally
                int i = 1;
                while (top_y + i * 20 < size.getHeight() * 0.9) {
                    action.moveTo((int) right_x, (int) (top_y + i * 20)).perform();
                    Thread.sleep(50);
                    action.moveTo((int) left_x, (int) (top_y + i * 20)).perform();
                    Thread.sleep(50);
                    i = i + 1;
                }
                // swiping vertically
                i = 1;
                action.moveTo((int) left_x, (int) top_y);
                while (left_x + i * 20 < right_x) {
                    action.moveTo((int) (left_x + i * 20), size.getHeight());
                    Thread.sleep(50);
                    action.moveTo((int) (left_x + i * 20), (int) top_y);
                    Thread.sleep(50);
                    i = i + 1;
                }
                repeats = repeats - 1;
            }

            action.release().perform();
        }

    }

    public void dragImage(String image, double x_offset, double y_offset) throws Exception {
        // drags image on screen using x and y offset from middle of the screen
        // 0.5 offset => middle point
        Point[] imgRect = findImageOnScreen(image, 10);
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        Point point = new Point(size.getWidth() * x_offset, size.getHeight() * y_offset);
        log("Dragging image to coordinates: " + point.toString());

        if (automationName.equals("Selendroid")) {
            TouchActions action = new TouchActions(driver);
            action.down((int) imgRect[4].x, (int) imgRect[4].y).perform();
            sleep(SHORT_SLEEP);
            action.move((int) point.x, (int) point.y).perform();
            action.up((int) point.x, (int) point.y).perform();
        } else {
            TouchAction action = new TouchAction(driver);
            action.press((int) imgRect[4].x, (int) imgRect[4].y).perform();
            sleep(SHORT_SLEEP);
            action.moveTo((int) point.x, (int) point.y).perform();
            action.release().perform();
        }
    }

    public void selendroidTapAtCoordinate(int x, int y, int secs) throws Exception {
        TouchActions actions = new TouchActions(driver);
        actions.down(x, y).perform();
        sleep(secs);
        actions.up(x, y).perform();
    }

    public void tapImageOnScreenWithOffset(String image, double x_offset, double y_offset, int retries) throws Exception {
        Point[] imgRect = findImageOnScreenNoAssert(image, retries);

        double newX = imgRect[4].x + (x_offset);
        double newY = imgRect[4].y + (y_offset);

        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) newX, (int) newY, 1);
        } else {
            driver.tap(1, (int) newX, (int) newY, 1);
        }
    }

    public void tapImageOnScreenTwice(String image, double x_offset, double y_offset) throws Exception {
        Point[] imgRect = findImageOnScreen(image, 4);
        Point top_left = imgRect[0];
        Point top_right = imgRect[1];
        Point bottom_left = imgRect[2];

        // adding the offset to each coordinate; if offset = 0.5, middle will be
        // returned
        double newX = top_left.x + (top_right.x - top_left.x) * x_offset;
        double newY = top_left.y + (bottom_left.y - top_left.y) * y_offset;

        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) newX, (int) newY, 1);
            sleep(SHORT_SLEEP);
            selendroidTapAtCoordinate((int) newX, (int) newY, 1);
        } else {
            driver.tap(1, (int) newX, (int) newY, 1);
            sleep(SHORT_SLEEP);
            driver.tap(1, (int) newX, (int) newY, 1);
        }
    }

    public void tapImageOnScreen(String image, double x_offset, double y_offset, int retries) throws Exception {
        Point[] imgRect = findImageOnScreen(image, retries);
        Point top_left = imgRect[0];
        Point top_right = imgRect[1];
        Point bottom_left = imgRect[2];

        // adding the offset to each coordinate; if offset = 0.5, middle will be
        // returned
        double newX = top_left.x + (top_right.x - top_left.x) * x_offset;
        double newY = top_left.y + (bottom_left.y - top_left.y) * y_offset;

        if (automationName.equals("Selendroid")) {
            selendroidTapAtCoordinate((int) newX, (int) newY, 1);
        } else {
            driver.tap(1, (int) newX, (int) newY, 1);
        }
    }

    public void swipeScreenWithImage(String image) throws Exception {
        swipeScreenWithImage(image, 1);
    }

}
