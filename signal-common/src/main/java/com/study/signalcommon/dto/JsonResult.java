package com.study.signalcommon.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/8/1.
 *
 * @author guijiamin.
 */
public class JsonResult implements Serializable {
    private int flag;
    private Object data;

    public JsonResult() {

    }

    public JsonResult(int flag) {
        this.flag = flag;
    }

    public JsonResult(int flag, Object data) {
        this.flag = flag;
        this.data = data;
    }

    public int getFlag() {
        return this.flag;
    }

    public JsonResult setFlag(int flag) {
        this.flag = flag;
        return this;
    }

    public Object getData() {
        if (this.data == null) {
            this.data = new HashMap<String, Object>();
        }
        return this.data;
    }

    public JsonResult setData(Object data) {
        if (this.flag > 0) {
            this.data = data;
        }
        return this;
    }

    /**
     * 向Map类型data中插入数据
     *
     * @param name
     * @param value
     */
    public JsonResult putData(String name, Object value) {
        Map<String, Object> map = this.initData(HashMap.class);
        map.put(name, value);
        return this;
    }

    /**
     * 向List类型data中添加数据
     *
     * @param object
     * @return
     */
    public JsonResult addData(Object object) {
        List<Object> list = this.initData(ArrayList.class);
        list.add(object);
        return this;
    }

    @SuppressWarnings("unchecked")
    private <E> E initData(Class<?> klass) {
        if (this.data == null) {
            try {
                this.data = klass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return (E) this.data;
    }

}

