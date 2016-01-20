package lib.recognition;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

public class AppiumSetUp {
	public URL url;
	public HttpURLConnection connection;
	public AppiumDriver<WebElement> driver;

	public void configure() throws IOException {
		start_appium_server();
		check_appium_server_started();
	}

	public void start_appium_server() throws IOException {
		String appium_server_url = "http://localhost:4723/wd/hub";
		String regex = ":(\\d+)\\/wd\\/hub$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(appium_server_url);
		String port = null;

		if (matcher.find()) {
			port = matcher.group(1);
		}

		url = new URL(appium_server_url);

		connection = (HttpURLConnection) url.openConnection();
		try {
			connection.connect();
		} catch (ConnectException e) {
			Util.exec_in_new_terminal("appium -p"+ port + "--session-override");
		}
	}

	public void check_appium_server_started() throws IOException {

		Callable<Object> callable = new Callable<Object>() {

			@Override
			public Boolean call() throws Exception {
				Boolean a = null;
				try {
					url = new URL("http://localhost:4723/wd/hub");
					connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					a = connection.getResponseCode() == 404;
				} catch (ConnectException e) {
				}

				return a;
			}
		};

		Util.wait_for(10, 0.5, callable);		
	}

	public AppiumDriver<WebElement> setUpDriver() {
		HashMap<?, ?> appium_description = Util.readFileYML();
		DesiredCapabilities capabilities = new DesiredCapabilities();
		if (appium_description.get("udid") != null)
			capabilities.setCapability("udid", appium_description.get("udid").toString());
		if (appium_description.get("device") != null)
			capabilities.setCapability("deviceName", appium_description.get("device").toString());
		if (appium_description.get("version") != null)
			capabilities.setCapability("platformVersion", appium_description.get("version").toString());
		if (appium_description.get("os") != null)
			capabilities.setCapability("platformName", appium_description.get("os").toString());
		if (appium_description.get("app_path") != null)
			capabilities.setCapability("app", appium_description.get("app_path").toString());
		if (appium_description.get("backend") != null)
			capabilities.setCapability("automationName", appium_description.get("backend").toString());		
		if (appium_description.get("appPackage") != null)
			capabilities.setCapability("appPackage", appium_description.get("appPackage").toString());
		if (appium_description.get("appWaitActivity") != null)
			capabilities.setCapability("appWaitActivity", appium_description.get("appWaitActivity").toString());		
		
		String url = "http://localhost:4723/wd/hub";
			
		try {
			if (appium_description.get("os").toString().toLowerCase().equals("android")) {
				driver = new AndroidDriver<WebElement>(new URL(url), capabilities);
			} else {
				driver = new IOSDriver<WebElement>(new URL(url), capabilities);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return (AppiumDriver<WebElement>) driver;
	}

}
