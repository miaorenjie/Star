package xiyou.mobile;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.omg.PortableInterceptor.NON_EXISTENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.print.attribute.standard.MediaSize;

import static xiyou.mobile.Bridge.DATA;
import static xiyou.mobile.Bridge.OK;
import static xiyou.mobile.Bridge.POSITION;
import static xiyou.mobile.Bridge.RESULT;
import static xiyou.mobile.Bridge.X;
import static xiyou.mobile.Bridge.Y;
import static xiyou.mobile.Bridge.p;

/**
 * Created by user on 2017/6/11.
 */

public class User {

    public static final String FRIENDS="friends_";
    public static final String FROM="user";
    public static final String USR="usrname";
    public static final String NAME="name";
    public static final String PASSWD="passwd";
    public static final String LV="lv";
    public static final String SEX="sex";
    public static final String AGE="age";
    public static final String ID="id";
    public static final String LOG_FAIL="fail";
    public static final String HASLOGED="hasLoged";
    public static final String LOG_SUCCES="success";
    public static final String EXIST="exist";
    public static final String NOEXIST="no_exist";
    public static final String EXIST_NAME="exist_name";
    public static final String EXIST_USR="exist_usr";
    public static final String ONLINE="online";

    public static final String METHOD_REGISTER="user_register";
    public static final String METHOD_LOGIN="user_login";

    private static HashMap<String,Boolean> olUsers=new HashMap<>();
    private static HashMap<String,LinkedBlockingQueue<byte[]>> msgs=new HashMap<>();
    private static HashMap<String,ArrayList<String>> friends=new HashMap<>(),waitFriends=new HashMap<>();
    private static HashMap<String ,String> ips=new HashMap<>(),prepareSync=new HashMap<>();

    public static JSONObject login(String usrname,String passwd)
    {
        JSONObject o=new JSONObject();
        ArrayList<ArrayList> x=SQLUtil.query(new String[]{NAME,SEX,LV,AGE},FROM,USR+" = '"+usrname+"' and "+PASSWD+" = '"+passwd+"'",null);

        if (x==null||x.size()==0)
        {
            o.put(RESULT,LOG_FAIL);
            return o;
        }

        if (olUsers.containsKey(x.get(0).get(0)))
        {
            o.put(RESULT,HASLOGED);
            return o;
        }
        ArrayList c=x.get(0);

        o.put(RESULT,LOG_SUCCES);
        o.put(NAME,c.get(0));
        o.put(SEX,c.get(1));
        o.put(LV,c.get(2));
        o.put(AGE,c.get(3));
        olUsers.put((String)c.get(0),true);
        if (!friends.containsKey((String)c.get(0)))
            initFriends((String)c.get(0));
        return o;
    }

    private static void initFriends(String name)
    {
        ArrayList<ArrayList> x=SQLUtil.query(new String []{NAME},FRIENDS+name,null,null);
        if (!friends.containsKey(name))
            friends.put(name,new ArrayList<String>());
        ArrayList <String> r=friends.get(name);
        for (int i=0;i<x.size();i++)
        {
            r.add((String)x.get(i).get(0));
        }
    }

    public static JSONObject setIp(String name,String ip)
    {
        ips.put(name,ip);
        JSONObject r=new JSONObject();
        r.put(RESULT,OK);
        return r;
    }

    public static JSONObject getIp(String name)
    {
        JSONObject r=new JSONObject();
        r.put(RESULT,OK);
        if (ips.containsKey(name))
            r.put(Bridge.IP,ips.get(name));
        else
            r.put(Bridge.IP,"");
        return r;
    }


    public static JSONObject register(String usrname,String passwd,String name)
    {
        JSONObject o=new JSONObject();
        if (exist_name(name).get(RESULT).equals(EXIST))
        {
            o.put(RESULT,EXIST_NAME);
            return o;
        }

        if (exist_nusr(usrname).get(RESULT).equals(EXIST))
        {
            o.put(RESULT,EXIST_USR);
            return o;
        }

        SQLUtil.insert(new String[]{USR,NAME,PASSWD},FROM,new Object[]{usrname,name,passwd});
        SQLUtil.exec("create table "+FRIENDS+name+"(name text not null);");

        o.put(RESULT,OK);
        return o;
    }

    public static void addMsg(String name,byte []c)
    {
        if (!msgs.containsKey(name))
        {
            msgs.put(name,new LinkedBlockingQueue<byte[]>());
        }
        msgs.get(name).add(c);
    }

    public static LinkedBlockingQueue<byte[]> getMsg(String name)
    {
        if (!msgs.containsKey(name))
        {
            msgs.put(name,new LinkedBlockingQueue<byte[]>());
        }
        return msgs.get(name);
    }

    public static JSONObject friendList(String name)
    {
        JSONObject r=new JSONObject();
        if (!friends.containsKey(name))
            initFriends(name);
        ArrayList<String> allFri=friends.get(name);
        JSONArray ar=new JSONArray();
        for (int i=0;i<allFri.size();i++)
        {
            JSONObject o=new JSONObject();
            o.put(NAME,allFri.get(i));
            o.put(ONLINE,olUsers.containsKey(allFri.get(i)));
            ar.add(o);
        }
        r.put(RESULT,ar);
        return r;
    }

    public static JSONObject exist_name(String name)
    {
        JSONObject o=new JSONObject();
        if (SQLUtil.query(new String[]{NAME},FROM,NAME+" = '"+name+"'",null).size()!=0)
            o.put(RESULT,EXIST);
        else
            o.put(RESULT, NOEXIST);

        return o;
    }

    public static JSONObject exist_nusr(String name)
    {
        JSONObject o=new JSONObject();
        if (SQLUtil.query(new String[]{NAME},FROM,USR+" = '"+name+"'",null).size()!=0)
            o.put(RESULT,EXIST);
        else
            o.put(RESULT, NOEXIST);

        return o;
    }

    public static JSONObject exist_friend(String caller,String name)
    {
        JSONObject o=new JSONObject();
        if (SQLUtil.query(new String[]{NAME},FRIENDS+caller,NAME+" = '"+name+"'",null).size()!=0)
            o.put(RESULT,EXIST);
        else
            o.put(RESULT, NOEXIST);

        return o;
    }

    public static void logout(String name)
    {
        ips.remove(name);
        olUsers.remove(name);
        prepareSync.remove(name);
    }

    public static void addFriendNoCheck(String me,String ta)
    {
        SQLUtil.insert(new String[]{NAME},FRIENDS+me,new Object[]{ta});
    }

    public static JSONObject getAddFriendMsg(String caller,String name)
    {
        if (!waitFriends.containsKey(caller))
            waitFriends.put(caller,new ArrayList<String>());

        if (!waitFriends.get(caller).contains(name))
        waitFriends.get(caller).add(name);
        JSONObject r=new JSONObject();
        r.put(Bridge.RESPONSECODE,Bridge.ADDFRIEND);
        r.put(Bridge.CALLER,caller);
        return r;
    }

    public static JSONObject getPermittAddMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (waitFriends.containsKey(name)&&waitFriends.get(name).contains(caller))
        {
            waitFriends.get(name).remove(caller);
            if (!friends.containsKey(name))
                initFriends(name);
            if (!friends.get(name).contains(caller))
            {
                friends.get(name).add(caller);
                friends.get(caller).add(name);
                addFriendNoCheck(caller,name);
                addFriendNoCheck(name,caller);
            }
            r.put(Bridge.RESPONSECODE,Bridge.PERMITADD);
            r.put(Bridge.CALLER,caller);

        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getRefusedAddFriendMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (waitFriends.containsKey(name)&&waitFriends.get(name).contains(caller))
        {
            waitFriends.get(name).remove(caller);
            r.put(Bridge.RESPONSECODE,Bridge.REFUSEDADDFRIEND);
            r.put(Bridge.CALLER,caller);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getRequestSyncMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.REQUESTSYNC);
            r.put(Bridge.CALLER,caller);
            prepareSync.put(caller,null);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getPermitSyncMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name)&&prepareSync.containsKey(name)&&prepareSync.get(name)==null)
        {
            r.put(Bridge.RESPONSECODE,Bridge.PERMITSYNC);
            r.put(Bridge.CALLER,caller);
            prepareSync.put(name,caller);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getRefusedSyncMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name)&&prepareSync.containsKey(name)&&prepareSync.get(name)==null)
        {
            r.put(Bridge.RESPONSECODE,Bridge.REFUSEDSYNC);
            r.put(Bridge.CALLER,caller);
            prepareSync.remove(name);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getRequestControl(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.REQUESTCONTROL);
            r.put(Bridge.CALLER,caller);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getPermitControl(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.PERMITCONTROL);
            r.put(Bridge.CALLER,caller);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getRefusedControlMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.REFUSEDCONTROL);
            r.put(Bridge.CALLER,caller);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getEndSyncMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (prepareSync.get(caller)!=null&&prepareSync.get(caller).equals(name))
        {
            prepareSync.remove(caller);
        }else
        if (prepareSync.get(name)!=null&&prepareSync.get(name).equals(caller))
        {
            prepareSync.remove(name);
        }else
        {
            r=null;
            return r;
        }
        r.put(Bridge.RESPONSECODE,Bridge.ENDSYNC);
        r.put(Bridge.CALLER,caller);
        return r;
    }

    public static JSONObject getClearScreenMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (prepareSync.get(caller)!=null&&prepareSync.get(caller).equals(name))
        {
            //prepareSync.remove(caller);
        }else
        if (prepareSync.get(name)!=null&&prepareSync.get(name).equals(caller))
        {
           // prepareSync.remove(name);
        }else
        {
            r=null;
            return r;
        }
        r.put(Bridge.RESPONSECODE,Bridge.CLEARSCREEN);
        r.put(Bridge.CALLER,caller);
        return r;
    }

    public static JSONObject getStartPauseMsg(String caller,String name)
    {
        JSONObject r=new JSONObject();
        if (prepareSync.get(caller)!=null&&prepareSync.get(caller).equals(name))
        {
            //prepareSync.remove(caller);
        }else
        if (prepareSync.get(name)!=null&&prepareSync.get(name).equals(caller))
        {
            // prepareSync.remove(name);
        }else
        {
            r=null;
            return r;
        }
        r.put(Bridge.RESPONSECODE,Bridge.STARTPAUSE);
        r.put(Bridge.CALLER,caller);
        return r;
    }

    public static JSONObject getSeekMsg(String caller,String name,int position)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.SEEK);
            r.put(Bridge.CALLER,caller);
            r.put(POSITION,position);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getSendDataMsg(String caller,String name,String data)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name)&&prepareSync.get(caller)!=null&&prepareSync.get(caller).equals(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.SENDDATA);
            r.put(Bridge.CALLER,caller);
            r.put(DATA,data);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getSendScreenSizeMsg(String caller,String name,int w,int h)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,Bridge.SENDSCREENSIZE);
            r.put(Bridge.CALLER,caller);
            r.put(X,w);
            r.put(Y,h);
        }else
        {
            r=null;
        }

        return r;
    }

    public static JSONObject getSendActionMsg(String caller,int action,int x,int y,String name)
    {
        JSONObject r=new JSONObject();
        if (friends.get(caller).contains(name))
        {
            r.put(Bridge.RESPONSECODE,action);
            r.put(Bridge.CALLER,caller);
            r.put(X,x);
            r.put(Y,y);
        }else
        {
            r=null;
        }

        return r;
    }
}
