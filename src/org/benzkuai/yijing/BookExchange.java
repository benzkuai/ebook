package org.benzkuai.yijing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cn.waps.AppConnect;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-25
 * Time: 下午11:33
 * To change this template use File | Settings | File Templates.
 */
public class BookExchange {
    private BookActivity m_context = null;

    private static final String XQJ_SP_FILE = "xqj_sp_file";
    private static final String XQJ_SP_POINTS = "xqj_sp_points";

    private static final int PER_CARD_COST = 50000;

    public BookExchange(BookActivity context) {
        m_context = context;
    }

    public void show()
    {
        LayoutInflater layout = LayoutInflater.from(m_context);
        View view = layout.inflate(R.layout.recommend, null);

        String title = m_context.getString(R.string.exchange);

        // 积分
//        SharedPreferences sp = m_context.getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
//        int points = sp.getInt(XQJ_SP_POINTS, 0);

        TextView textView = (TextView)view.findViewById(R.id.id_textView_points);

        String prompt = m_context.getString(R.string.your_order);
        textView.setText(prompt);

//        final TextView textViewSn = (TextView)view.findViewById(R.id.id_textView_sn);
//
//        Button button = (Button)view.findViewById(R.id.id_button_exchange);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 再查一次，花费后数据有变化
//                SharedPreferences sp = m_context.getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
//                int pointsValue = sp.getInt(XQJ_SP_POINTS, 0);
//
//                String result = "";
//                if (pointsValue >= PER_CARD_COST)
//                {
//                    AppConnect.getInstance(m_context).spendPoints((pointsValue/PER_CARD_COST)*PER_CARD_COST,m_context);
//                    result += "恭喜！兑换成功！";
//                    result +="\n";
//                }
//                else
//                {
//                    result += "积分不足("+PER_CARD_COST+")无法兑换，请在黄金屋中赚取！";
//                }
//
//                for (int i=0; i< pointsValue /PER_CARD_COST; i++)
//                {
//                    result += "K-KEY"+(i+1)+":" + getKey(pointsValue, i+1) + "\n";
//                }
//                textViewSn.setText(result);
//            }
//        });

        AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
//        builder.setIcon(R.drawable.ico3);
        builder.setTitle(title);
//        builder.setMessage();


        builder.setView(view);
        builder.setPositiveButton(m_context.getString(R.string.confirm), null);
        builder.show();
    }

    public String getKey(int points, int index)
    {
        String key = "";
        Time t = new Time("GMT+8");
        t.setToNow();

        int year = t.year;
        int month = t.month;
        int day = t.monthDay;
        int hour = t.hour;
        int minute = t.minute;
        int second = t.second;

        String[] tableAlpha = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N",
                "O","P","Q","R","S","T","U","V","W","X","Y","Z"};
        String[] tableNum = {"0","1","2","3","4","5","6","7","8","9"};
        int[] randomAlpha = new int[12];
        int[] randomNum = new int[5];
        for (int i = 0; i< 12; i++)
        {
            randomAlpha[i] =  (int)(Math.random() * 26);
        }
        for (int j = 0; j<5;j++)
        {
            randomNum[j] =  (int)(Math.random() * 10);
        }

        key = tableAlpha[randomAlpha[0]]+tableNum[randomNum[0]]+tableNum[randomNum[1]]+tableAlpha[randomAlpha[1]]
                + hour
                + tableAlpha[randomAlpha[2]]
                + minute
                + tableAlpha[randomAlpha[3]]
                + second
                + tableAlpha[randomAlpha[4]]
                + month
                + tableAlpha[randomAlpha[5]]
                + tableAlpha[randomAlpha[6]]
                + day

//                + year
                + tableAlpha[randomAlpha[7]]
                + index
                + tableAlpha[randomAlpha[8]]
                + tableAlpha[randomAlpha[9]]

//                + tableAlpha[randomAlpha[10]]
                + tableNum[randomNum[2]]
                + points
                + tableNum[randomNum[3]]
                + tableNum[randomNum[4]]
                + tableAlpha[randomAlpha[11]];


        return key;
    }


}
