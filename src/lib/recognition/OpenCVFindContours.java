package lib.recognition;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class OpenCVFindContours {

    public void example() {
        System.load(System.getProperty("user.dir") + "/javalib/libopencv_java2412.dylib");

        Mat image = Highgui.imread(System.getProperty("user.dir") + "/screenshot/Screenshot.png",
                Imgproc.COLOR_BGR2GRAY);
        Mat imageHSV = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageBlurr = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageA = new Mat(image.size(), Core.DEPTH_MASK_ALL);
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imageBlurr, imageA, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 5);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int i = 0; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > 50) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                if (rect.height > 28) {
                    Core.rectangle(image, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
                }
            }
        }
        Highgui.imwrite(System.getProperty("user.dir") + "/screenshot/grayscale_with_results.png",
                image);
    }

    public ArrayList<List<Rect>> recursive(List<Rect> list, ArrayList<List<Rect>> arraylist) {

        for (int i = 0; i < list.size() - 1; i++) {
            ArrayList<Rect> rectlist = new ArrayList<Rect>();
            if (list.get(i).x + list.get(i).width + 20 < list.get(i + 1).x) {
                rectlist.addAll(list.subList(0, i + 1));
                arraylist.add(rectlist);
                list.removeAll(rectlist);
                recursive(list, arraylist);
            } else if (i + 1 == list.size() - 1) {
                rectlist.addAll(list.subList(0, list.size()));
                arraylist.add(rectlist);
                list.removeAll(rectlist);
            }
        }
        return arraylist;
    }

    public ArrayList<Integer> numberOfWords(String filename) {

        System.load(System.getProperty("user.dir") + "/javalib/libopencv_java2412.dylib");

        Mat image = Highgui.imread("findcontours/" + Util.readFileYML().get("udid").toString() + "/" + filename,
                Imgproc.COLOR_BGR2GRAY);
        Mat imageHSV = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageBlurr = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageA = new Mat(image.size(), Core.DEPTH_MASK_ALL);
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imageBlurr, imageA, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 5);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // start i=1 loai tru duong vien ngoai cung
        for (int i = 1; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > 50) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                if (rect.height > 28) {
                    Core.rectangle(image, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
                }
            }
        }

        List<Rect> rectlist = new ArrayList<Rect>();
        for (int i = 1; i < contours.size(); i++) {
            rectlist.add(Imgproc.boundingRect(contours.get(i)));
        }

        List<Rect> listtemp = new ArrayList<Rect>();
        for (int i = 0; i < rectlist.size(); i++) {
            // chi lay nhung duong vien trong khoang y 1350-> 1630
            if (rectlist.get(i).y < 1350 || rectlist.get(i).y > 1630) {
                listtemp.add(rectlist.get(i));
            }
        }

        rectlist.removeAll(listtemp);

        Collections.sort(rectlist, new Comparator<Rect>() {

            public int compare(Rect x1, Rect x2) {
                int result = Double.compare(x1.y, x2.y);
                if (result == 0) {
                    result = Double.compare(x1.x, x2.x);
                }
                return result;
            }
        });

        List<Integer> intlist = new ArrayList<Integer>();
        for (int i = 0; i < rectlist.size(); i++) {
            intlist.add(rectlist.get(i).y);
        }

        Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        for (Integer i : intlist) {
            Integer count = map.get(i);
            map.put(i, (count == null) ? 1 : count + 1);
        }

        // sap xep lai map
        Map<Integer, Integer> treeMap = new TreeMap<Integer, Integer>(map);

        List<List<Rect>> arraylist = new ArrayList<List<Rect>>();

        for (Map.Entry<Integer, Integer> entry : treeMap.entrySet()) {

            List<Rect> templist = new ArrayList<Rect>();
            if (entry.getValue() > 1) {

                for (int i = 0; i < rectlist.size(); i++) {
                    if (rectlist.get(i).y == entry.getKey()) {
                        templist.add(rectlist.get(i));
                    }
                }
            }
            arraylist.add((ArrayList<Rect>) templist);
        }

        // loai bo duong vien trong chi lay duong vien ngoai
        for (int i = 0; i < arraylist.size(); i++) {
            if (arraylist.get(i).get(0).y + 10 > arraylist.get(i + 1).get(0).y) {
                arraylist.remove(arraylist.get(i + 1));
            }
        }

        // so sanh khoang cach x de lay ra array list cac tu
        ArrayList<List<Rect>> finalarray = new ArrayList<List<Rect>>();

        for (int i = 0; i < arraylist.size(); i++) {
            finalarray = recursive(arraylist.get(i), finalarray);
        }

        Highgui.imwrite("findcontours/" + Util.readFileYML().get("udid").toString() + "/" + "squarecontour_" + filename
                + "_with_results.png", image);

        ArrayList<Integer> numberOfWords = new ArrayList<Integer>();

        for (int i = 0; i < finalarray.size(); i++) {
            numberOfWords.add(finalarray.get(i).size());
        }

        return numberOfWords;
    }

    public ArrayList<List<Rect>> getArrayListPosition(List<Rect> list, ArrayList<List<Rect>> arraylist, int column) {

        if (list.size() > 0) {
            ArrayList<Rect> rectlist = new ArrayList<Rect>();
            rectlist.addAll(list.subList(0, column));
            arraylist.add(rectlist);

            list.removeAll(rectlist);

            ArrayList<Rect> templist = new ArrayList<Rect>();
            for (int i = 0; i < list.size(); i++) {

                if (list.get(i).y < rectlist.get(0).y + rectlist.get(0).height) {
                    templist.add(list.get(i));
                }

            }

            list.removeAll(templist);

            getArrayListPosition(list, arraylist, column);
        } else {

            return arraylist;
        }
        return arraylist;
    }

    public Rect[][] multiDimentionPositionArray(String filepath, String[][] ar) {
        System.load(System.getProperty("user.dir") + "/javalib/libopencv_java2412.dylib");

        Mat image = Highgui.imread(filepath, Imgproc.COLOR_BGR2GRAY);

        Mat imageHSV = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageBlurr = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageA = new Mat(image.size(), Core.DEPTH_MASK_ALL);
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imageBlurr, imageA, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 5);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> rectlist = new ArrayList<Rect>();
        for (int i = 1; i < contours.size(); i++) {
            rectlist.add(Imgproc.boundingRect(contours.get(i)));
        }

        List<Rect> templist = new ArrayList<Rect>();
        for (int i = 0; i < rectlist.size(); i++) {

            if (rectlist.get(i).height < 70 || rectlist.get(i).y < 200 || rectlist.get(i).y > 1340) {
                templist.add(rectlist.get(i));
            }
        }

        rectlist.removeAll(templist);

        Collections.sort(rectlist, new Comparator<Rect>() {

            public int compare(Rect x1, Rect x2) {
                int result = Double.compare(x1.y, x2.y);
                if (result == 0) {
                    result = Double.compare(x1.x, x2.x);
                }
                return result;
            }
        });

        Rect[][] multiDementionPositionArray = new Rect[ar[0].length][ar.length];

        ArrayList<List<Rect>> arraylist = new ArrayList<List<Rect>>();

        arraylist = getArrayListPosition(rectlist, new ArrayList<List<Rect>>(), ar[0].length);

        for (int i = 0; i < arraylist.size(); i++) {
            Collections.sort(arraylist.get(i), new Comparator<Rect>() {
                public int compare(Rect x1, Rect x2) {
                    return Double.compare(x1.x, x2.x);
                }
            });
        }

        for (int i = 0; i < ar.length; i++) {
            for (int j = 0; j < ar[0].length; j++) {
                multiDementionPositionArray[i][j] = arraylist.get(i).get(j);
            }
        }

        for (int i = 0; i < ar.length; i++) {
            for (int j = 0; j < ar[0].length; j++) {
                Core.rectangle(image,
                        new Point(multiDementionPositionArray[i][j].x, multiDementionPositionArray[i][j].y),
                        new Point(multiDementionPositionArray[i][j].x + multiDementionPositionArray[i][j].width,
                                multiDementionPositionArray[i][j].y + multiDementionPositionArray[i][j].height),
                        new Scalar(0, 0, 255));
            }
        }

        Highgui.imwrite("findcontours/" + Util.readFileYML().get("udid").toString() + "/" + "wordcontour_"
                + new File(filepath).getName() + "_with_results.png", image);

        return multiDementionPositionArray;
    }

    public Rect[][] multiDimentionPositionArray(String filepath) {
        System.load(System.getProperty("user.dir") + "/javalib/libopencv_java2412.dylib");

        Mat image = Highgui.imread(filepath, Imgproc.COLOR_BGR2GRAY);

        Mat imageHSV = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageBlurr = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageA = new Mat(image.size(), Core.DEPTH_MASK_ALL);
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imageBlurr, imageA, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 5);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> rectlist = new ArrayList<Rect>();
        for (int i = 1; i < contours.size(); i++) {
            rectlist.add(Imgproc.boundingRect(contours.get(i)));
        }

        List<Rect> templist = new ArrayList<Rect>();
        for (int i = 0; i < rectlist.size(); i++) {

            if (rectlist.get(i).height < 70 || rectlist.get(i).y < 200 || rectlist.get(i).y > 1340) {
                templist.add(rectlist.get(i));
            }
        }

        rectlist.removeAll(templist);

        Collections.sort(rectlist, new Comparator<Rect>() {

            public int compare(Rect x1, Rect x2) {
                int result = Double.compare(x1.y, x2.y);
                if (result == 0) {
                    result = Double.compare(x1.x, x2.x);
                }
                return result;
            }
        });

        String[][] ar = new OpticalCharacterRecognition().getWordsArray(filepath);

        Rect[][] multiDementionPositionArray = new Rect[ar[0].length][ar.length];

        ArrayList<List<Rect>> arraylist = new ArrayList<List<Rect>>();

        arraylist = getArrayListPosition(rectlist, new ArrayList<List<Rect>>(), ar[0].length);

        for (int i = 0; i < arraylist.size(); i++) {
            Collections.sort(arraylist.get(i), new Comparator<Rect>() {
                public int compare(Rect x1, Rect x2) {
                    return Double.compare(x1.x, x2.x);
                }
            });
        }

        for (int i = 0; i < ar.length; i++) {
            for (int j = 0; j < ar[0].length; j++) {
                multiDementionPositionArray[i][j] = arraylist.get(i).get(j);
            }
        }

        for (int i = 0; i < ar.length; i++) {
            for (int j = 0; j < ar[0].length; j++) {
                Core.rectangle(image,
                        new Point(multiDementionPositionArray[i][j].x, multiDementionPositionArray[i][j].y),
                        new Point(multiDementionPositionArray[i][j].x + multiDementionPositionArray[i][j].width,
                                multiDementionPositionArray[i][j].y + multiDementionPositionArray[i][j].height),
                        new Scalar(0, 0, 255));
            }
        }

        Highgui.imwrite("findcontours/" + Util.readFileYML().get("udid").toString() + "/" + "wordcontour_"
                + new File(filepath).getName() + "_with_results.png", image);

        return multiDementionPositionArray;
    }

}
