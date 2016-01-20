package lib.recognition;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.opencv.core.Rect;

import com.apache.pdfbox.ocr.tesseract.TessBaseAPI;

public class OpticalCharacterRecognition {

	public String characterRecognition(String filepath) {
		TessBaseAPI api = new TessBaseAPI();
		api.init("src/main/resources/data", "eng");
		Image image = null;
		try {
			image = ImageIO.read(new File(filepath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedImage buffered = (BufferedImage) image;
		api.setBufferedImage(buffered);
		String text = api.getUTF8Text();
		api.end();
		
		return text;
	}
	
	public String[][] getWordsArray(String filepath) {
		try {
			filepath = new ImageUtil().cropImage(filepath, new Rect(1, 189, 1078, 1150));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String text = characterRecognition(filepath).trim();

		String[] tokens = text.split("\n");

		// remove null and empty values
		tokens = Arrays.stream(tokens).filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);

		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = tokens[i].trim().replaceAll(" ", "");			
		}		
		
		int x = tokens[0].length();
		int y = tokens.length;
		
		String[][] multiDementionWordsArray = new String[x][y];

		for (int i = 0; i < y; i++) {
			for (int j = 0; j < x; j++) {
				multiDementionWordsArray[i][j] = Character.toString(tokens[i].toCharArray()[j]);
			}
		}

		return multiDementionWordsArray;
	}
	
	
}
