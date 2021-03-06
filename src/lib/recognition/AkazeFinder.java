package lib.recognition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.appium.java_client.AppiumDriver;

public class AkazeFinder {

    private static final Logger logger = LoggerFactory.getLogger(AkazeFinder.class);
    public String rotation;
    public AppiumDriver<WebElement> driver;

    public AkazeFinder() {
        this.rotation = "notSet";
    }

    public AkazeFinder(AppiumDriver<WebElement> driver) {
        this.rotation = "notSet";
        this.driver = driver;
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    public AkazeFinder(String setRotation) {
        setRotation(setRotation);
    }

    public Point[] findImage(String object_filename_nopng, String scene_filename_nopng, int score) {

        logger.info("AkazeFinder - findImage() started...");
        setupOpenCVEnv();
        String object_filename = object_filename_nopng + ".png";
        String scene_filename = scene_filename_nopng + ".png";

        Mat img_object = Highgui.imread(object_filename, Highgui.CV_LOAD_IMAGE_UNCHANGED);
        Mat img_scene = Highgui.imread(scene_filename, Highgui.CV_LOAD_IMAGE_UNCHANGED);

        rotateImage(scene_filename, img_scene);
        String jsonResults = null;
        try {
            jsonResults = runAkazeMatch(object_filename, scene_filename);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (jsonResults == null) {
            return null;
        }

        logger.info("Keypoints for {} to be found in {} are in file {}", object_filename, scene_filename, jsonResults);

        double initial_width = img_object.size().width;

        Highgui.imwrite(scene_filename, img_scene);

        // finding homography
        LinkedList<Point> objList = new LinkedList<Point>();
        LinkedList<Point> sceneList = new LinkedList<Point>();
        JSONObject jsonObject = getJsonObject(jsonResults);
        if (jsonObject == null) {
            logger.error("ERROR: Json file couldn't be processed. ");
            return null;
        }
        JSONArray keypointsPairs = null;
        try {
            keypointsPairs = jsonObject.getJSONArray("keypoint-pairs");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        Point[] objPoints = new Point[keypointsPairs.length()];
        Point[] scenePoints = new Point[keypointsPairs.length()];
        int j = 0;
        for (int i = 0; i < keypointsPairs.length(); i++) {
            try {
                objPoints[j] = new Point(Integer.parseInt(keypointsPairs.getJSONObject(i).getString("x1")),
                        Integer.parseInt(keypointsPairs.getJSONObject(i).getString("y1")));
                scenePoints[j] = new Point(Integer.parseInt(keypointsPairs.getJSONObject(i).getString("x2")),
                        Integer.parseInt(keypointsPairs.getJSONObject(i).getString("y2")));
                j++;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

        }

        String filename = scene_filename_nopng + "_with_results.png";
        Mat pointsImg = Highgui.imread(scene_filename, Highgui.CV_LOAD_IMAGE_COLOR);

        Mat objectImg = Highgui.imread(object_filename, Highgui.CV_LOAD_IMAGE_COLOR);
        for (int i = 0; i < objPoints.length; i++) {
            Point objectPoint = new Point(objPoints[i].x, objPoints[i].y);
            objList.addLast(objectPoint);
            Core.circle(objectImg, objectPoint, 5, new Scalar(0, 255, 0, -5));
            Point scenePoint = new Point(scenePoints[i].x - initial_width, scenePoints[i].y);
            sceneList.addLast(scenePoint);
            Core.circle(pointsImg, scenePoint, 5, new Scalar(0, 255, 0, -5));
        }
        Highgui.imwrite(filename, pointsImg);

        if ((objList.size() < score) || (sceneList.size() < score)) {
            logger.error("Not enough mathches found. ");
            return null;
        }

        MatOfPoint2f obj = new MatOfPoint2f();
        obj.fromList(objList);
        MatOfPoint2f scene = new MatOfPoint2f();
        scene.fromList(sceneList);

        Mat H = Calib3d.findHomography(obj, scene);

        Mat scene_corners = drawFoundHomography(scene_filename_nopng, img_object, filename, H);

        // Find the center point by determining the position of 4 points
        List<Point> array_points = new ArrayList<Point>();
        array_points.add(new Point(scene_corners.get(0, 0)));
        array_points.add(new Point(scene_corners.get(1, 0)));
        array_points.add(new Point(scene_corners.get(2, 0)));
        array_points.add(new Point(scene_corners.get(3, 0)));

        // get average height of 4 points
        double average = 0;
        for (int i = 0; i < array_points.size(); i++) {
            average += array_points.get(i).y;
        }

        List<Point> array_top = new ArrayList<Point>();
        List<Point> array_bot = new ArrayList<Point>();

        // based on average height we get 2 points for each array
        for (int i = 0; i < array_points.size(); i++) {
            if (array_points.get(i).y < (average / 4)) {
                array_top.add(array_points.get(i));
            } else {
                array_bot.add(array_points.get(i));
            }
        }

        // sorting array to get correct position.
        Collections.sort(array_top, new Comparator<Point>() {

            public int compare(Point x1, Point x2) {
                return Double.compare(x1.x, x2.x);
            }
        });

        Collections.sort(array_bot, new Comparator<Point>() {

            public int compare(Point x1, Point x2) {
                return Double.compare(x1.x, x2.x);
            }
        });

        // special case if an array list has 3 points
        if (array_top.size() > 2) {
            Point temp = new Point();
            temp = array_top.get(array_top.size() - 1);
            array_top.remove(array_top.size() - 1);
            array_bot.add(temp);

            // re-sort in order to get correct position
            Collections.sort(array_bot, new Comparator<Point>() {

                public int compare(Point x1, Point x2) {
                    return Double.compare(x1.x, x2.x);
                }
            });
        } else if (array_bot.size() > 2) {
            Point temp = new Point();
            temp = array_bot.get(array_bot.size() - 1);
            array_bot.remove(array_bot.size() - 1);
            array_top.add(temp);

            // re-sort in order to get correct position
            Collections.sort(array_top, new Comparator<Point>() {

                public int compare(Point x1, Point x2) {
                    return Double.compare(x1.x, x2.x);
                }
            });
        }

        //special case if 2 points of an array list have same x-coordinates
        if (array_top.get(0).x == array_top.get(1).x) {
            Collections.sort(array_top, new Comparator<Point>() {

                public int compare(Point x1, Point x2) {
                    return (int) (x2.y - x1.y);
                }
            });

        } else if (array_bot.get(0).x == array_bot.get(1).x) {
            Collections.sort(array_bot, new Comparator<Point>() {

                public int compare(Point x1, Point x2) {
                    return (int) (x2.y - x1.y);
                }
            });

        }

        Point top_left = array_top.get(0);
        Point top_right = array_top.get(1);
        Point bottom_left = array_bot.get(0);
        Point bottom_right = array_bot.get(1);

        Point[] points = new Point[5];
        points[0] = top_left;
        points[1] = top_right;
        points[2] = bottom_left;
        points[3] = bottom_right;
        Point center = null;

        String current_orientation = Util.get_current_orientation(driver);
        Point screen_dimensions = Util.get_screen_dimensions(driver);

        switch (current_orientation) {
            case "ORIENTATION_PORTRAIT":
                center = new Point(top_left.x + ((top_right.x - top_left.x) / 2),
                        top_left.y + ((bottom_left.y - top_left.y) / 2));

                break;

            case "ORIENTATION_LANDSCAPELEFT":
                screen_dimensions = Util.get_screen_dimensions(driver);
                center = new Point((top_right.y + ((bottom_right.y - top_right.y) / 2)),
                        screen_dimensions.y - (top_left.x + ((top_right.x - top_left.x) / 2)));

                break;

            case "ORIENTATION_PORTRAIT_UPSIDEDOWN":
                screen_dimensions = Util.get_screen_dimensions(driver);
                center = new Point(screen_dimensions.x - (bottom_left.x + ((bottom_right.x - bottom_left.x) / 2)),
                        screen_dimensions.y - (top_left.y + ((bottom_left.y - top_left.y) / 2)));

                break;

            case "ORIENTATION_LANDSCAPERIGHT":
                screen_dimensions = Util.get_screen_dimensions(driver);
                center = new Point(screen_dimensions.x - (top_left.y + (bottom_left.y - top_left.y) / 2),
                        top_left.x + ((top_right.x - top_left.x) / 2));

                break;
        }

        center = new Point(top_left.x + ((top_right.x - top_left.x) / 2),
                top_left.y + ((bottom_left.y - top_left.y) / 2));

        points[4] = center;
        logger.info("Image found at coordinates: " + (int) center.x + ", " + (int) center.y);

        return points;
    }

    private Mat drawFoundHomography(String scene_filename_nopng, Mat img_object, String filename, Mat h) {
        Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
        Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

        obj_corners.put(0, 0, new double[]{0, 0});
        obj_corners.put(1, 0, new double[]{img_object.cols(), 0});
        obj_corners.put(2, 0, new double[]{img_object.cols(), img_object.rows()});
        obj_corners.put(3, 0, new double[]{0, img_object.rows()});

        Core.perspectiveTransform(obj_corners, scene_corners, h);

        Mat img = Highgui.imread(filename, Highgui.CV_LOAD_IMAGE_COLOR);

        Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0),
                4);
        Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0),
                4);
        Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0),
                4);
        Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0),
                4);

        filename = scene_filename_nopng + "_with_results.png";
        Highgui.imwrite(filename, img);
        return scene_corners;
    }

    private String runAkazeMatch(String object_filename, String scene_filename)
            throws InterruptedException, IOException {

        long timestamp = System.currentTimeMillis();
        String jsonFilename = "./target/keypoints/keypoints_" + timestamp + ".json";
        logger.info("Json file should be found at: {}", jsonFilename);
        File file = new File(jsonFilename);
        file.getParentFile().mkdirs();
        String[] akazeMatchCommand = {"akaze/bin/akaze_match", object_filename, scene_filename, "--json", jsonFilename,
            "--dthreshold", "0.0001"};
        try {
            ProcessBuilder p = new ProcessBuilder(akazeMatchCommand);
            Process proc = p.start();
            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            while ((br.readLine()) != null) {
                System.out.print(".");
            }
            int exitVal = proc.waitFor();
            logger.info("Akaze matching process exited with value: " + exitVal);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (!file.exists()) {
            logger.error("ERROR: Image recognition with Akaze failed. No json file created.");
            return null;
        } else {
            return jsonFilename;
        }
    }

    private void rotateImage(String scene_filename, Mat img_scene) {
        if (rotation.equals("90 degrees")) {
            rotateImage90n(img_scene, img_scene, 90);
        }
        if (rotation.equals("180 degrees")) {
            rotateImage90n(img_scene, img_scene, 180);
        }
        if (rotation.equals("270 degrees")) {
            rotateImage90n(img_scene, img_scene, 270);
        }

        Highgui.imwrite(scene_filename, img_scene);

    }

    private void setupOpenCVEnv() {

        Field fieldSysPath = null;
        try {
            fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        fieldSysPath.setAccessible(true);
        try {
            fieldSysPath.set(null, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        System.load(System.getProperty("user.dir") + "/javalib/libopencv_java2412.dylib");
    }

    private JSONObject getJsonObject(String filename) {
        File jsonFile = new File(filename);
        InputStream is = null;
        try {
            is = new FileInputStream(jsonFile);
            String jsonTxt = IOUtils.toString(is);
            return new JSONObject(jsonTxt);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void rotateImage90n(Mat source, Mat dest, int angle) {
        // angle : factor of 90, even it is not factor of 90, the angle will be
        // mapped to the range of [-360, 360].
        // {angle = 90n; n = {-4, -3, -2, -1, 0, 1, 2, 3, 4} }
        // if angle bigger than 360 or smaller than -360, the angle will be
        // mapped to -360 ~ 360
        // mapping rule is : angle = ((angle / 90) % 4) * 90;
        //
        // ex : 89 will map to 0, 98 to 90, 179 to 90, 270 to 3, 360 to 0.

        source.copyTo(dest);

        angle = ((angle / 90) % 4) * 90;

        int flipHorizontalOrVertical;
        // 0 : flip vertical; 1 flip horizontal
        if (angle > 0) {
            flipHorizontalOrVertical = 0;
        } else {
            flipHorizontalOrVertical = 1;
        }

        int number = (int) (angle / 90);

        for (int i = 0; i != number; ++i) {
            Core.transpose(dest, dest);
            Core.flip(dest, dest, flipHorizontalOrVertical);
        }
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}

class PointSortX implements Comparator<Point> {

    public int compare(Point a, Point b) {
        return (a.x < b.x) ? -1 : (a.x > b.x) ? 1 : 0;
    }
}

class PointSortY implements Comparator<Point> {

    public int compare(Point a, Point b) {
        return (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : 0;
    }
}
