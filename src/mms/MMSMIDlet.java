/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mms;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.*;
import javax.wireless.messaging.*;

/**
 * @author Administrator
 */
public class MMSMIDlet extends MIDlet implements CommandListener, MessageListener, Runnable {

    private MMSSender mSender = null;
    private Thread mReceiver = null;
    private Command mExitCommand = new Command("Exit", Command.EXIT, 2);
    private Command mRedCommand = new Command("Send red", Command.SCREEN, 1);
    private Command mBlueCommand = new Command("Send Blue", Command.SCREEN, 1);
    protected static final String RED_IMAGE = "/mms/red.png";
    protected static final String BLUE_IMAGE = "/mms/blue.png";
    protected static final String DEFAULT_IMAGE = "/mms/wait.png";
    private Display mDisplay = null;
    protected ImageItem mColorSquare = null;
    protected Image mInitialImage = null;
    private String mAppID = "MMSMIDlet";
    private TextField mNumberEntry = null;
    private Form mForm = null;
    private Integer mMonitor = new Integer(0);
    private boolean mEndNow = false;
    private MessageConnection conn = null;
    protected int mMsgAvail = 0;

    public MMSMIDlet() {
        mSender = MMSSender.getInstance();
    }

    public void startApp() {
        if (mForm == null) {
            mForm = new Form(mAppID);
            mNumberEntry = new TextField("Connect To:", null, 256, TextField.PHONENUMBER);
            try {
                mInitialImage = Image.createImage(DEFAULT_IMAGE);
            } catch (Exception e) {
                System.out.println("start app err");
                e.printStackTrace();
            }
            mColorSquare = new ImageItem(null, mInitialImage, ImageItem.LAYOUT_DEFAULT, "waiting for image");
            mForm.append(mNumberEntry);
            mForm.append(mColorSquare);
            mForm.addCommand(mRedCommand);
            mForm.addCommand(mExitCommand);
            mForm.addCommand(mBlueCommand);
            mForm.setCommandListener(this);
        }
        Display.getDisplay(this).setCurrent(mForm);
        try {
            conn = (MessageConnection) Connector.open("mms://:" + mAppID);
            conn.setMessageListener(this);
        } catch (Exception e) {
            System.out.println("" + e);
        }
        if (conn != null) {
            startReceive();
        }
    }

    protected void pauseApp() {
        mEndNow = true;
        try {
            conn.setMessageListener(null);
            conn.close();
        } catch (IOException e) {
            System.out.println("pause app caught");
            e.printStackTrace();
        }
    }

    protected void destroyApp(boolean unconditional) {
        mEndNow = true;
        try {
            conn.close();
        } catch (IOException e) {
            System.out.println("destroy app caught");
            e.printStackTrace();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == mExitCommand) {
            if (!mSender.isSending()) {
                destroyApp(true);
                notifyDestroyed();
            }
        } else if (c == mRedCommand) {
            String dest = mNumberEntry.getString();
            if (dest.length() > 0) {
                mSender.sendMsg(dest, mAppID, RED_IMAGE);
            }
        } else if (c == mBlueCommand) {
            String dest = mNumberEntry.getString();
            if (dest.length() > 0) {
                mSender.sendMsg(dest, mAppID, BLUE_IMAGE);
            }
        }
    }

    public void notifyIncomingMessage(MessageConnection msgConn) {
        if (msgConn == conn) {
            getMessage();
        }
    }

    public void run() {
        Message msg = null;
        String msgReceived = null;
        Image receivedImage = null;
        mMsgAvail = 0;
        while (!mEndNow) {
            synchronized (mMonitor) {
                //enter monitor
                if (mMsgAvail <= 0) {
                    try {
                        mMonitor.wait();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                mMsgAvail--;
            }
            try {
                msg = conn.receive();
                if (msg instanceof MultipartMessage) {
                    MultipartMessage mpm = (MultipartMessage) msg;
                    MessagePart[] parts = mpm.getMessageParts();
                    System.out.println(parts.length);
                    if (parts != null) {
                        for (int i = 0; i < parts.length; i++) {
                            MessagePart mp = parts[i];
                            byte[] ba = mp.getContent();
                            receivedImage = Image.createImage(ba, 0, ba.length);
                            mColorSquare.setImage(receivedImage);

                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    private void getMessage() {
        synchronized (mMonitor) {
            mMsgAvail++;
            mMonitor.notify();
        }
    }

    private void startReceive() {
        mEndNow = false;
        // start receive thread
        mReceiver = new Thread(this);
        mReceiver.start();

    }
}
