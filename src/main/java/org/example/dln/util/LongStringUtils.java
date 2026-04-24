package org.example.dln.util;

/**
 * Long 类型到字符串的统一转换工具。
 */
public final class LongStringUtils {
    /**
    * 禁止实例化工具类。
    */
    private LongStringUtils() {
    }

    /**
    * 将 Long 类型值转换为字符串。
     * @param value Long类型值
    */
    public static String toStringValue(Long value) {
        return value == null ? null : value.toString();
    }
}
