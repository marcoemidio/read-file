package pt.timestamp.readfile.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author Marco Emidio
 */
@Data
public class JsonRequest {
    
    @JsonProperty("filename")
    String fileName;
    @JsonProperty("content")
    String content;

}