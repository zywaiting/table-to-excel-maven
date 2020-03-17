package xin.zhuyao.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhuyao
 * @description 属性注解用于获取表字段意思
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldName {

    /**
     * 名称
     * @return
     */
    String name() default "";

    /**
     * 信息
     * @return
     */
    String message();
}
