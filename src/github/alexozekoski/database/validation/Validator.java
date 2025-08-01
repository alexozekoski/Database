/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.alexozekoski.database.Log;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.model.ModelSerial;
import github.alexozekoski.database.model.ModelUtil;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author alexo
 */
public class Validator {

    public static final List<Validation> VALIDATIONS = new ArrayList();

    static {
        VALIDATIONS.add(new ColumnValidation());
        VALIDATIONS.add(new StringValidation());
        VALIDATIONS.add(new PrimitiveValidation());
        VALIDATIONS.add(new BooleanValidation());
        VALIDATIONS.add(new CustomValidation());
    }

    public static JsonObject validate(Model model, String... columns) {
        Field[] fields = ModelUtil.getValidateColumns(model.getClass());
        JsonObject validation = new JsonObject();
        Validator validator = new Validator(model);
        List<String> list = columns == null || columns.length == 0 ? null : Arrays.asList(columns);
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            String key = column.name().isEmpty() ? column.value() : column.name();
            if (list == null || list.contains(key)) {
                if (column.validate()) {
                    validator.getInvalids().clear();
                    validator.setField(field);
                    validator.setColumn(column);

                    if (!validator.validate()) {
                        validation.add(key, validator.toJson());
                        continue;
                    }
                    if (Model.class.isAssignableFrom(field.getType())) {
                        Model subModel = (Model) ModelUtil.getObject(model, field);
                        if (subModel != null) {
                            List<String> subList = null;
                            if (list != null) {
                                subList = new ArrayList<>(list.size());
                                for (String klist : list) {
                                    String nk = key + ".";
                                    if (klist.startsWith(nk)) {
                                        String r = klist.replace(nk, "");
                                        subList.add(r);
                                    }
                                }
                            }
                            JsonObject valSubmodel = subModel.validate(subList != null ? subList.toArray(new String[subList.size()]) : null);
                            if (valSubmodel != null) {
                                validation.add(key, valSubmodel);
                            }
                        }
                    }
                }
            }
        }
        return validation.keySet().isEmpty() ? null : validation;
    }

    private List<Invalid> invalids = new ArrayList<>();

    private Field field = null;

    private Model model;

    private Column column = null;

    public Validator(Model model) {
        this.model = model;
    }

    public List<Invalid> getInvalids() {
        return invalids;
    }

    public void setInvalids(List<Invalid> invalids) {
        this.invalids = invalids;
    }

    public void addInvalid(int code, String message) {
        addInvalid(code, message, null);
    }

    public void addInvalid(Invalid invalid) {
        this.invalids.add(invalid);
    }

    public void addInvalid(int code, String message, JsonElement extra) {
        invalids.add(new Invalid(code, message, extra));
    }

    public boolean validate() {
        try {
            Object value = field.get(model);

            if (value != null && (value.getClass().isArray() || List.class.isInstance(value))) {
                if (value.getClass().isArray()) {
                    int length = Array.getLength(value);
                    for (int i = 0; i < length; i++) {
                        Object valueUni = Array.get(value, i);
                        for (Validation validation : VALIDATIONS) {
                            validation.valid(model, field, column, valueUni, this);
                        }
                    }
                } else {
                    List values = (List) value;
                    for (Object valueUni : values) {
                        for (Validation validation : VALIDATIONS) {
                            validation.valid(model, field, column, valueUni, this);
                        }
                    }
                }
            } else {
                for (Validation validation : VALIDATIONS) {
                    validation.valid(model, field, column, value, this);
                }
            }
            model.onValidateColumn(getColumn(), value, this);
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return invalids.isEmpty();
    }

    public JsonArray toJson() {
        JsonArray json = new JsonArray();
        for (Invalid invalid : invalids) {
            json.add(invalid.toJson());
        }
        return json;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Column getColumn() {
        return column;
    }

    public void setColumn(Column column) {
        this.column = column;
    }
}
