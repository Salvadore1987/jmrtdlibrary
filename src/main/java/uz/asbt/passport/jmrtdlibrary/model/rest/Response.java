package uz.asbt.passport.jmrtdlibrary.model.rest;

import lombok.Data;

@Data
public class Response<T> {

    private int code;
    private String message;
    private T response;

}
