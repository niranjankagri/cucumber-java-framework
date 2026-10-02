package utils;

import pages.BaseClass;
import java.io.FileInputStream;
import java.io.IOException;

public class PropertyLoader {

	public PropertyLoader() {
		try {
			FileInputStream configFile = new FileInputStream("src/main/resources/config.properties");
			BaseClass.properties.load(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
