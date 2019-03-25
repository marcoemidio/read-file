package pt.timestamp.readfile.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Marco Emidio
 */
@Data
@AllArgsConstructor
public class JsonResponse {
    
    @JsonProperty("filename")
    String fileName;
    @JsonProperty("signedContent")
    String signedContent;
    @JsonProperty("status")
    String status;

}