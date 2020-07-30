package com.cyf.xiaoxuedi.response;

public class CommonReturnType {
    //表明对应请求的返回处理结果："success"或"fail"
    private String code;
    private String info;
    private Object data;

    //如果code=10000，则data内返回前端需要的json数据

    //
    public static CommonReturnType create(Object result) {
        return CommonReturnType.create(result, "10000","success");
    }

    public static CommonReturnType create(Object result, String code, String info) {
        CommonReturnType type = new CommonReturnType();
        type.setData(result);
        type.setCode(code);
        type.setInfo(info);
        return type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String status) {
        this.code = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getInfo() { return info; }

    public void setInfo(String info) { this.info = info; }
}
