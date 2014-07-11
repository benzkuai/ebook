package org.benzkuai.yijing;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.ConditionVariable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-31
 * Time: 上午10:52
 * To change this template use File | Settings | File Templates.
 */
public class BookSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    // holder 和 绘制thread
    private SurfaceHolder m_surfaceHolder = null;
    private BookDrawThread m_drawThread = null;
    public static final Object bookLock = new Object();

    // bitmap 和 canvas
    private static final int SCENE_SIZE = 3;
    private Bitmap[] m_bmp4Scene = null;
    private Canvas[] m_canvas4Scene = null;
    private int m_viewWidth = 0;
    private int m_viewHeight = 0;
    private float[] m_color4x5 = new float[20];

    // factory
    private BookFactory m_factory = null;
    private int m_primaryItem = 0;

    // 记录滑动动作
    private PointF m_pointDown = new PointF();
    private PointF m_pointUp = new PointF();
    private boolean m_animatePaging = false;
    private float m_gravity = 0.3f;
    private float m_time = 0f;
    private static final float SCOPE_SIZE = 8;
    private SLIP_ORIENTATION m_slipOrientation = SLIP_ORIENTATION.ORIENTATION_NONE;

    // 自动翻页
    private boolean m_isAutoPaging = false;
    private int m_autoTime = 15;//单位：秒
    private boolean m_isContinue = false;
    private int m_second = 0;

    // clock界面
    private WindowManager m_windowManager = null;
    private WindowManager.LayoutParams m_wmParam = null;
    private View m_viewClock = null;
    private TextView m_textViewClock = null;
    private boolean m_isShown = false;

    // 在目录切换
    private boolean m_isDirectoryChanging = false;

    // 路径
    private Path m_path0 = new Path();
    private Path m_path1 = new Path();
    private Path m_path2 = new Path();
    private Path m_path3 = new Path();
    private Path m_path4 = new Path();
    private Path m_path5 = new Path();

    // 点
    private PointF m_a0 = new PointF();
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
    private double m_distance_a0d = 0;

    // 点到直线的距离
    private double m_distance_k_a0d = 0;

    public enum SLIP_ORIENTATION
    {
        ORIENTATION_PAGE_UP,
        ORIENTATION_PAGE_DOWN,
        ORIENTATION_NONE
    }

    public BookSurfaceView(Context context) {
        super(context);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public BookSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public BookSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        m_drawThread = new BookDrawThread(this);
        createClockWindow();
        if (m_isAutoPaging)
        {
            showClockWindow(true);
        }
        m_drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //To change body of implemented methods use File | Settings | File Templates.
        showClockWindow(false);
        m_drawThread.exit();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (bookLock)
        {
            // 自动翻页时，禁止响应，直到关闭自动翻页
            if (m_isAutoPaging)
            {
                return true;
            }

            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                {
                    disableAnimate();
                    m_pointDown.x = event.getX();
                    m_pointDown.y = event.getY();
                }
                break;
                case MotionEvent.ACTION_MOVE:
                {
                    moveMonitor(event.getX(), event.getY());
                }
                break;
                case MotionEvent.ACTION_UP:
                {
                    m_time = 0;
                    m_pointUp.x = event.getX();
                    m_pointUp.y = event.getY();
                    startAnimate();
                }
                break;
                default:
                    ;
            }
        }

        return true;
    }

    public void init(Context context, int viewWidth, int viewHeight)
    {
        m_viewWidth = viewWidth;
        m_viewHeight = viewHeight;

        // 循环使用
        m_bmp4Scene = new Bitmap[SCENE_SIZE];
        m_canvas4Scene = new Canvas[SCENE_SIZE];
        for (int i=0; i<SCENE_SIZE; i++)
        {
            m_bmp4Scene[i] = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            m_canvas4Scene[i] = new Canvas(m_bmp4Scene[i]);
        }

        m_surfaceHolder = getHolder();
        m_surfaceHolder.addCallback(this);
//        m_drawThread = new BookDrawThread(this);

        // 加载文本，并缓存三页面bitmap
        m_factory = new BookFactory(context, viewWidth, viewHeight);
        m_primaryItem = m_factory.getPrimaryItem();

        cacheBmp(m_primaryItem-1, 0);
        cacheBmp(m_primaryItem, 1);
        cacheBmp(m_primaryItem+1, 2);

        // 自动翻页时间
        m_autoTime = m_factory.getAutoTime();

        // 设置颜色矩阵
        m_color4x5[0]=(float) 0.208;
        m_color4x5[1]=(float) 0.509;
        m_color4x5[2]=(float) 0.082;
        m_color4x5[3]=0;
        m_color4x5[4]=0;
        m_color4x5[5]=(float) 0.208;
        m_color4x5[6]=(float) 0.509;
        m_color4x5[7]=(float) 0.082;
        m_color4x5[8]=0;
        m_color4x5[9]=0;
        m_color4x5[10]=(float) 0.208;
        m_color4x5[11]=(float) 0.509;
        m_color4x5[12]=(float) 0.082;
        m_color4x5[13]=0;
        m_color4x5[14]=0;
        m_color4x5[15]=0;
        m_color4x5[16]=0;
        m_color4x5[17]=0;
        m_color4x5[18]=1;
        m_color4x5[19]=0;
//        blurImageAmeliorate2(m_bmp4Scene[1]);
//        drawStatic();
    }

    /**
     * 无论item是多少，hash到三个缓存之一中
     * 推论：3个相邻的item必然hash到3个不同的缓存中,由item即可定位相应的缓存下标
     * 0,表示pre，1表示current，2表示next
     * @param item
     */
    public void cacheBmp(int item, int index)
    {
        m_factory.drawPage(m_canvas4Scene[index], item);
    }

    /**
     * 增加延续的动画 ，是否满足动画条件
     */
    public void startAnimate()
    {
        // 只需要控制状态，绘制动作在draw函数中
        if (Math.abs(m_pointUp.x - m_pointDown.x) > m_viewWidth / 3)
        { // 超过某个页面宽度，则认为是“上翻页动作”
            enableAnimate();
        }
        else
        {
            m_animatePaging = false;
            disableAnimate();
        }
    }

    public void moveMonitor(float x, float y)
    {
        synchronized (bookLock)
        {
            if (Math.abs(x - m_pointDown.x) > SCOPE_SIZE)
            {
                m_animatePaging = true;
                m_a0.x = x;
                m_a0.y = y;

                if (x > m_pointDown.x)
                { //-->
                    m_slipOrientation = SLIP_ORIENTATION.ORIENTATION_PAGE_UP;
                }
                else if (x < m_pointDown.x)
                {//<--
                    m_slipOrientation = SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN;
                }
                else
                {
                    // ==的情况不需要，肯定是要移动了才能决定翻页的方向
                }
            }
            else
            {
                // 默认是偏移8个像素才激活翻页效果绘制
            }
        }

    }

    public void exitDrawThread()
    {
        m_drawThread.exit();
    }

    /**
     * 绘制动作入口
     */
    public void draw()
    {
        synchronized (bookLock)
        {
            if (m_surfaceHolder != null)
            {
                if (!m_animatePaging)
                {
                    drawStatic(); // 解决初始界面显示
                }
                else
                {
                    calcPointAndPath(m_a0.x, m_a0.y);
                    drawMoving();
                }
            }

        }

    }

    public void drawStatic()
    {
        Canvas canvas = m_surfaceHolder.lockCanvas();
        if (canvas != null)
        {
            canvas.drawBitmap(m_bmp4Scene[1], 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
            m_surfaceHolder.unlockCanvasAndPost(canvas);
        }

    }


    public void drawMoving()
    {
        Canvas canvas = m_surfaceHolder.lockCanvas(); //双缓冲的，不会出显闪烁

        if (null == canvas)
        {
            return;
        }
        // 渐变填充
        GradientDrawable gradient = new GradientDrawable();

        // 当前页面
        canvas.save();
        canvas.clipPath(m_path0, Region.Op.DIFFERENCE);
        Paint paintMatrix = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix cm = new ColorMatrix(m_color4x5);
        paintMatrix.setColorFilter(new ColorMatrixColorFilter(cm)); // 这种方法处理黑白画图，效果可以接受，如果直接操作像素点，效率太低了，要10s时间。
        canvas.drawBitmap(m_bmp4Scene[1], 0, 0, paintMatrix);
        canvas.drawColor(0x20C8C8C8);
        canvas.restore();

        // 当前卷页面
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1);
        // 这部分是在背面画文字，使用3x3矩阵做变换:关于y对称。
        // （其它效果也是通告调整这个矩阵实现，如：关于x对称，y=x对称，原点对称。。。
        // 感觉不画字效果更好些，所以不画了。
//        Matrix matrix = new Matrix();
//        float[] values ={-1f,0.0f,0.0f,0.0f,1f,0.0f,0.0f,0.0f,1.0f};
//        matrix.setValues(values);
//        Bitmap bmp2 = Bitmap.createBitmap(m_bmp4Scene[1], 0, 0, m_viewWidth, m_viewHeight, matrix, true);
//        canvas.translate(m_d.x, m_d.y);
//        canvas.rotate(-(float) m_angle_da0 - 90);
//        canvas.drawBitmap(bmp2, 0, (int)m_distance_a0d - m_viewHeight, paintMatrix);
        canvas.drawColor(0x20C8C8C8);
        canvas.restore();
        // 当前卷页面上的高光部分
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1);
        if (m_slipOrientation == SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN)
        {
            canvas.translate(m_k.x, m_k.y);
        }
        else if (m_slipOrientation == SLIP_ORIENTATION.ORIENTATION_PAGE_UP)
        {
            canvas.translate(m_l.x, m_l.y);
        }

        canvas.rotate(-(float)m_angle_b0a0);
        int[] color_a0kl = new int[3];
        color_a0kl[0] = 0x00000000;
        color_a0kl[1] = 0x80ffffff;
        color_a0kl[2] = 0x00000000;
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradient.setColors(color_a0kl);
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setBounds(-(int) m_distance_hc*3/2, 0, 0, (int) m_distance_kl);
        gradient.draw(canvas);
        canvas.drawColor(0x20C8C8C8);
        canvas.restore();

        // 下页面
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1, Region.Op.DIFFERENCE);
        if (SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN == m_slipOrientation)
        {
            canvas.drawBitmap(m_bmp4Scene[2], 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
        }
        else if (SLIP_ORIENTATION.ORIENTATION_PAGE_UP == m_slipOrientation)
        {
            canvas.drawBitmap(m_bmp4Scene[0], 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
        }
        canvas.restore();

        // 下页面上的阴影
        canvas.save();
        canvas.clipPath(m_path0);
        canvas.clipPath(m_path1, Region.Op.DIFFERENCE);
        canvas.clipPath(m_path2);
        if (m_slipOrientation == SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN)
        {
            canvas.translate(m_f.x, m_f.y);
        }
        else if (m_slipOrientation == SLIP_ORIENTATION.ORIENTATION_PAGE_UP)
        {
            canvas.translate(m_g.x, m_g.y);
        }
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
//        canvas.save();
//        Paint paint = new Paint();
//        paint.setAntiAlias(true);
//        paint.setColor(0xffff0000);
//        canvas.drawLine(m_d.x, m_d.y, m_e.x, m_e.y, paint);
//        canvas.drawLine(m_f.x, m_f.y, m_g.x, m_g.y, paint);
//        canvas.drawLine(m_k.x, m_k.y, m_l.x, m_l.y, paint);
//        canvas.restore();

        m_surfaceHolder.unlockCanvasAndPost(canvas);
    }

    public void  calcPointAndPath(float x, float y)
    {

        if (SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN == m_slipOrientation)
        {
            m_b0.x = getWidth();
            m_b0.y = getHeight();
        }
        else  if (SLIP_ORIENTATION.ORIENTATION_PAGE_UP == m_slipOrientation)
        {
            m_b0.x = 0;
            m_b0.y = getHeight();
        }
        else
        {
          //
        }

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
        m_distance_a0d = Math.sqrt((a0.y-d.y)*(a0.y-d.y)+(a0.x-d.x)*(a0.x-d.x));
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



    /**
     * 翻页动画
     */
    private Handler m_handler = new Handler();
    private Runnable m_runnable = new Runnable()
    {
        @Override

        public void run()
        {
            synchronized (bookLock)
            {
                animateTurnPage();
//                try {
//                    Thread.sleep(1); // 解决CPU占用过高问题
//                } catch (InterruptedException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }
            }
        }
    } ;

    public void enableAnimate()
    {
        m_handler.postDelayed(m_runnable, 5);// 翻页动画刷新频率
    }

    public void disableAnimate()
    {
        m_handler.removeCallbacks(m_runnable);
    }

    // pageDown
    public void animatePageDown()
    {
        // begin-2014-3-1,修复bug：到达封底还有继续翻页，导致回翻页时页面内容出错。
        if (m_factory.isBackCover())
        {
            m_animatePaging = false;
            disableAnimate();
            return;
        }
        //end-2014-3-1 ,修复bug：到达封底还有继续翻页，导致回翻页时页面内容出错。

        m_time++;
        m_a0.x -= (int)(m_gravity*m_time*m_time);

        // 在自动翻页时，同时也改变y的坐标, y不要太小，否则会影响翻页效果
        if (m_isAutoPaging && (m_a0.y > m_viewHeight*4/5))
        {
            m_a0.y -= (int)(m_gravity*m_time*m_time);
        }

        if (m_a0.x < -3*m_viewWidth) // 滑出去3个界面宽度,停止动画
        {
            disableAnimate();
            m_time = 0;

            // 动画完毕后，此时可缓存下一页面
            m_animatePaging = false;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            if (m_isDirectoryChanging)
            {
                m_isDirectoryChanging = false;
                m_canvas4Scene[1].drawBitmap(m_bmp4Scene[2], 0, 0, paint);
                cacheBmp(m_primaryItem-1, 0);// 需要将primaryItem项前后两页面缓存起来
                cacheBmp(m_primaryItem+1, 2);
                m_factory.setPrimaryItem(m_primaryItem);
            }
            else
            {
                m_canvas4Scene[0].drawBitmap(m_bmp4Scene[1], 0, 0, paint);
                m_canvas4Scene[1].drawBitmap(m_bmp4Scene[2], 0, 0, paint);
                m_primaryItem++;
                m_factory.setPrimaryItem(m_primaryItem);
                cacheBmp(m_primaryItem+1, 2);
            }

        }
        else
        {
            enableAnimate();
        }
    }

    // pageUp
    public void animatePageUp()
    {
        // begin-2014-3-1,修复bug：到达封面还有继续翻页，导致回翻页时页面内容出错。
        if (m_factory.isCover())
        {
            m_animatePaging = false;
            disableAnimate();
            return;
        }
        // end-2014-3-1,修复bug：到达封面还有继续翻页，导致回翻页时页面内容出错。

        m_time++;
        m_a0.x += (int)(m_gravity*m_time*m_time);
        if (m_a0.x > 3*m_viewWidth)
        {
            disableAnimate();
            m_time = 0;
            // 动画完毕后，此时可缓存下一页面
            m_animatePaging = false;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            m_canvas4Scene[2].drawBitmap(m_bmp4Scene[1], 0, 0, paint);
            m_canvas4Scene[1].drawBitmap(m_bmp4Scene[0], 0, 0, paint);
            m_primaryItem--;
            m_factory.setPrimaryItem(m_primaryItem);
            cacheBmp(m_primaryItem-1, 0);
        }
        else
        {
            enableAnimate();
        }
    }

    public void animateTurnPage()
    {
        if (m_slipOrientation == SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN)
        {
            animatePageDown();
        }
        else if (m_slipOrientation == SLIP_ORIENTATION.ORIENTATION_PAGE_UP)
        {
            animatePageUp();
        }
    }

    /**
     * 目录
     * @return
     */
    public CharSequence[] getDirectory()
    {
        return m_factory.getDirectory();
    }

    public void gotoChapter(int item)
    {
        m_isDirectoryChanging = true;
        m_factory.gotoChapter(item);
        m_primaryItem = m_factory.getPrimaryItem();

//        cacheBmp(m_primaryItem-1, 0);
        cacheBmp(m_primaryItem, 2);
//        cacheBmp(m_primaryItem+1, 2);
        // 用动画过渡
        autoPageDown();
    }

    /**
     *
     * 自动翻页时，禁止touch的响应
     */
    private Handler m_handlerAuto = new Handler();
    private Runnable m_runnableAuto = new Runnable()
    {
        @Override
        public void run()
        {
            synchronized (bookLock)
            {
                updateClock(m_second);
                if (0 == m_second)
                {
                    autoPageDown();
                    m_second = m_autoTime;
                }
                m_second--;
                startAuto();
            }
        }
    } ;

    public void enableAuto()
    {
        m_isAutoPaging = true;
        m_second = m_autoTime;
        updateClock(m_second);
        showClockWindow(true);
        startAuto();
    }

    public void startAuto()
    {
        //        m_handlerAuto.postDelayed(m_runnableAuto, m_autoTime*1000);// 自动翻页时间间隔
        m_handlerAuto.postDelayed(m_runnableAuto, 1000);// 每秒钟刷新一次秒表，
    }

    public void disableAuto(boolean isContinue)
    {
        showClockWindow(false);
        m_isContinue = isContinue;
        m_isAutoPaging = false;
        m_handlerAuto.removeCallbacks(m_runnableAuto);
    }

    public boolean isAutoPaging()
    {
        return m_isAutoPaging;
    }

    public boolean isContinue()
    {
        return m_isContinue;
    }

    public void autoPageDown()
    {
        m_animatePaging = true;
        m_a0.x = m_viewWidth - 80;
        m_a0.y = m_viewHeight - 50;
        m_slipOrientation = SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN;
        enableAnimate();
    }

    public void createClockWindow()
    {
        m_viewClock = LayoutInflater.from(getContext()).inflate(R.layout.clock, null);
        m_windowManager = (WindowManager)getContext().getSystemService("window");
        m_wmParam = new WindowManager.LayoutParams();
        m_wmParam.type = 2002;
        m_wmParam.flags |= 8;
        m_wmParam.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP; // 调整悬浮窗口至左上角
        // 以屏幕左上角为原点，设置x、y初始值
        m_wmParam.x = 0;
        m_wmParam.y = 0;
        // 设置悬浮窗口长宽数据
        m_wmParam.width = WindowManager.LayoutParams.WRAP_CONTENT;
        m_wmParam.height = WindowManager.LayoutParams.WRAP_CONTENT;
        m_wmParam.format = 1;

//        // 增加容器到system是需要权限的（权限：SYSTEM_ALERT_WINDOW），否则报异常
//        m_windowManager.addView(m_viewClock, m_wmParam);

        // 读出秒数
//        SharedPreferences sp = getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
//        m_autoTime = sp.getInt(KEY_SETTING_AUTO_TIME, 15); //默认15s

        // 秒表框
        m_textViewClock = (TextView)m_viewClock.findViewById(R.id.id_textView_clock);
        m_textViewClock.setText("");
    }

    public void showClockWindow(boolean isShow)
    {
        if (isShow)
        {
            // 增加容器到system是需要权限的（权限：SYSTEM_ALERT_WINDOW），否则报异常
            m_windowManager.addView(m_viewClock, m_wmParam);
            m_textViewClock.setText(""+m_second);
            m_isShown = true;
        }
        else
        {
            if (m_isShown)
            {
                m_windowManager.removeView(m_viewClock);
                m_isShown = false;
            }
        }
    }

    public void updateClock(int second)
    {
        if (m_second <= 5)
        {
            m_textViewClock.setTextColor(0xffff0000);
        }
        else
        {
            m_textViewClock.setTextColor(0xffffffff);
        }
        m_textViewClock.setText(""+second);
    }

    /**
     * 设置
     * @return
     */
    public int getFontSize()
    {
        return m_factory.getFontSize();
    }

    public int getAutoTime()
    {
        return m_factory.getAutoTime();
    }

    public void saveSetting()
    {
        m_factory.saveSetting();
    }

    public void setSetting(int fontSize, int autoTime)
    {
        if (m_factory.getAutoTime() != autoTime)
        {
//            if (m_isAutoPaging)此时必须是已经停止自动翻页状态，待设置完成后可能再执行。
//            {
//
//            }
            m_autoTime = autoTime;
            m_factory.setAutoTime(autoTime);
        }

        if (m_factory.getFontSize() != fontSize)
        {
            m_factory.setFontSize(fontSize);

            // 翻页到设置完成的页面，用动画作过渡
            m_isDirectoryChanging = true;//借用，因为行为是一样的，都需要刷新-1,+1页面
            cacheBmp(m_factory.getPrimaryItem(), 2);
            m_animatePaging = true;
            m_a0.x = m_viewWidth - 80;
            m_a0.y = m_viewHeight - 50;
            m_slipOrientation = SLIP_ORIENTATION.ORIENTATION_PAGE_DOWN;
            enableAnimate();
        }

    }

}
