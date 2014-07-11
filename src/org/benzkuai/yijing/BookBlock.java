package org.benzkuai.yijing;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-12
 * Time: 下午12:52
 * To change this template use File | Settings | File Templates.
 */
public class BookBlock {
    // 字节
    private int m_offsetBegin = 0;//在章中的偏移位置（起点）
    private int m_offsetEnd = 0; //在章中的偏移位置 （终点）
    private int m_length = 0;         // 此块的长度（字节数）
    private int m_offsetInBlock = 0;  //在块中的偏移
    private byte[] m_byteArray = null;

    // 解码（字符）,编码方式（字符集）由上层传来
    private CharSequence m_charSequence = null;//接口
    private int m_charPosition = 0;//解码后，初始字符位置下标(用于分页）
    private int m_charLength = 0; //解码后，字符串长度

    /**
     *
     */
    public BookBlock()
    {

    }

    public void setBegin(int offsetInChapter)
    {
        m_offsetBegin = offsetInChapter;
    }

    public void setEnd(int offsetInChapter)
    {
        m_offsetEnd = offsetInChapter;
        m_length = m_offsetEnd - m_offsetBegin + 1;
    }

    public void obtainBytes(byte[] chapter)
    {
        m_byteArray = new byte[m_length];
        System.arraycopy(chapter, m_offsetBegin, m_byteArray, 0, m_length);
    }

    public void decode(String charset)
    {
        try
        {
            m_charSequence = new String(m_byteArray, charset);
            m_charLength = m_charSequence.length();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        // CharSequence是接口,String实现它
    }

    public int getCharOffset()
    {
        return m_charPosition;
    }

    public void resetCharOffset()
    {
        m_charPosition = 0;
    }

    public int fillPage(Paint paint, int visibleWidth, int remainLines, String charset)
    {
        // 如果没解码，先解码
        if (null == m_charSequence)
        {
            decode(charset);
        }

        // 此函数有能力从字符串的后面开始测量，有方向参数false，为从后开始
        int measured = 0;
        int lines = 0;
        int remainLinesTemp = 0;
        boolean isBlockEnd = false;
        boolean isLineFull = false;

        do {
            measured = paint.breakText(m_charSequence, m_charPosition, m_charLength, true, visibleWidth, null);
            m_charPosition += measured;
            lines++;
            if (m_charPosition >= m_charLength)//字符串结束
            {
                isBlockEnd = true;
            }
            if (lines >= remainLines)//行满
            {
                isLineFull = true;
            }

            if (isBlockEnd && isLineFull)
            {
                remainLinesTemp = 0;
                break;
            }
            else if (isLineFull) // 大块，还可以再填充新页面
            {
                remainLinesTemp = -1;
                break;
            }
            else if (isBlockEnd)
            {
                remainLinesTemp = remainLines - lines;
                break;
            }

        }   while (true);

        return remainLinesTemp;
    }

    public int drawPage(Canvas canvas, Paint paint, int offset, int visibleWidth,
                        int lineStart, int remainLines,
                        int marginX, int marginY, int lineSpace)
    {
        // 此函数有能力从字符串的后面开始测量，有方向参数false，为从后开始
        int measured = 0;
        int linesDrawn = 0;
        int remainLinesTemp = remainLines;
        boolean isBlockEnd = false;
        boolean isLineFull = false;

        float ascent = paint.ascent();// 下降基线数值，此值为负
//        float fDescent = paint.descent();// 此值为正
        float textSize = paint.getTextSize();

        float x = marginX;
        float y = 0;
        int charOffset = offset;
        do {

            // 测量
            measured = paint.breakText(m_charSequence, charOffset, m_charLength, true, visibleWidth, null);

            // 绘制
            paint.setShadowLayer(3, 1, 2, 0x60333333);
            y = -ascent + (lineStart+linesDrawn)*(textSize + lineSpace) + marginY;
            canvas.drawText(m_charSequence, charOffset, charOffset+ measured, x, y, paint);
            charOffset += measured;
            linesDrawn++;
            if (charOffset >= m_charLength)//字符串结束
            {
                isBlockEnd = true;
            }
            if (linesDrawn >= remainLines)//行满
            {
                isLineFull = true;
            }

            if (isBlockEnd && isLineFull)
            {
                remainLinesTemp = 0;
                break;
            }
            else if (isLineFull) // 大块，还可以再填充新页面
            {
                remainLinesTemp = -1;
                break;
            }
            else if (isBlockEnd)
            {
                remainLinesTemp = remainLines - linesDrawn;
                break;
            }

        }   while (true);

        return remainLinesTemp;
    }
}