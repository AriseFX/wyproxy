package com.wy.common.execution;

/**
 * 业务异常的封装
 *
 * @author wy
 * @date 2016年11月12日 下午5:05:10
 */
public class ServiceException extends RuntimeException {

    private final String errorMessage;

    public ServiceException( String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

}
