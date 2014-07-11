package org.benzkuai.yijing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.storage.StorageManager;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import cn.waps.AppConnect;
import cn.waps.UpdatePointsNotifier;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-12
 * Time: 下午12:53
 * To change this template use File | Settings | File Templates.
 */
public class BookActivity extends Activity
        implements UpdatePointsNotifier
{
    private BookSurfaceView m_bookView = null;


    // 菜单项
    private final int MENU_ITEM_DIRECTORY = 1;
    private final int MENU_ITEM_SETTING = 2;
    private final int MENU_ITEM_EXCHANGE = 3;
    private final int MENU_ITEM_AUTO = 4;
    private final int MENU_ITEM_ABOUT = 5;
    private final int MENU_ITEM_UPDATE = 6;
    private final int MENU_ITEM_RESPONSE = 7;
    private final int MENU_ITEM_GOLD_HOUSE = 8;

    // 是否选择一项目菜单，以确定是否重新启动自动翻页
    private boolean m_isSelected = false;

    private static final String XQJ_SP_FILE = "xqj_sp_file";
    private static final String XQJ_SP_POINTS = "xqj_sp_points";

    public BookActivity() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        try
        {
            setContentView(R.layout.bookview);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        int width = bundle.getInt("width");
        int height = bundle.getInt("height");

        // 自适应view的尺寸，初始化factory实例
        m_bookView = (BookSurfaceView)findViewById(R.id.id_book_view);
        m_bookView.init(this, width, height);

        // 屏幕尺寸
//        DisplayMetrics dm = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//        m_bookView.init(this, dm.widthPixels, dm.heightPixels);


        // 屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 创建这个悬浮窗口
//        createClockWindow();

        //---------------------广告开始------------------------
//        AppConnect.getInstance("fe41e46a3f34c1d5517a6cb982c4301c", "gfan", this);


//        AppConnect.getInstance(this).showMore(this);
//        AppConnect.getInstance(this).showMore(this, "fe41e46a3f34c1d5517a6cb982c4301c");
        AppConnect.getInstance(this).setCrashReport(true);

//        LinearLayout adlayout =(LinearLayout)findViewById(R.id.AdLinearLayout);
//        AppConnect.getInstance(this).showBannerAd(this, adlayout);
//        AppConnect.getInstance(this).showOffers(this);
        AppConnect.getInstance(this).getPoints(this);
        //---------------------广告结束------------------------
    }

    //---------------------广告开始------------------------
    @Override
    public void getUpdatePoints(String s, int i) {
        //To change body of implemented methods use File | Settings | File Templates.
        Log.v("k1", s+"-ok-("+i+")");

        SharedPreferences sp = getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(XQJ_SP_POINTS, i);
        editor.commit();

//
    }

    @Override
    public void getUpdatePointsFailed(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
        Log.v("k1", s+"-er-("+0+")");
    }
    //---------------------广告结束------------------------


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_DIRECTORY, 1, R.string.directory);
        menu.add(0, MENU_ITEM_GOLD_HOUSE, 5, R.string.gold_house);
        menu.add(0, MENU_ITEM_SETTING, 6, R.string.setting);
        menu.add(0, MENU_ITEM_EXCHANGE, 4, R.string.exchange);
        menu.add(0, MENU_ITEM_AUTO, 2, R.string.auto_open);
        menu.add(0, MENU_ITEM_ABOUT, 7, R.string.about);
        menu.add(0, MENU_ITEM_UPDATE, 6, R.string.update);
        menu.add(0, MENU_ITEM_RESPONSE, 3, R.string.response);
        return super.onCreateOptionsMenu(menu);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        m_isSelected = false;
        if (m_bookView.isAutoPaging())
        {
            m_bookView.disableAuto(true); //临时关闭的，在菜单关闭后再继续自动翻页
        }
        return super.onPrepareOptionsMenu(menu);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // 未做任何操作的关闭，只能在这里有机会进行启动
        // 返回键，菜单键，home键也会到这，但走过这不会阻塞就将app关闭
        // 当选择一个菜单项时，就是具休的菜单项里调用activity的方法continueSomething进行启动
        if (!m_isSelected && m_bookView.isContinue())
        {
            m_bookView.enableAuto();
        }
        super.onOptionsMenuClosed(menu);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        m_isSelected = true;
        switch (item.getItemId())
        {
            case MENU_ITEM_DIRECTORY:
            {
                BookDirectory directory = new BookDirectory(this);
                directory.show();
            }
            break;
            case MENU_ITEM_SETTING:
            {
                BookSetting setting = new BookSetting(this);
                setting.show();
            }
            break;
            case MENU_ITEM_EXCHANGE:
            {
                BookExchange exchange = new BookExchange(this);
                exchange.show();
            }
            break;
            case MENU_ITEM_AUTO:
            {
                if (item.getTitle() == getString(R.string.auto_open))
                {// 如果是关闭状态的，则转成打开状态
                    String auto = getString(R.string.auto_close);
                    item.setTitle(auto);
                    m_bookView.enableAuto();
                }
                else
                {
                    item.setTitle(getString(R.string.auto_open));
                    m_bookView.disableAuto(false);// 这是手关闭，不需要再自动打开
                }
            }
            break;
            case MENU_ITEM_ABOUT:
            {
                BookAbout about = new BookAbout(this);
                about.show();
            }
            break;
            case MENU_ITEM_UPDATE:
            {
                AppConnect.getInstance(this).checkUpdate(this);
            }
            break;
            case MENU_ITEM_RESPONSE:
            {
                AppConnect.getInstance(this).showFeedback(this);
            }
            break;
            case MENU_ITEM_GOLD_HOUSE:
            {
                AppConnect.getInstance(this).showOffers(this);
            }
            break;
            default:
                ;
        }

        return true;
    }

    @Override
    protected void onPause() {
        // 返回键，home键，activity被遮挡等都可触发此操作，且此activity可能被系统回收了
//        m_bookView.exitDrawThread();
        m_bookView.saveSetting();
//        m_bookView.showClockWindow(false);
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onStart() {
        super.onStart();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();    //To change body of overridden methods use File | Settings | File Templates.

    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void gotoChapter(int chapterNumber)
    {
        m_bookView.gotoChapter(chapterNumber);
    }

    public void setSetting(int fontSize, int autoTime)
    {
        m_bookView.setSetting(fontSize, autoTime);
    }

    public int getFontSize()
    {
        return m_bookView.getFontSize();
    }

    public int getAutoTime()
    {
        return m_bookView.getAutoTime();
    }

    public CharSequence[] getDirectory()
    {
       return m_bookView.getDirectory();
    }

    public void continueSomething()
    {
        // 不消耗多少时间，可以写入配置文件
        m_bookView.saveSetting();

        if (m_isSelected && m_bookView.isContinue())
        {
            m_bookView.enableAuto();
        }
    }
}
