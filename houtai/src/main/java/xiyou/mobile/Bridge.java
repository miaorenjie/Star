package xiyou.mobile;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.parser.JSONParser;

/**
 * Created by user on 2017/6/11.
 */

public class Bridge implements Runnable{

    public static final int METHOD_INVOKE=0;
    public static final int METHOD_CONNECT=1;
    public static final String RESULT="result";
    public static final String OK="ok";
    public static final String FAIL="failed";
    public static final String METHOD="method";
    public static final String PARAM="param";
    public static final String CLASS="cls";
    public static final String REQUESTCODE="request";
    public static final String RESPONSECODE="response";
    public static final String CALLER="caller";
    public static final String IP="ip";
    public static final String X="x";
    public static final String Y="y";
    public static final String DATA="data";
    public static final String POSITION="position";

    public static final int ADDFRIEND=0;
    public static final int PERMITADD=1;
    public static final int GETIP=2;
    public static final int ACTION_DOWN=3;
    public static final int ACTION_MOVE=4;
    public static final int REQUESTSYNC=5;
    public static final int PERMITSYNC=6;
    public static final int PERMITCONTROL=7;
    public static final int REQUESTCONTROL=8;
    public static final int SENDSCREENSIZE=9;
    public static final int ENDSYNC=10;
    public static final int SENDDATA=11;
    public static final int SEEK=12;
    public static final int REFUSEDSYNC=13;
    public static final int REFUSEDCONTROL=14;
    public static final int REFUSEDADDFRIEND=15;
    public static final int CLEARSCREEN=16;
    public static final int STARTPAUSE=17;

    public static int port=12543;
    private LinkedBlockingQueue<Socket> sockets=new LinkedBlockingQueue<>();
    private HashMap<String,WriteThread> wts=new HashMap<>();

    public Bridge()
    {}

    public void start()
    {
        try {
            ServerSocket ss=new ServerSocket(port);
            while (true)
            {
                sockets.add(ss.accept());
                new Thread(this).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
            p(e.toString());
        }
    }


    public static void p(String s)
    {
        System.out.println(s);
    }

    @Override
    public void run() {
        final Socket s=sockets.remove();
        boolean login=false;
        String mName=null;
        int datalen=0;
        int readcount=0;
        byte[] cc=null;
        int c;
        WriteThread wt=null;
        InputStream is=null;
        try
        {

            try {
                is=s.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            wt=new WriteThread(s);
            wt.start();

            while (true)
            {
                p("wait:");
                cc=new byte[4];
                if (!read(is,cc)) break;
                for (int i=0;i<4;i++)
                    datalen=datalen<<8|(0xff&cc[i]);

                p("recv data_len"+datalen);

                try {
                    c=is.read();
                    if (c==-1)
                        break ;
                } catch (IOException e) {
                    p(e.toString());
                    break;
                }
                if (c==METHOD_INVOKE)
                {
                    p("get:");
                    cc=new byte[datalen];
                    if (!read(is,cc))break;
                    String string= null;
                    try {
                        string = new String(cc,"utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    p(string);
                    JSONObject o= null;
                    o = JSONObject.fromObject(string);

                    JSONArray param=o.getJSONArray(PARAM);
                    Object params[]=new Object[param.size()];
                    for (int i=0;i<params.length;i++)
                    {
                        params[i]=param.get(i);
                    }
                    JSONObject r=invoke(o.getString(CLASS),o.getString(METHOD),params);
                    if (o.getString(METHOD).equals("login")&&r.getString(RESULT).equals(User.LOG_SUCCES)) {
                        if (login)
                        {
                            wts.remove(mName);
                            User.logout(mName);
                        }

                        login = true;
                        mName=r.getString(User.NAME);
                        wts.put(mName,wt);

                        LinkedBlockingQueue<byte[]> oldMsg=User.getMsg(mName);
                        while (!oldMsg.isEmpty())
                            try {
                                wt.add(oldMsg.take());
                            } catch (InterruptedException e) {
                                p(e.toString());
                            }
                    }
                    p("response:\n"+r.toString());
                    try {
                        wt.add(r.toString().getBytes("utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }else if (c==METHOD_CONNECT)
                {
                    if (!login)
                    {
                        p("login first");
                        continue;
                    }
                    p("connect:");
                    cc=new byte[datalen];
                    if (!read(is,cc))break;
                    String string= null;
                    try {
                        string = new String(cc,"utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    p(string);
                    JSONObject o=JSONObject.fromObject(string);
                    JSONObject r=connect(mName,o);
                    p(mName+"response:\n"+r.toString());
                    try {
                        wt.add(r.toString().getBytes("utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e)
        {

        }finally {

        }


        if (login)
        User.logout(mName);
        p("end"+mName);
        wts.remove(wt);
        wt.dead();
    }

    private JSONObject connect(String caller,JSONObject o)
    {
        JSONObject r=null;
        switch (o.getInt(REQUESTCODE))
        {
            case ADDFRIEND:
                r=addFriend(caller,o.getString(User.NAME));
                break;
            case PERMITADD:
                r=permittAdd(caller,o.getString(User.NAME));
                break;
            case ACTION_DOWN:
                r=sendAction(caller,o.getInt(REQUESTCODE),o.getInt(X),o.getInt(Y),o.getString(User.NAME));
                break;
            case ACTION_MOVE:
                r=sendAction(caller,o.getInt(REQUESTCODE),o.getInt(X),o.getInt(Y),o.getString(User.NAME));
                break;
            case REQUESTSYNC:
                r=requestSync(caller,o.getString(User.NAME));
                break;
            case PERMITSYNC:
                r=permitSync(caller,o.getString(User.NAME));
                break;
            case REQUESTCONTROL:
                r=requestControl(caller,o.getString(User.NAME));
                break;
            case PERMITCONTROL:
                r=permitControl(caller,o.getString(User.NAME));
                break;
            case SENDSCREENSIZE:
                r=sendScreenSize(caller,o.getString(User.NAME),o.getInt(X),o.getInt(Y));
                break;
            case ENDSYNC:
                r=endSync(caller,o.getString(User.NAME));
                break;
            case SENDDATA:
                r=sendData(caller,o.getString(User.NAME),o.getString(DATA));
                break;
            case SEEK:
                r=seek(caller,o.getString(User.NAME),o.getInt(POSITION));
                break;
            case REFUSEDCONTROL:
                r=refusedControl(caller,o.getString(User.NAME));
                break;
            case REFUSEDADDFRIEND:
                r=refusedAddFriend(caller,o.getString(User.NAME));
                break;
            case REFUSEDSYNC:
                r=refusedSync(caller,o.getString(User.NAME));
                break;
            case CLEARSCREEN:
                r=clearScreen(caller,o.getString(User.NAME));
                break;
            case STARTPAUSE:
                r=startPause(caller,o.getString(User.NAME));
                break;
        }

        return r;
    }

    private JSONObject startPause(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getStartPauseMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject clearScreen(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getClearScreenMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject refusedSync(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getRefusedSyncMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject refusedControl(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getRefusedControlMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject refusedAddFriend(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getRefusedAddFriendMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject seek(String caller,String name,int position)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getSeekMsg(caller,name,position);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject sendData(String caller,String name,String data)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getSendDataMsg(caller,name,data);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject addFriend(String caller,String name)
    {
        JSONObject o=new JSONObject();
        if (caller.equals("15829333942")||(!caller.equals(name)&&User.exist_name(name).get(RESULT).equals(User.EXIST)&&User.exist_friend(caller,name).getString(RESULT).equals(User.NOEXIST)))
            o.put(RESULT,OK);
        else {
            o.put(RESULT, FAIL);
            return o;
        }
        JSONObject r=User.getAddFriendMsg(caller,name);
        byte[] msg= new byte[0];
        try {
            msg = r.toString().getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (wts.containsKey(name))
        {
            wts.get(name).add(msg);
        }else
        {
            User.addMsg(name,msg);
        }

        return o;
    }

    private JSONObject permittAdd(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getPermittAddMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                User.addMsg(name,msg);
            }
            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject sendAction(String caller,int action,int x,int y,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getSendActionMsg(caller,action,x,y,name);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject requestSync(String caller,String name)
    {
        JSONObject o=new JSONObject();

        JSONObject r=User.getRequestSyncMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= null;
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                //User.addMsg(name,msg);
            }
            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject permitSync(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getPermitSyncMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= null;
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                //User.addMsg(name,msg);
            }
            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject requestControl(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getRequestControl(caller,name);
        if (r!=null)
        {
            byte[] msg= null;
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                //User.addMsg(name,msg);
            }
            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject permitControl(String caller,String name)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getPermitControl(caller,name);
        if (r!=null)
        {
            byte[] msg= null;
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                //User.addMsg(name,msg);
            }
            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject sendScreenSize(String caller,String name,int w,int h)
    {
        JSONObject o=new JSONObject();


        JSONObject r=User.getSendScreenSizeMsg(caller,name,w,h);
        if (r!=null)
        {
            byte[] msg= new byte[0];
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                o.put(RESULT,FAIL);
                return o;
            }

            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    private JSONObject endSync(String caller,String name)
    {
        JSONObject o=new JSONObject();

        JSONObject r=User.getEndSyncMsg(caller,name);
        if (r!=null)
        {
            byte[] msg= null;
            try {
                msg = r.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (wts.containsKey(name))
            {
                wts.get(name).add(msg);
            }else
            {
                //User.addMsg(name,msg);
            }
            o.put(RESULT,OK);
        }else
        {
            o.put(RESULT,FAIL);
        }
        return o;
    }

    public JSONObject invoke(String cls,String method,Object ...params)
    {
        JSONObject r=null;
        Class[] types=new Class[params.length];
        for (int i=0;i<types.length;i++)
            types[i]=params[i].getClass();
        try {
            Method m=Class.forName(cls).getDeclaredMethod(method,types);
            m.setAccessible(true);
            r=(JSONObject) m.invoke(null,params);
        } catch (NoSuchMethodException e) {
            p(e.toString());
        } catch (ClassNotFoundException e) {
            p(e.toString());
        } catch (InvocationTargetException e) {
            Throwable ee=e.getCause();
            p(ee.toString());
            while (ee.getCause()!=null)
            {
                ee=ee.getCause();
                p(ee.toString());
            }
        } catch (IllegalAccessException e) {
            p(e.toString());
        }

        return r;
    }

    public static boolean read(InputStream is,byte[] cc)
    {
        int readcount=0;
        try {
            int x=0;
            while (true)
            {
                x=is.read(cc,readcount,cc.length-readcount);
                if (x==-1)
                    return false;
                readcount+=x;
                if (readcount==cc.length)
                    break;
            }
        } catch (IOException e) {
            p(e.toString());
            return false;
        }
        return true;
    }

    public static boolean write(OutputStream os,byte[] cc)
    {
        try {
            int datalen=cc.length;
            byte []len_byte=new byte[4];
            for (int i=0;i<4;i++)
            {
                len_byte[3-i]= (byte) ((byte)datalen&(byte)0xff);
                datalen=datalen>>8;
            }
            os.write(len_byte);
            os.write(cc,0,cc.length);
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

}
