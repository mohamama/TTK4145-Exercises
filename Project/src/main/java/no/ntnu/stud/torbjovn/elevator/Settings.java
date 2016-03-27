package no.ntnu.stud.torbjovn.elevator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class Settings {
	private static Logger LOGGER = Logger.getLogger(Settings.class.getSimpleName());
	
	private static Properties properties;
	
	private static void init()
	{
		properties = new Properties();
		try {
			properties.load(new FileInputStream("elevator.config"));
		} catch (IOException e) {
			LOGGER.severe("Could not load config file.\n"+e.getMessage());
		}
	}
	
	public static String getSetting(String key)
	{
		if(properties == null)
		{
			init();
		}
		
		if(properties != null)
		{
			String prop = properties.getProperty(key);
			if(prop == null) throw new NullPointerException("Property '"+ key + "' is not set in config file!");
			return prop;
		}
		throw new IllegalStateException("No config file was loaded!");
	}

}
