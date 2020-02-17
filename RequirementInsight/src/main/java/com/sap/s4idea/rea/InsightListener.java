package com.sap.s4idea.rea;

import javax.servlet.http.HttpServlet;
import java.util.Timer;

public class InsightListener extends HttpServlet {
    public InsightListener() {
        super();
    }

    public void init() {
        try{
            Timer timer = new Timer();
//        timer.schedule(new TwitterTask(), 1000, 30000);
            timer.schedule(new WechatTask(),1000,15000);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
