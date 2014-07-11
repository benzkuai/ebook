package org.benzkuai.yijing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-18
 * Time: 下午7:49
 * To change this template use File | Settings | File Templates.
 */
public class BookChapter {

    // 章序号
    private int m_chapterNumber = 0;

    // 字节数组
    private byte[] m_byteArray = null;
    private int m_block = 0; // 在第N块上
    private int m_offset = 0;// 在第N块的X偏移处

    // 块容器
    private Vector<BookBlock> m_blockVector = null;

    // 页面数
    private int m_pageCount = 0;

    // 页面映射容器
    private Map m_pageMap = null;

    public BookChapter(Context context, String chapterName)
    {
        // 由文件名获取id，给open函数用
        String packageName = context.getPackageName();
        String type = "raw";
        int id = context.getResources().getIdentifier(chapterName, type, packageName);

        // 文件输入流（raw）
        InputStream in = context.getResources().openRawResource(id);
        try
        {
            int length = in.available();
            m_byteArray = new byte[length];
            in.read(m_byteArray);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // 对象创建
        m_blockVector = new Vector<BookBlock>();
        m_pageMap = new HashMap();
    }

    /**
     * 将整章内容划分成块
     * 划块的目的是：解决解码的起点问题。
     */
    public void sliceBlock()
    {
        m_blockVector.clear();
        boolean newBlock = true;
        BookBlock block = null;
        for (int i=0; i<m_byteArray.length; i++)
        {
            if (newBlock)
            {
                block = new BookBlock();
                block.setBegin(i);
                newBlock = false;
            }
            // 换行符（规则：文件结尾也必须是换行符）
            if (0x0a == m_byteArray[i])
            {
                block.setEnd(i);
                block.obtainBytes(m_byteArray);//获取文本内容，填充到块中 （内存成块copy，比较快）
                m_blockVector.add(block);
                block = null;
                newBlock = true;// 新开块
            }
        }
    }

    /**
     * 页面的计算，需要明确字节编码（字符集）
     * @param paint
     * @param visibleWidth
     * @param lineCapacity
     * @param charset    字符集
     */
    public void slicePages(Paint paint, int visibleWidth, int lineCapacity, String charset)
     {
         m_pageMap.clear();
         boolean isNewPage = true;
         BookPageMapParam pageMapParam = null;

         int pages = 0;
         int blockCount = m_blockVector.size();
         int remainLines = lineCapacity;

         // 将所有块的字符位置置0
         for (int j=0; j<blockCount;j++)
         {
             m_blockVector.get(j).resetCharOffset();
         }

         for (int i=0; i<blockCount; i++)
         {
             if (isNewPage)
             {
                 pageMapParam = new BookPageMapParam();
                 pageMapParam.m_block = i;
                 pageMapParam.m_charOffset = m_blockVector.get(i).getCharOffset();//字符在块内的偏移
                 m_pageMap.put(pages, pageMapParam);
                 pageMapParam = null;
                 isNewPage = false;
             }
             remainLines = m_blockVector.get(i).fillPage(paint, visibleWidth, remainLines, charset);
             if (remainLines > 0)
             {
                 if (i == (blockCount-1))// 页面完成且有留白，但已经是最后一块了，无新块可用
                 {
                     pages++;
                     remainLines = lineCapacity;//新页面，剩余行数要重置
                     isNewPage = true;
                 }
                 // 页面未完成，需要新块
             }
             else if (0 == remainLines)
             {
                 // 页面完成，用新块填充新页面
                 pages++;
                 remainLines = lineCapacity;
                 isNewPage = true;
             }
             else if (-1 == remainLines)
             {
                 // 此块有富裕，可以再填充新页面
                 pages++;
                 i--;
                 remainLines = lineCapacity;
                 isNewPage = true;
                 // 块内字符偏移由块自己维护
             }
         }

         m_pageCount = pages;
     }

    public int getPageCount()
    {
        return m_pageCount;
    }

    public void drawPage(Canvas canvas, Paint paint, int visibleWidth,
                         int lineCapacity, int pageIndex,
                         int marginX, int marginY, int lineSpace)
    {
        if (!m_pageMap.containsKey(pageIndex))
        {
            canvas.drawText("杯具，没页面数据!",50, 100, paint);
            return;
        }

        // 总是从映射表中取出已经计算完毕的参数进行绘制
        BookPageMapParam pageMapParam = (BookPageMapParam)m_pageMap.get(pageIndex);

        int blockCount = m_blockVector.size();
        int remainLines = lineCapacity;
        int lineStart = lineCapacity - remainLines;
        int offset = 0;
        for (int i=pageMapParam.m_block; i<blockCount; i++)
        {
            if (pageMapParam.m_block == i)
            {
                offset = pageMapParam.m_charOffset; //只有第一块的字符起点可能是不为0的偏移，其它必然是0
            }
            else
            {
                offset = 0;
            }
            remainLines = m_blockVector.get(i).drawPage(canvas, paint, offset, visibleWidth,
                    lineStart, remainLines, marginX, marginY, lineSpace);

            if (remainLines > 0)
            {
                if (i == (blockCount-1))// 页面完成且有留白，但已经是最后一块了，无新块可用
                {
                   break;
                }
                // 页面未完成，需要新块
                lineStart = lineCapacity - remainLines;
            }
            else if (0 == remainLines)
            {
                // 页面完成，用新块填充新页面
               break;
            }
            else if (-1 == remainLines)
            {
                // 此块有富裕，可以再填充新页面
                break;
            }
        }


    }
}

