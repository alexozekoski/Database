/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 * @author alexo
 */
public class Invalid {

    public static final int CODE_MIN_LENGTH = 14;

    public static final int CODE_MAX_LENGTH = 15;

    public static final int CODE_MIN_VALUE = 10;

    public static final int CODE_MAX_VALUE = 11;

    public static final int CODE_INVALID_VALUE = 12;

    public static final int CODE_NOT_EXISTS = 13;

    public static final int CODE_REQUIRED = 1;

    public static final int CODE_MAX_STRING = 2;

    public static final int CODE_UNIQUE = 3;

    public static final int CODE_FOREIGN_KEY = 4;

    public static Invalid NOTNULL = new Invalid(CODE_REQUIRED, "Required");

    public static Invalid MAX_STRING = new Invalid(CODE_MAX_STRING, "Max length");

    public static Invalid UNIQUE = new Invalid(CODE_UNIQUE, "Already exists");

    public static Invalid FOREIGN_KEY = new Invalid(CODE_FOREIGN_KEY, "Not exists in table");

    public static Invalid INVALID_VALUE = new Invalid(CODE_INVALID_VALUE, "Invalid value");

    public static Invalid NOT_EXISTS = new Invalid(CODE_INVALID_VALUE, "Value not exists");

    private int code;
    private String message;
    private JsonElement meta;

    public Invalid(int code, String message) {
        this(code, message, null);
    }

    public Invalid(int code, String message, JsonElement meta) {
        this.code = code;
        this.message = message;
        this.meta = meta;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JsonElement getMeta() {
        return meta;
    }

    public void setMeta(JsonElement meta) {
        this.meta = meta;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (message != null) {
            json.addProperty("message", message);
        }
        if (meta != null) {
            json.add("meta", meta);
        }
        json.addProperty("code", code);

        return json;
    }
}
