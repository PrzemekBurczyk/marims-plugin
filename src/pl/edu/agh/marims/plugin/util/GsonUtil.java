package pl.edu.agh.marims.plugin.util;

import com.google.gson.*;
import com.google.gson.annotations.Expose;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GsonUtil {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    static {
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static Gson getGson() {
        return getGsonBuilder().create();
    }

    public static GsonBuilder getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy());
        gsonBuilder.addDeserializationExclusionStrategy(new DeserializationExclusionStrategy());
        gsonBuilder.registerTypeAdapter(Date.class, new DateSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
        return gsonBuilder;
    }

    private static class DeserializationExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            final Expose expose = fieldAttributes.getAnnotation(Expose.class);
            return expose != null && !expose.deserialize();
        }

    }

    private static class SerializationExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            final Expose expose = fieldAttributes.getAnnotation(Expose.class);
            return expose != null && !expose.serialize();
        }

    }

    private static class DateSerializer implements JsonSerializer<Date> {

        @Override
        public JsonElement serialize(Date date, Type type, JsonSerializationContext context) {
            String stringDate = simpleDateFormat.format(date);
            return new JsonPrimitive(stringDate);
        }
    }

    private static class DateDeserializer implements JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return deserialize(json.getAsJsonPrimitive().getAsString());
        }

        public static Date deserialize(String dateString) {
            Date date = null;
            try {
                date = simpleDateFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return date;
        }
    }

}
