package me.maplef.mapbotv4.utils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TextComparator {
    public static double getLSTSimilarity(String str1 , String str2){
        int str1Len = str1.length(), str2Len = str2.length();
        if (str1Len == 0 || str2Len == 0) return 0;

        int[][] distance = new int[str1Len + 1][str2Len + 1];

        int maxLen = Math.max(str1Len, str2Len);
        for (int num = 0; num < maxLen + 1; num++){
            if (num < str1Len + 1) distance[num][0] = num;
            if (num < str2Len + 1) distance[0][num] = num;
        }

        for (int row = 1; row < str1Len+1; row++){
            char c1 = str1.charAt(row - 1);
            for (int col = 1; col < str2Len+1; col++){
                char c2 = str2.charAt(col - 1);
                if (c1 == c2)
                    distance[row][col] = distance[row - 1][col - 1];
                else
                    distance[row][col] = Math.min(Math.min(distance[row-1][col], distance[row][col-1]), distance[row-1][col-1]) + 1;
            }
        }

        int notSimilarNum = distance[str1Len][str2Len];

        return 1 - (double)notSimilarNum / maxLen;
    }

    public static double getCosSimilarity(String doc1, String doc2) {
        if (doc1 != null && doc1.trim().length() > 0 && doc2 != null && doc2.trim().length() > 0) {

            Map<Integer, int[]> AlgorithmMap = new HashMap<>();

            for (int i = 0; i < doc1.length(); i++) {
                char d1 = doc1.charAt(i);
                if(isHanZi(d1) || ((int)d1>=65 && (int)d1<=90) || ((int)d1>=97 && (int)d1<=122)){
                    int charIndex = getGB2312Id(d1);
                    if(charIndex == -1) charIndex = d1;
                    int[] fq = AlgorithmMap.get(charIndex);
                    if(fq != null && fq.length == 2){
                        fq[0]++;//已有该字符，加1
                    }else {
                        fq = new int[2];
                        fq[0] = 1;
                        fq[1] = 0;
                        AlgorithmMap.put(charIndex, fq);
                    }
                }
            }

            for (int i = 0; i < doc2.length(); i++) {
                char d2 = doc2.charAt(i);
                if(isHanZi(d2) || ((int)d2>=65 && (int)d2<=90) || ((int)d2>=97 && (int)d2<=122)){
                    int charIndex = getGB2312Id(d2);
                    if(charIndex == -1) charIndex = d2;
                    int[] fq = AlgorithmMap.get(charIndex);
                    if(fq != null && fq.length == 2){
                        fq[1]++;
                    }else {
                        fq = new int[2];
                        fq[0] = 0;
                        fq[1] = 1;
                        AlgorithmMap.put(charIndex, fq);
                    }
                }
            }

            Iterator<Integer> iterator = AlgorithmMap.keySet().iterator();
            double sqdoc1 = 0;
            double sqdoc2 = 0;
            double denominator = 0;
            while(iterator.hasNext()){
                int[] c = AlgorithmMap.get(iterator.next());
                denominator += c[0]*c[1];
                sqdoc1 += c[0]*c[0];
                sqdoc2 += c[1]*c[1];
            }

            return denominator / Math.sqrt(sqdoc1*sqdoc2);//余弦计算
        } else {
            throw new NullPointerException(" the Document is null or have not cahrs!!");
        }
    }

    public static boolean isHanZi(char ch) {
        return (ch >= 0x4E00 && ch <= 0x9FA5);
    }

    /**
     * 根据输入的Unicode字符，获取它的GB2312编码或者ascii编码，
     *
     * @param ch 输入的GB2312中文字符或者ASCII字符(128个)
     * @return ch在GB2312中的位置，-1表示该字符不认识
     */
    public static short getGB2312Id(char ch) {
        try {
            byte[] buffer = Character.toString(ch).getBytes("GB2312");
            if (buffer.length != 2) {
                // 正常情况下buffer应该是两个字节，否则说明ch不属于GB2312编码，故返回'?'，此时说明不认识该字符
                return -1;
            }
            int b0 = (buffer[0] & 0x0FF) - 161; // 编码从A1开始，因此减去0xA1=161
            int b1 = (buffer[1] & 0x0FF) - 161;
            return (short) (b0 * 94 + b1);// 第一个字符和最后一个字符没有汉字，因此每个区只收16*6-2=94个汉字
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return -1;
    }

//    public static void main(String[] args) {
//        String str1="余弦定理算法：doc1 与 doc2 相似度为：0.9954971, 耗时：22mm";
//        String str2="余弦定理算法：doc1 和doc2 相似度为：0.99425095, 用时：33mm";
//        long start=System.currentTimeMillis();
//        double Similarity=Cosine.getSimilarity(str1, str2);
//        System.out.println("用时:"+(System.currentTimeMillis()-start));
//        System.out.println(Similarity);
//    }
}

