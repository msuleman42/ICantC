package com.example.icantc;


import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class sendLocation {

    private String longtitude;
    private String latitude;

    private static final String ACCOUNT_SID = "AC4be07569e18112715b6707ad4299ed57";
    private static final String AUTH_TOKEN = "329a106eb4cd850ba4be669c93f7f374";

    public sendLocation(String longtitude, String latitude)
    {
        this.longtitude = longtitude;
        this.latitude = latitude;
    }

    public void sendMessage(){
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message = Message.creator(new PhoneNumber("+447938866157"), new PhoneNumber("+18023922280"),
                "This is a message from RollSafe Jennie has sent you there last location " + longtitude + " " + latitude).create();

        System.out.println(message.getSid());
    }

//    public void sendCurrentLocation()
//    {
//        sendLocation location = new sendLocation(String.valueOf(121), String.valueOf(334));
//
//        location.sendMessage();
//    }
}
