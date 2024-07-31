package com.ccctc.adaptor.util.mock;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class MockUtils {
    /**
     * Extract a CCC ID, if found, from an id in the format cccid:ABC1234
     *
     * @param id Id to find CCC ID in
     * @return CCC ID (if found)
     */
    public static String extractCccId(String id) {
        if (id != null && id.length() >= 6 && id.substring(0, 6).equals("cccid:"))
            return id.substring(6);

        return null;
    }

    /**
     * Fix a map by converting an sisPersonId entry to a cccid entry if it start with cccid:
     *
     * @param map Map to fix
     */
    public static void fixMap(Map<String, Object> map) {
        if (map != null && map.containsKey("sisPersonId")) {
            String cccid = extractCccId((String) map.get("sisPersonId"));
            if (cccid != null) {
                map.remove("sisPersonId");
                map.put("cccid", cccid);
            }
        }
    }

    /**
     * Remove time component from a Date object (sets the date to midnight)
     *
     * @param date Original date
     * @return New date with time component to set to midnight
     */
    public static Date removeTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
