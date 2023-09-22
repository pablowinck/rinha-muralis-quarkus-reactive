package br.com.pablowinter;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ErrorPageResponseExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
//        Logger.getLogger("ErrorPageResponseExceptionMapper").severe(exception.getMessage());
//        exception.printStackTrace();
        return Response.status(400).entity("").build();
    }
}
