package lib.recognition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.opencv.core.Point;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import io.appium.java_client.AppiumDriver;

public class Util {

    public static Logger logger = LoggerFactory.getLogger(Util.class);

    public static String takeScreenshot(AppiumDriver<WebElement> driver) throws Exception {
        String fullFileName = "screenshot" + "_" + System.currentTimeMillis();

        try {
            File imgFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(imgFile, new File("screenshot/" + fullFileName + ".png"));
            logger.debug("Util::takeScreenShot: " + fullFileName + ".png saved to screenshot folder");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "screenshot/" + fullFileName + ".png";
    }

    public static void exec_in_new_terminal(String cmd) throws IOException {
        String command = "tell app \"Terminal\" to do script \"" + cmd + "\"";
        Runtime runtime = Runtime.getRuntime();
        String[] args = {"osascript", "-e", command};
        runtime.exec(args);
    }

    public static Point get_screen_dimensions(AppiumDriver<WebElement> driver) {
        return new Point(driver.manage().window().getSize().getWidth(), driver.manage().window().getSize().getHeight());
    }

    public static void wait_for(double wait_for_timeout, double wait_for_interval, Callable<?> callable) {
        double current_time = System.currentTimeMillis();
        double end_time = current_time + (wait_for_timeout * 1000);

        Boolean boo = true;
        while (boo) {
            if (System.currentTimeMillis() > end_time) {
                try {
                    boo = false;
                    throw new TimeoutException("Time Out!");
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }

            try {
                if (callable.call() != null && callable.call().toString().equals("true")) {
                    boo = false;
                } else {
                    Thread.sleep((long) (1000 * wait_for_interval));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String get_current_orientation(AppiumDriver<WebElement> driver) {
        if (readFileYML().get("os").toString().equals("android")) {
            return android_current_orientation();
        }

        return ios_current_orientation(driver);
    }

    public static String android_current_orientation() {
        String[] args = {"bash", "-c", "adb shell dumpsys input | grep 'SurfaceOrientation' | awk '{ print $2 }'"};

        String output = "";
        try {
            Runtime r = Runtime.getRuntime();
            Process proc = r.exec(args);
            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);

            String line = null;
            while ((line = br.readLine()) != null) {
                output += line + "\n";
                proc.waitFor();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        output = output.split("\n")[0];

        String orientation = null;
        switch (Integer.parseInt(output)) {
            case 0:
                orientation = "ORIENTATION_PORTRAIT";
                break;
            case 1:
                orientation = "ORIENTATION_LANDSCAPELEFT";
                break;
            case 2:
                orientation = "ORIENTATION_PORTRAIT_UPSIDEDOWN";
                break;
            case 3:
                orientation = "ORIENTATION_LANDSCAPERIGHT";
                break;
        }

        return orientation;
    }

    public static String ios_current_orientation(AppiumDriver<WebElement> driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String orientation = null;
        switch (Integer.parseInt((String) js.executeScript("UIATarget.localTarget().frontMostApp().interfaceOrientation();"))) {
            case 1:
                orientation = "ORIENTATION_PORTRAIT";
                break;
            case 2:
                orientation = "ORIENTATION_PORTRAIT_UPSIDEDOWN";
                break;
            case 3:
                orientation = "ORIENTATION_LANDSCAPELEFT";
                break;
            case 4:
                orientation = "ORIENTATION_LANDSCAPERIGHT";
                break;
        }
        return orientation;
    }

    public static HashMap<?, ?> readFileYML(String ymlFile) {
        InputStream input = null;
        try {
            input = new FileInputStream(new File(ymlFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return (HashMap<?, ?>) new Yaml().load(input);
    }

    public static HashMap<?, ?> readFileYML() {
        InputStream input = null;
        try {
            input = new FileInputStream(new File("src/test/resources/appium.yml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        HashMap<?, ?> map = (HashMap<?, ?>) new Yaml().load(input);
        return (HashMap<?, ?>) map.get("appium");
    }

}
