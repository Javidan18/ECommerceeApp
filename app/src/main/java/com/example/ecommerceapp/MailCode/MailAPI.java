package com.example.ecommerceapp.MailCode;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@SuppressLint("StaticFieldLeak")
public class MailAPI extends AsyncTask<Void, Void, Void> {

    Session session;
    String emailAddress, subjectOfMail, message, otpCode;

    public MailAPI(String emailAddress, String subjectOfMail, String message, String otpCode) {
        this.emailAddress = emailAddress;
        this.subjectOfMail = subjectOfMail;
        this.message = message;
        this.otpCode = otpCode;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(v);
    }

    @Override
    protected Void doInBackground(Void... params) {
        Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");


        session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("noreply.stis@gmail.com", "ybgaskpymbaeaujx");
            }
        });

        try {
            MimeMessage mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress("noreply.stis@gmail.com"));
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
            mm.setSubject(subjectOfMail);
            if (otpCode.equals("-1")) {
                mm.setText(message);
            } else {
                mm.setText(message + otpCode);
            }
            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
