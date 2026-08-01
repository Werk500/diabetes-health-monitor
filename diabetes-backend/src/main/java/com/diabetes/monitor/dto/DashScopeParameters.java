package com.diabetes.monitor.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class DashScopeParameters {
    @JsonProperty("result_format")
    private String resultFormat = "message";
    @JsonProperty("incremental_output")
    private Boolean incrementalOutput = true;
    private Double temperature = 0.7;
    @JsonProperty("top_p")
    private Double topP = 0.8;
    @JsonProperty("max_tokens")
    private Integer maxTokens = 2000;
}