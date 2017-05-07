package com.leaves.app.shareme.gson;

import com.google.gson.Gson;
import com.leaves.app.shareme.bean.Message;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Leaves on 2017/5/7.
 */

public final class GsonUtils {
    public static <T> Message<T> fromJsonObject(Gson gson, String s, Class<T> clazz) {
        Type type = new ParameterizedTypeImpl(Message.class, new Class[]{clazz});
        return gson.fromJson(s, type);
    }

    public static <T> Message<List<T>> fromJsonArray(Gson gson, String s, Class<T> clazz) {
        // 生成List<T> 中的 List<T>
        Type listType = new ParameterizedTypeImpl(List.class, new Class[]{clazz});
        // 根据List<T>生成完整的Result<List<T>>
        Type type = new ParameterizedTypeImpl(Message.class, new Type[]{listType});
        return gson.fromJson(s, type);
    }
}
