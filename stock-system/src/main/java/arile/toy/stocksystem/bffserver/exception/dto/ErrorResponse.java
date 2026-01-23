package arile.toy.stocksystem.bffserver.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(HttpStatus status, String message){
}
