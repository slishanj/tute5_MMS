/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mms;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.MessagePart;
import javax.wireless.messaging.MultipartMessage;

/**
 *
 * @author Administrator
 */
class MMSSender implements Runnable {

    private static MMSSender inst = new MMSSender();
    private String mAppID = null;
    private String mReceiver = null;
    private String mImageToSend = null;
    private boolean mSending = false;

    private MMSSender() {
    }

    public static MMSSender getInstance() {

        return inst;
    }

    public void sendMsg(String rcvr, String appid, String img) {
        if (mSending) {
            return;
        }
        mReceiver = rcvr;
        mAppID = appid;
        mImageToSend = img;
        Thread th = new Thread(this);
        th.start();
    }

    public boolean isSending() {
        return mSending;
    }
//send color image

    public void run() {
        mSending = true;
        try {
            sendMMS();
        } catch (Exception e) {
            System.out.println(e);
        }
        mSending = false;
    }

    private void sendMMS() {
       String address = "mms://"+mReceiver+":"+mAppID;
        MessageConnection conn = null;
        try {
            /** open message connection*/
            conn = (MessageConnection) Connector.open(address);
            MultipartMessage mpMessage = (MultipartMessage) conn.newMessage(MessageConnection.MULTIPART_MESSAGE);
            mpMessage.setSubject("MMSMIDlet image");
            InputStream is = getClass().getResourceAsStream(mImageToSend);
            byte[] bImage = new byte[is.available()];
            is.read(bImage);
            mpMessage.addMessagePart(new MessagePart(bImage, 0, bImage.length, "image/png", "id1",null,null));
            conn.send(mpMessage);
        } catch (Throwable t) {
            System.out.println("sending caught");
            System.out.println(t);
        }finally{
            if(conn!=null){
                try {
                    conn.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }
}
