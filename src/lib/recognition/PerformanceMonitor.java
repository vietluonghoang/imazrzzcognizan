package lib.recognition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformanceMonitor {
			
	public String createCSVFile() throws IOException {		
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();
		String current_date = dateFormat.format(date);
		
		String file_path =  System.getProperty("user.dir") + "/report/" + "monitor_profile_" + current_date
		+ ".csv";
		String[] FILE_HEADER = {"case_name","cpuinfo(%)","cpuinfo-detail","meminfo(kB)","meminfo-detail"};
		
		File file = new File(file_path);
		file.createNewFile();
		
		FileWriter fwTest = new FileWriter(file);
		for(String s: FILE_HEADER){
			fwTest.append(s);
			fwTest.append(",");
		}
		
		fwTest.append(System.getProperty("line.separator"));
		fwTest.close();

		return file_path;
	}

	public void performanceMonitor(String case_name, HashMap<String, Boolean> hash_profile, String filepath) throws IOException {
		String package_name = "com.alleylabs.wordgame";

		BufferedReader br = null;
		String line = "";
		String cpuinfo = "";
		String cpuinfo_detail = "";
		String meminfo = "";
		String meminfo_detail = "";
		Runtime r = null;
		InputStream stdin = null;
		InputStreamReader isr = null;
		String adbShell = "";
		String regex = "";
		Pattern pattern = null;
		Matcher matcher = null;

		String[] arr = new String[5];
		arr[0]= case_name;

		adbShell = "adb shell dumpsys cpuinfo " + package_name;
		
		String output = "";
		try {
			r = Runtime.getRuntime();
			Process proc = r.exec(adbShell);
			stdin = proc.getInputStream();
			isr = new InputStreamReader(stdin);
			br = new BufferedReader(isr);

			while ((line = br.readLine()) != null) {
				output += line + "\n";
				proc.waitFor();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}	
		
		regex = "[-+]?(\\d*[.])?\\d+%\\s\\d+\\/com.alleylabs.wordgame";
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(output);

		if (matcher.find()) {
			cpuinfo = matcher.group(0).split("%")[0];
		} else {
			cpuinfo = "0";
		}
		
		arr[1]= cpuinfo;

		regex = "[\\d.]*[-+]?(\\d*[.])?\\d+%\\sTOTAL.*";
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(output);

		if (matcher.find()) {
			cpuinfo_detail = matcher.group(0);
		} else {
			cpuinfo_detail = "0";
		}

		arr[2]= cpuinfo_detail;

		output = "";
		
		adbShell = "adb shell dumpsys meminfo " + package_name;
		try {
			r = Runtime.getRuntime();
			Process proc = r.exec(adbShell);
			stdin = proc.getInputStream();
			isr = new InputStreamReader(stdin);
			br = new BufferedReader(isr);

			while ((line = br.readLine()) != null) {
				output += line + "\n";
				proc.waitFor();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		regex = "TOTAL\\s+\\d+";
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(output);

		if (matcher.find()) {			
			meminfo = matcher.group(0).split("\\s+")[1];
		} else {
			meminfo = "0";
		}

		arr[3]= meminfo;
		
		regex = "([\\s\\S]*)Free";	
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(output);
		
		if (matcher.find()) {
		
			meminfo_detail = matcher.group() + "\n";
			regex = "\\sTOTAL.*";
			
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(output);
			if (matcher.find()) {
				meminfo_detail += matcher.group(0);
			}

		} else {
			meminfo_detail = "0";
		}

		arr[4]= meminfo_detail;
		
		br = new BufferedReader(new FileReader(filepath));
		
		output = "";
		while ((line = br.readLine()) != null) {
			output += line + "\n";
		}
	
		File file = new File(filepath);
		
		FileWriter fwTest = new FileWriter(file);
		fwTest.append(output);
		
		for(String s: arr){
			fwTest.append("\""+s+"\"");
			fwTest.append(",");
		}
		
		fwTest.append(System.getProperty("line.separator"));
		fwTest.close();
				
	}

	public void hash_profile(HashMap<String, Boolean> hash_profile) {
		Map<String, Boolean> hash = new HashMap<String, Boolean>();
		hash.put("accessibility", false);
		hash.put("account", false);
		hash.put("activity", false);
		hash.put("alarm", false);
		hash.put("appwidget", false);
		hash.put("audio", false);
		hash.put("backup", false);
		hash.put("battery", false);
		hash.put("connectivity", false);
		hash.put("content", false);
		hash.put("cpuinfo", true);

		hash.put("dbinfo", false);
		hash.put("device_policy", false);
		hash.put("devicestoragemonitor", false);
		hash.put("diskstats", false);
		hash.put("dropbox", false);
		hash.put("gfxinfo", false);
		hash.put("input", false);
		hash.put("input_method", false);
		hash.put("iphonesubinfo", false);
		hash.put("location", false);
		hash.put("media.audio_flinger", false);
		hash.put("media.audio_policy", false);
		hash.put("media.camera", false);
		hash.put("meminfo", true);

		hash.put("mount", false);
		hash.put("netpolicy", false);
		hash.put("netstats", false);
		hash.put("network_management", false);
		hash.put("nfc", false);
		hash.put("notification", false);
		hash.put("package", false);
		hash.put("power", false);
		hash.put("samplingprofiler", false);
		hash.put("search", false);
		hash.put("sensorservice", false);
		hash.put("servicediscovery", false);
		hash.put("statusbar", false);
		hash.put("SurfaceFlinger", false);
		hash.put("telephony.registry", false);
		hash.put("textservices", false);
		hash.put("uimode", false);
		hash.put("updatelock", false);
		hash.put("usagestats", false);
		hash.put("usb", false);
		hash.put("wallpaper", false);
		hash.put("wifi", false);
		hash.put("wifip2p", false);
		hash.put("window", false);

		 if (hash_profile != null) {
		 hash.putAll(hash_profile);
		 }
	}

}
