package lib.recognition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Devicetarget {

    public void getDeviceTarget() {
        String adbShell = "adb devices";
        String output = "";
        try {
            Runtime r = Runtime.getRuntime();
            Process proc = r.exec(adbShell);
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

        String regex = "(\\S+)\\sdevice\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            System.out.println(matcher.group(1));
        }
    }
}
