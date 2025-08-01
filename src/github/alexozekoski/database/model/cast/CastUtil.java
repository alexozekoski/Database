/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model.cast;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import github.alexozekoski.database.Log;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author alexo
 */
public class CastUtil {

    public static final String DATE_JSON = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String DATE_SQL = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String TIME_JSON = "HH:mm";

    private static final String[] FORMATS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy/MM/dd"
    };

    public static JsonElement sqlToJson(Object sqlObject) {
        if (sqlObject == null) {
            return JsonNull.INSTANCE;
        }
        if (Number.class.isInstance(sqlObject)) {
            return new JsonPrimitive((Number) sqlObject);
        }
        if (Boolean.class.isInstance(sqlObject)) {
            return new JsonPrimitive((Number) sqlObject);
        }
        return new JsonPrimitive(sqlObject.toString());

    }

    public static Date timeToDateUtil(long time) {
        time -= TimeZone.getDefault().getOffset(time);
        return new Date(time);
    }

    public static java.sql.Date timeToDate(long time) {
        time -= TimeZone.getDefault().getOffset(time);
        return new java.sql.Date(time);
    }

    public static Date stringToDateUtil(String jsonDate) {
        if (jsonDate == null) {
            return null;
        }
        for (String format : FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(jsonDate);
            } catch (ParseException ignored) {
            }
        }
        Log.printWarning(new Exception("Failed parse data " + jsonDate));
        return null;
    }

    public static Date jsonDateUtil(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.getAsJsonPrimitive().isNumber()) {
            return new Date(json.getAsLong());
        }
        return stringToDateUtil(json.getAsString());
    }

    public static java.sql.Date jsonDate(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.getAsJsonPrimitive().isNumber()) {
            return new java.sql.Date(json.getAsLong());
        }
        return new java.sql.Date(stringToDateUtil(json.getAsString()).getTime());
    }

    public static Timestamp jsonTimestamp(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.getAsJsonPrimitive().isNumber()) {
            return new Timestamp(json.getAsLong());
        }
        return new Timestamp(stringToDateUtil(json.getAsString()).getTime());
    }

    public static Timestamp stringTimestamp(String date) {
        return new Timestamp(stringToDateUtil(date).getTime());
    }

    public static Time jsonTime(JsonElement json) throws ParseException {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.getAsJsonPrimitive().isNumber()) {
            return new Time(json.getAsLong());
        }
        String jsonTime = json.getAsString();
        if (jsonTime.length() == 5) {
            return new Time(new SimpleDateFormat(TIME_JSON).parse(jsonTime).getTime());
        }
        return new Time(stringToDateUtil(json.getAsString()).getTime());
    }

    public static JsonElement toJsonTime(Time time) {
        if (time == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(new SimpleDateFormat(TIME_JSON).format(time));
    }

    public static JsonElement toJson(Date date) {
        if (date == null) {
            return JsonNull.INSTANCE;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return new JsonPrimitive(sdf.format(date));
    }

    public static Date toDateUtil(Object sqlvalue) throws ParseException {
        if (sqlvalue == null) {
            return null;
        }
        if (Date.class.isInstance(sqlvalue)) {
            return (Date) sqlvalue;
        }
        if (String.class.isInstance(sqlvalue)) {
            return new SimpleDateFormat(DATE_SQL).parse((String) sqlvalue);
        }
        if (Number.class.isInstance(sqlvalue)) {
            return new Date((Long) sqlvalue);
        }
        return null;
    }

    public static java.sql.Date toDate(Object sqlvalue) throws ParseException {
        if (sqlvalue == null) {
            return null;
        }
        if (java.sql.Date.class.isInstance(sqlvalue)) {
            return (java.sql.Date) sqlvalue;
        }
        if (Date.class.isInstance(sqlvalue)) {
            return new java.sql.Date(((Date) sqlvalue).getTime());
        }
        if (String.class.isInstance(sqlvalue)) {
            return new java.sql.Date(new SimpleDateFormat(DATE_SQL).parse((String) sqlvalue).getTime());
        }
        if (Number.class.isInstance(sqlvalue)) {
            return new java.sql.Date((Long) sqlvalue);
        }
        return null;
    }

    public static Timestamp toTimestamp(Object sqlvalue) throws ParseException {
        if (sqlvalue == null) {
            return null;
        }
        if (Timestamp.class.isInstance(sqlvalue)) {
            return (Timestamp) sqlvalue;
        }
        if (String.class.isInstance(sqlvalue)) {
            return new Timestamp(new SimpleDateFormat(DATE_SQL).parse((String) sqlvalue).getTime());
        }
        if (Number.class.isInstance(sqlvalue)) {
            return new Timestamp((Long) sqlvalue);
        }
        if (Date.class.isInstance(sqlvalue)) {
            return new Timestamp(((Date) sqlvalue).getTime());
        }
        return null;
    }

    public static Time toTime(Object sqlvalue) throws ParseException {
        if (sqlvalue == null) {
            return null;
        }
        if (Time.class.isInstance(sqlvalue)) {
            return (Time) sqlvalue;
        }
        if (Date.class.isInstance(sqlvalue)) {
            return new Time(((Date) sqlvalue).getTime());
        }
        if (String.class.isInstance(sqlvalue)) {
            return new Time(new SimpleDateFormat(DATE_SQL).parse((String) sqlvalue).getTime());
        }
        if (Number.class.isInstance(sqlvalue)) {
            if (sqlvalue instanceof Integer) {
                sqlvalue = ((Integer) sqlvalue).longValue();
            }
            return new Time((Long) sqlvalue);
        }
        return null;
    }

}
