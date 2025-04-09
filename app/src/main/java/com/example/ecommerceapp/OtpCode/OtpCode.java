package com.example.ecommerceapp.OtpCode;

import com.example.ecommerceapp.MailCode.MailAPI;

public class OtpCode {

    public String invoke(String emailAddress) {
        String subjectOfMail = "OTP Code";
        String message = "Your OTP code is ";
        String otpCode = String.valueOf((int) (Math.random() * 9000 + 1000));

        MailAPI api = new MailAPI(emailAddress, subjectOfMail, message, otpCode);
        api.execute();

        return otpCode;
    }
}
