package io.petcare.backend.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "petcare.ai")
public class PetCareAiProperties {

    private boolean enabled;
    private String provider = "openai-compatible";
    private String model = "qwen3.7-plus";
    private String baseUrl;

    @DecimalMin("0.0")
    @DecimalMax(value = "1.999")
    private double temperature = 0.3;

    @Min(1)
    @Max(50)
    private int maxContextMessages = 10;

    @Valid
    private Tool tool = new Tool();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxContextMessages() {
        return maxContextMessages;
    }

    public void setMaxContextMessages(int maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool == null ? new Tool() : tool;
    }

    public static class Tool {

        @Min(1)
        @Max(30)
        private int maxHealthDays = 30;

        @Min(1)
        @Max(365)
        private int maxWeightDays = 90;

        public int getMaxHealthDays() {
            return maxHealthDays;
        }

        public void setMaxHealthDays(int maxHealthDays) {
            this.maxHealthDays = maxHealthDays;
        }

        public int getMaxWeightDays() {
            return maxWeightDays;
        }

        public void setMaxWeightDays(int maxWeightDays) {
            this.maxWeightDays = maxWeightDays;
        }
    }
}
