package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注释，标识某个方法需要进行功能字段自动填充处理
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {

    //数据库操作类型：UPDATE INSERT只有这两种操作字段update，create相关字段才需要修改
    OperationType value();
}
