/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetel.graph.ContextProvider;
import org.jetel.graph.runtime.EngineInitializer;



/** Utility class providing methods for logging safe messages (password obfuscation in URL, ...)
 * 
 * 
 * @author Tomas Laurincik (info@cloveretl.com)
 *         (c) Javlin, a.s. (www.cloveretl.com)
 *
 * @created 20.2.2012
 */
public class SafeLogUtils {
	/** Pattern for identifying URL with password in a given text */
	private static final Pattern URL_PASSWORD_PATTERN = Pattern.compile(".+://(.*?):([^\\*]*?)@.+");
	

	/**
	 * Obfuscates passwords in URLs in given text.
	 * Moreover secure parameters are backward resolved if it is possible.
	 * 
	 * @param text - text to obfuscate passwords
	 * @return obfuscated text
	 */
	public static String obfuscateSensitiveInformation(String text) {
		if (text == null) {
			return null;
		}
		
		//firstly, obfuscate secure parameters using authority proxy
		if (EngineInitializer.isInitialized()) {
			text = ContextProvider.getAuthorityProxy().obfuscateSecureParameters(text);
		}
		
		//secondly, try to detect passwords in possible URLs 
		boolean changed = false;
		String result = text;
		do {
			changed = false;
			Matcher m = URL_PASSWORD_PATTERN.matcher(result);
			
			if (m.matches()) {
				StringBuilder builder = new StringBuilder(result.substring(0, m.start(2)));

				builder.append("***");
				builder.append(result.substring(m.end(2)));
				result = builder.toString();
				
				changed = true;
			} 
		} while (changed); 
		
		
		return result;
	}
	
	private SafeLogUtils() {
	}
}
