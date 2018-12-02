package io.norland.response;


import lombok.Data;

@Data
public class ActionAndModel {
    /**
     * response 回复
     * nrsp或者null则不回复
     * rdt跳转（未实现）
     */
    private String action;
    /**
     * 返回给客户端的数据流
     */
    private Object value;

    private ActionAndModel() {
    }

    public static ActionAndModel noResponseModel() {
        ActionAndModel model = new ActionAndModel();
        model.setAction(Actions.NO_RESPONSE);
        return model;
    }

    public static ActionAndModel responseModel(Object value) {
        ActionAndModel model = new ActionAndModel();
        model.setAction(Actions.RESPONSE);
        model.setValue(value);
        return model;
    }
}
