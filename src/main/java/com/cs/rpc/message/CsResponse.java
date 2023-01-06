package com.cs.rpc.message;

import lombok.*;

import java.io.Serializable;

/**
 * @author Acos
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CsResponse<T> implements Serializable {

    private String requestId;
    /**
     * response code
     */
    private Integer code;
    /**
     * response message
     */
    private String message;
    /**
     * response body
     */
    private T data;

    public static <T> CsResponse<T> success(T data, String requestId) {
        CsResponse<T> response = new CsResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    public static <T> CsResponse<T> fail(String message) {
        CsResponse<T> response = new CsResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

}
