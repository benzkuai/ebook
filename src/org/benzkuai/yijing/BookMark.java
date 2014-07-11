package org.benzkuai.yijing;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-1-17
 * Time: 下午11:37
 * To change this template use File | Settings | File Templates.
 */
public class BookMark {
    public int m_chapterNumber = 0;//章编号(1起始)
    public int m_pageNumber = 0;  //书页编号（0起始）
    public int m_primaryItem = 0; //viewPager中当前选中页面，即当前显示页面

    public boolean m_isCover = false; //封面
    public int m_item_in_cover = 0; // 在封面时的item号
    public boolean m_isBackCover = false; //封底
    public int m_item_in_backCover = 0;
}
