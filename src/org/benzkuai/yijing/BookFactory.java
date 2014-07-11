package org.benzkuai.yijing;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-12
 * Time: 下午12:53
 * To change this template use File | Settings | File Templates.
 */
public class BookFactory {

    private static final String XQJ_SP_FILE = "xqj_sp_file";
    private static final String KEY_BOOKMARK0_CHAPTER_NUMBER = "key_bookmark0_chapter_number";
    private static final String KEY_BOOKMARK0_PAGE_NUMBER = "key_bookmark0_page_number";
    private static final String KEY_SETTING_TEXT_SIZE = "key_setting_text_size";
    private static final String KEY_SETTING_AUTO_TIME = "key_setting_auto_time";

    // context
    private Context m_context = null;

    private String m_charset = "GBK";

    // 画笔 & 背景色
    private Paint m_paint = null;
    private float m_textSize = 24.0f;
    private int m_textColor = 0xff340002;
    private int m_backgroundColor = 0xff220088;

    // 自动翻页时间间隔
    private int m_autoTime = 15;

    //可见区域
    private int m_viewWidth = 0;
    private int m_viewHeight = 0;
    private int m_visibleWidth = 0;
    private int m_visibleHeight = 0;
    private int m_marginX = 0;
    private int m_marginY = 0;
    private int m_lineSpace = 10;//  行间距

    // 行能力  (一页面最大承载多少行)
    private int m_lineCapacity = 0;

    // 章对应映射表
    private Map m_chapterNameMap = null;
    private Map m_chapterInfoMap = null;
    private Map m_chapterMap = null;
    private static final String[] CHAPTER_NAME_TABLE = {"yijing",
            "c1", "c2","c3", "c4", "c5","c6", "c7", "c8", "c9", "c10",
            "c11", "c12","c13", "c14", "c15","c16", "c17", "c18", "c19", "c20",
            "c21", "c22","c23", "c24", "c25","c26", "c27", "c28", "c29", "c30",
            "c31", "c32","c33", "c34", "c35","c36", "c37", "c38", "c39", "c40",
            "c41", "c42","c43", "c44", "c45","c46", "c47", "c48", "c49", "c50",
            "c51", "c52","c53", "c54", "c55","c56", "c57", "c58", "c59", "c60",
            "c61", "c62","c63", "c64"};
    private CharSequence[] m_directory = null;

    // 需要估算出一个总页面数来决定adapter的item数量
    private int m_totalPages = 0;

    // 由于具体的页号已经不与书的页面对应，
    // 所以将当前页号作为基准点，只有pageUp与pageDown两种情况了
//    private int m_currentPage = 0;

    // 书签0,它总是指向当前显示的书页，包括默认的打开页面，此书签自动保存
    private BookMark m_bookmark0 = null;

    public enum PAGE_TYPE
    {
        PAGE_COMMON,
        PAGE_COVER,
        PAGE_BACK_COVER
    }

    /**
     * 构造函数
     * @param context
     * @param viewWidth
     * @param viewHeight
     */
    public BookFactory(Context context, int viewWidth, int viewHeight)
    {
        m_chapterInfoMap = new HashMap();
        m_chapterMap = new HashMap();
        m_context = context;

        // 由可见区域及画笔，计算行容量
        m_viewWidth = viewWidth;
        m_viewHeight = viewHeight;

        m_paint = new Paint(Paint.ANTI_ALIAS_FLAG);//抗锯齿
        m_paint.setColor(m_textColor);
//        m_paint.setTextSize(m_textSize);     放在设置函数中
        // 从配置文件中读取字号
        getSetting();
        initByTextSize((int)m_textSize);

        // 建立章映射表 (没有与文字尺寸的信息，只需要加载一次即可)
        preLoadChapters();

        // 读出bookmark0，并加载bookmark0对应的章节内容
        obtainBookmark0();
    }

    public void getSetting()
    {
        SharedPreferences sp = m_context.getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
        m_textSize = sp.getInt(KEY_SETTING_TEXT_SIZE, 30);
        m_autoTime = sp.getInt(KEY_SETTING_AUTO_TIME, 15); //15s
    }

    public void saveSetting()
    {
        SharedPreferences sp = m_context.getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(KEY_BOOKMARK0_CHAPTER_NUMBER, m_bookmark0.m_chapterNumber);
        editor.putInt(KEY_BOOKMARK0_PAGE_NUMBER, m_bookmark0.m_pageNumber);
        editor.putInt(KEY_SETTING_TEXT_SIZE, (int)m_textSize);
        editor.putInt(KEY_SETTING_AUTO_TIME, m_autoTime);
        editor.commit();
    }

    public boolean isCover()
    {
        return m_bookmark0.m_isCover;
    }

    public boolean isBackCover()
    {
        return m_bookmark0.m_isBackCover;
    }

    public void initByTextSize(int textSize)
    {
//        m_chapterInfoMap.clear();  没有与文字尺寸的信息，只需要加载一次即可
//        m_chapterMap.clear();

        m_textSize = textSize;
        m_paint.setTextSize(textSize);

        m_marginX = (int)m_textSize;
        m_marginY = (int)m_textSize;
        m_visibleWidth = m_viewWidth - m_marginX*2;
        m_visibleHeight = m_viewHeight - m_marginY*2;
        m_lineCapacity = m_visibleHeight / ((int)m_textSize + m_lineSpace);

    }

    /**
     * 预加载所有章节，只取文件size，不取内容，内存不允许，耗时太久
     */
    public void preLoadChapters()
    {
        // 预览每章文件的大小，以确定起始viewPage的数据和阅读百分比
        int chapterCount = CHAPTER_NAME_TABLE.length;
        String type = "raw";
        String packageName = m_context.getPackageName();
        BookChapterInfo chapterInfo = null;
        InputStream in = null;

        // 0作为填充结构，对算法实现有两点好处
        // 1，简化特殊的第一个元素
        // 2，章是以1作为起点，符合章的下标
        chapterInfo = new BookChapterInfo();
        chapterInfo.m_chapterName = CHAPTER_NAME_TABLE[0];
        chapterInfo.m_sizeSelf = 0;
        chapterInfo.m_sizeTotal = 0;
        m_chapterInfoMap.put(0, chapterInfo);
        for (int i=1; i<chapterCount; i++)
        {
            // 由文件名获取资源id ，进而打开文件流
            chapterInfo = new BookChapterInfo();
            chapterInfo.m_chapterName = CHAPTER_NAME_TABLE[i];
            int id = m_context.getResources().getIdentifier(chapterInfo.m_chapterName, type, packageName);
            in = m_context.getResources().openRawResource(id);
            try
            {
                chapterInfo.m_sizeSelf = in.available();
                // 自动计算出章名称
                boolean find = false;
                int j = 1;
                for (; j<chapterInfo.m_sizeSelf; j++)
                {
                     if (0x0a == in.read())
                     {
                         find = true;
                         break;
                     }
                }

                if (find)
                {
                    byte[] buf = new byte[j];
                    in.reset();
                    in.read(buf);
                    chapterInfo.m_chapterName = new String(buf, m_charset);
                    buf = null;
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


            // 后面的章节的size等于上个章节的size加上自己的，方便后期阅读百分比的计算
            chapterInfo.m_sizeTotal = chapterInfo.m_sizeSelf + ((BookChapterInfo)m_chapterInfoMap.get(i-1)).m_sizeTotal;
            m_chapterInfoMap.put(i, chapterInfo);
        }
    }

    /**
     * 读取bookmark0
     */
    public void obtainBookmark0()
    {
        // 0书签只用于保存当前书页面，可见是一个动态值
        m_bookmark0 = new BookMark();
        SharedPreferences sp = m_context.getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
        // 如果没历史记录，默认读取第一章，第一页面
        m_bookmark0.m_chapterNumber = sp.getInt(KEY_BOOKMARK0_CHAPTER_NUMBER, 1);
        m_bookmark0.m_pageNumber = sp.getInt(KEY_BOOKMARK0_PAGE_NUMBER, 0);

        // 按百分比来进行估算的item值
        page2item(m_bookmark0);
    }

    public void loadChapter(int chapterNumber)
    {
        BookChapter chapter = new BookChapter(m_context, CHAPTER_NAME_TABLE[chapterNumber]);
        chapter.sliceBlock();
        chapter.slicePages(m_paint, m_visibleWidth, m_lineCapacity, m_charset);
        m_chapterMap.put(chapterNumber, chapter);
        chapter = null;
    }

    public void page2item(BookMark bookmark)
    {
        // 由总文件字节估算出大概的页面数量
        // 这个数量大了没事，小了不行，所以尽量大些，防止小屏幕出现太多页面超出这个总页面数量
        double totalBytes = ((BookChapterInfo)m_chapterInfoMap.get(m_chapterInfoMap.size()-1)).m_sizeTotal;
        double totalPages = (totalBytes / (1024*10) + 1)*20 + 5000;//10KB 估算成 20页面,保底5000页面
        m_totalPages = (int)totalPages;

        // 此章已经加载（必须判断），可计算页面参数
        if (!m_chapterMap.containsKey(bookmark.m_chapterNumber))
        {
            loadChapter(bookmark.m_chapterNumber);
        }
        BookChapterInfo chapterInfo = (BookChapterInfo)m_chapterInfoMap.get(bookmark.m_chapterNumber);
        double pages = ((BookChapter)m_chapterMap.get(bookmark.m_chapterNumber)).getPageCount();
        double offsetBytes = (double)(bookmark.m_pageNumber+1) / pages * ((double)chapterInfo.m_sizeSelf); // 字节
        double item = (offsetBytes + (chapterInfo.m_sizeTotal-chapterInfo.m_sizeSelf)) / totalBytes * totalPages;

        // 页面是item对应
        bookmark.m_primaryItem = (int)item + 10;//设计成远离0-item，效果好
    }

    /**
     * 设置viewPager的当前项
     * @return
     */
    public int getPrimaryItem()
    {
        return m_bookmark0.m_primaryItem;
    }

    /**
     * 保存0书签
     */
//    public void saveBookmark0()
//    {
//        SharedPreferences sp = m_context.getSharedPreferences(XQJ_SP_FILE, Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sp.edit();
//        editor.putInt(KEY_BOOKMARK0_CHAPTER_NUMBER, m_bookmark0.m_chapterNumber);
//        editor.putInt(KEY_BOOKMARK0_PAGE_NUMBER, m_bookmark0.m_pageNumber);
//        editor.commit();
//    }


    /**
     * 如果设置主项成功（即到达这里），必须更新bookmark0
      * @param primaryItem
     */
    public void setPrimaryItem(int primaryItem)
    {
        // 需要同步更新 BookPage  (m_page)
        if (primaryItem == m_bookmark0.m_primaryItem + 1)//pageDown
        {
            m_bookmark0.m_primaryItem = primaryItem;
            pageDown(m_bookmark0);
        }
        else if (primaryItem == m_bookmark0.m_primaryItem - 1)//pageUp
        {
            m_bookmark0.m_primaryItem = primaryItem;
            pageUp(m_bookmark0);
        }
        else if (primaryItem == m_bookmark0.m_primaryItem)
        {
            // 不用做什么
        }
    }

    public int getFontSize()
    {
        return (int)m_textSize;
    }

    public void setFontSize(int fontSize)
    {
        // 设置了字体大小后，必须刷新所有页面数据
        initByTextSize(fontSize);

        // mark0需要刷新，只需要刷新页面号，章号不变，章必须重新加载
//        loadChapter(m_bookmark0.m_chapterNumber);  优化，不需要删除重新建，只要将页面信息刷新即可

        BookChapter chapter = (BookChapter)m_chapterMap.get(m_bookmark0.m_chapterNumber);
        double percent = (double)(m_bookmark0.m_pageNumber+1) / (double)chapter.getPageCount();

        // 必须将所有章内容按新size进行刷新
        Collection<BookChapter> c = m_chapterMap.values();
        Iterator it = c.iterator();
        for(;it.hasNext();)
        {
            ((BookChapter)it.next()).slicePages(m_paint, m_visibleWidth, m_lineCapacity, m_charset);
        }

//        chapter.slicePages(m_paint, m_visibleWidth, m_lineCapacity, m_charset);
        m_bookmark0.m_pageNumber = (int)((double)chapter.getPageCount()*percent) - 1;
        if (m_bookmark0.m_pageNumber < 0)
        {
            m_bookmark0.m_pageNumber = 0;//只能第一页面开始
        }
        // primaryItem不用刷新
    }

    public int getAutoTime()
    {
        return m_autoTime;
    }

    public void setAutoTime(int autoTime)
    {
        m_autoTime = autoTime;
    }

    public void pageDown(BookMark bookmark)
    {
        // 封面的下一页面，只需要将封面的标记清除即可，不需要其它操作
        if (bookmark.m_isCover)
        {
            bookmark.m_isCover = false;
            return;
        }

        // 清空标记
//        bookmark.m_isCover = false;
//        bookmark.m_isBackCover = false;

        // 已达最后一章
        int finalChapterNumber = m_chapterInfoMap.size()-1;//因为有个0作初始
        if (!m_chapterMap.containsKey(finalChapterNumber))
        {
            loadChapter(finalChapterNumber);
        }

        // 已达封底
        if (bookmark.m_chapterNumber == finalChapterNumber
                && bookmark.m_pageNumber == ((BookChapter)m_chapterMap.get(finalChapterNumber)).getPageCount() - 1)
        {
            bookmark.m_isBackCover = true;
        }
        else if (bookmark.m_pageNumber < ((BookChapter)m_chapterMap.get(bookmark.m_chapterNumber)).getPageCount() - 1)
        { //本章下一页面
            bookmark.m_pageNumber += 1;
        }
        else
        {//下章第一页面
            bookmark.m_chapterNumber += 1;
            // 如果这章内容不存在，就加载
            if (!m_chapterMap.containsKey(bookmark.m_chapterNumber))
            {
                loadChapter(bookmark.m_chapterNumber);
            }
            bookmark.m_pageNumber = 0;//页面以0为起点，章以1为起点
        }
    }

    public void pageUp(BookMark bookmark)
    {
        // 封底的上一页面，只需要将封底的标记清除即可，不需要其它操作
        if (bookmark.m_isBackCover)
        {
            bookmark.m_isBackCover = false;
            return;
        }

        // 清空标记 ,用不到
//        bookmark.m_isCover = false;
//        bookmark.m_isBackCover = false;

        // 已达封面
        if (bookmark.m_chapterNumber == 1 && bookmark.m_pageNumber == 0)
        {
            bookmark.m_isCover = true;
            bookmark.m_item_in_cover = bookmark.m_primaryItem;
        }
        else if (bookmark.m_pageNumber > 0)
        { //本章上一页面
            bookmark.m_pageNumber -= 1;
        }
        else
        { // 上章最后一页面
            bookmark.m_chapterNumber -= 1;
            if (!m_chapterMap.containsKey(bookmark.m_chapterNumber))
            {
                loadChapter(bookmark.m_chapterNumber);
            }
            bookmark.m_pageNumber = ((BookChapter)m_chapterMap.get(bookmark.m_chapterNumber)).getPageCount() - 1;//上一章最后一页面
        }
    }

    /**
     * 给出primary页面的属性，0正常，-1封面，+1封底
     * @return
     */
    public PAGE_TYPE getPrimaryItemType()
    {
        PAGE_TYPE pageType = null;
        if (m_bookmark0.m_isCover)
        {
            pageType = PAGE_TYPE.PAGE_COVER; //-1封面
        }
        if (m_bookmark0.m_isBackCover)
        {
            pageType = PAGE_TYPE.PAGE_BACK_COVER;   //+1封底
        }
        return pageType;
    }

    public int getPageCount()
    {
        return m_totalPages;// 暂时以这个大数作变页面数的缓冲池，约为可能页面的2倍
    }

    /**
     * 重新设置可见区域，需要清除块及其与页面的映射信息，重新计算
     * @param visibleWidth
     * @param visibleHeight
     */
    public void resetVisibleArea(int visibleWidth, int visibleHeight)
    {
        m_visibleWidth = visibleWidth;
        m_visibleHeight = visibleHeight;
        m_lineCapacity = m_visibleHeight / (int)m_textSize;
    }

    /**
     * 计算阅读的百分比
     * @return
     */
    public String getPercent()
    {
        // 总文件字节
        double totalBytes = ((BookChapterInfo)m_chapterInfoMap.get(m_chapterInfoMap.size()-1)).m_sizeTotal;

        // 此章已经加载，可计算页面参数
        BookChapterInfo chapterInfo = (BookChapterInfo)m_chapterInfoMap.get(m_bookmark0.m_chapterNumber);
        double pages = ((BookChapter)m_chapterMap.get(m_bookmark0.m_chapterNumber)).getPageCount();
        double selfBytes = (double)(m_bookmark0.m_pageNumber+1) / pages * ((double)chapterInfo.m_sizeSelf); // 字节
        double percent = (selfBytes + (chapterInfo.m_sizeTotal-chapterInfo.m_sizeSelf)) / totalBytes;

        // 转成百分比
        BigDecimal big = new BigDecimal(percent * 100);
        BigDecimal bigPercent = big.setScale(2, RoundingMode.HALF_UP);

        return bigPercent+"%";
    }

    public CharSequence[] getDirectory()
    {
        if (null == m_directory)
        {
            int size = m_chapterInfoMap.size() - 1;
            m_directory = new CharSequence[size];
            for (int i=0; i<size; i++)
            {
                m_directory[i] = ((BookChapterInfo)m_chapterInfoMap.get(i+1)).m_chapterName;
            }
        }
        return m_directory;
    }

    public void gotoChapter(int chapterNumber)
    {
        m_bookmark0.m_chapterNumber = chapterNumber;
        m_bookmark0.m_pageNumber = 0;
        page2item(m_bookmark0);
    }

    /**
     * 按页面的索引，绘制一页
     * 采用映射机制。好处：
     * 只需要处理未来的情况，即新页面的情况；而不要处理历史的情况，即旧页面的情况
     * 因为旧页面已经做好映射了，直接取出参数，绘制就行了
     * 所以，只需要有变更保存已经映射到的页面与对应的块位置即可
     * 只有新页面才要分块
     * @param canvas
     * @param item
     */
    public void drawPage(Canvas canvas, int item)
    {
        // 画背景(以view来画)
        canvas.drawColor(m_backgroundColor, PorterDuff.Mode.SRC);
        Bitmap bmp = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.bg_960x540_1);
        Rect srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        Rect dstRect = new Rect(0, 0, m_viewWidth, m_viewHeight);
        canvas.drawBitmap(bmp, srcRect, dstRect, m_paint);

        // 画页面 (以可视区域画，比view小)
        if (item == m_bookmark0.m_primaryItem + 1)
        {
            drawDownPage(canvas, item);
        }
        else if (item == m_bookmark0.m_primaryItem - 1)
        {
            drawUpPage(canvas, item);
        }
        else if (item == m_bookmark0.m_primaryItem)
        {
            drawPrimaryPage(canvas, item);
        }

    }

    public void drawUpPage(Canvas canvas, int item)
    {
        BookMark bookmark = new BookMark();
        bookmark.m_chapterNumber = m_bookmark0.m_chapterNumber;
        bookmark.m_pageNumber = m_bookmark0.m_pageNumber;
        bookmark.m_primaryItem = item;

        pageUp(bookmark);
        if (!bookmark.m_isCover)
        {
            BookChapter chapter = (BookChapter)m_chapterMap.get(bookmark.m_chapterNumber);
            chapter.drawPage(canvas, m_paint, m_visibleWidth,
                    m_lineCapacity, bookmark.m_pageNumber,
                    m_marginX, m_marginY, m_lineSpace);

            //写出页码
            drawPageNumber(canvas, bookmark);
//            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//            paint.setTextSize((float)m_textSize/3.0f*2.0f);
//            paint.setColor(0x88201004);
//            String pageNumber = "【第["+bookmark.m_chapterNumber+"]章,第["+(bookmark.m_pageNumber+1)+"/"+chapter.getPageCount()+"]页】";
//            float width = paint.measureText(pageNumber);
//            canvas.drawText(pageNumber, 0, pageNumber.length(), (m_viewWidth - width)/2, m_viewHeight - m_marginY, paint);
        }
        else
        {
            drawCover(canvas, PAGE_TYPE.PAGE_COVER);
//            String cover = "封面--《寻秦记》--黄易作品";
//            float width = m_paint.measureText(cover);
//            canvas.drawText(cover, 0, cover.length(), (m_viewWidth - width)/2, m_textSize*4, m_paint);
//            Bitmap bmp = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.cover);
//            float top = (m_viewWidth-bmp.getWidth())/2;
//            float left = (m_viewHeight-bmp.getHeight())/2;
//            canvas.drawBitmap(bmp, top, left, m_paint);
        }

    }

    public void drawDownPage(Canvas canvas, int item)
    {
        BookMark bookmark = new BookMark();
        bookmark.m_chapterNumber = m_bookmark0.m_chapterNumber;
        bookmark.m_pageNumber = m_bookmark0.m_pageNumber;
        bookmark.m_primaryItem = item;
        pageDown(bookmark);
        if (!bookmark.m_isBackCover)
        {
            BookChapter chapter = (BookChapter)m_chapterMap.get(bookmark.m_chapterNumber);
            chapter.drawPage(canvas,m_paint, m_visibleWidth,
                    m_lineCapacity, bookmark.m_pageNumber,
                    m_marginX, m_marginY, m_lineSpace);

            //写出页码
            drawPageNumber(canvas, bookmark);
//            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//            paint.setTextSize((float)m_textSize/3.0f*2.0f);
//            paint.setColor(0x88201004);
//            String pageNumber = "【第["+bookmark.m_chapterNumber+"]章,第["+(bookmark.m_pageNumber+1)+"/"+chapter.getPageCount()+"]页】";
//            float width = paint.measureText(pageNumber);
//            canvas.drawText(pageNumber, 0, pageNumber.length(), (m_viewWidth - width)/2, m_viewHeight - m_marginY, paint);
        }
        else
        {
            drawCover(canvas, PAGE_TYPE.PAGE_BACK_COVER);
//            String cover = "封底--《寻秦记》--黄易作品";
//            float width = m_paint.measureText(cover);
//            canvas.drawText(cover, 0, cover.length(), (m_viewWidth - width)/2, m_textSize*4, m_paint);
//            Bitmap bmp = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.backcover);
//            float top = (m_viewWidth-bmp.getWidth())/2;
//            float left = (m_viewHeight-bmp.getHeight())/2;
//            canvas.drawBitmap(bmp, top, left, m_paint);
        }

    }

    public void drawPrimaryPage(Canvas canvas,int item)
    {
        m_bookmark0.m_primaryItem = item;
        BookChapter chapter = (BookChapter)m_chapterMap.get(m_bookmark0.m_chapterNumber);
        chapter.drawPage(canvas,m_paint, m_visibleWidth,
                m_lineCapacity, m_bookmark0.m_pageNumber,
                m_marginX, m_marginY, m_lineSpace);

        drawPageNumber(canvas, m_bookmark0);
        // 写页码
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setTextSize((float)m_textSize/3.0f*2.0f);
//        paint.setColor(0x88201004);
//        String pageNumber = "【第["+m_bookmark0.m_chapterNumber+"]章,第["+(m_bookmark0.m_pageNumber+1)+"/"+chapter.getPageCount()+"]页】";
//        float width = paint.measureText(pageNumber);
//        canvas.drawText(pageNumber, 0, pageNumber.length(), (m_viewWidth - width)/2, m_viewHeight - m_marginY, paint);
    }

    public void drawPageNumber(Canvas canvas, BookMark bookmark)
    {

        // 写页码
        BookChapter chapter = (BookChapter)m_chapterMap.get(bookmark.m_chapterNumber);
        BookChapterInfo chapterInfo = (BookChapterInfo)m_chapterInfoMap.get(bookmark.m_chapterNumber);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize((float)m_textSize/3.0f*2.0f);
        paint.setColor(m_textColor);
        float ascent = paint.ascent();
        String pageNumber = "【"+chapterInfo.m_chapterName+"第("+(bookmark.m_pageNumber+1)+"/"+chapter.getPageCount()+")页-"+getPercent()+"】";
        float width = paint.measureText(pageNumber);
        canvas.drawText(pageNumber, 0, pageNumber.length(), (m_viewWidth - width)/2, m_viewHeight - m_marginY - ascent, paint);

        // 画页眉线与页脚线
        // 页眉线
        float sx = m_marginX;
        float sy =  m_marginY-10;
        float ex =  m_viewWidth - m_marginX;
        float ey = sy;
        Paint paint4Line = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint4Line.setColor(0xffffffff);
        paint4Line.setStrokeWidth(2);
        canvas.drawLine(sx, sy, ex, ey, paint4Line);
        sy -= 1;
        ey = sy;
        paint4Line.setStrokeWidth(1);
        paint4Line.setColor(0xff000000);
        canvas.drawLine(sx, sy, ex, ey, paint4Line);

        // 页脚线
        sy =  m_viewHeight - m_marginY - ascent - (float)m_textSize/3.0f*2.0f - 10;
        ey = sy;
        paint4Line.setColor(0xffffffff);
        paint4Line.setStrokeWidth(2);
        canvas.drawLine(sx, sy, ex, ey, paint4Line);
        sy -= 1;
        ey = sy;
        paint4Line.setStrokeWidth(1);
        paint4Line.setColor(0xff000000);
        canvas.drawLine(sx, sy, ex, ey, paint4Line);

    }

    public void drawCover(Canvas canvas, PAGE_TYPE pageType)
    {
        String cover = null;
        Bitmap bmp = null;
        if (PAGE_TYPE.PAGE_COVER == pageType)
        {
            cover = m_context.getString(R.string.cover);
            bmp = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.cover_512x512);
        }
        else if (PAGE_TYPE.PAGE_BACK_COVER == pageType)
        {
            cover = m_context.getString(R.string.back_cover);
            bmp = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.cover_512x512);
        }

        float width = m_paint.measureText(cover);
        canvas.drawText(cover, 0, cover.length(), (m_viewWidth - width)/2, m_textSize*4, m_paint);
        float top = (m_viewWidth-bmp.getWidth())/2;
        float left = (m_viewHeight-bmp.getHeight())/2;
        canvas.drawBitmap(bmp, top, left, m_paint);
    }
}
