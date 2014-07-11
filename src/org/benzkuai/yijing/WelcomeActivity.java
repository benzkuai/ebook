package org.benzkuai.yijing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.bmob.*;
import cn.waps.AppConnect;
import cn.waps.AppListener;
import com.otomod.ad.*;

import java.util.List;
import java.util.Random;

/**
 * Created by zhouzhou on 14-2-21.
 */
public class WelcomeActivity extends Activity implements View.OnClickListener{

    private static final String APP_ID_BMOB = "93451709d841b6bc31f622cdf2c593ce";

    public WelcomeActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome);

//        TextView goodTitle = (TextView)findViewById(R.id.id_textView_good_title);
//        goodTitle.setText(getString(R.string.good_title));

        findViewById(R.id.id_imageButton_go).setOnClickListener(this);
//        TextView textView = (TextView)findViewById(R.id.id_textView_good);

//        String[] wisdom = new String[]{
//                getString(R.string.good_1),
//                getString(R.string.good_2),
//                getString(R.string.good_3),
//                getString(R.string.good_4),
//                getString(R.string.good_5),
//                getString(R.string.good_6),
//                getString(R.string.good_7),
//                getString(R.string.good_8)};
//
//        int index = (int)(Math.random()*8);
//        textView.setText(wisdom[index]);

//        Toast.makeText(this.getBaseContext(), "你知道吗？黄金屋可以获取积分哦！", 1).show();

        AppConnect.getInstance("53d79f347612e941ac83ea3c7147f4f7", "xiaomi", this);


        // 广告配置项
        String hudong = AppConnect.getInstance(this).getConfig_Sync("hudong");
        String chaping = AppConnect.getInstance(this).getConfig_Sync("chaping");

        // 互动广告
        AppConnect.getInstance(this).getConfig("hudong", "no", new AppListener(){
            @Override
            public void onGetConfig(String value) {
                if (value.equals("1"))
                {
//                    showAd_hudong();
                }
            }
        });

        // 插屏广告
        AppConnect.getInstance(this).initPopAd(this);
        AppConnect.getInstance(this).getConfig("chaping", "no", new AppListener(){
            @Override
            public void onGetConfig(String value) {
                if (value.equals("1"))
                {
//                    showAd_chaping();
                }
            }
        });

        // 百灵欧拓 广告
        new AdApplication("bdf84266465ea656ab65adef0c7e13c6", "a4500932aec311e38af690b11c572964",
                this);
//        LinearLayout	container	=(LinearLayout)findViewById(R.id.AdLinearLayout2);
//        new AdStaticBanner(this,	container);	 //静态广告条
//        new AdAppBanner(this,	container);		 //App广告条
//        new AdDynamicBanner(this,	container);	//动态广告条
//        new AdPopup(this);
        LinearLayout ad1 = (LinearLayout) findViewById(R.id.AdLinearLayout2);
        // App广告条
        try {
            new AdStaticBanner(this, ad1, new OtomodAdListenerImpl());
            new	AdFullScreen(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        // 自家下载
//        AppConnect.getInstance(this).showMore(this);
    }

    @Override
    public void onClick(View view) {
//        LayoutInflater layout = LayoutInflater.from(this);
//        View viewWelcome = layout.inflate(R.layout.welcome, null);
//        viewWelcome.findViewById()
        View viewWelcome = findViewById(R.id.id_111);


        Intent intent = new Intent(WelcomeActivity.this, BookActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("width", viewWelcome.getWidth());
        bundle.putInt("height", viewWelcome.getHeight());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
//        AppConnect.getInstance(this).close();
        super.onDestroy();
    }

    public void showAd_hudong()
    {
        LinearLayout adlayout =(LinearLayout)findViewById(R.id.AdLinearLayout2);
        AppConnect.getInstance(this).showBannerAd(this, adlayout);
    }

    public void showAd_chaping()
    {
        AppConnect.getInstance(this).showPopAd(this);
    }

    public class OtomodAdListenerImpl implements OtomodAdListener {

        @Override
        public void onAdSuccess() {
            System.out.println("oto广告加载成功");
        }

        @Override
        public void onAdFailed() {
            System.out.println("oto广告加载失败");
        }


    }
}
