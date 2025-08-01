package uz.asbt.passport.jmrtdlibrary.model.rest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Response<T> {

    private int code;
    private String message;
    private T response;

}
