package org.benzkuai.yijing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-25
 * Time: 下午9:48
 * To change this template use File | Settings | File Templates.
 */
public class BookAbout {
    private BookActivity m_activity = null;

    public BookAbout(BookActivity activity) {
        m_activity = activity;
    }

    public void show()
    {

       try
       {
           // 关于
           String title = m_activity.getString(R.string.about);

           String versionMessage  = m_activity.getString(R.string.about_book_info);
           versionMessage += "\n"; //支持换行符
           versionMessage += m_activity.getString(R.string.about_app_version);
           versionMessage += m_activity.getPackageManager().getPackageInfo(m_activity.getPackageName(), 0).versionName;
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_function);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_func_1);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_func_2);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_func_3);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_func_4);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_func_5);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_func_6);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_app_author);
           versionMessage += "\n";
           versionMessage += m_activity.getString(R.string.about_copyright);


            AlertDialog.Builder builder = new AlertDialog.Builder(m_activity);
//            builder.setIcon(R.drawable.ico3);
            builder.setTitle(title);
            builder.setMessage(versionMessage);
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_activity.continueSomething();
                }
            });
            builder.show();

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
