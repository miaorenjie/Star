package xiyou.mobile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static xiyou.mobile.Bridge.p;

/**
 * Created by user on 2017/6/12.
 */

public class WriteThread extends Thread {

    private OutputStream os=null;
    private Socket s=null;
    private boolean alive=true;
    private LinkedBlockingQueue<byte[]> responses=new LinkedBlockingQueue<>();

    public WriteThread(Socket s)
    {
        this.s=s;
    }

    public void add(byte[] x)
    {
        p("add to que:"+new String(x)+"\n");
        responses.add(x);
    }

    public String getIp()
    {
        return s.getInetAddress().toString();
    }


    @Override
    public void run() {
        super.run();
        try {
            os=s.getOutputStream();
        } catch (IOException e) {
            p(e.toString());
            return ;
        }

        while (alive)
        {

            try {
                if (!Bridge.write(os,responses.take()))break;
            } catch (InterruptedException e) {
                p("when write response:"+e.toString());
            }
        }
    }

    public void dead()
    {
        alive=false;
        interrupt();
    }
}
