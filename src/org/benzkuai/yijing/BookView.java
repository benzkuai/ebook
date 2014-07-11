package org.benzkuai.yijing;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-27
 * Time: 下午11:10
 * To change this template use File | Settings | File Templates.
 */
public class BookView extends View {

    private BookFactory m_bookFactory = null;
    private int m_primaryItem = 0;

    private Bitmap m_bmp4Current = null;
    private Canvas m_canvas4Current = null;
    private Bitmap m_bmp4Next = null;
    private Canvas m_canvas4Next = null;
    // 路径
    private Path m_path0 = new Path();
    private Path m_path1 = new Path();
    private Path m_path2 = new Path();
    private Path m_path3 = new Path();
    private Path m_path4 = new Path();
    private Path m_path5 = new Path();

    // 点
//    private PointF m_a0 = new PointF();
    private PointF m_b0 = new PointF();
//    private PointF m_c = new PointF();
    private PointF m_d = new PointF();
    private PointF m_e = new PointF();
    private PointF m_f = new PointF();
    private PointF m_g = new PointF();
//    private PointF m_h = new PointF();
    private PointF m_i = new PointF();
    private PointF m_j = new PointF();
    private PointF m_k = new PointF();
    private PointF m_l = new PointF();
//    private PointF m_m = new PointF();
//    private PointF m_n = new PointF();
//    private PointF m_o = new PointF();

    // 夹角
    private double m_angle_b0a0 = 0;
    private double m_angle_da0 = 0;

    // 两点间距离
    private double m_distance_hc = 0;
    private double m_distance_fg = 0;
    private double m_distance_kl = 0;
    private double m_distance_ko = 0;
    private double m_distance_fi = 0;
    private double m_distance_jg = 0;

    // 点到直线的距离
    private double m_distance_k_a0d = 0;


    public BookView(Context context) {
        super(context);    //To change body of overridden methods use File | Settings | File Templates.

    }

    public BookView(Context context, AttributeSet attrs) {
        super(context, attrs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void initFactory(Context context, int viewWidth, int viewHeight)
    {
        m_bookFactory = new BookFactory(context, viewWidth, viewHeight);
        m_primaryItem = m_bookFactory.getPrimaryItem();

        m_bmp4Current = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        m_canvas4Current = new Canvas(m_bmp4Current);
        m_bmp4Next = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        m_canvas4Next = new Canvas(m_bmp4Next);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        return super.onTouchEvent(event);    //To change body of overridden methods use File | Settings | File Templates.
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {

                    m_bookFactory.drawPage(m_canvas4Current, m_primaryItem);
                    m_bookFactory.drawPage(m_canvas4Next, m_primaryItem+1);

            }
            break;
            case MotionEvent.ACTION_MOVE:
            {
                calcPointAndPath(event.getX(), event.getY());

            }
            break;
            case MotionEvent.ACTION_UP:
            {}
            break;
            default:
                ;
        }

        postInvalidate();
        return true;
    }

    public void  calcPointAndPath(float x, float y)
    {
        m_b0.x = getWidth();
        m_b0.y = getHeight();
        // 已知点
        PointF a0 = new PointF(x, -y);
        PointF b0 = new PointF(m_b0.x, -m_b0.y);
        float k0 = (a0.y - b0.y)/(a0.x - b0.x);

        // a0,b0的中点c
        PointF c = new PointF((a0.x+b0.x)/2, (a0.y+b0.y)/2);

        // 点斜式直线方程，求出直线de与b0d、b0e的交点d，e
        float k1 = -1/k0;
        PointF d = new PointF();
        d.x = b0.x;
        d.y = k1*(d.x-c.x) + c.y;
        PointF e = new PointF();
        e.y = b0.y;
        e.x = (e.y-c.y)/k1 + c.x;

        // 点斜式直线方程，求出直线fg与b0f、b0g的交点f，g
        // h是中心点，k1
        PointF h = new PointF((a0.x+c.x)/2, (a0.y+c.y)/2);
        PointF f = new PointF();
        f.x = b0.x;
        f.y = k1*(f.x-h.x) + h.y;
        PointF g = new PointF();
        g.y = b0.y;
        g.x = (g.y-h.y)/k1 + h.x;

        // a0d中心点
        PointF i = new PointF((a0.x+d.x)/2, (a0.y+d.y)/2);
        // a0e中心点
        PointF j = new PointF((a0.x+e.x)/2, (a0.y+e.y)/2);

        // fi中心点m
        PointF m = new PointF((f.x+i.x)/2, (f.y+i.y)/2);
        // gj中心点n
        PointF n = new PointF((g.x+j.x)/2, (g.y+j.y)/2);
        // md中心点k
        PointF k = new PointF((m.x+d.x)/2, (m.y+d.y)/2);
        // ne中心点l
        PointF l = new PointF((n.x+e.x)/2, (n.y+e.y)/2);

        // k2是a0d的斜率
        float k2 = (a0.y - d.y)/(a0.x - d.x);
        // 由点斜式得平行于a0d的直接ko，过k点；其中o点是ko与a0b0的交点
        PointF o = new PointF();
//        o.y = k2*(o.x - k.x) + k.y;
        o.x = (- k2*k.x + k.y - a0.y + k0*a0.x)/(k0 - k2);
        o.y = k2*(o.x - k.x) + k.y;

        // 求直线b0a0与x轴正方向形成的夹角（第四象限的）
        double angleRad = Math.atan2(b0.y - a0.y, b0.x - a0.x);
        m_angle_b0a0 = Math.toDegrees(angleRad);
        // 求两点间hc,fg的距离
        m_distance_hc = Math.sqrt((h.y-c.y)*(h.y-c.y)+(h.x-c.x)*(h.x-c.x));
        m_distance_fg = Math.sqrt((f.y-g.y)*(f.y-g.y)+(f.x-g.x)*(f.x-g.x));
        m_distance_kl = Math.sqrt((k.y-l.y)*(k.y-l.y)+(k.x-l.x)*(k.x-l.x));
        m_distance_fi = Math.sqrt((f.y-i.y)*(f.y-i.y)+(f.x-i.x)*(f.x-i.x));
        // 求点到直线的距离
        // 点k到直线a0d的距离
//        y - k2*x + k2 * m_d.x - m_d.y = 0;一般形式直线方程
        m_distance_k_a0d = Math.abs(k.y*1 -k.x*k2 + k2 * m_d.x - m_d.y)/Math.sqrt(1*1+(-k2)*(-k2));
        // 两点ko之间的距离
        m_distance_ko = Math.sqrt((k.y-o.y)*(k.y-o.y)+(k.x-o.x)*(k.x-o.x));
        // 求直线da0与x轴正方向形成的夹角（第四象限的）
        angleRad = Math.atan2(-d.y + a0.y, -d.x + a0.x);
        m_angle_da0 = Math.toDegrees(angleRad);

        // 转成视图
        a0.y = -a0.y;
        b0.y = -b0.y;
        c.y = -c.y;
        d.y = -d.y;
        e.y = -e.y;
        f.y = -f.y;
        g.y = -g.y;
        h.y = -h.y;
        i.y = -i.y;
        j.y = -j.y;
        k.y = -k.y;
        l.y = -l.y;
        m.y = -m.y;
        n.y = -n.y;
        o.y = -o.y;

        // 写入成员变量
        m_d = d;
        m_e = e;
        m_f = f;
        m_g = g;
        m_k = k;
        m_l = l;
        m_i = i;
        m_j = j;


        // 翻页形状path0
        m_path0.reset();
        m_path0.moveTo(b0.x, b0.y);
        m_path0.lineTo(f.x, f.y);
        m_path0.quadTo(d.x, d.y, i.x, i.y);
        m_path0.lineTo(a0.x, a0.y);
        m_path0.lineTo(j.x, j.y);
        m_path0.quadTo(e.x, e.y, g.x, g.y);
        m_path0.close();

        // 翻页形状path1
        m_path1.reset();
        m_path1.moveTo(a0.x, a0.y);
        m_path1.lineTo(l.x, l.y);
        m_path1.lineTo(k.x, k.y);
        m_path1.close();

        // 翻页形状path2
        m_path2.reset();
        m_path2.moveTo(f.x, f.y);
        m_path2.lineTo(g.x, g.y);
        m_path2.lineTo(e.x, e.y);
        m_path2.lineTo(d.x, d.y);
        m_path2.close();

        // 上页面阴影形状path3
        m_path3.reset();
        m_path3.moveTo(d.x, d.y);
        m_path3.lineTo(k.x, k.y);
        m_path3.lineTo(o.x, o.y);
        m_path3.lineTo(a0.x, a0.y);
        m_path3.close();

        // 转弯处三角形状path4,path5
        m_path4.reset();
        m_path4.moveTo(m_d.x, m_d.y);
        m_path4.lineTo(m_f.x, m_f.y);
        m_path4.lineTo(m_i.x, m_i.y);
        m_path4.close();

        m_path5.reset();
        m_path5.moveTo(m_j.x, m_j.y);
        m_path5.lineTo(m_g.x, m_g.y);
        m_path5.lineTo(m_e.x, m_e.y);
        m_path5.close();


    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);    //To change body of overridden methods use File | Settings | File Templates.

        GradientDrawable gradient = new GradientDrawable();

        // 当前页面
        canvas.save();

        canvas.clipPath(m_path0, Region.Op.DIFFERENCE);
//        canvas.drawColor(0xffff0000);

//            m_bookFactory.drawPage(canvas, m_primaryItem);
        canvas.drawBitmap(m_bmp4Current, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));

        canvas.restore();



        // 当前卷页面
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1);
//        canvas.drawColor(0xff0000ff);
        canvas.restore();
        // 当前卷页面上的高光部分
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1);
        canvas.translate(m_k.x, m_k.y);
        canvas.rotate(-(float)m_angle_b0a0);
        int[] color_a0kl = new int[3];
        color_a0kl[0] = 0x00ffffff;
        color_a0kl[1] = 0xffffffff;
        color_a0kl[2] = 0x00ffffff;
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradient.setColors(color_a0kl);
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setBounds(-(int) m_distance_hc*3/2, 0, 0, (int) m_distance_kl);
        gradient.draw(canvas);
        canvas.restore();

        // 下页面
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1, Region.Op.DIFFERENCE);
//        canvas.drawColor(0xff00ff00);
        canvas.drawBitmap(m_bmp4Next, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
        canvas.restore();
        // 下面上的阴影
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1, Region.Op.DIFFERENCE);
        canvas.clipPath(m_path2);
        canvas.translate(m_f.x, m_f.y);
        canvas.rotate(-(float)m_angle_b0a0);
        int[] color_hc = new int[2];
        color_hc[0] = 0xff000000;
        color_hc[1] = 0x00000000;
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradient.setColors(color_hc);
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setBounds(0, 0, (int) m_distance_hc, (int) m_distance_fg);
        gradient.draw(canvas);
        canvas.restore();

        // 当前页面上的右上边阴影
//        canvas.save();
//        canvas.clipPath(m_path3);
////        canvas.drawColor(0xffffffff);
//        canvas.clipPath(m_path0, Region.Op.DIFFERENCE);
//        canvas.translate(m_k.x, m_k.y);
//        canvas.rotate(-(float)m_angle_da0-90);
//        int[] color_a0dko = new int[2];
//        color_a0dko[0] = 0x01000000;
//        color_a0dko[1] = 0xFF000000;
//        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
//        gradient.setColors(color_a0dko);
//        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
//        gradient.setBounds(0, 0, (int)m_distance_k_a0d,(int) m_distance_ko);
//        gradient.draw(canvas);
//        canvas.restore();

        //转弯处阴影
        canvas.save();
        canvas.clipPath(m_path4);
        canvas.clipPath(m_path0, Region.Op.DIFFERENCE);
//        canvas.drawColor(0xff000000);
        canvas.translate(m_f.x, m_f.y);
        canvas.rotate(-(float)m_angle_b0a0);
        int[] color_kfi = new int[2];
        color_kfi[0] = 0x00000000;
        color_kfi[1] = 0xa0000000;
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradient.setColors(color_kfi);
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setBounds(0, 0, (int)m_distance_hc,(int) m_distance_fi);
        gradient.draw(canvas);
        canvas.restore();


        // 画参考线
        canvas.save();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xffff0000);
        canvas.drawLine(m_d.x, m_d.y, m_e.x, m_e.y, paint);
        canvas.drawLine(m_f.x, m_f.y, m_g.x, m_g.y, paint);
        canvas.drawLine(m_k.x, m_k.y, m_l.x, m_l.y, paint);
        canvas.restore();


    }

    public void drawCurrentPage(Canvas canvas)
    {

    }
}