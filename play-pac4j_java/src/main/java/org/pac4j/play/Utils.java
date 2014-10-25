package org.pac4j.play;

import org.apache.commons.lang3.StringUtils;

public class Utils {

	/**
     * This method returns the default value from a specified value.
     * 
     * @param value
     * @param defaultValue
     * @return value or defaultValue, if the first is blank
     */
    public static String getOrElse(final String value, final String defaultValue) {
        String redirectUrl = defaultValue;

        if (StringUtils.isNotBlank(value)) {
            redirectUrl = value;
        }

        return redirectUrl;
    }

}
