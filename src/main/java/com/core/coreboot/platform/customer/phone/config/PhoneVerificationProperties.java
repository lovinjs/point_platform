package com.core.coreboot.platform.customer.phone.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.phone-verification")
public class PhoneVerificationProperties {
    private PhoneVerificationMode mode = PhoneVerificationMode.DISABLED;
    private Duration codeTtl = Duration.ofMinutes(5);
    private Duration resendInterval = Duration.ofMinutes(1);
    private int maxVerifyAttempts = 5;
    private int customerHourlyLimit = 5;
    private int customerDailyLimit = 10;
    private int phoneHourlyLimit = 5;
    private int phoneDailyLimit = 10;
    private int globalDailyLimit = 5000;

    public PhoneVerificationMode getMode() {
        return mode;
    }

    public void setMode(PhoneVerificationMode mode) {
        this.mode = mode;
    }

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    public int getCustomerHourlyLimit() {
        return customerHourlyLimit;
    }

    public void setCustomerHourlyLimit(int customerHourlyLimit) {
        this.customerHourlyLimit = customerHourlyLimit;
    }

    public int getCustomerDailyLimit() {
        return customerDailyLimit;
    }

    public void setCustomerDailyLimit(int customerDailyLimit) {
        this.customerDailyLimit = customerDailyLimit;
    }

    public int getPhoneHourlyLimit() {
        return phoneHourlyLimit;
    }

    public void setPhoneHourlyLimit(int phoneHourlyLimit) {
        this.phoneHourlyLimit = phoneHourlyLimit;
    }

    public int getPhoneDailyLimit() {
        return phoneDailyLimit;
    }

    public void setPhoneDailyLimit(int phoneDailyLimit) {
        this.phoneDailyLimit = phoneDailyLimit;
    }

    public int getGlobalDailyLimit() {
        return globalDailyLimit;
    }

    public void setGlobalDailyLimit(int globalDailyLimit) {
        this.globalDailyLimit = globalDailyLimit;
    }
}
