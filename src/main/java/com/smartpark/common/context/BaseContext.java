package com.smartpark.common.context;
//context：上下文工具类

//当前线程中存储和获取用户ID
/*    使用的业务逻辑：
    1.在用户登录后，将用户ID存入ThreadLocal
    2.在后续的业务处理中，任何地方都可以方便地获取当前用户ID
    3.在请求处理完成后，清除ThreadLocal中的数据*/

public class BaseContext {
    // 使用 ThreadLocal<Long> 来存储当前线程的ID值
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
