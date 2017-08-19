package xiyou.mobile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by user on 2017/5/18.
 */

public class SQLUtil {
    static final String ADDRESS="jdbc:mysql://localhost/iqiyi?user=root&password=aa2193&useSSL=false&useUnicode=true&characterEncoding=UTF-8";

    static{
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            p(e);
        }
    }

    public static ArrayList<ArrayList> query(String []what,String from,String where,String orderby)
    {
        ArrayList<ArrayList> res=new ArrayList<>();
        String cmd="select ";
        if (what!=null) {
            cmd += what[0];
            for (int i = 1; i < what.length; i++)
                cmd += "," + what[i];
        }else
        {
            cmd+="*";
        }
        cmd +=" from "+from;
        if (where!=null&&where.length()>0)
            cmd+=" where "+where+" ";
        if (orderby!=null&&orderby.length()>0)
            cmd+=" order by "+orderby;
        cmd+=";";
        p(cmd);
        Connection coon;
        try {
            coon= DriverManager.getConnection(ADDRESS);
            ResultSet rs=coon.createStatement().executeQuery(cmd);
            while(rs.next()) {
                ArrayList al=new ArrayList();
                for (int i=1;i<=rs.getMetaData().getColumnCount();i++)
                {
                    al.add(rs.getObject(i));
                }
                p(al);
                res.add(al);
            }
        } catch (SQLException e) {
            p("error:"+e.toString());
        }

        return res;
    }

    public static boolean insert(String []what,String table,Object []values)
    {
        String cmd="insert into "+table+" ";

        for (int i=0;i<values.length;i++)
        {
            if (values[i] instanceof String)
            {
                values[i]="'"+values[i]+"'";
            }
        }

        if (what!=null&&what.length>=1)
        {
            cmd+="("+what[0];

            for (int i=1;i<what.length;i++)
                cmd+=","+what[i];
            cmd+=") ";
        }
        cmd+="values ("+values[0];
        for (int i=1;i<values.length;i++)
            cmd += "," + values[i];
        cmd+=");";
        p(cmd);
        Connection coon;
        try {
            coon= DriverManager.getConnection(ADDRESS);
            coon.createStatement().execute(cmd);
        } catch (SQLException e) {
            p("error:"+e.toString());
        }

        return true;
    }

    public static void delete(String from,String where)
    {
        String cmd="delete from "+from+" where "+where;
        p(cmd);
        Connection coon;
        try {
            coon= DriverManager.getConnection(ADDRESS);
            coon.createStatement().execute(cmd);
        } catch (SQLException e) {
            p("error:"+e.toString());
        }

    }

    public static void update(String []what,String table,Object []values,String where)
    {
        String cmd="update "+table+" set ";

        for (int i=0;i<values.length;i++)
        {
            if (values[i] instanceof String)
            {
                values[i]="'"+values[i]+"'";
            }
        }

        if (what!=null&&what.length>=1)
        {
            cmd+= what[0]+" = "+values[0];
            for (int i=1;i<what.length;i++)
                cmd+=","+what[i]+" = "+values[i];
        }

        if (where!=null&&where.length()>0)
        {
            cmd+=" where "+where;
        }
        cmd+=";";
        p(cmd);
        Connection coon;
        try {
            coon= DriverManager.getConnection(ADDRESS);
            coon.createStatement().execute(cmd);
        } catch (SQLException e) {
            p("error:"+e.toString());
        }
    }

    public static void exec(String cmd)
    {
        p(cmd);
        Connection coon;
        try {
            coon= DriverManager.getConnection(ADDRESS);
            coon.createStatement().execute(cmd);
        } catch (SQLException e) {
            p("error:" + e.toString());
        }
    }

    public static void p(ArrayList o)
    {
        for (int i=0;i<o.size();i++)
        {
            System.out.print(o.get(i)+" ");
        }
        p("\n");
    }

    public static void p(Object o)
    {System.out.println(o);}

}
